package org.addition.epanetold;

import org.addition.epanetold.Types.*;
import org.addition.epanetold.Types.EnumVariables.*;

import java.io.*;
import java.util.*;


public strictfp class InputReader {
    static final ResourceBundle errorBundle = PropertyResourceBundle.getBundle("Error");

    private int nodesCount = 0;              // counter for juntions, tanks total while loading.
    private int linksCount = 0;              // counter for pipes, pumps and valves.
    private int juncsCount = 0;
    private int pipesCount = 0;
    private int tanksCount = 0;
    private int pumpsCount = 0;
    private int valvesCount = 0;
    private int controlsCount = 0;
    private int labelCount = 0;

    Map<String, Integer> nodeHashTable;
    Map<String, Integer> linkHashTable;

    List<Tmplist> patternList;
    List<Tmplist> curveList;

    private Tmplist prevPattern;
    private Tmplist prevCurve;

    private Epanet epanet;
    private Network net;
    private Rules rules;
    private Report rep;

    InputReader(Epanet epanet) {
        this.epanet = epanet;
        nodeHashTable = new Hashtable<String, Integer>();
        linkHashTable = new Hashtable<String, Integer>();
        patternList = new ArrayList<Tmplist>();
        curveList = new ArrayList<Tmplist>();
    }

    void loadDependencies() {
        net = epanet.getNetwork();
        rules = epanet.getRules();
        rep = epanet.getReport();
    }

    public int netsize(File f) {
        int errcode = 0;
        FileReader textReader;

        try {
            textReader = new FileReader(f);
        } catch (FileNotFoundException ex) {
            return 0;
        }

        BufferedReader buffReader = new BufferedReader(textReader);

        SectType sectionType = SectType._TITLE;

        addPattern("");

        String line;
        try {
            while ((line = buffReader.readLine()) != null) {
                if (line.indexOf(";") >= 0)
                    line = line.substring(0, line.indexOf(";"));
                line = line.trim();
                if (line.length() == 0)
                    continue;

                String[] tokens = line.split("[ \t]+");
                if (tokens.length == 0) continue;

//                StringTokenizer sTokenizer = new StringTokenizer(line);

                // Check for available tokens
//                if (sTokenizer.countTokens() == 0)
//                    continue;
//
//                String firstToken = sTokenizer.nextToken();

                // Check for comments
                if (tokens[0].contains(";")) continue;


                if (tokens[0].contains("[")) {
                    SectType type = findSectionType(tokens[0]);

                    if (type != null)
                        sectionType = type;
                    else {
                        //TODO: handle error
                        sectionType = null;
                        System.err.println("Invalid section type " + tokens[0]);
                    }
                } else if (sectionType != null) {
                    switch (sectionType) {
                        case _RULES:
                            if (rules != null)
                                rules.addrule(tokens[0]);
                            break;
                        case _PATTERNS:
                            errcode = addPattern(tokens[0]);
                            break;
                        case _CURVES:
                            errcode = addCurve(tokens[0]);
                            break;
                        default:
                            net.addSections(sectionType);
                    }
                }

                if (errcode > 0)
                    break;
            }
        } catch (IOException ex) {
            return 0;
        }

        net.setMaxNodes(net.getSections(SectType._JUNCTIONS) +
                net.getSections(SectType._TANKS) +
                net.getSections(SectType._RESERVOIRS));

        net.setMaxLinks(net.getSections(SectType._PIPES) +
                net.getSections(SectType._PUMPS) +
                net.getSections(SectType._VALVES));

        net.setMaxTanks(net.getSections(SectType._TANKS) + net.getSections(SectType._RESERVOIRS));

        if (patternList.size() < 1) net.setSections(SectType._PATTERNS, 1);
        if (errcode == 0) {
            if (net.getSections(SectType._JUNCTIONS) < 1) errcode = 223;
            else if (net.getMaxTanks() == 0) errcode = 224;
        }

        try {
            buffReader.close();
            textReader.close();
        } catch (IOException ex) {
            return 0;
        }

        if(net.getMaxLinks() == 0 || net.getMaxNodes() == 0)
            return 200;

        rules.initrules();
        rules.allocrules();
        return errcode;
    }


    public int getdata(File f) {
        int errcode = 0;
        int errsum = 0;

        FileReader textReader = null;

        try {
            textReader = new FileReader(f);
        } catch (IOException ex) {
            return 0;
        }

        BufferedReader buffReader = new BufferedReader(textReader);
        SectType sectionType = SectType._TITLE;


        String line;
        try {
            while ((line = buffReader.readLine()) != null) {
                if (line.indexOf(";") >= 0)
                    line = line.substring(0, line.indexOf(";"));
                line = line.trim();
                if (line.length() == 0)
                    continue;


//                StringTokenizer sTokenizer = new StringTokenizer(line);
//
//                // Check for available tokens
//                if (sTokenizer.countTokens() == 0)
//                    continue;
//
//                String firstToken = sTokenizer.nextToken();
//
//                // netsize tokens
//                Vector<String> sTokens = new Vector<String>();
//
//                sTokens.add(firstToken);

//                while (sTokenizer.hasMoreTokens()) {
//                    sTokens.add(sTokenizer.nextToken());
//                }

                String[] tokens = line.split("[ \t]+");
                if (tokens.length == 0) continue;
                if (tokens[0].contains("[")) {
                    SectType type = findSectionType(tokens[0]);
                    if (type != null)
                        sectionType = type;
                    else {
                        //TODO: handle error
                        sectionType = null;
                        System.err.println("Invalid section type " + tokens[0]);
                    }
                } else if (sectionType != null) {
                    String[] array = tokens;//sTokens.toArray(new String[sTokens.size()]);

                    int secErrCode = newSection(sectionType, line, array);

                    if (secErrCode > 0) {
                        inperrmsg(secErrCode, sectionType, line, array);
                        errsum++;
                    }
                }
                if (errsum == Constants.MAXERRS) break;
            }
        } catch (IOException ex) {
            return 0;
        }
        errcode = 0;
        if (errsum > 0) errcode = 200;

        try {
            buffReader.close();
            textReader.close();
        } catch (IOException ex) {
            return 0;
        }

        if (errcode == 0) errcode = unlinked();
        if (errcode == 0) errcode = getpattern();
        if (errcode == 0) errcode = getpumpparams();

        return errcode;

    }

    void inperrmsg(int err, SectType sect, String line, String[] Tok) {
        String fmt;
        String id;

        if (sect == SectType._ENERGY)
            id = Tok[1];
        else
            id = Tok[0];

        if (err == 213 || err == 214)
            id = "";

        fmt = errorBundle.getString("ERR" + err);

        rep.writeline(String.format(fmt, EnumVariables.RptSectTxt[sect.ordinal()], id));


        if (sect == SectType._CONTROLS || err == 201 || err == 213)
            rep.writeline(line);
        else
            rep.writeline("");
    }

    int getpattern() {


        for (int i = 0; i <= net.getSections(SectType._PATTERNS); i++) {

            if(net.getPattern(i).getFactorsList().size() == 0){
                net.getPattern(i).getFactorsList().add(1.0);
            }

            if (net.getPattern(i).getId().equals(net.DefPatID)) {
                net.DefPat = i;
                break;
            }
        }
        return 0;
    }


    int unlinked() {
        int[] marked = new int[nodesCount + 1];
        int i, err, errcode;
        errcode = 0;
        err = 0;

        for (i = 1; i <= linksCount; i++) {
            marked[net.getLink(i).getN1()]++;
            marked[net.getLink(i).getN2()]++;
        }
        for (i = 1; i <= juncsCount; i++) {
            if (marked[i] == 0) {
                err++;
                rep.writeline(String.format(errorBundle.getString("ERR233"), net.getNode(i).getId()));
            }
            if (err >= Constants.MAXERRS) break;
        }
        if (err > 0) errcode = 200;

        return (errcode);
    }

    int getpumpparams() {
        int i, j = 0, k, m, n = 0;
        double a, b, c,
                h0 = 0.0, h1 = 0.0, h2 = 0.0, q1 = 0.0, q2 = 0.0;

        for (i = 1; i <= net.getSections(SectType._PUMPS); i++) {
            Pump pump = net.getPump(i);
            k = pump.getLink();
            if (pump.getPtype() == PumpType.CONST_HP) {
                pump.setH0(0.0);
                pump.setR(-8.814 * net.getLink(k).getKm());
                pump.setN(-1.0);
                pump.setHMax(Constants.BIG);
                pump.setQMax(Constants.BIG);
                pump.setQ0(1.0);
                continue;
            } else if (pump.getPtype() == PumpType.NOCURVE) {
                j = pump.getHcurve();
                if (j == 0) {
                    rep.writeline(String.format(errorBundle.getString("ERR226"), net.getLink(k).getId()));
                    return (200);
                }
                n = net.getCurve(j).getNpts();
                if (n == 1) {
                    pump.setPtype(PumpType.POWER_FUNC);
                    q1 = net.getCurve(j).getX().get(0);
                    h1 = net.getCurve(j).getY().get(0);
                    h0 = 1.33334 * h1;
                    q2 = 2.0 * q1;
                    h2 = 0.0;
                } else if (n == 3
                        && net.getCurve(j).getX().get(0) == 0.0) {
                    pump.setPtype(PumpType.POWER_FUNC);
                    h0 = net.getCurve(j).getY().get(0);
                    q1 = net.getCurve(j).getX().get(1);
                    h1 = net.getCurve(j).getY().get(1);
                    q2 = net.getCurve(j).getX().get(2);
                    h2 = net.getCurve(j).getY().get(2);
                } else pump.setPtype(PumpType.CUSTOM);


                if (pump.getPtype() == PumpType.POWER_FUNC) {
                    double[] abc;
                    if ((abc = powercurve(h0, h1, h2, q1, q2)) == null) {
                        rep.writeline(String.format(errorBundle.getString("ERR227"), net.getLink(k).getId()));
                        return (200);
                    } else {
                        pump.setH0(-abc[0]);
                        pump.setR(-abc[1]);
                        pump.setN(abc[2]);
                        pump.setQ0(q1);
                        pump.setQMax(Math.pow((-abc[0] / abc[1]), (1.0 / abc[2])));
                        pump.setHMax(h0);
                    }
                }
            }

            if (pump.getPtype() == PumpType.CUSTOM) {
                for (m = 1; m < n; m++) {
                    if (net.getCurve(j).getY().get(m) >= net.getCurve(j).getY().get(m - 1)) {
                        rep.writeline(String.format(errorBundle.getString("ERR227"), net.getLink(k).getId()));
                        return (200);
                    }
                }
                pump.setQMax(net.getCurve(j).getX().get(n - 1));
                pump.setQ0((net.getCurve(j).getX().get(0) + pump.getQMax()) / 2.0);
                pump.setHMax(net.getCurve(j).getY().get(0));
            }
        }
        return (0);
    }


    int newSection(SectType type, String line, String[] Tok) {
        int n;
        switch (type) {
            case _TITLE: {
                List<String> titleStr = net.getTitleText();
                if (titleStr != null && titleStr.size() < 3)
                    titleStr.add(line);
                return (0);
            }
            case _JUNCTIONS:
                return (parseJunction(Tok));

            case _RESERVOIRS:
            case _TANKS:
                return (parseTank(Tok));

            case _PIPES:
                return (parsePipe(Tok));
            case _PUMPS:
                return (parsePump(Tok));
            case _VALVES:
                return (parseValve(Tok));
            case _PATTERNS:
                return (parsePattern(Tok));
            case _CURVES:
                return (parseCurve(Tok));
            case _DEMANDS:
                return (parseDemand(Tok));
            case _CONTROLS:
                return (parseControl(Tok));
            case _RULES:
                if (rules != null)
                    return rules.parseRule(Tok);
                else return (0);
            case _SOURCES:
                return (parseSource(Tok));
            case _EMITTERS:
                return (parseEmitter(Tok));
            case _QUALITY:
                return (parseQuality(Tok));
            case _STATUS:
                return (parseStatus(Tok));
            case _ROUGHNESS:
                return (0);
            case _ENERGY:
                return (parseEnergy(Tok));
            case _REACTIONS:
                return (parseReact(Tok));
            case _MIXING:
                return (parseMixing(Tok));
            case _REPORT:
                return (parseReport(Tok));
            case _TIMES:
                return (parseTime(Tok));
            case _OPTIONS:
                return (parseOption(Tok));

            // Unused data.
            case _COORDS:
                return (parseCoordinate(Tok));
            case _LABELS:
                return (parseLabel(Tok));
            case _TAGS:
                return (0);
            case _VERTICES:
                return (parseVertice(Tok));
            case _BACKDROP:
                return (0);

            default:
                return (0);
        }

    }


    private SectType findSectionType(String line) {
        int i = 0;
        for (String k : EnumVariables.SectTxt) {
            if (line.contains(k)) {
                return SectType.class.getEnumConstants()[i];
            }
            i++;
        }

        return null;
    }


    private void addRule(String tok) {
        if (tok.contains(EnumVariables.w_RULE))
            net.addSections(SectType._RULES);
    }


    private int addPattern(String id) {
        List<Tmplist> plist = patternList;
        Tmplist p;

        if (plist.size() > 0 && plist.get(0).getID().equals(id)) return 0;

        if (findID(id, plist) == null) {
            net.addSections(SectType._PATTERNS);
            plist.add(0, new Tmplist(plist.size(), id));
        }

        return (0);
    }


    private int addCurve(String id) {
        List<Tmplist> clist = curveList;
        Tmplist c;

        if (clist.size() > 0 && clist.get(0).getID().equals(id)) return (0);

        if (findID(id, clist) == null) {
            net.addSections(SectType._CURVES);
            clist.add(0, new Tmplist(clist.size() + 1, id));
        }

        return 0;
    }


    static private Tmplist findID(String id, List<Tmplist> patlist) {
        for (Tmplist t : patlist) {
            if (t.getID().equals(id))
                return t;
        }

        return null;
    }


    private boolean addNodeID(int id, String node) {
        if (nodeHashTable.get(node) != null) return false;
        nodeHashTable.put(node, id);
        return true;
    }

    int findNode(String node) {
        Object oVal = nodeHashTable.get(node);
        if (oVal == null)
            return 0;
        return (Integer) oVal;
    }

    int findLink(String link) {
        Object oVal = linkHashTable.get(link);
        if (oVal == null)
            return 0;
        return (Integer) oVal;
    }

    private boolean addLinkID(int id, String link) {
        if (linkHashTable.get(link) != null) return false;
        linkHashTable.put(link, id);
        return true;
    }

    public int parseCoordinate(String[] Tok) {
        if (Tok.length < 3)
            return 0;
        int nodeID = findNode(Tok[0]);

        if (nodeID == 0)
            return 0;

        Double x = Utilities.getFloat(Tok[1]);
        Double y = Utilities.getFloat(Tok[2]);

        if (x == null || y == null)
            return 0;

        net.getNode(nodeID).setPosition(new Point(x, y));

        return 0;
    }

    public int parseLabel(String [] Tok)
    {
        if (Tok.length < 3)
            return 0;

        labelCount++;

        Label l = net.getLabel(labelCount);
        Double x = Utilities.getFloat(Tok[0]);
        Double y = Utilities.getFloat(Tok[1]);

        l.setPosition(new Point(x,y));
        if(Tok[2].length()>1)
            l.setText(Tok[2].substring(1,Tok[2].length()-1));

        return 0;
    }
    public int parseVertice(String[] Tok) {
        if (Tok.length < 3)
            return 0;
        int linkID = findLink(Tok[0]);

        if (linkID == 0)
            return 0;

        Double x = Utilities.getFloat(Tok[1]);
        Double y = Utilities.getFloat(Tok[2]);

        if (x == null || y == null)
            return 0;

        net.getLink(linkID).getVertices().add(new Point(x, y));

        return 0;
    }

    public int parseJunction(String[] Tok) {
        int n = 0, p = 0;
        n = Tok.length;
        Double el, y = 0.0d;

        if (nodesCount == net.getMaxNodes()) return (200);

        nodesCount++;
        juncsCount++;

        Node activeNode = net.getNode(juncsCount);

        if (!addNodeID(juncsCount, Tok[0])) return (215);
        activeNode.setId(Tok[0]);

        if (n < 2) return (201);

        if ((el = Utilities.getFloat(Tok[1])) == null) return (202);

        if (n >= 3 && (y = Utilities.getFloat(Tok[2])) == null) return (202);

        if (n >= 4) {
            Tmplist pat = findID(Tok[3], patternList);

            if (pat == null) return 205;
            p = pat.getI();
        }

        activeNode.setElevation(el);
        activeNode.setC0(0.0d);
        activeNode.setSource(null);
        activeNode.setKe(0.0);
        activeNode.setReportFlag(false);


        if (n >= 3) {
            Demand demand = new Demand();
            activeNode.getDemand().add(demand);
            demand.setBase(y);
            demand.setPattern(p);
            net.setNodeDemand(juncsCount, y);
        } else
            net.setNodeDemand(juncsCount, Constants.MISSING);

        return 0;
    }


    int parseTank(String[] Tok) {
        int i, n = Tok.length, p = 0, vcurve = 0;
        Double el = 0.0d,
                initlevel = 0.0d,
                minlevel = 0.0d,
                maxlevel = 0.0d,
                minvol = 0.0d,
                diam = 0.0d,
                area;


        if (tanksCount == (net.getSections(SectType._TANKS)
                + net.getSections(SectType._RESERVOIRS))
                || nodesCount == net.getMaxNodes()) return (200);

        nodesCount++;
        tanksCount++;

        Tank activeTank = net.getTank(tanksCount);
        Node activeNode = null;

        i = net.getSections(SectType._JUNCTIONS) + tanksCount;
        activeNode = net.getNode(i);

        if (!addNodeID(i, Tok[0])) return (215);
        activeNode.setId(Tok[0]);

        if (n < 2) return (201);

        if ((el = Utilities.getFloat(Tok[1])) == null) return (202);

        if (n <= 3) {
            if (n == 3) {
                Tmplist t = findID(Tok[2], patternList);
                if (t == null) return (205);
                p = t.getI();
            }
        } else if (n < 6)
            return (201);
        else {
            //is a tank
            if ((initlevel = Utilities.getFloat(Tok[2])) == null) return (202);
            if ((minlevel = Utilities.getFloat(Tok[3])) == null) return (202);
            if ((maxlevel = Utilities.getFloat(Tok[4])) == null) return (202);
            if ((diam = Utilities.getFloat(Tok[5])) == null) return (202);

            if (diam < 0.0)
                return (202);

            if (n >= 7
                    && (minvol = Utilities.getFloat(Tok[6])) == null) return (202);

            if (n == 8) {
                Tmplist t = findID(Tok[7], curveList);
                if (t == null)
                    return (202);
                vcurve = t.getI();
            }
        }


        activeNode.setReportFlag(false);
        activeNode.setElevation(el);
        activeNode.setC0(0.0d);
        activeNode.setSource(null);
        activeNode.setKe(0.0);

        activeTank.setNode(i);
        activeTank.setH0(initlevel);
        activeTank.setHMin(minlevel);
        activeTank.setHMax(maxlevel);
        activeTank.setArea(diam);
        activeTank.setPattern(p);
        activeTank.setKb(Constants.MISSING);

        area = Constants.PI * diam * diam / 4.0d;
        activeTank.setVmin(area * minlevel);
        if (minvol > 0.0) activeTank.setVmin(minvol);
        activeTank.setV0(activeTank.getVmin() + area * (initlevel - minlevel));
        activeTank.setVmax(activeTank.getVmin() + area * (maxlevel - minlevel));

        activeTank.setVcurve(vcurve);
        activeTank.setMixModel(MixType.MIX1);
        activeTank.setV1max(1.0);
        return (0);
    }


    int parsePipe(String[] Tok) {
        int j1, j2, n = Tok.length;
        LinkType type = LinkType.PIPE;
        StatType status = StatType.OPEN;
        Double length, diam, rcoeff, lcoeff = 0.0d;

        if (linksCount == net.getMaxLinks()) return (200);

        pipesCount++;
        linksCount++;

        Link activeLink = net.getLink(linksCount);

        if (!addLinkID(linksCount, Tok[0])) return (215);
        activeLink.setId(Tok[0]);

        if (n < 6) return (201);
        if ((j1 = findNode(Tok[1])) == 0 ||
                (j2 = findNode(Tok[2])) == 0
                ) return (203);


        if (j1 == j2) return (222);

        if ((length = Utilities.getFloat(Tok[3])) == null ||
                (diam = Utilities.getFloat(Tok[4])) == null ||
                (rcoeff = Utilities.getFloat(Tok[5])) == null
                ) return (202);


        if (length <= 0.0 || diam <= 0.0 || rcoeff <= 0.0) return (202);

        if (n == 7) {
            if (Utilities.match(Tok[6], EnumVariables.w_CV)) type = LinkType.CV;
            else if (Utilities.match(Tok[6], EnumVariables.w_CLOSED)) status = StatType.CLOSED;
            else if (Utilities.match(Tok[6], EnumVariables.w_OPEN)) status = StatType.OPEN;
            else if ((lcoeff = Utilities.getFloat(Tok[6])) == null) return (202);
        }

        if (n == 8) {
            if ((lcoeff = Utilities.getFloat(Tok[6])) == null) return (202);
            if (Utilities.match(Tok[7], EnumVariables.w_CV)) type = LinkType.CV;
            else if (Utilities.match(Tok[7], EnumVariables.w_CLOSED)) status = StatType.CLOSED;
            else if (Utilities.match(Tok[7], EnumVariables.w_OPEN)) status = StatType.OPEN;
            else
                return (202);
        }

        if (lcoeff < 0.0) return (202);

        activeLink.setN1(j1);
        activeLink.setN2(j2);
        activeLink.setLenght(length);
        activeLink.setDiameter(diam);
        activeLink.setKc(rcoeff);
        activeLink.setKm(lcoeff);
        activeLink.setKb(Constants.MISSING);
        activeLink.setKw(Constants.MISSING);
        activeLink.setType(type);
        activeLink.setStatus(status);
        activeLink.setReportFlag(false);
        return (0);
    }

    int parsePump(String[] Tok) {
        int j, j1, j2, m, n = Tok.length;
        Double y;
        Double[] X = new Double[6];


        if (linksCount == net.getMaxLinks() ||
                pumpsCount == net.getSections(SectType._PUMPS)
                ) return (200);

        linksCount++;
        pumpsCount++;

        Link activeLink = net.getLink(linksCount);
        Pump activePump = net.getPump(pumpsCount);

        if (!addLinkID(linksCount, Tok[0])) return (215);
        activeLink.setId(Tok[0]);

        if (n < 4)
            return (201);
        if ((j1 = findNode(Tok[1])) == 0 || (j2 = findNode(Tok[2])) == 0) return (203);
        if (j1 == j2) return (222);

        activeLink.setN1(j1);
        activeLink.setN2(j2);
        activeLink.setDiameter(pumpsCount);
        activeLink.setLenght(0.0d);
        activeLink.setKc(1.0d);
        activeLink.setKm(0.0d);
        activeLink.setKb(0.0d);
        activeLink.setKw(0.0d);
        activeLink.setType(LinkType.PUMP);
        activeLink.setStatus(StatType.OPEN);
        activeLink.setReportFlag(false);

        activePump.setLink(linksCount);
        activePump.setPtype(PumpType.NOCURVE);
        activePump.setHcurve(0);
        activePump.setEcurve(0);
        activePump.setUpat(0);
        activePump.setEcost(0.0d);
        activePump.setEpat(0);

        if ((X[0] = Utilities.getFloat(Tok[3])) != null) {

            m = 1;
            for (j = 4; j < n; j++) {
                if ((X[m] = Utilities.getFloat(Tok[j])) == null) return (202);
                m++;
            }
            return (getpumpcurve(linksCount, pumpsCount, m, X));
        }

        m = 4;
        while (m < n) {

            if (Utilities.match(Tok[m - 1], EnumVariables.w_POWER)) {
                y = Double.parseDouble(Tok[m]);
                if (y <= 0.0) return (202);
                activePump.setPtype(PumpType.CONST_HP);
                activeLink.setKm(y);
            } else if (Utilities.match(Tok[m - 1], EnumVariables.w_HEAD)) {
                Tmplist t = findID(Tok[m], curveList);
                if (t == null) return (206);
                activePump.setHcurve(t.getI());
            } else if (Utilities.match(Tok[m - 1], EnumVariables.w_PATTERN)) {
                Tmplist t = findID(Tok[m], patternList);
                if (t == null) return (205);
                activePump.setUpat(t.getI());
            } else if (Utilities.match(Tok[m - 1], EnumVariables.w_SPEED)) {
                if ((y = Utilities.getFloat(Tok[m])) == null) return (202);
                if (y < 0.0) return (202);
                activeLink.setKc(y);
            } else
                return (201);
            m = m + 2;
        }
        return (0);
    }


    int parseValve(String[] Tok) {
        int j1,
                j2,
                n = Tok.length;
        StatType status = StatType.ACTIVE;
        LinkType type;

        double diam = 0.0,
                setting,
                lcoeff = 0.0;

        Tmplist t;

        if (linksCount == net.getMaxLinks() ||
                valvesCount == net.getSections(SectType._VALVES)
                ) return (200);

        valvesCount++;
        linksCount++;

        if (!addLinkID(linksCount, Tok[0])) return (215);
        net.getLink(linksCount).setId(Tok[0]);

        if (n < 6) return (201);
        if ((j1 = findNode(Tok[1])) == 0 ||
                (j2 = findNode(Tok[2])) == 0
                ) return (203);

        if (j1 == j2) return (222);


        if (Utilities.match(Tok[4], EnumVariables.w_PRV)) type = LinkType.PRV;

        else if (Utilities.match(Tok[4], EnumVariables.w_PSV)) type = LinkType.PSV;
        else if (Utilities.match(Tok[4], EnumVariables.w_PBV)) type = LinkType.PBV;
        else if (Utilities.match(Tok[4], EnumVariables.w_FCV)) type = LinkType.FCV;
        else if (Utilities.match(Tok[4], EnumVariables.w_TCV)) type = LinkType.TCV;
        else if (Utilities.match(Tok[4], EnumVariables.w_GPV)) type = LinkType.GPV;
        else return (201);
        try {
            diam = Double.parseDouble(Tok[3]);
        } catch (NumberFormatException ex) {
            return (202);
        }
        if (diam <= 0.0) return (202);

        if (type == LinkType.GPV) {
            t = findID(Tok[5], curveList);
            if (t == null) return (206);
            setting = t.getI();

            status = StatType.OPEN;

        } else
            try {
                setting = Double.parseDouble(Tok[5]);
            } catch (NumberFormatException ex) {
                return (202);
            }

        if (n >= 7)
            try {
                lcoeff = Double.parseDouble(Tok[6]);
            } catch (NumberFormatException ex) {
                return (202);
            }


        if ((j1 > juncsCount || j2 > juncsCount) &&
                (type == LinkType.PRV || type == LinkType.PSV || type == LinkType.FCV)
                ) return (219);
        if (!valvecheck(type, j1, j2)) return (220);

        Link activeLine = net.getLink(linksCount);
        Valve activeValve = net.getValve(valvesCount);

        activeLine.setN1(j1);
        activeLine.setN2(j2);
        activeLine.setDiameter(diam);
        activeLine.setLenght(0.0d);
        activeLine.setKc(setting);
        activeLine.setKm(lcoeff);
        activeLine.setKb(0.0d);
        activeLine.setKw(0.0d);
        activeLine.setType(type);
        activeLine.setStatus(status);
        activeLine.setReportFlag(false);
        activeValve.setLink(linksCount);
        return (0);
    }


    int parsePattern(String[] Tok) {
        int i, n = Tok.length - 1;
        double x;
        Tmplist p;

        if (n < 1) return (201);
        if (prevPattern != null && Tok[0].equals(prevPattern.getID()))
            p = prevPattern;
        else
            p = findID(Tok[0], patternList);

        if (p == null)
            return (205);

        Pattern pat = net.getPattern(p.getI());
        pat.setId(Tok[0]);

        for (i = 1; i <= n; i++) {
            try {
                x = Double.parseDouble(Tok[i]);
            } catch (NumberFormatException ex) {
                return (202);
            }
            pat.add(x);
        }

        prevPattern = p;
        return (0);
    }


    int parseCurve(String[] Tok) {
        double x, y;
        Tmplist c;

        if (Tok.length < 3) return (201);
        if (prevCurve != null && Tok[0].equals(prevCurve.getID())
                ) c = prevCurve;
        else
            c = findID(Tok[0], curveList);
        if (c == null) return (205);


        try {
            x = Double.parseDouble(Tok[1]);
        } catch (NumberFormatException ex) {
            return (202);
        }
        try {
            y = Double.parseDouble(Tok[2]);
        } catch (NumberFormatException ex) {
            return (202);
        }

        Curve cur = net.getCurve(c.getI());
        cur.setId(Tok[0]);

        cur.getX().add(x);
        cur.getY().add(y);
        prevCurve = c;
        return (0);
    }


    int parseDemand(String[] Tok) {
        int j, n = Tok.length, p = 0;
        double y;
        Demand demand = null;
        Tmplist pat;

        if (n < 2) return (201);
        try {
            y = Double.parseDouble(Tok[1]);
        } catch (NumberFormatException ex) {
            return (202);
        }

        if (Utilities.match(Tok[0], EnumVariables.w_MULTIPLY)) {
            if (y <= 0.0) return (202);
            else
                net.Dmult = y;
            return (0);
        }

        if ((j = findNode(Tok[0])) == 0) return (208);
        if (j > juncsCount) return (208);
        if (n >= 3) {
            pat = findID(Tok[2], patternList);
            if (pat == null) return (205);
            p = pat.getI();
        }

        if (net.getNode(j).getDemand().size() > 0)
            demand = net.getNode(j).getDemand().get(0);

        Double d = net.getNodeDemand(j);
        if (demand != null && d != Constants.MISSING) {
            demand.setBase(y);
            demand.setPattern(p);
            net.setNodeDemand(j, Constants.MISSING);
            // d = MISSING;
        } else {
            demand = new Demand();
            demand.setBase(y);
            demand.setPattern(p);
            net.getNode(j).getDemand().add(0, demand);
        }
        return (0);
    }


    int parseControl(String[] Tok) {
        int i = 0,
                k,
                n = Tok.length;
        StatType status = StatType.ACTIVE;
        LinkType ltype;
        double setting = Constants.MISSING,
                time = 0.0,
                level = 0.0;

        if (n < 6) return (201);

        k = findLink(Tok[1]);
        if (k == 0) return (204);
        ltype = net.getLink(k).getType();
        if (ltype == LinkType.CV) return (207);


        if (Utilities.match(Tok[2], EnumVariables.w_OPEN)) {
            status = StatType.OPEN;
            if (ltype == LinkType.PUMP) setting = 1.0;
            if (ltype == LinkType.GPV) setting = net.getLink(k).getKc();
        } else if (Utilities.match(Tok[2], EnumVariables.w_CLOSED)) {
            status = StatType.CLOSED;
            if (ltype == LinkType.PUMP) setting = 0.0;
            if (ltype == LinkType.GPV) setting = net.getLink(k).getKc();
        } else if (ltype == LinkType.GPV) return (206);
        else
            try {
                setting = Double.parseDouble(Tok[2]);
            } catch (NumberFormatException ex) {
                return (202);
            }


        if (ltype == LinkType.PUMP || ltype == LinkType.PIPE) {
            if (setting != Constants.MISSING) {
                if (setting < 0.0) return (202);
                else if (setting == 0.0) status = StatType.CLOSED;
                else status = StatType.OPEN;
            }
        }

        ControlType ctype;


        if (Utilities.match(Tok[4], EnumVariables.w_TIME)) ctype = ControlType.TIMER;
        else if (Utilities.match(Tok[4], EnumVariables.w_CLOCKTIME)) ctype = ControlType.TIMEOFDAY;
        else {
            if (n < 8) return (201);
            if ((i = findNode(Tok[5])) == 0) return (203);
            if (Utilities.match(Tok[6], EnumVariables.w_BELOW)) ctype = ControlType.LOWLEVEL;
            else if (Utilities.match(Tok[6], EnumVariables.w_ABOVE)) ctype = ControlType.HILEVEL;
            else return (201);
        }

        switch (ctype) {
            case TIMER:
            case TIMEOFDAY:
                if (n == 6) time = Utilities.hour(Tok[5], "");
                if (n == 7) time = Utilities.hour(Tok[5], Tok[6]);
                if (time < 0.0) return (201);
                break;
            case LOWLEVEL:
            case HILEVEL:
                try {
                    level = Double.parseDouble(Tok[7]);
                } catch (NumberFormatException ex) {
                    return (202);
                }
                break;
        }

        controlsCount++;
        if (controlsCount > net.getSections(SectType._CONTROLS)) return (200);
        Control cntr = net.getControl(controlsCount);
        cntr.setLink(k);
        cntr.setNode(i);
        cntr.setType(ctype);
        cntr.setStatus(status);
        cntr.setSetting(setting);
        cntr.setTime((long) (3600.0 * time));
        if (ctype == ControlType.TIMEOFDAY)
            cntr.setTime(cntr.getTime() % Constants.SECperDAY);
        cntr.setGrade(level);
        return (0);
    }


    int parseSource(String[] Tok) {
        int i,
                j,
                n = Tok.length,
                p = 0;
        SourceType type = SourceType.CONCEN;
        double c0 = 0;
        Tmplist pat;

        if (n < 2) return (201);
        if ((j = findNode(Tok[0])) == 0) return (203);

        i = 2;

        if (Utilities.match(Tok[1], EnumVariables.w_CONCEN)) type = SourceType.CONCEN;
        else if (Utilities.match(Tok[1], EnumVariables.w_MASS)) type = SourceType.MASS;
        else if (Utilities.match(Tok[1], EnumVariables.w_SETPOINT)) type = SourceType.SETPOINT;
        else if (Utilities.match(Tok[1], EnumVariables.w_FLOWPACED)) type = SourceType.FLOWPACED;
        else
            i = 1;
        try {
            c0 = Double.parseDouble(Tok[i]);
        } catch (NumberFormatException ex) {
            return (202);
        }

        if (n > i + 1 && Tok[i + 1].length() > 0 && !Tok[i + 1].equals("*")) {
            pat = findID(Tok[i + 1], patternList);
            if (pat == null) return (205);
            p = pat.getI();
        }

        Source src = new Source();

        src.setC0(c0);
        src.setPattern(p);
        src.setType(type);

        net.getNode(j).setSource(src);
        return (0);
    }


    int parseEmitter(String[] Tok) {
        int j, n = Tok.length;
        double k;

        if (n < 2) return (201);
        if ((j = findNode(Tok[0])) == 0) return (203);
        if (j > juncsCount) return (209);
        try {
            k = Double.parseDouble(Tok[1]);
        } catch (NumberFormatException ex) {
            return (202);
        }
        if (k < 0.0) return (202);
        net.getNode(j).setKe(k);
        return (0);
    }


    int parseQuality(String[] Tok) {
        int j, n = Tok.length;
        long i, i0, i1;
        double c0;

        if (nodesCount == 0) return (208);

        if (n < 2) return (0);
        if (n == 2) {
            if ((j = findNode(Tok[0])) == 0) return (0);
            try {
                c0 = Double.parseDouble(Tok[1]);
            } catch (NumberFormatException ex) {
                return (209);
            }
            net.getNode(j).setC0(c0);
        } else {
            try {
                c0 = Double.parseDouble(Tok[2]);
            } catch (NumberFormatException ex) {
                return (209);
            }


            if ((i0 = Long.parseLong(Tok[0])) > 0 && (i1 = Long.parseLong(Tok[1])) > 0) {
                for (j = 1; j <= nodesCount; j++) {
                    i = Integer.parseInt(net.getNode(j).getId());
                    if (i >= i0 && i <= i1) net.getNode(j).setC0(c0);
                }
            } else {
                for (j = 1; j <= nodesCount; j++)
                    if ((Tok[0].compareTo(net.getNode(j).getId()) <= 0) &&
                            (Tok[1].compareTo(net.getNode(j).getId()) >= 0)
                            ) net.getNode(j).setC0(c0);
            }
        }
        return (0);
    }


    int parseReact(String[] Tok) {
        int item, j, n = Tok.length;
        long i, i1, i2;
        double y;

        if (n < 3) return (0);


        if (Utilities.match(Tok[0], EnumVariables.w_ORDER)) {
            try {
                y = Double.parseDouble(Tok[n - 1]);
            } catch (NumberFormatException ex) {
                return (213);
            }

            if (Utilities.match(Tok[1], EnumVariables.w_BULK)) net.BulkOrder = y;
            else if (Utilities.match(Tok[1], EnumVariables.w_TANK)) net.TankOrder = y;
            else if (Utilities.match(Tok[1], EnumVariables.w_WALL)) {
                if (y == 0.0) net.WallOrder = 0.0;
                else if (y == 1.0) net.WallOrder = 1.0;
                else return (213);
            } else return (213);
            return (0);
        }

        if (Utilities.match(Tok[0], EnumVariables.w_ROUGHNESS)) {
            try {
                y = Double.parseDouble(Tok[n - 1]);
            } catch (NumberFormatException ex) {
                return (213);
            }
            net.Rfactor = y;
            return (0);
        }

        if (Utilities.match(Tok[0], EnumVariables.w_LIMITING)) {
            try {
                y = Double.parseDouble(Tok[n - 1]);
            } catch (NumberFormatException ex) {
                return (213);
            }
            net.Climit = y;
            return (0);
        }

        if (Utilities.match(Tok[0], EnumVariables.w_GLOBAL)) {
            try {
                y = Double.parseDouble(Tok[n - 1]);
            } catch (NumberFormatException ex) {
                return (213);
            }
            if (Utilities.match(Tok[1], EnumVariables.w_BULK)) net.Kbulk = y;
            else if (Utilities.match(Tok[1], EnumVariables.w_WALL)) net.Kwall = y;
            else return (201);
            return (0);
        }

        if (Utilities.match(Tok[0], EnumVariables.w_BULK)) item = 1;
        else if (Utilities.match(Tok[0], EnumVariables.w_WALL)) item = 2;
        else if (Utilities.match(Tok[0], EnumVariables.w_TANK)) item = 3;
        else return (201);

        Tok[0] = Tok[1];

        if (item == 3) {
            try {
                y = Double.parseDouble(Tok[n - 1]);
            } catch (NumberFormatException ex) {
                return (209);
            }
            if (n == 3) {
                if ((j = findNode(Tok[1])) <= juncsCount) return (0);
                net.getTank(j - juncsCount).setKb(y);
            } else {
                if ((i1 = Long.parseLong(Tok[1])) > 0 && (i2 = Long.parseLong(Tok[2])) > 0) {
                    for (j = juncsCount + 1; j <= nodesCount; j++) {
                        i = Long.parseLong(net.getNode(j).getId());
                        if (i >= i1 && i <= i2) net.getTank(j - juncsCount).setKb(y);
                    }
                } else for (j = juncsCount + 1; j <= nodesCount; j++)
                    if (Tok[1].compareTo(net.getNode(j).getId()) <= 0 &&
                            Tok[2].compareTo(net.getNode(j).getId()) >= 0)
                        net.getTank(j - juncsCount).setKb(y);
            }
        } else {
            try {
                y = Double.parseDouble(Tok[n - 1]);
            } catch (NumberFormatException ex) {
                return (211);
            }
            if (linksCount == 0) return (0);
            if (n == 3) {
                if ((j = findLink(Tok[1])) == 0) return (0);
                if (item == 1)
                    net.getLink(j).setKb(y);
                else
                    net.getLink(j).setKw(y);
            } else {


                if ((i1 = Long.parseLong(Tok[1])) > 0 && (i2 = Long.parseLong(Tok[2])) > 0) {
                    for (j = 1; j <= linksCount; j++) {
                        i = Long.parseLong(net.getLink(j).getId());
                        if (i >= i1 && i <= i2) {
                            if (item == 1)
                                net.getLink(j).setKb(y);
                            else
                                net.getLink(j).setKw(y);
                        }
                    }
                } else for (j = 1; j <= linksCount; j++)
                    if (Tok[1].compareTo(net.getLink(j).getId()) <= 0 &&
                            Tok[2].compareTo(net.getLink(j).getId()) >= 0) {
                        if (item == 1)
                            net.getLink(j).setKb(y);
                        else
                            net.getLink(j).setKw(y);
                    }
            }
        }
        return (0);
    }


    int parseMixing(String[] Tok) {
        int i, j, n = Tok.length;
        double v;

        if (nodesCount == 0)
            return (208);

        if (n < 2) return (0);
        if ((j = findNode(Tok[0])) <= juncsCount) return (0);
        if ((i = Utilities.findMatch(Tok[1], EnumVariables.MixTxt)) < 0) return (201);
        v = 1.0;
        if ((MixType.values()[i] == MixType.MIX2) &&
                (n == 3)) {
            try {
                v = Double.parseDouble(Tok[2]);
            } catch (NumberFormatException ex) {
                return (209);
            }
        }

        if (v == 0.0) v = 1.0;
        n = j - juncsCount;
        if (net.getTank(n).getArea() == 0.0) return (0);
        net.getTank(n).setMixModel(MixType.values()[i]);
        net.getTank(n).setV1max(v);
        return (0);
    }


    int parseStatus(String[] Tok) {
        int j, n = Tok.length - 1;
        long i, i0, i1;
        double y = 0.0;
        StatType status = StatType.ACTIVE;

        if (linksCount == 0) return (210);

        if (n < 1) return (201);

        if (Utilities.match(Tok[n], EnumVariables.w_OPEN)) status = StatType.OPEN;
        else if (Utilities.match(Tok[n], EnumVariables.w_CLOSED)) status = StatType.CLOSED;
        else
            try {
                y = Double.parseDouble(Tok[n]);
            } catch (NumberFormatException ex) {
                return (211);
            }

        if (y < 0.0)
            return (211);

        if (n == 1) {
            if ((j = findLink(Tok[0])) == 0) return (0);

            if (net.getLink(j).getType() == LinkType.CV) return (211);

            if (net.getLink(j).getType() == LinkType.GPV
                    && status == StatType.ACTIVE) return (211);

            changestatus(j, status, y);
        } else {
            if ((i0 = Long.parseLong(Tok[0])) > 0 && (i1 = Long.parseLong(Tok[1])) > 0) {
                for (j = 1; j <= linksCount; j++) {
                    i = Long.parseLong(net.getLink(j).getId());
                    if (i >= i0 && i <= i1) changestatus(j, status, y);
                }
            } else
                for (j = 1; j <= linksCount; j++)
                    if (Tok[0].compareTo(net.getLink(j).getId()) <= 0 &&
                            Tok[1].compareTo(net.getLink(j).getId()) >= 0)
                        changestatus(j, status, y);
        }
        return (0);
    }


    int parseEnergy(String[] Tok) {
        int j, k, n = Tok.length;
        Double y;
        Tmplist t;

        if (n < 3) return (201);


        if (Utilities.match(Tok[0], EnumVariables.w_DMNDCHARGE)) {
            if ((y = Utilities.getFloat(Tok[2])) == null)
                return (213);
            net.Dcost = y;
            return (0);
        }
        if (Utilities.match(Tok[0], EnumVariables.w_GLOBAL)) {
            j = 0;
        } else if (Utilities.match(Tok[0], EnumVariables.w_PUMP)) {
            if (n < 4) return (201);
            k = findLink(Tok[1]);
            if (k == 0) return (216);
            if (net.getLink(k).getType() != LinkType.PUMP) return (216);
            j = pumpIndex(k);
        } else return (201);


        if (Utilities.match(Tok[n - 2], EnumVariables.w_PRICE)) {
            if ((y = Utilities.getFloat(Tok[n - 1])) == null) {
                if (j == 0)
                    return (213);
                else
                    return (217);
            }

            if (j == 0)
                net.Ecost = y;
            else
                net.getPump(j).setEcost(y);

            return (0);
        } else if (Utilities.match(Tok[n - 2], EnumVariables.w_PATTERN)) {
            t = findID(Tok[n - 1], patternList);
            if (t == null) {
                if (j == 0) return (213);
                else return (217);
            }
            if (j == 0)
                net.Epat = t.getI();
            else
                net.getPump(j).setEpat(t.getI());
            return (0);
        } else if (Utilities.match(Tok[n - 2], EnumVariables.w_EFFIC)) {
            if (j == 0) {
                if ((y = Utilities.getFloat(Tok[n - 1])) == null)
                    return (213);
                if (y <= 0.0)
                    return (213);
                net.Epump = y;
            } else {
                t = findID(Tok[n - 1], curveList);
                if (t == null) return (217);
                net.getPump(j).setEcurve(t.getI());
            }
            return (0);
        }
        return (201);
    }


    int parseReport(String[] Tok) {
        int j, n = Tok.length - 1;
        FieldType i;
        Double y;

        if (n < 1) return (201);

        if (Utilities.match(Tok[0], EnumVariables.w_PAGE)) {
            if ((y = Utilities.getFloat(Tok[n])) == null) return (213);
            //try{y = Double.parseDouble(Tok[n]);}catch(NumberFormatException ex) {return(213);}
            if (y < 0.0 || y > 255.0) return (213);
            net.PageSize = y.intValue();
            return (0);
        }


        if (Utilities.match(Tok[0], EnumVariables.w_STATUS)) {
            if (Utilities.match(Tok[n], EnumVariables.w_NO)) net.Statflag = StatFlag.FALSE;
            if (Utilities.match(Tok[n], EnumVariables.w_YES)) net.Statflag = StatFlag.TRUE;
            if (Utilities.match(Tok[n], EnumVariables.w_FULL)) net.Statflag = StatFlag.FULL;
            return (0);
        }

        if (Utilities.match(Tok[0], EnumVariables.w_SUMMARY)) {
            if (Utilities.match(Tok[n], EnumVariables.w_NO)) net.Summaryflag = false;
            if (Utilities.match(Tok[n], EnumVariables.w_YES)) net.Summaryflag = true;
            return (0);
        }

        if (Utilities.match(Tok[0], EnumVariables.w_MESSAGES)) {
            if (Utilities.match(Tok[n], EnumVariables.w_NO)) net.Messageflag = false;
            if (Utilities.match(Tok[n], EnumVariables.w_YES)) net.Messageflag = true;
            return (0);
        }

        if (Utilities.match(Tok[0], EnumVariables.w_ENERGY)) {
            if (Utilities.match(Tok[n], EnumVariables.w_NO)) net.Energyflag = false;
            if (Utilities.match(Tok[n], EnumVariables.w_YES)) net.Energyflag = true;
            return (0);
        }

        if (Utilities.match(Tok[0], EnumVariables.w_NODE)) {
            if (Utilities.match(Tok[n], EnumVariables.w_NONE)) net.Nodeflag = ReportFlag.FALSE;
            else if (Utilities.match(Tok[n], EnumVariables.w_ALL)) net.Nodeflag = ReportFlag.TRUE;
            else {
                if (nodesCount == 0) return (208);
                for (int ii = 1; ii <= n; ii++) {
                    if ((j = findNode(Tok[n])) == 0) return (208);
                    net.getNode(j).setReportFlag(true);
                }
                net.Nodeflag = ReportFlag.SOME;
            }
            return (0);
        }

        if (Utilities.match(Tok[0], EnumVariables.w_LINK)) {
            if (Utilities.match(Tok[n], EnumVariables.w_NONE)) net.Linkflag = ReportFlag.FALSE;
            else if (Utilities.match(Tok[n], EnumVariables.w_ALL)) net.Linkflag = ReportFlag.TRUE;
            else {
                if (linksCount == 0) return (210);
                for (int ii = 1; ii <= n; ii++) {
                    if ((j = findLink(Tok[ii])) == 0) return (210);
                    net.getLink(j).setReportFlag(true);
                }
                net.Linkflag = ReportFlag.SOME;
            }
            return (0);
        }

        int iFieldID = 0;
        if (Tok[0].equalsIgnoreCase(EnumVariables.w_HEADLOSS)) iFieldID = FieldType.HEADLOSS.ordinal();
        else
            iFieldID = Utilities.findMatch(Tok[0], EnumVariables.Fldname);

        if (iFieldID >= 0) {
            if (iFieldID > FieldType.FRICTION.ordinal()) return (201);

            if (Tok.length == 1 || Utilities.match(Tok[1], EnumVariables.w_YES)) {
                net.getField(iFieldID).setEnabled(true);
                return (0);
            }
            if (Utilities.match(Tok[1], EnumVariables.w_NO)) {
                net.getField(iFieldID).setEnabled(false);
                return (0);
            }
            RangeType rj;

            if (Tok.length < 3) return (201);
            if (Utilities.match(Tok[1], EnumVariables.w_BELOW)) rj = RangeType.LOW;
            else if (Utilities.match(Tok[1], EnumVariables.w_ABOVE)) rj = RangeType.HI;
            else if (Utilities.match(Tok[1], EnumVariables.w_PRECISION)) rj = RangeType.PREC;
            else return (201);
            if ((y = Utilities.getFloat(Tok[2])) == null) return (201);
            if (rj == RangeType.PREC) {
                net.getField(iFieldID).setEnabled(true);
                net.getField(iFieldID).setPrecision(roundOff(y));
            } else net.getField(iFieldID).setRptLim(rj.ordinal(), y);
            return (0);
        }

        if (Utilities.match(Tok[0], EnumVariables.w_FILE)) {
            net.Rpt2Fname = Tok[1];
            return (0);
        }

        return (201);
    }


    int parseTime(String[] Tok) {
        int n = Tok.length - 1;
        long t;
        Double y;

        if (n < 1) return (201);

        if (Utilities.match(Tok[0], EnumVariables.w_STATISTIC)) {
            if (Utilities.match(Tok[n], EnumVariables.w_NONE)) net.Tstatflag = TstatType.SERIES;
            else if (Utilities.match(Tok[n], EnumVariables.w_NO)) net.Tstatflag = TstatType.SERIES;
            else if (Utilities.match(Tok[n], EnumVariables.w_AVG)) net.Tstatflag = TstatType.AVG;
            else if (Utilities.match(Tok[n], EnumVariables.w_MIN)) net.Tstatflag = TstatType.MIN;
            else if (Utilities.match(Tok[n], EnumVariables.w_MAX)) net.Tstatflag = TstatType.MAX;
            else if (Utilities.match(Tok[n], EnumVariables.w_RANGE)) net.Tstatflag = TstatType.RANGE;
            else return (201);
            return (0);
        }

        if ((y = Utilities.getFloat(Tok[n])) == null) {
            if ((y = Utilities.hour(Tok[n], "")) < 0.0) {
                if ((y = Utilities.hour(Tok[n - 1], Tok[n])) < 0.0) return (213);
            }
        }
        t = (long) (3600.0 * y);

        if (Utilities.match(Tok[0], EnumVariables.w_DURATION)) net.Dur = t;
        else if (Utilities.match(Tok[0], EnumVariables.w_HYDRAULIC)) net.Hstep = t;
        else if (Utilities.match(Tok[0], EnumVariables.w_QUALITY)) net.Qstep = t;
        else if (Utilities.match(Tok[0], EnumVariables.w_RULE)) net.Rulestep = t;
        else if (Utilities.match(Tok[0], EnumVariables.w_MINIMUM)) return (0);
        else if (Utilities.match(Tok[0], EnumVariables.w_PATTERN)) {
            if (Utilities.match(Tok[1], EnumVariables.w_TIME)) net.Pstep = t;
            else if (Utilities.match(Tok[1], EnumVariables.w_START)) net.Pstart = t;
            else return (201);
        } else if (Utilities.match(Tok[0], EnumVariables.w_REPORT)) {
            if (Utilities.match(Tok[1], EnumVariables.w_TIME)) net.Rstep = t;
            else if (Utilities.match(Tok[1], EnumVariables.w_START)) net.Rstart = t;
            else return (201);
        } else if (Utilities.match(Tok[0], EnumVariables.w_START)) net.Tstart = t % Constants.SECperDAY;
        else return (201);
        return (0);
    }


    int parseOption(String[] Tok) {
        int i, n = Tok.length - 1;

        i = optionchoice(Tok, n);
        if (i >= 0) return (i);
        return (optionvalue(Tok, n));
    }


    int optionchoice(String[] Tok, int n) {

        if (n < 0) return (201);
        if (Utilities.match(Tok[0], EnumVariables.w_UNITS)) {
            if (n < 1) return (0);
            else if (Utilities.match(Tok[1], EnumVariables.w_CFS)) net.Flowflag = FlowUnitsType.CFS;
            else if (Utilities.match(Tok[1], EnumVariables.w_GPM)) net.Flowflag = FlowUnitsType.GPM;
            else if (Utilities.match(Tok[1], EnumVariables.w_AFD)) net.Flowflag = FlowUnitsType.AFD;
            else if (Utilities.match(Tok[1], EnumVariables.w_MGD)) net.Flowflag = FlowUnitsType.MGD;
            else if (Utilities.match(Tok[1], EnumVariables.w_IMGD)) net.Flowflag = FlowUnitsType.IMGD;
            else if (Utilities.match(Tok[1], EnumVariables.w_LPS)) net.Flowflag = FlowUnitsType.LPS;
            else if (Utilities.match(Tok[1], EnumVariables.w_LPM)) net.Flowflag = FlowUnitsType.LPM;
            else if (Utilities.match(Tok[1], EnumVariables.w_CMH)) net.Flowflag = FlowUnitsType.CMH;
            else if (Utilities.match(Tok[1], EnumVariables.w_CMD)) net.Flowflag = FlowUnitsType.CMD;
            else if (Utilities.match(Tok[1], EnumVariables.w_MLD)) net.Flowflag = FlowUnitsType.MLD;
            else if (Utilities.match(Tok[1], EnumVariables.w_SI)) net.Flowflag = FlowUnitsType.LPS;
            else return (201);
        } else if (Utilities.match(Tok[0], EnumVariables.w_PRESSURE)) {
            if (n < 1) return (0);
            else if (Utilities.match(Tok[1], EnumVariables.w_PSI)) net.Pressflag = PressUnitsType.PSI;
            else if (Utilities.match(Tok[1], EnumVariables.w_KPA)) net.Pressflag = PressUnitsType.KPA;
            else if (Utilities.match(Tok[1], EnumVariables.w_METERS)) net.Pressflag = PressUnitsType.METERS;
            else return (201);
        } else if (Utilities.match(Tok[0], EnumVariables.w_HEADLOSS)) {
            if (n < 1) return (0);
            else if (Utilities.match(Tok[1], EnumVariables.w_HW)) net.Formflag = FormType.HW;
            else if (Utilities.match(Tok[1], EnumVariables.w_DW)) net.Formflag = FormType.DW;
            else if (Utilities.match(Tok[1], EnumVariables.w_CM)) net.Formflag = FormType.CM;
            else return (201);
        } else if (Utilities.match(Tok[0], EnumVariables.w_HYDRAULIC)) {
            if (n < 2) return (0);
            else if (Utilities.match(Tok[1], EnumVariables.w_USE)) net.Hydflag = Hydtype.USE;
            else if (Utilities.match(Tok[1], EnumVariables.w_SAVE)) net.Hydflag = Hydtype.SAVE;
            else return (201);
            net.HydFname = Tok[2];
        } else if (Utilities.match(Tok[0], EnumVariables.w_QUALITY)) {
            if (n < 1) return (0);
            else if (Utilities.match(Tok[1], EnumVariables.w_NONE)) net.Qualflag = QualType.NONE;
            else if (Utilities.match(Tok[1], EnumVariables.w_CHEM)) net.Qualflag = QualType.CHEM;
            else if (Utilities.match(Tok[1], EnumVariables.w_AGE)) net.Qualflag = QualType.AGE;
            else if (Utilities.match(Tok[1], EnumVariables.w_TRACE)) net.Qualflag = QualType.TRACE;
            else {
                net.Qualflag = QualType.CHEM;
                net.ChemName = Tok[1];
                if (n >= 2)
                    net.ChemUnits = Tok[2];
            }
            if (net.Qualflag == QualType.TRACE) {

                Tok[0] = "";    //TODO:?
                if (n < 2) return (212);
                Tok[0] = Tok[2];
                net.TraceNode = findNode(Tok[2]);
                if (net.TraceNode == 0) return (212);
                net.ChemName = EnumVariables.u_PERCENT;
                net.ChemUnits = Tok[2];
            }
            if (net.Qualflag == QualType.AGE) {
                net.ChemName = EnumVariables.w_AGE;
                net.ChemUnits = EnumVariables.u_HOURS;
            }
        } else if (Utilities.match(Tok[0], EnumVariables.w_MAP)) {
            if (n < 1) return (0);
            net.MapFname = Tok[1];
        }
        //else if (Tok[0],w_VERIFY))
        //{
        //}
        else if (Utilities.match(Tok[0], EnumVariables.w_UNBALANCED)) {
            if (n < 1) return (0);
            if (Utilities.match(Tok[1], EnumVariables.w_STOP)) net.ExtraIter = -1;
            else if (Utilities.match(Tok[1], EnumVariables.w_CONTINUE)) {
                if (n >= 2) net.ExtraIter = Integer.parseInt(Tok[2]);
                else net.ExtraIter = 0;
            } else return (201);
        } else if (Utilities.match(Tok[0], EnumVariables.w_PATTERN)) {
            if (n < 1) return (0);
            net.DefPatID = Tok[1];
        } else return (-1);
        return (0);
    }


    int optionvalue(String[] Tok, int n) {
        int nvalue = 1;
        Double y;

        if (Utilities.match(Tok[0], EnumVariables.w_SEGMENTS)) return (0);

        if (Utilities.match(Tok[0], EnumVariables.w_SPECGRAV) || Utilities.match(Tok[0], EnumVariables.w_EMITTER)
                || Utilities.match(Tok[0], EnumVariables.w_DEMAND)) nvalue = 2;
        if (n < nvalue) return (0);

        if ((y = Utilities.getFloat(Tok[nvalue])) == null) return (213);

        if (Utilities.match(Tok[0], EnumVariables.w_TOLERANCE)) {
            if (y < 0.0) return (213);
            net.Ctol = y;
            return (0);
        }

        if (Utilities.match(Tok[0], EnumVariables.w_DIFFUSIVITY)) {
            if (y < 0.0) return (213);
            net.Diffus = y;
            return (0);
        }

        if (Utilities.match(Tok[0], EnumVariables.w_DAMPLIMIT)) {
            net.DampLimit = y;
            return (0);
        }

        if (y <= 0.0) return (213);

        if (Utilities.match(Tok[0], EnumVariables.w_VISCOSITY)) net.Viscos = y;
        else if (Utilities.match(Tok[0], EnumVariables.w_SPECGRAV)) net.SpGrav = y;
        else if (Utilities.match(Tok[0], EnumVariables.w_TRIALS)) net.MaxIter = y.intValue();
        else if (Utilities.match(Tok[0], EnumVariables.w_ACCURACY)) {
            y = Math.max(y, 1.e-5);
            y = Math.min(y, 1.e-1);
            net.Hacc = y;
        } else if (Utilities.match(Tok[0], EnumVariables.w_HTOL)) net.Htol = y;
        else if (Utilities.match(Tok[0], EnumVariables.w_QTOL)) net.Qtol = y;
        else if (Utilities.match(Tok[0], EnumVariables.w_RQTOL)) {
            if (y >= 1.0) return (213);
            net.RQtol = y;
        } else if (Utilities.match(Tok[0], EnumVariables.w_CHECKFREQ)) net.CheckFreq = y.intValue();
        else if (Utilities.match(Tok[0], EnumVariables.w_MAXCHECK)) net.MaxCheck = y.intValue();
        else if (Utilities.match(Tok[0], EnumVariables.w_EMITTER)) net.Qexp = 1.0d / y;
        else if (Utilities.match(Tok[0], EnumVariables.w_DEMAND)) net.Dmult = y;
        else return (201);
        return (0);
    }


    /*double  hour(String time, String units)
  {
      int    n=0;
      double  [] y = {0.0d,0.0d,0.0d};
      String [] s = time.split(":");


      for(int i = 0;i<s.length && i<=3;i++)
      {
          try{y[i] = Double.parseDouble(s[i]);}catch(NumberFormatException ex){return(-1.0d);}
          n++;
      }

      if (n == 1)
      {
          if (units.length() == 0)
              return(y[0]);

          if (Utilities.match(units,w_SECONDS)) return(y[0]/3600.0);
          if (Utilities.match(units,w_MINUTES)) return(y[0]/60.0);
          if (Utilities.match(units,w_HOURS))   return(y[0]);
          if (Utilities.match(units,w_DAYS))    return(y[0]*24.0);
      }

      if (n > 1) y[0] = y[0] + y[1]/60.0 + y[2]/3600.0;

      if (units.length() == '\0')
          return(y[0]);

      if (units.equalsIgnoreCase(w_AM))
      {
          if (y[0] >= 13.0) return(-1.0);
          if (y[0] >= 12.0) return(y[0]-12.0);
          else return(y[0]);
      }
      if (units.equalsIgnoreCase(w_PM))
      {
          if (y[0] >= 13.0) return(-1.0);
          if (y[0] >= 12.0) return(y[0]);
          else return(y[0]+12.0);
      }
      return(-1.0);
  }  */


    boolean valvecheck(LinkType type, int j1, int j2) {
        int k, vk, vj1, vj2;
        LinkType vtype;
        /* Examine each existing valve */
        for (k = 1; k <= valvesCount; k++) {
            vk = net.getValve(k).getLink();
            vj1 = net.getLink(vk).getN1();
            vj2 = net.getLink(vk).getN2();
            vtype = net.getLink(vk).getType();

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

    private int getpumpcurve(int link, int pump, int n, Double[] X) {
        double a, b, c, h0, h1, h2, q1, q2;

        Pump activePump = net.getPump(pump);
        Link activeLink = net.getLink(link);

        if (n == 1) {
            if (X[0] <= 0.0) return (202);
            activePump.setPtype(PumpType.CONST_HP);
            activeLink.setKm(X[0]);
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
            } else return (202);
            activePump.setPtype(PumpType.POWER_FUNC);
            double[] abc = powercurve(h0, h1, h2, q1, q2);
            if (abc == null) return (206);

            activePump.setH0(-abc[0]);
            activePump.setR(-abc[1]);
            activePump.setN(abc[2]);
            activePump.setQ0(q1);
            activePump.setQMax(Math.pow((-abc[0] / abc[1]), (1.0 / abc[2])));
            activePump.setHMax(h0);
        }
        return (0);
    }


    double[] powercurve(double h0, double h1, double h2, double q1, double q2) {
        double[] vec = new double[3];
        double h4, h5;
        if (
                h0 < Constants.TINY ||
                        h0 - h1 < Constants.TINY ||
                        h1 - h2 < Constants.TINY ||
                        q1 < Constants.TINY ||
                        q2 - q1 < Constants.TINY
                ) return (null);
        vec[0] = h0;
        h4 = h0 - h1;
        h5 = h0 - h2;
        vec[2] = Math.log(h5 / h4) / Math.log(q2 / q1);
        if (vec[2] <= 0.0d || vec[2] > 20.0d) return (null);
        vec[1] = -h4 / Math.pow(q1, vec[2]);


        if (vec[1] >= 0.0) return (null);

        return (vec);
    }


    void changestatus(int j, StatType status, double y) {
        Link lLink = net.getLink(j);

        if (lLink.getType() == LinkType.PIPE || lLink.getType() == LinkType.GPV) {
            if (status != StatType.ACTIVE) lLink.setStatus(status);
        } else if (lLink.getType() == LinkType.PUMP) {
            if (status == StatType.ACTIVE) {
                lLink.setKc(y);
                status = StatType.OPEN;
                if (y == 0.0) status = StatType.CLOSED;
            } else if (status == StatType.OPEN) lLink.setKc(1.0);
            lLink.setStatus(status);
        } else if (lLink.getType().ordinal() >= LinkType.PRV.ordinal()) {
            lLink.setKc(y);
            lLink.setStatus(status);
            if (status != StatType.ACTIVE) lLink.setKc(Constants.MISSING);
        }
    }

    private int roundOff(double x) {
        return x >= 0 ? (int) (x + .5) : (int) (x - 0.5);
    }

    private int pumpIndex(int id) {
        return roundOff(net.getLink(id).getDiameter());
    }


}
