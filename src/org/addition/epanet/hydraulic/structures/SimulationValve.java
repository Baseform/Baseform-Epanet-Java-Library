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
import org.addition.epanet.network.FieldsMap;
import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.network.structures.Curve;
import org.addition.epanet.network.structures.Link;
import org.addition.epanet.network.structures.Link.LinkType;
import org.addition.epanet.network.structures.Link.StatType;
import org.addition.epanet.util.ENException;
import org.addition.epanet.util.Utilities;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 */
public class SimulationValve extends SimulationLink {

    public SimulationValve(Collection<SimulationNode> indexedNodes, Link ref, int idx) {
        super(indexedNodes, ref, idx);
    }


    // Computes solution matrix coeffs. for a completely open, closed, or throttled control valve.
    protected void valveCoeff(PropertiesMap pMap) throws ENException {
        double p;

        // Valve is closed. Use a very small matrix coeff.
        if (status.id <= StatType.CLOSED.id) {
            invHeadLoss = 1.0 / Constants.CBIG;
            flowCorrection = flow;
            return;
        }

        // Account for any minor headloss through the valve
        if (getKm() > 0.0) {
            p = 2.0 * getKm() * Math.abs(flow);
            if (p < pMap.getRQtol())
                p = pMap.getRQtol();

            invHeadLoss = 1.0 / p;
            flowCorrection = flow / 2.0;
        } else {
            invHeadLoss = 1.0 / pMap.getRQtol();
            flowCorrection = flow;
        }
    }

    // Computes solution matrix coeffs. for a completely open, closed, or throttled control valve.
    private void valveCoeff(PropertiesMap pMap, double km) throws ENException {
        double p;

        // Valve is closed. Use a very small matrix coeff.
        if (status.id <= StatType.CLOSED.id) {
            invHeadLoss = 1.0 / Constants.CBIG;
            flowCorrection = flow;
            return;
        }

        // Account for any minor headloss through the valve
        if (km > 0.0) {
            p = 2.0 * km * Math.abs(flow);
            if (p < pMap.getRQtol())
                p = pMap.getRQtol();

            invHeadLoss = 1.0 / p;
            flowCorrection = flow / 2.0;
        } else {
            invHeadLoss = 1.0 / pMap.getRQtol();
            flowCorrection = flow;
        }
    }

    // Computes P & Y coeffs. for pressure breaker valve
    void pbvCoeff(PropertiesMap pMap) throws ENException {
        if (setting == Constants.MISSING || setting == 0.0)
            valveCoeff(pMap);
        else if (getKm() * (flow * flow) > setting)
            valveCoeff(pMap);
        else {
            invHeadLoss = Constants.CBIG;
            flowCorrection = setting * Constants.CBIG;
        }
    }

    // Computes P & Y coeffs. for throttle control valve
    void tcvCoeff(PropertiesMap pMap) throws ENException {
        double km = getKm();

        if (setting != Constants.MISSING)
            km = (0.02517 * setting / Math.pow(getDiameter(), 4));

        valveCoeff(pMap, km);
    }

    // Computes P & Y coeffs. for general purpose valve
    void gpvCoeff(FieldsMap fMap, PropertiesMap pMap, Curve[] curves) throws ENException {
        if (status == StatType.CLOSED)
            valveCoeff(pMap);
        else {
            double q = Math.max(Math.abs(flow), Constants.TINY);
            Curve.Coeffs coeffs = curves[(int) Math.round(setting)].getCoeff(fMap, q);
            invHeadLoss = 1.0 / Math.max(coeffs.r, pMap.getRQtol());
            flowCorrection = invHeadLoss * (coeffs.h0 + coeffs.r * q) * Utilities.getSignal(flow);
        }
    }

    // Updates status of a flow control valve.
    StatType fcvStatus(PropertiesMap pMap, StatType s) throws ENException {
        StatType status;
        status = s;
        if (getFirst().getSimHead() - getSecond().getSimHead() < -pMap.getHtol()) status = StatType.XFCV;
        else if (flow < -pMap.getQtol()) status = StatType.XFCV;
        else if (s == StatType.XFCV && flow >= setting) status = StatType.ACTIVE;
        return (status);
    }


