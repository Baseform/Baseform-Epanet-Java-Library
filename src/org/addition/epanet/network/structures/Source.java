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

import org.addition.epanet.network.io.Keywords;
import org.addition.epanet.util.Utilities;

/**
 * Water quality object, source quality.
 */
public class Source
{
    /**
     * Source type
      */
    static public enum Type{
        /**
         * Inflow concentration.
         */
        CONCEN      (0,Keywords.w_CONCEN),
        /**
         * Flow paced booster.
         */
        FLOWPACED   (3,Keywords.w_FLOWPACED),
        /**
         * Mass inflow booster.
         */
        MASS        (1,Keywords.w_MASS),
        /**
         * Setpoint booster.
         */
        SETPOINT    (2,Keywords.w_SETPOINT);

        public static Type parse(String text){
            for (Type type : Type.values())
                if (Utilities.match(text, type.parseStr)) return type;
            return null;
        }

        /**
         * Sequencial Id.
         */
        public final int id;

        /**
         * Parse string id.
         */
        public final String parseStr;

        private Type(int val, String str){id = val;parseStr = str;}
    }

    /**
     * Base concentration.
     */
    private double C0;
    /**
     *  Time pattern reference.
     */
    private Pattern pattern;

    /**
     * Source type.
     */
    private Type type;

    public double getC0(){
        return C0;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public Type getType() {
        return type;
    }

    public void setC0(double c0) {
        C0 = c0;
    }


    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public void setType(Type type) {
        this.type = type;
    }
}