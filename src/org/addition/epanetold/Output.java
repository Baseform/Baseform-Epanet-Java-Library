package org.addition.epanetold;


import java.io.*;
import java.util.Arrays;

import org.addition.epanetold.Types.EnumVariables.*;
import org.addition.epanetold.Types.Link;
import org.addition.epanetold.Types.Pump;

public class Output {
    DataOutputStream mOutFile;

    Epanet epanet;

    Quality qual;
    Network net;
    Hydraulic hyd;
    long headerOffset = 0;

    Output(Epanet epanet){
        this.epanet = epanet;
    }

    public void loadDependencies(){
        net = epanet.getNetwork();
        hyd = epanet.getHydraulicsSolver();
        qual = epanet.getQuality();

    }

    int open(File f)
    {
        int errCode = 0;
        try {
            mOutFile = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));


            errCode = Utilities.ERRCODE(errCode,savenetdata());
            errCode = Utilities.ERRCODE(errCode,saveenergy());

            mOutFile.close();

            headerOffset = f.length();

            // this isn't very nice..
            mOutFile = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f,true)));

        } catch (IOException e) {
            return 304;
        }

        return(errCode);
    }

    int close()
    {
        try {
            mOutFile.close();
        } catch (IOException e) {
            return 308;
        }
        return 0;
    }
    void writeString(DataOutputStream stream,String str, int size )
    {
        char[] buff = null;
        if(str!=null)
            buff = str.toCharArray();
        for(int i = 0;i<size;i++)
        {
            try {
                if(buff!=null && i<buff.length)
                    stream.writeByte(buff[i]);
                else
                    stream.writeByte('\0');
            } catch (IOException e) {
                return;
            }
        }
    }

    // saves input data in original units to binary output file using fixed-sized (4-byte) records
    int  savenetdata()
    {
        int   nmax;
        int   [] ibuf;
        float [] x;
        int   errcode = 0;

        int Nnodes = net.getMaxNodes();
        int Nlinks = net.getMaxLinks();
        int Ntanks = net.getMaxTanks();

        nmax =  Math.max(Math.max(Nnodes,Nlinks) + 1,15);

        ibuf = new int[nmax];
        x = new float [nmax];

        ibuf[0] = Constants.MAGICNUMBER;
        ibuf[1] = Constants.CODEVERSION;

        ibuf[2] = Nnodes;
        ibuf[3] = net.getMaxTanks();
        ibuf[4] = Nlinks;
        ibuf[5] = net.getSections(SectType._PUMPS);
        ibuf[6] = net.getSections(SectType._VALVES);
        ibuf[7] = net.Qualflag.ordinal();
        ibuf[8] = net.TraceNode;
        ibuf[9] = net.Flowflag.ordinal();
        ibuf[10] = net.Pressflag.ordinal();
        ibuf[11] = net.Tstatflag.ordinal();
        ibuf[12] = (int)net.Rstart;
        ibuf[13] = (int)net.Rstep;
        ibuf[14] = (int)net.Dur;

        try{
            for(int i = 0;i<15;i++)
                mOutFile.writeInt(ibuf[i]);

            for(int i = 0;i<3;i++)
                if(net.getTitleText()!=null && i<net.getTitleText().size())
                    writeString(mOutFile,net.getTitleText().get(i),Constants.MAXMSG+1);
                else
                    writeString(mOutFile,"",Constants.MAXMSG+1);

            writeString(mOutFile,epanet.mInpFile.getAbsolutePath(),Constants.MAXFNAME+1);
            writeString(mOutFile,net.Rpt2Fname,Constants.MAXFNAME+1);
            writeString(mOutFile,net.ChemName,Constants.MAXID+1);
            writeString(mOutFile,net.getField(FieldType.QUALITY).getUnits(),Constants.MAXID+1);
        }
        catch(IOException e)
        {
            return 308;
        }

        for (int i=1; i<=Nnodes; i++)
            writeString(mOutFile,net.getNode(i).getId(),Constants.MAXID+1);

        for (int i=1; i<=Nlinks; i++)
            writeString(mOutFile,net.getLink(i).getId(),Constants.MAXID+1);

        try{
            for (int i=1; i<=Nlinks; i++)
                mOutFile.writeInt(net.getLink(i).getN1());

            for (int i=1; i<=Nlinks; i++)
                mOutFile.writeInt(net.getLink(i).getN2());

            for (int i=1; i<=Nlinks; i++)
                mOutFile.writeInt(net.getLink(i).getType().ordinal());


            for (int i=1; i<=Ntanks; i++)
                mOutFile.writeInt(net.getTank(i).getNode());

            for (int i=1; i<=Ntanks; i++)
                mOutFile.writeFloat((float)net.getTank(i).getArea());

            for (int i=1; i<=Nnodes; i++)
                mOutFile.writeFloat((float)(net.getNode(i).getElevation()*net.getUcf(FieldType.ELEV)));

            for (int i=1; i<=Nlinks; i++)
                mOutFile.writeFloat((float)(net.getLink(i).getLenght()*net.getUcf(FieldType.ELEV)));

            for (int i=1; i<=Nlinks; i++)
            {
                if (net.getLink(i).getType() != LinkType.PUMP)
                    mOutFile.writeFloat((float)(net.getLink(i).getDiameter()*net.getUcf(FieldType.DIAM)));
                else
                    mOutFile.writeFloat(0.0f);
            }
        }
        catch (IOException e){
            return 308;
        }

        return(errcode);
    }


    // saves energy usage by each pump to OutFile in binary format.
    int  saveenergy() {
        int   j;
        int  index;
        float [] x = new float[6];
        double hdur,
                t;

        hdur = net.Dur / 3600.0;
        for (int i=1; i<=net.getSections(SectType._PUMPS); i++)
        {
            Pump pump = net.getPump(i);
            if (hdur == 0.0)
            {
                for (j=0; j<5; j++) x[j] = (float)pump.getEnergy(j);
                x[5] = (float)(pump.getEnergy(5)*24.0);
            }
            else
            {
                t = pump.getEnergy(0);
                x[0] = (float)(t/hdur);
                x[1] = 0.0f;
                x[2] = 0.0f;
                x[3] = 0.0f;
                x[4] = 0.0f;
                if (t > 0.0)
                {
                    x[1] = (float)(pump.getEnergy(1)/t);
                    x[2] = (float)(pump.getEnergy(2)/t);
                    x[3] = (float)(pump.getEnergy(3)/t);
                }
                x[4] = (float)pump.getEnergy(4);
                x[5] = (float)(pump.getEnergy(5)*24.0/hdur);
            }
            x[0] *= 100.0f;
            x[1] *= 100.0f;

            if (net.Unitsflag == UnitsType.SI) x[2] *= (float)(1000.0/Constants.LPSperCFS/3600.0);
            else                 x[2] *= (float)(1.0e6/Constants.GPMperCFS/60.0);
            for (j=0; j<6; j++) net.getPump(i).setEnergy(j,x[j]);
            index = pump.getLink();

            try {
                mOutFile.writeInt(index);
                for(int ij = 0;ij<6;ij++)
                    mOutFile.writeFloat(x[ij]);
            } catch (IOException e) {
                return 308;
            }

        }
        net.Emax = net.Emax*net.Dcost;
        x[0] = (float)net.Emax;

        try {
            mOutFile.writeFloat(x[0]);
        } catch (IOException e) {
            return 308;
        }

        return(0);
    }

    // writes simulation results to output file
    int  saveoutput()
    {
        int   j;
        int   errcode = 0;
        int   temperrcode = 0;
        float [] x = new float[Math.max(net.getMaxNodes(),net.getMaxLinks())+1];


        for (j=FieldType.DEMAND.ordinal(); j<=FieldType.QUALITY.ordinal(); j++)
            if((temperrcode =
                    nodeoutput(FieldType.values()[j],x,net.getUcf(FieldType.values()[j])))>errcode)
                errcode = temperrcode;

        for (j=FieldType.FLOW.ordinal(); j<=FieldType.FRICTION.ordinal(); j++)
            if((temperrcode =
                    linkoutput(FieldType.values()[j],x,net.getUcf(FieldType.values()[j])))>errcode)
                errcode = temperrcode;

        return(errcode);
    }

    // writes results for node variable j to output file
    int  nodeoutput(FieldType j, float [] x, double ucf)
    {
        int Nnodes = net.getMaxNodes();

        switch(j)
        {
            case DEMAND:    for (int i=1; i<=Nnodes; i++)
                x[i] = (float)(hyd.D[i]*ucf);
                break;
            case HEAD:      for (int i=1; i<=Nnodes; i++)
                x[i] = (float)(hyd.H[i]*ucf);
                break;
            case PRESSURE:  for (int i=1; i<=Nnodes; i++)
                x[i] = (float)((hyd.H[i] - net.getNode(i).getElevation())*ucf);
                break;
            case QUALITY:   for (int i=1; i<=Nnodes; i++)
                x[i] = (float)(qual.C[i]*ucf);
        }

        for(int i = 1;i<=Nnodes;i++)
            try {
                mOutFile.writeFloat(x[i]);
            } catch (IOException e) {
                return 308;
            }

        return(0);
    }


    int  linkoutput(FieldType j, float [] x, double ucf)
    {
        double a,h,q,f;

        switch(j)
        {
            case FLOW:      for (int i=1; i<=net.getMaxLinks(); i++)
                x[i] = (float)(hyd.Q[i]*ucf);
                break;
            case VELOCITY:  for (int i=1; i<=net.getMaxLinks(); i++)
            {
                if (net.getLink(i).getType() == LinkType.PUMP) x[i] = 0.0f;
                else
                {
                    q = Math.abs(hyd.Q[i]);
                    a = Constants.PI*Math.pow(net.getLink(i).getDiameter(),2)/4.0;
                    x[i] = (float)(q/a*ucf);
                }
            }
                break;
            case HEADLOSS:  for (int i=1; i<=net.getMaxLinks(); i++)
            {
                Link lk = net.getLink(i);
                if (hyd.S[i].ordinal() <= StatType.CLOSED.ordinal()) x[i] = 0.0f;
                else
                {
                    h = hyd.H[lk.getN1()] - hyd.H[lk.getN2()];
                    if (lk.getType() != LinkType.PUMP) h = Math.abs(h);
                    if (lk.getType().ordinal() <= LinkType.PIPE.ordinal())
                        x[i] = (float)(1000.0*h/lk.getLenght());
                    else
                        x[i] = (float)(h*ucf);
                }
            }
                break;
            case LINKQUAL:  for (int i=1; i<=net.getMaxLinks(); i++)
                x[i] = (float)(qual.avgqual(i)*ucf);
                break;
            case STATUS:    for (int i=1; i<=net.getMaxLinks(); i++)
                x[i] = (float)hyd.S[i].ordinal();
                break;
            case SETTING:   for (int i=1; i<=net.getMaxLinks(); i++)
            {
                if (hyd.K[i] != Constants.MISSING)
                    switch (net.getLink(i).getType())
                    {
                        case CV:
                        case PIPE: x[i] = (float)hyd.K[i];
                            break;
                        case PUMP: x[i] = (float)hyd.K[i];
                            break;
                        case PRV:
                        case PSV:
                        case PBV:  x[i] = (float)(hyd.K[i]*net.getUcf(FieldType.PRESSURE));
                            break;
                        case FCV:  x[i] = (float)(hyd.K[i]*net.getUcf(FieldType.FLOW));
                            break;
                        case TCV:  x[i] = (float)hyd.K[i];
                            break;
                        default:   x[i] = 0.0f;
                    }
                else x[i] = 0.0f;
            }
                break;
            case REACTRATE:
                if (net.Qualflag == QualType.NONE)
                    Arrays.fill(x,net.getMaxLinks()+1);
                else
                    for (int i=1; i<=net.getMaxLinks(); i++)
                        x[i] = (float)(qual.R[i]*ucf);
                break;
            case FRICTION:
                for (int i=1; i<=net.getMaxLinks(); i++)
                {
                    Link lk = net.getLink(i);
                    if (net.getLink(i).getType().ordinal() <= LinkType.PIPE.ordinal() && Math.abs(hyd.Q[i]) > Constants.TINY)
                    {
                        h = Math.abs(hyd.H[lk.getN1()] - hyd.H[lk.getN2()]);
                        f = 39.725*h*Math.pow(lk.getDiameter(), 5)/lk.getLenght()/(hyd.Q[i]*hyd.Q[i]);
                        x[i] = (float)f;
                    }
                    else x[i] = 0.0f;
                }
                break;
        }

        for(int i = 1;i<=net.getMaxLinks();i++)
            try {
                mOutFile.writeFloat(x[i]);
            } catch (IOException e) {
                return 308;
            }

        return(0);
    }

    // saves time series statistics, reaction rates & epilog to output file.
    int  savefinaloutput(DataInputStream inTmpStream)
    {
        int errcode = 0;
        int tmperrcode;

        if (net.Tstatflag != TstatType.SERIES && mOutFile != null)
        {
            float [] x = new float [Math.max(net.getMaxNodes(),net.getMaxLinks())+1];

            if( (tmperrcode = savetimestat(inTmpStream,x,HdrType.NODEHDR))>errcode)
                errcode = tmperrcode;

            if( (tmperrcode = savetimestat(inTmpStream,x,HdrType.LINKHDR))>errcode)
                errcode = tmperrcode;

            if (errcode!=0) qual.Nperiods = 1;

            /*try{
                //mOutFile.close();
            }
            catch (Exception ex){
                return 308;
            }*/

        }


        if (mOutFile != null)
        {
            if( (tmperrcode = savenetreacts(qual.Wbulk,qual.Wwall,qual.Wtank,qual.Wsource))>errcode)
                errcode = tmperrcode;

            if( (tmperrcode = saveepilog())>errcode)
                errcode = tmperrcode;

        }
        return(errcode);
    }

    // computes time series statistic for nodes or links and saves to normal output file.
    int  savetimestat(DataInputStream inTmpStream,float [] x, HdrType objtype)
    {
        int   n, n1, n2;
        int   i, j,  p, errcode = 0;
        long  startbyte, skipbytes;
        float [] stat1, stat2;
        float xx;

        int Nnodes = net.getMaxNodes();
        int Nlinks = net.getMaxLinks();

        if (objtype == HdrType.NODEHDR)
        {
            startbyte = 0;
            skipbytes = (Nnodes*(FieldType.QUALITY.ordinal()-FieldType.DEMAND.ordinal()) +
                    Nlinks*(FieldType.FRICTION.ordinal()-FieldType.FLOW.ordinal()+1))*(Float.SIZE/8);
            n = Nnodes;
            n1 = FieldType.DEMAND.ordinal();
            n2 = FieldType.QUALITY.ordinal();
        }
        else
        {
            startbyte = Nnodes*(FieldType.QUALITY.ordinal()-FieldType.DEMAND.ordinal()+1)*(Float.SIZE/8);
            skipbytes = (Nnodes*(FieldType.QUALITY.ordinal()-FieldType.DEMAND.ordinal()+1) +
                    Nlinks*(FieldType.FRICTION.ordinal()-FieldType.FLOW.ordinal()))*(Float.SIZE/8);
            n = Nlinks;
            n1 = FieldType.FLOW.ordinal();
            n2 = FieldType.FRICTION.ordinal();
        }

        stat1 = new float [n+1];
        stat2 = new float [n+1];

        try{
            inTmpStream.skipBytes((int)startbyte);
        }
        catch(IOException e){
            return 308;
        }

        for (j=n1; j<=n2; j++)
        {
            if (net.Tstatflag != TstatType.AVG)
                for (i=1; i<=n; i++)
                {
                    stat1[i] = (float)-Constants.MISSING;
                    stat2[i] = (float) Constants.MISSING;
                }

            try{
                inTmpStream.reset();
                inTmpStream.skipBytes((int)(startbyte + (j - n1) * n * (4)));

                for (p=1; p<=qual.Nperiods; p++)
                {

                    for(int xi = 1;xi<=n;xi++)
                        x[xi] = inTmpStream.readFloat();

                    for (i=1; i<=n; i++)
                    {
                        xx = x[i];
                        if (objtype == HdrType.LINKHDR)
                        {
                            if (j == FieldType.FLOW.ordinal())
                                xx = Math.abs(xx);

                            if (j == FieldType.STATUS.ordinal())
                            {
                                if (xx >= StatType.OPEN.ordinal())
                                    xx = 1.0f;
                                else
                                    xx = 0.0f;
                            }
                        }
                        if (net.Tstatflag == TstatType.AVG)  stat1[i] += xx;
                        else
                        {
                            stat1[i] = Math.min(stat1[i], xx);
                            stat2[i] = Math.max(stat2[i], xx);
                        }
                    }


                    if (p < qual.Nperiods)
                        inTmpStream.skipBytes((int)skipbytes);

                }


                switch (net.Tstatflag)
                {
                    case AVG:   for (i=1; i<=n; i++) x[i] = stat1[i]/(float)qual.Nperiods;
                        break;
                    case MIN:   for (i=1; i<=n; i++) x[i] = stat1[i];
                        break;
                    case MAX:   for (i=1; i<=n; i++) x[i] = stat2[i];
                        break;
                    case RANGE: for (i=1; i<=n; i++) x[i] = stat2[i] - stat1[i];
                        break;
                }

                if (objtype == HdrType.LINKHDR && FieldType.values()[j] == FieldType.STATUS)
                {
                    for (i=1; i<=n; i++)
                    {
                        if (x[i] < 0.5f) x[i] = (float)StatType.CLOSED.ordinal();
                        else             x[i] = (float)StatType.OPEN.ordinal();
                    }
                }

                for(int xi = 1;xi<=n;xi++)
                    mOutFile.writeFloat(x[xi]);

                if (objtype == HdrType.NODEHDR) switch (FieldType.values()[j])
                {
                    case DEMAND:  for (i=1; i<=n; i++) hyd.D[i] = x[i]/net.getUcf(FieldType.DEMAND);
                        break;
                    case HEAD:    for (i=1; i<=n; i++) hyd.H[i] = x[i]/net.getUcf(FieldType.HEAD);
                        break;
                    case QUALITY: for (i=1; i<=n; i++) qual.C[i] = x[i]/net.getUcf(FieldType.QUALITY);
                        break;
                }
                else
                if (FieldType.values()[j] == FieldType.FLOW)
                    for (i=1; i<=n; i++) hyd.Q[i] = x[i]/net.getUcf(FieldType.FLOW);

            }
            catch(IOException e)
            {
                return 308;
            }
        }
        return 0;
    }

    // Writes average network-wide reaction rates (in mass/hr) to binary output file.
    int  savenetreacts(double wbulk, double wwall, double wtank, double wsource)
    {
        double t;
        float [] w = new float [4];
        if (net.Dur > 0) t = (double)net.Dur/3600.;
        else t = 1.;
        w[0] = (float)(wbulk/t);
        w[1] = (float)(wwall/t);
        w[2] = (float)(wtank/t);
        w[3] = (float)(wsource/t);
        for(int i = 0;i<4;i++)
            try {
                mOutFile.writeFloat(w[i]);
            } catch (IOException e) {
                return 308;
            }
        return 0;
    }

    // Writes Nperiods, Warnflag, & Magic Number to end of binary output file.
    int  saveepilog()
    {
        try{
            mOutFile.writeInt(qual.Nperiods);
            mOutFile.writeInt(epanet.Warnflag);
            mOutFile.writeInt(Constants.MAGICNUMBER);
        }
        catch(IOException e){
            return 308;
        }

        return 0;
    }


    public long getHeaderOffset() {
        return headerOffset;
    }
}
