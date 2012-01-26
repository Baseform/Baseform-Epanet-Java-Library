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

package org.addition.epanet.ui;


import org.addition.epanet.Constants;
import org.addition.epanet.hydraulic.io.AwareStep;
import org.addition.epanet.hydraulic.io.HydraulicReader;
import org.addition.epanet.msx.ENToolkit2;
import org.addition.epanet.msx.EpanetMSX;
import org.addition.epanet.msx.MsxReader;
import org.addition.epanet.msx.Structures.Species;
import org.addition.epanet.network.FieldsMap;
import org.addition.epanet.network.Network;
import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.network.structures.Link;
import org.addition.epanet.network.structures.Node;
import org.addition.epanet.quality.QualityReader;
import org.addition.epanet.util.ENException;
import org.addition.epanet.util.Utilities;
import org.addition.epanet.util.XLSXWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Iterator;

/**
 * This class handles the XLSX generation from the binary files created by Epanet and MSX simulations, the reported
 * fields are configured via the ReportOptions class.
 */
public class ReportGenerator {

    /**
     * Hydraulic report fields.
     */
    public enum HydVariable {
        HYDR_VARIABLE_HEAD(0, "Node head", true),
        HYDR_VARIABLE_DEMANDS(1, "Node actual demand", true),
        HYDR_VARIABLE_PRESSURE(2, "Node pressure", true),
        HYDR_VARIABLE_FLOWS(3, "Link flows", false),
        HYDR_VARIABLE_VELOCITY(4, "Link velocity", false),
        HYDR_VARIABLE_HEADLOSS(5, "Link unit headloss", false),
        HYDR_VARIABLE_FRICTION(6, "Link friction factor", false);
        public final boolean isNode;
        public final int id;
        public final String name;

        HydVariable(int val, String text, boolean node) {
            id = val;
            name = text;
            isNode = node;
        }

        public static String[] getNames() {
            String[] ret = new String[values().length];
            for (int i = 0; i < HydVariable.values().length; i++)
                ret[i] = HydVariable.values()[i].name;
            return ret;
        }
    }

    /**
     * Quality report fields.
     */
    public enum QualVariable {
        QUAL_VARIABLE_NODES(0, "Node quality", true),
        QUAL_VARIABLE_LINKS(1, "Link quality", false);
        //QUAL_VARIABLE_RATE      (2,"Link reaction rate",false);

        public final boolean isNode;
        public final int id;
        public final String name;

        QualVariable(int val, String text, boolean node) {
            id = val;
            name = text;
            isNode = node;
        }

        public static String[] getNames() {
            String[] ret = new String[values().length];
            for (int i = 0; i < QualVariable.values().length; i++)
                ret[i] = QualVariable.values()[i].name;
            return ret;
        }
    }

    private XLSXWriter sheet;
    private File xlsxFile;

    /**
     * Current report time.
     */
    private long Rtime;


    public ReportGenerator(File xlsxFile) {
        this.xlsxFile = xlsxFile;
        sheet = new XLSXWriter();

    }

    /**
     * Set excel cells transposition mode.
     *
     * @param value
     */
    public void setTransposedMode(boolean value) {
        sheet.setTransposedMode(value);
    }

    /**
     * Get current report time progress.
     *
     * @return
     */
    public long getRtime() {
        return Rtime;
    }

