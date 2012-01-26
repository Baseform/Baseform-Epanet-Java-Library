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

package org.addition.epanet.msx;

import org.addition.epanet.msx.EnumTypes.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Report {




    private static final int SERIES_TABLE  =0;
    private static final int STATS_TABLE   =1;


    private Network     MSX;
    private ENToolkit2  epanet;
    private Output      out;
    private InpReader   inpReader;

    public void loadDependencies(EpanetMSX epa){
        MSX=epa.getNetwork();
        epanet=epa.getENToolkit() ;
        out=epa.getOutput();
        inpReader = epa.getReader();
    }

    private static String [] Logo =
            {   "******************************************************************",
                    "*                      E P A N E T  -  M S X                     *",
                    "*                   Multi-Species Water Quality                  *",
                    "*                   Analysis for Pipe  Networks                  *",
                    "*                           Version 1.0                          *",
                    "******************************************************************"};

    private final static String PageHdr = "  Page %d                                    ";
    private static String [] StatsHdrs = {"", "Average Values  ", "Minimum Values  ",
            "Maximum Values  ", "Range of Values "};
    private static String Line;
    private static long LineNum;
    private static long PageNum;
    private static int  [] RptdSpecies;

    private class  TableHeader{
        public String Line1;
        public String Line2;
        public String Line3;
        public String Line4;
        public String Line5;
    }
    TableHeader TableHdr;


    Report(){
        TableHdr = new TableHeader();
    }
    private String IDname;


    public int  MSXrpt_write(File outputFile)
    {
        RandomAccessFile raf;
        int  magic = 0;
        int  j;

        // check that results are available
        if ( MSX.Nperiods < 1 )
            return 0;

        try{
            long flen =  outputFile.length();
            raf = new RandomAccessFile(outputFile,"r");
            raf.skipBytes((int)flen-Integer.SIZE/8);
            magic = raf.readInt();
        }
        catch (IOException ex){
            return  ErrorCodeType.ERR_IO_OUT_FILE.id;
        }

        if ( magic != Constants.MAGICNUMBER )
            return ErrorCodeType.ERR_IO_OUT_FILE.id;

        // write program logo & project title
        PageNum = 1;
        LineNum = 1;

        newPage();
        for (j=0; j<=5; j++)
            writeLine(Logo[j]);

        writeLine("");
        writeLine(MSX.Title);

        // generate the appropriate type of table
        if ( MSX.Statflag == TstatType.SERIES )
            createSeriesTables(raf);
        else
            createStatsTables(raf);

        writeLine("");
        return 0;
    }

    void createSeriesTables(RandomAccessFile raf){
        int  j;

        // Report on all requested nodes
        for (j=1; j<=MSX.Nobjects[ObjectTypes.NODE.id]; j++)
        {
            if ( !MSX.Node[j].getRpt() ) continue;
            IDname = epanet.ENgetnodeid(j);
            createTableHdr(ObjectTypes.NODE, SERIES_TABLE);
            writeNodeTable(raf,j, SERIES_TABLE);
        }

        // Report on all requested links
        for (j=1; j<=MSX.Nobjects[ObjectTypes.LINK.id]; j++)
        {
            if ( !MSX.Link[j].getRpt() ) continue;
            IDname = epanet.ENgetlinkid(j);
            createTableHdr(ObjectTypes.LINK, SERIES_TABLE);
            writeLinkTable(raf,j, SERIES_TABLE);
        }
    }



    void createStatsTables(RandomAccessFile raf){
        int  j;
        int  count;

        // check if any nodes to be reported
        count = 0;
        for (j = 1; j <= MSX.Nobjects[ObjectTypes.NODE.id]; j++) count += MSX.Node[j].getRpt()?1:0;

        // report on all requested nodes
        if ( count > 0 )
        {
            createTableHdr(ObjectTypes.NODE, STATS_TABLE);
            for (j = 1; j <= MSX.Nobjects[ObjectTypes.NODE.id]; j++)
            {
                if ( MSX.Node[j].getRpt()) writeNodeTable(raf,j, STATS_TABLE);
            }
        }

        // Check if any links to be reported
        count = 0;
        for (j = 1; j <= MSX.Nobjects[ObjectTypes.LINK.id]; j++) count += MSX.Link[j].getRpt()?1:0;

        // Report on all requested links
        if ( count > 0 )
        {
            createTableHdr(ObjectTypes.LINK, STATS_TABLE);
            for (j = 1; j <= MSX.Nobjects[ObjectTypes.LINK.id]; j++)
            {
                if ( MSX.Link[j].getRpt() ) writeLinkTable(raf,j, STATS_TABLE);
            }
        }
    }

    void createTableHdr(ObjectTypes objType, int tableType)
    {
        int   m;
        String s1;

        if ( tableType == SERIES_TABLE )
        {
            if ( objType == ObjectTypes.NODE )
                TableHdr.Line1 = String.format("<<< Node %s >>>", IDname);
            else
                TableHdr.Line1 = String.format("<<< Link %s >>>", IDname);
            TableHdr.Line2 = "Time   ";
            TableHdr.Line3 = "hr:min ";
            TableHdr.Line4 = "-------";
        }
        if ( tableType == STATS_TABLE )
        {
            TableHdr.Line1 = "";
            TableHdr.Line2 = String.format("%-16s", StatsHdrs[tableType]);
            if ( objType == ObjectTypes.NODE ) TableHdr.Line3 = "for Node        ";
            else                   TableHdr.Line3 = "for Link        ";
            TableHdr.Line4 = "----------------";
        }
        for (m=1; m<=MSX.Nobjects[ObjectTypes.SPECIES.id]; m++)
        {
            if ( MSX.Species[m].getRpt()==0 ) continue;
            if ( objType == ObjectTypes.NODE && MSX.Species[m].getType() == SpeciesType.WALL ) continue;
            s1 = String.format("  %10s", MSX.Species[m].getId());
            TableHdr.Line2 += s1;
            TableHdr.Line4 += "  ----------";
            s1 = inpReader.MSXinp_getSpeciesUnits(m);

            TableHdr.Line3 +=  String.format("  %10s", s1);
        }
        if ( MSX.PageSize > 0 && MSX.PageSize - LineNum < 8 ) newPage();
        else writeTableHdr();
    }


    void  writeTableHdr()
    {
        if ( MSX.PageSize > 0 && MSX.PageSize - LineNum < 6 ) newPage();
        writeLine("");
        writeLine(TableHdr.Line1);
        writeLine("");
        writeLine(TableHdr.Line2);
        writeLine(TableHdr.Line3);
        writeLine(TableHdr.Line4);
    }

    void  writeNodeTable(RandomAccessFile raf,int j, int tableType)
    {
        int   k, m;
        int [] hrs = new int[1], mins = new int[1];
        float c;

        for (k=0; k<MSX.Nperiods; k++)
        {
            if ( tableType == SERIES_TABLE )
            {
                getHrsMins(k, hrs, mins);
                Line = String.format("%4d:%02d", hrs[0], mins[0]);
            }
            if ( tableType == STATS_TABLE )
            {
                IDname = epanet.ENgetnodeid(j);
                Line = String.format("%-16s", IDname);
            }
            for (m=1; m<=MSX.Nobjects[ObjectTypes.SPECIES.id]; m++)
            {
                if ( MSX.Species[m].getRpt() == 0 ) continue;
                if ( MSX.Species[m].getType() == SpeciesType.WALL ) continue;
                c = out.MSXout_getNodeQual(raf,k, j, m);
                Line += String.format("  %10."+MSX.Species[m].getPrecision()+"f", c);
            }
            writeLine(Line);
        }
    }

    void  writeLinkTable(RandomAccessFile raf,int j, int tableType)
    {
        int   k, m;
        int [] hrs = new int [1], mins = new int [1];
        float c;

        for (k=0; k<MSX.Nperiods; k++)
        {
            if ( tableType == SERIES_TABLE )
            {
                getHrsMins(k, hrs, mins);
                Line = String.format("%4d:%02d", hrs[0], mins[0]);
            }
            if ( tableType == STATS_TABLE )
            {
                IDname = epanet.ENgetlinkid(j);
                Line = String.format("%-16s", IDname);
            }
            for (m=1; m<=MSX.Nobjects[ObjectTypes.SPECIES.id]; m++)
            {
                if ( MSX.Species[m].getRpt() ==0) continue;
                c = out.MSXout_getLinkQual(raf,k, j, m);
                Line += String.format( "  %10."+MSX.Species[m].getPrecision()+"f", c);
            }
            writeLine(Line);
        }
    }

    void getHrsMins(int k, int [] hrs, int [] mins)
    {
        long m, h;

        m = (MSX.Rstart + k*MSX.Rstep) / 60;
        h = m / 60;
        m = m - 60*h;
        hrs[0] = (int)h;
        mins[0] = (int)m;
    }


    void  newPage()
    {
        String  s;
        LineNum = 1;
        writeLine(String.format("\nPage %-3d                                             EPANET-MSX 1.0",   //(modified, FS-01/07/08)
                PageNum));
        writeLine("");
        if ( PageNum > 1 ) writeTableHdr();
        PageNum++;
    }


    void  writeLine(String line)
    {
        if ( LineNum == MSX.PageSize )
            newPage();

        //if ( MSX.RptFile.file ) fprintf(MSX.RptFile.file, "  %s\n", line);   //(modified, FS-01/07/2008)
        //if(MSX.RptFile.getFileIO()!=null){
        //    BufferedWriter din = (BufferedWriter)MSX.RptFile.getFileIO();
        //    try {
        //        din.write(String.format("  %s\n", line));
        //    } catch (IOException e) {
        //        return;
        //    }
        //}
        //else
        //    epanet.ENwriteline(line);
        System.out.println(line);

        LineNum++;
    }

}
