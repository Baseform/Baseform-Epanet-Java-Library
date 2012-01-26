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


import org.addition.epanet.util.ENException;
import org.addition.epanet.network.Network;
import org.addition.epanet.util.Utilities;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Excel XLSX file parser.
 */
public class ExcelParser extends InpParser {


    public ExcelParser(Logger logger) {
        super(logger);
        log = logger;
    }

    private String convertCell(XSSFCell cell) throws ENException {
        if (cell.getCellType() == XSSFCell.CELL_TYPE_NUMERIC) {
            if (timeStyles.contains(cell.getCellStyle()))
                return Utilities.getClockTime(Math.round(cell.getNumericCellValue() * 86400));

            return Double.toString(cell.getNumericCellValue());
        } else if (cell.getCellType() == XSSFCell.CELL_TYPE_STRING)
            return cell.getStringCellValue();
        else
            throw new ENException(201);
    }

    List<XSSFCellStyle> timeStyles = new ArrayList<XSSFCellStyle>();

    private void findTimeStyle(XSSFWorkbook workbook) {
        final List<Short> validTimeFormats = Arrays.asList(new Short[]{0x12, // "h:mm AM/PM"
                0x13, // "h:mm:ss AM/PM"
                0x14, // "h:mm"
                0x15, // "h:mm:ss"
                0x16, // "m/d/yy h:mm"
                0x2d, // "mm:ss"
                0x2e, // "[h]:mm:ss"
                0x2f, // "mm:ss.0"
        });

        StylesTable styleTable = workbook.getStylesSource();
        int stylesCount = styleTable.getNumCellStyles();
        for (int i = 0; i < stylesCount; i++) {
            XSSFCellStyle style = styleTable.getStyleAt(i);

            //if(org.apache.poi.ss.usermodel.DateUtil.isInternalDateFormat(style.getDataFormat()))
            if (validTimeFormats.contains(style.getDataFormat()))
                timeStyles.add(style);
            else if (style.getDataFormatString().toLowerCase().contains("[h]:mm") ||
                        style.getDataFormatString().toLowerCase().contains("[hh]:mm"))
                timeStyles.add(style);
        }
    }

    @Override
    public Network parse(Network net, File f) throws ENException {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(f);
            XSSFWorkbook workbook = new XSSFWorkbook(stream);

            findTimeStyle(workbook);

            Pattern tagPattern = Pattern.compile("\\[.*\\]");
            int errSum = 0;

            List<XSSFSheet> sheetPC = new ArrayList<XSSFSheet>();
            List<XSSFSheet> sheetOthers = new ArrayList<XSSFSheet>();
            List<XSSFSheet> sheetNodes = new ArrayList<XSSFSheet>();
            List<XSSFSheet> sheetTanks = new ArrayList<XSSFSheet>();

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                XSSFSheet sh = workbook.getSheetAt(i);
                if (sh.getSheetName().equalsIgnoreCase("Patterns") || sh.getSheetName().equalsIgnoreCase("Curves")) {
                    sheetPC.add(sh);
                } else if (sh.getSheetName().equals("Junctions"))
                    sheetNodes.add(sh);
                else if (sh.getSheetName().equals("Tanks") || sh.getSheetName().equals("Reservoirs"))
                    sheetTanks.add(sh);
                else
                    sheetOthers.add(sh);

            }
            errSum = parseWorksheet(net, sheetPC, tagPattern, errSum);   // parse the patterns and curves
            errSum = parseWorksheet(net, sheetNodes, tagPattern, errSum);   // parse the nodes
            errSum = parseWorksheet(net, sheetTanks, tagPattern, errSum);   // parse the nodes
            errSum = parseWorksheet(net, sheetOthers, tagPattern, errSum);  // parse other elements

            if (errSum != 0)
                throw new ENException(200);


            stream.close();

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

    private int parseWorksheet(Network net, List<XSSFSheet> sheets, Pattern tagPattern, int errSum) throws ENException {
        for (XSSFSheet sheet : sheets) {

            boolean lastRowNull = true;
            boolean lastRowHeader = false;
            Network.SectType lastType = null;

            for (int rowCount = 0, tRowId = 0; rowCount < sheet.getPhysicalNumberOfRows(); tRowId++) {
                XSSFRow row = sheet.getRow(tRowId);

                if (row != null) {
                    List<String> tokens = new ArrayList<String>();

                    String comments = "";
                    boolean allAreBold = true;

                    for (int cellCount = 0, tCellId = 0; cellCount < row.getPhysicalNumberOfCells(); tCellId++) {
                        XSSFCell cell = row.getCell(tCellId);
                        if (cell != null) {
                            String value = convertCell(cell);
                            if (value.startsWith(";")) {
                                comments += value;
                            } else
                                tokens.add(value);

                            allAreBold = allAreBold & cell.getCellStyle().getFont().getBold(); // TODO remover

                            cellCount++;
                        }
                    }

                    if (tokens.size() > 0) {
                        if (lastRowNull && tagPattern.matcher(tokens.get(0)).matches()) {
                            lastType = Network.SectType.parse(tokens.get(0));
                            lastRowHeader = true;
                        } else {
                            String[] tokArray = tokens.toArray(new String[tokens.size()]);

                            if (lastRowHeader && allAreBold) {
                                //System.out.println("Formating Header : " + tokens.toArray(new String[tokens.size()]));
                            } else {
                                try {
                                    parseSect(net, lastType, comments, tokArray);
                                } catch (ENException e) {
                                    String line = "";
                                    for (String tk : tokArray)
                                        line += tk + " ";

                                    logException(lastType, e, line, tokArray);
                                    errSum++;
                                }
                            }
                        }
                    }

                    lastRowNull = false;
                    rowCount++;
                }

                if (row == null || row != null && row.getPhysicalNumberOfCells() == 0) {
                    lastRowNull = true;
                    continue;
                }

            }
        }
        return errSum;
    }

    private void parseSect(Network net, Network.SectType type, String comments, String[] tokens) throws ENException {
        switch (type) {

            case TITLE:
                break;
            case JUNCTIONS:
                parseJunction(net, tokens, comments);
                break;
            case RESERVOIRS:
            case TANKS:
                parseTank(net, tokens, comments);
                break;
            case PIPES:
                parsePipe(net, tokens, comments);
                break;
            case PUMPS:
                parsePump(net, tokens, comments);
                break;
            case VALVES:
                parseValve(net, tokens, comments);
                break;
            case CONTROLS:
                parseControl(net, tokens);
                break;
            case RULES: {
                String line = "";
                for (String t : tokens)
                    line += t;
                parseRule(net, tokens, line);
                break;
            }
            case DEMANDS:
                parseDemand(net, tokens);
                break;
            case SOURCES:
                parseSource(net, tokens);
                break;
            case EMITTERS:
                parseEmitter(net, tokens);
                break;
            case PATTERNS:
                parsePattern(net, tokens);
                break;
            case CURVES:
                parseCurve(net, tokens);
                break;
            case QUALITY:
                parseQuality(net, tokens);
                break;
            case STATUS:
                parseStatus(net, tokens);
                break;
            case ROUGHNESS:
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
            case BACKDROP:
                break;
            case TAGS:
                break;
            case END:
                break;
        }
    }

}
