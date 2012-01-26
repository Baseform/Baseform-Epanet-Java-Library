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

import org.addition.epanet.msx.EnumTypes;

public class Source {
    private EnumTypes.SourceType    type;               // sourceType
    private int                     species;            // species index
    private double                  c0;                 // base concentration
    private int                     pattern;            // time pattern index
    private double                  massRate;           // actual mass flow rate

    public int getSpecies() {
        return species;
    }

    public void setSpecies(int species) {
        this.species = species;
    }

    public double getC0() {
        return c0;
    }

    public void setC0(double c0) {
        this.c0 = c0;
    }

    public int getPattern() {
        return pattern;
    }

    public void setPattern(int pattern) {
        this.pattern = pattern;
    }

    public double getMassRate() {
        return massRate;
    }

    public void setMassRate(double sMass) {
        this.massRate = sMass;
    }

    public EnumTypes.SourceType getType() {
        return type;
    }

    public void setType(EnumTypes.SourceType type) {
        this.type = type;
    }

    public Source() {
        type = EnumTypes.SourceType.CONCEN;
        species = 0;
        c0 = 0;
        pattern = 0;
        massRate = 0;
    }
}
