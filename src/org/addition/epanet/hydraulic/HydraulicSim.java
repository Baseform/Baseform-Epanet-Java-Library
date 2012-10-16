/*
 * Copyright (C) 2012  Addition, Lda. (addition at addition dot pt)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */

package org.addition.epanet.hydraulic;


import org.addition.epanet.Constants;
import org.addition.epanet.hydraulic.io.AwareStep;
import org.addition.epanet.hydraulic.models.CMModelCalculator;
import org.addition.epanet.hydraulic.models.DwModelCalculator;
import org.addition.epanet.hydraulic.models.HWModelCalculator;
import org.addition.epanet.hydraulic.models.PipeHeadModel;
import org.addition.epanet.hydraulic.structures.*;
import org.addition.epanet.network.FieldsMap;
import org.addition.epanet.network.FieldsMap.Type;
import org.addition.epanet.network.Network;
import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.network.io.Keywords;
import org.addition.epanet.network.structures.*;
import org.addition.epanet.network.structures.Link.LinkType;
import org.addition.epanet.network.structures.Link.StatType;
import org.addition.epanet.util.ENException;
import org.addition.epanet.util.Utilities;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Hydraulic simulation class.
 */
public class HydraulicSim {


    public static final int BUFFER_SIZE = 512 * 1024; //512kb
    protected transient boolean running;
    protected transient Thread runningThread;

    protected HydraulicSim() {
    }


    public ByteBuffer getStepSavingByteBuffer() {
        return stepSavingByteBuffer;
    }

    public void setStepSavingByteBuffer(ByteBuffer stepSavingByteBuffer) {
        this.stepSavingByteBuffer = stepSavingByteBuffer;
    }

    public ByteBuffer stepSavingByteBuffer = null;


    /**
     * Step solving result
     */
    protected static class NetSolveStep {
        private NetSolveStep(int iter, double relerr) {
            this.iter = iter;
            this.relerr = relerr;
        }

        public int iter;
        public double relerr;
    }

    /**
     * Event logger reference.
     */
    protected Logger logger;


    protected List<SimulationNode> nNodes;
    protected List<SimulationLink> nLinks;
    protected List<SimulationPump> nPumps;
    protected List<SimulationTank> nTanks;
    protected List<SimulationNode> nJunctions;
    protected List<SimulationValve> nValves;


    protected List<SimulationControl> nControls;
    protected List<SimulationRule> nRules;
    protected Curve[] nCurves;


    /**
     * Simulation conversion units.
     */
    protected FieldsMap fMap;

    /**
     * Simulation properties.
     */
    protected PropertiesMap pMap;

    /**
     * Energy cost time pattern.
     */
    protected Pattern Epat;

    /**
     * Linear system solver support class.
     */
    protected SparseMatrix smat;

    /**
     * Linear system variable storage class.
     */
    protected LSVariables lsv;

    /**
     * Current report time.
     */
    protected long Rtime;

    /**
     * Current hydraulic simulation time.
     */
    protected long Htime;

    /**
     * System wide demand.
     */
    protected double Dsystem;

    /**
     * Output stream of the hydraulic solution.
     */
    protected DataOutput simulationOutput;

    /**
     * Pipe headloss model calculator.
     */
    protected PipeHeadModel pHLModel;

    /**
     * Get current hydraulic simulation time.
     *
     * @return
     */
    public long getHtime() {
        return Htime;
    }

    /**
     * Get current report time.
     *
     * @return
     */
    public long getRtime() {
        return Rtime;
    }


    /**
     * Init hydraulic simulation, preparing the linear solver and the hydraulic structures wrappers.
     *
     * @param net Hydraulic network reference.
     * @param log Logger reference.
     * @throws ENException
     */
    public HydraulicSim(Network net, Logger log) throws ENException {
        List<Node> tmpNodes = new ArrayList<Node>(net.getNodes());
        List<Link> tmpLinks = new ArrayList<Link>(net.getLinks());
        running = false;
        logger = log;
        createSimulationNetwork(tmpNodes, tmpLinks, net);
    }

