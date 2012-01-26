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

package org.addition.epanet.network;

import org.addition.epanet.util.ENException;
import org.addition.epanet.network.io.Keywords;
import org.addition.epanet.network.structures.*;
import org.addition.epanet.util.Utilities;

import java.util.*;

/**
 * Hydraulic network structure.
 */
public class Network {

    /**
     * Available files types.
     */
    public enum FileType {
        EXCEL_FILE,
        INP_FILE,
        NULL_FILE,
        XML_FILE,
        XML_GZ_FILE,
    }

    /**
     * Available section types.
     */
    static public enum SectType {
        BACKDROP(26, Keywords.s_BACKDROP, Keywords.t_BACKDROP),
        CONTROLS(7, Keywords.s_CONTROLS, Keywords.t_CONTROL),
        COORDS(23, Keywords.s_COORDS, Keywords.t_COORD),
        CURVES(13, Keywords.s_CURVES, Keywords.t_CURVE),
        DEMANDS(9, Keywords.s_DEMANDS, Keywords.t_DEMAND),
        EMITTERS(11, Keywords.s_EMITTERS, Keywords.t_EMITTER),
        END(28, Keywords.s_END, Keywords.t_END),
        ENERGY(17, Keywords.s_ENERGY, Keywords.t_ENERGY),
        JUNCTIONS(1, Keywords.s_JUNCTIONS, Keywords.t_JUNCTION),
        LABELS(25, Keywords.s_LABELS, Keywords.t_LABEL),
        MIXING(19, Keywords.s_MIXING, Keywords.t_MIXING),
        OPTIONS(22, Keywords.s_OPTIONS, Keywords.t_OPTION),
        PATTERNS(12, Keywords.s_PATTERNS, Keywords.t_PATTERN),
        PIPES(4, Keywords.s_PIPES, Keywords.t_PIPE),
        PUMPS(5, Keywords.s_PUMPS, Keywords.t_PUMP),
        QUALITY(14, Keywords.s_QUALITY, Keywords.t_QUALITY),
        REACTIONS(18, Keywords.s_REACTIONS, Keywords.t_REACTION),
        REPORT(20, Keywords.s_REPORT, Keywords.t_REPORT),
        RESERVOIRS(2, Keywords.s_RESERVOIRS, Keywords.t_RESERVOIR),
        ROUGHNESS(16, Keywords.s_ROUGHNESS, Keywords.t_ROUGHNESS),
        RULES(8, Keywords.s_RULES, Keywords.t_RULE),
        SOURCES(10, Keywords.s_SOURCES, Keywords.t_SOURCE),
        STATUS(15, Keywords.s_STATUS, Keywords.t_STATUS),
        TAGS(27, Keywords.s_TAGS, Keywords.t_TAG),
        TANKS(3, Keywords.s_TANKS, Keywords.t_TANK),
        TIMES(21, Keywords.s_TIMES, Keywords.t_TIME),
        TITLE(0, Keywords.s_TITLE, Keywords.t_TITLE),
        VALVES(6, Keywords.s_VALVES, Keywords.t_VALVE),
        VERTICES(24, Keywords.s_VERTICES, Keywords.t_VERTICE);

        /**
         * Get section type from string.
         *
         * @param text Parse string.
         * @return Section type.
         */
        public static SectType parse(String text) {
            for (SectType type : SectType.values())
                if (Utilities.match(text, type.parseStr)) return type;
            return null;
        }
        /**
         * Sequencial id.
         */
        public final int id;
        /**
         * Parse string id.
         */
        public final String parseStr;

        /**
         * Report string id.
         */
        public final String reportStr;

        private SectType(int id, String text) {
            this.id = id;
            this.parseStr = text;
            this.reportStr = "";
        }

        private SectType(int id, String text, String repStr) {
            this.id = id;
            this.parseStr = text;
            this.reportStr = repStr;
        }
    }

    private List<Control> controls;
    private Map<String, Curve> curves;
    /**
     * Fields map with report variables properties and conversion units.
     */
    private transient FieldsMap fields;
    /**
     * Transient colleciton of junctions.
     */
    private transient Map<String, Node> junctions;
    private List<Label> labels;
    private Map<String, Link> links;
    private Map<String, Node> nodes;
    private Map<String, Pattern> patterns;

    /**
     * Properties Map with simulation configuration properties.
     */
    private PropertiesMap properties;

    /**
     * Transient colleciton of pumps.
     */
    private transient Map<String, Pump> pumps;

    private Map<String, Rule> rules;
    /**
     * Transient collection of tanks(and reservoirs)
     */
    private transient Map<String, Tank> tanks;
    private List<String> titleText;
    /**
     * Transient colleciton of valves.
     */
    private transient Map<String, Valve> valves;


    public Network() {
        titleText = new ArrayList<String>();
        patterns = new LinkedHashMap<String, Pattern>();
        nodes = new LinkedHashMap<String, Node>();
        links = new LinkedHashMap<String, Link>();

        curves = new LinkedHashMap<String, Curve>();
        controls = new ArrayList<Control>();
        labels = new ArrayList<Label>();
        rules = new LinkedHashMap<String, Rule>();

        tanks = null;
        pumps = null;
        valves = null;

        fields = new FieldsMap();

        properties = new PropertiesMap();

        addPattern("", new Pattern());
    }

    public void addControl(Control ctr) {
        controls.add(ctr);
    }


    public void addCurve(String s, Curve cur) {
        cur.setId(s);
        curves.put(s, cur);
    }

    public void addJunction(String id, Node junc) {
        junc.setId(id);
        nodes.put(id, junc);
    }

