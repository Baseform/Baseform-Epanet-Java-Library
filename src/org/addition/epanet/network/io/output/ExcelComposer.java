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

package org.addition.epanet.network.io.output;


import org.addition.epanet.Constants;
import org.addition.epanet.util.ENException;
import org.addition.epanet.network.FieldsMap;
import org.addition.epanet.network.FieldsMap.Type;
import org.addition.epanet.network.Network;
import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.network.io.Keywords;
import org.addition.epanet.network.structures.*;
import org.addition.epanet.network.structures.Control.ControlType;
import org.addition.epanet.network.structures.Link.LinkType;
import org.addition.epanet.network.structures.Link.StatType;
import org.addition.epanet.util.Utilities;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * EXCEL XLSX composer class.
 */
public class ExcelComposer extends OutputComposer {


    class ExcelWriter {
        Row activeRow = null;
        Sheet activeSheet;
        int cellCount = 0;
        int rowCount = 0;

        CellStyle timeStyle;
        CellStyle topBold;

        ExcelWriter() {
            topBold = workbook.createCellStyle();
            Font newFont = workbook.createFont();
            newFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
            topBold.setFont(newFont);

            timeStyle = workbook.createCellStyle();
            timeStyle.setDataFormat((short)0x2e);
        }

        public void newLine() throws IOException {
            activeRow = activeSheet.createRow(rowCount++);
            cellCount = 0;
        }


        private void newSpreadsheet(String name) {
            rowCount = 0;
            cellCount = 0;
            activeSheet = workbook.createSheet(name);
            activeRow = null;
        }


        public void write(Object... args) {
            if (activeRow == null) {
                activeRow = activeSheet.createRow(rowCount++);
                cellCount = 0;
            }

            for (Object obj : args) {
                if (obj instanceof String && obj.equals(NEWLINE)) {
                    activeRow = activeSheet.createRow(rowCount++);
                    cellCount = 0;
                    continue;
                }

                Cell c = activeRow.createCell(cellCount++);

                if(obj==null)
                    c.setCellType(Cell.CELL_TYPE_BLANK);
                else if(obj instanceof Date){
                    c.setCellValue( ((double)((Date)obj).getTime()) /86400.0d  );
                    c.setCellStyle(timeStyle);
                }
                else if (obj instanceof Boolean)
                    c.setCellValue(((Boolean) obj));
                else if (obj instanceof Number)
                    c.setCellValue(((Number) obj).doubleValue());
                else
                    c.setCellValue( obj.toString());
            }
        }

        public void writeHeader(String str) throws IOException {
            String[] sections = str.split("[\t]+");
            //for (int i = 0; i < sections.length; i++)
            //    sections[i] = ";" + sections[i];
            //if(sections.length>0)
            //    sections[0] = ";" + sections[0];

            //write(sections);
            if (activeRow == null) {
                activeRow = activeSheet.createRow(rowCount++);
                cellCount = 0;
            }

            for (String obj : sections) {
                Cell c = activeRow.createCell(cellCount++);
                c.setCellStyle(topBold);
                c.setCellValue( obj.toString());
            }

            newLine();
        }
    }

    private static final String COORDINATES_SUBTITLE = "Node\tX-Coord\tY-Coord";
    private static final String CURVE_SUBTITLE = "ID\tX-Value\tY-Value";
    private static final String DEMANDS_SUBTITLE = "Junction\tDemand\tPattern\tCategory";
    private static final String EMITTERS_SUBTITLE = "Junction\tCoefficient";
    private static final String JUNCS_SUBTITLE = "ID\tElev\tDemand\tPattern\tComment";
    private static final String LABELS_SUBTITLE = "X-Coord\tY-Coord\tLabel & Anchor Node";
    private static final String MIXING_SUBTITLE = "Tank\tModel";
    private static final String NEWLINE = "\n";
    private static final String PATTERNS_SUBTITLE = "ID\tMultipliers";
    private static final String PIPES_SUBTITLE = "ID\tNode1\tNode2\tLength\tDiameter\tRoughness\tMinorLoss\tStatus\tComment";
    private static final String PUMPS_SUBTITLE = "ID\tNode1\tNode2\tParameters\tValue\tComment";
    private static final String QUALITY_SUBTITLE = "Node\tInitQual";
    private static final String REACTIONS_SUBTITLE = "Type\tPipe/Tank";
    private static final String RESERVOIRS_SUBTITLE = "ID\tHead\tPattern\tComment";
    private static final String SOURCE_SUBTITLE = "Node\tType\tQuality\tPattern";
    private static final String STATUS_SUBTITLE = "ID\tStatus/Setting";
    private static final String TANK_SUBTITLE = "ID\tElevation\tInitLevel\tMinLevel\tMaxLevel\tDiameter\tMinVol\tVolCurve\tComment";
    private static final String TITLE_SUBTITLE  = "Text";
    private static final String VALVES_SUBTITLE = "ID\tNode1\tNode2\tDiameter\tType\tSetting\tMinorLoss\tComment";