    protected void createSimulationNetwork(List<Node> tmpNodes, List<Link> tmpLinks, Network net) throws ENException {

        nNodes = new ArrayList<SimulationNode>();
        nLinks = new ArrayList<SimulationLink>();
        nPumps = new ArrayList<SimulationPump>();
        nTanks = new ArrayList<SimulationTank>();
        nJunctions = new ArrayList<SimulationNode>();
        nValves = new ArrayList<SimulationValve>();
        nRules = new ArrayList<SimulationRule>();


        Map<String, SimulationNode> nodesById = new HashMap<String, SimulationNode>();
        for (Node n : tmpNodes) {
            SimulationNode node = SimulationNode.createIndexedNode(n, nNodes.size());
            nNodes.add(node);
            nodesById.put(node.getId(), node);

            if (node instanceof SimulationTank)
                nTanks.add((SimulationTank) node);
            else
                nJunctions.add(node);
        }

        for (Link l : tmpLinks) {
            SimulationLink link = SimulationLink.createIndexedLink(nodesById, l, nLinks.size());
            nLinks.add(link);

            if (link instanceof SimulationValve)
                nValves.add((SimulationValve) link);
            else if (link instanceof SimulationPump)
                nPumps.add((SimulationPump) link);
        }

        for (Rule r : net.getRules()) {
            SimulationRule rule = new SimulationRule(r, nLinks, nNodes);//, tmpLinks, tmpNodes);
            nRules.add(rule);
        }

        nCurves = net.getCurves().toArray(new Curve[net.getCurves().size()]);
        nControls = new ArrayList<SimulationControl>();

        for (Control ctr : net.getControls())
            nControls.add(new SimulationControl(nNodes, nLinks, ctr));


        fMap = net.getFieldsMap();
        pMap = net.getPropertiesMap();
        Epat = net.getPattern(pMap.getEpatId());
        smat = new SparseMatrix(nNodes, nLinks, nJunctions.size());
        lsv = new LSVariables(nNodes.size(), smat.getCoeffsCount());

        Htime = 0;

        switch (pMap.getFormflag()) {

            case HW:
                pHLModel = new HWModelCalculator();
                break;
            case DW:
                pHLModel = new DwModelCalculator();
                break;
            case CM:
                pHLModel = new CMModelCalculator();
                break;
        }


        for (SimulationLink link : nLinks) {
            link.initLinkFlow();
        }


        for (SimulationNode node : nJunctions) {
            if (node.getKe() > 0.0)
                node.setSimEmitter(1.0);
        }

        for (SimulationLink link : nLinks) {

            if ((link.getType() == LinkType.PRV ||
                    link.getType() == LinkType.PSV ||
                    link.getType() == LinkType.FCV)
                    &&
                    (link.getRoughness() != Constants.MISSING))
                link.setSimStatus(StatType.ACTIVE);


            if (link.getSimStatus().id <= StatType.CLOSED.id)
                link.setSimFlow(Constants.QZERO);
            else if (Math.abs(link.getSimFlow()) <= Constants.QZERO)
                link.initLinkFlow(link.getSimStatus(), link.getSimSetting());

            link.setSimOldStatus(link.getSimStatus());
        }

        for (SimulationPump pump : nPumps) {
            for (int j = 0; j < 6; j++)
                pump.setEnergy(j, 0.0);
        }

        Htime = 0;
        Rtime = pMap.getRstep();
    }