    // Computes solution matrix coeffs. for pressure reducing valves
    void prvCoeff(PropertiesMap pMap, LSVariables ls, SparseMatrix smat) throws ENException {
        int k = getIndex();
        int i = smat.getRow(first.getIndex());
        int j = smat.getRow(second.getIndex());

        double hset = second.getElevation() + setting;

        if (status == StatType.ACTIVE) {

            invHeadLoss = 0.0;
            flowCorrection = flow + ls.getNodalInFlow(second);
            ls.addRHSCoeff(j, +(hset * Constants.CBIG));
            ls.addAii(j, +Constants.CBIG);
            if (ls.getNodalInFlow(second) < 0.0)
                ls.addRHSCoeff(i, +ls.getNodalInFlow(second));
            return;
        }

        valveCoeff(pMap);

        ls.addAij(smat.getNdx(k), -invHeadLoss);
        ls.addAii(i, +invHeadLoss);
        ls.addAii(j, +invHeadLoss);
        ls.addRHSCoeff(i, +(flowCorrection - flow));
        ls.addRHSCoeff(j, -(flowCorrection - flow));
    }


    // Computes solution matrix coeffs. for pressure sustaining valve
    void psvCoeff(PropertiesMap pMap, LSVariables ls, SparseMatrix smat) throws ENException {
        int k = getIndex();
        int i = smat.getRow(first.getIndex());
        int j = smat.getRow(second.getIndex());
        double hset = first.getElevation() + setting;

        if (status == StatType.ACTIVE) {
            invHeadLoss = 0.0;
            flowCorrection = flow - ls.getNodalInFlow(first);
            ls.addRHSCoeff(i, +(hset * Constants.CBIG));
            ls.addAii(i, +Constants.CBIG);
            if (ls.getNodalInFlow(first) > 0.0) ls.addRHSCoeff(j, +ls.getNodalInFlow(first));
            return;
        }

        valveCoeff(pMap);
        ls.addAij(smat.getNdx(k), -invHeadLoss);
        ls.addAii(i, +invHeadLoss);
        ls.addAii(j, +invHeadLoss);
        ls.addRHSCoeff(i, +(flowCorrection - flow));
        ls.addRHSCoeff(j, -(flowCorrection - flow));
    }

    // computes solution matrix coeffs. for flow control valve
    void fcvCoeff(PropertiesMap pMap, LSVariables ls, SparseMatrix smat) throws ENException {
        int k = getIndex();
        double q = setting;
        int i = smat.getRow(first.getIndex());
        int j = smat.getRow(second.getIndex());

        // If valve active, break network at valve and treat
        // flow setting as external demand at upstream node
        // and external supply at downstream node.
        if (status == StatType.ACTIVE) {
            ls.addNodalInFlow(first.getIndex(), -q);
            ls.addRHSCoeff(i, -q);
            ls.addNodalInFlow(second.getIndex(), +q);
            ls.addRHSCoeff(j, +q);
            invHeadLoss = 1.0 / Constants.CBIG;
            ls.addAij(smat.getNdx(k), -invHeadLoss);
            ls.addAii(i, +invHeadLoss);
            ls.addAii(j, +invHeadLoss);
            flowCorrection = flow - q;
        } else {
            //  Otherwise treat valve as an open pipe
            valveCoeff(pMap);
            ls.addAij(smat.getNdx(k), -invHeadLoss);
            ls.addAii(i, +invHeadLoss);
            ls.addAii(j, +invHeadLoss);
            ls.addRHSCoeff(i, +(flowCorrection - flow));
            ls.addRHSCoeff(j, -(flowCorrection - flow));
        }
    }

    // Determines if a node belongs to an active control valve
    // whose setting causes an inconsistent set of eqns. If so,
    // the valve status is fixed open and a warning condition
    // is generated.
    public static boolean checkBadValve(PropertiesMap pMap, Logger log, List<SimulationValve> valves, long Htime, int n) throws ENException {
        for (SimulationValve link : valves) {
            SimulationNode n1 = link.getFirst();
            SimulationNode n2 = link.getSecond();
            if (n == n1.getIndex() || n == n2.getIndex()) {
                if (link.getType() == LinkType.PRV || link.getType() == LinkType.PSV || link.getType() == LinkType.FCV) {
                    if (link.status == StatType.ACTIVE) {
                        if (pMap.getStatflag() == PropertiesMap.StatFlag.FULL) {
                            logBadValve(log, link, Htime);
                        }
                        if (link.getType() == LinkType.FCV)
                            link.status = StatType.XFCV;
                        else
                            link.status = StatType.XPRESSURE;
                        return true;
                    }
                }
                return false;
            }
        }

        return false;
    }

    private static void logBadValve(Logger log, SimulationLink link, long Htime) {
        log.warning(String.format(Utilities.getText("FMT61"), Utilities.getClockTime(Htime), link.getLink().getId()));
    }