    //private static final String TITLE_TAG       = "[TITLE]";
    //private static final String JUNCTIONS_TAG   = "[JUNCTIONS]";
    //private static final String TANKS_TAG       = "[TANKS]";
    //private static final String RESERVOIRS_TAG  = "[RESERVOIRS]";
    //private static final String PIPES_TAG       = "[PIPES]";
    //private static final String PUMPS_TAG       = "[PUMPS]";
    //private static final String VALVES_TAG      = "[VALVES]";
    //private static final String DEMANDS_TAG     = "[DEMANDS]";
    //private static final String EMITTERS_TAG    = "[EMITTERS]";
    //private static final String STATUS_TAG      = "[STATUS]";
    //private static final String PATTERNS_TAG    = "[PATTERNS]";
    //private static final String CURVES_TAG      = "[CURVES]";
    //private static final String CONTROLS_TAG    = "[CONTROLS]";
    //private static final String QUALITY_TAG     = "[QUALITY]";
    //private static final String SOURCE_TAG      = "[SOURCE]";
    //private static final String MIXING_TAG      = "[MIXING]";
    //private static final String REACTIONS_TAG   = "[REACTIONS]";
    //private static final String ENERGY_TAG      = "[ENERGY]";
    //private static final String TIMES_TAG       = "[TIMES]";
    //private static final String OPTIONS_TAG     = "[OPTIONS]";
    //private static final String REPORT_TAG      = "[REPORT]";
    //private static final String COORDINATES_TAG = "[COORDINATES]";
    //private static final String RULES_TAG       = "[RULES]";
    //private static final String VERTICES_TAG    = "[VERTICES]";
    //private static final String LABELS_TAG      = "[LABELS]";


    private static final String VERTICES_SUBTITLE = "Link\tX-Coord\tY-Coord";

    Workbook workbook;

    ExcelWriter writer;

    public ExcelComposer() {
    }

    private void composeControls(Network net) throws IOException, ENException {
        Control[] controls = net.getControls();
        FieldsMap fmap = net.getFieldsMap();

        writer.write(Network.SectType.CONTROLS.parseStr,NEWLINE);
        writer.writeHeader("Code");

        for (Control control : controls) {
            // Check that controlled link exists
            if (control.getLink() == null) continue;

            // Get text of control's link status/setting
            if (control.getSetting() == Constants.MISSING)
                writer.write("LINK", control.getLink().getId(), control.getStatus().parseStr);
            else {
                Double kc = control.getSetting();
                switch (control.getLink().getType()) {
                    case PRV:
                    case PSV:
                    case PBV:
                        kc = fmap.revertUnit(Type.PRESSURE, kc);
                        break;
                    case FCV:
                        kc = fmap.revertUnit(Type.FLOW, kc);
                        break;
                }
                writer.write("LINK", control.getLink().getId(), kc);
            }


            switch (control.getType()) {
                // Print level control
                case LOWLEVEL:
                case HILEVEL:
                    double kc = control.getGrade() - control.getNode().getElevation();
                    if (control.getNode() instanceof Tank) kc = fmap.revertUnit(FieldsMap.Type.HEAD, kc);
                    else
                        kc = fmap.revertUnit(FieldsMap.Type.PRESSURE, kc);
                    writer.write("IF", "NODE", control.getNode().getId(), control.getType().parseStr, kc);
                    break;

                // Print timer control
                case TIMER:
                    writer.write("AT",
                            ControlType.TIMER.parseStr, control.getTime() / 3600.0f, "HOURS");
                    break;

                // Print time-of-day control
                case TIMEOFDAY:
                    writer.write("AT", ControlType.TIMEOFDAY.parseStr, Utilities.getClockTime(control.getTime()));
                    break;
            }
            writer.newLine();
        }
        writer.newLine();
    }

    private void composeCoordinates(Network net) throws IOException {
        writer.write(Network.SectType.COORDS.parseStr,NEWLINE);
        writer.writeHeader(COORDINATES_SUBTITLE);

        for (Node node : net.getNodes()) {
            if (node.getPosition() != null) {
                writer.write(node.getId(), node.getPosition().getX(), node.getPosition().getY(), NEWLINE);
            }
        }
        writer.newLine();
    }


