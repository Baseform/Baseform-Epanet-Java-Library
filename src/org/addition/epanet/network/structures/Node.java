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

package org.addition.epanet.network.structures;

import org.addition.epanet.network.PropertiesMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Hydraulic node structure  (junction)
 */
public class Node implements Comparable<Node> {
    /**
     * Node id string.
     */
    private String id;
    /**
     * Node elevation(foot).
     */
    private double elevation;

    /**
     * Node demand list.
     */
    private List<Demand> demand;
    /**
     * Water quality source.
     */
    private Source source;
    /**
     * Initial species concentrations.
     */
    private double[] C0;
    /**
     * Emitter coefficient.
     */
    private double Ke;
    /**
     * Node reporting flag.
     */
    private boolean rptFlag;
    /**
     * Node position.
     */
    private Point position;

    /**
     *
     */
    private transient double initDemand;
    /**
     * Node comment.
     */
    private String comment;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public double getInitDemand() {
        return initDemand;
    }

    public void setInitDemand(double initDemand) {
        this.initDemand = initDemand;
    }

    //public NodeType getType() {
    //    return type;
    //}
    //
    //public void setType(NodeType type) {
    //    this.type = type;
    //}

    public Node() {
        C0 = new double[1];
        comment = "";
        initDemand = 0;
        //type = NodeType.JUNC;
        demand = new ArrayList<Demand>();
        position = new Point();
    }

    public Point getPosition() {
        return position;
    }

    public void setPosition(Point position) {
        this.position = position;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getElevation() {
        return elevation;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    public List<Demand> getDemand() {
        return demand;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public double[] getC0() {
        return C0;
    }

    public void setC0(double[] c0) {
        C0 = c0;
    }

    public double getKe() {
        return Ke;
    }

    public void setKe(double ke) {
        Ke = ke;
    }

    public boolean isRptFlag() {
        return rptFlag;
    }

    public void setReportFlag(boolean rptFlag) {
        this.rptFlag = rptFlag;
    }

    public double getNUElevation(PropertiesMap.UnitsType units) {
        return NUConvert.revertDistance(units, elevation);
    }

    public void setNUElevation(PropertiesMap.UnitsType units, double elevation) {
        elevation = NUConvert.convertDistance(units, elevation);
    }

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//
//        Node node = (Node) o;
//
//        if (id != null ? !id.equals(node.id) : node.id != null) return false;
//
//        return true;
//    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public int compareTo(Node o) {
        return id.compareTo(o.id);
    }
}