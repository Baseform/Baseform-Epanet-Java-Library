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

import org.addition.epanet.util.DblList;

import java.util.List;

/**
 * Temporal pattern.
 */
public class Pattern {
    /**
     * Pattern factors list.
     */
    private DblList factors;
    /**
     * Pattern name.
     */
    private String  id;

    public Pattern() {
        this.id = "";
        factors = new DblList();
    }

    public void add(Double factor){
        factors.add(factor);
    }


    public List<Double> getFactorsList(){
        return factors;
    }

    public String getId() {
        return id;
    }

    public int getLength(){
        return factors.size();
    }

    public void setId(String text){
        id = text;
    }
}
