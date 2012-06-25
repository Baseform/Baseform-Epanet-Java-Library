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

import org.addition.epanet.Constants;
import org.addition.epanet.network.FieldsMap;
import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.network.io.Keywords;
import org.addition.epanet.util.ENException;
import org.addition.epanet.util.Utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Hydraulic link structure (pipe)
 */
public class Link implements Comparable<Link> {
    /**
     * Init links flow resistance values.
     *
     * @param formflag
     * @param hexp
     * @throws org.addition.epanet.util.ENException
     *
     */
    public void initResistance(PropertiesMap.FormType formflag, Double hexp) {
        double e, d, L;
        this.setFlowResistance(Constants.CSMALL);
        switch (this.getType()) {
            case CV:
            case PIPE:
                e = this.getRoughness();
                d = this.getDiameter();
                L = this.getLenght();
                switch (formflag) {
                    case HW:
                        this.setFlowResistance(4.727 * L / Math.pow(e, hexp) / Math.pow(d, 4.871));
                        break;
                    case DW:
                        this.setFlowResistance(L / 2.0 / 32.2 / d / Math.pow(Constants.PI * Math.pow(d, 2) / 4.0, 2));
                        break;
                    case CM:
                        this.setFlowResistance(Math.pow(4.0 * e / (1.49 * Constants.PI * d * d), 2) *
                                Math.pow((d / 4.0), -1.333) * L);
                }
                break;

            case PUMP:
                this.setFlowResistance(Constants.CBIG);
                break;
        }
    }

    /**
     * Type of link
     */
    static public enum LinkType {
        /**
         * Pipe with check valve.
         */
        CV(0, Keywords.w_CV),
        /**
         * Flow control valve.
         */
        FCV(6, Keywords.w_FCV),
        /**
         * General purpose valve.
         */
        GPV(8, Keywords.w_GPV),
        /**
         * Pressure breaker valve.
         */
        PBV(5, Keywords.w_PBV),
        /**
         * Regular pipe.
         */
        PIPE(1, Keywords.w_PIPE),
        /**
         * Pressure reducing valve.
         */
        PRV(3, Keywords.w_PRV),
        /**
         * Pressure sustaining valve.
         */
        PSV(4, Keywords.w_PSV),
        /**
         * Pump.
         */
        PUMP(2, Keywords.w_PUMP),
        /**
         * Throttle control valve.
         */
        TCV(7, Keywords.w_TCV);

        public static LinkType parse(String text) {
            for (LinkType type : LinkType.values())
                if (Utilities.match(text, type.parseStr)) return type;
            return null;
        }

        /**
         * Sequencial ID.
         */
        public final int id;

        /**
         * Valve type string.
         */
        public final String parseStr;
        ;

        private LinkType(int val, String str) {
            id = val;
            parseStr = str;
        }
    }

    /**
     * Link/Tank/Pump status
     */
    static public enum StatType {
        /**
         * Valve active (partially open).
         */
        ACTIVE(4, Keywords.w_ACTIVE, Keywords.t_ACTIVE),
        /**
         * Closed.
         */
        CLOSED(2, Keywords.w_CLOSED, Keywords.t_CLOSED),
        /**
         * Tank emptying.
         */
        EMPTYING(9, "", Keywords.t_EMPTYING),
        /**
         * Tank filling.
         */
        FILLING(8, "", Keywords.t_FILLING),
        /**
         * Open.
         */
        OPEN(3, Keywords.w_OPEN, Keywords.t_OPEN),
        /**
         * Temporarily closed.
         */
        TEMPCLOSED(1, "", Keywords.t_TEMPCLOSED),
        /**
         * FCV cannot supply flow.
         */
        XFCV(6, "", Keywords.t_XFCV),
        /**
         * Pump exceeds maximum flow.
         */
        XFLOW(5, "", Keywords.t_XFLOW),
        /**
         * Pump cannot deliver head (closed).
         */
        XHEAD(0, "", Keywords.t_XHEAD),
        /**
         * Valve cannot supply pressure.
         */
        XPRESSURE(7, "", Keywords.t_XPRESSURE);

        /**
         * Sequencial id.
         */
        public final int id;

        /**
         * Parse string.
         */
        public final String parseStr;

        /**
         * Report string.
         */
        public final String reportStr;


        private StatType(int val, String str, String rep) {
            id = val;
            parseStr = str;
            reportStr = rep;
        }
    }