    private void composeCurves(Network net) throws IOException, ENException {

        List<Curve> curves = new ArrayList<Curve>(net.getCurves());

        writer.write(Network.SectType.CURVES.parseStr,NEWLINE);
        writer.writeHeader(CURVE_SUBTITLE);

        for (Curve c : curves) {
            for (int i = 0; i < c.getNpts(); i++) {
                writer.write(c.getId(), c.getX().get(i), c.getY().get(i));
                writer.newLine();
            }
        }

        writer.newLine();
    }

    private void composeDemands(Network net) throws IOException, ENException {
        FieldsMap fMap = net.getFieldsMap();

        writer.write(Network.SectType.DEMANDS.parseStr,NEWLINE);
        writer.writeHeader(DEMANDS_SUBTITLE);

        double ucf = fMap.getUnits(FieldsMap.Type.DEMAND);

        for (Node node : net.getJunctions()) {

            if (node.getDemand().size() > 1)
                for(int i = 1;i<node.getDemand().size();i++){
                    Demand demand = node.getDemand().get(i);
                    writer.write(node.getId(), ucf * demand.getBase());
                    if (demand.getPattern() != null && !demand.getPattern().getId().equals(""))
                        writer.write(demand.getPattern().getId());
                    writer.newLine();
                }
        }

        writer.newLine();
    }

    private void composeEmitters(Network net) throws IOException, ENException {
        writer.write(Network.SectType.EMITTERS.parseStr,NEWLINE);
        writer.writeHeader(EMITTERS_SUBTITLE);

        double uflow = net.getFieldsMap().getUnits(Type.FLOW);
        double upressure = net.getFieldsMap().getUnits(Type.PRESSURE);
        double Qexp = net.getPropertiesMap().getQexp();

        for (Node node : net.getJunctions()) {
            if (node.getKe() == 0.0) continue;
            double ke = uflow / Math.pow(upressure * node.getKe(), (1.0 / Qexp));
            writer.write(node.getId(), ke);
            writer.newLine();
        }

        writer.newLine();
    }

    private void composeEnergy(Network net) throws IOException, ENException {
        PropertiesMap pMap = net.getPropertiesMap();

        writer.write(Network.SectType.ENERGY.parseStr,NEWLINE);

        if (pMap.getEcost() != 0.0)
            writer.write("GLOBAL", "PRICE", pMap.getEcost(), NEWLINE);

        if (!pMap.getEpatId().equals(""))
            writer.write("GLOBAL", "PATTERN", pMap.getEpatId(), NEWLINE);

        writer.write("GLOBAL", "EFFIC", pMap.getEpump(), NEWLINE);
        writer.write("DEMAND", "CHARGE", pMap.getDcost(), NEWLINE);

        for (Pump p : net.getPumps()) {
            if (p.getEcost() > 0.0)
                writer.write("PUMP", p.getId(), "PRICE", p.getEcost(), NEWLINE);
            if (p.getEpat() != null)
                writer.write("PUMP", p.getId(), "PATTERN", p.getEpat().getId(), NEWLINE);
            if (p.getEcurve() != null)
                writer.write("PUMP", p.getId(), "EFFIC", p.getId(), p.getEcurve().getId(), NEWLINE);
        }
        writer.newLine();

    }

    public void composeHeader(Network net) throws IOException {
        if (net.getTitleText().size() == 0)
            return;

        writer.write(Network.SectType.TITLE.parseStr,NEWLINE);
        writer.writeHeader(TITLE_SUBTITLE);

        for (String str : net.getTitleText()) {
            writer.write(str);
            writer.newLine();
        }

        writer.newLine();
    }

    private void composeJunctions(Network net) throws IOException, ENException {
        FieldsMap fMap = net.getFieldsMap();
        PropertiesMap pMap = net.getPropertiesMap();

        writer.write(Network.SectType.JUNCTIONS.parseStr,NEWLINE);
        writer.writeHeader(JUNCS_SUBTITLE);

        for (Node node : net.getJunctions()) {
            writer.write(node.getId(), fMap.revertUnit(Type.ELEV, node.getElevation()));

            if( node.getDemand().size()>0){
                Demand d = node.getDemand().get(0);
                writer.write(fMap.revertUnit(FieldsMap.Type.DEMAND, d.getBase()));

                if (!d.getPattern().getId().equals("") && !pMap.getDefPatId().equals(d.getPattern().getId()))
                    writer.write(d.getPattern().getId());
            }

            if (node.getComment().length() != 0)
                writer.write(";" + node.getComment());

            writer.newLine();
        }


        writer.newLine();
    }

