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

import java.util.ArrayList;
import java.util.List;

// Node object
public class Node {
    private List<Source> sources;        // ptr. to WQ source list
    private double  [] c;                // current species concentrations
    private double  [] c0;               // initial species concentrations
    private int     tank;                // tank index
    private boolean    rpt;                 // reporting flag


    public Node(int species) {
        sources = new ArrayList<Source>();
        tank = 0;
        rpt = false;
        c = new double[species];
        c0 = new double[species];
    }

    public List<Source> getSources() {
        return sources;
    }

    public double[] getC() {
        return c;
    }

    public void setC(double[] c) {
        this.c = c;
    }

    public double[] getC0() {
        return c0;
    }

    public void setC0(double[] c0) {
        this.c0 = c0;
    }

    public int getTank() {
        return tank;
    }

    public void setTank(int tank) {
        this.tank = tank;
    }

    public boolean getRpt() {
        return rpt;
    }

    public void setRpt(boolean rpt) {
        this.rpt = rpt;
    }


}
