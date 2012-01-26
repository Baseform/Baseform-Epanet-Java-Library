package org.addition.epanetold.ui;

import org.addition.epanetold.*;
import org.addition.epanetold.Types.*;
import org.addition.epanetold.Types.EnumVariables.*;
import org.addition.epanetMSX.EpanetMSX;
import org.addition.epanetMSX.Structures.Species;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ReportGenerator {
    private EpanetMSX epanetMSX;
    private Epanet epanet;

    Long timingsStart;
    Long timingsHyd;
    Long timingsEnd;

    public void setTimings(Long start, Long hyd, Long end){
        timingsStart = start;
        timingsHyd = hyd;
        timingsEnd = end;
    }

    public void setOutFile(File outFile) {
        enOutFile = outFile;
    }

    public void writeEvents(File tempHydEventsFile) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(tempHydEventsFile),"UTF8"));
            String line;

            SimpleXLSXFile.Spreadsheet events = xlsxFile.newSpreadsheet("Hydraulic Events");

            while((line=reader.readLine())!=null){
                if (line.contains(":"))
                {
                    List<Object> obj = new ArrayList<Object>();
                    obj.add(line.substring(0,line.lastIndexOf(':')));
                    obj.add(line.substring(line.lastIndexOf(':')+1));
                    events.addRow(obj);
                }
                else
                    events.addRow(line);
            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeSummary(File tempSummaryFile) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(tempSummaryFile),"UTF8"));
            String line;

            SimpleXLSXFile.Spreadsheet events = xlsxFile.newSpreadsheet("Summary");

            while((line=reader.readLine())!=null){

                if (line.contains("..."))
                {
                    //List<Object> obj = new ArrayList<Object>();
                    //obj.add(line.substring(0,line.indexOf("...")));
                    //obj.add(line.substring(line.lastIndexOf("...")+3));
                    events.addRow(line.substring(0,line.indexOf("...")), line.substring(line.lastIndexOf("...")+3));
                }
                else if (line.contains(":"))
                {
                    //List<Object> obj = new ArrayList<Object>();
                    //obj.add(line.substring(0,line.indexOf(':')));
                    //obj.add(line.substring(line.indexOf(':')+1));
                    events.addRow(line.substring(0,line.indexOf(':')),line.substring(line.indexOf(':')+1));
                }
                else
                    events.addRow(line);
            }

            FlowUnitsType flowflag;
            PressUnitsType pressUnitsType;

            if(epanet!=null){
                flowflag = epanet.getNetwork().getFlowUnits();
                pressUnitsType = epanet.getNetwork().getPressureUnits();
            }
            else {
                flowflag = epanetMSX.getENToolkit().getNetwork().getFlowUnits();
                pressUnitsType = epanetMSX.getENToolkit().getNetwork().getPressureUnits();
            }

            events.addRow("");
            events.addRow("Report period",Utilities.clocktime(reportPeriod));


            if(timingsStart!=null && timingsHyd !=null){
                events.addRow("Hydraulic simulation time ",Utilities.clocktime( (timingsHyd-timingsStart)/1000));
            }

            if(timingsHyd!=null && timingsEnd !=null){
                events.addRow("Quality simulation time ",Utilities.clocktime( (timingsEnd-timingsHyd)/1000));
            }

            if(showHydraulics){
                events.addRow("");
                events.addRow("Hydraulic units");
                for(int  i = 0;i< hydVariables.length;i++){
                    if(!hydVariables[i])
                        continue;

                    List<Object> obj = new ArrayList<Object>();
                    obj.add(HydVariable.values()[i].name);

                    switch (HydVariable.values()[i]) {

                        case HYDR_VARIABLE_HEAD:
                            if(siUnits)
                                obj.add(EnumVariables.u_METERS);
                            else
                                obj.add(EnumVariables.u_FEET);
                            break;
                        case HYDR_VARIABLE_DEMANDS:
                            if(siUnits)
                                obj.add(EnumVariables.u_LPS);
                            else
                                obj.add(EnumVariables.u_GPM);
                            break;
                        case HYDR_VARIABLE_FLOWS:
                            if(siUnits)
                                obj.add(EnumVariables.u_LPS);
                            else
                                obj.add(EnumVariables.u_GPM);
                            break;
                        case HYDR_VARIABLE_PRESSURE:
                            if(siUnits){
                                if (pressUnitsType == EnumVariables.PressUnitsType.METERS)
                                    obj.add(EnumVariables.u_METERS);
                                else
                                    obj.add(EnumVariables.u_KPA);
                            }
                            else
                                obj.add(EnumVariables.u_PSI);
                            break;
                        case HYDR_VARIABLE_VELOCITY:
                            if(siUnits)
                                obj.add(EnumVariables.u_MperSEC);
                            else
                                obj.add(EnumVariables.u_FTperSEC);
                            break;
                        case HYDR_VARIABLE_HEADLOSS:
                            if(siUnits)
                                obj.add(EnumVariables.u_per1000M);
                            else
                                obj.add(EnumVariables.u_per1000FT);
                            break;
                        case HYDR_VARIABLE_FRICTION:
                            break;
                        default:
                            continue;
                    }

                    events.addRow(obj);
                }
            }

            if(showQuality){
                events.addRow("");
                events.addRow("Quality units");
                for(int  i = 0;i< qualVariables.length;i++){
                    if(!qualVariables[i])
                        continue;

                    List<Object> obj = new ArrayList<Object>();
                    obj.add(QualVariable.values()[i].name);

                    switch (QualVariable.values()[i]) {


                        case QUAL_VARIABLE_NODES:
                            obj.add(epanet.getNetwork().getChemName() + " " + epanet.getNetwork().getChemUnits());
                            break;
                        case QUAL_VARIABLE_LINKS:
                            obj.add(epanet.getNetwork().getChemName() + " " + epanet.getNetwork().getChemUnits());
                            break;
                        case QUAL_VARIABLE_RATE:
                            obj.add(epanet.getNetwork().getField(EnumVariables.FieldType.REACTRATE).getUnits());
                            break;
                    }

                    events.addRow(obj);
                }
            }

            if(showMSXQuality){
                events.addRow("");
                events.addRow("Quality MSX units");
                for(int  i = 0;i< speVariables.length;i++){
                    if(!speVariables[i])
                        continue;

                    events.addRow(epanetMSX.getNetwork().getSpecies()[i+1].getId(),epanetMSX.getReader().MSXinp_getSpeciesUnits(i+1));
                }

            }

            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // "Nodes heads", "Nodes actual demands","Link flows","Link status", "Link settings","Node pressure","Link Velocity", "Link headloss"
    public enum HydVariable{
        HYDR_VARIABLE_HEAD      (0,"Node head",true),
        HYDR_VARIABLE_DEMANDS   (1,"Node actual demand",true),
        HYDR_VARIABLE_FLOWS     (2,"Link flows",false),
        HYDR_VARIABLE_STATUS    (3,"Link status",false),
        HYDR_VARIABLE_SETTINGS  (4,"Link settings",false),
        HYDR_VARIABLE_PRESSURE  (5,"Node pressure",true),
        HYDR_VARIABLE_VELOCITY  (6,"Link velocity",false),
        HYDR_VARIABLE_HEADLOSS  (7,"Link unit headloss",false),
        HYDR_VARIABLE_FRICTION  (8,"Link friction factor",false);
        public final boolean isNode;
        public final int id;
        public final String name;
        HydVariable(int val, String text, boolean node){id =val;name = text;isNode=node;}
    }

    // "Nodes quality", "Link quality","Link reaction rate"
    public enum QualVariable{
        QUAL_VARIABLE_NODES     (0,"Node quality",true),
        QUAL_VARIABLE_LINKS     (1,"Link quality",false),
        QUAL_VARIABLE_RATE      (2,"Link reaction rate",false);

        public final boolean isNode;
        public final int id;
        public final String name;
        QualVariable(int val, String text, boolean node){id =val;name = text;isNode=node;}
    }


    File hydFile;
    File enOutFile;
    File msxOutFile;

    boolean showHydraulics;
    boolean showQuality;
    boolean showMSXQuality;
    boolean [] hydVariables;
    boolean [] qualVariables;
    boolean [] speVariables;
    boolean showSummary;

    SimpleXLSXFile xlsxFile;

    int duration;
    int reportPeriod;
    boolean siUnits;
    Network netwk;

    long repStartTime = 0;

    public ReportGenerator(Epanet epa, EpanetMSX epaMSX){
        siUnits = true;

        epanet = epa;
        epanetMSX = epaMSX;
        showHydraulics = false;
        showQuality = false;
        showMSXQuality = false;
        showSummary = false;
        hydVariables = new boolean[HydVariable.values().length];
        qualVariables = new boolean[QualVariable.values().length];

        if(epa!=null){
            duration = (int)epa.getNetwork().getDur();
            reportPeriod = (int)epa.getNetwork().getHstep();
            hydFile = epa.getHydFile();
            enOutFile = epa.getOutFile();
            netwk = epa.getNetwork();
        }
        else if(epaMSX!=null){
            duration = (int)epaMSX.getENToolkit().ENgettimeparam(ENToolkit.EN_DURATION);
            hydFile = new File(epaMSX.getNetwork().getHydFile().getFilename());
            msxOutFile = new File(epaMSX.getNetwork().getOutFile().getFilename());
            reportPeriod = (int)epaMSX.getNetwork().getQstep();
            netwk = epaMSX.getENToolkit().getNetwork();
            if(epaMSX.getNetwork().getSpecies().length>0)
                speVariables = new boolean[epaMSX.getNetwork().getSpecies().length];
        }
        xlsxFile = new SimpleXLSXFile();
    }


    public void setReportStartTime(long repStartTime) {
        this.repStartTime = repStartTime;
    }


    public void setHydFile(File hydFile) {
        this.hydFile = hydFile;
    }

    void openReport(String report){
        xlsxFile = new SimpleXLSXFile();
    }


    public void writeHydraulics() throws IOException
    {
        if(hydFile==null || !hydFile.exists() || !showHydraulics)
            return;

        boolean isAnyEnabled = false;
        for (boolean b : hydVariables)
            if(b){
                isAnyEnabled=true;
                break;
            }
        if(!isAnyEnabled)
            return;


        int Nnodes = netwk.getMaxNodes();
        int Nlinks = netwk.getMaxLinks();
        SimpleXLSXFile.Spreadsheet sheets[] = new SimpleXLSXFile.Spreadsheet[hydVariables.length];

        Object[] nodesHead = new String[Nnodes + 1];
        nodesHead[0] = "Time/node";
        for (int i = 1; i <= Nnodes; i++) {
            Node node = netwk.getNode(i);
            nodesHead[i] = node.getId();
        }

        Object[] linksHead = new String[Nlinks + 1];
        linksHead[0] = "Time/link";
        for (int i = 1; i <= Nlinks; i++) {
            Link link = netwk.getLink(i);
            linksHead[i] = link.getId();
        }

        // init sheets and headers
        for(int i = 0;i<hydVariables.length;i++)
            if(hydVariables[i])
            {
                sheets[i] = xlsxFile.newSpreadsheet(HydVariable.values()[i].name);

                if(HydVariable.values()[i].isNode)
                    sheets[i].addRow(nodesHead);
                else
                    sheets[i].addRow(linksHead);
            }
            else
                sheets[i] = null;

        DataInputStream din = new DataInputStream(new BufferedInputStream(new FileInputStream(hydFile)));

        // Parse hydraulics file

        din.skipBytes(32);

        Object nodeRow[] = new Object[Nnodes + 1];
        Object linkRow[] = new Object[Nlinks + 1];

        int stepByteSize = (Nnodes * 2 + Nlinks*3 + 2 )*4;

        UnitsType unitsType = siUnits?EnumVariables.UnitsType.SI:EnumVariables.UnitsType.US;

        for(int time = 0;time<= duration;time+=reportPeriod){
            int hydTime = din.readInt();

            // Passo menor que o esperado.
            if(hydTime<time){
                System.out.println(hydTime + "-" + time);
                if(hydTime == duration)
                    break;
                time -= reportPeriod;
                din.skipBytes(stepByteSize - 4);
                continue;
            }
            else
                System.out.println(hydTime + "-" + time + " OK");

            if(hydTime<this.repStartTime){
                din.skipBytes(stepByteSize - 4);
                continue;
            }

            HydraulicTS hyd = readhyd(din,netwk);

            nodeRow[0] = Utilities.clocktime(hydTime);

            // DEMAND
            if(sheets[HydVariable.HYDR_VARIABLE_DEMANDS.id]!=null){
                for (int i = 1; i <= Nnodes; i++) {
                    nodeRow[i] = convertUnits(FieldType.DEMAND, (double) hyd.D[i],0);
                }
                sheets[HydVariable.HYDR_VARIABLE_DEMANDS.id].addRow(nodeRow);
            }

            // HEAD
            if(sheets[HydVariable.HYDR_VARIABLE_HEAD.id]!=null){
                for (int i = 1; i <= Nnodes; i++)
                    nodeRow[i] = convertUnits(FieldType.ELEV,(double) hyd.H[i],0);

                sheets[HydVariable.HYDR_VARIABLE_HEAD.id].addRow(nodeRow);
            }

            linkRow[0] = Utilities.clocktime(hydTime);

            // FLOW
            if(sheets[HydVariable.HYDR_VARIABLE_FLOWS.id]!=null){
                for (int i = 1; i <= Nlinks; i++)
                    linkRow[i] = convertUnits(FieldType.FLOW,(double) hyd.Q[i],0);

                sheets[HydVariable.HYDR_VARIABLE_FLOWS.id].addRow(linkRow);
            }

            // LINK STATUS
            if(sheets[HydVariable.HYDR_VARIABLE_STATUS.id]!=null){
                for (int i = 1; i <= Nlinks; i++)
                    if(hyd.S[i]!=null)
                        linkRow[i] = (double) hyd.S[i].ordinal();
                    else
                        linkRow[i] = -1.0;
                sheets[HydVariable.HYDR_VARIABLE_STATUS.id].addRow(linkRow);
            }

            // LINK SETTING
            if(sheets[HydVariable.HYDR_VARIABLE_SETTINGS.id]!=null){
                for (int i = 1; i <= Nlinks; i++)
                    linkRow[i] = convertUnits(FieldType.SETTING,(double) hyd.K[i],i);

                sheets[HydVariable.HYDR_VARIABLE_SETTINGS.id].addRow(linkRow);
            }

            // ##########################
            // NODE PRESSURE
            if(sheets[HydVariable.HYDR_VARIABLE_PRESSURE.id]!=null){
                for (int i = 1; i <= Nnodes; i++)
                    nodeRow[i] = (float)convertPressure(unitsType,hyd.H[i] - netwk.getNode(i).getElevation());

                sheets[HydVariable.HYDR_VARIABLE_PRESSURE.id].addRow(nodeRow);
            }

            //###########################
            // LINK VELOCITY
            if(sheets[HydVariable.HYDR_VARIABLE_VELOCITY.id]!=null){
                for (int i = 1; i <= Nlinks; i++){
                    if (netwk.getLink(i).getType() == LinkType.PUMP)
                        linkRow[i] = 0.0f;
                    else
                    {
                        double q = Math.abs(hyd.Q[i]);
                        double a = Constants.PI*Math.pow(netwk.getLink(i).getDiameter(),2)/4.0;
                        linkRow[i] = (float)convertUnits(FieldType.ELEV,q/a,0);
                    }
                }
                sheets[HydVariable.HYDR_VARIABLE_VELOCITY.id].addRow(linkRow);
            }

            //###########################
            // LINK HEADLOSS
            if(sheets[HydVariable.HYDR_VARIABLE_HEADLOSS.id]!=null){
                for (int i = 1; i <= Nlinks; i++){
                    Link lk = netwk.getLink(i);
                    if (hyd.S[i].ordinal() <= StatType.CLOSED.ordinal()) linkRow[i] = 0.0f;
                    else
                    {
                        double h = hyd.H[lk.getN1()] - hyd.H[lk.getN2()];
                        if (lk.getType() != LinkType.PUMP) h = Math.abs(h);
                        if (lk.getType().ordinal() <= LinkType.PIPE.ordinal())
                            linkRow[i] = (float)(1000.0*h/lk.getLenght());
                        else
                            linkRow[i] = (float)convertUnits(FieldType.ELEV,h,0);
                    }

                }
                sheets[HydVariable.HYDR_VARIABLE_HEADLOSS.id].addRow(linkRow);
            }

            //###########################
            // LINK FRICTION
            if(sheets[HydVariable.HYDR_VARIABLE_FRICTION.id]!=null){
                for (int i=1; i<=netwk.getMaxLinks(); i++)
                {
                    Link lk = netwk.getLink(i);
                    if (netwk.getLink(i).getType().ordinal() <= LinkType.PIPE.ordinal() && Math.abs(hyd.Q[i]) > Constants.TINY)
                    {
                        double h = Math.abs(hyd.H[lk.getN1()] - hyd.H[lk.getN2()]);
                        double f = 39.725*h*Math.pow(lk.getDiameter(), 5)/lk.getLenght()/(hyd.Q[i]*hyd.Q[i]);
                        linkRow[i] = (float)f;
                    }
                    else linkRow[i] = 0.0f;
                }
                sheets[HydVariable.HYDR_VARIABLE_FRICTION.id].addRow(linkRow);
            }

            if(hyd.hydstep==0)
                break;

        }

        din.close();

    }


    public void writeQuality() throws IOException
    {
        if(epanet==null || enOutFile==null || !enOutFile.exists() || !showQuality)
            return;


        int Nnodes = netwk.getMaxNodes();
        int Nlinks = netwk.getMaxLinks();

        SimpleXLSXFile.Spreadsheet sheets[] = new SimpleXLSXFile.Spreadsheet[qualVariables.length];

        Object[] nodesHead = new String[Nnodes + 1];
        nodesHead[0] = "Time/node";
        for (int i = 1; i <= Nnodes; i++) {
            Node node = netwk.getNode(i);
            nodesHead[i] = node.getId();
        }

        Object[] linksHead = new String[Nlinks + 1];
        linksHead[0] = "Time/link";
        for (int i = 1; i <= Nlinks; i++) {
            Link link = netwk.getLink(i);
            linksHead[i] = link.getId();
        }

        // Init sheets and headers
        for(int i = 0;i<qualVariables.length;i++)
            if(qualVariables[i])
            {
                sheets[i] = xlsxFile.newSpreadsheet(QualVariable.values()[i].name);

                if(QualVariable.values()[i].isNode)
                    sheets[i].addRow(nodesHead);
                else
                    sheets[i].addRow(linksHead);
            }
            else
                sheets[i] = null;

        // Parse hydraulics file
        DataInputStream outDin = new DataInputStream(new BufferedInputStream(new FileInputStream(enOutFile)));
        outDin.skipBytes((int) epanet.getOutput().getHeaderOffset());

        DataInputStream hydDin = new DataInputStream(new BufferedInputStream(new FileInputStream(hydFile)));
        hydDin.skipBytes(32);


        Object nodeRow[] = new Object[Nnodes + 1];
        Object linkRow[] = new Object[Nlinks + 1];

        int hydStepSize = (Nnodes * 2 + Nlinks*3)*4;
        int outStepSize = (Nnodes * 4 + Nlinks*8)*4;

        //UnitsType epanetUnis = netwk.getUnitsflag();
        //UnitsType unitsType = siUnits?EnumVariables.UnitsType.SI:EnumVariables.UnitsType.US;

        //System.out.println("Periods : " + epanetold.getQuality().getNperiods());
        for(int it = 0, time=0; it< epanet.getQuality().getNperiods();it++,time+=reportPeriod){

            int hydTime = hydDin.readInt();
            hydDin.skipBytes(hydStepSize);
            int hydTStep = hydDin.readInt();

            if(hydTime<time){
                //System.out.println(it + " " + hydTime + "-" + time);
                if(hydTime == duration)
                    break;
                time -= reportPeriod;
                outDin.skipBytes(outStepSize);
                continue;
            }
            //else
            //    System.out.println(it + " " + hydTime + "-" + time + " OK");



            nodeRow[0] = Utilities.clocktime(hydTime);
            linkRow[0] = Utilities.clocktime(hydTime);

            outDin.skipBytes(Nnodes*4);
            outDin.skipBytes(Nnodes*4);
            outDin.skipBytes(Nnodes*4);

            /// NODES QUALITY
            for (int i = 1; i <= Nnodes; i++) {
                nodeRow[i] = (outDin.readFloat());
            }

            if(hydTime>=this.repStartTime)
                if(sheets[QualVariable.QUAL_VARIABLE_NODES.id]!=null){
                    sheets[QualVariable.QUAL_VARIABLE_NODES.id].addRow(nodeRow);
                }

            outDin.skipBytes(Nlinks*4);// FLOW
            outDin.skipBytes(Nlinks*4);// VELOCITY
            outDin.skipBytes(Nlinks*4);// HEADLOSS

            /// LINK QUALITY
            for (int i = 1; i <= Nlinks; i++) {
                linkRow[i] = (outDin.readFloat());
            }

            if(hydTime>=this.repStartTime)
                if(sheets[QualVariable.QUAL_VARIABLE_LINKS.id]!=null){
                    sheets[QualVariable.QUAL_VARIABLE_LINKS.id].addRow(linkRow);
                }

            outDin.skipBytes(Nlinks*4);// STATUS
            outDin.skipBytes(Nlinks*4);// SETTING

            /// REACT RATE
            for (int i = 1; i <= Nlinks; i++) {
                linkRow[i] = (outDin.readFloat());
            }

            if(hydTime>=this.repStartTime)
                if(sheets[QualVariable.QUAL_VARIABLE_RATE.id]!=null){
                    sheets[QualVariable.QUAL_VARIABLE_RATE.id].addRow(linkRow);
                }

            outDin.skipBytes(Nlinks*4);// FRICTION


            if(hydTStep==0)
                break;
        }

        outDin.close();
    }


    public void writeQualityMSX() throws IOException
    {
        if(epanetMSX==null  || !showMSXQuality)
            return;


        boolean isAnyEnabled = false;
        for (boolean b : speVariables)
            if(b){
                isAnyEnabled=true;
                break;
            }
        if(!isAnyEnabled)
            return;


        int Nnodes = netwk.getMaxNodes();
        int Nlinks = netwk.getMaxLinks();

        epanetMSX.getReport().extraOpenOutputFile();
        org.addition.epanetMSX.Network MSX = epanetMSX.getNetwork();
        int count = 0;
        for (int j = 1; j < MSX.getNodes().length; j++) count += MSX.getNodes()[j].getRpt();
        SimpleXLSXFile.Spreadsheet sheetsNodes[] = new SimpleXLSXFile.Spreadsheet[count];

        for (int j = 1,i = 0; j < MSX.getNodes().length; j++){
            if(MSX.getNodes()[j].getRpt()!=0){
                sheetsNodes[i] = xlsxFile.newSpreadsheet("Node&lt;&lt;"+epanetMSX.getENToolkit().ENgetnodeid(j)+"&gt;&gt;");
                List<Object> rowsTop = new ArrayList<Object>();
                rowsTop.add("Time");

                for(int aux = 1;aux<epanetMSX.getNetwork().getSpecies().length;aux++){
                    Species spe = epanetMSX.getNetwork().getSpecies()[aux];
                    if(spe !=null && spe.getId()!=null && speVariables[aux-1]){
                        rowsTop.add(spe.getId());
                    }
                }
                sheetsNodes[i].addRow(rowsTop);
                i++;
            }
        }

        count= 0;
        for (int j = 1; j < MSX.getLinks().length; j++) count += MSX.getLinks()[j].getRpt();
        SimpleXLSXFile.Spreadsheet sheetsLinks[] = new SimpleXLSXFile.Spreadsheet[count];

        for (int j = 1,i = 0; j < MSX.getLinks().length; j++){
            if(MSX.getLinks()[j].getRpt()!=0){
                sheetsLinks[i] = xlsxFile.newSpreadsheet("Link&lt;&lt;"+epanetMSX.getENToolkit().ENgetlinkid(j)+"&gt;&gt;");
                List<Object> rowsTop = new ArrayList<Object>();
                rowsTop.add("Time");

                for(int aux = 1;aux<epanetMSX.getNetwork().getSpecies().length;aux++){
                    Species spe = epanetMSX.getNetwork().getSpecies()[aux];
                    if(spe !=null && spe.getId()!=null && speVariables[aux-1]){
                        rowsTop.add(spe.getId());
                    }
                }
                sheetsLinks[i].addRow(rowsTop);
                i++;
            }
        }

        DataInputStream hydDin = new DataInputStream(new BufferedInputStream(new FileInputStream(epanetMSX.getNetwork().getHydFile().getFilename())));
        hydDin.skipBytes(32);
        int hydStepSize = (Nnodes * 2 + Nlinks*3)*4;

        for(int it = 0, time=0; it< MSX.getNperiods();it++,time+=reportPeriod){
            //int hydTime = hydDin.readInt();
            hydDin.skipBytes(hydStepSize);
            int hydTStep = hydDin.readInt();

            //if(hydTime<time){
            //    //System.out.println(it + " " + hydTime + "-" + time);
            //    if(hydTime == duration)
            //        break;
            //    time -= reportPeriod;
            //
            //    continue;
            //}
            //else
            //    System.out.println(it + " " + hydTime + "-" + time + " OK");

            if(time<this.repStartTime)
              continue;

            String cellTime = Utilities.clocktime(time);

            for (int j = 1,i = 0; j < MSX.getNodes().length; j++){
                if(MSX.getNodes()[j].getRpt()==0)
                    continue;

                List<Object> row = new ArrayList<Object>();
                row.add(cellTime);

                for(int aux = 1;aux<epanetMSX.getNetwork().getSpecies().length;aux++){

                    if(!speVariables[aux-1])
                        continue;
                    double qual = epanetMSX.getOutput().MSXout_getNodeQual(it,j,aux);
                    row.add(new Double(qual));
                }

                sheetsNodes[i].addRow(row);

                i++;
            }

            for (int j = 1,i = 0; j < MSX.getLinks().length; j++){
                if(MSX.getLinks()[j].getRpt()==0)
                    continue;

                List<Object> row = new ArrayList<Object>();
                row.add(cellTime);

                for(int aux = 1;aux<epanetMSX.getNetwork().getSpecies().length;aux++){

                    if(!speVariables[aux-1])
                        continue;
                    double qual = epanetMSX.getOutput().MSXout_getLinkQual(it, j, aux);
                    row.add(new Double(qual));
                }

                sheetsLinks[i].addRow(row);

                i++;
            }

            if(hydTStep==0)
                break;
        }



        hydDin.close();
        epanetMSX.getReport().extraCloseOutputFile();

    }

    public void saveReport(String filename) throws IOException {
        xlsxFile.save(new FileOutputStream(filename));
        xlsxFile.finish();
    }

    public void setShowHydraulics(boolean showHydraulics) {
        this.showHydraulics = showHydraulics;
    }

    public void setShowQuality(boolean showQuality) {
        this.showQuality = showQuality;
    }

    public void setShowMSXQuality(boolean showMSXQuality) {
        this.showMSXQuality = showMSXQuality;
    }

    public void setShowSummary(boolean showSummary) {
        this.showSummary = showSummary;
    }

    public void setHydVariables(int id ,boolean status) {
        this.hydVariables[id] = status;
    }

    public void setQualityVariables(int i, boolean selected) {
        this.qualVariables[i] = selected;
    }

    public void setSpeVariables(int id ,boolean status) {
        this.speVariables[id] = status;
    }

    public void setReportPeriod(int reportPeriod) {
        this.reportPeriod = reportPeriod;
    }

    public void setSiUnits(boolean siUnits) {
        this.siUnits = siUnits;
    }

    private static HydraulicTS readhyd(DataInputStream file, Network net) throws IOException {
        int Nnodes = net.getMaxNodes();
        int Nlinks = net.getMaxLinks();
        HydraulicTS hyd = new HydraulicTS(Nnodes, Nlinks);

        for (int i = 0; i < Nnodes; i++)
            hyd.D[i + 1] = file.readFloat();

        for (int i = 0; i < Nnodes; i++)
            hyd.H[i + 1] = file.readFloat();

        for (int i = 0; i < Nlinks; i++)
            hyd.Q[i + 1] = file.readFloat();

        for (int i = 0; i < Nlinks; i++){
            Integer val = new Integer((int)file.readFloat());
            try{
                hyd.S[i + 1] = EnumVariables.StatType.values()[val];
            }
            catch (Exception ex){
                hyd.S[i + 1] = null;
            }
        }
        for (int i = 0; i < Nlinks; i++)
            hyd.K[i + 1] = file.readFloat();

        hyd.hydstep = file.readInt();

        return hyd;
    }

    private static class HydraulicTS {
        public float[] D;
        public float[] H;
        public float[] Q;
        public EnumVariables.StatType[] S;
        public float[] K;
        public long hydtime;
        public int hydstep;

        public HydraulicTS(int nnodes, int nlinks) {
            D = new float[nnodes + 1];
            H = new float[nnodes + 1];
            Q = new float[nlinks + 1];
            S = new EnumVariables.StatType[nlinks + 1];
            K = new float[nlinks + 1];
        }
    }

    //public static void main(String[] args) throws IOException {
    //    SimpleXLSXFile xlsx = new  SimpleXLSXFile("teste.xlsx");
    //
    //    Spreadsheet sheet = xlsx.newSpreadsheet("YEP");
    //    sheet.addRow(new String("fkjfdjkfdjkdf"));
    //    sheet.addRow(new Double[]{1.0,2.0,3.0,4.0});
    //    xlsx.save();
    //}

    private double convertUnits(EnumVariables.FieldType types, double source, int link_id){
        UnitsType type = siUnits?EnumVariables.UnitsType.SI:EnumVariables.UnitsType.US;
        switch (types)
        {
            case ELEV:
                if(siUnits)
                    return source * Constants.MperFT;
                else if(!siUnits)
                    return source;
            case DEMAND:
                return convertDemand(type,source);
            case FLOW:
                return convertDemand(type,source);
            case SETTING:
                Link lk = netwk.getLink(link_id);
                return convertSetting(lk,type,source);
            default:
                return source;
        }
    }

    public double convertDemand(EnumVariables.UnitsType type,double value){
        FlowUnitsType flow = netwk.getFlowUnits();
        if(type.equals(EnumVariables.UnitsType.SI)){
            double qcf = Constants.LPSperCFS;
            if (flow == EnumVariables.FlowUnitsType.LPM) qcf = Constants.LPMperCFS;
            if (flow == EnumVariables.FlowUnitsType.MLD) qcf = Constants.MLDperCFS;
            if (flow == EnumVariables.FlowUnitsType.CMH) qcf = Constants.CMHperCFS;
            if (flow == EnumVariables.FlowUnitsType.CMD) qcf = Constants.CMDperCFS;
            return qcf * value;
        }
        else
        {
            double qcf = 1.0;
            if (flow == EnumVariables.FlowUnitsType.GPM) qcf = Constants.GPMperCFS;
            if (flow == EnumVariables.FlowUnitsType.MGD) qcf = Constants.MGDperCFS;
            if (flow == EnumVariables.FlowUnitsType.IMGD)qcf = Constants.IMGDperCFS;
            if (flow == EnumVariables.FlowUnitsType.AFD) qcf = Constants.AFDperCFS;
            return qcf * value;
        }
    }

    public double convertPressure(EnumVariables.UnitsType type, double value){
        PressUnitsType pressure = netwk.getPressureUnits();
        double SpGrav = netwk.getSpGrav();
        if(type.equals(EnumVariables.UnitsType.SI)){
            double pcf;
            if (pressure == EnumVariables.PressUnitsType.METERS)
                pcf = Constants.MperFT*SpGrav;
            else
                pcf = Constants.KPAperPSI*Constants.PSIperFT*SpGrav;

            return value*pcf;
        }
        else
            return value * Constants.PSIperFT*SpGrav;
    }
    public double convertSetting(Link lk,EnumVariables.UnitsType type,double value){
        double ret;
        if (value != Constants.MISSING)
            switch (lk.getType())
            {
                case CV:
                case PIPE: ret = value;
                    break;
                case PUMP: ret = value;
                    break;
                case PRV:
                case PSV:
                case PBV:  ret = convertPressure(type,value);
                    break;
                case FCV:  ret = (value*convertDemand(type,value));
                    break;
                case TCV:  ret = value;
                    break;
                default:   ret = 0.0f;
            }
        else ret = 0.0f;

        return ret;
    }

}
