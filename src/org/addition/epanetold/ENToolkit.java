package org.addition.epanetold;


import org.addition.epanetold.Types.Demand;
import org.addition.epanetold.Types.EnumVariables.*;
import org.addition.epanetold.Types.Link;
import org.addition.epanetold.Types.Node;
import org.addition.epanetold.Types.Source;

import java.io.*;

public class ENToolkit {

    public static final int EN_ELEVATION   =0;    /* Node parameters */
    public static final int EN_BASEDEMAND  =1;
    public static final int EN_PATTERN     =2;
    public static final int EN_EMITTER     =3;
    public static final int EN_INITQUAL    =4;
    public static final int EN_SOURCEQUAL  =5;
    public static final int EN_SOURCEPAT   =6;
    public static final int EN_SOURCETYPE  =7;
    public static final int EN_TANKLEVEL   =8;
    public static final int EN_DEMAND      =9;
    public static final int EN_HEAD        =10;
    public static final int EN_PRESSURE    =11;
    public static final int EN_QUALITY     =12;
    public static final int EN_SOURCEMASS  =13;
    public static final int EN_INITVOLUME  =14;
    public static final int EN_MIXMODEL    =15;
    public static final int EN_MIXZONEVOL  =16;

    public static final int EN_TANKDIAM    =17;
    public static final int EN_MINVOLUME   =18;
    public static final int EN_VOLCURVE    =19;
    public static final int EN_MINLEVEL    =20;
    public static final int EN_MAXLEVEL    =21;
    public static final int EN_MIXFRACTION =22;
    public static final int EN_TANK_KBULK  =23;

    public static final int EN_DIAMETER    =0;    /* Link parameters */
    public static final int EN_LENGTH      =1;
    public static final int EN_ROUGHNESS   =2;
    public static final int EN_MINORLOSS   =3;
    public static final int EN_INITSTATUS  =4;
    public static final int EN_INITSETTING =5;
    public static final int EN_KBULK       =6;
    public static final int EN_KWALL       =7;
    public static final int EN_FLOW        =8;
    public static final int EN_VELOCITY    =9;
    public static final int EN_HEADLOSS    =10;
    public static final int EN_STATUS      =11;
    public static final int EN_SETTING     =12;
    public static final int EN_ENERGY      =13;

    public static final int EN_DURATION    =0;    /* Time parameters */
    public static final int EN_HYDSTEP     =1;
    public static final int EN_QUALSTEP    =2;
    public static final int EN_PATTERNSTEP =3;
    public static final int EN_PATTERNSTART=4;
    public static final int EN_REPORTSTEP  =5;
    public static final int EN_REPORTSTART =6;
    public static final int EN_RULESTEP    =7;
    public static final int EN_STATISTIC   =8;
    public static final int EN_PERIODS     =9;

    public static final int EN_NODECOUNT   =0;    /* Component counts */
    public static final int EN_TANKCOUNT   =1;
    public static final int EN_LINKCOUNT   =2;
    public static final int EN_PATCOUNT    =3;
    public static final int EN_CURVECOUNT  =4;
    public static final int EN_CONTROLCOUNT=5;

    public static final int EN_JUNCTION    =0;    /* Node types */
    public static final int EN_RESERVOIR   =1;
    public static final int EN_TANK        =2;

    public static final int EN_CVPIPE      =0;    /* Link types */
    public static final int EN_PIPE        =1;
    public static final int EN_PUMP        =2;
    public static final int EN_PRV         =3;
    public static final int EN_PSV         =4;
    public static final int EN_PBV         =5;
    public static final int EN_FCV         =6;
    public static final int EN_TCV         =7;
    public static final int EN_GPV         =8;

    public static final int EN_NONE        =0;    /* Quality analysis types */
    public static final int EN_CHEM        =1;
    public static final int EN_AGE         =2;
    public static final int EN_TRACE       =3;

    public static final int EN_CONCEN      =0;    /* Source quality types */
    public static final int EN_MASS        =1;
    public static final int EN_SETPOINT    =2;
    public static final int EN_FLOWPACED   =3;