    // Updates status of a pressure reducing valve.
    private StatType prvStatus(PropertiesMap pMap, double hset) throws ENException {
        if (setting == Constants.MISSING)
            return (status);

        double htol = pMap.getHtol();
        double hml = getKm() * (flow * flow);
        double h1 = first.getSimHead();
        double h2 = second.getSimHead();

        StatType tStatus = status;
        switch (status) {
            case ACTIVE:
                if (flow < -pMap.getQtol())
                    tStatus = StatType.CLOSED;
                else if (h1 - hml < hset - htol)
                    tStatus = StatType.OPEN;
                else
                    tStatus = StatType.ACTIVE;
                break;
            case OPEN:
                if (flow < -pMap.getQtol())
                    tStatus = StatType.CLOSED;
                else if (h2 >= hset + htol)
                    tStatus = StatType.ACTIVE;
                else
                    tStatus = StatType.OPEN;
                break;
            case CLOSED:
                if (h1 >= hset + htol && h2 < hset - htol)
                    tStatus = StatType.ACTIVE;
                else if (h1 < hset - htol && h1 > h2 + htol)
                    tStatus = StatType.OPEN;
                else
                    tStatus = StatType.CLOSED;
                break;
            case XPRESSURE:
                if (flow < -pMap.getQtol())
                    tStatus = StatType.CLOSED;
                break;
        }
        return (tStatus);
    }

    // Updates status of a pressure sustaining valve.
    private StatType psvStatus(PropertiesMap pMap, double hset) throws ENException {
        if (setting == Constants.MISSING)
            return (status);

        double h1 = first.getSimHead();
        double h2 = second.getSimHead();
        double htol = pMap.getHtol();
        double hml = getKm() * (flow * flow);
        StatType tStatus = status;
        switch (status) {
            case ACTIVE:
                if (flow < -pMap.getQtol())
                    tStatus = StatType.CLOSED;
                else if (h2 + hml > hset + htol)
                    tStatus = StatType.OPEN;
                else
                    tStatus = StatType.ACTIVE;
                break;
            case OPEN:
                if (flow < -pMap.getQtol())
                    tStatus = StatType.CLOSED;
                else if (h1 < hset - htol)
                    tStatus = StatType.ACTIVE;
                else
                    tStatus = StatType.OPEN;
                break;
            case CLOSED:
                if (h2 > hset + htol && h1 > h2 + htol)
                    tStatus = StatType.OPEN;
                else if (h1 >= hset + htol && h1 > h2 + htol)
                    tStatus = StatType.ACTIVE;
                else
                    tStatus = StatType.CLOSED;
                break;
            case XPRESSURE:
                if (flow < -pMap.getQtol())
                    tStatus = StatType.CLOSED;
                break;
        }
        return (tStatus);
    }

    // Compute P & Y coefficients for PBV,TCV,GPV valves
    public boolean computeValveCoeff(FieldsMap fMap, PropertiesMap pMap, Curve[] curves) throws ENException {
        switch (getType()) {
            case PBV:
                pbvCoeff(pMap);
                break;
            case TCV:
                tcvCoeff(pMap);
                break;
            case GPV:
                gpvCoeff(fMap, pMap, curves);
                break;
            case FCV:
            case PRV:
            case PSV:
                if (getSimSetting() == Constants.MISSING)
                    valveCoeff(pMap);
                else
                    return false;
                break;
        }
        return true;
    }

    // Updates status for PRVs & PSVs whose status is not fixed to OPEN/CLOSED
    public static boolean valveStatus(FieldsMap fMap, PropertiesMap pMap, Logger log, List<SimulationValve> valves) throws ENException {
        boolean change = false;

        for (SimulationValve v : valves) {

            if (v.setting == Constants.MISSING) continue;

            StatType s = v.status;

            switch (v.getType()) {
                case PRV: {
                    double hset = v.second.getElevation() + v.setting;
                    v.status = v.prvStatus(pMap, hset);
                    break;
                }
                case PSV: {
                    double hset = v.first.getElevation() + v.setting;
                    v.status = v.psvStatus(pMap, hset);
                    break;
                }

                default:
                    continue;
            }

            if (s != v.status) {
                if (pMap.getStatflag() == PropertiesMap.StatFlag.FULL)
                    logStatChange(fMap, log, v, s, v.status);
                change = true;
            }
        }
        return (change);
    }


    // Computes solution matrix coeffs. for PRVs, PSVs & FCVs whose status is not fixed to OPEN/CLOSED
    public static void computeMatrixCoeffs(PropertiesMap pMap, LSVariables ls, SparseMatrix smat, List<SimulationValve> valves) throws ENException {
        for (SimulationValve valve : valves) {
            if (valve.getSimSetting() == Constants.MISSING)
                continue;

            switch (valve.getType()) {
                case PRV:
                    valve.prvCoeff(pMap, ls, smat);
                    break;
                case PSV:
                    valve.psvCoeff(pMap, ls, smat);
                    break;
                case FCV:
                    valve.fcvCoeff(pMap, ls, smat);
                    break;
            }
        }
    }

}
