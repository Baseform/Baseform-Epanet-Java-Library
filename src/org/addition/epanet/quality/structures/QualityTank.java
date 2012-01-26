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

package org.addition.epanet.quality.structures;

import org.addition.epanet.network.structures.Node;
import org.addition.epanet.network.structures.Tank;
import java.util.LinkedList;

/**
 * Wrapper class for the Tank in the water quality simulation.
 */
public class QualityTank extends QualityNode
{
    /**
     * Current species concentration [user units].
     */
    private double concentration;

    /**
     * Discrete water quality segments assigned to this tank.
     */
    private final LinkedList<QualitySegment> segments;

    /**
     * Tank current volume [Feet^3].
     */
    private double volume;

    /**
     * Initialize tank properties from the original tank node.
     */
    public QualityTank(Node node) {
        super(node);
        segments = new LinkedList<QualitySegment>();
        volume = ((Tank)node).getV0();
        concentration =node.getC0()[0];
    }

    /**
     * Get species concentration.
     */
    public double getConcentration() {
        return concentration;
    }

    public LinkedList<QualitySegment> getSegments() {
        return segments;
    }

    /**
     * Get tank water volume.
     * @return Water volume [Feet^3].
     */
    public double getVolume() {
        return volume;
    }

    /**
     * Set species concentrations.
     */
    public void setConcentration(double concentration) {
        this.concentration = concentration;
    }

    /**
     * Set water tank volume.
     * @param volume Water volume [Feet^3].
     */
    public void setVolume(double volume) {
        this.volume = volume;
    }
}

