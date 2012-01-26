package org.addition.epanetold.Types;

public class EnumVariables {

    public static String  SEPSTR = " \t\n\r"; /* Token separator characters */

    /* Water quality analysis option:  */
    public enum QualType{
        NONE,          /*    no quality analysis              */
        CHEM,          /*    analyze a chemical               */
        AGE,           /*    analyze water age                */
        TRACE          /*    trace % of flow from a source    */
    }

    /* Type of node: */
    public enum NodeType{
        JUNC,          /*    junction                         */
        RESERV,        /*    reservoir                        */
        TANK           /*    tank                             */
    }

    /* Type of link: */
    public enum LinkType{
        CV,            /*    pipe with check valve            */
        PIPE,          /*    regular pipe                     */
        PUMP,          /*    pump                             */
        PRV,           /*    pressure reducing valve          */
        PSV,           /*    pressure sustaining valve        */
        PBV,           /*    pressure breaker valve           */
        FCV,           /*    flow control valve               */
        TCV,           /*    throttle control valve           */
        GPV            /*    general purpose valve            */
    }

    /* Type of curve: */
    public enum CurveType{
        V_CURVE,       /*    volume curve                      */
        P_CURVE,       /*    pump curve                        */
        E_CURVE,       /*    efficiency curve                  */
        H_CURVE        /*    head loss curve                   */
    }

    /* Type of pump curve: */
    public enum PumpType{
        CONST_HP,      /*    constant horsepower              */
        POWER_FUNC,    /*    power function                   */
        CUSTOM,        /*    user-defined custom curve        */
        NOCURVE
    }

    /* Type of source quality input */
    public enum SourceType{
        CONCEN,        /*    inflow concentration             */
        MASS,          /*    mass inflow booster              */
        SETPOINT,      /*    setpoint booster                 */
        FLOWPACED      /*    flow paced booster               */
    }

    /* Control condition type: */
    public enum ControlType{
        LOWLEVEL,      /*    act when grade below set level   */
        HILEVEL,       /*    act when grade above set level   */
        TIMER,         /*    act when set time reached        */
        TIMEOFDAY      /*    act when time of day occurs      */
    }

    /* Link/Tank status: */
    public enum StatType{
        XHEAD,        /*   pump cannot deliver head (closed) */
        TEMPCLOSED,   /*   temporarily closed                */
        CLOSED,       /*   closed                            */
        OPEN,         /*   open                              */
        ACTIVE,       /*   valve active (partially open)     */
        XFLOW,        /*   pump exceeds maximum flow         */
        XFCV,         /*   FCV cannot supply flow            */
        XPRESSURE,    /*   valve cannot supply pressure      */
        FILLING,      /*   tank filling                      */
        EMPTYING      /*   tank emptying                     */
    }

    /* Head loss formula: */
    public enum FormType{
        HW,           /*   Hazen-Williams                    */
        DW,           /*   Darcy-Weisbach                    */
        CM            /*   Chezy-Manning                     */
    }

    /* Unit system: */
    public enum UnitsType{
        US,           /*   US                                */
        SI            /*   SI (metric)                       */
    }

    /* Flow units: */
    public enum FlowUnitsType{
        CFS,          /*   cubic feet per second             */
        GPM,          /*   gallons per minute                */
        MGD,          /*   million gallons per day           */
        IMGD,         /*   imperial million gal. per day     */
        AFD,          /*   acre-feet per day                 */
        LPS,          /*   liters per second                 */
        LPM,          /*   liters per minute                 */
        MLD,          /*   megaliters per day                */
        CMH,          /*   cubic meters per hour             */
        CMD           /*   cubic meters per day              */
    }

    /* Pressure units: */
    public enum PressUnitsType{
        PSI,          /*   pounds per square inch            */
        KPA,          /*   kiloPascals                       */
        METERS        /*   meters                            */
    }

    /* Range limits: */
    public enum RangeType{
        LOW,          /*   lower limit                       */
        HI,           /*   upper limit                       */
        PREC          /*   precision                         */
    }

    /* Tank mixing regimes */
    public enum MixType{
        MIX1,         /*   1-compartment model               */
        MIX2,         /*   2-compartment model               */
        FIFO,         /*   First in, first out model         */
        LIFO          /*   Last in, first out model          */
    }



    /* Time series statistics */
    public enum TstatType{
        SERIES,       /*   none                              */
        AVG,          /*   time-averages                     */
        MIN,          /*   minimum values                    */
        MAX,          /*   maximum values                    */
        RANGE         /*   max - min values                  */
    }


