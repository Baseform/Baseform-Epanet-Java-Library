package org.addition.epanetold;

import org.addition.epanetold.Types.*;
import org.addition.epanetold.Types.EnumVariables.*;

import java.util.*;

public strictfp class Network {

    private static final ResourceBundle errorBundle = PropertyResourceBundle.getBundle("Error");

    private final int[] sectionCounter  = new int [EnumVariables.SectType.values().length];

    private int maxNodes=0;
    private int maxLinks=0;
    private int maxTanks=0;

    private List<String> titleText;
    private List<Pattern> patterns;
    private List<Node> nodes;
    private List<Tank> tanks;
    private List<Link> links;
    private DblList nodeDemand;
    private List<Pump> pumps;
    private List<Valve> valves;
    private List<Curve> curves;
    private List<Control> controls;
    private List<Field> fields;
    private List<Label> labels;

    // Reaction coefficients data
    double  BulkOrder;  // Bulk flow reaction order
    double  TankOrder;  // Pipe wall reaction order
    double  WallOrder;  // Pipe wall reaction order
    double  Rfactor;    // Roughness-reaction factor
    double  Climit;     // Limiting potential quality
    double  Kbulk;      // Global bulk reaction coeff.
    double  Kwall;      // Global wall reaction coeff,No global wall reaction
    double  Dcost;      // Energy demand charge/kw/day
    double  Ecost;      // Base energy cost per kwh
    int     Epat;       // Energy cost time pattern
    double  Epump;      // Global pump efficiency

    int PageSize;                                   // Lines/page in output report
    EnumVariables.StatFlag      Statflag;           // Status report flag
    boolean                     Summaryflag;        // Report summary flag
    boolean                     Messageflag;        // Error/warning message flag
    boolean                     Energyflag;         // Energy report flag
    EnumVariables.ReportFlag    Nodeflag;           // Node report flag
    EnumVariables.ReportFlag    Linkflag;           // Link report flag
    String                      Rpt2Fname;          // Secondary report file name

    // Simulation flags
    EnumVariables.TstatType Tstatflag;      // Time statistics flag
    long Hstep;                             //Nominal hyd. time step (sec)
    long Dur;                               //Duration of simulation (sec)
    long Qstep;                             // Quality time step (sec)
    long Rulestep;                          // Reporting time step (sec)
    long Pstep;                             // Time pattern time step (sec)
    long Pstart;                            // Starting pattern time (sec)
    long Rstep;                             // Reporting time step (sec)
    long Rstart;                            // Time when reporting starts
    long Tstart;                            // Starting time of day (sec)

    // Control flags
    EnumVariables.FlowUnitsType     Flowflag;     // Flow units flag
    EnumVariables.PressUnitsType    Pressflag;    // Pressure units flag


    EnumVariables.FormType          Formflag;     // Hydraulic formula flag
    EnumVariables.Hydtype           Hydflag;      // Hydraulics flag
    EnumVariables.QualType          Qualflag;     // Water quality flag


    EnumVariables.UnitsType         Unitsflag;    // Unit system flag

    String HydFname;    // Hydraulics file name
    String ChemName;    // Name of chemical
    String ChemUnits;   // Units of chemical
    String DefPatID;
    String MapFname;
    int    TraceNode;   // Source node for flow tracing
    int    ExtraIter;
    double Ctol;
    double Diffus;
    double DampLimit;
    double Viscos;
    double SpGrav;
    int    MaxIter;
    double Hacc;
    double Htol;
    double Qtol;
    double RQtol;
    double Hexp;        // Exponent in headloss formula
    double Qexp;
    int CheckFreq;
    int MaxCheck;
    double Dmult;       // Demand multiplier
    int    DefPat;      // Default demand pattern
    double  Emax;       // Peak energy usage
    private double [] Ucf;      // Units convertion factors

    private Report rep;
    private final Epanet epanet;



    //private QualType qualflag;

    public Network(Epanet epanet){
        this.epanet = epanet;
        for(int i = 0;i<sectionCounter.length;i++){
            sectionCounter[i]=0;
        }
    }

    void loadDependencies(){
        rep = epanet.getReport();
    }

    double getUcf(FieldType field){
        return Ucf[field.ordinal()];
    }


    public long getHstep() {
        return Hstep;
    }

    void setDefaults(){
        Messageflag = true;
        DefPatID  = Constants.DEFPATID;
        Hydflag   = EnumVariables.Hydtype.SCRATCH;          // No external hydraulics file
        Qualflag  = EnumVariables.QualType.NONE;            // No quality simulation
        Formflag  = EnumVariables.FormType.HW;              // Use Hazen-Williams formula
        Unitsflag = EnumVariables.UnitsType.US;             // US unit system
        Flowflag  = EnumVariables.FlowUnitsType.GPM;        // Flow units are gpm
        Pressflag = EnumVariables.PressUnitsType.PSI;       // Pressure units are psi
        Tstatflag = EnumVariables.TstatType.SERIES;         // Generate time series output
        //Warnflag  = FALSE;                                // Warning flag is off
        Htol      = Constants.HTOL;                         // Default head tolerance
        Qtol      = Constants.QTOL;                         // Default flow tolerance
        Hacc      = Constants.HACC;                         // Default hydraulic accuracy
        Ctol      = Constants.MISSING;                      // No pre-set quality tolerance
        MaxIter   = Constants.MAXITER;                      // Default max. hydraulic trials
        ExtraIter = -1;                                     // Stop if network unbalanced
        Dur       = 0;                                      // 0 sec duration (steady state)
        Tstart    = 0;                                      // Starting time of day
        Pstart    = 0;                                      // Starting pattern period
        Hstep     = 3600;                                   // 1 hr hydraulic time step
        Qstep     = 0;                                      // No pre-set quality time step
        Pstep     = 3600;                                   // 1 hr time pattern period
        Rstep     = 3600;                                   // 1 hr reporting period
        Rulestep  = 0;                                      // No pre-set rule time step
        Rstart    = 0;                                      // Start reporting at time 0
        TraceNode = 0;                                      // No source tracing
        BulkOrder = 1.0;                                    // 1st-order bulk reaction rate
        WallOrder = 1.0;                                    // 1st-order wall reaction rate
        TankOrder = 1.0;                                    // 1st-order tank reaction rate
        Kbulk     = 0.0;                                    // No global bulk reaction
        Kwall     = 0.0;                                    // No global wall reaction
        Climit    = 0.0;                                    // No limiting potential quality
        Diffus    = Constants.MISSING;                      // Temporary diffusivity
        Rfactor   = 0.0;                                    // No roughness-reaction factor
        Viscos    = Constants.MISSING;                      // Temporary viscosity
        SpGrav    = Constants.SPGRAV;                       // Default specific gravity
        DefPat    = 0;                                      // Default demand pattern index
        Epat      = 0;                                      // No energy price pattern
        Ecost     = 0.0;                                    // Zero unit energy cost
        Dcost     = 0.0;                                    // Zero energy demand charge
        Epump     = Constants.EPUMP;                        // Default pump efficiency
        Emax      = 0.0;                                    // Zero peak energy usage
        Qexp      = 2.0;                                    // Flow exponent for emitters
        Dmult     = 1.0;                                    // Demand multiplier
        RQtol     = Constants.RQTOL;                        // Default hydraulics parameters
        CheckFreq = Constants.CHECKFREQ;
        MaxCheck  = Constants.MAXCHECK;
        DampLimit = Constants.DAMPLIMIT;
        Ucf = new double[Constants.MAXVAR];
    }

    public int allocdata(){
        if(getSections(EnumVariables.SectType._TITLE)>0)
            titleText = new ArrayList<String>();

        if(maxNodes>0)
        {
            nodeDemand = new DblList(maxNodes+1);
            for(int i = 0;i<maxNodes;i++) nodeDemand.add(0.0d);

            nodes = new ArrayList<Node>(maxNodes+1);
            for(int i = 0;i<maxNodes+1;i++) nodes.add(new Node());
        }

        maxTanks = getSections(EnumVariables.SectType._TANKS)+
                getSections(EnumVariables.SectType._RESERVOIRS);
        if(maxTanks>0){
            tanks = new ArrayList<Tank>(maxTanks+1);
            for(int i = 0;i<maxTanks+1;i++)
                tanks.add(new Tank());
        }

        if(maxLinks>0)
        {
            links = new ArrayList<Link>(maxLinks+1);
            for(int i = 0;i<maxLinks+1;i++)
                links.add(new Link());
        }

        int lPumpsCount = getSections(EnumVariables.SectType._PUMPS);
        if(lPumpsCount>0)
        {
            pumps = new ArrayList<Pump>(lPumpsCount+1);
            for(int i = 0;i<lPumpsCount+1;i++)
                pumps.add(new Pump());
        }

        int lValves = getSections(EnumVariables.SectType._VALVES);
        if(lValves>0)
        {
            valves = new ArrayList<Valve>(lValves+1);
            for(int i = 0;i<lValves+1;i++)
                valves.add(new Valve());
        }

        int lPatterns = getSections(EnumVariables.SectType._PATTERNS);
        if(lPatterns>0){
            patterns = new ArrayList<Pattern>(lPatterns+1);
            for(int i = 0;i<lPatterns+1;i++)
                patterns.add(new Pattern());
        }

        int lCurves = getSections(EnumVariables.SectType._CURVES);
        if(lCurves>0){
            curves = new ArrayList<Curve>(lCurves+1);
            for(int i = 0;i<lCurves+1;i++)
                curves.add(new Curve());
        }

        int lControls = getSections(EnumVariables.SectType._CONTROLS);
        if(lControls>0){
            controls = new ArrayList<Control>(lControls+1);
            for(int i = 0;i<lControls+1;i++)
                controls.add(new Control());
        }

        int lFields = Constants.MAXVAR;
        if(lFields>0){
            fields = new ArrayList<Field>(lFields);
            for(int i = 0;i<lFields;i++){
                fields.add(new Field());
                fields.get(i).setName(EnumVariables.Fldname[i]);
                fields.get(i).setEnabled(false);
                fields.get(i).setPrecision(2);
                fields.get(i).setRptLim(EnumVariables.RangeType.LOW.ordinal(),Constants.BIG*Constants.BIG);
                fields.get(i).setRptLim(EnumVariables.RangeType.HI.ordinal(),-Constants.BIG*Constants.BIG);
            }

            fields.get(EnumVariables.FieldType.FRICTION.ordinal()).setPrecision(3);

            for (int i=EnumVariables.FieldType.DEMAND.ordinal();
                 i<= EnumVariables.FieldType.QUALITY.ordinal(); i++)
                fields.get(i).setEnabled(true);

            for (int i=EnumVariables.FieldType.FLOW.ordinal();
                 i<=EnumVariables.FieldType.HEADLOSS.ordinal(); i++)
                fields.get(i).setEnabled(true);
        }


        int lLabels = getSections(SectType._LABELS);
        if(lLabels>0)
        {
            labels = new ArrayList<Label>(lLabels+1);
            for(int i = 0;i<=lLabels;i++)
                labels.add(new Label());
        }
        return 0;
    }

    public double getDemandMultiplier(){
        return Dmult;
    }

    public EnumVariables.QualType getQualityFlag(){
        return Qualflag;
    }

    public int getMaxTanks() {
        return maxTanks;
    }

    public Pump getPump(int id){
        return pumps.get(id);
    }

    public Link getLink(int id){
        return links.get(id);
    }

    public Node getNode(int id){
        return nodes.get(id);
    }

    public Tank getTank(int id){
        return tanks.get(id);
    }

    public Valve getValve(int id){
        return valves.get(id);
    }

    public Pattern getPattern(int id){
        return patterns.get(id);
    }

    public Curve getCurve(int id){
        return curves.get(id);
    }

    public Field getField(int id){
        return fields.get(id);
    }

    public Field getField(EnumVariables.FieldType type){
        return fields.get(type.ordinal());
    }


    public Double getNodeDemand(int id){
        return nodeDemand.get(id);
    }

    public void setNodeDemand(int id, Double val){
        nodeDemand.set(id,val);
    }

    public Control getControl(int id){
        return controls.get(id);
    }

    public List<String> getTitleText(){
        return titleText;
    }

    public int getMaxNodes(){
        return maxNodes;
    }

    public int getMaxLinks(){
        return maxLinks;
    }

    void setMaxNodes(int count){
        maxNodes=count;
    }

    void setMaxLinks(int count){
        maxLinks=count;
    }




    public int getSections(EnumVariables.SectType type){
        return sectionCounter[type.ordinal()];
    }

    void setSections(EnumVariables.SectType type, int value){
        sectionCounter[type.ordinal()]=value;
    }

    void addSections(EnumVariables.SectType type){
        sectionCounter[type.ordinal()]++;
    }

    public String toString(){
        String res=" Network log\n";
        res+="  MaxNodes size : " + Integer.toString( maxNodes )+ "\n";
        res+="  MaxLinks size : " + Integer.toString( maxLinks )+ "\n";
        for(int i = 0;i<sectionCounter.length;i++){
            if(sectionCounter[i]>0)
                res+= "  "+EnumVariables.SectType.values()[i].toString() + " size : " +
                        Integer.toString( sectionCounter[i] )+ "\n";
        }
        return res;
    }


    void  initUnits()
    {
        double  dcf,
                ccf,
                qcf,
                hcf,
                pcf,
                wcf;

        if (Unitsflag == EnumVariables.UnitsType.SI)
        {
            getField(EnumVariables.FieldType.DEMAND).setUnits(EnumVariables.RptFlowUnitsTxt[Flowflag.ordinal()]);
            getField(EnumVariables.FieldType.ELEV).setUnits(EnumVariables.u_METERS);
            getField(EnumVariables.FieldType.HEAD).setUnits(EnumVariables.u_METERS);

            if (Pressflag == EnumVariables.PressUnitsType.METERS)
                getField(EnumVariables.FieldType.PRESSURE).setUnits(EnumVariables.u_METERS);
            else
                getField(EnumVariables.FieldType.PRESSURE).setUnits(EnumVariables.u_KPA);

            getField(EnumVariables.FieldType.LENGTH).setUnits(EnumVariables.u_METERS);
            getField(EnumVariables.FieldType.DIAM).setUnits(EnumVariables.u_MMETERS);
            getField(EnumVariables.FieldType.FLOW).setUnits(EnumVariables.RptFlowUnitsTxt[Flowflag.ordinal()]);
            getField(EnumVariables.FieldType.VELOCITY).setUnits(EnumVariables.u_MperSEC);
            getField(EnumVariables.FieldType.HEADLOSS).setUnits(EnumVariables.u_per1000M);
            getField(EnumVariables.FieldType.FRICTION).setUnits("");
            getField(EnumVariables.FieldType.POWER).setUnits(EnumVariables.u_KW);

            dcf = 1000.0*Constants.MperFT;
            qcf = Constants.LPSperCFS;
            if (Flowflag == EnumVariables.FlowUnitsType.LPM) qcf = Constants.LPMperCFS;
            if (Flowflag == EnumVariables.FlowUnitsType.MLD) qcf = Constants.MLDperCFS;
            if (Flowflag == EnumVariables.FlowUnitsType.CMH) qcf = Constants.CMHperCFS;
            if (Flowflag == EnumVariables.FlowUnitsType.CMD) qcf = Constants.CMDperCFS;
            hcf = Constants.MperFT;
            if (Pressflag == EnumVariables.PressUnitsType.METERS) pcf = Constants.MperFT*SpGrav;
            else pcf = Constants.KPAperPSI*Constants.PSIperFT*SpGrav;
            wcf = Constants.KWperHP;
        }
        else
        {
            getField(EnumVariables.FieldType.DEMAND).setUnits(EnumVariables.RptFlowUnitsTxt[Flowflag.ordinal()]);
            getField(EnumVariables.FieldType.ELEV).setUnits(EnumVariables.u_FEET);
            getField(EnumVariables.FieldType.HEAD).setUnits(EnumVariables.u_FEET);

            getField(EnumVariables.FieldType.PRESSURE).setUnits(EnumVariables.u_PSI);
            getField(EnumVariables.FieldType.LENGTH).setUnits(EnumVariables.u_FEET);
            getField(EnumVariables.FieldType.DIAM).setUnits(EnumVariables.u_INCHES);
            getField(EnumVariables.FieldType.FLOW).setUnits(EnumVariables.RptFlowUnitsTxt[Flowflag.ordinal()]);
            getField(EnumVariables.FieldType.VELOCITY).setUnits(EnumVariables.u_FTperSEC);
            getField(EnumVariables.FieldType.HEADLOSS).setUnits(EnumVariables.u_per1000FT);
            getField(EnumVariables.FieldType.FRICTION).setUnits("");
            getField(EnumVariables.FieldType.POWER).setUnits(EnumVariables.u_HP);


            dcf = 12.0;
            qcf = 1.0;
            if (Flowflag == EnumVariables.FlowUnitsType.GPM) qcf = Constants.GPMperCFS;
            if (Flowflag == EnumVariables.FlowUnitsType.MGD) qcf = Constants.MGDperCFS;
            if (Flowflag == EnumVariables.FlowUnitsType.IMGD)qcf = Constants.IMGDperCFS;
            if (Flowflag == EnumVariables.FlowUnitsType.AFD) qcf = Constants.AFDperCFS;
            hcf = 1.0;
            pcf = Constants.PSIperFT*SpGrav;
            wcf = 1.0;
        }
        getField(EnumVariables.FieldType.QUALITY).setUnits("");
        ccf = 1.0;
        if (Qualflag == EnumVariables.QualType.CHEM)
        {
            ccf = 1.0/Constants.LperFT3;
            getField(EnumVariables.FieldType.QUALITY).setUnits(ChemUnits);
            getField(EnumVariables.FieldType.REACTRATE).setUnits(ChemUnits + EnumVariables.t_PERDAY);
        }
        else if (Qualflag == EnumVariables.QualType.AGE)
            getField(EnumVariables.FieldType.QUALITY).setUnits(EnumVariables.u_HOURS);
        else if (Qualflag == EnumVariables.QualType.TRACE)
            getField(EnumVariables.FieldType.QUALITY).setUnits(EnumVariables.u_PERCENT);

        Ucf[EnumVariables.FieldType.DEMAND.ordinal()]    = qcf;
        Ucf[EnumVariables.FieldType.ELEV.ordinal()]      = hcf;
        Ucf[EnumVariables.FieldType.HEAD.ordinal()]      = hcf;
        Ucf[EnumVariables.FieldType.PRESSURE.ordinal()]  = pcf;
        Ucf[EnumVariables.FieldType.QUALITY.ordinal()]   = ccf;
        Ucf[EnumVariables.FieldType.LENGTH.ordinal()]    = hcf;
        Ucf[EnumVariables.FieldType.DIAM.ordinal()]      = dcf;
        Ucf[EnumVariables.FieldType.FLOW.ordinal()]      = qcf;
        Ucf[EnumVariables.FieldType.VELOCITY.ordinal()]  = hcf;
        Ucf[EnumVariables.FieldType.HEADLOSS.ordinal()]  = hcf;
        Ucf[EnumVariables.FieldType.LINKQUAL.ordinal()]  = ccf;
        Ucf[EnumVariables.FieldType.REACTRATE.ordinal()] = ccf;
        Ucf[EnumVariables.FieldType.FRICTION.ordinal()]  = 1.0;
        Ucf[EnumVariables.FieldType.POWER.ordinal()]     = wcf;
        Ucf[EnumVariables.FieldType.VOLUME.ordinal()]    = hcf*hcf*hcf;

        if (Hstep < 1800)
        {
            Ucf[EnumVariables.FieldType.TIME.ordinal()] = 1.0/60.0;
            getField(EnumVariables.FieldType.TIME).setUnits(EnumVariables.u_MINUTES);
        }
        else
        {
            Ucf[EnumVariables.FieldType.TIME.ordinal()] =  1.0/3600.0;
            getField(EnumVariables.FieldType.TIME).setUnits(EnumVariables.u_HOURS);
        }

    }


    void  adjustData()
    {
        int   i;
        double ucf;

        if (Pstep <= 0) Pstep = 3600;
        if (Rstep == 0) Rstep = Pstep;

        if (Hstep <=  0)   Hstep = 3600;
        if (Hstep > Pstep) Hstep = Pstep;
        if (Hstep > Rstep) Hstep = Rstep;

        if (Rstart > Dur) Rstart = 0;


        if (Dur == 0) Qualflag = EnumVariables.QualType.NONE;


        if (Qstep == 0) Qstep = Hstep/10;


        if (Rulestep == 0) Rulestep = Hstep/10;
        Rulestep = Math.min(Rulestep, Hstep);

        Qstep = Math.min(Qstep, Hstep);


        if (Ctol == Constants.MISSING)
        {
            if (Qualflag == EnumVariables.QualType.AGE) Ctol = Constants.AGETOL;
            else                 Ctol = Constants.CHEMTOL;
        }

        switch (Flowflag)
        {
            case LPS:
            case LPM:
            case MLD:
            case CMH:
            case CMD:
                Unitsflag = EnumVariables.UnitsType.SI;
                break;
            default:
                Unitsflag = EnumVariables.UnitsType.US;
        }


        if (Unitsflag != EnumVariables.UnitsType.SI) Pressflag = EnumVariables.PressUnitsType.PSI;
        else if (Pressflag == EnumVariables.PressUnitsType.PSI) Pressflag = EnumVariables.PressUnitsType.METERS;

        ucf = 1.0;
        if (Unitsflag == EnumVariables.UnitsType.SI) ucf = Math.pow(Constants.MperFT,2);

        if (Viscos == Constants.MISSING)
            Viscos = Constants.VISCOS;
        else if (Viscos > 1.e-3)
            Viscos = Viscos*Constants.VISCOS;
        else
            Viscos = Viscos/ucf;

        if (Diffus == Constants.MISSING)
            Diffus = Constants.DIFFUS;
        else if (Diffus > 1.e-4)
            Diffus = Diffus*Constants.DIFFUS;
        else
            Diffus = Diffus/ucf;

        if (Formflag == EnumVariables.FormType.HW) Hexp = 1.852;
        else                Hexp = 2.0;


        for (i=1; i<=maxLinks; i++)
        {
            Link link = getLink(i);
            if (link.getType().ordinal() > EnumVariables.LinkType.PIPE.ordinal()) continue;
            if (link.getKb() == Constants.MISSING) link.setKb(Kbulk);
            if (link.getKw() == Constants.MISSING)
            {
                if (Rfactor == 0.0)   link.setKw(Kwall);
                else if ((link.getKc() > 0.0) && (link.getDiameter() > 0.0))
                {
                    if (Formflag == EnumVariables.FormType.HW) link.setKw(Rfactor/link.getKc());
                    if (Formflag == EnumVariables.FormType.DW) link.setKw(Rfactor/Math.abs(Math.log(link.getKc()/link.getDiameter())));
                    if (Formflag == EnumVariables.FormType.CM) link.setKw(Rfactor*link.getKc());
                }
                else link.setKw(0.0);
            }
        }
        for (i=1; i<=maxTanks; i++)
            if (getTank(i).getKb() == Constants.MISSING) getTank(i).setKb(Kbulk);

        for (i=1; i<=maxNodes; i++)
        {
            for (Demand d : getNode(i).getDemand()){
                if (d.getPattern() == 0)
                    d.setPattern(DefPat);
            }
        }

        if (Qualflag == EnumVariables.QualType.NONE) getField(EnumVariables.FieldType.QUALITY.ordinal()).setEnabled(false);

    }



    int initTanks()
    {
        int   i,j,n = 0;
        double a;
        int   errcode = 0,
                levelerr;

        for (j=1; j<= getSections(EnumVariables.SectType._TANKS) +
                getSections(EnumVariables.SectType._RESERVOIRS); j++)
        {


            if (tanks.get(j).getArea() == 0.0) continue;

            levelerr = 0;
            if (tanks.get(j).getH0()   > tanks.get(j).getHMax() ||
                    tanks.get(j).getHMin() > tanks.get(j).getHMax() ||
                    tanks.get(j).getH0()   < tanks.get(j).getHMin()
                    ) levelerr = 1;

            i = tanks.get(j).getVcurve();
            if (i > 0)
            {
                n =curves.get(i).getNpts() - 1;
                if (tanks.get(j).getHMin() < curves.get(i).getX().get(0) ||
                        tanks.get(j).getHMax() > curves.get(i).getX().get(n)
                        ) levelerr = 1;
            }

            if (levelerr!=0)
            {
                rep.writeline(String.format(errorBundle.getString("ERR225"),nodes.get(tanks.get(j).getNode())));
                errcode = 200;
            }

            else if (i > 0)
            {
                Curve c = curves.get(i);

                tanks.get(j).setVmin(interp(c.getNpts(),c.getX(),
                        c.getY(),tanks.get(j).getHMin()));
                tanks.get(j).setVmax(interp(c.getNpts(),c.getX(),
                        c.getY(),tanks.get(j).getHMax()));
                tanks.get(j).setV0(interp(c.getNpts(),c.getX(),
                        c.getY(),tanks.get(j).getH0()));

                a = (c.getY().get(n) - c.getY().get(0))/
                        (c.getX().get(n) - c.getX().get(0));
                tanks.get(j).setArea(Math.sqrt(4.0*a/Constants.PI));
            }
        }
        return(errcode);
    }



    private double  interp(int n, List<Double> x, List<Double> y, double xx)
    {
        int    k,m;
        double  dx,dy;

        m = n - 1;
        if (xx <= x.get(0)) return(y.get(0));
        for (k=1; k<=m; k++)
        {
            if (x.get(k) >= xx)
            {
                dx = x.get(k)-x.get(k-1);
                dy = y.get(k)-y.get(k-1);
                if (Math.abs(dx) < Constants.TINY) return(y.get(k));
                else return(y.get(k) - (x.get(k)-xx)*dy/dx);
            }
        }
        return(y.get(m));
    }




    void convertUnits()
    {
        int   i,j,k;
        double ucf;



        for (i=1; i<=maxNodes; i++)
        {
            nodes.get(i).setElevation( nodes.get(i).getElevation() / Ucf[FieldType.ELEV.ordinal()]);
            nodes.get(i).setC0( nodes.get(i).getC0() / Ucf[FieldType.QUALITY.ordinal()]);
        }


        for (i=1; i<= getSections(SectType._JUNCTIONS); i++)
        {
            for(Demand d : nodes.get(i).getDemand())
            {
                d.setBase( d.getBase() / Ucf[FieldType.DEMAND.ordinal()]);
            }
        }


        ucf = Math.pow(Ucf[FieldType.FLOW.ordinal()],Qexp)/Ucf[FieldType.PRESSURE.ordinal()];
        for (i=1; i<= getSections(SectType._JUNCTIONS); i++)
            if (nodes.get(i).getKe() > 0.0) nodes.get(i).setKe(ucf/Math.pow(nodes.get(i).getKe(),Qexp));


        for (j=1; j<=maxTanks; j++)
        {
            i = tanks.get(j).getNode();
            Node ni = nodes.get(i);
            Tank tj = tanks.get(j);
            tj.setH0(ni.getElevation() + tj.getH0()/Ucf[FieldType.ELEV.ordinal()]);
            tj.setHMin(ni.getElevation() + tj.getHMin()/Ucf[FieldType.ELEV.ordinal()]);
            tj.setHMax(ni.getElevation() + tj.getHMax()/Ucf[FieldType.ELEV.ordinal()]);
            tj.setArea(Constants.PI*Math.pow(tj.getArea()/Ucf[FieldType.ELEV.ordinal()],2)/4.0);
            tj.setV0(tj.getV0() / Ucf[FieldType.VOLUME.ordinal()]);
            tj.setVmin(tj.getVmin()/Ucf[FieldType.VOLUME.ordinal()]);
            tj.setVmax(tj.getVmax()/Ucf[FieldType.VOLUME.ordinal()]);
            tj.setKb(tj.getKb()/Constants.SECperDAY);
            tj.setVolume(tj.getV0());
            tj.setConcentration(ni.getC0());
            tj.setV1max(tanks.get(j).getV1max() * tanks.get(j).getVmax());
        }


        Climit /= Ucf[FieldType.QUALITY.ordinal()];
        Ctol   /= Ucf[FieldType.QUALITY.ordinal()];

        Kbulk /= Constants.SECperDAY;
        Kwall /= Constants.SECperDAY;


        for (k=1; k<=maxLinks; k++)
        {
            Link lk = links.get(k);
            if (lk.getType().ordinal() <= LinkType.PIPE.ordinal())
            {
                if (Formflag  == FormType.DW) lk.setKc(lk.getKc() / (1000.0*Ucf[FieldType.ELEV.ordinal()]));
                lk.setDiameter(lk.getDiameter() / Ucf[FieldType.DIAM.ordinal()] );
                lk.setLenght(lk.getLenght() / Ucf[FieldType.LENGTH.ordinal()]);

                lk.setKm(0.02517*lk.getKm()/Math.pow(lk.getDiameter(),2)/Math.pow(lk.getDiameter(),2));

                lk.setKb(lk.getKb()/ Constants.SECperDAY);
                lk.setKw(lk.getKw()/ Constants.SECperDAY);
            }

            else if (lk.getType().equals(LinkType.PUMP))
            {
                i = Utilities.pumpIndex(this,k);
                Pump pm_i = pumps.get(i);

                if (pm_i.getPtype().equals(PumpType.CONST_HP))
                {
                    if (Unitsflag.equals(UnitsType.SI))pm_i.setR(pm_i.getR() / Ucf[FieldType.POWER.ordinal()]);
                }
                else
                {
                    if (pm_i.getPtype().equals(PumpType.POWER_FUNC))
                    {
                        pm_i.setH0( pm_i.getH0() / Ucf[FieldType.HEAD.ordinal()]);
                        pm_i.setR(pm_i.getR() * (Math.pow(Ucf[FieldType.FLOW.ordinal()],pm_i.getN()))/Ucf[FieldType.HEAD.ordinal()]);
                    }
                    pm_i.setQ0( pm_i.getQ0() / Ucf[FieldType.FLOW.ordinal()]);
                    pm_i.setQMax(pm_i.getQMax() / Ucf[FieldType.FLOW.ordinal()]);
                    pm_i.setHMax(pm_i.getHMax() / Ucf[FieldType.HEAD.ordinal()]);
                }
            }

            else
            {
                lk.setDiameter( lk.getDiameter() / Ucf[FieldType.DIAM.ordinal()]);
                lk.setKm(0.02517*lk.getKm()/Math.pow(lk.getDiameter(),2)/Math.pow(lk.getDiameter(),2));
                if (lk.getKc() != Constants.MISSING) switch (lk.getType())
                {
                    case FCV: lk.setKc(lk.getKc()/Ucf[FieldType.FLOW.ordinal()]); break;
                    case PRV:
                    case PSV:
                    case PBV: lk.setKc(lk.getKc()/Ucf[FieldType.PRESSURE.ordinal()]); break;
                }
            }

            resistance(lk);
        }

        for (i=1; i<= getSections(SectType._CONTROLS); i++)
        {
            Control c_i = controls.get(i);
            if ( (k = c_i.getLink()) == 0) continue;
            if ( (j = c_i.getNode()) > 0)
            {
                if (j > getSections(SectType._JUNCTIONS))
                    c_i.setGrade(nodes.get(j).getElevation() +
                            controls.get(i).getGrade()/Ucf[FieldType.ELEV.ordinal()]);
                else
                    c_i.setGrade(nodes.get(j).getElevation() + c_i.getGrade()/Ucf[FieldType.PRESSURE.ordinal()]);
            }

            if (c_i.getSetting() != Constants.MISSING) switch (links.get(k).getType())
            {
                case PRV:
                case PSV:
                case PBV:
                    c_i.setSetting(c_i.getSetting() / Ucf[FieldType.PRESSURE.ordinal()]);
                    break;
                case FCV:
                    c_i.setSetting(c_i.getSetting() / Ucf[FieldType.FLOW.ordinal()]);
            }
        }
    }




    void  resistance(Link lk)
    {
        double e,d,L;
        lk.setR(Constants.CSMALL);
        switch (lk.getType())
        {
            case CV:
            case PIPE:
                e = lk.getKc();
                d = lk.getDiameter();
                L = lk.getLenght();
                switch(Formflag)
                {
                    case HW: lk.setR(4.727*L/Math.pow(e,Hexp)/Math.pow(d,4.871));
                        break;
                    case DW: lk.setR(L/2.0/32.2/d/Math.pow(Constants.PI*Math.pow(d,2)/4.0,2));
                        break;
                    case CM: lk.setR( Math.pow(4.0*e/(1.49*Constants.PI*d*d),2)*
                            Math.pow((d/4.0),-1.333)*L);
                }
                break;

            case PUMP:
                lk.setR(Constants.CBIG);
                break;
        }
    }

    public void setMaxTanks(int i) {
        maxTanks = i;
    }
    public long getDur() {
        return Dur;
    }
    public UnitsType getUnitsflag() {
        return Unitsflag;
    }
    public FormType getFormflag() {
        return Formflag;
    }


    public Tank[] getTanksArray(){
        List<Tank> realTanks = new ArrayList<Tank>();
        for (Tank tank : tanks) {
            if(tank.getArea()!=0d)
                realTanks.add(tank);
        }
        return realTanks.toArray(new Tank[realTanks.size()]);
    }

    public Tank[] getReservoirsArray(){
        List<Tank> realReservoirs = new ArrayList<Tank>();
        for (Tank tank : tanks) {
            if(tank.getArea()==0d)
                realReservoirs.add(tank);
        }
        return realReservoirs.toArray(new Tank[realReservoirs.size()]);
    }

    public Label getLabel(int id)
    {
        return labels.get(id);
    }

    public FlowUnitsType getFlowUnits() {
        return Flowflag;
    }

    public double getSpGrav() {
        return SpGrav;
    }

    public PressUnitsType getPressureUnits() {
        return Pressflag;
    }

    public QualType getQualflag() {
        return Qualflag;
    }

    public String getChemName() {
        return ChemName;
    }


    public String getChemUnits() {
        return ChemUnits;
    }

    public long getPstep() {
        return Pstep;
    }

    public long getRstart() {
        return Rstart;
    }

    public long getQstep() {
        return Qstep;
    }

    public long getRstep() {
        return Rstep;
    }

    public void setHydTStep(long hydTStep) {
        Hstep=hydTStep;
    }

    public void setDuration(long durationTime) {
        Dur = durationTime;
    }

    public void setQualTStep(long qualTStep) {
        Qstep = qualTStep;
    }

    public void setRstep(long step){
        Rstep =  step;
    }
}
