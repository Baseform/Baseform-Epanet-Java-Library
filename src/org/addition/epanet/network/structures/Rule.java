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
 * Rule source code class.
 */
public class Rule {
    /**
     * Rule object types.
     */
    static public enum Objects{
        r_JUNC      (Keywords.wr_JUNC  ),
        r_LINK      (Keywords.wr_LINK  ),
        r_NODE      (Keywords.wr_NODE  ),
        r_PIPE      (Keywords.wr_PIPE  ),
        r_PUMP      (Keywords.wr_PUMP  ),
        r_RESERV    (Keywords.wr_RESERV),
        r_SYSTEM    (Keywords.wr_SYSTEM),
        r_TANK      (Keywords.wr_TANK  ),
        r_VALVE     (Keywords.wr_VALVE );

        public static Objects parse(String text) {
            for (Objects type : Objects.values())
                if (Utilities.match(text, type.parseStr)) return type;
            return null;
        }
        public final String parseStr;

        private Objects(String text) {
            this.parseStr = text;
        }
    }

    /**
     * Rule operators.
     */
    static public enum Operators {
        ABOVE(Keywords.wr_ABOVE),
        BELOW(Keywords.wr_BELOW),
        EQ("="),
        GE(">="),
        GT(">"),
        IS(Keywords.wr_IS),
        LE("<="),
        LT("<"),
        NE("<>"),
        NOT(Keywords.wr_NOT);
        public static Operators parse(String text) {
            for (Operators type : Operators.values())
                if (text.equalsIgnoreCase(type.parseStr)) return type;
            return null;
        }
        public final String parseStr;

        private Operators(String text) {
            this.parseStr = text;
        }
    }

    /**
     * Rule statements.
     */
    static public enum Rulewords{
        r_AND       (Keywords.wr_AND     ),
        r_ELSE      (Keywords.wr_ELSE    ),
        r_ERROR     (""   ),
        r_IF        (Keywords.wr_IF      ),
        r_OR        (Keywords.wr_OR      ),
        r_PRIORITY  (Keywords.wr_PRIORITY),
        r_RULE      (Keywords.wr_RULE    ),
        r_THEN      (Keywords.wr_THEN    );

        public static Rulewords parse(String text) {
            for (Rulewords type : Rulewords.values())
                if (Utilities.match(text, type.parseStr)) return type;
            return null;
        }

        public final String parseStr;

        private Rulewords(String text) {
            this.parseStr = text;
        }
    }

    /**
     * Rule values types.
     */
    static public enum Values{
        IS_ACTIVE   (3,Keywords.wr_ACTIVE),
        IS_CLOSED   (2,Keywords.wr_CLOSED),
        IS_NUMBER   (0,"XXXX"),
        IS_OPEN     (1,Keywords.wr_OPEN);

        public static Values parse(String text) {
            for (Values type : Values.values())
                if (Utilities.match(text, type.parseStr)) return type;
            return null;
        }
        public final int id;

        public final String parseStr;

        private Values(int id,String text) {
            this.id = id;
            this.parseStr = text;
        }
    }

    /**
     * Rule variables.
     */
    static public enum Varwords{
        r_CLOCKTIME(Keywords.wr_CLOCKTIME),
        r_DEMAND   (Keywords.wr_DEMAND   ),
        r_DRAINTIME(Keywords.wr_DRAINTIME ),
        r_FILLTIME (Keywords.wr_FILLTIME ),
        r_FLOW     (Keywords.wr_FLOW     ),
        r_GRADE    (Keywords.wr_GRADE    ),
        r_HEAD     (Keywords.wr_HEAD     ),
        r_LEVEL    (Keywords.wr_LEVEL    ),
        r_POWER    (Keywords.wr_POWER    ),
        r_PRESSURE (Keywords.wr_PRESSURE ),
        r_SETTING  (Keywords.wr_SETTING  ),
        r_STATUS   (Keywords.wr_STATUS   ),
        r_TIME     (Keywords.wr_TIME     );

        public static Varwords parse(String text) {
            for (Varwords type : Varwords.values())
                if (Utilities.match(text, type.parseStr)) return type;
            return null;
        }

        public final String parseStr;

        private Varwords(String text) {
            this.parseStr = text;
        }
    }

    private String code;

    private String label;
    public Rule() {
        label = "";
        code = "";
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setLabel(String label) {
        this.label = label;
    }

}