    /* (equals # items enumed below) */
    /* Network variables: */
    public enum FieldType{
        ELEV,         /*   nodal elevation                   */
        DEMAND,       /*   nodal demand flow                 */
        HEAD,         /*   nodal hydraulic head              */
        PRESSURE,     /*   nodal pressure                    */
        QUALITY,      /*   nodal water quality               */

        LENGTH,       /*   link length                       */
        DIAM,         /*   link diameter                     */
        FLOW,         /*   link flow rate                    */
        VELOCITY,     /*   link flow velocity                */
        HEADLOSS,     /*   link head loss                    */
        LINKQUAL,     /*   avg. water quality in link        */
        STATUS,       /*   link status                       */
        SETTING,      /*   pump/valve setting                */
        REACTRATE,    /*   avg. reaction rate in link        */
        FRICTION,     /*   link friction factor              */

        POWER,        /*   pump power output                 */
        TIME,         /*   simulation time                   */
        VOLUME,       /*   tank volume                       */
        CLOCKTIME,    /*   simulation time of day            */
        FILLTIME,     /*   time to fill a tank               */
        DRAINTIME     /*   time to drain a tank              */
    }

    public enum StatFlag{
        FALSE,
        TRUE,
        FULL
    }

    public enum ReportFlag{
        FALSE, //0
        TRUE, //1
        SOME //2
    }
    public enum SectType {
        _TITLE, _JUNCTIONS, _RESERVOIRS, _TANKS, _PIPES, _PUMPS,
        _VALVES, _CONTROLS, _RULES, _DEMANDS, _SOURCES, _EMITTERS,
        _PATTERNS, _CURVES, _QUALITY, _STATUS, _ROUGHNESS, _ENERGY,
        _REACTIONS, _MIXING, _REPORT, _TIMES, _OPTIONS,
        _COORDS, _VERTICES, _LABELS, _BACKDROP, _TAGS, _END
    }

    /* Type of table heading */
    public enum HdrType{
        STATHDR,      /*  Hydraulic Status       */
        ENERHDR,      /*  Energy Usage           */
        NODEHDR,      /*  node Results           */
        LINKHDR       /*  Link Results           */
    }

    /* Hydraulics solution option: */
    public enum Hydtype                    {
        USE,           /*    use from previous run            */
        SAVE,          /*    save after current run           */
        SCRATCH        /*    use temporary file               */
    }

    public static double MISSING =  -1.E10d;
    public static double TINY =     1.E-6d;
    public static long  SECperDAY = 86400;

    /* ------------ Keyword Dictionary ---------- */
    public static String   w_USE        ="USE";
    public static String   w_SAVE       ="SAVE";

    public static String   w_NONE       ="NONE";
    public static String   w_ALL        ="ALL";

    public static String   w_CHEM       ="CHEM";
    public static String   w_AGE        ="AGE";
    public static String   w_TRACE      ="TRACE";

    public static String   w_SYSTEM     ="SYST";
    public static String   w_JUNC       ="Junc";
    public static String   w_RESERV     ="Reser";
    public static String   w_TANK       ="Tank";
    public static String   w_CV         ="CV";
    public static String   w_PIPE       ="Pipe";
    public static String   w_PUMP       ="Pump";
    public static String   w_VALVE      ="Valve";
    public static String   w_PRV        ="PRV";
    public static String   w_PSV        ="PSV";
    public static String   w_PBV        ="PBV";
    public static String   w_FCV        ="FCV";
    public static String   w_TCV        ="TCV";
    public static String   w_GPV        ="GPV";

    public static String   w_OPEN       ="OPEN";
    public static String   w_CLOSED     ="CLOSED";
    public static String   w_ACTIVE     ="ACTIVE";
    public static String   w_TIME       ="TIME";
    public static String   w_ABOVE      ="ABOVE";
    public static String   w_BELOW      ="BELOW";
    public static String   w_PRECISION  ="PREC";
    public static String   w_IS         ="IS";
    public static String   w_NOT        ="NOT";

    public static String   w_ADD        ="ADD";
    public static String   w_MULTIPLY   ="MULT";

    public static String   w_LIMITING   ="LIMIT";
    public static String   w_ORDER      ="ORDER";
    public static String   w_GLOBAL     ="GLOB";
    public static String   w_BULK       ="BULK";
    public static String   w_WALL       ="WALL";

    public static String   w_PAGE       ="PAGE";
    public static String   w_STATUS     ="STATUS";
    public static String   w_SUMMARY    ="SUMM";
    public static String   w_MESSAGES   ="MESS";
    public static String   w_ENERGY     ="ENER";
    public static String   w_NODE       ="NODE";
    public static String   w_LINK       ="LINK";
    public static String   w_FILE       ="FILE";
    public static String   w_YES        ="YES";
    public static String   w_NO         ="NO";
    public static String   w_FULL       ="FULL";

