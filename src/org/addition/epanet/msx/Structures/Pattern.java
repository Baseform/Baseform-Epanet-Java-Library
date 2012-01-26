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

// Time Pattern object
public strictfp class Pattern {
    private String          id;             // pattern ID
    private long            interval;       // current time interval
    private int             current;        // current multiplier
    private List<Double>    multipliers;    // list of multipliers

    public int getLength(){
        return multipliers.size();
    }

    public void setId(String text){
        id = text;
    }

    public String getId() {
        return id;
    }

    public List<Double> getMultipliers(){
        return multipliers;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public Pattern() {
        this.id = "";
        multipliers = new ArrayList<Double>();
        current = 0;
        interval = 0;
    }

    public Pattern clone(){
        Pattern pat = new Pattern();
        pat.id = id;
        pat.current = current;
        pat.multipliers = new ArrayList<Double>(multipliers);
        pat.interval = interval;
        return  pat;
    }
}