    private void composeLabels(Network net) throws IOException {
        writer.write(Network.SectType.LABELS.parseStr,NEWLINE);
        writer.writeHeader(LABELS_SUBTITLE);

        for (Label label : net.getLabels()) {
            writer.write(label.getPosition().getX(), label.getPosition().getY(),label.getText(), NEWLINE);
        }
        writer.newLine();
    }

    private void composeMixing(Network net) throws IOException
    {
        writer.write(Network.SectType.MIXING.parseStr,NEWLINE);
        writer.writeHeader(MIXING_SUBTITLE);

        for (Tank tank : net.getTanks()) {
            if (tank.getArea() == 0.0) continue;
            writer.write(tank.getId(), tank.getMixModel().parseStr,
                    (tank.getV1max() / tank.getVmax()));
            writer.newLine();
        }
        writer.newLine();
    }

    private void composeOptions(Network net) throws IOException, ENException {
        writer.write(Network.SectType.OPTIONS.parseStr,NEWLINE);

        PropertiesMap pMap = net.getPropertiesMap();
        FieldsMap fMap = net.getFieldsMap();

        writer.write("UNITS", pMap.getFlowflag().parseStr, NEWLINE);
        writer.write("PRESSURE", pMap.getPressflag().parseStr, NEWLINE);
        writer.write("HEADLOSS", pMap.getFormflag().parseStr, NEWLINE);

        if (!pMap.getDefPatId().equals(""))
            writer.write("PATTERN", pMap.getDefPatId(), NEWLINE);

        if (pMap.getHydflag() == PropertiesMap.Hydtype.USE)
            writer.write("HYDRAULICS USE", pMap.getHydFname(), NEWLINE);

        if (pMap.getHydflag() == PropertiesMap.Hydtype.SAVE)
            writer.write("HYDRAULICS SAVE", pMap.getHydFname(), NEWLINE);

        if (pMap.getExtraIter() == -1)
            writer.write("UNBALANCED", "STOP", NEWLINE);

        if (pMap.getExtraIter() >= 0)
            writer.write("UNBALANCED", "CONTINUE", pMap.getExtraIter(), NEWLINE);

        if (pMap.getQualflag() == PropertiesMap.QualType.CHEM)
            writer.write("QUALITY", pMap.getChemName(), pMap.getChemUnits(), NEWLINE);

        if (pMap.getQualflag() == PropertiesMap.QualType.TRACE)
            writer.write("QUALITY", "TRACE", pMap.getTraceNode(), NEWLINE);

        if (pMap.getQualflag() == PropertiesMap.QualType.AGE)
            writer.write("QUALITY", "AGE", NEWLINE);

        if (pMap.getQualflag() == PropertiesMap.QualType.NONE)
            writer.write("QUALITY", "NONE", NEWLINE);

        writer.write("DEMAND", "MULTIPLIER", pMap.getDmult(), NEWLINE);

        writer.write("EMITTER", "EXPONENT", 1.0 / pMap.getQexp(), NEWLINE);

        writer.write("VISCOSITY", pMap.getViscos() / Constants.VISCOS, NEWLINE);

        writer.write("DIFFUSIVITY", pMap.getDiffus() / Constants.DIFFUS, NEWLINE);

        writer.write("SPECIFIC", "GRAVITY", pMap.getSpGrav(), NEWLINE);

        writer.write("TRIALS", pMap.getMaxIter(), NEWLINE);

        writer.write("ACCURACY", pMap.getHacc(), NEWLINE);

        writer.write("TOLERANCE", fMap.revertUnit(FieldsMap.Type.QUALITY, pMap.getCtol()), NEWLINE);

        writer.write("CHECKFREQ", pMap.getCheckFreq(), NEWLINE);

        writer.write("MAXCHECK", pMap.getMaxCheck(), NEWLINE);

        writer.write("DAMPLIMIT", pMap.getDampLimit(), NEWLINE);

        writer.newLine();
    }

    private void composePatterns(Network net) throws IOException, ENException {

        List<Pattern> pats = new ArrayList<Pattern>(net.getPatterns());

        writer.write(Network.SectType.PATTERNS.parseStr,NEWLINE);
        writer.writeHeader(PATTERNS_SUBTITLE);

        for (int i = 1; i < pats.size(); i++) {
            Pattern pat = pats.get(i);
            List<Double> F = pat.getFactorsList();
            for (int j = 0; j < pats.get(i).getLength(); j++) {
                if (j % 6 == 0)
                    writer.write(pat.getId());
                writer.write(F.get(j));

                if (j % 6 == 5)
                    writer.newLine();
            }
            writer.newLine();
        }

        writer.newLine();
    }

