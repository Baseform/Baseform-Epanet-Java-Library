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

package org.addition.epanet.hydraulic.models;


import org.addition.epanet.util.ENException;
import org.addition.epanet.hydraulic.structures.SimulationLink;
import org.addition.epanet.network.PropertiesMap;

/**
 * Pipe head loss model calculator.
 */
public interface PipeHeadModel {

    /**
     * Link coefficients.
     */
    public static class LinkCoeffs{
        public LinkCoeffs(double invHeadLoss, double flowCorrection) {
            this.invHeadLoss = invHeadLoss;
            this.flowCorrection = flowCorrection;
        }

        private double invHeadLoss;
        private double flowCorrection;

        public double getInvHeadLoss() {
            return invHeadLoss;
        }

        public double getFlowCorrection() {
            return flowCorrection;
        }
    }
    //public double compute(PropertiesMap pMap,SimulationLink link) throws ENException;

    /**
     * Compute link coefficients through the implemented pipe headloss model.
     * @param pMap Network properties map.
     * @param sL Simulation link.
     * @return Computed link coefficients.
     * @throws ENException
     */
    public LinkCoeffs compute(PropertiesMap pMap,SimulationLink sL) throws ENException;
}
