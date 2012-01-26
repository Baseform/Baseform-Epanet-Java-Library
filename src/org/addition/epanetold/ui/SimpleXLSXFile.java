package org.addition.epanetold.ui;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;




public class SimpleXLSXFile {

    private static String ColumnName(int index)
    {
        index -= 1; //adjust so it matches 0-indexed array rather than 1-indexed column

        int quotient = index / 26;
        if (quotient > 0)
            return ColumnName(quotient) + chars[index % 26];
        else
            return Character.toString (chars[index % 26]) ;
    }
    static private char[] chars = new char[] {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};


    class Spreadsheet {

        File tmpFile;
        private BufferedWriter tmpWriter;
        private String name;
        //private List<String> sharedStr;
        private int rowNo = 1;
        private int wordCount = 0;

        public int getWordCount(){
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

        public void addRow(Object... row) throws IOException {
            tmpWriter.write(String.format("\t<row r=\"%d\" spans=\"1:%d\">", rowNo, row.length));
            tmpWriter.newLine();

            int columnCount = 0;

            for (Object o : row) {
                if (o instanceof Number)
                    //tmpWriter.write(String.format("\t\t<c><v>%s</v></c>", o));
                    tmpWriter.write(String.format("<c r=\"%s%d\" t=\"n\">\n<v>%s</v>\n</c>",ColumnName(columnCount+1),rowNo,o));
                else {
                    if (o == null) o = "";
                    int idx = sharedStrings.indexOf(o.toString());
                    if (idx < 0) {
                        sharedStrings.add(o.toString());
                        idx = sharedStrings.indexOf(o.toString());
                    }
                    wordCount++;
                    tmpWriter.write(String.format("\t\t<c r=\"%s%d\" t=\"s\"><v>%d</v></c>",ColumnName(columnCount+1), rowNo,idx));

                }

                columnCount++;
            }
            tmpWriter.write("\t</row>");
            tmpWriter.newLine();
            rowNo++;
        }


        public void addRow(List row) throws IOException {
            addRow(row.toArray());
        }

        public String getName() {
            return name;
        }
        public void finish(){
            try{
                if(tmpWriter!=null) tmpWriter.close();
                tmpWriter=null;
                tmpFile.delete();
            }catch (Exception ignored){}
        }

    }

    private ArrayList<String> sharedStrings = new ArrayList<String>();

    public SimpleXLSXFile() {
        sheets = new ArrayList<Spreadsheet>();
    }




    private void createWorksheet(Spreadsheet sheet, int pos) throws IOException {
        ZipEntry entry = new ZipEntry("xl/worksheets/sheet" + pos + ".xml");
        zos.putNextEntry(entry);
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        writer.newLine();
        writer.write("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">");
        writer.newLine();
        writer.write("<sheetData>");
        writer.newLine();

        sheet.getTmpWriter().flush();
        BufferedReader b = new BufferedReader(new InputStreamReader(new FileInputStream(sheet.getTmpFile()), "UTF-8"));
        String l;
        while ((l = b.readLine()) != null) {
            writer.write(l);
            writer.newLine();
        }
        b.close();

        writer.write("</sheetData>");
        writer.newLine();
        writer.write("</worksheet>");
        writer.newLine();
        writer.flush();
        zos.closeEntry();
    }


    private void createSharedStringsXML() throws IOException {
        ZipEntry entry = new ZipEntry("xl/sharedStrings.xml");
        zos.putNextEntry(entry);

        int count = 0;
        for(Spreadsheet s : sheets){
            count+= s.getWordCount();
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

        zos.setLevel(9);
        writer = new BufferedWriter(new OutputStreamWriter(zos,"UTF-8"));
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

    public static void main(String[] args) {
        try {
            SimpleXLSXFile xlsxFile = new SimpleXLSXFile();

            Spreadsheet sheet1 = xlsxFile.newSpreadsheet("a1");
            sheet1.addRow("1.1", "a12");
            sheet1.addRow("a21", "a22");
            sheet1.addRow("testing", "abc");
            sheet1.addRow("&", "<", ">");

            Spreadsheet sheet2 = xlsxFile.newSpreadsheet("a2");
            sheet2.addRow("b11", "b12");
            sheet2.addRow("b21", "b22");
            sheet2.addRow("abc", "testing");


            xlsxFile.save(new FileOutputStream("testXLSX.xlsx"));
            xlsxFile.finish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void finish() throws IOException {
        zos.finish();
        for (Spreadsheet sheet : sheets) {
            sheet.finish();
        }
        zos.close();
    }


}