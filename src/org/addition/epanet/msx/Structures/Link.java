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

package org.addition.epanet.msx.Structures;

// Link  Object
public class Link {

    int    n1;           // start node index
    int    n2;           // end node index
    double diam;         // diameter
    double len;          // length
    boolean   rpt;          // reporting flag
    double [] c0;        // initial species concentrations
    double [] param;     // kinetic parameter values
    double roughness;    //roughness

    public Link(int species, int parameter) {
        c0 = new double[species];
        param = new double[parameter];
        rpt = false;
    }

    public int getN1() {
        return n1;
    }

    public void setN1(int n1) {
        this.n1 = n1;
    }

    public int getN2() {
        return n2;
    }

    public void setN2(int n2) {
        this.n2 = n2;
    }

    public double getDiam() {
        return diam;
    }

    public void setDiam(double diam) {
        this.diam = diam;
    }

    public double getLen() {
        return len;
    }

    public void setLen(double len) {
        this.len = len;
    }

    public boolean getRpt() {
        return rpt;
    }

    public void setRpt(boolean rpt) {
        this.rpt = rpt;
    }

    public double[] getC0() {
        return c0;
    }

    public void setC0(double[] c0) {
        this.c0 = c0;
    }

    public double[] getParam() {
        return param;
    }

    public void setParam(double[] param) {
        this.param = param;
    }

    public double getRoughness() {
        return roughness;
    }

    public void setRoughness(double roughness) {
        this.roughness = roughness;
    }
}