    /**
     * Initial species concentrations.
     */
    private double[] c0;
    /**
     * Link comment (parsed from INP or excel file)
     */
    private String comment;
    /**
     * Link diameter (feet).
     */
    private double diameter;
    /**
     * First node.
     */
    private Node first;
    /**
     * Link name.
     */
    private String ID;
    /**
     * Bulk react. coeff.
     */
    private double kb;
    /**
     * Minor loss coeff.
     */
    private double km;
    /**
     * Wall react. coeff.
     */
    private double kw;
    /**
     * Link length (feet).
     */
    private double lenght;
    /**
     * Kinetic parameter values.
     */
    private double[] param;

    /**
     * Flow resistance.
     */
    private double resistance;
    /**
     * Roughness factor.
     */
    private double roughness;
    /**
     * Link report flag.
     */
    private boolean rptFlag;
    /**
     * Second node.
     */
    private Node second;
    /**
     * Link status.
     */
    private StatType status;
    /**
     * Link subtype.
     */
    private LinkType type;
    /**
     * List of points for link path rendering.
     */
    private List<Point> vertices;

    public Link() {
        comment = "";
        vertices = new ArrayList<Point>();
        type = LinkType.CV;
        status = StatType.XHEAD;
    }

    public double[] getC0() {
        return c0;
    }

    public String getComment() {
        return comment;
    }

    public double getDiameter() {
        return diameter;
    }

    public Node getFirst() {
        return first;
    }

    public double getFlowResistance() {
        return resistance;
    }

    public String getId() {
        return ID;
    }

    public double getKb() {
        return kb;
    }

    public double getKm() {
        return km;
    }

    public double getKw() {
        return kw;
    }

    public double getLenght() {
        return lenght;
    }

    public double getNUDiameter(PropertiesMap.UnitsType type) {
        return NUConvert.revertDiameter(type, diameter);
    }

    public double getNULength(PropertiesMap.UnitsType type) {
        return NUConvert.revertDistance(type, lenght);
    }

    public double getNURoughness(PropertiesMap.FlowUnitsType fType, PropertiesMap.PressUnitsType pType, double SpGrav) {
        switch (getType()) {
            case FCV:
                return NUConvert.revertFlow(fType, roughness);
            case PRV:
            case PSV:
            case PBV:
                return NUConvert.revertPressure(pType, SpGrav, roughness);
        }
        return roughness;
    }

    public double[] getParam() {
        return param;
    }

    public double getRoughness() {
        return roughness;
    }

    public Node getSecond() {
        return second;
    }

    public StatType getStat() {
        return status;
    }

    public LinkType getType() {
        return type;
    }

    public List<Point> getVertices() {
        return vertices;
    }

    public boolean isRptFlag() {
        return rptFlag;
    }

    public void setC0(double[] c0) {
        this.c0 = c0;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setDiameter(double diameter) {
        this.diameter = diameter;
    }

    public void setDiameterAndUpdate(double diameter, org.addition.epanet.network.Network net) throws ENException {
        double realkm = km * Math.pow(this.diameter, 4.0) / 0.02517;
        this.diameter = diameter;
        km = 0.02517 * realkm / Math.pow(diameter, 4);
        initResistance(net.getPropertiesMap().getFormflag(), net.getPropertiesMap().getHexp());
    }


    public void setFirst(Node n1) {
        first = n1;
    }

    public void setFlowResistance(double r) {
        this.resistance = r;
    }

    public void setId(String id) {
        ID = id;
    }

    public void setKb(double kb) {
        this.kb = kb;
    }

    public void setKm(double km) {
        this.km = km;
    }

    public void setKw(double kw) {
        this.kw = kw;
    }

    public void setLenght(double len) {
        this.lenght = len;
    }

    public void setNUDiameter(PropertiesMap.UnitsType type, double value) {
        diameter = NUConvert.convertDistance(type, value);
    }

    public void setNULenght(PropertiesMap.UnitsType type, double value) {
        lenght = NUConvert.convertDistance(type, value);
    }

    public void setParam(double[] param) {
        this.param = param;
    }

    public void setReportFlag(boolean rptFlag) {
        this.rptFlag = rptFlag;
    }

    public void setRoughness(double kc) {
        this.roughness = kc;
    }

    public void setSecond(Node n2) {
        second = n2;
    }

    public void setStatus(StatType stat) {
        this.status = stat;
    }

    public void setType(LinkType type) {
        this.type = type;
    }

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//
//        Link link = (Link) o;
//
//        if (ID != null ? !ID.equals(link.ID) : link.ID != null) return false;
//
//        return true;
//    }

    @Override
    public int hashCode() {
        return ID != null ? ID.hashCode() : 0;
    }

    public int compareTo(Link o) {
        return ID.compareTo(o.ID);
    }
}