    private void composePipes(Network net) throws IOException, ENException {
        FieldsMap fMap = net.getFieldsMap();
        PropertiesMap pMap = net.getPropertiesMap();

        List<Link> pipes = new ArrayList<Link>();
        for (Link link : net.getLinks())
            if (link.getType().id <= LinkType.PIPE.id)
                pipes.add(link);

        writer.write(Network.SectType.PIPES.parseStr,NEWLINE);
        writer.writeHeader(PIPES_SUBTITLE);

        for (Link link : pipes) {
            double d = link.getDiameter();
            double kc = link.getRoughness();
            if (pMap.getFormflag() == PropertiesMap.FormType.DW)
                kc = fMap.revertUnit(Type.ELEV, kc * 1000.0);

            double km = link.getKm() * Math.pow(d, 4.0) / 0.02517;

            writer.write(link.getId(),
                    link.getFirst().getId(),
                    link.getSecond().getId(),
                    fMap.revertUnit(Type.LENGTH, link.getLenght()),
                    fMap.revertUnit(Type.DIAM, d));

            //if (pMap.getFormflag() == FormType.DW)
            writer.write(kc, km);

            if (link.getType() == LinkType.CV)
                writer.write("CV");
            else if (link.getStat() == StatType.CLOSED)
                writer.write("CLOSED");
            else if (link.getStat() == StatType.OPEN)
                writer.write("OPEN");

            if (link.getComment().length() != 0)
                writer.write(";" + link.getComment());
            writer.newLine();
        }

        writer.newLine();
    }

    private void composePumps(Network net) throws IOException, ENException {
        FieldsMap fMap = net.getFieldsMap();
        List<Pump> pumps = new ArrayList<Pump>(net.getPumps());

        writer.write(Network.SectType.PUMPS.parseStr,NEWLINE);
        writer.writeHeader(PUMPS_SUBTITLE);

        for (Pump pump : pumps) {
            writer.write(pump.getId(),
                    pump.getFirst().getId(), pump.getSecond().getId());


            // Pump has constant power
            if (pump.getPtype() == Pump.Type.CONST_HP)
                writer.write("POWER", pump.getKm());
                // Pump has a head curve
            else if (pump.getHcurve() != null)
                writer.write("HEAD", pump.getHcurve().getId());
                // Old format used for pump curve
            else {
                writer.write(
                        fMap.revertUnit(Type.HEAD, -pump.getH0()),
                        fMap.revertUnit(Type.HEAD, -pump.getH0() - pump.getFlowCoefficient() * Math.pow(pump.getQ0(), pump.getN())),
                        fMap.revertUnit(Type.FLOW, pump.getQ0()), 0.0,
                        fMap.revertUnit(Type.FLOW, pump.getQmax()
                        ));
                continue;
            }

            if (pump.getUpat() != null)
                writer.write("PATTERN", pump.getUpat().getId());

            if (pump.getRoughness() != 1.0)
                writer.write("SPEED", pump.getRoughness());

            if (pump.getComment().length() != 0)
                writer.write(";" + pump.getComment());

            writer.newLine();
        }

        writer.newLine();
    }

    private void composeQuality(Network net) throws IOException, ENException {
        Collection<Node> nodes = net.getNodes();
        FieldsMap fmap = net.getFieldsMap();

        writer.write(Network.SectType.QUALITY.parseStr,NEWLINE);
        writer.writeHeader(QUALITY_SUBTITLE);

        for (Node node : nodes) {
            if (node.getC0().length == 1) {
                if (node.getC0()[0] == 0.0) continue;
                writer.write(node.getId(), fmap.revertUnit(FieldsMap.Type.QUALITY, node.getC0()[0]));
            }
            writer.newLine();
        }
        writer.newLine();
    }

    @Override
    public void composer(Network net, File f) throws ENException {

        workbook = new XSSFWorkbook();
        writer = new ExcelWriter();

        try {


            writer.newSpreadsheet("Junctions");
            composeJunctions(net);

            writer.newSpreadsheet("Tanks");
            composeReservoirs(net);
            composeTanks(net);

            writer.newSpreadsheet("Pipes");
            composePipes(net);

            writer.newSpreadsheet("Pumps");
            composePumps(net);
            composeEnergy(net);

            writer.newSpreadsheet("Valves");
            composeValves(net);

            writer.newSpreadsheet("Demands");
            composeDemands(net);

            writer.newSpreadsheet("Patterns");
            composePatterns(net);
            writer.newSpreadsheet("Curves");
            composeCurves(net);

            writer.newSpreadsheet("Script");
            composeControls(net);
            composeRules(net);

            writer.newSpreadsheet("Quality");
            composeQuality(net);
            composeSource(net);
            composeMixing(net);
            composeReaction(net);


            writer.newSpreadsheet("Config");
            composeHeader(net);
            composeTimes(net);
            composeOptions(net);
            composeReport(net);
            composeEmitters(net);
            composeStatus(net);

            writer.newSpreadsheet("GIS");
            composeLabels(net);
            composeCoordinates(net);
            composeVertices(net);



            workbook.write(new FileOutputStream(f));
        } catch (IOException e) {

        }
    }


