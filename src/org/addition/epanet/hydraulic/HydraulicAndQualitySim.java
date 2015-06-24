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

import java.io.IOException;
import java.util.logging.Logger;

import org.addition.epanet.hydraulic.io.AwareStep;
import org.addition.epanet.network.Network;
import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.quality.QualitySim;
import org.addition.epanet.util.ENException;

/**
 * Hybrid Hydraulic and Water Quality simulation class.
 */
public class HydraulicAndQualitySim extends HydraulicSim {

    /**
     * Optional related QualitySim when Water Quality analysis is needed.
     */
    private QualitySim qualitySim;

    /**
     * Init hydraulic simulation, preparing the linear solver and the hydraulic structures wrappers.
     *
     * @param net Hydraulic network reference.
     * @param log Logger reference.
     * @throws ENException
     */
    public HydraulicAndQualitySim(Network net, Logger log) throws ENException {
        super(net, log);
        if (!net.getPropertiesMap().getQualflag().equals(PropertiesMap.QualType.NONE)) qualitySim = new QualitySim(net, log);
    }

    /**
     * Write the simulation results to current output.
     */
    @Override
    protected void writeSimulationOutput() throws ENException, IOException {
        if (simulationOutput != null && qualitySim != null) {
            AwareStep.writeHydAndQual(simulationOutput, this, qualitySim, Rtime-Htime, Htime);
            return;
        }
        super.writeSimulationOutput();
    }

    /**
     * Finds length of next time step & updates tank levels and rule-based control actions.
     */
    @Override
    protected long nextHyd() throws ENException, IOException {
        long hydstep = super.nextHyd();

        if (qualitySim != null) {
            for (long i = 0, qstep = pMap.getQstep(), numQsteps = hydstep/qstep; i < numQsteps; i++) {
                qualitySim.simulateSingleStep(nNodes, nLinks, qstep);
            }
        }
        return hydstep;
    }
}
