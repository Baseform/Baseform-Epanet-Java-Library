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

package org.addition.epanet.network.io.input;

import org.addition.epanet.Constants;
import org.addition.epanet.util.ENException;
import org.addition.epanet.network.FieldsMap;
import org.addition.epanet.network.FieldsMap.*;
import org.addition.epanet.network.Network;
import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.util.ENLevels;
import org.addition.epanet.util.Utilities;
import org.addition.epanet.network.structures.Link.*;

import java.util.logging.Logger;

import org.addition.epanet.network.structures.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract input file parser.
 */
public abstract class InputParser {


    /**
     * Reference to the error logger.
     */
    protected Logger log;

    protected InputParser(Logger log) {
        this.log = log;
    }

    public static InputParser create(Network.FileType type, Logger log) {
        switch (type) {
            case INP_FILE:
                return new InpParser(log);
            case EXCEL_FILE:
                return new ExcelParser(log);
            case XML_FILE:
                return new XMLParser(log,false);
            case XML_GZ_FILE:
                return new XMLParser(log,true);
            case NULL_FILE:
                return new NullParser(log);
        }
        return null;
    }

    public abstract Network parse(Network net, File f) throws ENException;

    /**
     * Prepare the hydraulic network for simulation.
     * @param net
     * @throws ENException
     */
    protected void convert(Network net) throws ENException {
        initTanks(net);
        initPumps(net);
        initPatterns(net);
        checkUnlinked(net);
        convertUnits(net);
    }

    /**
     * Adjust simulation configurations.
     * @param net
     * @throws ENException
     */
    public static void adjust(Network net) throws ENException {
        adjustData(net);
    }

    /**
     * Adjust simulation configurations.
     * @param net
     * @throws ENException
     */
    private static void adjustData(Network net) throws ENException {
        PropertiesMap m = net.getPropertiesMap();
        int i;
        double ucf;

        if (m.getPstep() <= 0) m.setPstep(3600);
        if (m.getRstep() == 0) m.setRstep(m.getPstep());
        if (m.getHstep() <= 0) m.setHstep(3600);
        if (m.getHstep() > m.getPstep()) m.setHstep(m.getPstep());
        if (m.getHstep() > m.getRstep()) m.setHstep(m.getRstep());
        if (m.getRstart() > m.getDuration()) m.setRstart(0);
        if (m.getDuration() == 0) m.setQualflag(PropertiesMap.QualType.NONE);
        if (m.getQstep() == 0) m.setQstep(m.getHstep() / 10);
        if (m.getRulestep() == 0) m.setRulestep(m.getHstep() / 10);

        m.setRulestep(Math.min(m.getRulestep(), m.getHstep()));
        m.setQstep(Math.min(m.getQstep(), m.getHstep()));

        if (m.getCtol() == Constants.MISSING) {
            if (m.getQualflag() == PropertiesMap.QualType.AGE)
                m.setCtol(Constants.AGETOL);
            else
                m.setCtol(Constants.CHEMTOL);
        }

        switch (m.getFlowflag()) {
            case LPS:
            case LPM:
            case MLD:
            case CMH:
            case CMD:
                m.setUnitsflag(PropertiesMap.UnitsType.SI);
                break;
            default:
                m.setUnitsflag(PropertiesMap.UnitsType.US);
        }


        if (m.getUnitsflag() != PropertiesMap.UnitsType.SI)
            m.setPressflag(PropertiesMap.PressUnitsType.PSI);
        else if (m.getPressflag() == PropertiesMap.PressUnitsType.PSI)
            m.setPressflag(PropertiesMap.PressUnitsType.METERS);

        ucf = 1.0;
        if (m.getUnitsflag() == PropertiesMap.UnitsType.SI)
            ucf = Math.pow(Constants.MperFT, 2);

        if (m.getViscos() == Constants.MISSING)
            m.setViscos(Constants.VISCOS);
        else if (m.getViscos() > 1.e-3)
            m.setViscos(m.getViscos() * Constants.VISCOS);
        else
            m.setViscos(m.getViscos() / ucf);

        if (m.getDiffus() == Constants.MISSING)
            m.setDiffus(Constants.DIFFUS);
        else if (m.getDiffus() > 1.e-4)
            m.setDiffus(m.getDiffus() * Constants.DIFFUS);
        else
            m.setDiffus(m.getDiffus() / ucf);

        if (m.getFormflag() == PropertiesMap.FormType.HW)
            m.setHexp(1.852);
        else
            m.setHexp(2.0);

        double Rfactor = m.getRfactor();
        PropertiesMap.FormType formFlag = m.getFormflag();
        double Kbulk = m.getKbulk();

        for (Link link : net.getLinks()) {
            if (link.getType().id > LinkType.PIPE.id)
                continue;

            if (link.getKb() == Constants.MISSING)
                link.setKb(Kbulk);

            if (link.getKw() == Constants.MISSING)
            {
                if (Rfactor == 0.0)
                    link.setKw(m.getKwall());
                else if ((link.getRoughness() > 0.0) && (link.getDiameter() > 0.0)) {
                    if (formFlag == PropertiesMap.FormType.HW)
                        link.setKw(Rfactor / link.getRoughness());
                    if (formFlag == PropertiesMap.FormType.DW)
                        link.setKw(Rfactor / Math.abs(Math.log(link.getRoughness() / link.getDiameter())));
                    if (formFlag == PropertiesMap.FormType.CM)
                        link.setKw(Rfactor * link.getRoughness());
                }
                else
                    link.setKw(0.0);
            }
        }

        for (Tank tank : net.getTanks())
            if (tank.getKb() == Constants.MISSING)
                tank.setKb(Kbulk);

        Pattern defpat = net.getPattern(m.getDefPatId());
        if (defpat == null)
            defpat = net.getPattern("");

        if (defpat == null)
            defpat = net.getPattern("");

        for (Node node : net.getNodes()) {
            for (Demand d : node.getDemand()) {
                if (d.getPattern() == null)
                    d.setPattern(defpat);
            }
        }

        if (m.getQualflag() == PropertiesMap.QualType.NONE)
            net.getFieldsMap().getField(Type.QUALITY).setEnabled(false);

    }

