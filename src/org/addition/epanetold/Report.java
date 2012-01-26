package org.addition.epanetold;

import java.io.*;
import java.util.Date;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.addition.epanetold.Types.*;
import org.addition.epanetold.Types.EnumVariables.*;

public class Report {
    public static final ResourceBundle textBundle = PropertyResourceBundle.getBundle("Text");
    public static final ResourceBundle errorBundle = PropertyResourceBundle.getBundle("Error");

    private static String getError(String key){
        return errorBundle.getString(key);
    }

    private static String getText(String key){
        return textBundle.getString(key);
    }


    BufferedWriter RptFile;
    private boolean Rptflag;


    private long LineNum;               // Current line number
    private long PageNum;               // Current page number
    private boolean Fprinterr;          // File write error flag

    private Epanet          epanet;
    private Network         net;
    private Hydraulic       hyd;
    private SparseMatrix    smat;
    private Output          out;

    Report(Epanet epanet) {
        this.epanet =  epanet;
        RptFile = null;
        Rptflag = true;
    }

    void loadDependencies(){
        net = epanet.getNetwork();
        hyd = epanet.getHydraulicsSolver();
        smat = epanet.getSparseMatrix();
        out = epanet.getOutput();
    }

    public boolean isRptflag() {
        return this.Rptflag;
    }

    public void setRptflag(boolean rptflag) {
        this.Rptflag = rptflag;
    }


    public int open(File f)
    {
        try {
            RptFile = new BufferedWriter(new FileWriter(f));
        } catch (IOException e) {
            return 308;
        }
        return 0;
    }

    /*public void openStdout()
    {
        try {
            RptFile = new BufferedWriter(new OutputStreamWriter(System.out));
        } catch (Exception e) {
            e.printStackTrace();
        }
    } */