    public static String   w_HW         ="H-W";
    public static String   w_DW         ="D-W";
    public static String   w_CM         ="C-M";

    public static String   w_CFS        ="CFS";
    public static String   w_GPM        ="GPM";
    public static String   w_MGD        ="MGD";
    public static String   w_IMGD       ="IMGD";
    public static String   w_AFD        ="AFD";
    public static String   w_LPS        ="LPS";
    public static String   w_LPM        ="LPM";
    public static String   w_MLD        ="MLD";
    public static String   w_CMH        ="CMH";
    public static String   w_CMD        ="CMD";
    public static String   w_SI         ="SI";

    public static String   w_PSI        ="PSI";
    public static String   w_KPA        ="KPA";
    public static String   w_METERS     ="METERS";

    public static String   w_ELEV       ="ELEV";
    public static String   w_DEMAND     ="DEMA";
    public static String   w_PRESSURE   ="PRES";
    public static String   w_QUALITY    ="QUAL";

    public static String   w_DIAM       ="DIAM";
    public static String   w_FLOW       ="FLOW";
    public static String   w_ROUGHNESS  ="ROUG";
    public static String   w_VELOCITY   ="VELO";
    public static String   w_HEADLOSS   ="HEADL";
    public static String   w_SETTING    ="SETT";
    public static String   w_VOLUME     ="VOLU";
    public static String   w_CLOCKTIME  ="CLOCKTIME";
    public static String   w_FILLTIME   ="FILL";
    public static String   w_DRAINTIME  ="DRAI";
    public static String   w_GRADE      ="GRADE";
    public static String   w_LEVEL      ="LEVEL";

    public static String   w_DURATION   ="DURA";
    public static String   w_HYDRAULIC  ="HYDR";
    public static String   w_MINIMUM    ="MINI";
    public static String   w_REPORT     ="REPO";
    public static String   w_START      ="STAR";

    public static String   w_UNITS      ="UNIT";
    public static String   w_MAP        ="MAP";
    public static String   w_VERIFY     ="VERI";
    public static String   w_VISCOSITY  ="VISC";
    public static String   w_DIFFUSIVITY="DIFF";
    public static String   w_SPECGRAV   ="SPEC";
    public static String   w_TRIALS     ="TRIAL";
    public static String   w_ACCURACY   ="ACCU";
    public static String   w_SEGMENTS   ="SEGM";
    public static String   w_TOLERANCE  ="TOLER";
    public static String   w_EMITTER    ="EMIT";

    public static String   w_PRICE      ="PRICE";
    public static String   w_DMNDCHARGE ="DEMAN";

    public static String   w_HTOL       ="HTOL";
    public static String   w_QTOL       ="QTOL";
    public static String   w_RQTOL      ="RQTOL";
    public static String   w_CHECKFREQ  ="CHECKFREQ";
    public static String   w_MAXCHECK   ="MAXCHECK";
    public static String   w_DAMPLIMIT  ="DAMPLIMIT";                                            //(2.00.12 - LR)

    public static String   w_SECONDS    ="SEC";
    public static String   w_MINUTES    ="MIN";
    public static String   w_HOURS      ="HOU";
    public static String   w_DAYS       ="DAY";
    public static String   w_AM         ="AM";
    public static String   w_PM         ="PM";

    public static String   w_CONCEN     ="CONCEN";
    public static String   w_MASS       ="MASS";
    public static String   w_SETPOINT   ="SETPOINT";
    public static String   w_FLOWPACED  ="FLOWPACED";

    public static String   w_PATTERN    ="PATT";
    public static String   w_CURVE      ="CURV";

    public static String   w_EFFIC      ="EFFI";
    public static String   w_HEAD       ="HEAD";
    public static String   w_POWER      ="POWE";
    public static String   w_SPEED      ="SPEE";

    public static String   w_MIXED      ="MIXED";
    public static String   w_2COMP      ="2COMP";
    public static String   w_FIFO       ="FIFO";
    public static String   w_LIFO       ="LIFO";

    public static String   w_STATISTIC  ="STAT";
    public static String   w_AVG        ="AVERAGE";
    public static String   w_MIN        ="MINIMUM";
    public static String   w_MAX        ="MAXIMUM";
    public static String   w_RANGE      ="RANGE";

    public static String   w_UNBALANCED ="UNBA";
    public static String   w_STOP       ="STOP";
    public static String   w_CONTINUE   ="CONT";