    /**
     * Initialize tank properties.
     * @param net Hydraulic network reference.
     * @throws ENException
     */
    private void initTanks(Network net) throws ENException {
        int n = 0;
        double a;

        for (Tank tank : net.getTanks()) {
            if (tank.getArea() == 0.0)
                continue;

            int levelerr = 0;
            if (tank.getH0() > tank.getHmax() ||
                    tank.getHmin() > tank.getHmax() ||
                    tank.getH0() < tank.getHmin()
                    ) levelerr = 1;

            Curve curv = tank.getVcurve();

            if (curv != null) {
                n = curv.getNpts() - 1;
                if (tank.getHmin() < curv.getX().get(0) ||
                        tank.getHmax() > curv.getX().get(n))
                    levelerr = 1;
            }

            if (levelerr != 0) {
                throw new ENException(225, tank.getId());
            } else if (curv != null) {

                tank.setVmin(Utilities.linearInterpolator(curv.getNpts(), curv.getX(),
                        curv.getY(), tank.getHmin()));
                tank.setVmax(Utilities.linearInterpolator(curv.getNpts(), curv.getX(),
                        curv.getY(), tank.getHmax()));
                tank.setV0(Utilities.linearInterpolator(curv.getNpts(), curv.getX(),
                        curv.getY(), tank.getH0()));

                a = (curv.getY().get(n) - curv.getY().get(0)) /
                        (curv.getX().get(n) - curv.getX().get(0));
                tank.setArea(Math.sqrt(4.0 * a / Constants.PI));
            }
        }
    }

