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

import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.network.structures.Link;
import org.addition.epanet.network.structures.Node;
import org.addition.epanet.util.ENException;

import java.util.LinkedList;
import java.util.List;

/**
 * Wrapper class for the Link in the water quality simulation.
 */
public class QualityLink {

    /**
     * Reference to the first water quality node.
     */
    private final QualityNode first;

    /**
     * Current water flow[Feet^3/Second].
     */
    private double  flow;

    /**
     * Current flow direction.
     */
    private boolean flowDir;

    /**
     * Current flow resistance[Feet/Second].
     */
    private double  flowResistance;

    /**
     * Reference to the original link.
     */
    private final Link link;

    /**
     * Reference to the second water quality node.
     */
    private final QualityNode second;

    /**
     * Linked list of discrete water parcels.
     */
    private final LinkedList<QualitySegment> segments;

    /**
     * Initialize a new water quality Link wrapper from the original Link.
     * @param oNodes
     * @param qNodes
     * @param link
     */
    public QualityLink(List<Node> oNodes,List<QualityNode> qNodes, Link link)
    {
        int n1 = oNodes.indexOf(link.getFirst());
        int n2 = oNodes.indexOf(link.getSecond());
        first = qNodes.get(n1);
        second = qNodes.get(n2);
        segments = new LinkedList<QualitySegment>();
        this.link = link;
    }

    /**
     * Get first node reference.
     * @return Reference to the water quality simulation node.
     */
    public QualityNode getFirst() {
        return first;
    }

    /**
     * Get the water flow.
     * @return
     */
    public double getFlow() {
        return flow;
    }

    /**
     * Get the water flow direction.
     * @return
     */
    public boolean getFlowDir() {
        return flowDir;
    }

    ///**
    // * Current reaction rate.
    // */
    //double  reactionRate;    // Pipe reaction rate
    //
    //public double getReactionRate() {
    //    return reactionRate;
    //}
    //
    //public void setReactionRate(double reactionRate) {
    //    this.reactionRate = reactionRate;
    //}

    /**
     * Get the link flow resistance.
     * @return [Feet/Second]
     */
    public double getFlowResistance() {
        return flowResistance;
    }

    /**
     * Get the original link.
     * @return Reference to the hydraulic network link.
     */
    public Link getLink() {
        return link;
    }

    /**
     * Get the second node reference
     * @return Reference to the water quality simulation node.
     */
    public QualityNode getSecond() {
        return second;
    }

    /**
     * Get the water quality segments in this link.
     * @return
     */
    public LinkedList<QualitySegment> getSegments() {
        return segments;
    }

    /**
     * Set the water flow.
     * @param hydFlow
     */
    public void setFlow(double hydFlow) {
        this.flow = hydFlow;
    }

    /**
     * Set the water flow direction.
     * @param flowDir
     */
    public void setFlowDir(boolean flowDir) {
        this.flowDir = flowDir;
    }

    /**
     * Set the link flow resistance.
     * @param kw [Feet/Second]
     */
    public void setFlowResistance(double kw) {
        flowResistance = kw;
    }

    /**
     * Get the upstream node.
     * @return
     */
    public QualityNode getUpStreamNode(){
        return ((flowDir) ? first : second);
    }

    /**
     * Get the downstream node.
     * @return
     */
    public QualityNode getDownStreamNode(){
        return ((flowDir) ? second : first);
    }

    /**
     * Get link volume.
     * @return
     */
    public double getLinkVolume(){
        return ( 0.785398*link.getLenght()*(link.getDiameter()*link.getDiameter()) );
    }

    /**
     * Get link average quality.
     * @param pMap
     * @return
     */
    public double getAverageQuality(PropertiesMap pMap) {
         double vsum = 0.0,
                msum = 0.0;

        try {
            if (pMap != null && pMap.getQualflag() == PropertiesMap.QualType.NONE)
                return (0.);
        } catch (ENException e) {
            return 0.0;
        }

        for (QualitySegment seg : getSegments()) {
            vsum += seg.v;
            msum += (seg.c) * (seg.v);
        }

        if (vsum > 0.0)
            return (msum / vsum);
        else
            return ((getFirst().getQuality() + getSecond().getQuality()) / 2.0);
    }
}
