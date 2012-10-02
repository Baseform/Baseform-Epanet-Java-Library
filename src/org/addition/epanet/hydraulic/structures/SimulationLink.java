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

package org.addition.epanet.hydraulic.structures;

import org.addition.epanet.Constants;
import org.addition.epanet.hydraulic.SparseMatrix;
import org.addition.epanet.hydraulic.models.PipeHeadModel;
import org.addition.epanet.network.FieldsMap;
import org.addition.epanet.network.FieldsMap.Type;
import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.network.structures.Curve;
import org.addition.epanet.network.structures.Link;
import org.addition.epanet.network.structures.Link.LinkType;
import org.addition.epanet.network.structures.Link.StatType;
import org.addition.epanet.network.structures.Pump;
import org.addition.epanet.network.structures.Valve;
import org.addition.epanet.util.ENException;
import org.addition.epanet.util.Utilities;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SimulationLink {


    protected SimulationNode first = null;
    protected SimulationNode second = null;
    protected final Link link;
    protected final int index;

    protected StatType status;         // Epanet 'S[k]', link current status
    protected double flow;           // Epanet 'Q[k]', link flow value
    protected double invHeadLoss;    // Epanet 'P[k]', Inverse headloss derivatives
    protected double flowCorrection; // Epanet 'Y[k]', Flow correction factors
    protected double setting;        // Epanet 'K[k]', Link setting
    protected StatType oldStatus;


    public static SimulationLink createIndexedLink(Map<String, SimulationNode> byId, Link ref, int idx) {
        SimulationLink ret = null;
        if (ref instanceof Valve)
            ret = new SimulationValve(byId.values(), ref, idx);
        else if (ref instanceof Pump)
            ret = new SimulationPump(byId.values(), ref, idx);
        else
            ret = new SimulationLink(byId, ref, idx);

        return ret;
    }

    public static SimulationLink createIndexedLink(List<SimulationNode> indexedNodes, Link ref, int idx) {
        SimulationLink ret = null;
        if (ref instanceof Valve)
            ret = new SimulationValve(indexedNodes, ref, idx);
        else if (ref instanceof Pump)
            ret = new SimulationPump(indexedNodes, ref, idx);
        else
            ret = new SimulationLink(indexedNodes, ref, idx);

        return ret;
    }

    public SimulationLink(Map<String, SimulationNode> byId, Link ref, int idx) {

        link = ref;
        first = byId.get(link.getFirst().getId());
        second = byId.get(link.getSecond().getId());
        this.index = idx;

        // Init
        setting = link.getRoughness();
        status = link.getStat();

    }

    public SimulationLink(Collection<SimulationNode> indexedNodes, Link ref, int idx) {
        link = ref;

        for (SimulationNode indexedNode : indexedNodes) {
            if (indexedNode.getId().equals(link.getFirst().getId()))
                first = indexedNode;
            else if (indexedNode.getId().equals(link.getSecond().getId()))
                second = indexedNode;
            if (first != null && second != null) break;
        }
        this.index = idx;

        // Init
        setting = link.getRoughness();
        status = link.getStat();
    }

    // Indexed link methods

    public SimulationNode getFirst() {
        return first;
    }

    public SimulationNode getSecond() {
        return second;
    }

    public Link getLink() {
        return link;
    }

    public int getIndex() {
        return index;
    }

    // Network link Getters

    public double[] getC0() {
        return link.getC0();
    }

    //public double[] getParam() {
    //    return node.getParam();
    //}

    public double getDiameter() {
        return link.getDiameter();
    }

    //public double getLenght() {
    //    return node.getLenght();
    //}

    public double getRoughness() {
        return link.getRoughness();
    }

    public double getKm() {
        return link.getKm();
    }

    //public double getKb() {
    //    return node.getKb();
    //}
    //
    //public double getKw() {
    //    return node.getKw();
    //}

    public double getFlowResistance() {
        return link.getFlowResistance();
    }

    public LinkType getType() {
        return link.getType();
    }

    //public StatType getStat() {
    //    return node.getStat();
    //}
    //
    //public boolean getRptFlag() {
    //    return node.isRptFlag();
    //}


    // Simulation getters & setters

    public StatType getSimStatus() {
        return status;
    }

    public void setSimStatus(StatType type) {
        status = type;
    }

    public double getSimFlow() {
        return flow;
    }

    public void setSimFlow(double flow) {
        this.flow = flow;
    }

    public double getSimSetting() {
        return setting;
    }

    public void setSimSetting(double value) {
        setting = value;
    }

    public double getSimInvHeadLoss() {
        return invHeadLoss;
    }

    public void setSimInvHeadLoss(double value) {
        invHeadLoss = value;
    }

    public double getSimFlowCorrection() {
        return flowCorrection;
    }

    public void setSimFlowCorrection(double value) {
        flowCorrection = value;
    }

    public StatType getSimOldStatus() {
        return oldStatus;
    }

    public void setSimOldStatus(StatType oldStatus) {
        this.oldStatus = oldStatus;
    }

    // Simulation Methods

    // Sets link status to OPEN(true) or CLOSED(false)
    void setLinkStatus(boolean value) {
        if (value) {
            if (this instanceof SimulationPump)
                setting = 1.0;

            else if (getType() != LinkType.GPV)
                setting = Constants.MISSING;

            status = StatType.OPEN;
        } else {
            if (this instanceof SimulationPump)
                setting = 0.0;
            else if (getType() != LinkType.GPV)
                setting = Constants.MISSING;

            status = StatType.CLOSED;
        }
    }

    // Sets pump speed or valve setting, adjusting link status and flow when necessary
    public void setLinkSetting(double value) {
        if (this instanceof SimulationPump) {
            setting = value;
            if (value > 0 && status.id <= StatType.CLOSED.id)
                status = StatType.OPEN;

            if (value == 0 && status.id > StatType.CLOSED.id)
                status = StatType.CLOSED;

        } else if (getType() == LinkType.FCV) {
            setting = value;
            status = StatType.ACTIVE;
        } else {
            if (setting == Constants.MISSING && status.id <= StatType.CLOSED.id)
                status = StatType.OPEN;

            setting = value;
        }
    }

    // Sets initial flow in link to QZERO if link is closed, to design flow for a pump,
    // or to flow at velocity of 1 fps for other links.
    public void initLinkFlow() {
        if (getSimStatus() == StatType.CLOSED)
            flow = Constants.QZERO;
        else if (this instanceof SimulationPump)
            flow = getRoughness() * ((SimulationPump) this).getQ0();
        else
            flow = Constants.PI * Math.pow(getDiameter(), 2) / 4.0;
    }

    public void initLinkFlow(StatType type, double Kc) {
        if (type == StatType.CLOSED)
            flow = Constants.QZERO;
        else if (this instanceof SimulationPump)
            flow = Kc * ((SimulationPump) this).getQ0();
        else
            flow = Constants.PI * Math.pow(getDiameter(), 2) / 4.0;
    }

//    public static long T1 = 0, T2 = 0, T3 = 0; //TODO:REMOVE THIS

    // Compute P, Y and matrix coeffs
    private void computeMatrixCoeff(final FieldsMap fMap,
                                    final PropertiesMap pMap,
                                    final PipeHeadModel hlModel,
                                    final Curve[] curves,
                                    final SparseMatrix smat, final LSVariables ls) throws ENException {

        switch (getType()) {
            // Pipes
            case CV:
            case PIPE:
                computePipeCoeff(pMap, hlModel);
                break;
            // Pumps
            case PUMP:
                ((SimulationPump) this).computePumpCoeff(fMap, pMap);
                break;
            // Valves
            case PBV:
            case TCV:
            case GPV:
            case FCV:
            case PRV:
            case PSV:
                // If valve status fixed then treat as pipe
                // otherwise ignore the valve for now.
                if (!((SimulationValve) this).computeValveCoeff(fMap, pMap, curves))
                    return;
                break;
            default:
                return;
        }

        int n1 = first.getIndex();
        int n2 = second.getIndex();

        ls.addNodalInFlow(n1, -flow);
        ls.addNodalInFlow(n2, +flow);

        ls.addAij(smat.getNdx(getIndex()), -invHeadLoss);

        if (!(first instanceof SimulationTank)) {
            ls.addAii(smat.getRow(n1), +invHeadLoss);
            ls.addRHSCoeff(smat.getRow(n1), +flowCorrection);
        } else
            ls.addRHSCoeff(smat.getRow(n2), +(invHeadLoss * first.getSimHead()));

        if (!(second instanceof SimulationTank)) {
            ls.addAii(smat.getRow(n2), +invHeadLoss);
            ls.addRHSCoeff(smat.getRow(n2), -flowCorrection);
        } else
            ls.addRHSCoeff(smat.getRow(n1), +(invHeadLoss * second.getSimHead()));

    }

    // Computes P & Y coefficients for pipe k
    private void computePipeCoeff(PropertiesMap pMap, PipeHeadModel hlModel) throws ENException {
        // For closed pipe use headloss formula: h = CBIG*q
        if (status.id <= StatType.CLOSED.id) {
            invHeadLoss = 1.0 / Constants.CBIG;
            flowCorrection = flow;
            return;
        }

        PipeHeadModel.LinkCoeffs coeffs = hlModel.compute(pMap, this);
        invHeadLoss = coeffs.getInvHeadLoss();
        flowCorrection = coeffs.getFlowCorrection();
    }


    // Closes link flowing into full or out of empty tank
    private void tankStatus(PropertiesMap pMap) throws ENException {
        double q = flow;
        SimulationNode n1 = getFirst();
        SimulationNode n2 = getSecond();

        // Make node n1 be the tank
        if (!(n1 instanceof SimulationTank)) {
            if (!(n2 instanceof SimulationTank))
                return;                      // neither n1 or n2 is a tank
            // N2 is a tank, swap !
            SimulationNode n = n1;
            n1 = n2;
            n2 = n;
            q = -q;
        }

        double h = n1.getSimHead() - n2.getSimHead();

        SimulationTank tank = (SimulationTank) n1;

        // Skip reservoirs & closed links
        if (tank.getArea() == 0.0 || status.id <= StatType.CLOSED.id)
            return;

        // If tank full, then prevent flow into it
        if (tank.getSimHead() >= tank.getHmax() - pMap.getHtol()) {
            //Case 1: Link is a pump discharging into tank
            if (getType() == LinkType.PUMP) {
                if (getSecond() == n1)
                    status = StatType.TEMPCLOSED;
            } else if (cvStatus(pMap, StatType.OPEN, h, q) == StatType.CLOSED) //  Case 2: Downstream head > tank head
                status = StatType.TEMPCLOSED;
        }

        // If tank empty, then prevent flow out of it
        if (tank.getSimHead() <= tank.getHmin() + pMap.getHtol()) {
            // Case 1: Link is a pump discharging from tank
            if (getType() == LinkType.PUMP) {
                if (getFirst() == n1)
                    status = StatType.TEMPCLOSED;
            }
            // Case 2: Tank head > downstream head
            else if (cvStatus(pMap, StatType.CLOSED, h, q) == StatType.OPEN)
                status = StatType.TEMPCLOSED;
        }
    }

    // Updates status of a check valve.
    private static StatType cvStatus(PropertiesMap pMap, StatType s, double dh, double q) throws ENException {
        if (Math.abs(dh) > pMap.getHtol()) {
            if (dh < -pMap.getHtol())
                return (StatType.CLOSED);
            else if (q < -pMap.getQtol())
                return (StatType.CLOSED);
            else
                return (StatType.OPEN);
        } else {
            if (q < -pMap.getQtol())
                return (StatType.CLOSED);
            else
                return (s);
        }
    }

    // Determines new status for pumps, CVs, FCVs & pipes to tanks.
    private boolean linkStatus(PropertiesMap pMap, FieldsMap fMap, Logger log) throws ENException {
        boolean change = false;

        double dh = first.getSimHead() - second.getSimHead();

        StatType tStatus = status;

        if (tStatus == StatType.XHEAD || tStatus == StatType.TEMPCLOSED)
            status = StatType.OPEN;

        if (getType() == LinkType.CV)
            status = cvStatus(pMap, status, dh, flow);

        if (this instanceof SimulationPump && status.id >= StatType.OPEN.id && setting > 0.0)
            status = ((SimulationPump) this).pumpStatus(pMap, -dh);

        if (getType() == LinkType.FCV && setting != Constants.MISSING)
            status = ((SimulationValve) this).fcvStatus(pMap, tStatus);

        if (first instanceof SimulationTank || second instanceof SimulationTank)
            tankStatus(pMap);

        if (tStatus != status) {
            change = true;
            if (pMap.getStatflag() == PropertiesMap.StatFlag.FULL)
                logStatChange(fMap, log, this, tStatus, status);
        }

        return (change);
    }

    protected static void logStatChange(FieldsMap fMap, Logger logger, SimulationLink link, StatType s1, StatType s2) throws ENException {

        if (s1 == s2) {

            switch (link.getType()) {
                case PRV:
                case PSV:
                case PBV:
                    link.setting *= fMap.getUnits(Type.PRESSURE);
                    break;
                case FCV:
                    link.setting *= fMap.getUnits(Type.FLOW);
            }
            logger.finest(String.format(Utilities.getText("FMT56"), link.getType().parseStr, link.getLink().getId(), link.setting));
            return;
        }

        StatType j1, j2;

        if (s1 == StatType.ACTIVE)
            j1 = StatType.ACTIVE;
        else if (s1.ordinal() <= StatType.CLOSED.ordinal())
            j1 = StatType.CLOSED;
        else
            j1 = StatType.OPEN;
        if (s2 == StatType.ACTIVE) j2 = StatType.ACTIVE;
        else if (s2.ordinal() <= StatType.CLOSED.ordinal())
            j2 = StatType.CLOSED;
        else
            j2 = StatType.OPEN;

        if (j1 != j2) {
            logger.finest(String.format(Utilities.getText("FMT57"), link.getType().parseStr, link.getLink().getId(), j1.reportStr, j2.reportStr));
        }

    }

    // Determines new status for pumps, CVs, FCVs & pipes to tanks.
    public static boolean linkStatus(PropertiesMap pMap, FieldsMap fMap, Logger log, List<SimulationLink> links) throws ENException {
        boolean change = false;
        for (SimulationLink link : links) {
            if (link.linkStatus(pMap, fMap, log))
                change = true;
        }
        return change;
    }

    // Computes solution matrix coefficients for links
    public static void computeMatrixCoeffs(final FieldsMap fMap,
                                           final PropertiesMap pMap,
                                           final PipeHeadModel hlModel,
                                           final List<SimulationLink> links, final Curve[] curves,
                                           final SparseMatrix smat, final LSVariables ls) throws ENException {

        for (final SimulationLink link : links) {
            link.computeMatrixCoeff(fMap, pMap, hlModel, curves, smat, ls);
        }
    }
}