    /**
     * Run hydraulic simuation.
     *
     * @param hyd Abstract file where the output of the hydraulic simulation will be writen.
     * @throws ENException
     */
    public void simulate(File hyd) throws ENException {
        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(hyd), BUFFER_SIZE);
            simulate(out);
            out.close();
        } catch (IOException ex) {
            throw new ENException(305);
        }
    }


    /**
     * Run hydraulic simuation.
     *
     * @param out output stream for the hydraulic simulation data.
     * @throws ENException
     */
    public void simulate(OutputStream out) throws ENException, IOException {
        simulate((DataOutput) new DataOutputStream(out));

    }

    public long simulateSingleStep() throws ENException, IOException {

        if (!running)
            running = true;

        if (!runHyd()) {
            running = false;
            return 0;
        }

        long hydstep = 0;

        if (Htime < pMap.getDuration())
            hydstep = timeStep();

        if (pMap.getDuration() == 0)
            SimulationPump.stepEnergy(pMap, fMap, Epat, nPumps, Htime, 0);
        else if (Htime < pMap.getDuration())
            SimulationPump.stepEnergy(pMap, fMap, Epat, nPumps, Htime, hydstep);

        if (Htime < pMap.getDuration()) {
            Htime += hydstep;
            if (Htime >= Rtime)
                Rtime += pMap.getRstep();
        }

        long tstep = hydstep;

        if (!running && tstep > 0) {
            running = false;
            return 0;
        }

        if (running && tstep > 0)
            return tstep;
        else {
            running = false;
            return 0;
        }
    }

    public void simulate(DataOutput out) throws ENException, IOException {
        boolean halted = false;

        if (running)
            throw new IllegalStateException("Already running");

        runningThread = Thread.currentThread();
        running = true;

        simulationOutput = out;
        if (simulationOutput != null) {
            AwareStep.writeHeader(out, this, pMap.getRstart(), pMap.getRstep(), pMap.getDuration());
        }
//        writeHeader(simulationOutput);
        try {
            long tstep;
            do {
                if (!runHyd())
                    break;

                tstep = nextHyd();

                if (!running && tstep > 0)
                    halted = true;
            }
            while (running && tstep > 0);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ENException(1000);
        } finally {
            running = false;
            runningThread = null;
        }

        if (halted)
            throw new ENException(1000);

    }


    /**
     * Halt hydraulic simulation.
     *
     * @throws InterruptedException
     */
    public void stopRunning() throws InterruptedException {
        running = false;
        if (runningThread != null && runningThread.isAlive())
            runningThread.join(1000);
    }

    /**
     * Solves network hydraulics in a single time period.
     */
    private boolean runHyd() throws ENException {

        // Find new demands & control actions
        computeDemands();
        computeControls();

        // Solve network hydraulic equations
        NetSolveStep nss = netSolve();

        // Report new status & save results
        if (pMap.getStatflag() != PropertiesMap.StatFlag.FALSE)
            logHydStat(nss);

        // If system unbalanced and no extra trials
        // allowed, then activate the Haltflag.
        if (nss.relerr > pMap.getHacc() && pMap.getExtraIter() == -1) {
            Htime = pMap.getDuration();
            return false;
        }

        logHydWarn(nss);

        return true;
    }

    /**
     * Solve the linear equation system to compute the links flows and nodes heads.
     *
     * @return Solver steps and relative error
     * @throws ENException
     */
    protected NetSolveStep netSolve() throws ENException {
        NetSolveStep ret = new NetSolveStep(0, 0);

        int nextCheck = pMap.getCheckFreq();

        if (pMap.getStatflag() == PropertiesMap.StatFlag.FULL)
            logRelErr(ret);

        int maxTrials = pMap.getMaxIter();

        if (pMap.getExtraIter() > 0)
            maxTrials += pMap.getExtraIter();

        double relaxFactor = 1.0;
        int errcode = 0;
        ret.iter = 1;

        while (ret.iter <= maxTrials) {
            //Compute coefficient matrices A & F and solve A*H = F
            // where H = heads, A = Jacobian coeffs. derived from
            // head loss gradients, & F = flow correction terms.
            newCoeffs();

            //dumpMatrixCoeffs(new File("dumpMatrix.txt"),true);

            // Solution for H is returned in F from call to linsolve().
            errcode = smat.linsolve(nJunctions.size(), lsv.getAiiVector(), lsv.getAijVector(), lsv.getRHSCoeffs());

            // Ill-conditioning problem
            if (errcode > 0) {
                // If control valve causing problem, fix its status & continue,
                // otherwise end the iterations with no solution.
                if (SimulationValve.checkBadValve(pMap, logger, nValves, Htime, smat.getOrder(errcode)))
                    continue;
                else break;
            }

            // Update current solution.
            // (Row[i] = row of solution matrix corresponding to node i).
            for (SimulationNode node : nJunctions) {
                node.setSimHead(lsv.getRHSCoeff(smat.getRow(node.getIndex()))); // Update heads
            }

            // Update flows
            ret.relerr = newFlows(relaxFactor);

            // Write convergence error to status report if called for
            if (pMap.getStatflag() == PropertiesMap.StatFlag.FULL)
                logRelErr(ret);

            relaxFactor = 1.0;

            boolean valveChange = false;

            //  Apply solution damping & check for change in valve status
            if (pMap.getDampLimit() > 0.0) {
                if (ret.relerr <= pMap.getDampLimit()) {
                    relaxFactor = 0.6;
                    valveChange = SimulationValve.valveStatus(fMap, pMap, logger, nValves);
                }
            } else
                valveChange = SimulationValve.valveStatus(fMap, pMap, logger, nValves);

            // Check for convergence
            if (ret.relerr <= pMap.getHacc()) {

                //  We have convergence. Quit if we are into extra iterations.
                if (ret.iter > pMap.getMaxIter())
                    break;

                //  Quit if no status changes occur.
                boolean statChange = false;

                if (valveChange)
                    statChange = true;

                if (SimulationLink.linkStatus(pMap, fMap, logger, nLinks))
                    statChange = true;

                if (SimulationControl.pSwitch(logger, pMap, fMap, nControls))
                    statChange = true;

                if (!statChange)
                    break;

                //  We have a status change so continue the iterations
                nextCheck = ret.iter + pMap.getCheckFreq();
            } else if (ret.iter <= pMap.getMaxCheck() && ret.iter == nextCheck) {
                // No convergence yet. See if its time for a periodic status
                // check  on pumps, CV's, and pipes connected to tanks.
                SimulationLink.linkStatus(pMap, fMap, logger, nLinks);
                nextCheck += pMap.getCheckFreq();
            }

            ret.iter++;
        }


        for (SimulationNode node : nJunctions)
            node.setSimDemand(node.getSimDemand() + node.getSimEmitter());

        if (errcode > 0) {
            logHydErr(smat.getOrder(errcode));
            errcode = 110;
            return ret;
        }

        if (errcode != 0)
            throw new ENException(errcode);

        return ret;
    }


    /**
     * Computes coefficients of linearized network eqns.
     */
    void newCoeffs() throws ENException {
        lsv.clear();

        for (SimulationLink link : nLinks) {
            link.setSimInvHeadLoss(0);
            link.setSimFlowCorrection(0);
        }

        SimulationLink.computeMatrixCoeffs(fMap, pMap, pHLModel, nLinks, nCurves, smat, lsv);   // Compute link coeffs.
        SimulationNode.computeEmitterCoeffs(pMap, nJunctions, smat, lsv);                       // Compute emitter coeffs.
        SimulationNode.computeNodeCoeffs(nJunctions, smat, lsv);                                // Compute node coeffs.
        SimulationValve.computeMatrixCoeffs(pMap, lsv, smat, nValves);                          // Compute valve coeffs.
    }

    /**
     * Updates link flows after new nodal heads computed.
     */
    double newFlows(double RelaxFactor) throws ENException {

        for (SimulationNode node : nTanks)
            node.setSimDemand(0);

        double qsum = 0.0;
        double dqsum = 0.0;

        for (SimulationLink link : nLinks) {
            SimulationNode n1 = link.getFirst();
            SimulationNode n2 = link.getSecond();

            double dh = n1.getSimHead() - n2.getSimHead();
            double dq = link.getSimFlowCorrection() - link.getSimInvHeadLoss() * dh;

            dq *= RelaxFactor;

            if (link instanceof SimulationPump) {
                if (((SimulationPump) link).getPtype() == Pump.Type.CONST_HP && dq > link.getSimFlow())
                    dq = link.getSimFlow() / 2.0;
            }

            link.setSimFlow(link.getSimFlow() - dq);

            qsum += Math.abs(link.getSimFlow());
            dqsum += Math.abs(dq);

            if (link.getSimStatus().id > StatType.CLOSED.id) {
                if (n1 instanceof SimulationTank)
                    n1.setSimDemand(n1.getSimDemand() - link.getSimFlow());
                if (n2 instanceof SimulationTank)
                    n2.setSimDemand(n2.getSimDemand() + link.getSimFlow());
            }
        }

        for (SimulationNode node : nJunctions) {

            if (node.getKe() == 0.0)
                continue;
            double dq = node.emitFlowChange(pMap);
            node.setSimEmitter(node.getSimEmitter() - dq);
            qsum += Math.abs(node.getSimEmitter());
            dqsum += Math.abs(dq);
        }

        if (qsum > pMap.getHacc())
            return (dqsum / qsum);
        else
            return (dqsum);

    }

    /**
     * Implements simple controls based on time or tank levels.
     */
    private void computeControls() throws ENException {
        SimulationControl.stepActions(logger, fMap, pMap, nControls, Htime);
    }

    /**
     * Computes demands at nodes during current time period.
     */
    private void computeDemands() throws ENException {
        // Determine total elapsed number of pattern periods
        long p = (Htime + pMap.getPstart()) / pMap.getPstep();

        Dsystem = 0.0; //System-wide demand

        // Update demand at each node according to its assigned pattern
        for (SimulationNode node : nJunctions) {
            double sum = 0.0;
            for (Demand demand : node.getDemand()) {
                // pattern period (k) = (elapsed periods) modulus (periods per pattern)
                List<Double> factors = demand.getPattern().getFactorsList();

                long k = p % (long) factors.size();
                double djunc = (demand.getBase()) * factors.get((int) k) * pMap.getDmult();
                if (djunc > 0.0)
                    Dsystem += djunc;

                sum += djunc;
            }
            node.setSimDemand(sum);
        }

        // Update head at fixed grade nodes with time patterns
        for (SimulationTank tank : nTanks) {
            if (tank.getArea() == 0.0) {
                Pattern pat = tank.getPattern();
                if (pat != null) {
                    List<Double> factors = pat.getFactorsList();
                    long k = p % (long) factors.size();

                    tank.setSimHead(tank.getElevation() * factors.get((int) k));
                }
            }
        }

        // Update status of pumps with utilization patterns
        for (SimulationPump pump : nPumps) {
            if (pump.getUpat() != null) {
                List<Double> factors = pump.getUpat().getFactorsList();
                long k = p % (long) factors.size();
                pump.setLinkSetting(factors.get((int) k));
            }
        }
    }

    /**
     * Finds length of next time step & updates tank levels and rule-based contol actions.
     */
    protected long nextHyd() throws ENException, IOException {
        long hydstep = 0;

        if (simulationOutput != null)
            AwareStep.write(simulationOutput, this, Htime);

        if (Htime < pMap.getDuration())
            hydstep = timeStep();

        if (pMap.getDuration() == 0)
            SimulationPump.stepEnergy(pMap, fMap, Epat, nPumps, Htime, 0);
        else if (Htime < pMap.getDuration())
            SimulationPump.stepEnergy(pMap, fMap, Epat, nPumps, Htime, hydstep);

        if (Htime < pMap.getDuration()) {
            Htime += hydstep;
            if (Htime >= Rtime)
                Rtime += pMap.getRstep();
        }

        return hydstep;
    }

    /**
     * Computes time step to advance hydraulic simulation.
     */
    long timeStep() throws ENException {
        long tstep = pMap.getHstep();

        long n = ((Htime + pMap.getPstart()) / pMap.getPstep()) + 1;
        long t = n * pMap.getPstep() - Htime;

        if (t > 0 && t < tstep)
            tstep = t;

        // Revise time step based on smallest time to fill or drain a tank
        t = Rtime - Htime;
        if (t > 0 && t < tstep) tstep = t;

        tstep = SimulationTank.minimumTimeStep(nTanks, tstep);
        tstep = SimulationControl.minimumTimeStep(fMap, pMap, nControls, Htime, tstep);

        if (nRules.size() > 0) {
            SimulationRule.Result res = SimulationRule.minimumTimeStep(fMap, pMap, logger, nRules, nTanks, Htime, tstep, Dsystem);
            tstep = res.step;
            Htime = res.htime;
        } else
            SimulationTank.stepWaterLevels(nTanks, fMap, tstep);

        return (tstep);
    }


    /**
     * Save current step simulation results in the temp hydfile.
     */
    private void saveStep() throws ENException {

        ByteBuffer bb = ByteBuffer.allocate((nLinks.size() * 3 * Float.SIZE + nNodes.size() * 2 * Float.SIZE + Integer.SIZE) / 8);

        try {
            bb.putInt((int) Htime);

            for (SimulationNode node : nNodes)
                bb.putFloat((float) node.getSimDemand());

            for (SimulationNode node : nNodes)
                bb.putFloat((float) node.getSimHead());

            for (SimulationLink link : nLinks)
                if (link.getSimStatus().id <= StatType.CLOSED.id)
                    bb.putFloat((float) 0.0);

                else
                    bb.putFloat((float) link.getSimFlow());

            for (SimulationLink link : nLinks)
                bb.putFloat(link.getSimStatus().id);

            for (SimulationLink link : nLinks)
                bb.putFloat((float) link.getSimSetting());
            bb.flip();
            simulationOutput.write(bb.array());

        } catch (IOException ex) {
            throw new ENException(308);
        }
    }


    // Report hydraulic warning.
    // Note: Warning conditions checked in following order:
    //  1. System balanced but unstable
    //  2. Negative pressures
    //  3. FCV cannot supply flow or PRV/PSV cannot maintain pressure
    //  4. Pump out of range
    //  5. Network disconnected
    //  6. System unbalanced
    private void logHydWarn(NetSolveStep nss) {
        try {
            int flag;

            String atime = Utilities.getClockTime(Htime);

            if (nss.iter > pMap.getMaxIter() && nss.relerr <= pMap.getHacc()) {
                if (pMap.getMessageflag())
                    logger.warning(String.format(Utilities.getError("WARN02"), atime));
                flag = 2;
            }

            // Check for negative pressures
            for (SimulationNode node : nJunctions) {
                if (node.getSimHead() < node.getElevation() && node.getSimDemand() > 0.0) {
                    if (pMap.getMessageflag())
                        logger.warning(String.format(Utilities.getError("WARN06"), atime));
                    flag = 6;
                    break;
                }
            }

            // Check for abnormal valve condition
            for (SimulationValve valve : nValves) {
                int j = valve.getIndex();
                if (valve.getSimStatus().id >= StatType.XFCV.id) {
                    if (pMap.getMessageflag())
                        logger.warning(String.format(Utilities.getError("WARN05"), valve.getType().parseStr, valve.getLink().getId(),
                                valve.getSimStatus().reportStr, atime));
                    flag = 5;
                }
            }

            // Check for abnormal pump condition
            for (SimulationPump pump : nPumps) {
                StatType s = pump.getSimStatus();
                if (pump.getSimStatus().id >= StatType.OPEN.id) {
                    if (pump.getSimFlow() > pump.getSimSetting() * pump.getQmax())
                        s = StatType.XFLOW;
                    if (pump.getSimFlow() < 0.0)
                        s = StatType.XHEAD;
                }

                if (s == StatType.XHEAD || s == StatType.XFLOW) {
                    if (pMap.getMessageflag())
                        logger.warning(String.format(Utilities.getError("WARN04"), pump.getLink().getId(), pump.getSimStatus().reportStr, atime));
                    flag = 4;
                }
            }

            // Check if system is unbalanced
            if (nss.iter > pMap.getMaxIter() && nss.relerr > pMap.getHacc()) {
                String str = String.format(Utilities.getError("WARN01"), atime);

                if (pMap.getExtraIter() == -1)
                    str += Keywords.t_HALTED;

                if (pMap.getMessageflag())
                    logger.warning(str);

                flag = 1;
            }
        } catch (ENException e) {
        }
    }

    // Report hydraulic status.
    private void logHydStat(NetSolveStep nss) {
        try {
            String atime = Utilities.getClockTime(Htime);
            if (nss.iter > 0) {
                if (nss.relerr <= pMap.getHacc())
                    logger.warning(String.format(Utilities.getText("FMT58"), atime, nss.iter));
                else
                    logger.warning(String.format(Utilities.getText("FMT59"), atime, nss.iter, nss.relerr));
            }

            for (SimulationTank tank : nTanks) {
                StatType newstat;

                if (Math.abs(tank.getSimDemand()) < 0.001)
                    newstat = StatType.CLOSED;
                else if (tank.getSimDemand() > 0.0)
                    newstat = StatType.FILLING;
                else if (tank.getSimDemand() < 0.0)
                    newstat = StatType.EMPTYING;
                else
                    newstat = tank.getOldStat();

                if (newstat != tank.getOldStat()) {
                    if (!tank.isReservoir())
                        logger.warning(String.format(Utilities.getText("FMT50"), atime, tank.getId(), newstat.reportStr,
                                (tank.getSimHead() - tank.getElevation()) * fMap.getUnits(Type.HEAD), fMap.getField(Type.HEAD).getUnits()));
                    else
                        logger.warning(String.format(Utilities.getText("FMT51"), atime, tank.getId(), newstat.reportStr));

                    tank.setOldStat(newstat);
                }
            }

            for (SimulationLink link : nLinks) {
                if (link.getSimStatus() != link.getSimOldStatus()) {
                    if (Htime == 0)
                        logger.warning(String.format(Utilities.getText("FMT52"),
                                atime,
                                link.getType().parseStr,
                                link.getLink().getId(),
                                link.getSimStatus().reportStr));
                    else
                        logger.warning(String.format(Utilities.getText("FMT53"), atime,
                                link.getType().parseStr,
                                link.getLink().getId(),
                                link.getSimOldStatus().reportStr,
                                link.getSimStatus().reportStr));
                    link.setSimOldStatus(link.getSimStatus());
                }
            }
        } catch (ENException e) {
        }
    }


    private void logRelErr(NetSolveStep ret) {
        if (ret.iter == 0) {
            logger.warning(String.format(Utilities.getText("FMT64"), Utilities.getClockTime(Htime)));
        } else {
            logger.warning(String.format(Utilities.getText("FMT65"), ret.iter, ret.relerr));
        }
    }

    private void logHydErr(int order) {
        try {
            if (pMap.getMessageflag())
                logger.warning(String.format(Utilities.getText("FMT62"),
                        Utilities.getClockTime(Htime), nNodes.get(order).getId()));
        } catch (ENException e) {
        }
        logHydStat(new NetSolveStep(0, 0));
    }

    public List<SimulationNode> getnNodes() {
        return nNodes;
    }

    public List<SimulationLink> getnLinks() {
        return nLinks;
    }

    public List<SimulationRule> getnRules() {
        return nRules;
    }

    public List<SimulationControl> getnControls() {
        return nControls;
    }

}
