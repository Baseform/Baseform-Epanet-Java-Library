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

package org.addition.epanet.util;


import org.apache.poi.util.IOUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class XLSXWriter {

    private boolean transposedMode = false;

    public void setTransposedMode(boolean transposedMode) {
        this.transposedMode = transposedMode;
    }

    public boolean getTransposedMode() {
        return transposedMode;
    }

    private static String ColumnName(int index) {
        index -= 1;

        int quotient = index / 26;
        if (quotient > 0)
            return ColumnName(quotient) + chars[index % 26];
        else
            return Character.toString(chars[index % 26]);
    }

    static private char[] chars = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};


    public class Spreadsheet {

        private static final int CELL_RECORD_WIDTH = 75;
        private File tmpFile;
        private BufferedWriter tmpWriter;
        private String name;
        private int rowNo = 1;
        private int wordCount = 0;

        private int maxColumns;
        private RandomAccessFile rndWriter;


        public void prepareTranspose(int rows, int columns) throws IOException {
            tmpWriter.close();

            char[] cleanString = new char[CELL_RECORD_WIDTH];
            Arrays.fill(cleanString, ' ');
            maxColumns = columns;
            tmpWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpFile), "UTF-8"));

            for (int i = 0; i < rows; i++) {

                char[] title = String.format("<row r=\"%d\" spans=\"1:%d\">", i + 1, columns).toCharArray();
                Arrays.fill(cleanString, ' ');
                System.arraycopy(title, 0, cleanString, 0, title.length);
                tmpWriter.write(cleanString, 0, CELL_RECORD_WIDTH);

                Arrays.fill(cleanString, ' ');
                for (int j = 0; j < columns; j++) {
                    tmpWriter.write(cleanString, 0, CELL_RECORD_WIDTH);
                }

                char[] end = "</row>".toCharArray();
                Arrays.fill(cleanString, ' ');
                System.arraycopy(end, 0, cleanString, 0, end.length);
                cleanString[49] = '\n';
                tmpWriter.write(cleanString, 0, CELL_RECORD_WIDTH);
            }
            tmpWriter.close();
            tmpWriter = null;
            rndWriter = new RandomAccessFile(tmpFile, "rw");
        }

        public int getWordCount() {
            return wordCount;
        }

        public File getTmpFile() {
            return tmpFile;
        }

        public BufferedWriter getTmpWriter() {
            return tmpWriter;
        }

        Spreadsheet(String name) throws IOException {
            this.name = name;
            tmpFile = File.createTempFile(Spreadsheet.class.getSimpleName(), name);
            tmpWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmpFile), "UTF-8"));
        }

        private void addRowNormal(Object... row) throws IOException {
            tmpWriter.write(String.format("\t<row r=\"%d\" spans=\"1:%d\">", rowNo, row.length));
            tmpWriter.newLine();

            int columnCount = 0;

            for (Object o : row) {
                if (o instanceof Number) {
                    tmpWriter.write("<c r=\"" + ColumnName(columnCount + 1) + rowNo + "\" t=\"n\">\n<v>" + o + "</v>\n</c>");
                } else {
                    if (o == null) o = "";
                    int idx = sharedStrings.indexOf(o.toString());
                    if (idx < 0) {
                        sharedStrings.add(o.toString());
                        idx = sharedStrings.indexOf(o.toString());
                    }
                    wordCount++;
                    tmpWriter.write(String.format("\t\t<c r=\"%s%d\" t=\"s\"><v>%d</v></c>", ColumnName(columnCount + 1), rowNo, idx));

                }

                columnCount++;
            }
            tmpWriter.write("\t</row>");
            tmpWriter.newLine();
            rowNo++;
        }

        public void addRow(Object... row) throws IOException {
            if (transposedMode)
                addRowTranspose2(row);
            else
                addRowNormal(row);
        }


        private void addRowTranspose2(Object... row) throws IOException {
            int newRowId = 1;
            int newColumnId = rowNo;
            long pos1 = CELL_RECORD_WIDTH + CELL_RECORD_WIDTH * (rowNo - 1);


            for (Object o : row) {
                rndWriter.seek(pos1 + CELL_RECORD_WIDTH * (2 + maxColumns) * (newRowId - 1));//50*(maxColumns+2)*(newRowId-1));
                if (o instanceof Number) {
                    byte[] buff = ("<c r=\"" + ColumnName(newColumnId) + newRowId + "\" t=\"n\"><v>" + o + "</v> </c>").getBytes("UTF-8");
                    rndWriter.write(buff);

                } else {
                    if (o == null) o = "";
                    int idx = sharedStrings.indexOf(o.toString());
                    if (idx < 0) {
                        sharedStrings.add(o.toString());
                        idx = sharedStrings.indexOf(o.toString());
                    }
                    wordCount++;
                    byte[] buff = String.format("<c r=\"%s%d\" t=\"s\"><v>%d</v></c>", ColumnName(newColumnId), newRowId, idx).getBytes("UTF-8");
                    rndWriter.write(buff);


                }

                newRowId++;
            }
            rowNo++;
        }

        public void addRow(List row) throws IOException {
            addRow(row.toArray());
        }

        public String getName() {
            return name;
        }

        public void finish() {
            try {
                if (tmpWriter != null) tmpWriter.close();
                if (rndWriter != null) rndWriter.close();
                tmpWriter = null;
                tmpFile.delete();
            } catch (Exception ignored) {
            }
        }

        public void close() {
            try {
                if (tmpWriter != null) {
                    tmpWriter.flush();
                    tmpWriter.close();
                }
                if (rndWriter != null) {
                    rndWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private ArrayList<String> sharedStrings = new ArrayList<String>();

    public XLSXWriter() {
        sheets = new ArrayList<Spreadsheet>();
    }


    private void createWorksheet(Spreadsheet sheet, int pos) throws IOException {
        sheet.close();

        ZipEntry entry = new ZipEntry("xl/worksheets/sheet" + pos + ".xml");
        zos.putNextEntry(entry);

        zos.write(("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n" +
                "<sheetData>\n").getBytes("UTF-8"));


        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sheet.getTmpFile()));
        IOUtils.copy(bis, zos);
        bis.close();

        zos.write(("</sheetData>\n" +
                "</worksheet>\n").getBytes("UTF-8"));

        zos.closeEntry();
    }


    private void createSharedStringsXML() throws IOException {
        ZipEntry entry = new ZipEntry("xl/sharedStrings.xml");
        zos.putNextEntry(entry);

        int count = 0;
        for (Spreadsheet s : sheets) {
            count += s.getWordCount();
        }

        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        writer.newLine();
        writer.write("<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"" + count + "\" uniqueCount=\"" + sharedStrings.size() + "\">");
        writer.newLine();

        for (int i = 0; i < sharedStrings.size(); i++) {
            String s = sharedStrings.get(i);
            //replace all & with &amp;
            s = s.replace("&", "&amp;");
            //replace all < with &lt;
            s = s.replace("<", "&lt;");
            //replace all > with &gt;
            s = s.replace(">", "&gt;");
            writer.write("\t<si><t>" + s + "</t></si>");
            writer.newLine();
        }
        writer.write("</sst>");
        writer.newLine();
        writer.flush();
        zos.closeEntry();
    }


    private void createWorkbookXML() throws IOException {
        ZipEntry entry = new ZipEntry("xl/workbook.xml");
        zos.putNextEntry(entry);
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        writer.newLine();
        writer.write("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");
        writer.newLine();
        writer.write("\t<sheets>");
        writer.newLine();
        int id = 1;
        for (Spreadsheet sheet : sheets) {
            String data = "<sheet name=\"" + sheet.getName() + "\" sheetId=\"" + id + "\" r:id=\"rId" + id + "\"/>";
            writer.write("\t\t" + data);
            writer.newLine();
            id++;
        }
        writer.write("\t</sheets>");
        writer.newLine();
        writer.write("</workbook>");
        writer.newLine();
        writer.flush();
        zos.closeEntry();
    }


    private void createXL_rel() throws IOException {
        ZipEntry entry = new ZipEntry("xl/_rels/workbook.xml.rels");
        zos.putNextEntry(entry);
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        writer.newLine();
        writer.write("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        writer.newLine();
        int id = 1;
        for (Spreadsheet sheet : sheets) {
            String data = "<Relationship Id=\"rId" + id + "\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet" + id + ".xml\"/>";
            writer.write("\t" + data);
            writer.newLine();
            id++;
        }
        {
            String data = "<Relationship Id=\"rId" + id + "\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings\" Target=\"sharedStrings.xml\"/>";
            writer.write("\t" + data);
            writer.newLine();
        }
        writer.write("</Relationships>");
        writer.newLine();
        writer.flush();
        zos.closeEntry();
    }

    private void creatContentType() throws IOException {
        ZipEntry entry = new ZipEntry("[Content_Types].xml");
        zos.putNextEntry(entry);

        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        writer.newLine();
        writer.write("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">");
        writer.newLine();
        writer.write("\t<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>");
        writer.newLine();
        writer.write("\t<Default Extension=\"xml\" ContentType=\"application/xml\"/>");
        writer.newLine();
        writer.write("\t<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
        writer.newLine();
        int id = 1;
        for (Spreadsheet sheet : sheets) {
            String data = "<Override PartName=\"/xl/worksheets/sheet" + id + ".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>";
            writer.write('\t' + data);
            writer.newLine();
            id++;
        }
        writer.write("\t<Override PartName=\"/xl/sharedStrings.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml\"/>");
        writer.newLine();
        writer.write("</Types>");
        writer.newLine();
        writer.flush();
        zos.closeEntry();
    }

    private void createRels() throws IOException {
        ZipEntry entry = new ZipEntry("_rels/.rels");
        zos.putNextEntry(entry);
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        writer.newLine();
        writer.write("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        writer.newLine();
        writer.write("\t<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>");
        writer.newLine();
        writer.write("</Relationships>");
        writer.newLine();
        writer.flush();
        zos.closeEntry();
    }

    public void save(OutputStream outputStream) throws IOException {
        zos = new ZipOutputStream(outputStream);
        zos.setLevel(1);
        writer = new BufferedWriter(new OutputStreamWriter(zos, "UTF-8"));
        createRels();

        creatContentType();

        createXL_rel();
        createWorkbookXML();
        createSharedStringsXML();
        for (int i = 0; i < sheets.size(); i++) {
            Spreadsheet sheet = sheets.get(i);
            createWorksheet(sheet, i + 1);
        }
    }


    private List<Spreadsheet> sheets;
    private ZipOutputStream zos;
    private BufferedWriter writer;

    public Spreadsheet newSpreadsheet(String name) throws IOException {
        Spreadsheet spreadsheet = new Spreadsheet(name);
        sheets.add(spreadsheet);
        return spreadsheet;
    }

    public void finish() throws IOException {
        zos.finish();
        for (Spreadsheet sheet : sheets) {
            sheet.finish();
        }
        zos.close();
    }


}
