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
 * Hazen-Williams model calculator.
 */
public class HWModelCalculator implements PipeHeadModel{

    public LinkCoeffs compute(PropertiesMap pMap,SimulationLink sL) throws ENException {
        // Evaluate headloss coefficients
        double q = Math.abs(sL.getSimFlow());      // Absolute flow
        double ml = sL.getLink().getKm();          // Minor loss coeff.
        double r = sL.getLink().getFlowResistance();         // Resistance coeff.

        double r1 = 1.0 * r + ml;

        // Use large P coefficient for small flow resistance product
        if (r1 * q < pMap.getRQtol()){
            return new LinkCoeffs(1d / pMap.getRQtol(),sL.getSimFlow() / pMap.getHexp());
        }

        double hpipe = r * Math.pow(q, pMap.getHexp());     // Friction head loss
        double p = pMap.getHexp() * hpipe;                  // Q*dh(friction)/dQ
        double hml;
        if (ml > 0d) {
            hml = ml * q * q;   // Minor head loss
            p += 2d * hml;     // Q*dh(Total)/dQ
        } else
            hml = 0d;

        p = sL.getSimFlow() / p;  // 1 / (dh/dQ)
        return new LinkCoeffs(Math.abs(p),p * (hpipe + hml));
    }
}