    /**
     * Generate hydraulic report.
     *
     * @param hydBinFile Abstract representation of the hydraulic simulation output file.
     * @param net        Hydraulic network.
     * @param values     Variables report flag.
     * @throws IOException
     * @throws org.addition.epanet.util.ENException
     *
     */
    public void createHydReport(File hydBinFile, Network net, boolean[] values) throws IOException, ENException {
        Rtime = 0;
        HydraulicReader dseek = new HydraulicReader(new RandomAccessFile(hydBinFile, "r"));

        int reportCount = (int) ((net.getPropertiesMap().getDuration() - net.getPropertiesMap().getRstart()) / net.getPropertiesMap().getRstep()) + 1;

        Object[] nodesHead = new String[dseek.getNodes() + 1];

        if (sheet.getTransposedMode())
            nodesHead[0] = "Node/Time";
        else
            nodesHead[0] = "Time/Node";

        int count = 1;
        for (Node node : net.getNodes()) {
            nodesHead[count++] = node.getId();
        }

        Object[] linksHead = new String[dseek.getLinks() + 1];

        if (sheet.getTransposedMode())
            linksHead[0] = "Link/Time";
        else
            linksHead[0] = "Time/Link";

        count = 1;
        for (Link link : net.getLinks()) {
            linksHead[count++] = link.getId();
        }

        XLSXWriter.Spreadsheet resultSheets[] = new XLSXWriter.Spreadsheet[HydVariable.values().length];
        Arrays.fill(resultSheets, null);

        for (HydVariable var : HydVariable.values()) {
            if (values == null || values[var.id]) {
                resultSheets[var.id] = sheet.newSpreadsheet(var.name);
                if (var.isNode) {
                    if (sheet.getTransposedMode())
                        resultSheets[var.id].prepareTranspose(nodesHead.length, reportCount + 1);
                    resultSheets[var.id].addRow(nodesHead);
                } else {
                    if (sheet.getTransposedMode())
                        resultSheets[var.id].prepareTranspose(linksHead.length, reportCount + 1);
                    resultSheets[var.id].addRow(linksHead);
                }
            }
        }

        Object nodeRow[] = new Object[dseek.getNodes() + 1];
        Object linkRow[] = new Object[dseek.getLinks() + 1];


        for (long time = net.getPropertiesMap().getRstart();
             time <= net.getPropertiesMap().getDuration();
             time += net.getPropertiesMap().getRstep()) {
            AwareStep step = dseek.getStep(time);
            if (step != null) {
                nodeRow[0] = Utilities.getClockTime(time);
                linkRow[0] = Utilities.getClockTime(time);

                int i;

                // NODES HEADS
                if (resultSheets[HydVariable.HYDR_VARIABLE_HEAD.id] != null) {
                    i = 0;
                    for (Node node : net.getNodes())
                        nodeRow[i + 1] = (double) step.getNodeHead(i++, node, net.getFieldsMap());

                    resultSheets[HydVariable.HYDR_VARIABLE_HEAD.id].addRow(nodeRow);
                }

                // NODES DEMANDS
                if (resultSheets[HydVariable.HYDR_VARIABLE_DEMANDS.id] != null) {
                    i = 0;
                    for (Node node : net.getNodes())
                        nodeRow[i + 1] = (double) step.getNodeDemand(i++, node, net.getFieldsMap());
                    resultSheets[HydVariable.HYDR_VARIABLE_DEMANDS.id].addRow(nodeRow);
                }

                // NODES PRESSURE
                if (resultSheets[HydVariable.HYDR_VARIABLE_PRESSURE.id] != null) {

                    i = 0;
                    for (Node node : net.getNodes())
                        nodeRow[i + 1] = (double) step.getNodePressure(i++, node, net.getFieldsMap());
                    resultSheets[HydVariable.HYDR_VARIABLE_PRESSURE.id].addRow(nodeRow);
                }

                // LINK FLOW
                if (resultSheets[HydVariable.HYDR_VARIABLE_FLOWS.id] != null) {
                    i = 0;
                    for (Link link : net.getLinks())
                        linkRow[i + 1] = (double) step.getLinkFlow(i++, link, net.getFieldsMap());
                    resultSheets[HydVariable.HYDR_VARIABLE_FLOWS.id].addRow(linkRow);
                }

                // LINK VELOCITY
                if (resultSheets[HydVariable.HYDR_VARIABLE_VELOCITY.id] != null) {
                    i = 0;
                    for (Link link : net.getLinks())
                        linkRow[i + 1] = (double) step.getLinkVelocity(i++, link, net.getFieldsMap());
                    resultSheets[HydVariable.HYDR_VARIABLE_VELOCITY.id].addRow(linkRow);
                }

                // LINK HEADLOSS
                if (resultSheets[HydVariable.HYDR_VARIABLE_HEADLOSS.id] != null) {

                    i = 0;
                    for (Link link : net.getLinks())
                        linkRow[i + 1] = (double) step.getLinkHeadLoss(i++, link, net.getFieldsMap());
                    resultSheets[HydVariable.HYDR_VARIABLE_HEADLOSS.id].addRow(linkRow);
                }

                // LINK FRICTION
                if (resultSheets[HydVariable.HYDR_VARIABLE_FRICTION.id] != null) {
                    i = 0;
                    for (Link link : net.getLinks())
                        linkRow[i + 1] = (double) step.getLinkFriction(i++, link, net.getFieldsMap());
                    resultSheets[HydVariable.HYDR_VARIABLE_FRICTION.id].addRow(linkRow);
                }
            }
            Rtime = time;
        }


        dseek.close();
    }

