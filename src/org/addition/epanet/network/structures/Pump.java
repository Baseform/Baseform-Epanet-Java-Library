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

/**
 * Hydraulic pump structure.
 */
public class Pump extends Link
{
    /**
     * Type of pump curve.
      */
    static public enum Type{
        /**
         * Constant horsepower.
         */
        CONST_HP(0),
        /**
         * User-defined custom curve.
         */
        CUSTOM(2),
        NOCURVE(3),

        /**
         * Power function.
         */
        POWER_FUNC(1);

        /**
         * Sequencial id.
         */
        public final int id;

        private Type(int val){id = val;}
    }

    /**
     * Unit energy cost.
     */
    private double eCost;
    /**
     * Effic. v. flow curve reference.
     */
    private Curve eCurve;
    /**
     * Energy usage statistics.
     */
    private double  energy[] = {0,0,0,0,0,0};
    /**
     * Energy cost pattern.
     */
    private Pattern ePat;
    /**
     * Shutoff head (feet)
     */
    private double h0;
    /**
     * Head v. flow curve reference.
     */
    private Curve hCurve;
    /**
     * Maximum head (feet)
     */
    private double hMax;
    /**
     * Flow exponent.
     */
    private double n;
    /**
     * Pump curve type.
     */
    private Type ptype;
    /**
     * Initial flow (feet^3/s).
     */
    private double q0;
    /**
     * Maximum flow (feet^3/s).
     */
    private double qMax;
    /**
     * Flow coefficient.
     */
    private double r;
    /**
     * Utilization pattern reference.
     */
    private Pattern uPat;

    public Pump() {
        super();
    }


    public double getEcost() {
        return eCost;
    }

    public Curve getEcurve() {
        return eCurve;
    }

    public double getEnergy(int id) {
        return energy[id];
    }

    public Pattern getEpat() {
        return ePat;
    }

    public double getFlowCoefficient() {
        return r;
    }

    public double getH0() {
        return h0;
    }

    public Curve getHcurve() {
        return hCurve;
    }

    public double getHmax() {
        return hMax;
    }

    public double getN() {
        return n;
    }

    public double getNUFlowCoefficient(PropertiesMap.UnitsType type){
        return NUConvert.revertPower(type,r);
    }


    public double getNUInitialFlow(PropertiesMap.FlowUnitsType type){
        return NUConvert.revertFlow(type,q0);
    }

    public double getNUMaxFlow(PropertiesMap.FlowUnitsType type){
        return NUConvert.revertFlow(type,qMax);
    }

    public double getNUMaxHead(PropertiesMap.UnitsType type){
        return NUConvert.revertDistance(type,hMax);
    }

    public double getNUShutoffHead(PropertiesMap.UnitsType type){
        return NUConvert.revertDistance(type,hMax);
    }

    public Type getPtype() {
        return ptype;
    }

    public double getQ0() {
        return q0;
    }

    public double getQmax() {
        return qMax;
    }

    public Pattern getUpat() {
        return uPat;
    }

    public void setEcost(double ecost) {
        eCost = ecost;
    }

    public void setEcurve(Curve ecurve) {
        this.eCurve = ecurve;
    }

    public void setEnergy(int id, double energy) {
        this.energy[id]=energy;
    }

    public void setEpat(Pattern epat) {
        ePat = epat;
    }

    public void setFlowCoefficient(double r) {
        this.r = r;
    }

    public void setH0(double h0) {
        this.h0 = h0;
    }

    public void setHcurve(Curve hcurve) {
        this.hCurve = hcurve;
    }

    public void setHmax(double hmax) {
        this.hMax = hmax;
    }

    public void setN(double n) {
        this.n = n;
    }

    public void setNUFlowCoefficient(PropertiesMap.UnitsType type, double value){
        r = NUConvert.convertPower(type,value);
    }

    public void setNUInitialFlow(PropertiesMap.FlowUnitsType type, double value){
        q0 = NUConvert.convertFlow(type,value);
    }

    public void setNUMaxFlow(PropertiesMap.FlowUnitsType type, double value){
        qMax = NUConvert.convertFlow(type,value);
    }

    public void setNUMaxHead(PropertiesMap.UnitsType type, double value){
        hMax = NUConvert.convertDistance(type,value);
    }

    public void setNUShutoffHead(PropertiesMap.UnitsType type, double value){
        h0 = NUConvert.convertDistance(type,value);
    }

    public void setPtype(Type ptype) {
        this.ptype = ptype;
    }

    public void setQ0(double q0) {
        this.q0 = q0;
    }

    public void setQmax(double qMax) {
        this.qMax = qMax;
    }

    public void setUpat(Pattern upat) {
        uPat = upat;
    }
}