    public static final int EN_CFS         =0;    /* Flow units types */
    public static final int EN_GPM         =1;
    public static final int EN_MGD         =2;
    public static final int EN_IMGD        =3;
    public static final int EN_AFD         =4;
    public static final int EN_LPS         =5;
    public static final int EN_LPM         =6;
    public static final int EN_MLD         =7;
    public static final int EN_CMH         =8;
    public static final int EN_CMD         =9;

    public static final int EN_TRIALS      =0;   /* Misc. options */
    public static final int EN_ACCURACY    =1;
    public static final int EN_TOLERANCE   =2;
    public static final int EN_EMITEXPON   =3;
    public static final int EN_DEMANDMULT  =4;

    public static final int EN_LOWLEVEL    =0;   /* Control types */
    public static final int EN_HILEVEL     =1;
    public static final int EN_TIMER       =2;
    public static final int EN_TIMEOFDAY   =3;

    public static final int EN_AVERAGE     =1;   /* Time statistic types.    */
    public static final int EN_MINIMUM     =2;
    public static final int EN_MAXIMUM     =3;
    public static final int EN_RANGE       =4;

    public static final int EN_MIX1        =0;   /* Tank mixing models */
    public static final int EN_MIX2        =1;
    public static final int EN_FIFO        =2;
    public static final int EN_LIFO        =3;

    public static final int EN_NOSAVE      =0 ;  /* Save-results-to-file flag */
    public static final int EN_SAVE        =1 ;
    public static final int EN_INITFLOW    =10;  /* Re-initialize flow flag   */


    public ENToolkit(Epanet epa) {
        epanet = epa;
    }

    private Epanet epanet;


    public void ENwriteline(String s){
        epanet.getReport().writeline(s);
    }

    public String ENgetlinkid(int j) {
        return epanet.getNetwork().getLink(j).getId();
    }

    public String ENgetnodeid(int j) {
        return epanet.getNetwork().getNode(j).getId();
    }

    public int ENsolveH() {
        epanet.simulateHydraulics(false);
        return 0;
    }

     public int ENsolveH(BufferedWriter writer) {
        epanet.simulateHydraulics2(writer,false);
        return 0;
    }

    public int ENsavehydfile(String filename) {
        File source = epanet.mHydFile;
        File target = new File(filename);
        BufferedInputStream in = null;
        OutputStream out = null;

        try {
            in = new BufferedInputStream(new FileInputStream(source));
            out = new BufferedOutputStream(new FileOutputStream(target));

            byte[] buf = new byte[1024];
            int len;

            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }

            in.close();
            out.close();
        }
        catch (IOException e) {return 305;}