    public static String   w_RULE       ="RULE";

    //text.h,186
    /* ---------Input Section Names ---------- */
    public static String s_TITLE = "[TITL";
    public static String s_JUNCTIONS =  "[JUNC";
    public static String s_RESERVOIRS = "[RESE";
    public static String s_TANKS =      "[TANK";
    public static String s_PIPES =      "[PIPE";
    public static String s_PUMPS =      "[PUMP";
    public static String s_VALVES =     "[VALV";
    public static String s_CONTROLS =   "[CONT";
    public static String s_RULES =      "[RULE";
    public static String s_DEMANDS =    "[DEMA";
    public static String s_SOURCES =    "[SOUR";
    public static String s_EMITTERS =   "[EMIT";
    public static String s_PATTERNS =   "[PATT";
    public static String s_CURVES =     "[CURV";
    public static String s_QUALITY =    "[QUAL";
    public static String s_STATUS =     "[STAT";
    public static String s_ROUGHNESS =  "[ROUG";
    public static String s_ENERGY =     "[ENER";
    public static String s_REACTIONS =  "[REAC";
    public static String s_MIXING =     "[MIXI";
    public static String s_REPORT =     "[REPO";
    public static String s_TIMES =      "[TIME";
    public static String s_OPTIONS =    "[OPTI";
    public static String s_COORDS =     "[COOR";
    public static String s_VERTICES =   "[VERT";
    public static String s_LABELS =     "[LABE";
    public static String s_BACKDROP =   "[BACK";
    public static String s_TAGS =       "[TAGS";
    public static String s_END =        "[END";

    public static String t_ELEV =       "Elevation";
    public static String t_DEMAND =     "Demand";
    public static String t_HEAD =       "Head";
    public static String t_PRESSURE =   "Pressure";
    public static String t_QUALITY =    "Quality";
    public static String t_LENGTH =     "Length";
    public static String t_DIAM =       "Diameter";
    public static String t_FLOW =       "Flow";
    public static String t_VELOCITY =   "Velocity";
    public static String t_HEADLOSS =   "Headloss";
    public static String t_LINKQUAL =   "Quality";
    public static String t_LINKSTATUS = "State";
    public static String t_SETTING =    "Setting";
    public static String t_REACTRATE =  "Reaction";
    public static String t_FRICTION =   "F-Factor";


    public static String t_JUNCTION   ="Junction"          ;
    public static String t_RESERVOIR  ="Reservoir"         ;
    public static String t_TANK       ="Tank"              ;
    public static String t_PIPE       ="Pipe"              ;
    public static String t_PUMP       ="Pump"              ;
    public static String t_VALVE      ="Valve"             ;
    public static String t_CONTROL    ="Control"           ;
    public static String t_RULE       ="Rule"              ;
    public static String t_DEMANDFOR  ="Demand for Node"   ;
    public static String t_SOURCE     ="Source"            ;
    public static String t_EMITTER    ="Emitter"           ;
    public static String t_PATTERN    ="Pattern"           ;
    public static String t_CURVE      ="Curve"             ;
    public static String t_STATUS     ="Status"            ;
    public static String t_ROUGHNESS  ="Roughness"         ;
    public static String t_ENERGY     ="Energy"            ;
    public static String t_REACTION   ="Reaction"          ;
    public static String t_MIXING     ="Mixing"            ;
    public static String t_REPORT     ="Report"            ;
    public static String t_TIME       ="Times"             ;
    public static String t_OPTION     ="Options"           ;
    public static String t_RULES_SECT ="[RULES] section"   ;
    public static String t_HALTED     =" EXECUTION HALTED.";
    public static String t_FUNCCALL   ="function call"     ;
    public static String t_CONTINUED  =" (continued)"      ;
    public static String t_perM3      ="  /m3"             ;
    public static String t_perMGAL    ="/Mgal"             ;
    public static String t_DIFFER     ="DIFFERENTIAL"      ;