    public void close()
    {
        if(RptFile==null) return;
        try {
            RptFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // writes formatted output report to file
    int  writereport(int Nperiods)
    {
        boolean  tflag;
        BufferedWriter  tfile;
        int   errcode = 0;

        /*Fprinterr = false;
        if (Rptflag && net.Rpt2Fname !=null && net.Rpt2Fname.length() == 0 && RptFile != null)
        {
            Utilities.writecon(textBundle.getString("FMT17"));
            Utilities.writecon(epanetold.getRpt1Fname());
            if (net.Energyflag) writeenergy();
            errcode = writeresults(Nperiods);
        }
        else if (net.Rpt2Fname.length() > 0)
        {

            if (net.Rpt2Fname.equalsIgnoreCase(epanetold.getInpFname()) ||
                    net.Rpt2Fname.equalsIgnoreCase(epanetold.getRpt1Fname()))
            {
                Utilities.writecon(getText("FMT17"));
                Utilities.writecon(epanetold.getRpt1Fname());
                if (net.Energyflag) writeenergy();
                errcode = writeresults(Nperiods);
            }
            else
            {*/
                tfile = RptFile;
                tflag = Rptflag;

               /* try {
                    //RptFile = new BufferedWriter(new FileWriter(epanetold.getRpt1Fname()));
                } catch (Exception e) {
                    RptFile = null;
                } */

                /*if (RptFile == null)
                {
                    RptFile = tfile;
                    Rptflag = tflag;
                    errcode = 303;
                }
                else
                {
                    Rptflag = true;
                    Utilities.writecon(getText("FMT17") + " " + epanetold.getRpt1Fname());
                    writelogo();
                    //if (net.Summaryflag)
                        writesummary();
                    //if (net.Energyflag)
                        writeenergy();
                    errcode = writeresults(Nperiods);     */
                    try {
                        RptFile.close();
                    } catch (IOException e) {

                    }
                    RptFile = tfile;
                    Rptflag = tflag;
               // }
            //}
       // }

       // if (Fprinterr) errmsg(309);
        return(0);
    }


    // writes program logo to report file.
    void writelogo()
    {
        String logo[] = {getText("LOG01"),getText("LOG02"),
                getText("LOG03"),getText("LOG04"),
                getText("LOG05"),getText("LOG06")};

        PageNum = 1;
        LineNum = 2;

        writeline(getText("FMT18") + (new Date()).toString());
        writeline("");

        for (String s: logo)
            writeline(s);

        writeline("");
    }


    // Writes summary system information to report file
    void  writesummary()
    {
        int  i;
        int  nres = 0;

        if(net.getTitleText()!=null)
            for (i=0; i<net.getTitleText().size() && i<3; i++){
                if (net.getTitleText().get(i).length() > 0){
                    if(net.getTitleText().get(i).length()<=70)
                        writeline(net.getTitleText().get(i));
                    else
                        writeline(net.getTitleText().get(i).substring(0,70));
                }
            }
        writeline(" ");
        writeline(String.format(getText("FMT19"),epanet.mInpFile.getName()));
        writeline(String.format(getText("FMT20"),net.getSections(SectType._JUNCTIONS)));
        for (i=1; i<=net.getMaxTanks(); i++)
            if (net.getTank(i).getArea() == 0.0) nres++;
        writeline(String.format(getText("FMT21a"),nres));
        writeline(String.format(getText("FMT21b"),net.getMaxTanks()-nres));
        writeline(String.format(getText("FMT22"),net.getSections(SectType._PIPES)));
        writeline(String.format(getText("FMT23"),net.getSections(SectType._PUMPS)));
        writeline(String.format(getText("FMT24"),net.getSections(SectType._VALVES)));
        writeline(String.format(getText("FMT25"),
                EnumVariables.RptFormTxt[net.Formflag.ordinal()]));
        writeline(String.format(getText("FMT26"),
                net.Hstep*net.getUcf(FieldType.TIME),net.getField(FieldType.TIME).getUnits()));
        writeline(String.format(getText("FMT27"),net.Hacc));
        writeline(String.format(getText("FMT27a"),net.CheckFreq));
        writeline(String.format(getText("FMT27b"),net.MaxCheck));
        writeline(String.format(getText("FMT27c"),net.DampLimit));
        writeline(String.format(getText("FMT28"),net.MaxIter));


        if (net.Qualflag == QualType.NONE || net.Dur == 0.0)
            writeline(getText("FMT29"));
        else if (net.Qualflag == QualType.CHEM)
            writeline(String.format(getText("FMT30"),net.ChemName));
        else if (net.Qualflag == QualType.TRACE)
            writeline(String.format(getText("FMT31"),net.getNode(net.TraceNode).getId()));
        else if (net.Qualflag == QualType.AGE)
            writeline(getText("FMT32"));

        if (net.Qualflag != QualType.NONE && net.Dur > 0)
        {
            writeline(String.format(getText("FMT33"),(float)net.Qstep/60.0));
            writeline(String.format(getText("FMT34"),net.Ctol*net.getUcf(FieldType.QUALITY),
                    net.getField(FieldType.QUALITY).getUnits()));
        }

        writeline(String.format(getText("FMT36"),net.SpGrav));
        writeline(String.format(getText("FMT37a"),net.Viscos/ Constants.VISCOS));
        writeline(String.format(getText("FMT37b"),net.Diffus/ Constants.DIFFUS));
        writeline(String.format(getText("FMT38"),net.Dmult));
        writeline(String.format(getText("FMT39"),net.Dur*net.getUcf(FieldType.TIME),
                    net.getField(FieldType.TIME).getUnits()));

        if (Rptflag)
        {
            writeline(getText("FMT40"));

            if (net.Nodeflag == ReportFlag.FALSE) writeline(getText("FMT41"));
            if (net.Nodeflag == ReportFlag.TRUE)  writeline(getText("FMT42"));
            if (net.Nodeflag == ReportFlag.SOME) writeline(getText("FMT43"));
            writelimits(FieldType.DEMAND,FieldType.QUALITY);
            if (net.Linkflag == ReportFlag.FALSE)  writeline(getText("FMT44"));
            if (net.Linkflag == ReportFlag.TRUE)  writeline(getText("FMT45"));
            if (net.Linkflag == ReportFlag.SOME)  writeline(getText("FMT46"));
            writelimits(FieldType.DIAM,FieldType.HEADLOSS);
        }
        writeline(" ");
    }

    public void  writesummary2(BufferedWriter buf)
    {
        int  i;
        int  nres = 0;
        try{
        if(net.getTitleText()!=null)
            for (i=0; i<net.getTitleText().size() && i<3; i++){
                if (net.getTitleText().get(i).length() > 0){
                    if(net.getTitleText().get(i).length()<=70)
                        buf.write(net.getTitleText().get(i)+"\n");
                    else
                        buf.write(net.getTitleText().get(i).substring(0,70)+"\n");
                }
            }
        buf.write("\n");
        buf.write(String.format(getText("FMT19"),epanet.mInpFile.getName())+"\n");
        buf.write(String.format(getText("FMT20"),net.getSections(SectType._JUNCTIONS))+"\n");
        for (i=1; i<=net.getMaxTanks(); i++)
            if (net.getTank(i).getArea() == 0.0) nres++;
        buf.write(String.format(getText("FMT21a"),nres)+"\n");
        buf.write(String.format(getText("FMT21b"),net.getMaxTanks()-nres)+"\n");
        buf.write(String.format(getText("FMT22"),net.getSections(SectType._PIPES))+"\n");
        buf.write(String.format(getText("FMT23"),net.getSections(SectType._PUMPS))+"\n");
        buf.write(String.format(getText("FMT24"),net.getSections(SectType._VALVES))+"\n");
        buf.write(String.format(getText("FMT25"),
                EnumVariables.RptFormTxt[net.Formflag.ordinal()])+"\n");
        //buf.write(String.format(getText("FMT26"),
        //        net.Hstep*net.getUcf(FieldType.TIME),net.getField(FieldType.TIME).getUnits())+"\n");
        buf.write("Hydraulic Timestep ................ "+ Utilities.clocktime(net.Hstep)+"\n");
        buf.write(String.format(getText("FMT27"),net.Hacc)+"\n");
        buf.write(String.format(getText("FMT27a"),net.CheckFreq)+"\n");
        buf.write(String.format(getText("FMT27b"),net.MaxCheck)+"\n");
        buf.write(String.format(getText("FMT27c"),net.DampLimit)+"\n");
        buf.write(String.format(getText("FMT28"),net.MaxIter)+"\n");


        if (net.Qualflag == QualType.NONE || net.Dur == 0.0)
            buf.write(getText("FMT29")+"\n");
        else if (net.Qualflag == QualType.CHEM)
            buf.write(String.format(getText("FMT30"),net.ChemName)+"\n");
        else if (net.Qualflag == QualType.TRACE)
            buf.write(String.format(getText("FMT31"),net.getNode(net.TraceNode).getId())+"\n");
        else if (net.Qualflag == QualType.AGE)
            buf.write(getText("FMT32")+"\n");

        if (net.Qualflag != QualType.NONE && net.Dur > 0)
        {
            //buf.write(String.format(getText("FMT33"),(float)net.Qstep/60.0)+"\n");
            buf.write("Water Quality Time Step ..........."+Utilities.clocktime(net.Qstep)+"\n");
            buf.write(String.format(getText("FMT34"),net.Ctol*net.getUcf(FieldType.QUALITY),
                    net.getField(FieldType.QUALITY).getUnits())+"\n");
        }

        buf.write(String.format(getText("FMT36"),net.SpGrav)+"\n");
        buf.write(String.format(getText("FMT37a"),net.Viscos/ Constants.VISCOS)+"\n");
        buf.write(String.format(getText("FMT37b"),net.Diffus/ Constants.DIFFUS)+"\n");
        buf.write(String.format(getText("FMT38"),net.Dmult)+"\n");
        buf.write(String.format(getText("FMT39"),net.Dur*net.getUcf(FieldType.TIME),
                    net.getField(FieldType.TIME).getUnits())+"\n");

        //if (Rptflag)
        //{
        //    buf.write(getText("FMT40"));
        //
        //    if (net.Nodeflag == ReportFlag.FALSE) buf.write(getText("FMT41")+"\n");
        //    if (net.Nodeflag == ReportFlag.TRUE)  buf.write(getText("FMT42")+"\n");
        //    if (net.Nodeflag == ReportFlag.SOME) buf.write(getText("FMT43")+"\n");
        //    writelimits(FieldType.DEMAND,FieldType.QUALITY);
        //    if (net.Linkflag == ReportFlag.FALSE)  buf.write(getText("FMT44")+"\n");
        //    if (net.Linkflag == ReportFlag.TRUE)  buf.write(getText("FMT45")+"\n");
        //    if (net.Linkflag == ReportFlag.SOME)  buf.write(getText("FMT46")+"\n");
        //    writelimits(FieldType.DIAM,FieldType.HEADLOSS);
        //}
        buf.write("\n");
        }
        catch (IOException e){}
    }

    // Writes hydraulic status report for solution found at current time period to report file
    void  writehydstat(int iter, double relerr)
    {
        int    i,n;
        StatType   newstat;
        String   atime;

        atime = Utilities.clocktime(hyd.Htime);
        if (iter > 0)
        {
            if (relerr <= net.Hacc)
                writeline(String.format(getText("FMT58"),atime,iter));
            else
                writeline(String.format(getText("FMT59"),atime,iter,relerr));
        }


        for (i=1; i<=net.getMaxTanks(); i++)
        {
            n = net.getTank(i).getNode();
            if (Math.abs(hyd.D[n]) < 0.001) newstat = StatType.CLOSED;
            else if (hyd.D[n] >  0.0)  newstat = StatType.FILLING;
            else if (hyd.D[n] <  0.0)  newstat = StatType.EMPTYING;
            else newstat = hyd.OldStat[net.getMaxLinks()+i];
            if (newstat != hyd.OldStat[net.getMaxLinks()+i])
            {
                if (net.getTank(i).getArea() > 0.0)
                    writeline(String.format(getText("FMT50"),
                            atime,
                            net.getNode(n).getId(),
                            EnumVariables.StatTxt[newstat.ordinal()],
                            (hyd.H[n]-net.getNode(n).getElevation())*net.getUcf(FieldType.HEAD),
                            net.getField(FieldType.HEAD).getUnits()));
                else
                    writeline(String.format(getText("FMT51"),
                            atime,
                            net.getNode(n).getId(),
                            EnumVariables.StatTxt[newstat.ordinal()]));

                hyd.OldStat[net.getMaxLinks()+i] = newstat;
            }
        }

        for (i=1; i<=net.getMaxLinks(); i++)
        {
            Link link = net.getLink(i);
            if (hyd.S[i] != hyd.OldStat[i])
            {
                if (hyd.Htime == 0)
                     writeline(String.format(getText("FMT52"),
                             atime,EnumVariables.LinkTxt[link.getType().ordinal()],link.getId(),
                            EnumVariables.StatTxt[hyd.S[i].ordinal()]));
                else
                   writeline(String.format(getText("FMT53"),
                           atime,EnumVariables.LinkTxt[link.getType().ordinal()],link.getId(),
                        EnumVariables.StatTxt[hyd.OldStat[i].ordinal()],
                           EnumVariables.StatTxt[hyd.S[i].ordinal()]));
                hyd.OldStat[i] = hyd.S[i];
            }
        }
        writeline(" ");
    }



    void  writehydstat2(BufferedWriter buf,int iter, double relerr)
    {
        try{
        int    i,n;
        StatType   newstat;
        String   atime;

        atime = Utilities.clocktime(hyd.Htime);
        if (iter > 0)
        {
            if (relerr <= net.Hacc)
                buf.write(String.format(getText("FMT58"),atime,iter)+"\n");
            else
                buf.write(String.format(getText("FMT59"),atime,iter,relerr)+"\n");
        }


        for (i=1; i<=net.getMaxTanks(); i++)
        {
            n = net.getTank(i).getNode();
            if (Math.abs(hyd.D[n]) < 0.001) newstat = StatType.CLOSED;
            else if (hyd.D[n] >  0.0)  newstat = StatType.FILLING;
            else if (hyd.D[n] <  0.0)  newstat = StatType.EMPTYING;
            else newstat = hyd.OldStat[net.getMaxLinks()+i];
            if (newstat != hyd.OldStat[net.getMaxLinks()+i])
            {
                if (net.getTank(i).getArea() > 0.0)
                    buf.write(String.format(getText("FMT50"),
                            atime,
                            net.getNode(n).getId(),
                            EnumVariables.StatTxt[newstat.ordinal()],
                            (hyd.H[n]-net.getNode(n).getElevation())*net.getUcf(FieldType.HEAD),
                            net.getField(FieldType.HEAD).getUnits())+"\n");
                else
                    buf.write(String.format(getText("FMT51"),
                            atime,
                            net.getNode(n).getId(),
                            EnumVariables.StatTxt[newstat.ordinal()])+"\n");

                hyd.OldStat[net.getMaxLinks()+i] = newstat;
            }
        }

        for (i=1; i<=net.getMaxLinks(); i++)
        {
            Link link = net.getLink(i);
            if (hyd.S[i] != hyd.OldStat[i])
            {
                if (hyd.Htime == 0)
                     buf.write(String.format(getText("FMT52"),
                             atime,EnumVariables.LinkTxt[link.getType().ordinal()],link.getId(),
                            EnumVariables.StatTxt[hyd.S[i].ordinal()])+"\n");
                else
                   buf.write(String.format(getText("FMT53"),
                           atime,EnumVariables.LinkTxt[link.getType().ordinal()],link.getId(),
                        EnumVariables.StatTxt[hyd.OldStat[i].ordinal()],
                           EnumVariables.StatTxt[hyd.S[i].ordinal()])+"\n");
                hyd.OldStat[i] = hyd.S[i];
            }
        }
        //buf.write("\n");
        }
        catch (IOException e){}
    }

    //  writes energy usage report to report file
    void  writeenergy()
    {
        int    j;
        double csum;

        if (net.getSections(SectType._PUMPS) == 0) return;
        writeline(" ");
        writeheader(HdrType.ENERHDR,false);
        csum = 0.0;
        for (j=1; j<=net.getSections(SectType._PUMPS); j++)
        {
            Pump pump = net.getPump(j);

            csum += pump.getEnergy(5);
            if (LineNum == (long)net.PageSize) writeheader(HdrType.ENERHDR,true);
            writeline(
                    String.format("%-8s  %6.2f %6.2f %9.2f %9.2f %9.2f %9.2f",
                    net.getLink(pump.getLink()).getId(),pump.getEnergy(0),pump.getEnergy(1),
                    pump.getEnergy(2),pump.getEnergy(3),pump.getEnergy(4),
                    pump.getEnergy(5))
            );
        }

        writeline(fillstr('-',63));


        writeline(String.format(getText("FMT74"),"",net.Emax*net.Dcost));
        writeline(String.format(getText("FMT75"),"",csum+net.Emax*net.Dcost));
        writeline(" ");
    }

    // Writes simulation results to report file
    int  writeresults(DataInputStream inStream,int Nperiods)
    {
        int    errcode = 0;
        float [][] x;
        int    j,m,n,nnv,nlv;


        if (RptFile == null) return(106);

        if (net.Nodeflag==ReportFlag.FALSE && net.Linkflag==ReportFlag.FALSE) return(errcode);
        nnv = 0;
        for (j=FieldType.ELEV.ordinal();
             j<=FieldType.QUALITY.ordinal(); j++) nnv += net.getField(j).isEnabled()?1:0;
        nlv = 0;
        for (j=FieldType.LENGTH.ordinal();
             j<=FieldType.FRICTION.ordinal(); j++) nlv += net.getField(j).isEnabled()?1:0;
        if (nnv == 0 && nlv == 0) return(errcode);


        m = Math.max( (FieldType.QUALITY.ordinal()-FieldType.DEMAND.ordinal()+1), (FieldType.FRICTION.ordinal()-FieldType.FLOW.ordinal()+1) );
        n = Math.max( (net.getMaxNodes()+1), (net.getMaxLinks()+1));

        x = new float[m][];
        for(int i =0;i<m;i++)
            x[i] = new float [n];

        try{
            inStream.skip(out.headerOffset);
        }
        catch (IOException e)
        {
            return 106;
        }

        hyd.Htime = net.Rstart;

        for (int np=1; np<=Nperiods; np++)
        {

            try{
                for (j=FieldType.DEMAND.ordinal(); j<=FieldType.QUALITY.ordinal(); j++)
                {
                    for(int ij = 1;ij<=net.getMaxNodes();ij++)
                        x[j-FieldType.DEMAND.ordinal()][ij] = inStream.readFloat();
                }

                if (nnv > 0 && net.Nodeflag != ReportFlag.FALSE)
                    writenodetable(x);

                for (j=FieldType.FLOW.ordinal(); j<=FieldType.FRICTION.ordinal(); j++)
                {
                    for(int ij = 1;ij<=net.getMaxLinks();ij++)
                        x[j-FieldType.FLOW.ordinal()][ij] = inStream.readFloat();
                }
            }
            catch (IOException e)
            {
                return 106;
            }

            if (nlv > 0 && net.Linkflag != ReportFlag.FALSE)
                writelinktable(x);

            hyd.Htime += net.Rstep;
        }


        return(errcode);
    }

    // writes node results for current time to report file
    void  writenodetable(float [][] x)
    {
        String   s,s1;
        double [] y = new double[Constants.MAXVAR];

        writeheader(HdrType.NODEHDR,false);

        for (int i=1; i<=net.getMaxNodes(); i++)
        {


            y[FieldType.ELEV.ordinal()] = net.getNode(i).getElevation()*net.getUcf(FieldType.ELEV);
            for (int j=FieldType.DEMAND.ordinal(); j<=FieldType.QUALITY.ordinal(); j++) y[j] = x[j-FieldType.DEMAND.ordinal()][i];


            if ((net.Nodeflag == ReportFlag.TRUE || net.getNode(i).getRptFlag()) && checklimits(y,FieldType.ELEV,FieldType.QUALITY))
            {

                if (LineNum == (long)net.PageSize) writeheader(HdrType.NODEHDR,true);
                s = String.format("%-15s",net.getNode(i).getId());
                for (int j=FieldType.ELEV.ordinal(); j<=FieldType.QUALITY.ordinal(); j++)
                {
                    if (net.getField(j).isEnabled())
                    {

                        if (Math.abs(y[j]) > 1.e6)
                            s1 = String.format("%10.2e", y[j]);
                        else
                            s1 = String.format("%10."+net.getField(j).getPrecision()+"f", y[j]);

                        s+=s1;
                    }
                }

                if (i > net.getSections(SectType._JUNCTIONS))
                {
                    s+="  ";
                    s+=EnumVariables.NodeTxt[getnodetype(i)];
                }

                writeline(s);
            }
        }
        writeline(" ");
    }

    // Writes link results for current time to report file
    void  writelinktable(float [][] x)
    {
        int    i,j;
        StatType k;
        String   s,s1;
        double []y = new double[Constants.MAXVAR];

        writeheader(HdrType.LINKHDR,false);

        for (i=1; i<=net.getMaxLinks(); i++)
        {

            y[FieldType.LENGTH.ordinal()] = net.getLink(i).getLenght()*net.getUcf(FieldType.LENGTH);
            y[FieldType.DIAM.ordinal()] = net.getLink(i).getDiameter()*net.getUcf(FieldType.DIAM);
            for (j=FieldType.FLOW.ordinal(); j<=FieldType.FRICTION.ordinal(); j++) y[j] = (x[j-FieldType.FLOW.ordinal()][i]);

            if ((net.Linkflag == ReportFlag.TRUE || net.getLink(i).isRptFlag()) && checklimits(y,FieldType.DIAM,FieldType.FRICTION))
            {
                if (LineNum == (long)net.PageSize) writeheader(HdrType.LINKHDR,true);

                s = String.format("%-15s", net.getLink(i).getId());

                for (j=FieldType.LENGTH.ordinal(); j<=FieldType.FRICTION.ordinal(); j++)
                {
                    if (net.getField(j).isEnabled())
                    {
                        if (j == FieldType.STATUS.ordinal())
                        {
                            if      (y[j] <= (double)StatType.CLOSED.ordinal()) k = StatType.CLOSED;
                            else if (y[j] == (double)StatType.ACTIVE.ordinal()) k = StatType.ACTIVE;
                            else
                                k = StatType.OPEN;

                            s1 = String.format("%10s", EnumVariables.StatTxt[k.ordinal()]);
                        }
                        else
                        {
                            if (Math.abs(y[j]) > 1.e6)
                                s1 = String.format("%10.2e",y[j]);
                            else
                                s1=String.format("%10." + net.getField(j).getPrecision()+ "f", y[j]);
                        }

                        s += s1;
                    }
                }

                if ( (j = net.getLink(i).getType().ordinal()) > LinkType.PIPE.ordinal())
                {
                    s+= "  ";
                    s+=EnumVariables.LinkTxt[j];
                }
                writeline(s);
            }
        }
        writeline(" ");
    }

    // Writes column headings for output report tables
    void  writeheader(HdrType type, boolean contin)
    {
        String   s,s1,s2,s3;
        int    i,n;


        if (Rptflag && LineNum+11 > (long)net.PageSize){
            while (LineNum < (long)net.PageSize)
                writeline(" ");
        }

        writeline("");


        if (type == HdrType.STATHDR)
        {
            s = getText("FMT49");
            if (contin)
                s+=EnumVariables.t_CONTINUED;
            writeline(s);
            s = fillstr('-',70);
            writeline(s);
        }


        if (type == HdrType.ENERHDR)
        {
            if (net.Unitsflag == UnitsType.SI)
                s1 = EnumVariables.t_perM3;
            else
                s1 = EnumVariables.t_perMGAL;
            s = getText("FMT71");
            if (contin)
                s+=EnumVariables.t_CONTINUED;
            writeline(s);
            writeline(fillstr('-',63));

            writeline(getText("FMT72"));
            writeline(String.format(getText("FMT73"), s1));
            writeline(fillstr('-',63));
        }


        if (type == HdrType.NODEHDR)
        {
            if (net.Tstatflag == TstatType.RANGE)
                s = String.format(getText("FMT76"),EnumVariables.t_DIFFER);
            else if (net.Tstatflag != TstatType.SERIES)
                s = String.format(getText("FMT76"),EnumVariables.TstatTxt[net.Tstatflag.ordinal()]);
            else if (net.Dur == 0)
                s = getText("FMT77");
            else
                s = String.format(getText("FMT78"),Utilities.clocktime(hyd.Htime));
            if (contin) s+=EnumVariables.t_CONTINUED;
            writeline(s);
            n = 15;
            s2 = String.format("%15s","");
            s = EnumVariables.t_NODEID;
            s3 =String.format("%-15s",s);
            for (i=FieldType.ELEV.ordinal(); i<FieldType.QUALITY.ordinal(); i++)
                if (net.getField(i).isEnabled())
                {
                    n += 10;
                    s = String.format("%10s",net.getField(i).getName());
                    s2+=s;
                    s = String.format("%10s",net.getField(i).getUnits());
                    s3+=s;
                }

            if (net.getField(FieldType.QUALITY).isEnabled())
            {
                n += 10;
                s=String.format("%10s",net.ChemName);
                s2+=s;
                s=String.format("%10s",net.ChemUnits);
                s3+=s;
            }

            s1 = fillstr('-',n);
            writeline(s1);
            writeline(s2);
            writeline(s3);
            writeline(s1);
        }

        if (type == HdrType.LINKHDR)
        {
            if      (net.Tstatflag == TstatType.RANGE)  s = String.format(getText("FMT79"),EnumVariables.t_DIFFER);
            else if (net.Tstatflag != TstatType.SERIES) s = String.format(getText("FMT79"),EnumVariables.TstatTxt[net.Tstatflag.ordinal()]);
            else if (net.Dur == 0)
                s=getText("FMT80");
            else
                s=String.format(getText("FMT81"),Utilities.clocktime(hyd.Htime));
            if (contin)
                s = EnumVariables.t_CONTINUED;
            writeline(s);
            n = 15;
            s2 = String.format("%15s","");
            s=EnumVariables.t_LINKID;
            s3=String.format("%-15s",s);
            for (i=FieldType.LENGTH.ordinal(); i<=FieldType.FRICTION.ordinal(); i++) if (net.getField(i).isEnabled() == true)
            {
                n += 10;

                s2+=String.format("%10s",net.getField(i).getName());
                s3+=String.format("%10s",net.getField(i).getUnits());
            }
            s1 = fillstr('-',n);
            writeline(s1);
            writeline(s2);
            writeline(s3);
            writeline(s1);
        }
    }



    // writes a line of output to report file
    public void writeline(String s)
    {
        if (RptFile == null) return;
        if (Rptflag)
        {
            if (this.LineNum == (long) net.PageSize)
            {
                PageNum++;
                write(String.format(getText("FMT82"), PageNum,net.getTitleText().get(0)));
                LineNum = 3;
            }
        }

        write("\n  "+s);
        LineNum++;
    }

    // Writes out convergence status of hydraulic solution
    void  writerelerr(int iter, double relerr)
    {
        if (iter == 0){
            writeline(String.format(textBundle.getString("FMT64"),Utilities.clocktime(hyd.Htime)));
        }
        else{
            writeline(String.format(textBundle.getString("FMT65"),iter,relerr));
        }
    }

    // writes change in link status to output report
    void  writestatchange(int k, StatType s1, StatType s2)
    {
        StatType    j1,j2;
        double setting;
        Link link = net.getLink(k);
        if (s1 == s2)
        {
            setting = hyd.K[k];

            switch (net.getLink(k).getType())
            {
                case PRV:
                case PSV:
                case PBV: setting *= net.getUcf(FieldType.PRESSURE); break;
                case FCV: setting *= net.getUcf(FieldType.FLOW);
            }
            writeline(String.format(getText("FMT56"),
                    EnumVariables.LinkTxt[link.getType().ordinal()],
                    link.getId(),
                    setting));
            return;
        }

        if      (s1 == StatType.ACTIVE) j1 = StatType.ACTIVE;
        else if (s1.ordinal() <= StatType.CLOSED.ordinal()) j1 = StatType.CLOSED;
        else                   j1 = StatType.OPEN;
        if      (s2 == StatType.ACTIVE) j2 = StatType.ACTIVE;
        else if (s2.ordinal() <= StatType.CLOSED.ordinal()) j2 = StatType.CLOSED;
        else                   j2 = StatType.OPEN;
        if (j1 != j2)
        {
            writeline(String.format(getText("FMT57"),EnumVariables.LinkTxt[net.getLink(k).getType().ordinal()],
                    net.getLink(k).getId(),EnumVariables.StatTxt[j1.ordinal()],EnumVariables.StatTxt[j2.ordinal()]));
        }
    }

    // writes control action taken to status report
    void writecontrolaction(int k, int i)
    {
        int n;
        String Msg="";
        switch (net.getControl(i).getType())
        {

            case LOWLEVEL:
            case HILEVEL:
                n = net.getControl(i).getNode();
                Msg = String.format(getText("FMT54"),Utilities.clocktime(hyd.Htime),
                        EnumVariables.LinkTxt[net.getLink(k).getType().ordinal()],
                        net.getLink(k).getId(),EnumVariables.NodeTxt[getnodetype(n)],net.getNode(n).getId());
                break;
            case TIMER:
            case TIMEOFDAY:
                Msg = String.format(getText("FMT55"),Utilities.clocktime(hyd.Htime),
                        EnumVariables.LinkTxt[net.getLink(k).getType().ordinal()],
                        net.getLink(k).getId());
                break;
            default: return;
        }
        writeline(Msg);
    }

    // writes rule action taken to status report
    void writeruleaction(int k, String ruleID)
    {
        writeline(String.format(getText("FMT63"),
                Utilities.clocktime(hyd.Htime),EnumVariables.LinkTxt[net.getLink(k).getType().ordinal()],
                net.getLink(k).getId(),ruleID));
    }

    // writes hydraulic warning message to report file
    int  writehydwarn(int iter, double relerr)
    {
        int  i,j;
        char flag = 0;
        StatType s;

        if (iter > net.MaxIter && relerr <= net.Hacc)
        {
            if (net.Messageflag) writeline(String.format(errorBundle.getString("WARN02"),Utilities.clocktime(hyd.Htime)));
            flag = 2;
        }

        for (i=1; i<=net.getSections(SectType._JUNCTIONS); i++)
        {
            if (hyd.H[i] < net.getNode(i).getElevation() && hyd.D[i] > 0.0){
                if (net.Messageflag) writeline(String.format(errorBundle.getString("WARN06"),Utilities.clocktime(hyd.Htime)));
                flag = 6;
                break;
            }
        }

        for (i=1; i<=net.getSections(SectType._VALVES); i++)
        {
            j = net.getValve(i).getLink();
            if (hyd.S[j].ordinal() >= StatType.XFCV.ordinal())
            {
                if (net.Messageflag) writeline(String.format(errorBundle.getString("WARN05"),
                        EnumVariables.LinkTxt[net.getLink(j).getType().ordinal()],
                        net.getLink(j).getId(),EnumVariables.StatTxt[hyd.S[j].ordinal()],
                        Utilities.clocktime(hyd.Htime)));
                flag = 5;
            }
        }

        for (i=1; i<=net.getSections(SectType._PUMPS); i++)
        {
            j = net.getPump(i).getLink();
            s = hyd.S[j];
            if (hyd.S[j].ordinal() >= StatType.OPEN.ordinal())
            {
                if (hyd.Q[j] > hyd.K[j]*net.getPump(i).getQMax()) s = StatType.XFLOW;
                if (hyd.Q[j] < 0.0) s = StatType.XHEAD;
            }
            if (s == StatType.XHEAD || s == StatType.XFLOW)
            {
                if (net.Messageflag) writeline(String.format(errorBundle.getString("WARN04"),net.getLink(j).getId(),
                        EnumVariables.StatTxt[s.ordinal()],
                        Utilities.clocktime(hyd.Htime)));
                flag = 4;
            }
        }

        if (iter > net.MaxIter && relerr > net.Hacc)
        {
            String Msg=String.format(errorBundle.getString("WARN01"), Utilities.clocktime(hyd.Htime));
            if (net.ExtraIter == -1) Msg+=EnumVariables.t_HALTED;
            if (net.Messageflag) writeline(Msg);
            flag = 1;
        }

        if (flag > 0)
        {
            disconnected();
            epanet.setWarnflag(flag);
        }
        return(flag);
    }

    // writes hydraulic warning message to report file
    int  writehydwarn2(BufferedWriter buf,int iter, double relerr)
    {
        int  i,j;
        char flag = 0;
        StatType s;

        try{
        if (iter > net.MaxIter && relerr <= net.Hacc)
        {
            if (net.Messageflag) buf.write(String.format(errorBundle.getString("WARN02"),Utilities.clocktime(hyd.Htime))+"\n");
            flag = 2;
        }

        for (i=1; i<=net.getSections(SectType._JUNCTIONS); i++)
        {
            if (hyd.H[i] < net.getNode(i).getElevation() && hyd.D[i] > 0.0){
                if (net.Messageflag) buf.write(String.format(errorBundle.getString("WARN06"),Utilities.clocktime(hyd.Htime))+"\n");
                flag = 6;
                break;
            }
        }

        for (i=1; i<=net.getSections(SectType._VALVES); i++)
        {
            j = net.getValve(i).getLink();
            if (hyd.S[j].ordinal() >= StatType.XFCV.ordinal())
            {
                if (net.Messageflag) buf.write(String.format(errorBundle.getString("WARN05"),
                        EnumVariables.LinkTxt[net.getLink(j).getType().ordinal()],
                        net.getLink(j).getId(),EnumVariables.StatTxt[hyd.S[j].ordinal()],
                        Utilities.clocktime(hyd.Htime))+"\n");
                flag = 5;
            }
        }

        for (i=1; i<=net.getSections(SectType._PUMPS); i++)
        {
            j = net.getPump(i).getLink();
            s = hyd.S[j];
            if (hyd.S[j].ordinal() >= StatType.OPEN.ordinal())
            {
                if (hyd.Q[j] > hyd.K[j]*net.getPump(i).getQMax()) s = StatType.XFLOW;
                if (hyd.Q[j] < 0.0) s = StatType.XHEAD;
            }
            if (s == StatType.XHEAD || s == StatType.XFLOW)
            {
                if (net.Messageflag) buf.write(String.format(errorBundle.getString("WARN04"),net.getLink(j).getId(),
                        EnumVariables.StatTxt[s.ordinal()],
                        Utilities.clocktime(hyd.Htime))+"\n");
                flag = 4;
            }
        }

        if (iter > net.MaxIter && relerr > net.Hacc)
        {
            String Msg=String.format(errorBundle.getString("WARN01"), Utilities.clocktime(hyd.Htime));
            if (net.ExtraIter == -1) Msg+=EnumVariables.t_HALTED;
            if (net.Messageflag) buf.write(Msg+"\n");
            flag = 1;
        }

        if (flag > 0)
        {
            disconnected2(buf);
            epanet.setWarnflag(flag);
        }
        }
        catch (IOException e){}
        return(flag);
    }

    // Outputs status & checks connectivity when network hydraulic equations cannot be solved.
    void  writehyderr(int errnode)
    {
        if (net.Messageflag) writeline(String.format(getText("FMT62"),
                Utilities.clocktime(hyd.Htime),net.getNode(errnode).getId()));
        writehydstat(0,0);
        disconnected();
    }

    //Tests current hydraulic solution to see if any closed links have caused the network to become disconnected.
    int  disconnected()
    {
        int  i=0, j=0;
        int  count, mcount;
        int  errcode = 0;
        int  [] nodelist = new int[net.getMaxNodes()+1];
        char [] marked = new char[net.getMaxNodes()+1];


        for (i=1; i<=net.getMaxTanks(); i++)
        {
            j = net.getSections(SectType._JUNCTIONS) + i;
            nodelist[i] = j;
            marked[j] = 1;
        }

        mcount = net.getMaxTanks();
        for (i=1; i<=net.getSections(SectType._JUNCTIONS); i++)
        {
            if (hyd.D[i] < 0.0)
            {
                mcount++;
                nodelist[mcount] = i;
                marked[j] = 1;
            }
        }

        marknodes(mcount,nodelist,marked);
        j = 0;
        count = 0;
        for (i=1; i<=net.getSections(SectType._JUNCTIONS); i++)
        {
            if (marked[i]==0 && hyd.D[i] != 0.0)
            {
                count++;
                if (count <= Constants.MAXCOUNT && net.Messageflag)
                {
                    writeline(String.format(errorBundle.getString("WARN03a"),net.getNode(i).getId(),Utilities.clocktime(hyd.Htime)));
                }
                j = i;
            }
        }


        if (count > 0 && net.Messageflag)
        {
            if (count > Constants.MAXCOUNT)
            {
                writeline(String.format(errorBundle.getString("WARN03b"), count-Constants.MAXCOUNT, Utilities.clocktime(hyd.Htime)));
            }
            getclosedlink(j,marked);
        }


        return(count);
    }

    int  disconnected2(BufferedWriter buff)
    {
        int  i=0, j=0;
        int  count=0, mcount;
        int  errcode = 0;
        int  [] nodelist = new int[net.getMaxNodes()+1];
        char [] marked = new char[net.getMaxNodes()+1];

        try{

        for (i=1; i<=net.getMaxTanks(); i++)
        {
            j = net.getSections(SectType._JUNCTIONS) + i;
            nodelist[i] = j;
            marked[j] = 1;
        }

        mcount = net.getMaxTanks();
        for (i=1; i<=net.getSections(SectType._JUNCTIONS); i++)
        {
            if (hyd.D[i] < 0.0)
            {
                mcount++;
                nodelist[mcount] = i;
                marked[j] = 1;
            }
        }

        marknodes(mcount,nodelist,marked);
        j = 0;
        count = 0;
        for (i=1; i<=net.getSections(SectType._JUNCTIONS); i++)
        {
            if (marked[i]==0 && hyd.D[i] != 0.0)
            {
                count++;
                if (count <= Constants.MAXCOUNT && net.Messageflag)
                {
                    buff.write(String.format(errorBundle.getString("WARN03a"),net.getNode(i).getId(),Utilities.clocktime(hyd.Htime)) + "\n");
                }
                j = i;
            }
        }


        if (count > 0 && net.Messageflag)
        {
            if (count > Constants.MAXCOUNT)
            {
                buff.write(String.format(errorBundle.getString("WARN03b"), count-Constants.MAXCOUNT, Utilities.clocktime(hyd.Htime)) + "\n");
            }
            getclosedlink(j,marked);
        }

        }
        catch (IOException e){}

        return(count);
    }

    //Marks all junction nodes connected to tanks.
    void  marknodes(int m, int [] nodelist, char [] marked)
    {
        int   i, j, k, n;

        n = 1;
        while (n <= m )
        {

            i = nodelist[n];
            for(AdjListItem alink : smat.Adjlist.get(i))
            {

                k = alink.getLink();
                j = alink.getNode();
                if (marked[j]!=0) continue;

                switch (net.getLink(k).getType())
                {
                    case CV:
                    case PRV:
                    case PSV: if (j == net.getLink(k).getN1()) continue;
                }

                if (hyd.S[k].ordinal() > StatType.CLOSED.ordinal())
                {
                    marked[j] = 1;
                    m++;
                    nodelist[m] = j;
                }
            }
            n++;
        }
    }

    // Determines if a closed link connects to junction i.
    void getclosedlink(int i, char [] marked)
    {
        int j,k;
        marked[i] = 2;
        for(AdjListItem alink : smat.Adjlist.get(i))
        {
            k = alink.getLink();
            j = alink.getNode();
            if (marked[j] == 2) continue;
            if (marked[j] == 1)
            {
                writeline(String.format(errorBundle.getString("WARN03c"), net.getLink(k).getId()));
                return;
            }
            else getclosedlink(j,marked);
        }
    }

    // Writes reporting criteria to output report
    void  writelimits(FieldType j1, FieldType j2)
    {
        int  j;
        for (j=j1.ordinal(); j<=j2.ordinal(); j++)
        {
            Field field = net.getField(j);
            if (field.getRptLim(RangeType.LOW.ordinal()) < Constants.BIG)
            {
                writeline(String.format(getText("FMT47"),field.getName()
                        ,field.getRptLim(RangeType.LOW.ordinal()),field.getUnits()));
            }
            if (field.getRptLim(RangeType.HI.ordinal()) > -Constants.BIG)
            {
                writeline(String.format(getText("FMT48"),field.getName()
                        ,field.getRptLim(RangeType.HI.ordinal()),field.getUnits()));
            }
        }
    }

    // checks if output reporting criteria is met
    boolean  checklimits(double [] y, FieldType j1, FieldType j2)
    {
        int j;
        for (j=j1.ordinal(); j<=j2.ordinal(); j++)
        {
            if (y[j] > net.getField(j).getRptLim(RangeType.LOW.ordinal())
                    ||  y[j] < net.getField(j).getRptLim(RangeType.HI.ordinal())) return false ;
        }
        return true;
    }

    //Fills n bytes of s to character ch.
    String fillstr(char ch, int n)
    {
        String ret="";
        for (int i=0; i<=n; i++) ret += ch;
        return ret;
    }

    // Determines type of node with index i
    int  getnodetype(int i)
    {
        if (i <= net.getSections(SectType._JUNCTIONS))
            return(0);
        if (net.getTank(i-net.getSections(SectType._JUNCTIONS)).getArea() == 0.0)
            return(1);
        return(2);
    }



    void  errmsg(int errcode)
    {
        if (errcode == 309){
            Utilities.writecon("\n  ");
            Utilities.writecon(Utilities.geterrmsg(errcode));
        }
        else if (RptFile != null && net.Messageflag &&  errcode>=101)
        {
            writeline(Utilities.geterrmsg(errcode));
        }
    }

    private void write(String s)
    {
        try {
            RptFile.write(s);
        } catch (IOException e) {
            Fprinterr = true;
        }
    }

    void writetime(String fmt){
        writeline(String.format(fmt,(new Date())));
    }

}
