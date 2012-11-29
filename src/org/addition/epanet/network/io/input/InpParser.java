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

package org.addition.epanet.network.io.input;


import java.io.*;
import java.util.ArrayList;
import java.util.List;


import org.addition.epanet.Constants;
import org.addition.epanet.network.io.Keywords;
import org.addition.epanet.util.ENException;
import org.addition.epanet.util.ENLevels;
import org.addition.epanet.util.Utilities;

import java.util.logging.Logger;

import org.addition.epanet.network.FieldsMap;
import org.addition.epanet.network.Network;
import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.network.structures.*;
import org.addition.epanet.network.FieldsMap.*;
import org.addition.epanet.network.structures.Control.*;
import org.addition.epanet.network.structures.Link.*;

/**
 * INP parser class.
 */
public class InpParser extends InputParser {


    private Rule.Rulewords ruleState;    // Last rule op
    private Rule currentRule;       // Current rule
    private static final String[] OPTION_VALUE_KEYWORDS = new String[]{
            Keywords.w_TOLERANCE, Keywords.w_DIFFUSIVITY, Keywords.w_DAMPLIMIT, Keywords.w_VISCOSITY, Keywords.w_SPECGRAV, Keywords.w_TRIALS, Keywords.w_ACCURACY,
            Keywords.w_HTOL, Keywords.w_QTOL, Keywords.w_RQTOL, Keywords.w_CHECKFREQ, Keywords.w_MAXCHECK, Keywords.w_EMITTER, Keywords.w_DEMAND
    };

    public InpParser(Logger log) {
        super(log);

        currentRule = null;
        ruleState = null;
    }