    /**
     * Generate quality report.
     *
     * @param qualFile Abstract representation of the quality simulation output file.
     * @param net      Hydraulic network.
     * @param nodes    Show nodes quality flag.
     * @param links    Show links quality flag.
     * @throws IOException
     * @throws ENException
     */
    public void createQualReport(File qualFile, Network net, boolean nodes, boolean links) throws IOException, ENException {
        Rtime = 0;
        QualityReader dseek = new QualityReader(net.getFieldsMap());

        int reportCount = (int) ((net.getPropertiesMap().getDuration() - net.getPropertiesMap().getRstart()) / net.getPropertiesMap().getRstep()) + 1;

        dseek.open(qualFile);

        Object[] nodesHead = new String[dseek.getNodes() + 1];
        if (sheet.getTransposedMode())
            nodesHead[0] = "Node/Time";
        else
            nodesHead[0] = "Time/Node";
        int count = 1;
        for (Node node : net.getNodes()) {
            nodesHead[count++] = node.getId();
        }

        Object[] linksHead = new String[dseek.getLinks() + 1];
        if (sheet.getTransposedMode())
            linksHead[0] = "Link/Time";
        else
            linksHead[0] = "Time/Link";
        count = 1;
        for (Link link : net.getLinks()) {
            linksHead[count++] = link.getId();
        }

        XLSXWriter.Spreadsheet resultSheets[] = new XLSXWriter.Spreadsheet[HydVariable.values().length];
        Arrays.fill(resultSheets, null);

        for (QualVariable var : QualVariable.values()) {
            if (var.isNode && nodes || !var.isNode && links) {
                resultSheets[var.id] = sheet.newSpreadsheet(var.name);
                if (var.isNode) {
                    if (sheet.getTransposedMode())
                        resultSheets[var.id].prepareTranspose(nodesHead.length, reportCount + 1);
                    resultSheets[var.id].addRow(nodesHead);
                } else {
                    if (sheet.getTransposedMode())
                        resultSheets[var.id].prepareTranspose(linksHead.length, reportCount + 1);
                    resultSheets[var.id].addRow(linksHead);
                }
            }
        }

        Object nodeRow[] = new Object[dseek.getNodes() + 1];
        Object linkRow[] = new Object[dseek.getLinks() + 1];

        Iterator<QualityReader.Step> qIt = dseek.iterator();
        for (long time = net.getPropertiesMap().getRstart();
             time <= net.getPropertiesMap().getDuration();
             time += net.getPropertiesMap().getRstep()) {
            if (!qIt.hasNext()) {
                return;
            }

            QualityReader.Step step = qIt.next();
            if (step != null) {
                nodeRow[0] = Utilities.getClockTime(time);
                linkRow[0] = Utilities.getClockTime(time);


                if (resultSheets[QualVariable.QUAL_VARIABLE_NODES.id] != null) {
                    for (int i = 0; i < dseek.getNodes(); i++)
                        nodeRow[i + 1] = (double) step.getNodeQuality(i);
                    resultSheets[HydVariable.HYDR_VARIABLE_HEAD.id].addRow(nodeRow);
                }


                if (resultSheets[QualVariable.QUAL_VARIABLE_LINKS.id] != null) {
                    for (int i = 0; i < dseek.getLinks(); i++)
                        linkRow[i + 1] = (double) step.getLinkQuality(i);
                    resultSheets[HydVariable.HYDR_VARIABLE_DEMANDS.id].addRow(linkRow);
                }
                Rtime = time;

            }
        }


        dseek.close();
    }