        return 0;
    }

    public int ENgetnodeindex(String s, int[] tmp) {
        tmp[0] = epanet.getInputReader().findNode(s);
        if (tmp[0] == 0)
            return(203);
        return 0;
    }

    public int ENgetlinkindex(String s, int[] tmp) {
        tmp[0] = epanet.getInputReader().findLink(s);
        if (tmp[0] == 0)
            return(204);
        return 0;
    }

    public int ENgetflowunits() {
        return epanet.getNetwork().Flowflag.ordinal();
    }

    public int ENgetnodetype(int i) throws Exception{

        int Njuncs = epanet.getNetwork().getSections(SectType._JUNCTIONS);
        if (i < 1 || i > epanet.getNetwork().getMaxNodes()) throw new Exception("203");
        if (i <= Njuncs)
            return EN_JUNCTION;
        else
        {
            if (epanet.getNetwork().getTank(i-Njuncs).getArea() == 0.0)
                return EN_RESERVOIR;
            else
                return EN_TANK;
        }
    }

    public float ENgetlinkvalue(int index, int code) throws Exception{
        double a,h,q, v = 0.0;
        float value = 0;
        // Check for valid arguments
        value = 0.0f;
        //if (!Openflag) return(102);
        int Nlinks = epanet.getNetwork().getMaxLinks();
        if (index <= 0 || index > Nlinks) throw new Exception("204");
        Link link = epanet.getNetwork().getLink(index);
        Network net = epanet.getNetwork();
        Hydraulic hyd = epanet.getHydraulicsSolver();

        // Retrieve called-for parameter
        switch (code)
        {
            case EN_DIAMETER:
                if (link.getType() == LinkType.PUMP) v = 0.0;
                else v = link.getDiameter()*net.getUcf(FieldType.DIAM);
                break;

            case EN_LENGTH:
                v = link.getLenght()*net.getUcf(FieldType.ELEV);
                break;

            case EN_ROUGHNESS:
                if (link.getType().ordinal() <= LinkType.PIPE.ordinal())
                {
                    if (net.Formflag == FormType.DW)
                        v = link.getKc()*(1000.0*net.getUcf(FieldType.ELEV));
                    else v = link.getKc();
                }
                else v = 0.0;
                break;

            case EN_MINORLOSS:
                if (link.getType() != LinkType.PUMP)
                {
                    v = link.getKm();
                    v *= (Math.pow(link.getDiameter(),2)*Math.pow(link.getDiameter(),2)/0.02517);
                }
                else v = 0.0;
                break;

            case EN_INITSTATUS:
                if (link.getStat().ordinal() <=StatType.CLOSED.ordinal()) v = 0.0;
                else v = 1.0;
                break;

            case EN_INITSETTING:
                if (link.getType() == LinkType.PIPE || link.getType() == LinkType.CV)
                    return(ENgetlinkvalue(index, EN_ROUGHNESS));
                //return(ENgetlinkvalue(index, EN_ROUGHNESS, value));
                v = link.getKc();
                switch (link.getType())
                {
                    case PRV:
                    case PSV:
                    case PBV: v *= net.getUcf(FieldType.PRESSURE); break;
                    case FCV: v *= net.getUcf(FieldType.FLOW);
                }
                break;

            case EN_KBULK:
                v = link.getKb()*Constants.SECperDAY;
                break;

            case EN_KWALL:
                v = link.getKw()*Constants.SECperDAY;
                break;

            case EN_FLOW:

                // Updated 10/25/00
                if (hyd.S[index].ordinal() <= StatType.CLOSED.ordinal()) v = 0.0;

                else v = hyd.Q[index]*net.getUcf(FieldType.FLOW);
                break;

            case EN_VELOCITY:
                if (link.getType() == LinkType.PUMP) v = 0.0;

                    // Updated 11/19/01
                else if (hyd.S[index].ordinal() <= StatType.CLOSED.ordinal()) v = 0.0;

                else
                {
                    q = Math.abs(hyd.Q[index]);
                    a = Constants.PI*Math.pow(link.getDiameter(), 2)/4.0;
                    v = q/a*net.getUcf(FieldType.VELOCITY);
                }
                break;

            case EN_HEADLOSS:

                // Updated 11/19/01
                if (hyd.S[index].ordinal() <= StatType.CLOSED.ordinal()) v = 0.0;

                else
                {
                    h = hyd.H[link.getN1()] - hyd.H[link.getN2()];
                    if (link.getType() != LinkType.PUMP) h = Math.abs(h);
                    v = h*net.getUcf(FieldType.HEADLOSS);
                }
                break;

            case EN_STATUS:
                if (hyd.S[index].ordinal() <= StatType.CLOSED.ordinal()) v = 0.0;
                else v = 1.0;
                break;

            case EN_SETTING:
                if (link.getType() == LinkType.PIPE || link.getType() == LinkType.CV)
                    return(ENgetlinkvalue(index, EN_ROUGHNESS));
                if (hyd.K[index] == Constants.MISSING) v = 0.0;
                else                     v = hyd.K[index];
                switch (link.getType())
                {
                    case PRV:
                    case PSV:
                    case PBV: v *= net.getUcf(FieldType.PRESSURE); break;
                    case FCV: v *= net.getUcf(FieldType.FLOW);
                }
                break;

            case EN_ENERGY:
                double [] ret = hyd.getenergy(index);//, &v, &a);
                v = ret[0];
                a = ret[1];
                break;

            default: throw new Exception("251");
        }
        value = (float)v;
        return(value);
    }

    public int ENgetcount(int code) {
        int count = 0;
        switch (code)
        {
            case EN_NODECOUNT:    count = epanet.getNetwork().getMaxNodes();    break;
            case EN_TANKCOUNT:    count = epanet.getNetwork().getMaxTanks();    break;
            case EN_LINKCOUNT:    count = epanet.getNetwork().getMaxLinks();    break;
            case EN_PATCOUNT:     count = epanet.getNetwork().getSections(SectType._PATTERNS); break;
            case EN_CURVECOUNT:   count = epanet.getNetwork().getSections(SectType._CURVES);   break;
            case EN_CONTROLCOUNT: count = epanet.getNetwork().getSections(SectType._CONTROLS); break;
            default:
                return 0;
        }
        return(count);
    }

    public long ENgettimeparam(int code) {
       long value = 0;
       //if (!Openflag) return(102);
       if (code < EN_DURATION || code > EN_PERIODS) return(251);
       switch (code)
       {
          case EN_DURATION:     value = epanet.getNetwork().Dur;       break;
          case EN_HYDSTEP:      value = epanet.getNetwork().Hstep;     break;
          case EN_QUALSTEP:     value = epanet.getNetwork().Qstep;     break;
          case EN_PATTERNSTEP:  value = epanet.getNetwork().Pstep;     break;
          case EN_PATTERNSTART: value = epanet.getNetwork().Pstart;    break;
          case EN_REPORTSTEP:   value = epanet.getNetwork().Rstep;     break;
          case EN_REPORTSTART:  value = epanet.getNetwork().Rstart;    break;
          case EN_STATISTIC:    value = epanet.getNetwork().Tstatflag.ordinal(); break;
          case EN_PERIODS:      value = epanet.getQuality().Nperiods;  break;
       }
       return(value);
    }

    public float ENgetnodevalue(int index, int code) throws Exception {
        double v = 0.0;
        //Demand demand;
        Source source;


        float value = 0.0f;


        if (index <= 0 || index > epanet.getNetwork().getMaxNodes()) return(203);
        Node node = epanet.getNetwork().getNode(index);
        Network net = epanet.getNetwork();
        Hydraulic hyd = epanet.getHydraulicsSolver();
        int Njuncs = epanet.getNetwork().getSections(SectType._JUNCTIONS);
        switch (code)
        {
            case EN_ELEVATION:
                v = node.getElevation()*net.getUcf(FieldType.ELEV);
                break;

            case EN_BASEDEMAND:
                v = 0.0;
                // NOTE: primary demand category is last on demand list
                if (index <= Njuncs)
                    for(Demand demand : net.getNode(index).getDemand())
                        v = demand.getBase();
                    //for (demand = Node[index].D; demand != NULL; demand = demand->next)
                    //    v = (demand->Base);
                v *= net.getUcf(FieldType.FLOW);
                break;

            case EN_PATTERN:
                v = 0.0;
                // NOTE: primary demand category is last on demand list
                if (index <= Njuncs)
                {
                    //for (demand = net.getNode(index).getDemand(); demand != NULL; demand = demand->next)
                    for(Demand demand : net.getNode(index).getDemand())
                        v = (double)(demand.getPattern());
                }
                else v = (double)(net.getTank(index-Njuncs).getPattern());
                break;

            case EN_EMITTER:
                v = 0.0;
                if (net.getNode(index).getKe() > 0.0)
                    v = net.getUcf(FieldType.FLOW)/Math.pow((net.getUcf(FieldType.PRESSURE) * net.getNode(index).getKe()), (1.0 / net.Qexp));
                break;

            case EN_INITQUAL:
                v = net.getNode(index).getC0()*net.getUcf(FieldType.QUALITY);
                break;

            // Additional parameters added for retrieval     //(2.00.11 - LR)
            case EN_SOURCEQUAL:
            case EN_SOURCETYPE:
            case EN_SOURCEMASS:
            case EN_SOURCEPAT:
                source = net.getNode(index).getSource();
                if (source == null) throw new Exception("240");
                if (code == EN_SOURCEQUAL)
                    v = source.getC0();
                else if (code == EN_SOURCEMASS)
                    v = source.getSmass()*60.0;
                else if (code == EN_SOURCEPAT)
                    v = source.getPattern();
                else
                    v = (double)source.getType().ordinal();
                return((float)v);

            case EN_TANKLEVEL:
                if (index <= Njuncs) return(251);
                v = (net.getTank(index-Njuncs).getH0() - net.getNode(index).getElevation())*net.getUcf(FieldType.ELEV);
                break;

            // New parameter added for retrieval                                     //(2.00.11 - LR)
            case EN_INITVOLUME:                                                      //(2.00.11 - LR)
                v = 0.0;                                                              //(2.00.11 - LR)
                if ( index > Njuncs ) v = net.getTank(index-Njuncs).getV0()*net.getUcf(FieldType.VOLUME);          //(2.00.11 - LR)
                break;                                                                //(2.00.11 - LR)

            // New parameter added for retrieval                                     //(2.00.11 - LR)
            case EN_MIXMODEL:                                                        //(2.00.11 - LR)
                v = MixType.MIX1.ordinal();                                                             //(2.00.11 - LR)
                if ( index > Njuncs ) v = net.getTank(index-Njuncs).getMixModel().ordinal();                //(2.00.11 - LR)
                break;                                                                //(2.00.11 - LR)

            // New parameter added for retrieval                                     //(2.00.11 - LR)
            case EN_MIXZONEVOL:                                                      //(2.00.11 - LR)
                v = 0.0;                                                              //(2.00.11 - LR)
                if ( index > Njuncs ) v = net.getTank(index-Njuncs).getV1max()*net.getUcf(FieldType.VOLUME);       //(2.00.11 - LR)
                break;                                                                //(2.00.11 - LR)

            case EN_DEMAND:
                v = hyd.D[index]*net.getUcf(FieldType.FLOW);
                break;

            case EN_HEAD:
                v = hyd.H[index]*net.getUcf(FieldType.HEAD);
                break;

            case EN_PRESSURE:
                v = (hyd.H[index] - net.getNode(index).getElevation())*net.getUcf(FieldType.PRESSURE);
                break;

            case EN_QUALITY:
                v = epanet.getQuality().C[index]*net.getUcf(FieldType.QUALITY);
                break;

            // New parameters added for retrieval begins here
            // (Thanks to Nicolas Basile of Ecole Polytechnique
            //  de Montreal for suggesting some of these.)

            case EN_TANKDIAM:
                v = 0.0;
                if ( index > Njuncs )
                {
                    v = 4.0/Constants.PI*Math.sqrt(net.getTank(index-Njuncs).getArea())*net.getUcf(FieldType.ELEV);
                }
                break;

            case EN_MINVOLUME:
                v = 0.0;
                if ( index > Njuncs ) v = net.getTank(index-Njuncs).getVmin() * net.getUcf(FieldType.VOLUME);
                break;

            case EN_VOLCURVE:
                v = 0.0;
                if ( index > Njuncs ) v = net.getTank(index-Njuncs).getVcurve();
                break;

            case EN_MINLEVEL:
                v = 0.0;
                if ( index > Njuncs )
                {
                    v = (net.getTank(index-Njuncs).getHMin() - net.getNode(index).getElevation()) * net.getUcf(FieldType.ELEV);
                }
                break;

            case EN_MAXLEVEL:
                v = 0.0;
                if ( index > Njuncs )
                {
                    v = (net.getTank(index-Njuncs).getHMax()  - net.getNode(index).getElevation()) * net.getUcf(FieldType.ELEV);
                }
                break;

            case EN_MIXFRACTION:
                v = 1.0;
                if ( index > Njuncs && net.getTank(index-Njuncs).getVmax() > 0.0)
                {
                    v = net.getTank(index-Njuncs).getV1max() / net.getTank(index-Njuncs).getVmax();
                }
                break;

            case EN_TANK_KBULK:
                v = 0.0;
                if (index > Njuncs) v = net.getTank(index-Njuncs).getKb() * Constants.SECperDAY;
                break;

            //  New parameter additions ends here.                                 //(2.00.12 - LR)

            default: throw new Exception("251");//return(251);
        }
       return (float)v;
    }

    public int[] ENgetlinknodes(int index) throws Exception {

       if (index < 1 || index > epanet.getNetwork().getMaxLinks()) throw new Exception("204");
       return new int[]{epanet.getNetwork().getLink(index).getN1(),epanet.getNetwork().getLink(index).getN2()};
    }

    public Network getNetwork() {
        return epanet.getNetwork();
    }

    public Report getReport(){
        return epanet.getReport();
    }
}
