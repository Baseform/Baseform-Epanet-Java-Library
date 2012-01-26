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
import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.network.io.Keywords;
import org.addition.epanet.util.Utilities;

/**
 * Hydraulic tank structure.
 */
public class Tank extends Node{

    /**
     * Tank mixing regimes.
      */
    static public enum MixType{
        /**
         * First in, first out model
         */
        FIFO(2,Keywords.w_FIFO),
        /**
         *  Last in, first out model
         */
        LIFO(3,Keywords.w_LIFO),
        /**
         * 1-compartment model
         */
        MIX1(0, Keywords.w_MIXED),
        /**
         * 2-compartment model
         */
        MIX2(1,Keywords.w_2COMP);

        public static MixType parse(String text){
            for (MixType type : MixType.values())
                if (Utilities.match(text, type.parseStr)) return type;
            return null;
        }

        /**
         * Sequencial ID.
         */
        public final int id;

        /**
         * Parse string id.
         */
        public final String parseStr;

        private MixType(int val, String pStr){id = val;parseStr=pStr;}
    }

    /**
     * Tank area (feet^2).
     */
    private double area;
    /**
     * Tank volume (feet^3).
     */
    //private double v;
    /**
     * Species concentration.
     */
    private double  [] c;
    /**
     * Initial water elev.
     */
    private double h0;
    /**
     * Maximum water elev (feet).
     */
    private double hMax;
    /**
     * Minimum water elev (feet).
     */
    private double hMin;
    /**
     * Reaction coeff. (1/days).
     */
    private double kb;
    /**
     * Type of mixing model
     */
    private MixType mixModel;
    /**
     * Fixed grade time pattern.
     */
    private Pattern pattern;
    /**
     * Initial volume (feet^3).
     */
    private double v0;
    /**
     * Mixing compartment size
     */
    private double  v1max;
    /**
     * Fixed grade time pattern
     */
    private Curve vCurve;
    /**
     * Maximum volume (feet^3).
     */
    private double vMax;
    /**
     * Minimum volume (feet^3).
     */
    private double vMin;


    public Tank() {
        super();
    }

    public double getArea() {
        return area;
    }

    public double [] getConcentration() {
        return c;
    }

    public double getH0() {
        return h0;
    }

    public double getHmax() {
        return hMax;
    }

    public double getHmin() {
        return hMin;
    }

    public double getKb() {
        return kb;
    }

    public MixType getMixModel() {
        return mixModel;
    }

    public double getNUArea(PropertiesMap.UnitsType type){
        return NUConvert.revertArea(type,area);
    }

    public double getNUInitHead(PropertiesMap.UnitsType type){
        return NUConvert.revertDistance(type,h0);
    }

    public double getNUInitVolume(PropertiesMap.UnitsType type){
        return NUConvert.revertVolume(type,v0);
    }

    public double getNUMaximumHead(PropertiesMap.UnitsType type){
        return NUConvert.revertDistance(type,hMax);
    }

    public double getNUMaxVolume(PropertiesMap.UnitsType type){
        return NUConvert.revertVolume(type,vMax);
    }

    public double getNUMinimumHead(PropertiesMap.UnitsType type){
        return NUConvert.revertDistance(type,hMin);
    }

    public double getNUMinVolume(PropertiesMap.UnitsType type){
        return NUConvert.revertVolume(type,vMin);
    }

    public void setNUMinVolume(PropertiesMap.UnitsType type, double value){
        vMin = NUConvert.convertVolume(type,value);
    }


    public double getNUMixCompartimentSize(PropertiesMap.UnitsType type){
        return NUConvert.revertVolume(type,v1max);
    }


    public Pattern getPattern() {
        return pattern;
    }

    public double getV0() {
        return v0;
    }

    public double getV1max() {
        return this.v1max;
    }

    public Curve getVcurve() {
        return vCurve;
    }

    public double getVmax() {
        return vMax;
    }

    public double getVmin() {
        return vMin;
    }

    public void setArea(double a) {
        area = a;
    }

    public void setConcentration(double [] c) {
        this.c = c;
    }

    public void setH0(double h0) {
        this.h0 = h0;
    }

    public void setHmax(double HMax) {
        this.hMax = HMax;
    }

    public void setHmin(double HMin) {
        this.hMin = HMin;
    }

    public void setKb(double kb) {
        this.kb = kb;
    }

    public void setMixModel(MixType mixModel) {
        this.mixModel = mixModel;
    }

    public void setNUArea(PropertiesMap.UnitsType type, double value){
        area = NUConvert.convertArea(type,value);
    }

    public void setNUInitHead(PropertiesMap.UnitsType type, double value){
        h0 = NUConvert.revertDistance(type,value);
    }

    public void setNUInitVolume(PropertiesMap.UnitsType type, double value){
        v0 = NUConvert.convertVolume(type,value);
    }

    //public double getVolume() {
    //    return v;
    //}
    //
    //public void setVolume(double v) {
    //    this.v = v;
    //}

    public void setNUMaximumHead(PropertiesMap.UnitsType type, double value){
        hMax = NUConvert.revertDistance(type,value);
    }

    public void setNUMaxVolume(PropertiesMap.UnitsType type, double value){
        vMax = NUConvert.convertVolume(type,value);
    }

    public void setNUMinimumHead(PropertiesMap.UnitsType type, double value){
        hMin = NUConvert.convertArea(type,value);
    }

    public void setNUMixCompartimentSize(PropertiesMap.UnitsType type, double value){
        v1max = NUConvert.convertVolume(type,value);
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public void setV0(double v0) {
        this.v0 = v0;
    }

    public void setV1max(double vLmax) {
        this.v1max = vLmax;
    }

    public void setVcurve(Curve vcurve) {
        vCurve = vcurve;
    }

    public void setVmax(double vmax) {
        vMax = vmax;
    }

    public void setVmin(double vmin) {
        vMin = vmin;
    }
}
