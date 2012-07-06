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
import org.addition.epanet.network.FieldsMap;
import org.addition.epanet.network.FieldsMap.Type;
import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.network.structures.Curve;
import org.addition.epanet.network.structures.Link;
import org.addition.epanet.network.structures.Link.StatType;
import org.addition.epanet.network.structures.Pattern;
import org.addition.epanet.network.structures.Pump;
import org.addition.epanet.util.ENException;
import org.addition.epanet.util.Utilities;

import java.util.Collection;
import java.util.List;

public class SimulationPump extends SimulationLink {

    private double h0;                  // Simulated shutoff head
    private double flowCoefficient;     // Simulated Flow coefficent
    private double n;                   // Simulated flow expoent

    public static class Energy {
        public Energy(double power, double efficiency) {
            this.power = power;
            this.efficiency = efficiency;
        }

        public double power;        // Pump used power (KW)
        public double efficiency;   // Pump effiency
    }

    public SimulationPump(Collection<SimulationNode> indexedNodes, Link ref, int idx) {
        super(indexedNodes, ref, idx);
        for (int i = 0; i < 6; i++)
            energy[i] = ((Pump) ref).getEnergy(0);

        h0 = ((Pump) ref).getH0();
        flowCoefficient = ((Pump) ref).getFlowCoefficient();
        n = ((Pump) ref).getN();
    }

    private double energy[] = {0, 0, 0, 0, 0, 0};


    public Pump.Type getPtype() {
        return ((Pump) link).getPtype();
    }

    public double getQ0() {
        return ((Pump) link).getQ0();
    }

    public double getQmax() {
        return ((Pump) link).getQmax();
    }

    public double getHmax() {
        return ((Pump) link).getHmax();
    }


    public Curve getHcurve() {
        return ((Pump) link).getHcurve();
    }

    public Curve getEcurve() {
        return ((Pump) link).getEcurve();
    }

    public Pattern getUpat() {
        return ((Pump) link).getUpat();
    }

    public Pattern getEpat() {
        return ((Pump) link).getEpat();
    }

    public double getEcost() {
        return ((Pump) link).getEcost();
    }


    // Simulation getters and setters


    public double getEnergy(int id) {
        return energy[id];//((Pump)node).getEnergy(id);
    }

    public void setEnergy(int id, double value) {
        energy[id] = value;
    }


    private void setH0(double h0) {
        this.h0 = h0;
    }

    public double getH0() {
        return h0;
    }

    public double getFlowCoefficient() {
        return flowCoefficient;
    }

    private void setFlowCoefficient(double flowCoefficient) {
        this.flowCoefficient = flowCoefficient;
    }

    private void setN(double n) {
        this.n = n;
    }

    public double getN() {
        return n;
    }

    // Computes flow energy associated with this link pump.
    private Energy getFlowEnergy(PropertiesMap pMap, FieldsMap fMap) throws ENException {
        Energy ret = new Energy(0.0, 0.0);

        if (status.id <= StatType.CLOSED.id) {
            return ret;
        }

        double q = Math.abs(flow);
        double dh = Math.abs(first.getSimHead() - second.getSimHead());

        double e = pMap.getEpump();

        if (getEcurve() != null) {
            Curve curve = getEcurve();
            e = Utilities.linearInterpolator(curve.getNpts(),
                    curve.getX(), curve.getY(), q * fMap.getUnits(Type.FLOW));
        }
        e = Math.min(e, 100.0);
        e = Math.max(e, 1.0);
        e /= 100.0;

        ret.power = dh * q * pMap.getSpGrav() / 8.814 / e * Constants.KWperHP;
        ret.efficiency = e;

        return ret;
    }


    // Accumulates pump energy usage.
    private double updateEnergy(PropertiesMap pMap, FieldsMap fMap,
                                long n, double c0, double f0, double dt) throws ENException {
        double c = 0;

        //Skip closed pumps
        if (status.id <= StatType.CLOSED.id) return 0.0;
        double q = Math.max(Constants.QZERO, Math.abs(flow));

        // Find pump-specific energy cost
        if (getEcost() > 0.0)
            c = getEcost();
        else
            c = c0;

        if (getEpat() != null) {
            int m = (int) (n % (long) getEpat().getFactorsList().size());
            c *= getEpat().getFactorsList().get(m);
        } else
            c *= f0;

        // Find pump energy & efficiency
        Energy energy = getFlowEnergy(pMap, fMap);

        // Update pump's cumulative statistics
        setEnergy(0, getEnergy(0) + dt);                        // Time on-line
        setEnergy(1, getEnergy(1) + energy.efficiency * dt);    // Effic.-hrs
        setEnergy(2, getEnergy(2) + energy.power / q * dt);     // kw/cfs-hrs
        setEnergy(3, getEnergy(3) + energy.power * dt);         // kw-hrs
        setEnergy(4, Math.max(getEnergy(4), energy.power));
        setEnergy(5, getEnergy(5) + c * energy.power * dt);         // cost-hrs.

        return energy.power;
    }

    // Computes P & Y coeffs. for pump in the link
    void computePumpCoeff(FieldsMap fMap, PropertiesMap pMap) throws ENException {
        double h0, q, r, n;

        if (status.id <= StatType.CLOSED.id || setting == 0.0) {
            invHeadLoss = 1.0 / Constants.CBIG;
            flowCorrection = flow;
            return;
        }

        q = Math.max(Math.abs(flow), Constants.TINY);

        if (getPtype() == Pump.Type.CUSTOM) {

            Curve.Coeffs coeffs = getHcurve().getCoeff(fMap, q / setting);

            setH0(-coeffs.h0);
            setFlowCoefficient(-coeffs.r);
            setN(1.0);
        }

        h0 = (setting * setting) * getH0();
        n = getN();
        r = getFlowCoefficient() * Math.pow(setting, 2.0 - n);
        if (n != 1.0) r = n * r * Math.pow(q, n - 1.0);

        invHeadLoss = 1.0 / Math.max(r, pMap.getRQtol());
        flowCorrection = flow / n + invHeadLoss * h0;
    }

    // Get new pump status
    // dh head gain
    public StatType pumpStatus(PropertiesMap pMap, double dh) throws ENException {
        double hmax;

        if (getPtype() == Pump.Type.CONST_HP)
            hmax = Constants.BIG;
        else
            hmax = (setting * setting) * getHmax();

        if (dh > hmax + pMap.getHtol())
            return (StatType.XHEAD);

        return (StatType.OPEN);
    }

    // Update pumps energy
    public static double stepEnergy(PropertiesMap pMap, FieldsMap fMap,
                                    Pattern Epat,
                                    List<SimulationPump> pumps,
                                    long htime, long hstep) throws ENException {
        double dt, psum = 0.0;


        if (pMap.getDuration() == 0)
            dt = 1.0;
        else if (htime < pMap.getDuration())
            dt = (double) hstep / 3600.0;
        else
            dt = 0.0;

        if (dt == 0.0)
            return 0.0;

        long n = (htime + pMap.getPstart()) / pMap.getPstep();


        double c0 = pMap.getEcost();
        double f0 = 1.0;

        if (Epat != null) {
            long m = n % (long) Epat.getFactorsList().size();
            f0 = Epat.getFactorsList().get((int) m);
        }

        for (SimulationPump pump : pumps) {
            psum += pump.updateEnergy(pMap, fMap, n, c0, f0, dt);
        }

        return psum;
    }


}