    private void composeReaction(Network net) throws IOException, ENException {
        PropertiesMap pMap = net.getPropertiesMap();

        writer.write(Network.SectType.REACTIONS.parseStr,NEWLINE);
        writer.writeHeader(REACTIONS_SUBTITLE);

        writer.write("ORDER", "BULK", pMap.getBulkOrder(), NEWLINE);
        writer.write("ORDER", "WALL", pMap.getWallOrder(), NEWLINE);
        writer.write("ORDER", "TANK", pMap.getTankOrder(), NEWLINE);
        writer.write("GLOBAL", "BULK", pMap.getKbulk() * Constants.SECperDAY, NEWLINE);
        writer.write("GLOBAL", "WALL", pMap.getKwall() * Constants.SECperDAY, NEWLINE);
        //if (pMap.getClimit() > 0.0)
        writer.write("LIMITING", "POTENTIAL", pMap.getClimit(), NEWLINE);

        //if (pMap.getRfactor() != Constants.MISSING && pMap.getRfactor() != 0.0)
        writer.write("ROUGHNESS", "CORRELATION", pMap.getRfactor(), NEWLINE);


        for (Link link : net.getLinks()) {
            if (link.getType().id > LinkType.PIPE.id)
                continue;

            if (link.getKb() != pMap.getKbulk())
                writer.write("BULK", link.getId(), link.getKb() * Constants.SECperDAY, NEWLINE);
            if (link.getKw() != pMap.getKwall())
                writer.write("WALL", link.getId(), link.getKw() * Constants.SECperDAY, NEWLINE);
        }

        for (Tank tank : net.getTanks()) {
            if (tank.getArea() == 0.0) continue;
            if (tank.getKb() != pMap.getKbulk())
                writer.write("TANK", tank.getId(), tank.getKb() * Constants.SECperDAY, NEWLINE);
        }
        writer.newLine();
    }

    private void composeReport(Network net) throws IOException, ENException {
        writer.write(Network.SectType.REPORT.parseStr,NEWLINE);

        PropertiesMap pMap = net.getPropertiesMap();
        FieldsMap fMap = net.getFieldsMap();
        writer.write("PAGESIZE", pMap.getPageSize(), NEWLINE);
        writer.write("STATUS", pMap.getStatflag().parseStr, NEWLINE);
        writer.write("SUMMARY", pMap.getSummaryflag() ? Keywords.w_YES : Keywords.w_NO, NEWLINE);
        writer.write("ENERGY", pMap.getEnergyflag() ? Keywords.w_YES : Keywords.w_NO, NEWLINE);

        switch (pMap.getNodeflag()) {
            case FALSE:
                writer.write("NODES", "NONE", NEWLINE);
                break;
            case TRUE:
                writer.write("NODES", "ALL", NEWLINE);
                break;
            case SOME: {
                int j = 0;
                for (Node node : net.getNodes()) {
                    if (node.isRptFlag()) {
                        if (j % 5 == 0) writer.write("NODES", NEWLINE);
                        writer.write(node.getId());
                        j++;
                    }
                }
                break;
            }
        }

        switch (pMap.getLinkflag()) {
            case FALSE:
                writer.write("LINKS", "NONE", NEWLINE);
                break;
            case TRUE:
                writer.write("LINKS", "ALL", NEWLINE);
                break;
            case SOME: {
                int j = 0;
                for (Link link : net.getLinks()) {
                    if (link.isRptFlag()) {
                        if (j % 5 == 0) writer.write("LINKS", NEWLINE);
                        writer.write(link.getId());
                        j++;
                    }
                }
                break;
            }
        }

        for (int i = 0; i < Type.FRICTION.id; i++) {
            Field f = fMap.getField(FieldsMap.Type.values()[i]);
            if (f.isEnabled()) {
                writer.write(f.getName(), "PRECISION", f.getPrecision(), NEWLINE);
                if (f.getRptLim(Field.RangeType.LOW) < Constants.BIG)
                    writer.write(f.getName(), "BELOW", f.getRptLim(Field.RangeType.LOW), NEWLINE);
                if (f.getRptLim(Field.RangeType.HI) > -Constants.BIG)
                    writer.write(f.getName(), "ABOVE", f.getRptLim(Field.RangeType.HI), NEWLINE);
            } else
                writer.write(f.getName(), "NO", NEWLINE);
        }

        writer.newLine();
    }