    /**
     * Generate multi-species quality report.
     *
     * @param msxBin Abstract representation of the MSX simulation output file.
     * @param net    Hydraulic network.
     * @param netMSX MSX network.
     * @param tk2    Hydraulic network - MSX bridge.
     * @param values Species report flag.
     * @throws IOException
     * @throws ENException
     */
    public void createMSXReport(File msxBin, Network net, EpanetMSX netMSX, ENToolkit2 tk2, boolean[] values) throws IOException, ENException {
        Rtime = 0;
        org.addition.epanet.msx.Structures.Node[] nodes = netMSX.getNetwork().getNodes();
        org.addition.epanet.msx.Structures.Link[] links = netMSX.getNetwork().getLinks();
        String[] nSpecies = netMSX.getSpeciesNames();

        int reportCount = (int) ((net.getPropertiesMap().getDuration() - net.getPropertiesMap().getRstart()) / net.getPropertiesMap().getRstep()) + 1;

        MsxReader reader = new MsxReader(nodes.length - 1, links.length - 1, nSpecies.length, netMSX.getResultsOffset());

        int totalSpecies;

        if (values != null) {
            totalSpecies = 0;
            for (boolean b : values)
                if (b)
                    totalSpecies++;
        } else
            totalSpecies = nSpecies.length;

        reader.open(msxBin);


        Object[] nodesHead = new String[nSpecies.length + 1];
        if (sheet.getTransposedMode())
            nodesHead[0] = "Node/Time";
        else
            nodesHead[0] = "Time/Node";
        Object[] linksHead = new String[nSpecies.length + 1];
        if (sheet.getTransposedMode())
            linksHead[0] = "Link/Time";
        else
            linksHead[0] = "Time/Link";

        int count = 1;
        for (int i = 0; i < nSpecies.length; i++)
            if (values == null || values[i]) {
                nodesHead[count] = nSpecies[i];
                linksHead[count++] = nSpecies[i];
            }

        Object nodeRow[] = new Object[totalSpecies + 1];
        for (int i = 1; i < nodes.length; i++) {
            if (nodes[i].getRpt()) {
                XLSXWriter.Spreadsheet spr = sheet.newSpreadsheet("Node&lt;&lt;" + tk2.ENgetnodeid(i) + "&gt;&gt;");
                if (sheet.getTransposedMode()) spr.prepareTranspose(nodesHead.length, reportCount + 1);
                spr.addRow(nodesHead);

                for (long time = net.getPropertiesMap().getRstart(), period = 0;
                     time <= net.getPropertiesMap().getDuration();
                     time += net.getPropertiesMap().getRstep(), period++) {

                    nodeRow[0] = Utilities.getClockTime(time);

                    for (int j = 0, ji = 0; j < nSpecies.length; j++) {
                        if (values == null || values[j])
                            nodeRow[ji++ + 1] = reader.getNodeQual((int) period, i, j + 1);
                    }

                    spr.addRow(nodeRow);
                }
            }
        }

        Object linkRow[] = new Object[totalSpecies + 1];
        for (int i = 1; i < links.length; i++) {
            if (links[i].getRpt()) {
                XLSXWriter.Spreadsheet spr = sheet.newSpreadsheet("Link&lt;&lt;" + tk2.ENgetlinkid(i) + "&gt;&gt;");
                if (sheet.getTransposedMode()) spr.prepareTranspose(linksHead.length, reportCount + 1);
                spr.addRow(linksHead);

                for (long time = net.getPropertiesMap().getRstart(), period = 0;
                     time <= net.getPropertiesMap().getDuration();
                     time += net.getPropertiesMap().getRstep(), period++) {

                    linkRow[0] = Utilities.getClockTime(time);

                    for (int j = 0, ji = 0; j < nSpecies.length; j++) {
                        if (values == null || values[j])
                            linkRow[ji++ + 1] = reader.getLinkQual((int) period, i, j + 1);
                    }

                    spr.addRow(linkRow);
                }
            }

        }

        reader.close();
    }

    /**
     * Write the final worksheet.
     *
     * @throws IOException
     */
    public void writeWorksheet() throws IOException {
        sheet.save(new FileOutputStream(xlsxFile));
        sheet.finish();

    }