    /**
     * Convert hydraulic structures values from user units to simulation system units.
     * @param net Hydraulic network reference.
     * @throws ENException
     */
    private void convertUnits(Network net) throws ENException {
        int i, j, k;
        double ucf;

        FieldsMap fMap = net.getFieldsMap();
        PropertiesMap pMap = net.getPropertiesMap();

        for (Node node : net.getNodes()) {
            node.setElevation(node.getElevation() / fMap.getUnits(Type.ELEV));
            node.setC0(new double[]{node.getC0()[0] / fMap.getUnits(Type.QUALITY)});
        }


        for (Node node : net.getNodes()) {
            if (node instanceof Tank)
                continue;

            for (Demand d : node.getDemand()) {
                d.setBase(d.getBase() / fMap.getUnits(Type.DEMAND));
            }
        }


        ucf = Math.pow(fMap.getUnits(Type.FLOW), pMap.getQexp()) / fMap.getUnits(Type.PRESSURE);

        for (Node node : net.getNodes()) {
            if (node instanceof Tank)
                continue;

            if (node.getKe() > 0.0)
                node.setKe(ucf / Math.pow(node.getKe(), pMap.getQexp()));
        }

        for (Tank tk : net.getTanks()) {
            tk.setH0(tk.getElevation() + tk.getH0() / fMap.getUnits(Type.ELEV));
            tk.setHmin(tk.getElevation() + tk.getHmin() / fMap.getUnits(Type.ELEV));
            tk.setHmax(tk.getElevation() + tk.getHmax() / fMap.getUnits(Type.ELEV));
            tk.setArea(Constants.PI * Math.pow(tk.getArea() / fMap.getUnits(Type.ELEV), 2) / 4.0);
            tk.setV0(tk.getV0() / fMap.getUnits(Type.VOLUME));
            tk.setVmin(tk.getVmin() / fMap.getUnits(Type.VOLUME));
            tk.setVmax(tk.getVmax() / fMap.getUnits(Type.VOLUME));
            tk.setKb(tk.getKb() / Constants.SECperDAY);
            //tk.setVolume(tk.getV0());
            tk.setConcentration(tk.getC0());
            tk.setV1max(tk.getV1max() * tk.getVmax());
        }


        pMap.setClimit(pMap.getClimit() / fMap.getUnits(Type.QUALITY));
        pMap.setCtol(pMap.getCtol() / fMap.getUnits(Type.QUALITY));

        pMap.setKbulk(pMap.getKbulk() / Constants.SECperDAY);
        pMap.setKwall(pMap.getKwall() / Constants.SECperDAY);


        for (Link lk : net.getLinks()) {

            if (lk.getType().id <= LinkType.PIPE.id) {
                if (pMap.getFormflag() == PropertiesMap.FormType.DW)
                    lk.setRoughness(lk.getRoughness() / (1000.0 * fMap.getUnits(Type.ELEV)));
                lk.setDiameter(lk.getDiameter() / fMap.getUnits(Type.DIAM));
                lk.setLenght(lk.getLenght() / fMap.getUnits(Type.LENGTH));

                lk.setKm(0.02517 * lk.getKm() / Math.pow(lk.getDiameter(), 2) / Math.pow(lk.getDiameter(), 2));

                lk.setKb(lk.getKb() / Constants.SECperDAY);
                lk.setKw(lk.getKw() / Constants.SECperDAY);
            } else if (lk instanceof Pump) {
                Pump pump = (Pump) lk;

                if (pump.getPtype().equals(Pump.Type.CONST_HP)) {
                    if (pMap.getUnitsflag() == PropertiesMap.UnitsType.SI)
                        pump.setFlowCoefficient(pump.getFlowCoefficient() / fMap.getUnits(Type.POWER));
                } else {
                    if (pump.getPtype().equals(Pump.Type.POWER_FUNC)) {
                        pump.setH0(pump.getH0() / fMap.getUnits(Type.HEAD));
                        pump.setFlowCoefficient(pump.getFlowCoefficient() * (Math.pow(fMap.getUnits(Type.FLOW), pump.getN())) / fMap.getUnits(Type.HEAD));
                    }
                    pump.setQ0(pump.getQ0() / fMap.getUnits(Type.FLOW));
                    pump.setQmax(pump.getQmax() / fMap.getUnits(Type.FLOW));
                    pump.setHmax(pump.getHmax() / fMap.getUnits(Type.HEAD));
                }
            } else {
                lk.setDiameter(lk.getDiameter() / fMap.getUnits(Type.DIAM));
                lk.setKm(0.02517 * lk.getKm() / Math.pow(lk.getDiameter(), 2) / Math.pow(lk.getDiameter(), 2));
                if (lk.getRoughness() != Constants.MISSING)
                    switch (lk.getType()) {
                        case FCV:
                            lk.setRoughness(lk.getRoughness() / fMap.getUnits(Type.FLOW));
                            break;
                        case PRV:
                        case PSV:
                        case PBV:
                            lk.setRoughness(lk.getRoughness() / fMap.getUnits(Type.PRESSURE));
                            break;
                    }
            }

            lk.initResistance(net.getPropertiesMap().getFormflag(),net.getPropertiesMap().getHexp());
        }

        for (Control c_i : net.getControls()) {


            if (c_i.getLink() == null) continue;
            if (c_i.getNode() != null) {
                Node node = c_i.getNode();
                if (node instanceof Tank)
                    c_i.setGrade(node.getElevation() +
                            c_i.getGrade() / fMap.getUnits(Type.ELEV));
                else
                    c_i.setGrade(node.getElevation() + c_i.getGrade() / fMap.getUnits(Type.PRESSURE));
            }

            if (c_i.getSetting() != Constants.MISSING)
                switch (c_i.getLink().getType()) {
                    case PRV:
                    case PSV:
                    case PBV:
                        c_i.setSetting(c_i.getSetting() / fMap.getUnits(Type.PRESSURE));
                        break;
                    case FCV:
                        c_i.setSetting(c_i.getSetting() / fMap.getUnits(Type.FLOW));
                }
        }
    }