    private void composeReservoirs(Network net) throws IOException, ENException {
        FieldsMap fMap = net.getFieldsMap();
        if (net.getTanks().size() == 0)
            return;

        List<Tank> reservoirs = new ArrayList<Tank>();
        for (Tank tank : net.getTanks())
            if (tank.getArea() == 0)
                reservoirs.add(tank);

        writer.write(Network.SectType.RESERVOIRS.parseStr,NEWLINE);
        writer.writeHeader(RESERVOIRS_SUBTITLE);

        for (Tank r : reservoirs) {
            writer.write(r.getId(), fMap.revertUnit(Type.ELEV, r.getElevation()));

            if (r.getPattern()!=null)
                writer.write(r.getPattern().getId());

            if (r.getComment().length() != 0)
                writer.write(";" + r.getComment());
            writer.newLine();
        }

        writer.newLine();
    }

    private void composeRules(Network net) throws IOException, ENException {
        writer.write(Network.SectType.RULES.parseStr,NEWLINE);
        for (Rule r : net.getRules()) {
            writer.write("RULE ",r.getLabel(), NEWLINE);
            for (String s : r.getCode().split("\n"))
                writer.write(s, NEWLINE);
            writer.newLine();
        }
        writer.newLine();
    }

    private void composeSource(Network net) throws IOException
    {
        Collection<Node> nodes = net.getNodes();

        writer.write(Network.SectType.SOURCES.parseStr,NEWLINE);
        writer.writeHeader(SOURCE_SUBTITLE);

        for (Node node : nodes) {
            Source source = node.getSource();
            if (source == null)
                continue;
            writer.write(node.getId(),
                    source.getType().parseStr,
                    source.getC0());
            if (source.getPattern() != null)
                writer.write(source.getPattern().getId());
            writer.newLine();
        }
        writer.newLine();
    }

    private void composeStatus(Network net) throws IOException, ENException {

        writer.write(Network.SectType.STATUS.parseStr,NEWLINE);
        writer.writeHeader(STATUS_SUBTITLE);

        for (Link link : net.getLinks()) {
            if (link.getType().id <= LinkType.PUMP.id) {
                if (link.getStat() == StatType.CLOSED)
                    writer.write(link.getId(), StatType.CLOSED.parseStr);
                else if (link.getType() == LinkType.PUMP) {  // Write pump speed here for pumps with old-style pump curve input
                    Pump pump = (Pump) link;
                    if (pump.getHcurve() == null &&
                            pump.getPtype() != Pump.Type.CONST_HP &&
                            pump.getRoughness() != 1.0)
                        writer.write(link.getId(), link.getRoughness());
                }
            } else if (link.getRoughness() == Constants.MISSING)  // Write fixed-status PRVs & PSVs (setting = MISSING)
            {
                if (link.getStat() == StatType.OPEN)
                    writer.write(link.getId(), StatType.OPEN.parseStr);

                if (link.getStat() == StatType.CLOSED)
                    writer.write(link.getId(), StatType.CLOSED.parseStr);

            }

            writer.newLine();
        }

        writer.newLine();
    }

    private void composeTanks(Network net) throws IOException, ENException {
        FieldsMap fMap = net.getFieldsMap();

        List<Tank> tanks = new ArrayList<Tank>();
        for (Tank tank : net.getTanks())
            if (tank.getArea() != 0)
                tanks.add(tank);

        writer.write(Network.SectType.TANKS.parseStr,NEWLINE);
        writer.writeHeader(TANK_SUBTITLE);

        for (Tank tank : tanks) {
            double Vmin = tank.getVmin();
            if(Math.abs(Vmin/tank.getArea() - (tank.getHmin()-tank.getElevation()))<0.1)
                Vmin = 0;

            writer.write(tank.getId(),
                    fMap.revertUnit(Type.ELEV, tank.getElevation()),
                    fMap.revertUnit(FieldsMap.Type.ELEV, tank.getH0() - tank.getElevation()),
                    fMap.revertUnit(Type.ELEV, tank.getHmin() - tank.getElevation()),
                    fMap.revertUnit(Type.ELEV, tank.getHmax() - tank.getElevation()),
                    fMap.revertUnit(Type.ELEV, 2 * Math.sqrt(tank.getArea() / Constants.PI)),
                    fMap.revertUnit(Type.VOLUME,Vmin));

            if (tank.getVcurve() != null)
                writer.write(tank.getVcurve().getId());

            if (tank.getComment().length() != 0)
                writer.write(";" + tank.getComment());
            writer.newLine();
        }

        writer.newLine();
    }