    // Parse demands and time patterns first.
    private void parsePC(Network net, File f) throws ENException {
        int errSum = 0;
        int lineCount = 0;
        Network.SectType sectionType = null;
        String line;
        BufferedReader buffReader;

        try {
            buffReader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "ISO-8859-1"));
        } catch (IOException e) {
            throw new ENException(302);
        }

        try {
            while ((line = buffReader.readLine()) != null) {
                lineCount++;

                line = line.trim();

                if (line.startsWith("[")) {
                    if (line.startsWith("[PATTERNS]")) {
                        sectionType = Network.SectType.PATTERNS;
                    } else if (line.startsWith("[CURVES]"))
                        sectionType = Network.SectType.CURVES;
                    else
                        sectionType = null;
                    continue;
                }

                if (sectionType != null) {
                    if (line.indexOf(";") >= 0)
                        line = line.substring(0, line.indexOf(";"));

                    if (line.length() == 0)
                        continue;

                    String[] tokens = line.split("[ \t]+");
                    if (tokens.length == 0) continue;

                    try {
                        if (sectionType == Network.SectType.PATTERNS) {
                            parsePattern(net, tokens);
                        } else if (sectionType == Network.SectType.CURVES)
                            parseCurve(net, tokens);
                    } catch (ENException e) {
                        log.log(ENLevels.ERROR, "parsePC", new ENException(e, sectionType.reportStr, tokens[0]));
                        errSum++;
                    }
                }

                if (errSum == Constants.MAXERRS) break;
            }
        } catch (IOException e) {
            throw new ENException(302);
        }

        if (errSum > 0)
            throw new ENException(200);

        try {
            buffReader.close();
        } catch (IOException e) {
            throw new ENException(302);
        }
    }

    // Parse INP file
    public Network parse(Network net, File f) throws ENException {

        parsePC(net, f);

        int errSum = 0;
        //int lineCount = 0;
        Network.SectType sectionType = null;
        String line;
        BufferedReader buffReader;

        try {
            buffReader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "ISO-8859-1"));
        } catch (IOException e) {
            throw new ENException(302);
        }

        try {
            while ((line = buffReader.readLine()) != null) {
                String comment = "";

                if (line.indexOf(";") >= 0) {
                    int index = line.indexOf(";");
                    if (index > 0)
                        comment = (line.substring(line.indexOf(";") + 1)).trim();

                    line = line.substring(0, index);
                }


                //lineCount++;
                line = line.trim();
                if (line.length() == 0)
                    continue;

                String[] tokens = line.split("[ \t]+");
                if (tokens.length == 0) continue;

                try {

                    if (tokens[0].contains("[")) {
                        Network.SectType type = findSectionType(tokens[0]);
                        if (type != null)
                            sectionType = type;
                        else {
                            sectionType = null;
                            log.log(ENLevels.ERROR, String.format("Unknown section type : %s", tokens[0]));
                            //throw new ENException(201, lineCount);
                        }
                    } else if (sectionType != null) {

                        switch (sectionType) {
                            case TITLE:
                                net.getTitleText().add(line);
                                break;
                            case JUNCTIONS:
                                parseJunction(net, tokens, comment);
                                break;

                            case RESERVOIRS:
                            case TANKS:
                                parseTank(net, tokens, comment);
                                break;

                            case PIPES:
                                parsePipe(net, tokens, comment);
                                break;
                            case PUMPS:
                                parsePump(net, tokens, comment);
                                break;
                            case VALVES:
                                parseValve(net, tokens, comment);
                                break;
                            case CONTROLS:
                                parseControl(net, tokens);
                                break;

                            case RULES:
                                parseRule(net, tokens, line);
                                break;

                            case DEMANDS:
                                parseDemand(net, tokens);
                                break;
                            case SOURCES:
                                parseSource(net, tokens);
                                break;
                            case EMITTERS:
                                parseEmitter(net, tokens);
                                break;
                            case QUALITY:
                                parseQuality(net, tokens);
                                break;
                            case STATUS:
                                parseStatus(net, tokens);
                                break;
                            case ENERGY:
                                parseEnergy(net, tokens);
                                break;
                            case REACTIONS:
                                parseReact(net, tokens);
                                break;
                            case MIXING:
                                parseMixing(net, tokens);
                                break;
                            case REPORT:
                                parseReport(net, tokens);
                                break;
                            case TIMES:
                                parseTime(net, tokens);
                                break;
                            case OPTIONS:
                                parseOption(net, tokens);
                                break;
                            case COORDS:
                                parseCoordinate(net, tokens);
                                break;
                            case VERTICES:
                                parseVertice(net, tokens);
                                break;
                            case LABELS:
                                parseLabel(net, tokens);
                                break;
                            default:
                                break;
                        }
                    }
                } catch (ENException e) {
                    logException(sectionType, e, line, tokens);
                    errSum++;
                }
                if (errSum == Constants.MAXERRS) break;
            }
        } catch (IOException e) {
            throw new ENException(302);
        }

        if (errSum > 0) {
            throw new ENException(200);
        }

        try {
            buffReader.close();
        } catch (IOException e) {
            throw new ENException(302);
        }

        adjust(net);
        net.getFieldsMap().prepare(net.getPropertiesMap().getUnitsflag(),
                net.getPropertiesMap().getFlowflag(),
                net.getPropertiesMap().getPressflag(),
                net.getPropertiesMap().getQualflag(),
                net.getPropertiesMap().getChemUnits(),
                net.getPropertiesMap().getSpGrav(),
                net.getPropertiesMap().getHstep());
        convert(net);
        return net;
    }

    protected void logException(Network.SectType type, ENException e, String line, String[] tokens) {
        if (type == Network.SectType.OPTIONS) {
            log.log(ENLevels.ERROR, "logException", new ENException(e, type.reportStr, line));
        } else
            log.log(ENLevels.ERROR, "logException", new ENException(e, type.reportStr, tokens[0]));
    }

    protected void parseJunction(Network net, String[] Tok, String comment) throws ENException {
        int n = Tok.length;
        Double el, y = 0.0d;
        Pattern p = null;

        Node nodeRef = new Node();

        if (net.getNode(Tok[0]) != null)
            throw new ENException(215, Network.SectType.JUNCTIONS, Tok[0]);

        net.addJunction(Tok[0], nodeRef);

        if (n < 2)
            throw new ENException(201);

        if ((el = Utilities.getDouble(Tok[1])) == null)
            throw new ENException(202, Network.SectType.JUNCTIONS, Tok[0]);

        if (n >= 3 && (y = Utilities.getDouble(Tok[2])) == null)
            throw new ENException(202, Network.SectType.JUNCTIONS, Tok[0]);

        if (n >= 4) {
            p = net.getPattern(Tok[3]);
            if (p == null)
                throw new ENException(205);
        }

        nodeRef.setElevation(el);
        nodeRef.setC0(new double[]{0.0});
        nodeRef.setSource(null);
        nodeRef.setKe(0.0);
        nodeRef.setReportFlag(false);

        if (comment.length() > 0)
            nodeRef.setComment(comment);

        if (n >= 3) {
            Demand demand = new Demand(y, p);
            nodeRef.getDemand().add(demand);

            nodeRef.setInitDemand(y);
        } else
            nodeRef.setInitDemand(Constants.MISSING);
    }


    protected void parseTank(Network net, String[] Tok, String comment) throws ENException {
        int n = Tok.length;
        Pattern p = null;
        Curve c = null;
        Double el,
                initlevel = 0.0d,
                minlevel = 0.0d,
                maxlevel = 0.0d,
                minvol = 0.0d,
                diam = 0.0d,
                area;

        Tank tank = new Tank();
        if (comment.length() > 0)
            tank.setComment(comment);

        if (net.getNode(Tok[0]) != null)
            throw new ENException(215);

        net.addTank(Tok[0], tank);

        if (n < 2)
            throw new ENException(201);

        if ((el = Utilities.getDouble(Tok[1])) == null)
            throw new ENException(202);

        if (n <= 3) {
            if (n == 3) {
                p = net.getPattern(Tok[2]);
                if (p == null)
                    throw new ENException(205);
            }
        } else if (n < 6)
            throw new ENException(201);
        else {
            if ((initlevel = Utilities.getDouble(Tok[2])) == null)
                throw new ENException(202, Network.SectType.TANKS, Tok[0]);
            if ((minlevel = Utilities.getDouble(Tok[3])) == null)
                throw new ENException(202, Network.SectType.TANKS, Tok[0]);
            if ((maxlevel = Utilities.getDouble(Tok[4])) == null)
                throw new ENException(202, Network.SectType.TANKS, Tok[0]);
            if ((diam = Utilities.getDouble(Tok[5])) == null)
                throw new ENException(202, Network.SectType.TANKS, Tok[0]);

            if (diam < 0.0)
                throw new ENException(202, Network.SectType.TANKS, Tok[0]);

            if (n >= 7
                    && (minvol = Utilities.getDouble(Tok[6])) == null)
                throw new ENException(202, Network.SectType.TANKS, Tok[0]);

            if (n == 8) {
                c = net.getCurve(Tok[7]);
                if (c == null) throw new ENException(202, Network.SectType.TANKS, Tok[0]);
            }
        }

        tank.setReportFlag(false);
        tank.setElevation(el);
        tank.setC0(new double[]{0.0d});
        tank.setSource(null);
        tank.setKe(0.0);

        tank.setH0(initlevel);
        tank.setHmin(minlevel);
        tank.setHmax(maxlevel);
        tank.setArea(diam);
        tank.setPattern(p);
        tank.setKb(Constants.MISSING);

        area = Constants.PI * diam * diam / 4.0d;

        tank.setVmin(area * minlevel);
        if (minvol > 0.0)
            tank.setVmin(minvol);

        tank.setV0(tank.getVmin() + area * (initlevel - minlevel));
        tank.setVmax(tank.getVmin() + area * (maxlevel - minlevel));

        tank.setVcurve(c);
        tank.setMixModel(Tank.MixType.MIX1);
        tank.setV1max(1.0);
    }


    protected void parsePipe(Network net, String[] Tok, String comment) throws ENException {
        Node j1, j2;
        int n = Tok.length;
        LinkType type = LinkType.PIPE;
        StatType status = StatType.OPEN;
        Double length, diam, rcoeff, lcoeff = 0.0d;

        Link link = new Link();

        if (net.getLink(Tok[0]) != null)
            throw new ENException(215);

        net.addPipe(Tok[0], link);

        if (n < 6)
            throw new ENException(201);

        if ((j1 = net.getNode(Tok[1])) == null ||
                (j2 = net.getNode(Tok[2])) == null
                ) throw new ENException(203);


        if (j1 == j2) throw new ENException(222);

        if ((length = Utilities.getDouble(Tok[3])) == null ||
                (diam = Utilities.getDouble(Tok[4])) == null ||
                (rcoeff = Utilities.getDouble(Tok[5])) == null
                ) throw new ENException(202);


        if (length <= 0.0 || diam <= 0.0 || rcoeff <= 0.0) throw new ENException(202);

        if (n == 7) {
            if (Utilities.match(Tok[6], LinkType.CV.parseStr)) type = LinkType.CV;
            else if (Utilities.match(Tok[6], StatType.CLOSED.parseStr)) status = StatType.CLOSED;
            else if (Utilities.match(Tok[6], StatType.OPEN.parseStr)) status = StatType.OPEN;
            else if ((lcoeff = Utilities.getDouble(Tok[6])) == null) throw new ENException(202);
        }

        if (n == 8) {
            if ((lcoeff = Utilities.getDouble(Tok[6])) == null) throw new ENException(202);
            if (Utilities.match(Tok[7], LinkType.CV.parseStr)) type = LinkType.CV;
            else if (Utilities.match(Tok[7], StatType.CLOSED.parseStr)) status = StatType.CLOSED;
            else if (Utilities.match(Tok[7], StatType.OPEN.parseStr)) status = StatType.OPEN;
            else
                throw new ENException(202);
        }

        if (lcoeff < 0.0) throw new ENException(202);

        link.setFirst(j1);
        link.setSecond(j2);
        link.setLenght(length);
        link.setDiameter(diam);
        link.setRoughness(rcoeff);
        link.setKm(lcoeff);
        link.setKb(Constants.MISSING);
        link.setKw(Constants.MISSING);
        link.setType(type);
        link.setStatus(status);
        link.setReportFlag(false);
        if (comment.length() > 0)
            link.setComment(comment);
    }


    protected void parsePump(Network net, String[] Tok, String comment) throws ENException {
        int j, m, n = Tok.length;
        Node j1, j2;
        Double y;
        Double[] X = new Double[6];

        Pump pump = new Pump();

        if (net.getLink(Tok[0]) != null)
            throw new ENException(215);

        net.addPump(Tok[0], pump);

        if (n < 4)
            throw new ENException(201);
        if ((j1 = net.getNode(Tok[1])) == null || (j2 = net.getNode(Tok[2])) == null) throw new ENException(203);
        if (j1 == j2) throw new ENException(222);

        // Link attributes
        pump.setFirst(j1);
        pump.setSecond(j2);
        pump.setDiameter(0);
        pump.setLenght(0.0d);
        pump.setRoughness(1.0d);
        pump.setKm(0.0d);
        pump.setKb(0.0d);
        pump.setKw(0.0d);
        pump.setType(LinkType.PUMP);
        pump.setStatus(StatType.OPEN);
        pump.setReportFlag(false);

        // Pump attributes
        pump.setPtype(Pump.Type.NOCURVE);
        pump.setHcurve(null);
        pump.setEcurve(null);
        pump.setUpat(null);
        pump.setEcost(0.0d);
        pump.setEpat(null);
        if (comment.length() > 0) pump.setComment(comment);

        if ((X[0] = Utilities.getDouble(Tok[3])) != null) {

            m = 1;
            for (j = 4; j < n; j++) {
                if ((X[m] = Utilities.getDouble(Tok[j])) == null) throw new ENException(202);
                m++;
            }
            getpumpcurve(Tok, pump, m, X);
            return; /* If 4-th token is a number then input follows Version 1.x format  so retrieve pump curve parameters */

        }

        m = 4;
        while (m < n) {

            if (Utilities.match(Tok[m - 1], Keywords.w_POWER)) {
                y = Double.parseDouble(Tok[m]);
                if (y <= 0.0) throw new ENException(202);
                pump.setPtype(Pump.Type.CONST_HP);
                pump.setKm(y);
            } else if (Utilities.match(Tok[m - 1], Keywords.w_HEAD)) {
                Curve t = net.getCurve(Tok[m]);
                if (t == null) throw new ENException(206);
                pump.setHcurve(t);
            } else if (Utilities.match(Tok[m - 1], Keywords.w_PATTERN)) {
                Pattern p = net.getPattern(Tok[m]);
                if (p == null) throw new ENException(205);
                pump.setUpat(p);
            } else if (Utilities.match(Tok[m - 1], Keywords.w_SPEED)) {
                if ((y = Utilities.getDouble(Tok[m])) == null) throw new ENException(202);
                if (y < 0.0) throw new ENException(202);
                pump.setRoughness(y);
            } else
                throw new ENException(201);
            m = m + 2;
        }
    }


    protected void parseValve(Network net, String[] Tok, String comment) throws ENException {
        Node j1, j2;
        int n = Tok.length;
        StatType status = StatType.ACTIVE;
        LinkType type;

        double diam, setting = 0.0, lcoeff = 0.0;

        Valve valve = new Valve();

        if (net.getLink(Tok[0]) != null)
            throw new ENException(215);

        net.addValve(Tok[0], valve);

        if (n < 6) throw new ENException(201);
        if ((j1 = net.getNode(Tok[1])) == null ||
                (j2 = net.getNode(Tok[2])) == null
                ) throw new ENException(203);

        if (j1 == j2)
            throw new ENException(222);

        //if (Utilities.match(Tok[4], Keywords.w_PRV)) type = LinkType.PRV;
        //else if (Utilities.match(Tok[4], Keywords.w_PSV)) type = LinkType.PSV;
        //else if (Utilities.match(Tok[4], Keywords.w_PBV)) type = LinkType.PBV;
        //else if (Utilities.match(Tok[4], Keywords.w_FCV)) type = LinkType.FCV;
        //else if (Utilities.match(Tok[4], Keywords.w_TCV)) type = LinkType.TCV;
        //else if (Utilities.match(Tok[4], Keywords.w_GPV)) type = LinkType.GPV;
        if ((type = LinkType.parse(Tok[4])) == null)
            throw new ENException(201);

        try {
            diam = Double.parseDouble(Tok[3]);
        } catch (NumberFormatException ex) {
            throw new ENException(202);
        }
        if (diam <= 0.0) throw new ENException(202);

        if (type == LinkType.GPV) {
            Curve t;
            if ((t = net.getCurve(Tok[5])) == null)
                throw new ENException(206);

            List<Curve> curv = new ArrayList<Curve>(net.getCurves());
            setting = curv.indexOf(t);
            log.warning("GPV Valve, index as roughness !");
            valve.setCurve(t);
            status = StatType.OPEN;
        } else
            try {
                setting = Double.parseDouble(Tok[5]);
            } catch (NumberFormatException ex) {
                throw new ENException(202);
            }

        if (n >= 7)
            try {
                lcoeff = Double.parseDouble(Tok[6]);
            } catch (NumberFormatException ex) {
                throw new ENException(202);
            }


        if ((j1 instanceof Tank || j2 instanceof Tank) &&
                (type == LinkType.PRV || type == LinkType.PSV || type == LinkType.FCV))
            throw new ENException(219);

        if (!valvecheck(net, type, j1, j2))
            throw new ENException(220);


        valve.setFirst(j1);
        valve.setSecond(j2);
        valve.setDiameter(diam);
        valve.setLenght(0.0d);
        valve.setRoughness(setting);
        valve.setKm(lcoeff);
        valve.setKb(0.0d);
        valve.setKw(0.0d);
        valve.setType(type);
        valve.setStatus(status);
        valve.setReportFlag(false);
        if (comment.length() > 0)
            valve.setComment(comment);
    }

    protected boolean valvecheck(Network net, LinkType type, Node j1, Node j2) {
        LinkType vtype;
        // Examine each existing valve
        for (Valve vk : net.getValves()) {
            Node vj1 = vk.getFirst();
            Node vj2 = vk.getSecond();
            vtype = vk.getType();

            if (vtype == LinkType.PRV && type == LinkType.PRV) {
                if (vj2 == j2 ||
                        vj2 == j1 ||
                        vj1 == j2) return (false);
            }

            if (vtype == LinkType.PSV && type == LinkType.PSV) {
                if (vj1 == j1 ||
                        vj1 == j2 ||
                        vj2 == j1) return (false);
            }

            if (vtype == LinkType.PSV && type == LinkType.PRV && vj1 == j2) return (false);
            if (vtype == LinkType.PRV && type == LinkType.PSV && vj2 == j1) return (false);

            if (vtype == LinkType.FCV && type == LinkType.PSV && vj2 == j1) return (false);
            if (vtype == LinkType.FCV && type == LinkType.PRV && vj1 == j2) return (false);

            if (vtype == LinkType.PSV && type == LinkType.FCV && vj1 == j2) return (false);
            if (vtype == LinkType.PRV && type == LinkType.FCV && vj2 == j1) return (false);
        }
        return (true);
    }

    protected void getpumpcurve(String Tok[], Pump pump, int n, Double[] X) throws ENException {
        double h0, h1, h2, q1, q2;

        if (n == 1) {
            if (X[0] <= 0.0) throw new ENException(202);
            pump.setPtype(Pump.Type.CONST_HP);
            pump.setKm(X[0]);
        } else {
            if (n == 2) {
                q1 = X[1];
                h1 = X[0];
                h0 = 1.33334 * h1;
                q2 = 2.0 * q1;
                h2 = 0.0;
            } else if (n >= 5) {
                h0 = X[0];
                h1 = X[1];
                q1 = X[2];
                h2 = X[3];
                q2 = X[4];
            } else throw new ENException(202);
            pump.setPtype(Pump.Type.POWER_FUNC);
            double[] a = new double[1], b = new double[1], c = new double[1];
            if (!Utilities.getPowerCurve(h0, h1, h2, q1, q2, a, b, c))
                throw new ENException(206);

            pump.setH0(-a[0]);
            pump.setFlowCoefficient(-b[0]);
            pump.setN(c[0]);
            pump.setQ0(q1);
            pump.setQmax(Math.pow((-a[0] / b[0]), (1.0 / c[0])));
            pump.setHmax(h0);
        }
    }

    protected void parsePattern(Network net, String[] tok) throws ENException {
        Pattern pat;

        if (net.getPatterns().size() == 0) {
            pat = new Pattern();
            net.addPattern(tok[0], pat);
        } else {
            pat = net.getPattern(tok[0]);

            if (pat == null) {
                pat = new Pattern();
                net.addPattern(tok[0], pat);
            }
        }

        for (int i = 1; i < tok.length; i++) {
            Double x;
            if ((x = Utilities.getDouble(tok[i])) != null)
                pat.add(x);
            else
                throw new ENException(202);
        }
    }


    protected void parseCurve(Network net, String[] tok) throws ENException {
        Curve cur;

        if (net.getCurves().size() == 0) {
            cur = new Curve();
            net.addCurve(tok[0], cur);
        } else {
            cur = net.getCurve(tok[0]);

            if (cur == null) {
                cur = new Curve();
                net.addCurve(tok[0], cur);
            }
        }

        Double x = Utilities.getDouble(tok[1]);
        Double y = Utilities.getDouble(tok[2]);

        if (x == null || y == null)
            throw new ENException(202);

        cur.getX().add(x);
        cur.getY().add(y);
    }

    protected void parseCoordinate(Network net, String[] tok) throws ENException {
        if (tok.length < 3)
            throw new ENException(201);

        Node nodeRef = net.getNode(tok[0]);

        if (nodeRef == null)
            throw new ENException(203);

        Double x = Utilities.getDouble(tok[1]);
        Double y = Utilities.getDouble(tok[2]);

        if (x == null || y == null)
            throw new ENException(202);

        nodeRef.setPosition(new Point(x, y));
    }

    protected void parseLabel(Network net, String[] tok) throws ENException {
        if (tok.length < 3)
            throw new ENException(201);

        Label l = new Label();
        Double x = Utilities.getDouble(tok[0]);
        Double y = Utilities.getDouble(tok[1]);

        if (x == null || y == null)
            throw new ENException(202);

        l.setPosition(new Point(x, y));
        //if (tok[2].length() > 1)
        //    l.setText(tok[2].substring(1, tok[2].length() - 1));
        for (int i = 2; i < tok.length; i++)
            if (l.getText().length() == 0)
                l.setText(tok[i].replace("\"", ""));
            else
                l.setText(l.getText() + " " + tok[i].replace("\"", ""));

        net.getLabels().add(l);
    }

    protected void parseVertice(Network net, String[] tok) throws ENException {
        if (tok.length < 3)
            throw new ENException(201);

        Link linkRef = net.getLink(tok[0]);

        if (linkRef == null)
            throw new ENException(204);

        Double x = Utilities.getDouble(tok[1]);
        Double y = Utilities.getDouble(tok[2]);

        if (x == null || y == null)
            throw new ENException(202);

        linkRef.getVertices().add(new Point(x, y));
    }

    protected void parseControl(Network net, String[] Tok) throws ENException {
        int n = Tok.length;
        LinkType ltype;
        StatType status = StatType.ACTIVE;

        double setting = Constants.MISSING, time = 0.0, level = 0.0;

        if (n < 6)
            throw new ENException(201);

        Node nodeRef = null;
        Link linkRef = net.getLink(Tok[1]);

        if (linkRef == null) throw new ENException(204);

        ltype = linkRef.getType();

        if (ltype == LinkType.CV) throw new ENException(207);

        if (Utilities.match(Tok[2], StatType.OPEN.parseStr)) {
            status = StatType.OPEN;
            if (ltype == LinkType.PUMP) setting = 1.0;
            if (ltype == LinkType.GPV) setting = linkRef.getRoughness();
        } else if (Utilities.match(Tok[2], StatType.CLOSED.parseStr)) {
            status = StatType.CLOSED;
            if (ltype == LinkType.PUMP) setting = 0.0;
            if (ltype == LinkType.GPV) setting = linkRef.getRoughness();
        } else if (ltype == LinkType.GPV)
            throw new ENException(206);
        else
            try {
                setting = Double.parseDouble(Tok[2]);
            } catch (NumberFormatException ex) {
                throw new ENException(202);
            }


        if (ltype == LinkType.PUMP || ltype == LinkType.PIPE) {
            if (setting != Constants.MISSING) {
                if (setting < 0.0) throw new ENException(202);
                else if (setting == 0.0) status = StatType.CLOSED;
                else status = StatType.OPEN;
            }
        }

        ControlType ctype;

        if (Utilities.match(Tok[4], Keywords.w_TIME))
            ctype = ControlType.TIMER;
        else if (Utilities.match(Tok[4], Keywords.w_CLOCKTIME))
            ctype = ControlType.TIMEOFDAY;
        else {
            if (n < 8)
                throw new ENException(201);
            if ((nodeRef = net.getNode(Tok[5])) == null)
                throw new ENException(203);
            if (Utilities.match(Tok[6], Keywords.w_BELOW)) ctype = ControlType.LOWLEVEL;
            else if (Utilities.match(Tok[6], Keywords.w_ABOVE)) ctype = ControlType.HILEVEL;
            else
                throw new ENException(201);
        }

        switch (ctype) {
            case TIMER:
            case TIMEOFDAY:
                if (n == 6) time = Utilities.getHour(Tok[5], "");
                if (n == 7) time = Utilities.getHour(Tok[5], Tok[6]);
                if (time < 0.0) throw new ENException(201);
                break;
            case LOWLEVEL:
            case HILEVEL:
                try {
                    level = Double.parseDouble(Tok[7]);
                } catch (NumberFormatException ex) {
                    throw new ENException(202);
                }
                break;
        }

        Control cntr = new Control();
        cntr.setLink(linkRef);
        cntr.setNode(nodeRef);
        cntr.setType(ctype);
        cntr.setStatus(status);
        cntr.setSetting(setting);
        cntr.setTime((long) (3600.0 * time));
        if (ctype == ControlType.TIMEOFDAY)
            cntr.setTime(cntr.getTime() % Constants.SECperDAY);
        cntr.setGrade(level);

        net.addControl(cntr);
    }


    protected void parseSource(Network net, String[] Tok) throws ENException {
        int n = Tok.length;
        Source.Type type = Source.Type.CONCEN;
        double c0 = 0;
        Pattern pat = null;
        Node nodeRef;

        if (n < 2) throw new ENException(201);
        if ((nodeRef = net.getNode(Tok[0])) == null) throw new ENException(203);

        int i = 2;

        if ((type = Source.Type.parse(Tok[1])) == null)
            i = 1;
        //if (Utilities.match(Tok[1], Keywords.w_CONCEN)) type = Source.Type.CONCEN;
        //else if (Utilities.match(Tok[1], Keywords.w_MASS)) type = Source.Type.MASS;
        //else if (Utilities.match(Tok[1], Keywords.w_SETPOINT)) type = Source.Type.SETPOINT;
        //else if (Utilities.match(Tok[1], Keywords.w_FLOWPACED)) type = Source.Type.FLOWPACED;
        //else i = 1;

        try {
            c0 = Double.parseDouble(Tok[i]);
        } catch (NumberFormatException ex) {
            throw new ENException(202);
        }

        if (n > i + 1 && Tok[i + 1].length() > 0 && !Tok[i + 1].equals("*")) {
            pat = net.getPattern(Tok[i + 1]);
            if (pat == null) throw new ENException(205);
        }

        Source src = new Source();

        src.setC0(c0);
        src.setPattern(pat);
        src.setType(type);

        nodeRef.setSource(src);
    }


    protected void parseEmitter(Network net, String[] Tok) throws ENException {
        int n = Tok.length;
        Node nodeRef;
        double k;

        if (n < 2) throw new ENException(201);
        if ((nodeRef = net.getNode(Tok[0])) == null) throw new ENException(203);
        if (nodeRef instanceof Tank)
            throw new ENException(209);
        try {
            k = Double.parseDouble(Tok[1]);
        } catch (NumberFormatException ex) {
            throw new ENException(202);
        }
        if (k < 0.0)
            throw new ENException(202);
        nodeRef.setKe(k);

    }


    protected void parseQuality(Network net, String[] Tok) throws ENException {
        int n = Tok.length;
        Node nodeRef = null;
        long i0 = 0, i1 = 0;
        double c0;

        if (n < 2) return;
        if (n == 2) {
            if ((nodeRef = net.getNode(Tok[0])) == null) return;
            try {
                c0 = Double.parseDouble(Tok[1]);
            } catch (NumberFormatException ex) {
                throw new ENException(209);
            }
            nodeRef.setC0(new double[]{c0});
        } else {
            try {
                c0 = Double.parseDouble(Tok[2]);
            } catch (NumberFormatException ex) {
                throw new ENException(209);
            }


            try {
                i0 = Long.parseLong(Tok[0]);
                i1 = Long.parseLong(Tok[1]);
            } finally {
                if (i0 > 0 && i1 > 0) {
                    for (Node j : net.getNodes()) {
                        try {
                            long i = (long) Double.parseDouble(j.getId());//Integer.parseInt(j.getId());
                            if (i >= i0 && i <= i1)
                                j.setC0(new double[]{c0});
                        } catch (Exception e) {
                        }
                    }
                } else {
                    for (Node j : net.getNodes()) {
                        if ((Tok[0].compareTo(j.getId()) <= 0) &&
                                (Tok[1].compareTo(j.getId()) >= 0))
                            j.setC0(new double[]{c0});
                    }
                }
            }
        }
    }

    protected void parseReact(Network net, String[] Tok) throws ENException {
        int item, n = Tok.length;
        double y;

        if (n < 3) return;


        if (Utilities.match(Tok[0], Keywords.w_ORDER)) {
            try {
                y = Double.parseDouble(Tok[n - 1]);
            } catch (NumberFormatException ex) {
                throw new ENException(213);
            }

            if (Utilities.match(Tok[1], Keywords.w_BULK)) net.getPropertiesMap().setBulkOrder(y);
            else if (Utilities.match(Tok[1], Keywords.w_TANK)) net.getPropertiesMap().setTankOrder(y);
            else if (Utilities.match(Tok[1], Keywords.w_WALL)) {
                if (y == 0.0) net.getPropertiesMap().setWallOrder(0.0);
                else if (y == 1.0) net.getPropertiesMap().setWallOrder(1.0);
                else throw new ENException(213);
            } else throw new ENException(213);
            return;
        }

        if (Utilities.match(Tok[0], Keywords.w_ROUGHNESS)) {
            try {
                y = Double.parseDouble(Tok[n - 1]);
            } catch (NumberFormatException ex) {
                throw new ENException(213);
            }
            net.getPropertiesMap().setRfactor(y);
            return;
        }

        if (Utilities.match(Tok[0], Keywords.w_LIMITING)) {
            try {
                y = Double.parseDouble(Tok[n - 1]);
            } catch (NumberFormatException ex) {
                throw new ENException(213);
            }
            net.getPropertiesMap().setClimit(y);
            return;
        }

        if (Utilities.match(Tok[0], Keywords.w_GLOBAL)) {
            try {
                y = Double.parseDouble(Tok[n - 1]);
            } catch (NumberFormatException ex) {
                throw new ENException(213);
            }
            if (Utilities.match(Tok[1], Keywords.w_BULK)) net.getPropertiesMap().setKbulk(y);
            else if (Utilities.match(Tok[1], Keywords.w_WALL)) net.getPropertiesMap().setKwall(y);
            else throw new ENException(201);
            return;
        }

        if (Utilities.match(Tok[0], Keywords.w_BULK)) item = 1;
        else if (Utilities.match(Tok[0], Keywords.w_WALL)) item = 2;
        else if (Utilities.match(Tok[0], Keywords.w_TANK)) item = 3;
        else throw new ENException(201);

        Tok[0] = Tok[1];

        if (item == 3) {
            Node nodeRef;
            try {
                y = Double.parseDouble(Tok[n - 1]);
            } catch (NumberFormatException ex) {
                throw new ENException(209);
            }
            if (n == 3) {
                if ((nodeRef = net.getNode(Tok[1])) == null)
                    throw new ENException(208); //if ((j = net.getNode(Tok[1])) <= juncsCount) return;
                if (!(nodeRef instanceof Tank)) return;
                ((Tank) nodeRef).setKb(y);//net.getTanks().get(j - juncsCount).setKb(y);
            } else {
                long i1 = 0, i2 = 0;
                try {
                    i1 = Long.parseLong(Tok[1]);
                    i2 = Long.parseLong(Tok[2]);
                } finally {
                    if (i1 > 0 && i2 > 0) {
                        for (Tank j : net.getTanks()) {
                            long i = Long.parseLong(j.getId());
                            if (i >= i1 && i <= i2)
                                j.setKb(y);
                        }
                    } else {
                        for (Tank j : net.getTanks()) {
                            if (Tok[1].compareTo(j.getId()) <= 0 &&
                                    Tok[2].compareTo(j.getId()) >= 0)
                                j.setKb(y);
                        }
                    }
                }
            }
        } else {
            try {
                y = Double.parseDouble(Tok[n - 1]);
            } catch (NumberFormatException ex) {
                throw new ENException(211);
            }
            if (net.getLinks().size() == 0) return;
            if (n == 3) {
                Link linkRef;
                if ((linkRef = net.getLink(Tok[1])) == null) return;
                if (item == 1)
                    linkRef.setKb(y);
                else
                    linkRef.setKw(y);
            } else {
                long i1 = 0, i2 = 0;
                try {
                    i1 = Long.parseLong(Tok[1]);
                    i2 = Long.parseLong(Tok[2]);
                } finally {
                    if (i1 > 0 && i2 > 0) {
                        for (Link j : net.getLinks()) {
                            try {
                                long i = Long.parseLong(j.getId());
                                if (i >= i1 && i <= i2) {
                                    if (item == 1)
                                        j.setKb(y);
                                    else
                                        j.setKw(y);
                                }
                            } catch (Exception e) {
                            }
                        }
                    } else
                        for (Link j : net.getLinks()) {
                            if (Tok[1].compareTo(j.getId()) <= 0 &&
                                    Tok[2].compareTo(j.getId()) >= 0) {
                                if (item == 1)
                                    j.setKb(y);
                                else
                                    j.setKw(y);
                            }
                        }
                }
            }
        }
    }


    protected void parseMixing(Network net, String[] Tok) throws ENException {
        int n = Tok.length;
        Tank.MixType i;
        double v;
        Node nodeRef;

        if (net.getNodes().size() == 0)
            throw new ENException(208);

        if (n < 2) return;

        nodeRef = net.getNode(Tok[0]);
        if (nodeRef == null) throw new ENException(208);
        if (!(nodeRef instanceof Tank)) return;
        Tank tankRef = (Tank) nodeRef;

        if ((i = Tank.MixType.parse(Tok[1])) == null)
            throw new ENException(201);

        v = 1.0;
        if (i == Tank.MixType.MIX2 && n == 3) {
            try {
                v = Double.parseDouble(Tok[2]);
            } catch (NumberFormatException ex) {
                throw new ENException(209);
            }
        }

        if (v == 0.0)
            v = 1.0;

        if (tankRef.getArea() == 0.0) return;
        tankRef.setMixModel(i);
        tankRef.setV1max(v);
    }


    protected void parseStatus(Network net, String[] Tok) throws ENException {
        int n = Tok.length - 1;
        double y = 0.0;
        StatType status = StatType.ACTIVE;

        if (net.getLinks().size() == 0) throw new ENException(210);

        if (n < 1) throw new ENException(201);

        if (Utilities.match(Tok[n], Keywords.w_OPEN)) status = StatType.OPEN;
        else if (Utilities.match(Tok[n], Keywords.w_CLOSED)) status = StatType.CLOSED;
        else
            try {
                y = Double.parseDouble(Tok[n]);
            } catch (NumberFormatException ex) {
                throw new ENException(211);
            }

        if (y < 0.0)
            throw new ENException(211);

        Link linkRef;
        if (n == 1) {
            if ((linkRef = net.getLink(Tok[0])) == null) return;

            if (linkRef.getType() == LinkType.CV) throw new ENException(211);

            if (linkRef.getType() == LinkType.GPV
                    && status == StatType.ACTIVE) throw new ENException(211);

            changeStatus(linkRef, status, y);
        } else {
            long i0 = 0, i1 = 0;
            try {
                i0 = Long.parseLong(Tok[0]);
                i1 = Long.parseLong(Tok[1]);
            } finally {
                if (i0 > 0 && i1 > 0) {
                    for (Link j : net.getLinks()) {
                        try {
                            long i = Long.parseLong(j.getId());
                            if (i >= i0 && i <= i1)
                                changeStatus(j, status, y);
                        } catch (Exception e) {
                        }
                    }
                } else
                    for (Link j : net.getLinks())
                        if (Tok[0].compareTo(j.getId()) <= 0 &&
                                Tok[1].compareTo(j.getId()) >= 0)
                            changeStatus(j, status, y);
            }
        }
    }

    protected void changeStatus(Link lLink, StatType status, double y) {
        if (lLink.getType() == LinkType.PIPE || lLink.getType() == LinkType.GPV) {
            if (status != StatType.ACTIVE) lLink.setStatus(status);
        } else if (lLink.getType() == LinkType.PUMP) {
            if (status == StatType.ACTIVE) {
                lLink.setRoughness(y);//lLink.setKc(y);
                status = StatType.OPEN;
                if (y == 0.0) status = StatType.CLOSED;
            } else if (status == StatType.OPEN) lLink.setRoughness(1.0); //lLink.setKc(1.0);
            lLink.setStatus(status);
        } else if (lLink.getType().id >= LinkType.PRV.id) {
            lLink.setRoughness(y);//lLink.setKc(y);
            lLink.setStatus(status);
            if (status != StatType.ACTIVE) lLink.setRoughness(Constants.MISSING);//lLink.setKc(Constants.MISSING);
        }
    }

    protected void parseEnergy(Network net, String[] Tok) throws ENException {
        int n = Tok.length;
        Double y;

        if (n < 3) throw new ENException(201);

        if (Utilities.match(Tok[0], Keywords.w_DMNDCHARGE)) {
            if ((y = Utilities.getDouble(Tok[2])) == null)
                throw new ENException(213);
            net.getPropertiesMap().setDcost(y);
            return;
        }

        Pump pumpRef;
        if (Utilities.match(Tok[0], Keywords.w_GLOBAL)) {
            pumpRef = null;
        } else if (Utilities.match(Tok[0], Keywords.w_PUMP)) {
            if (n < 4) throw new ENException(201);
            Link linkRef = net.getLink(Tok[1]);
            if (linkRef == null) throw new ENException(216);
            if (linkRef.getType() != LinkType.PUMP) throw new ENException(216);
            pumpRef = (Pump) linkRef;
        } else throw new ENException(201);


        if (Utilities.match(Tok[n - 2], Keywords.w_PRICE)) {
            if ((y = Utilities.getDouble(Tok[n - 1])) == null) {
                if (pumpRef == null)
                    throw new ENException(213);
                else
                    throw new ENException(217);
            }

            if (pumpRef == null)
                net.getPropertiesMap().setEcost(y);
            else
                pumpRef.setEcost(y);

            return;
        } else if (Utilities.match(Tok[n - 2], Keywords.w_PATTERN)) {
            Pattern t = net.getPattern(Tok[n - 1]);
            if (t == null) {
                if (pumpRef == null) throw new ENException(213);
                else throw new ENException(217);
            }
            if (pumpRef == null)
                net.getPropertiesMap().setEpatId(t.getId());
            else
                pumpRef.setEpat(t);
            return;
        } else if (Utilities.match(Tok[n - 2], Keywords.w_EFFIC)) {
            if (pumpRef == null) {
                if ((y = Utilities.getDouble(Tok[n - 1])) == null)
                    throw new ENException(213);
                if (y <= 0.0)
                    throw new ENException(213);
                net.getPropertiesMap().setEpump(y);
            } else {
                Curve t = net.getCurve(Tok[n - 1]);
                if (t == null) throw new ENException(217);
                pumpRef.setEcurve(t);
            }
            return;
        }
        throw new ENException(201);
    }


    protected void parseReport(Network net, String[] Tok) throws ENException {
        int n = Tok.length - 1;
        //FieldType i;
        Double y;

        if (n < 1) throw new ENException(201);

        if (Utilities.match(Tok[0], Keywords.w_PAGE)) {
            if ((y = Utilities.getDouble(Tok[n])) == null) throw new ENException(213);
            if (y < 0.0 || y > 255.0) throw new ENException(213);
            net.getPropertiesMap().setPageSize(y.intValue());
            return;
        }


        if (Utilities.match(Tok[0], Keywords.w_STATUS)) {
            net.getPropertiesMap().setStatflag(PropertiesMap.StatFlag.parse(Tok[n]));
            //if (Utilities.match(Tok[n], Keywords.w_NO)) net.getPropertiesMap().setStatflag(PropertiesMap.StatFlag.FALSE);
            //if (Utilities.match(Tok[n], Keywords.w_YES)) net.getPropertiesMap().setStatflag(PropertiesMap.StatFlag.TRUE);
            //if (Utilities.match(Tok[n], Keywords.w_FULL)) net.getPropertiesMap().setStatflag(PropertiesMap.StatFlag.FULL);
            return;
        }

        if (Utilities.match(Tok[0], Keywords.w_SUMMARY)) {
            if (Utilities.match(Tok[n], Keywords.w_NO)) net.getPropertiesMap().setSummaryflag(false);
            if (Utilities.match(Tok[n], Keywords.w_YES)) net.getPropertiesMap().setSummaryflag(true);
            return;
        }

        if (Utilities.match(Tok[0], Keywords.w_MESSAGES)) {
            if (Utilities.match(Tok[n], Keywords.w_NO)) net.getPropertiesMap().setMessageflag(false);
            if (Utilities.match(Tok[n], Keywords.w_YES)) net.getPropertiesMap().setMessageflag(true);
            return;
        }

        if (Utilities.match(Tok[0], Keywords.w_ENERGY)) {
            if (Utilities.match(Tok[n], Keywords.w_NO)) net.getPropertiesMap().setEnergyflag(false);
            if (Utilities.match(Tok[n], Keywords.w_YES)) net.getPropertiesMap().setEnergyflag(true);
            return;
        }

        if (Utilities.match(Tok[0], Keywords.w_NODE)) {
            if (Utilities.match(Tok[n], Keywords.w_NONE))
                net.getPropertiesMap().setNodeflag(PropertiesMap.ReportFlag.FALSE);
            else if (Utilities.match(Tok[n], Keywords.w_ALL))
                net.getPropertiesMap().setNodeflag(PropertiesMap.ReportFlag.TRUE);
            else {
                if (net.getNodes().size() == 0) throw new ENException(208);
                for (int ii = 1; ii <= n; ii++) {
                    Node nodeRef;
                    if ((nodeRef = net.getNode(Tok[n])) == null) throw new ENException(208);
                    nodeRef.setReportFlag(true);
                }
                net.getPropertiesMap().setNodeflag(PropertiesMap.ReportFlag.SOME);
            }
            return;
        }

        if (Utilities.match(Tok[0], Keywords.w_LINK)) {
            if (Utilities.match(Tok[n], Keywords.w_NONE))
                net.getPropertiesMap().setLinkflag(PropertiesMap.ReportFlag.FALSE);
            else if (Utilities.match(Tok[n], Keywords.w_ALL))
                net.getPropertiesMap().setLinkflag(PropertiesMap.ReportFlag.TRUE);
            else {
                if (net.getLinks().size() == 0) throw new ENException(210);
                for (int ii = 1; ii <= n; ii++) {
                    Link linkRef = null;
                    if ((linkRef = net.getLink(Tok[ii])) == null) throw new ENException(210);
                    linkRef.setReportFlag(true);
                }
                net.getPropertiesMap().setLinkflag(PropertiesMap.ReportFlag.SOME);
            }
            return;
        }

        Type iFieldID;
        FieldsMap fMap = net.getFieldsMap();

        if (Utilities.match(Tok[0], Keywords.w_HEADLOSS))
            iFieldID = Type.HEADLOSS;
        else
            iFieldID = Type.parse(Tok[0]);

        if (iFieldID != null) {
            if (iFieldID.id > Type.FRICTION.id)
                throw new ENException(201);

            if (Tok.length == 1 || Utilities.match(Tok[1], Keywords.w_YES)) {
                fMap.getField(iFieldID).setEnabled(true);
                return;
            }

            if (Utilities.match(Tok[1], Keywords.w_NO)) {
                fMap.getField(iFieldID).setEnabled(false);
                return;
            }

            Field.RangeType rj;

            if (Tok.length < 3)
                throw new ENException(201);

            if (Utilities.match(Tok[1], Keywords.w_BELOW))
                rj = Field.RangeType.LOW;
            else if (Utilities.match(Tok[1], Keywords.w_ABOVE))
                rj = Field.RangeType.HI;
            else if (Utilities.match(Tok[1], Keywords.w_PRECISION))
                rj = Field.RangeType.PREC;
            else
                throw new ENException(201);

            if ((y = Utilities.getDouble(Tok[2])) == null)
                throw new ENException(201);

            if (rj == Field.RangeType.PREC) {
                fMap.getField(iFieldID).setEnabled(true);
                fMap.getField(iFieldID).setPrecision((int) Math.round(y));//roundOff(y));
            } else
                fMap.getField(iFieldID).setRptLim(rj, y);

            return;
        }

        if (Utilities.match(Tok[0], Keywords.w_FILE)) {
            net.getPropertiesMap().setAltReport(Tok[1]);
            return;
        }

        log.info("Unknow section keyword "+Tok[0]+" value "+Tok[1]);
//        throw new ENException(201);
    }

    protected void parseOption(Network net, String[] Tok) throws ENException {
        int n = Tok.length - 1;
        boolean notHandled = optionChoice(net, Tok, n);
        if (notHandled)
            notHandled = optionValue(net, Tok, n);
        if (notHandled) {
            net.getPropertiesMap().put(Tok[0], Tok[1]);
        }
    }

    /**
     * Handles options that are choice values, such as quality type, for example.
     *
     * @param net - newtwork
     * @param Tok - token arry
     * @param n   - number of tokens
     * @return true is it didn't handle the option.
     * @throws ENException
     */
    protected boolean optionChoice(Network net, String[] Tok, int n) throws ENException {
        PropertiesMap map = net.getPropertiesMap();

        if (n < 0)
            throw new ENException(201);

        if (Utilities.match(Tok[0], Keywords.w_UNITS)) {
            PropertiesMap.FlowUnitsType type;
            if (n < 1)
                return false;
            else if ((type = PropertiesMap.FlowUnitsType.parse(Tok[1])) != null)
                map.setFlowflag(type);
            else
                throw new ENException(201);
        } else if (Utilities.match(Tok[0], Keywords.w_PRESSURE)) {
            if (n < 1) return false;
            else if (Utilities.match(Tok[1], Keywords.w_PSI)) map.setPressflag(PropertiesMap.PressUnitsType.PSI);
            else if (Utilities.match(Tok[1], Keywords.w_KPA)) map.setPressflag(PropertiesMap.PressUnitsType.KPA);
            else if (Utilities.match(Tok[1], Keywords.w_METERS)) map.setPressflag(PropertiesMap.PressUnitsType.METERS);
            else
                throw new ENException(201);
        } else if (Utilities.match(Tok[0], Keywords.w_HEADLOSS)) {
            if (n < 1) return false;
            else if (Utilities.match(Tok[1], Keywords.w_HW)) map.setFormflag(PropertiesMap.FormType.HW);
            else if (Utilities.match(Tok[1], Keywords.w_DW)) map.setFormflag(PropertiesMap.FormType.DW);
            else if (Utilities.match(Tok[1], Keywords.w_CM)) map.setFormflag(PropertiesMap.FormType.CM);
            else throw new ENException(201);
        } else if (Utilities.match(Tok[0], Keywords.w_HYDRAULIC)) {
            if (n < 2)
                return false;
            else if (Utilities.match(Tok[1], Keywords.w_USE)) map.setHydflag(PropertiesMap.Hydtype.USE);
            else if (Utilities.match(Tok[1], Keywords.w_SAVE)) map.setHydflag(PropertiesMap.Hydtype.SAVE);
            else
                throw new ENException(201);
            map.setHydFname(Tok[2]);
        } else if (Utilities.match(Tok[0], Keywords.w_QUALITY)) {
            PropertiesMap.QualType type;
            if (n < 1)
                return false;
            else if ((type = PropertiesMap.QualType.parse(Tok[1])) != null)
                map.setQualflag(type);
                //else if (Utilities.match(Tok[1], Keywords.w_NONE)) net.setQualflag(QualType.NONE);
                //else if (Utilities.match(Tok[1], Keywords.w_CHEM)) net.setQualflag(QualType.CHEM);
                //else if (Utilities.match(Tok[1], Keywords.w_AGE)) net.setQualflag(QualType.AGE);
                //else if (Utilities.match(Tok[1], Keywords.w_TRACE)) net.setQualflag(QualType.TRACE);
            else {
                map.setQualflag(PropertiesMap.QualType.CHEM);
                map.setChemName(Tok[1]);
                if (n >= 2)
                    map.setChemUnits(Tok[2]);
            }
            if (map.getQualflag() == PropertiesMap.QualType.TRACE) {

                Tok[0] = "";
                if (n < 2)
                    throw new ENException(212);
                Tok[0] = Tok[2];
                Node nodeRef = net.getNode(Tok[2]);
                if (nodeRef == null)
                    throw new ENException(212);
                map.setTraceNode(nodeRef.getId());
                map.setChemName(Keywords.u_PERCENT);
                map.setChemUnits(Tok[2]);
            }
            if (map.getQualflag() == PropertiesMap.QualType.AGE) {
                map.setChemName(Keywords.w_AGE);
                map.setChemUnits(Keywords.u_HOURS);
            }
        } else if (Utilities.match(Tok[0], Keywords.w_MAP)) {
            if (n < 1)
                return false;
            map.setMapFname(Tok[1]);
        } else if (Utilities.match(Tok[0], Keywords.w_UNBALANCED)) {
            if (n < 1)
                return false;
            if (Utilities.match(Tok[1], Keywords.w_STOP))
                map.setExtraIter(-1);
            else if (Utilities.match(Tok[1], Keywords.w_CONTINUE)) {
                if (n >= 2)
                    try {
                        map.setExtraIter((int) Double.parseDouble(Tok[2]));
                    } catch (Exception e) {
                        throw new ENException(201);
                    }
                else
                    map.setExtraIter(0);
            } else throw new ENException(201);
        } else if (Utilities.match(Tok[0], Keywords.w_PATTERN)) {
            if (n < 1)
                return false;
            map.setDefPatId(Tok[1]);
        } else
            return true;
        return false;
    }


    protected boolean optionValue(Network net, String[] Tok, int n) throws ENException {
        int nvalue = 1;
        Double y;
        PropertiesMap map = net.getPropertiesMap();

        String name = Tok[0];


        if (Utilities.match(name, Keywords.w_SPECGRAV) || Utilities.match(name, Keywords.w_EMITTER)
                || Utilities.match(name, Keywords.w_DEMAND)) nvalue = 2;

        String keyword = null;
        for (String k : OPTION_VALUE_KEYWORDS) {
            if (Utilities.match(name, k)) {
                keyword = k;
                break;
            }
        }
        if (keyword == null) return true;
        name = keyword;

        y = Utilities.getDouble(Tok[nvalue]);

        if (y == null) throw new ENException(213);

        if (Utilities.match(name, Keywords.w_TOLERANCE)) {
            if (y < 0.0)
                throw new ENException(213);
            map.setCtol(y);
            return false;
        }

        if (Utilities.match(name, Keywords.w_DIFFUSIVITY)) {
            if (y < 0.0)
                throw new ENException(213);
            map.setDiffus(y);
            return false;
        }

        if (Utilities.match(name, Keywords.w_DAMPLIMIT)) {
            map.setDampLimit(y);
            return false;
        }

        if (y <= 0.0) throw new ENException(213);

        if (Utilities.match(name, Keywords.w_VISCOSITY)) map.setViscos(y);
        else if (Utilities.match(name, Keywords.w_SPECGRAV)) map.setSpGrav(y);
        else if (Utilities.match(name, Keywords.w_TRIALS)) map.setMaxIter(y.intValue());
        else if (Utilities.match(name, Keywords.w_ACCURACY)) {
            y = Math.max(y, 1.e-5);
            y = Math.min(y, 1.e-1);
            map.setHacc(y);
        } else if (Utilities.match(name, Keywords.w_HTOL)) map.setHtol(y);
        else if (Utilities.match(name, Keywords.w_QTOL)) map.setQtol(y);
        else if (Utilities.match(name, Keywords.w_RQTOL)) {
            if (y >= 1.0) throw new ENException(213);
            map.setRQtol(y);
        } else if (Utilities.match(name, Keywords.w_CHECKFREQ)) map.setCheckFreq(y.intValue());
        else if (Utilities.match(name, Keywords.w_MAXCHECK)) map.setMaxCheck(y.intValue());
        else if (Utilities.match(name, Keywords.w_EMITTER)) map.setQexp(1.0d / y);
        else if (Utilities.match(name, Keywords.w_DEMAND)) map.setDmult(y);

        return false;
    }

    protected void parseTime(Network net, String[] Tok) throws ENException {
        int n = Tok.length - 1;
        long t;
        Double y;
        PropertiesMap map = net.getPropertiesMap();

        if (n < 1)
            throw new ENException(201);

        if (Utilities.match(Tok[0], Keywords.w_STATISTIC)) {
            if (Utilities.match(Tok[n], Keywords.w_NONE)) map.setTstatflag(PropertiesMap.TstatType.SERIES);
            else if (Utilities.match(Tok[n], Keywords.w_NO)) map.setTstatflag(PropertiesMap.TstatType.SERIES);
            else if (Utilities.match(Tok[n], Keywords.w_AVG)) map.setTstatflag(PropertiesMap.TstatType.AVG);
            else if (Utilities.match(Tok[n], Keywords.w_MIN)) map.setTstatflag(PropertiesMap.TstatType.MIN);
            else if (Utilities.match(Tok[n], Keywords.w_MAX)) map.setTstatflag(PropertiesMap.TstatType.MAX);
            else if (Utilities.match(Tok[n], Keywords.w_RANGE)) map.setTstatflag(PropertiesMap.TstatType.RANGE);
            else
                throw new ENException(201);
            return;
        }

        if ((y = Utilities.getDouble(Tok[n])) == null) {
            if ((y = Utilities.getHour(Tok[n], "")) < 0.0) {
                if ((y = Utilities.getHour(Tok[n - 1], Tok[n])) < 0.0)
                    throw new ENException(213);
            }
        }
        t = (long) (3600.0 * y);

        if (Utilities.match(Tok[0], Keywords.w_DURATION))
            map.setDuration(t);
        else if (Utilities.match(Tok[0], Keywords.w_HYDRAULIC))
            map.setHstep(t);
        else if (Utilities.match(Tok[0], Keywords.w_QUALITY))
            map.setQstep(t);
        else if (Utilities.match(Tok[0], Keywords.w_RULE))
            map.setRulestep(t);
        else if (Utilities.match(Tok[0], Keywords.w_MINIMUM))
            return;
        else if (Utilities.match(Tok[0], Keywords.w_PATTERN)) {
            if (Utilities.match(Tok[1], Keywords.w_TIME))
                map.setPstep(t);
            else if (Utilities.match(Tok[1], Keywords.w_START))
                map.setPstart(t);
            else
                throw new ENException(201);
        } else if (Utilities.match(Tok[0], Keywords.w_REPORT)) {
            if (Utilities.match(Tok[1], Keywords.w_TIME))
                map.setRstep(t);
            else if (Utilities.match(Tok[1], Keywords.w_START))
                map.setRstart(t);
            else
                throw new ENException(201);
        } else if (Utilities.match(Tok[0], Keywords.w_START))
            map.setTstart(t % Constants.SECperDAY);
        else throw new ENException(201);

    }

    protected Network.SectType findSectionType(String line) {
        for (Network.SectType type : Network.SectType.values())
            if (line.contains(type.parseStr))
                return type;
        return null;
    }

    protected void parseDemand(Network net, String[] Tok) throws ENException {
        int n = Tok.length;
        double y;
        Demand demand = null;
        Pattern pat = null;

        if (n < 2)
            throw new ENException(201);

        try {
            y = Double.parseDouble(Tok[1]);
        } catch (NumberFormatException ex) {
            throw new ENException(202);
        }

        if (Utilities.match(Tok[0], Keywords.w_MULTIPLY)) {
            if (y <= 0.0)
                throw new ENException(202);
            else
                net.getPropertiesMap().setDmult(y);
            return;
        }

        Node nodeRef;
        if ((nodeRef = net.getNode(Tok[0])) == null)
            throw new ENException(208);

        if (nodeRef instanceof Tank)
            throw new ENException(208);

        if (n >= 3) {
            pat = net.getPattern(Tok[2]);
            if (pat == null)
                throw new ENException(205);
        }

        if (nodeRef.getDemand().size() > 0)
            demand = nodeRef.getDemand().get(0);

        if (demand != null && nodeRef.getInitDemand() != Constants.MISSING) {
            demand.setBase(y);
            demand.setPattern(pat);
            nodeRef.setInitDemand(Constants.MISSING);
        } else {
            demand = new Demand(y, pat);
            nodeRef.getDemand().add(demand);
        }

    }

    protected void parseRule(Network net, String[] tok, String line) throws ENException {
        Rule.Rulewords key = Rule.Rulewords.parse(tok[0]);
        if (key == Rule.Rulewords.r_RULE) {
            currentRule = new Rule();
            currentRule.setLabel(tok[1]);
            ruleState = Rule.Rulewords.r_RULE;
            net.addRule(currentRule);
        } else if (currentRule != null) {
            currentRule.setCode(currentRule.getCode() + line + "\n");
        }

    }

    /*protected void parseRule(Network net,String [] tok)  throws ENException
    {
        if (ruleState == Rule.Rulewords.r_ERROR)
            return;

        Rule.Rulewords key = Rule.Rulewords.parse(tok[0]);

        if(key == null)  throw new ENException(201);

        switch (key)
        {
            case r_RULE:
                currentRule = new Rule();
                currentRule.setLabel(tok[1]);
                ruleState = Rule.Rulewords.r_RULE;
                net.addRule(currentRule);
                break;

            case r_IF:
                if (!ruleState.equals(Rule.Rulewords.r_RULE))
                    throw new ENException(221);
                ruleState = Rule.Rulewords.r_IF;
                parsePremise(net, tok, Rule.Rulewords.r_AND);
                break;

            case r_AND:
                if (ruleState == Rule.Rulewords.r_IF)
                    parsePremise(net, tok, Rule.Rulewords.r_AND);
                else if (ruleState == Rule.Rulewords.r_THEN || ruleState == Rule.Rulewords.r_ELSE)
                   parseAction(net,ruleState,tok);
                else
                    throw new ENException(221);
                break;

            case r_OR:
                if (ruleState == Rule.Rulewords.r_IF)
                    parsePremise(net, tok, Rule.Rulewords.r_OR);
                else
                    throw new ENException(221);
                break;

            case r_THEN:
                if (ruleState != Rule.Rulewords.r_IF)
                    throw new ENException (221);
                ruleState = Rule.Rulewords.r_THEN;
                parseAction(net, ruleState, tok);
                break;

            case r_ELSE:
                if (ruleState != Rule.Rulewords.r_THEN)
                    throw new ENException(221);
                ruleState = Rule.Rulewords.r_ELSE;
                parseAction(net, ruleState, tok);
                break;

            case r_PRIORITY:
                if (!ruleState.equals(Rule.Rulewords.r_THEN) && !ruleState.equals(Rule.Rulewords.r_ELSE))
                    throw new ENException(221);
                ruleState = Rule.Rulewords.r_PRIORITY;
                parsePriority(net, tok);
                break;

            default:
                 throw new ENException(201);
        }
    }

    protected void parsePremise(Network net,String []Tok,Rule.Rulewords logop) throws ENException {
        Rule.Objects obj;
        Rule.Varwords v;

        Rule.Premise p = new Rule.Premise();

        if (Tok.length != 5 && Tok.length != 6)
            throw new ENException(201);

        obj = Rule.Objects.parse(Tok[1]);

        p.setLogop(logop);

        if (obj == Rule.Objects.r_SYSTEM){
            v = Rule.Varwords.parse(Tok[2]);

            if (v != Rule.Varwords.r_DEMAND && v != Rule.Varwords.r_TIME && v != Rule.Varwords.r_CLOCKTIME)
                throw new ENException(201);

            p.setObject(Rule.Objects.r_SYSTEM);
        }
        else
        {
            v = Rule.Varwords.parse(Tok[3]);
            if (v == null)
                throw new ENException(201);
            switch (obj)
            {
                case r_NODE:
                case r_JUNC:
                case r_RESERV:
                case r_TANK:
                    obj = Rule.Objects.r_NODE;
                    break;
                case r_LINK:
                case r_PIPE:
                case r_PUMP:
                case r_VALVE:
                    obj = Rule.Objects.r_LINK;
                    break;
                default:
                    throw new ENException(201);
            }

            if (obj == Rule.Objects.r_NODE)
            {
                Node nodeRef = net.getNode(Tok[2]);
                if (nodeRef == null)
                    throw new ENException(203);
                switch (v){
                    case r_DEMAND:
                    case r_HEAD:
                    case r_GRADE:
                    case r_LEVEL:
                    case r_PRESSURE:
                        break;
                    case r_FILLTIME:
                    case r_DRAINTIME: if (nodeRef instanceof Tank) throw new ENException(201); break;

                    default:
                        throw new ENException(201);
                }
                p.setObject(nodeRef);
            }
            else
            {
                Link linkRef = net.getLink(Tok[2]);
                if (linkRef == null)
                    throw new ENException(204);
                switch (v){
                    case r_FLOW:
                    case r_STATUS:
                    case r_SETTING:
                        break;
                    default:
                        throw new ENException(201);
                }
                p.setObject(linkRef);
            }
        }

        Rule.Operators op;

        if (obj == Rule.Objects.r_SYSTEM)
            op = Rule.Operators.parse(Tok[3]);
        else
            op = Rule.Operators.parse(Tok[4]);

        if (op == null)
            throw new ENException(201);

        switch(op)
        {
            case IS:
                p.setRelop(Operators.EQ);
                break;
            case NOT:
                 p.setRelop(Operators.NE);
                break;
            case BELOW:
                p.setRelop(Operators.LT);
                break;
            case ABOVE:
                p.setRelop(Operators.GT);
                break;
            default:
                p.setRelop(op);
        }

        Values status = Values.IS_NUMBER;
        Double value = Constants.MISSING;

        if (v == Varwords.r_TIME || v == Varwords.r_CLOCKTIME)
        {
            if (Tok.length == 6)
                value = Utilities.getHour(Tok[4],Tok[5])*3600.;
            else
                value = Utilities.getHour(Tok[4],"")*3600.;
            if (value < 0.0) throw new ENException(202);
        }
        else{
            status = Values.parse(Tok[Tok.length-1]);
            if (status==null || status.id <= Values.IS_NUMBER.id){
                if ((value = Utilities.getDouble(Tok[Tok.length - 1]))==null)
                    throw new ENException(202);
                if (v == Varwords.r_FILLTIME || v == Varwords.r_DRAINTIME)
                    value = value*3600.0;
            }
        }


        p.setVariable(v);
        p.setStatus(status);
        p.setValue(value);

        currentRule.addPremise(p);
    }

    protected void parseAction(Network net,Rulewords state, String[] tok) throws ENException {
        int Ntokens = tok.length;

        Values s,k;
        Double x;

        if (Ntokens != 6)
            throw new ENException(201);

        Link linkRef = net.getLink(tok[2]);

        if (linkRef == null)
            throw new ENException(204);

        if (linkRef.getType() == LinkType.CV)
            throw new ENException(207);

        s = null;
        x = Constants.MISSING;
        k = Values.parse(tok[5]);

        if (k!=null && k.id > Values.IS_NUMBER.id)
            s = k;
        else
        {
            if ( (x = Utilities.getDouble(tok[5]))==null )
                throw new ENException(202);
            if (x < 0.0)
                throw new ENException(202);
        }

        if (x != Constants.MISSING && linkRef.getType() == LinkType.GPV)
            throw new ENException(202);

        if (x != Constants.MISSING && linkRef.getType() == LinkType.PIPE){
            if (x == 0.0)
                s = Values.IS_CLOSED;
            else
                s = Values.IS_OPEN;
            x = Constants.MISSING;
        }

        Action a = new Action();
        a.setLink(linkRef);
        a.setStatus(s);
        a.setSetting(x);

        if (state == Rulewords.r_THEN)
            currentRule.addActionT(a);
        else
            currentRule.addActionF(a);
    }

    protected void parsePriority(Network net,String[] tok) throws ENException {
        Double x;
        if ( (x = Utilities.getDouble(tok[1]))==null)
            throw new ENException(202);
        currentRule.setPriority(x);
    }  */


}