    /**
     * Initialize pump properties.
     * @param net Hydraulic network reference.
     * @throws ENException
     */
    private void initPumps(Network net) throws ENException {
        double h0 = 0.0, h1 = 0.0, h2 = 0.0, q1 = 0.0, q2 = 0.0;

        for (Pump pump : net.getPumps()) {
            // Constant Hp pump
            if (pump.getPtype() == Pump.Type.CONST_HP) {
                pump.setH0(0.0);
                pump.setFlowCoefficient(-8.814 * pump.getKm());
                pump.setN(-1.0);
                pump.setHmax(Constants.BIG);
                pump.setQmax(Constants.BIG);
                pump.setQ0(1.0);
                continue;
            }

            // Set parameters for pump curves
            else if (pump.getPtype() == Pump.Type.NOCURVE) {
                Curve curve = pump.getHcurve();
                if (curve == null) {
                    throw new ENException(226, pump.getId());
                }
                int n = curve.getNpts();
                if (n == 1) {
                    pump.setPtype(Pump.Type.POWER_FUNC);
                    q1 = curve.getX().get(0);
                    h1 = curve.getY().get(0);
                    h0 = 1.33334 * h1;
                    q2 = 2.0 * q1;
                    h2 = 0.0;
                } else if (n == 3 && curve.getX().get(0) == 0.0) {
                    pump.setPtype(Pump.Type.POWER_FUNC);
                    h0 = curve.getY().get(0);
                    q1 = curve.getX().get(1);
                    h1 = curve.getY().get(1);
                    q2 = curve.getX().get(2);
                    h2 = curve.getY().get(2);
                } else
                    pump.setPtype(Pump.Type.CUSTOM);

                // Compute shape factors & limits of power function pump curves
                if (pump.getPtype() == Pump.Type.POWER_FUNC) {
                    double[] a = new double[1], b = new double[1], c = new double[1];
                    if (!Utilities.getPowerCurve(h0, h1, h2, q1, q2, a, b, c))
                        throw new ENException(227, pump.getId());


                    pump.setH0(-a[0]);
                    pump.setFlowCoefficient(-b[0]);
                    pump.setN(c[0]);
                    pump.setQ0(q1);
                    pump.setQmax(Math.pow(-a[0] / b[0], (1.0 / c[0])));
                    pump.setHmax(h0);
                }
            }

            // Assign limits to custom pump curves
            if (pump.getPtype() == Pump.Type.CUSTOM) {
                Curve curve = pump.getHcurve();
                for (int m = 1; m < curve.getNpts(); m++) {
                    if (curve.getY().get(m) >= curve.getY().get(m - 1)) // Check for invalid curve
                    {
                        throw new ENException(227, pump.getId());
                    }
                }
                pump.setQmax(curve.getX().get(curve.getNpts() - 1));
                pump.setQ0((curve.getX().get(0) + pump.getQmax()) / 2.0);
                pump.setHmax(curve.getY().get(0));
            }
        }

    }

    /**
     * Initialize patterns.
     * @param net Hydraulic network reference.
     * @throws ENException
     */
    private void initPatterns(Network net) throws ENException {
        for (Pattern par : net.getPatterns()) {
            if (par.getFactorsList().size() == 0) {
                par.getFactorsList().add(1.0);
            }
        }
    }


    /**
     * Check for unlinked nodes.
     * @param net Hydraulic network reference.
     * @throws ENException
     */
    private void checkUnlinked(Network net) throws ENException {
        int[] marked = new int[net.getNodes().size() + 1];
        List<Link> links = new ArrayList<Link>(net.getLinks());
        List<Node> nodes = new ArrayList<Node>(net.getNodes());

        int err = 0;

        for (Link link : links) {
            marked[nodes.indexOf(link.getFirst())]++;
            marked[nodes.indexOf(link.getSecond())]++;
        }

        int i = 0;
        for (Node node : nodes) {
            if (marked[i] == 0) {
                err++;
                log.log(ENLevels.ERROR, "checkUnlinked", new ENException(233, node.getId()));
            }

            if (err >= Constants.MAXERRS)
                break;

            i++;
        }

//        if (err > 0)
//            throw new ENException(200);
    }
}
