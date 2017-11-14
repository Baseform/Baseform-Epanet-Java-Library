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

package org.addition.epanet;


import org.addition.epanet.hydraulic.HydraulicSim;
import org.addition.epanet.hydraulic.io.AwareStep;
import org.addition.epanet.hydraulic.io.HydraulicReader;
import org.addition.epanet.network.FieldsMap;
import org.addition.epanet.network.Network;
import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.network.io.input.InputParser;
import org.addition.epanet.network.structures.Demand;
import org.addition.epanet.network.structures.Link;
import org.addition.epanet.network.structures.Node;
import org.addition.epanet.quality.QualitySim;
import org.addition.epanet.util.ENException;
import org.addition.epanet.util.Utilities;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class EPATool {

    public static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    public static void consoleLog(String msg) {
        System.out.println(msg + " " + TIME_FORMAT.format(new Date(System.currentTimeMillis())));
    }

    public static String convertToScientifcNotation(Double value, double max_threshold, double min_threshold, int decimal) {
        if (value == null)
            return null;

        if (value != 0.0 && (Math.abs(value) > max_threshold || Math.abs(value) < min_threshold))
            return String.format("%." + decimal + "e", value);

        return String.format("%." + decimal + "f", value);
    }

    static enum NodeVariableType {
        ELEVATION("ELEVATION", FieldsMap.Type.ELEV),
        PRESSURE("PRESSURE", FieldsMap.Type.PRESSURE),
        HEAD("HEAD", FieldsMap.Type.HEAD),
        QUALITY("QUALITY", FieldsMap.Type.QUALITY),
        INITQUALITY("INITQUALITY", FieldsMap.Type.QUALITY),
        BASEDEMAND("BASEDEMAND", FieldsMap.Type.DEMAND),
        DEMAND("DEMAND", FieldsMap.Type.DEMAND);

        public final String name;
        public final FieldsMap.Type type;

        NodeVariableType(String name, FieldsMap.Type type) {
            this.name = name;
            this.type = type;
        }

        public double getValue(FieldsMap fmap, AwareStep step, Node node, int index) throws ENException {
            switch (this) {
                case BASEDEMAND: {
                    double dsum = 0;
                    for (Demand demand : node.getDemand()) {
                        dsum += demand.getBase();
                    }
                    return fmap.revertUnit(type, dsum);
                }
                case ELEVATION:
                    return fmap.revertUnit(type, node.getElevation());
                case DEMAND:
                    return step != null ? step.getNodeDemand(index, node, fmap) : 0;
                case HEAD:
                    return step != null ? step.getNodeHead(index, node, fmap) : 0;
                case INITQUALITY: {
                    double dsum = 0;
                    for (double v : node.getC0()) {
                        dsum += v;
                    }
                    return dsum != 0 ? fmap.revertUnit(type, dsum / node.getC0().length) : fmap.revertUnit(type, dsum);
                }
                case PRESSURE:
                    return step != null ? step.getNodePressure(index, node, fmap) : 0;
                case QUALITY:
                    return step != null ? step.getNodeQuality(index) : 0;
                default:
                    return 0.0;
            }
        }
    }


    static enum LinkVariableType {
        LENGHT("LENGHT", FieldsMap.Type.LENGTH),
        DIAMETER("DIAMETER", FieldsMap.Type.DIAM),
        ROUGHNESS("ROUGHNESS", null),
        FLOW("FLOW", FieldsMap.Type.FLOW),
        VELOCITY("VELOCITY", FieldsMap.Type.VELOCITY),
        UNITHEADLOSS("UNITHEADLOSS", FieldsMap.Type.HEADLOSS),
        FRICTIONFACTOR("FRICTIONFACTOR", FieldsMap.Type.FRICTION),
        QUALITY("QUALITY", FieldsMap.Type.QUALITY);

        public final String name;
        public final FieldsMap.Type type;

        LinkVariableType(String name, FieldsMap.Type type) {
            this.name = name;
            this.type = type;
        }


        public double getValue(PropertiesMap.FormType formType, FieldsMap fmap, AwareStep step, Link link, int index) throws ENException {
            switch (this) {
                case LENGHT:
                    return fmap.revertUnit(type, link.getLenght());
                case DIAMETER:
                    return fmap.revertUnit(type, link.getDiameter());
                case ROUGHNESS:
                    if (link.getType() == Link.LinkType.PIPE && formType == PropertiesMap.FormType.DW)
                        return fmap.revertUnit(FieldsMap.Type.DIAM, link.getRoughness());
                    else
                        return link.getRoughness();
                case FLOW:
                    return step != null ? step.getLinkFlow(index, link, fmap) : 0;
                case VELOCITY:
                    return step != null ? Math.abs(step.getLinkVelocity(index, link, fmap)) : 0;
                case UNITHEADLOSS:
                    return step != null ? step.getLinkHeadLoss(index, link, fmap) : 0;
                case FRICTIONFACTOR:
                    return step != null ? step.getLinkFriction(index, link, fmap) : 0;
                case QUALITY:
                    return step != null ? fmap.revertUnit(type, step.getLinkAvrQuality(index)) : 0;
                default:
                    return 0.0;
            }
        }
    }


    public static void main(String[] args) {
        Logger log = Logger.getLogger(EPATool.class.toString());
        log.setUseParentHandlers(false);

        File hydFile = null;
        File qualFile = null;
        Network net = new Network();
        PropertiesMap pMap = null;

        List<NodeVariableType> nodesVariables = new ArrayList<NodeVariableType>();
        List<LinkVariableType> linksVariables = new ArrayList<LinkVariableType>();

        File inFile = null;
        List<Long> targetTimes = new ArrayList<Long>();
        List<String> targetNodes = new ArrayList<String>();
        List<String> targetLinks = new ArrayList<String>();

        int parseMode = 0;
        for (int i = 0; i < args.length; i++) {
            if (args[i].endsWith(".inp")) {
                parseMode = 0;
                inFile = new File(args[i]);
                if (!inFile.exists()) {
                    consoleLog("END_RUN_ERR");
                    System.out.println("File not found !");
                    return;
                }
                continue;
            } else if (args[i].equals("-T")) {
                parseMode = 1;
                continue;
            } else if (args[i].equals("-N")) {
                parseMode = 2;
                continue;
            } else if (args[i].equals("-L")) {
                parseMode = 3;
                continue;
            }

            if (parseMode == 1) {
                targetTimes.add((long) (Utilities.getHour(args[i], "") * 3600));
            } else if (parseMode == 2) {
                targetNodes.add(args[i]);
            } else if (parseMode == 3) {
                targetLinks.add(args[i]);
            }
        }

        try {
            InputParser parserINP = InputParser.create(Network.FileType.INP_FILE, log);
            parserINP.parse(net, inFile);
            pMap = net.getPropertiesMap();

            if (targetTimes.size() > 0) {
                for (Long time : targetTimes) {
                    String epanetTime = Utilities.getClockTime(time);
                    if (time < pMap.getRstart())
                        throw new Exception("Target time \"" + epanetTime + "\" smaller than simulation start time");

                    if (time > pMap.getDuration())
                        throw new Exception("Target time \"" + epanetTime + "\" bigger than simulation duration");

                    if ((time - pMap.getRstart()) % pMap.getRstep() != 0)
                        throw new Exception("Target time \"" + epanetTime + "\" not found");
                }
            }

            for (String nodeName : targetNodes) {
                if (net.getNode(nodeName) == null)
                    throw new Exception("Node \"" + nodeName + "\" not found");
            }

            for (String linkName : targetLinks) {
                if (net.getLink(linkName) == null)
                    throw new Exception("Link \"" + linkName + "\" not found");
            }

            nodesVariables.add(NodeVariableType.ELEVATION);
            nodesVariables.add(NodeVariableType.BASEDEMAND);
            if (!pMap.getQualflag().equals(PropertiesMap.QualType.NONE))
                nodesVariables.add(NodeVariableType.INITQUALITY);
            nodesVariables.add(NodeVariableType.PRESSURE);
            nodesVariables.add(NodeVariableType.HEAD);
            nodesVariables.add(NodeVariableType.DEMAND);
            if (!pMap.getQualflag().equals(PropertiesMap.QualType.NONE)) nodesVariables.add(NodeVariableType.QUALITY);

            linksVariables.add(LinkVariableType.LENGHT);
            linksVariables.add(LinkVariableType.DIAMETER);
            linksVariables.add(LinkVariableType.ROUGHNESS);
            linksVariables.add(LinkVariableType.FLOW);
            linksVariables.add(LinkVariableType.VELOCITY);
            linksVariables.add(LinkVariableType.UNITHEADLOSS);
            linksVariables.add(LinkVariableType.FRICTIONFACTOR);
            if (!pMap.getQualflag().equals(PropertiesMap.QualType.NONE)) linksVariables.add(LinkVariableType.QUALITY);

            hydFile = File.createTempFile("hydSim", "bin");

            consoleLog("START_RUNNING");

            HydraulicSim hydSim = new HydraulicSim(net, log);
            hydSim.simulate(hydFile);


            if (!net.getPropertiesMap().getQualflag().equals(PropertiesMap.QualType.NONE)) {
                qualFile = File.createTempFile("qualSim", "bin");

                QualitySim q = new QualitySim(net, log);
                q.simulate(hydFile, qualFile);
            }


            HydraulicReader hydReader = new HydraulicReader(new RandomAccessFile(hydFile, "r"));

            BufferedWriter nodesTextWriter = null;
            BufferedWriter linksTextWriter = null;
            File nodesOutputFile = null;
            File linksOutputFile = null;

            if (targetNodes.size() == 0 && targetLinks.size() == 0 || targetNodes.size() > 0) {
                nodesOutputFile = new File(inFile.getAbsolutePath() + ".nodes.out");
                nodesTextWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(nodesOutputFile), "UTF-8"));

                nodesTextWriter.write("\t");
                for (NodeVariableType nodeVar : nodesVariables) {
                    nodesTextWriter.write("\t");
                    nodesTextWriter.write(nodeVar.name);
                }
                nodesTextWriter.write("\n\t");

                for (NodeVariableType nodeVar : nodesVariables) {
                    nodesTextWriter.write("\t");
                    nodesTextWriter.write(net.getFieldsMap().getField(nodeVar.type).getUnits());
                }
                nodesTextWriter.write("\n");
            }


            if (targetNodes.size() == 0 && targetLinks.size() == 0 || targetLinks.size() > 0) {
                linksOutputFile = new File(inFile.getAbsolutePath() + ".links.out");
                linksTextWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(linksOutputFile), "UTF-8"));

                linksTextWriter.write("\t");
                for (LinkVariableType linkVar : linksVariables) {
                    linksTextWriter.write("\t");
                    linksTextWriter.write(linkVar.name);
                }
                linksTextWriter.write("\n\t");

                for (LinkVariableType linkVar : linksVariables) {
                    linksTextWriter.write("\t");
                    if (linkVar.type == null) {
                        continue;
                    }
                    linksTextWriter.write(net.getFieldsMap().getField(linkVar.type).getUnits());
                }
                linksTextWriter.write("\n");
            }


            for (long time = pMap.getRstart(); time <= pMap.getDuration(); time += pMap.getRstep()) {
                AwareStep step = hydReader.getStep((int) time);

                int i = 0;

                if (targetTimes.size() > 0 && !targetTimes.contains(time))
                    continue;

                if (nodesTextWriter != null) {
                    for (Node node : net.getNodes()) {
                        if (targetNodes.size() > 0 && !targetNodes.contains(node.getId()))
                            continue;

                        nodesTextWriter.write(node.getId());

                        nodesTextWriter.write("\t");
                        nodesTextWriter.write(Utilities.getClockTime(time));

                        for (NodeVariableType nodeVar : nodesVariables) {
                            nodesTextWriter.write("\t");
                            Double val = nodeVar.getValue(net.getFieldsMap(), step, node, i);
                            nodesTextWriter.write(convertToScientifcNotation(val, 1000, 0.01, 2));
                        }

                        nodesTextWriter.write("\n");

                        i++;
                    }
                }

                i = 0;

                if (linksTextWriter != null) {
                    for (Link link : net.getLinks()) {
                        if (targetLinks.size() > 0 && !targetLinks.contains(link.getId()))
                            continue;

                        linksTextWriter.write(link.getId());

                        linksTextWriter.write("\t");
                        linksTextWriter.write(Utilities.getClockTime(time));

                        for (LinkVariableType linkVar : linksVariables) {
                            linksTextWriter.write("\t");
                            Double val = linkVar.getValue(net.getPropertiesMap().getFormflag(), net.getFieldsMap(), step, link, i);
                            linksTextWriter.write(convertToScientifcNotation(val, 1000, 0.01, 2));
                        }

                        linksTextWriter.write("\n");

                        i++;
                    }
                }
            }

            if (nodesTextWriter != null) {
                nodesTextWriter.close();
                consoleLog("NODES FILE \"" + nodesOutputFile.getAbsolutePath() + "\"");
            }

            if (linksTextWriter != null) {
                linksTextWriter.close();
                consoleLog("LINKS FILES \"" + nodesOutputFile.getAbsolutePath() + "\"");
            }

            consoleLog("END_RUN_OK");
        } catch (ENException e) {
            consoleLog("END_RUN_ERR");
            e.printStackTrace();
        } catch (IOException e) {
            consoleLog("END_RUN_ERR");
            e.printStackTrace();
        } catch (Exception e) {
            consoleLog("END_RUN_ERR");
            e.printStackTrace();
        }

        if (hydFile != null)
            hydFile.delete();

        if (qualFile != null)
            qualFile.delete();
    }
}
