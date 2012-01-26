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
import org.addition.epanet.network.structures.Curve;
import org.addition.epanet.network.structures.Link.StatType;
import org.addition.epanet.network.structures.Node;
import org.addition.epanet.network.structures.Pattern;
import org.addition.epanet.network.structures.Tank;
import org.addition.epanet.util.ENException;
import org.addition.epanet.util.Utilities;

import java.util.List;

/**
 *
 */
public class SimulationTank extends SimulationNode {

    private StatType oldStat;

    public SimulationTank(Node ref, int idx) {
        super(ref, idx);
        volume = ((Tank) node).getV0();

        // Init
        head = ((Tank) node).getH0();
        demand = (0.0);
        oldStat = StatType.TEMPCLOSED;
    }


    public Tank getNode() {
        return (Tank) node;
    }


    private double volume;


    public double getArea() {
        return ((Tank) node).getArea();
    }

    public double getHmin() {
        return ((Tank) node).getHmin();
    }

    public double getHmax() {
        return ((Tank) node).getHmax();
    }

    //public double getH0() {
    //    return ((Tank)node).getH0();
    //}

    public double getVmin() {
        return ((Tank) node).getVmin();
    }

    public double getVmax() {
        return ((Tank) node).getVmax();
    }

    public double getV0() {
        return ((Tank) node).getV0();
    }

    //public double getKb() {
    //    return ((Tank)node).getKb();
    //}
    //
    //public double [] getConcentration() {
    //    return ((Tank)node).getConcentration();
    //}

    public Pattern getPattern() {
        return ((Tank) node).getPattern();
    }

    public Curve getVcurve() {
        return ((Tank) node).getVcurve();
    }

    //public MixType getMixModel() {
    //    return ((Tank)node).getMixModel();
    //}
    //
    //public double getV1max() {
    //    return ((Tank)node).getV1max();
    //}


    /// Simulation getters & setters.

    public double getSimVolume() {
        return volume;//((Tank)node).getSimVolume();
    }

    public boolean isReservoir() {
        return ((Tank) node).getArea() == 0;
    }

    public StatType getOldStat() {
        return oldStat;
    }

    public void setOldStat(StatType oldStat) {
        this.oldStat = oldStat;
    }


    /// Simulation methods

    // Finds water volume in tank corresponding to elevation 'h'
    double findVolume(FieldsMap fMap, double h) throws ENException {

        Curve curve = getVcurve();
        if (curve == null)
            return (getVmin() + (h - getHmin()) * getArea());
        else {
            return (Utilities.linearInterpolator(curve.getNpts(), curve.getX(), curve.getY(),
                    (h - getElevation()) * fMap.getUnits(Type.HEAD) / fMap.getUnits(Type.VOLUME)));
        }

    }

    // Computes new water levels in tank after current time step, with Euler integrator.
    private void updateLevel(FieldsMap fMap, long tstep) throws ENException {

        if (getArea() == 0.0) // Reservoir
            return;

        // Euler
        double dv = demand * tstep;
        volume += dv;

        if (volume + demand >= getVmax())
            volume = getVmax();

        if (volume - demand <= getVmin())
            volume = getVmin();

        head = findGrade(fMap);
    }

    // Finds water level in tank corresponding to current volume
    private double findGrade(FieldsMap fMap) throws ENException {
        Curve curve = getVcurve();
        if (curve == null)
            return (getHmin() + (volume - getVmin()) / getArea());
        else
            return (getElevation() + Utilities.linearInterpolator(curve.getNpts(), curve.getY(), curve.getX(),
                    volume * fMap.getUnits(Type.VOLUME)) / fMap.getUnits(Type.HEAD));
    }

    // Get the required time step based to fill or drain a tank
    private long getRequiredTimeStep(long tstep) {
        if (isReservoir()) return tstep;  //  Skip reservoirs

        double h = head;    // Current tank grade
        double q = demand;  // Flow into tank
        double v = 0.0;

        if (Math.abs(q) <= Constants.QZERO)
            return tstep;

        if (q > 0.0 && h < getHmax())
            v = getVmax() - getSimVolume();        // Volume to fill
        else if (q < 0.0 && h > getHmin())
            v = getVmin() - getSimVolume();        // Volume to drain
        else
            return tstep;

        // Compute time to fill/drain
        long t = Math.round(v / q);

        // Revise time step
        if (t > 0 && t < tstep)
            tstep = t;

        return tstep;
    }

    // Revises time step based on shortest time to fill or drain a tank
    public static long minimumTimeStep(List<SimulationTank> tanks, long tstep) {
        long newTStep = tstep;
        for (SimulationTank tank : tanks)
            newTStep = tank.getRequiredTimeStep(newTStep);
        return newTStep;
    }

    // Computes new water levels in tanks after current time step.
    public static void stepWaterLevels(List<SimulationTank> tanks, FieldsMap fMap, long tstep) throws ENException {
        for (SimulationTank tank : tanks)
            tank.updateLevel(fMap, tstep);
    }

}