    public void addPattern(String s, Pattern pat) {
        pat.setId(s);
        patterns.put(s, pat);
    }

    public void addPipe(String id, Link linkRef) {
        linkRef.setId(id);
        links.put(id, linkRef);
    }

    public void addPump(String id, Pump pump) {
        pump.setId(id);
        if (pumps == null)
            pumps = new LinkedHashMap<String, Pump>();
        pumps.put(id, pump);
        links.put(id, pump);
    }

    public void addRule(Rule r) {
        rules.put(r.getLabel(), r);
    }


    public void addTank(String id, Tank tank) {
        tank.setId(id);
        if (tanks == null)
            tanks = new LinkedHashMap<String, Tank>();
        tanks.put(id, tank);
        nodes.put(id, tank);
    }

    public void addValve(String id, Valve valve) {
        valve.setId(id);
        if (valves == null)
            valves = new LinkedHashMap<String, Valve>();
        valves.put(id, valve);
        links.put(id, valve);
    }

    public Control[] getControls() {
        return controls.toArray(new Control[controls.size()]);
    }

    public Curve getCurve(String name) {
        Object obj = curves.get(name);
        if (obj == null)
            return null;
        else {
            return (Curve) obj;
        }
    }

    public Collection<Curve> getCurves() {
        return curves.values();
    }

    public FieldsMap getFieldsMap() {
        return fields;
    }

    public Collection<Node> getJunctions() {
        if (junctions == null) {
            Map<String, Node> tempJunc = new LinkedHashMap<String, Node>();
            for (Node n : nodes.values()) {
                if (!(n instanceof Tank))
                    tempJunc.put(n.getId(), n);
            }
            if (tempJunc.size() == 0)
                return new ArrayList<Node>();
            else
                junctions = tempJunc;
        }
        return junctions.values();
    }

    public List<Label> getLabels() {
        return labels;
    }

    public Link getLink(String name) {
        Object obj = links.get(name);
        if (obj == null)
            return null;
        else {
            return (Link) obj;
        }
    }

    public Collection<Link> getLinks() {
        return links.values();
    }

    public Node getNode(String name) {
        Object obj = nodes.get(name);
        if (obj == null)
            return null;
        else {
            return (Node) obj;
        }
    }

    public Collection<Node> getNodes() {
        return nodes.values();
    }

    public Pattern getPattern(String name) {
        Object obj = patterns.get(name);
        if (obj == null)
            return null;
        else {
            return (Pattern) obj;
        }
    }

    public Collection<Pattern> getPatterns() {
        return patterns.values();
    }

    public PropertiesMap getPropertiesMap() {
        return properties;
    }

    public Collection<Pump> getPumps() {
        if (pumps == null) {
            Map<String, Pump> tempPump = new LinkedHashMap<String, Pump>();
            for (Link l : links.values()) {
                if (l instanceof Pump)
                    tempPump.put(l.getId(), (Pump) l);
            }
            if (tempPump.size() == 0)
                return new ArrayList<Pump>();
            else
                pumps = tempPump;
        }
        return pumps.values();
    }

    public Rule getRule(String ruleName) {
        Object obj = rules.get(ruleName);
        if (obj == null)
            return null;
        else {
            return (Rule) obj;
        }
    }

    public Collection<Rule> getRules() {
        return rules.values();
    }

    public Collection<Tank> getTanks() {
        if (tanks == null) {
            Map<String, Tank> tempTank = new LinkedHashMap<String, Tank>();
            for (Node n : nodes.values()) {
                if (n instanceof Tank)
                    tempTank.put(n.getId(), (Tank) n);
            }
            if (tempTank.size() == 0)
                return new ArrayList<Tank>();
            else
                tanks = tempTank;
        }
        return tanks.values();
    }

    public List<String> getTitleText() {
        return titleText;
    }

    public Collection<Valve> getValves() {
        if (valves == null) {
            Map<String, Valve> tempValve = new LinkedHashMap<String, Valve>();
            for (Link l : links.values()) {
                if (l instanceof Valve)
                    tempValve.put(l.getId(), (Valve) l);
            }
            if (tempValve.size() == 0)
                return new ArrayList<Valve>();
            else
                valves = tempValve;
        }
        return valves.values();
    }

    private Object readResolve() throws ENException {
        updatedUnitsProperty();
        return this;
    }

    public void updatedUnitsProperty() throws ENException {
        fields = new FieldsMap();
        fields.prepare(getPropertiesMap().getUnitsflag(),
                getPropertiesMap().getFlowflag(),
                getPropertiesMap().getPressflag(),
                getPropertiesMap().getQualflag(),
                getPropertiesMap().getChemUnits(),
                getPropertiesMap().getSpGrav(),
                getPropertiesMap().getHstep());
    }

    public String toString() {
        String res = " Network\n";
        res += "  Nodes : " + nodes.size() + "\n";
        res += "  Links : " + links.size() + "\n";
        res += "  Pattern : " + patterns.size() + "\n";
        res += "  Curves : " + curves.size() + "\n";
        res += "  Controls : " + controls.size() + "\n";
        res += "  Labels : " + labels.size() + "\n";
        res += "  Rules : " + rules.size() + "\n";
        if (tanks != null) res += "  Tanks : " + tanks.size() + "\n";
        if (pumps != null) res += "  Pumps : " + pumps.size() + "\n";
        if (valves != null) res += "  Valves : " + valves.size() + "\n";
        return res;
    }

//    public Node getNodeByIndex(int idx) {
//        return  new ArrayList<Node>(nodes.values()).get(idx);
//    }
//    public Link getLinkByIndex(int idx) {
//        return  new ArrayList<Link>(links.values()).get(idx);
//    }
}