    /**
     * Write simulation summary to one worksheet.
     *
     * @param inpFile Hydraulic network file.
     * @param net     Hydraulic network.
     * @param msxFile MSX file.
     * @param msx     MSX solver.
     * @throws IOException
     */
    public void writeSummary(File inpFile, Network net, File msxFile, EpanetMSX msx) throws IOException {
        XLSXWriter.Spreadsheet sh = sheet.newSpreadsheet("Summary");

        try {
            PropertiesMap pMap = net.getPropertiesMap();
            FieldsMap fMap = net.getFieldsMap();

            if (net.getTitleText() != null)
                for (int i = 0; i < net.getTitleText().size() && i < 3; i++) {
                    if (net.getTitleText().get(i).length() > 0) {
                        if (net.getTitleText().get(i).length() <= 70)
                            sh.addRow(net.getTitleText().get(i));
                    }
                }
            sh.addRow("\n");
            sh.addRow(Utilities.getText("FMT19"), inpFile.getName());
            sh.addRow(Utilities.getText("FMT20"), net.getJunctions().size());

            int nReservoirs = 0;
            int nTanks = 0;
            for (org.addition.epanet.network.structures.Tank tk : net.getTanks())
                if (tk.getArea() == 0)
                    nReservoirs++;
                else
                    nTanks++;

            int nValves = net.getValves().size();
            int nPumps = net.getPumps().size();
            int nPipes = net.getLinks().size() - nPumps - nValves;

            sh.addRow(Utilities.getText("FMT21a"), nReservoirs);
            sh.addRow(Utilities.getText("FMT21b"), nTanks);
            sh.addRow(Utilities.getText("FMT22"), nPipes);
            sh.addRow(Utilities.getText("FMT23"), nPumps);
            sh.addRow(Utilities.getText("FMT24"), nValves);
            sh.addRow(Utilities.getText("FMT25"), pMap.getFormflag().parseStr);

            sh.addRow(Utilities.getText("FMT26"), Utilities.getClockTime(pMap.getHstep()));
            sh.addRow(Utilities.getText("FMT27"), pMap.getHacc());
            sh.addRow(Utilities.getText("FMT27a"), pMap.getCheckFreq());
            sh.addRow(Utilities.getText("FMT27b"), pMap.getMaxCheck());
            sh.addRow(Utilities.getText("FMT27c"), pMap.getDampLimit());
            sh.addRow(Utilities.getText("FMT28"), pMap.getMaxIter());

            if (pMap.getQualflag() == PropertiesMap.QualType.NONE || pMap.getDuration() == 0.0)
                sh.addRow(Utilities.getText("FMT29"), "None");
            else if (pMap.getQualflag() == PropertiesMap.QualType.CHEM)
                sh.addRow(Utilities.getText("FMT30"), pMap.getChemName());
            else if (pMap.getQualflag() == PropertiesMap.QualType.TRACE)
                sh.addRow(Utilities.getText("FMT31"), "Trace From Node", net.getNode(pMap.getTraceNode()).getId());
            else if (pMap.getQualflag() == PropertiesMap.QualType.AGE)
                sh.addRow(Utilities.getText("FMT32"), "Age");

            if (pMap.getQualflag() != PropertiesMap.QualType.NONE && pMap.getDuration() > 0) {
                sh.addRow(Utilities.getText("FMT33"), "Time Step", Utilities.getClockTime(pMap.getQstep()));
                sh.addRow(Utilities.getText("FMT34"), "Tolerance", fMap.revertUnit(FieldsMap.Type.QUALITY, pMap.getCtol()),
                        fMap.getField(FieldsMap.Type.QUALITY).getUnits());
            }

            sh.addRow(Utilities.getText("FMT36"), pMap.getSpGrav());
            sh.addRow(Utilities.getText("FMT37a"), pMap.getViscos() / Constants.VISCOS);
            sh.addRow(Utilities.getText("FMT37b"), pMap.getDiffus() / Constants.DIFFUS);
            sh.addRow(Utilities.getText("FMT38"), pMap.getDmult());
            sh.addRow(Utilities.getText("FMT39"), fMap.revertUnit(FieldsMap.Type.TIME, pMap.getDuration()), fMap.getField(FieldsMap.Type.TIME).getUnits());

            if (msxFile != null && msx != null) {
                sh.addRow("");
                sh.addRow("MSX data file", msxFile.getName());
                sh.addRow("Species");
                Species[] spe = msx.getNetwork().getSpecies();
                for (int i = 1; i < msx.getNetwork().getSpecies().length; i++)
                    sh.addRow(spe[i].getId(), spe[i].getUnits());
            }
        } catch (IOException e) {

        } catch (ENException e) {
            e.printStackTrace();
        }
    }


}