    // Units
    public static String  u_CFS        ="cfs";
    public static String  u_GPM        ="gpm";
    public static String  u_AFD        ="a-f/d";
    public static String  u_MGD        ="mgd";
    public static String  u_IMGD       ="Imgd";
    public static String  u_LPS        ="L/s";
    public static String  u_LPM        ="Lpm";
    public static String  u_CMH        ="m3/h";
    public static String  u_CMD        ="m3/d";
    public static String  u_MLD        ="ML/d";
    public static String  u_MGperL     ="mg/L";
    public static String  u_UGperL     ="ug/L";
    public static String  u_HOURS      ="hrs";
    public static String  u_MINUTES    ="min";
    public static String  u_PERCENT    ="% from";
    public static String  u_METERS     ="m";
    public static String  u_MMETERS    ="mm";
    public static String  u_MperSEC    ="m/s";
    public static String  u_SQMperSEC  ="sq m/sec";
    public static String  u_per1000M   ="/1000m";
    public static String  u_KW         ="kw";
    public static String  u_FEET       ="ft";
    public static String  u_INCHES     ="in";
    public static String  u_PSI        ="psi";
    public static String  u_KPA        ="kPa";
    public static String  u_FTperSEC   ="fps";
    public static String  u_SQFTperSEC ="sq ft/sec";
    public static String  u_per1000FT  ="/1000ft";
    public static String  u_HP         ="hp";

    public static String  t_NODEID    ="Node";
    public static String  t_LINKID    ="Link";
    public static String  t_PERDAY    ="/day";

    public static String  t_HW        = "Hazen-Williams";
    public static String  t_DW        = "Darcy-Weisbach";
    public static String  t_CM        = "Chezy-Manning";

    public static String t_XHEAD      ="closed because cannot deliver head";
    public static String t_TEMPCLOSED ="temporarily closed";
    public static String t_CLOSED     ="closed";
    public static String t_OPEN       ="open";
    public static String t_ACTIVE     ="active";
    public static String t_XFLOW      ="open but exceeds maximum flow";
    public static String t_XFCV       ="open but cannot deliver flow";
    public static String t_XPRESSURE  ="open but cannot deliver pressure";
    public static String t_FILLING    ="filling";
    public static String t_EMPTYING   ="emptying";


    public static String [] SectTxt
            = {s_TITLE,  s_JUNCTIONS, s_RESERVOIRS,
            s_TANKS,     s_PIPES,     s_PUMPS,
            s_VALVES,    s_CONTROLS,  s_RULES,
            s_DEMANDS,   s_SOURCES,   s_EMITTERS,
            s_PATTERNS,  s_CURVES,    s_QUALITY,
            s_STATUS,    s_ROUGHNESS, s_ENERGY,
            s_REACTIONS, s_MIXING,    s_REPORT,
            s_TIMES,     s_OPTIONS,   s_COORDS,
            s_VERTICES,  s_LABELS,    s_BACKDROP,
            s_TAGS,      s_END};

    public static String [] MixTxt = {w_MIXED,
            w_2COMP,
            w_FIFO,
            w_LIFO};

    public static String [] Fldname
            ={t_ELEV,    t_DEMAND,    t_HEAD,
            t_PRESSURE,  t_QUALITY,   t_LENGTH,
            t_DIAM,      t_FLOW,      t_VELOCITY,
            t_HEADLOSS,  t_LINKQUAL,  t_LINKSTATUS,
            t_SETTING,   t_REACTRATE, t_FRICTION,
            "", "", "", "", "", ""};

    public static String [] RptSectTxt
            = {null,    t_JUNCTION, t_RESERVOIR,
            t_TANK,     t_PIPE,     t_PUMP,
            t_VALVE,    t_CONTROL,  t_RULE,
            t_DEMANDFOR,t_SOURCE,   t_EMITTER,
            t_PATTERN,  t_CURVE,    t_QUALITY,
            t_STATUS,   t_ROUGHNESS,t_ENERGY,
            t_REACTION, t_MIXING,   t_REPORT,
            t_TIME,     t_OPTION};

    public static String []  RptFlowUnitsTxt
            = {u_CFS,
            u_GPM,
            u_MGD,
            u_IMGD,
            u_AFD,
            u_LPS,
            u_LPM,
            u_MLD,
            u_CMH,
            u_CMD};

    public static String [] NodeTxt
            = {t_JUNCTION,
            t_RESERVOIR,
            t_TANK};

    public static String [] LinkTxt
            = {w_CV,
            w_PIPE,
            w_PUMP,
            w_PRV,
            w_PSV,
            w_PBV,
            w_FCV,
            w_TCV,
            w_GPV};

    public static String [] RptFormTxt      = {t_HW,
                           t_DW,
                           t_CM};

    public static String [] TstatTxt        = {w_NONE,
                           w_AVG,
                           w_MIN,
                           w_MAX,
                           w_RANGE};

     public static String [] StatTxt  = {t_XHEAD,
                           t_TEMPCLOSED,
                           t_CLOSED,
                           t_OPEN,
                           t_ACTIVE,
                           t_XFLOW,
                           t_XFCV,
                           t_XPRESSURE,
                           t_FILLING,
                           t_EMPTYING};

}


