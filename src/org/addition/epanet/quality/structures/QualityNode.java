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

/**
 * Wrapper class for the Node in the water quality simulation.
 */
public class QualityNode {

    /**
     * Factory method to instantiate the quality node from the hydraulic network node.
     */
    public static QualityNode create(Node node){
        if(node instanceof Tank)
            return new QualityTank(node);
        else
            return new QualityNode(node);
    }

    /**
     * Node demand [Feet^3/Second]
     */
    private double  demand;

    /**
     *  Total mass inflow to node.
     */
    private double  massIn;

    /**
     *
     */
    private double  massRate;

    /**
     * Hydraulic network node reference.
     */
    private final Node    node;

    /**
     * Species concentration [user units].
     */
    private double  quality;

    /**
     *
     */
    private double  sourceContribution;

    /**
     * Total volume inflow to node.
     */
    private double  volumeIn;

    /**
     * Init quality node properties.
     */
    QualityNode(Node node) {
        this.node = node;
        quality = node.getC0()[0];
        if(this.node.getSource()!=null)
            massRate = 0.0;
    }

    public double getDemand() {
        return demand;
    }

    public double getMassIn() {
        return massIn;
    }

    public double getMassRate() {
        return massRate;
    }


    /**
     * Get the original hydraulic network node.
     */
    public Node getNode() {
        return node;
    }

    public double getQuality() {
        return quality;
    }

    public double getSourceContribution() {
        return sourceContribution;
    }

    public double getVolumeIn() {
        return volumeIn;
    }

    public void setDemand(double demand) {
        this.demand = demand;
    }

    public void setMassIn(double massIn) {
        this.massIn = massIn;
    }

    public void setMassRate(double massRate) {
        this.massRate = massRate;
    }

    public void setQuality(double quality) {
        this.quality = quality;
    }

    public void setSourceContribution(double sourceConcentration) {
        this.sourceContribution = sourceConcentration;
    }

    public void setVolumeIn(double volumeIn) {
        this.volumeIn = volumeIn;
    }
}
