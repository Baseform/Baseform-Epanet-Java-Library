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

/**
 * Report field properties.
 */
public class Field {
    /**
     * Range limits.
      */
    static public enum RangeType{
        /**
         * upper limit
         */
        HI(1),
        /**
         * lower limit
         */
        LOW(0),
        /**
         * precision
         */
        PREC(2);

        public final int id;

        private RangeType(int id) {this.id = id;}
    }

    /**
     * Enabled if in table.
     */
    private boolean  enabled;
    /**
     * Name of reported variable.
     */
    private String   name;
    /**
     * Number of decimal places.
     */
    private int      precision;
    /**
     *  Lower/upper report limits.
     */
    private double   rptLim[]={0d,0d,0d};
    /**
     * Units of reported variable.
     */
    private String   units;

    /**
     * Init field name, precision, report limit and state.
     * @param name Field name.
     */
    public Field(String name) {
        this.name = name;
        enabled = false;
        precision = 2;
        setRptLim(RangeType.LOW, Constants.BIG * Constants.BIG);
        setRptLim(RangeType.HI, -Constants.BIG * Constants.BIG);
    }

    public String getName() {
        return name;
    }

    public int getPrecision() {
        return precision;
    }

    public double getRptLim(RangeType type) {
        return rptLim[type.id];
    }

    public String getUnits() {
        return units;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public void setRptLim(RangeType type, double rptLim) {
        this.rptLim[type.id] = rptLim;
    }

    public void setUnits(String units) {
        this.units = units;
    }
}