    private void composeTimes(Network net) throws IOException, ENException {
        writer.write(Network.SectType.TIMES.parseStr,NEWLINE);
        PropertiesMap pMap = net.getPropertiesMap();
        //writer.write("DURATION", Utilities.getClockTime(pMap.getDuration()), NEWLINE);
        //writer.write("HYDRAULIC", "TIMESTEP", Utilities.getClockTime(pMap.getHstep()), NEWLINE);
        //writer.write("QUALITY", "TIMESTEP", Utilities.getClockTime(pMap.getQstep()), NEWLINE);
        //writer.write("REPORT", "TIMESTEP", Utilities.getClockTime(pMap.getRstep()), NEWLINE);
        //writer.write("REPORT", "START", Utilities.getClockTime(pMap.getRstart()), NEWLINE);
        //writer.write("PATTERN", "TIMESTEP", Utilities.getClockTime(pMap.getPstep()), NEWLINE);
        //writer.write("PATTERN", "START", Utilities.getClockTime(pMap.getPstart()), NEWLINE);
        //writer.write("RULE", "TIMESTEP", Utilities.getClockTime(pMap.getRulestep()), NEWLINE);
        //writer.write("START", "CLOCKTIME", Utilities.getClockTime(pMap.getTstart()), NEWLINE);
        writer.write("DURATION", new Date(pMap.getDuration()), NEWLINE);
        writer.write("HYDRAULIC", "TIMESTEP", new Date(pMap.getHstep()), NEWLINE);
        writer.write("QUALITY", "TIMESTEP", new Date(pMap.getQstep()), NEWLINE);
        writer.write("REPORT", "TIMESTEP", new Date(pMap.getRstep()), NEWLINE);
        writer.write("REPORT", "START", new Date(pMap.getRstart()), NEWLINE);
        writer.write("PATTERN", "TIMESTEP", new Date(pMap.getPstep()), NEWLINE);
        writer.write("PATTERN", "START", new Date(pMap.getPstart()), NEWLINE);
        writer.write("RULE", "TIMESTEP", new Date(pMap.getRulestep()), NEWLINE);
        writer.write("START", "CLOCKTIME", new Date(pMap.getTstart()), NEWLINE);
        writer.write("STATISTIC", pMap.getTstatflag().parseStr, NEWLINE);
        writer.newLine();
    }

    private void composeValves(Network net) throws IOException, ENException {
        FieldsMap fMap = net.getFieldsMap();
        List<Valve> valves = new ArrayList<Valve>(net.getValves());

        writer.write(Network.SectType.VALVES.parseStr,NEWLINE);
        writer.writeHeader(VALVES_SUBTITLE);

        for (Valve valve : valves) {
            double d = valve.getDiameter();
            double kc = valve.getRoughness();
            if (kc == Constants.MISSING)
                kc = 0.0;

            switch (valve.getType()) {
                case FCV:
                    kc = fMap.revertUnit(Type.FLOW, kc);
                    break;
                case PRV:
                case PSV:
                case PBV:
                    kc = fMap.revertUnit(Type.PRESSURE, kc);
                    break;
            }

            double km = valve.getKm() * Math.pow(d, 4) / 0.02517;

            writer.write(valve.getId(),
                    valve.getFirst().getId(),
                    valve.getSecond().getId(),
                    fMap.revertUnit(Type.DIAM, d),
                    valve.getType().parseStr);

            if (valve.getType() == LinkType.GPV && valve.getCurve() != null)
                writer.write(valve.getCurve().getId(), km);
            else
                writer.write(kc, km);

            if (valve.getComment().length() != 0)
                writer.write(";" + valve.getComment());
            writer.newLine();
        }
        writer.newLine();
    }

    private void composeVertices(Network net) throws IOException {
        writer.write(Network.SectType.VERTICES.parseStr,NEWLINE);
        writer.writeHeader(VERTICES_SUBTITLE);

        for (Link link : net.getLinks()) {
            for (Point p : link.getVertices()) {
                writer.write(link.getId(), p.getX(), p.getY(), NEWLINE);
            }
        }

        writer.newLine();
    }


}
