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

package org.addition.epanet.network.io;

/**
 * Parse and report keywords.
 */
public class Keywords {
    public static String s_BACKDROP =   "[BACKDROP]";
    public static String s_CONTROLS =   "[CONTROLS]";

    public static String s_COORDS =     "[COORDINATES]";
    public static String s_CURVES =     "[CURVES]";

    public static String s_DEMANDS =    "[DEMANDS]";
    public static String s_EMITTERS =   "[EMITTERS]";
    public static String s_END =        "[END]";

    public static String s_ENERGY =     "[ENERGY]";
    public static String s_JUNCTIONS =  "[JUNCTIONS]";
    public static String s_LABELS =     "[LABELS]";
    public static String s_MIXING =     "[MIXING]";
    public static String s_OPTIONS =    "[OPTIONS]";
    public static String s_PATTERNS =   "[PATTERNS]";
    public static String s_PIPES =      "[PIPES]";
    public static String s_PUMPS =      "[PUMPS]";
    public static String s_QUALITY =    "[QUALITY]";
    public static String s_REACTIONS =  "[REACTIONS]";
    public static String s_REPORT =     "[REPORT]";
    public static String s_RESERVOIRS = "[RESERVOIRS]";
    public static String s_ROUGHNESS =  "[ROUGHNESS]";
    public static String s_RULES =      "[RULES]";

    public static String s_SOURCES =    "[SOURCES]";
    public static String s_STATUS =     "[STATUS]";
    public static String s_TAGS =       "[TAGS]";
    public static String s_TANKS =      "[TANKS]";
    public static String s_TIMES =      "[TIMES]";
    // INP sections types strings
    //public static String s_TITLE =      "[TITL";
    //public static String s_JUNCTIONS =  "[JUNC";
    //public static String s_RESERVOIRS = "[RESE";
    //public static String s_TANKS =      "[TANK";
    //public static String s_PIPES =      "[PIPE";
    //public static String s_PUMPS =      "[PUMP";
    //public static String s_VALVES =     "[VALV";
    //public static String s_CONTROLS =   "[CONT";
    //public static String s_RULES =      "[RULE";
    //public static String s_DEMANDS =    "[DEMA";
    //public static String s_SOURCES =    "[SOUR";
    //public static String s_EMITTERS =   "[EMIT";
    //public static String s_PATTERNS =   "[PATT";
    //public static String s_CURVES =     "[CURV";
    //public static String s_QUALITY =    "[QUAL";
    //public static String s_STATUS =     "[STAT";
    //public static String s_ROUGHNESS =  "[ROUG";
    //public static String s_ENERGY =     "[ENER";
    //public static String s_REACTIONS =  "[REAC";
    //public static String s_MIXING =     "[MIXI";
    //public static String s_REPORT =     "[REPO";
    //public static String s_TIMES =      "[TIME";
    //public static String s_OPTIONS =    "[OPTI";
    //public static String s_COORDS =     "[COOR";
    //public static String s_VERTICES =   "[VERT";
    //public static String s_LABELS =     "[LABE";
    //public static String s_BACKDROP =   "[BACK";
    //public static String s_TAGS =       "[TAGS";
    //public static String s_END =        "[END";
    public static String s_TITLE =      "[TITLE]";
    public static String s_VALVES =     "[VALVES]";
    public static String s_VERTICES =   "[VERTICES]";
    public static String t_ABOVE      ="above";

    public static String t_ACTIVE      ="active";
    public static String t_BACKDROP = "Backdrop";

    public static String t_BELOW      ="below";
    public static String t_CHEMICAL   ="Chemical";
    public static String t_CLOSED      ="closed";
    public static String  t_CM         ="Chezy-Manning";
    public static String t_CONTINUED  =" (continued)";

    public static String t_CONTROL    ="Control";
    public static String t_COORD      = "Coordinate";
    public static String t_CURVE      ="Curve";
    public static String t_DEMAND =     "Demand";
    public static String t_DEMANDFOR  ="Demand for Node";
    public static String t_DIAM =       "Diameter";
    public static String t_DIFFER     ="DIFFERENTIAL";
    public static String  t_DW         ="Darcy-Weisbach";
    public static String t_ELEV =       "Elevation";
    public static String t_EMITTER    ="Emitter";
    public static String t_EMPTYING    ="emptying";

    public static String t_END      = "End";
    public static String t_ENERGY     ="Energy";
    public static String t_FILLING     ="filling";

    public static String t_FLOW =       "Flow";
    public static String t_FRICTION =   "F-Factor";
    public static String t_FUNCCALL   ="function call";
    public static String t_HALTED     =" EXECUTION HALTED.";
    public static String t_HEAD =       "Head";
    public static String t_HEADLOSS =   "Headloss";
    public static String  t_HW         ="Hazen-Williams";
    public static String t_JUNCTION   ="Junction";
    public static String t_LABEL    = "Label";
    public static String t_LENGTH =     "Length";
    public static String  t_LINKID     ="Link";

    public static String t_LINKQUAL =   "Quality";
    public static String t_LINKSTATUS = "State";
    public static String t_MIXING     ="Mixing";

    public static String  t_NODEID     ="Node";
    public static String t_OPEN        ="open";
    public static String t_OPTION     ="Options";
    public static String t_PATTERN    ="Pattern";

    public static String  t_PERDAY     ="/day";
    public static String t_perM3      ="  /m3";
    public static String t_perMGAL    ="/Mgal";
    public static String t_PIPE       ="Pipe";
    public static String t_PRESSURE =   "Pressure";
    public static String t_PUMP       ="Pump";
    public static String t_QUALITY =    "Quality";
    public static String t_REACTION   ="Reaction";
    public static String t_REACTRATE =  "Reaction";
    public static String t_REPORT     ="Report";
    public static String t_RESERVOIR  ="Reservoir";
    public static String t_ROUGHNESS  ="Roughness";

    public static String t_RULE       ="Rule";
    public static String t_RULES_SECT ="[RULES] section";
    public static String t_SETTING =    "Setting";
    public static String t_SOURCE     ="Source";
    public static String t_STATUS     ="Status";

    public static String t_TAG      = "Tag";
    public static String t_TANK       ="Tank";
    public static String t_TEMPCLOSED  ="temporarily closed";
    public static String t_TIME       ="Times";
    public static String t_TITLE = "Title";
    public static String t_VALVE      ="Valve";
    public static String t_VELOCITY =   "Velocity";
    public static String t_VERTICE  = "Vertice";
    public static String t_XFCV        ="open but cannot deliver flow";
    public static String t_XFLOW       ="open but exceeds maximum flow";
    public static String t_XHEAD       ="closed because cannot deliver head";

    public static String t_XPRESSURE   ="open but cannot deliver pressure";
    public static String  u_AFD        ="a-f/d";

    // Units
    public static String  u_CFS        ="cfs";
    public static String  u_CMD        ="m3/d";
    public static String  u_CMH        ="m3/h";
    public static String  u_FEET       ="ft";
    public static String  u_FTperSEC   ="fps";
    public static String  u_GPM        ="gpm";

    public static String  u_HOURS      ="hrs";
    public static String  u_HP         ="hp";
    public static String  u_IMGD       ="Imgd";
    public static String  u_INCHES     ="in";
    public static String  u_KPA        ="kPa";
    public static String  u_KW         ="kw";

    public static String  u_LPM        ="Lpm";
    public static String  u_LPS        ="L/s";
    public static String  u_METERS     ="m";
    public static String  u_MGD        ="mgd";

    public static String  u_MGperL     ="mg/L";
    public static String  u_MINUTES    ="min";

    public static String  u_MLD        ="ML/d";
    public static String  u_MMETERS    ="mm";
    public static String  u_MperSEC    ="m/s";
    public static String  u_per1000FT  ="/kft";

    public static String  u_per1000M   ="/km";
    public static String  u_PERCENT    ="%";
    public static String  u_PSI        ="psi";
    public static String  u_SQFTperSEC ="sq ft/sec";

    public static String  u_SQMperSEC  ="sq m/sec";
    public static String  u_UGperL     ="ug/L";
    public static String   w_2COMP      ="2COMP";
    public static String   w_ABOVE      ="ABOVE";
    public static String   w_ACCURACY   ="ACCU";

    public static String   w_ACTIVE     ="ACTIVE";
    public static String   w_ADD        ="ADD";
    public static String   w_AFD        ="AFD";

    public static String   w_AGE        ="AGE";

    public static String   w_ALL        ="ALL";
    public static String   w_AM         ="AM";
    public static String   w_AVG        ="AVERAGE";
    public static String   w_BELOW      ="BELOW";
    public static String   w_BULK       ="BULK";
    public static String   w_CFS        ="CFS";
    public static String   w_CHECKFREQ  ="CHECKFREQ";
    public static String   w_CHEM       ="CHEM";
    public static String   w_CLOCKTIME  ="CLOCKTIME";
    public static String   w_CLOSED     ="CLOSED";
    public static String   w_CM         ="C-M";
    public static String   w_CMD        ="CMD";
    public static String   w_CMH        ="CMH";
    public static String   w_CONCEN     ="CONCEN";
    public static String   w_CONTINUE   ="CONT";
    public static String   w_CURVE      ="CURV";
    public static String   w_CV         ="CV";
    public static String   w_DAMPLIMIT  ="DAMPLIMIT";
    public static String   w_DAYS       ="DAY";
    public static String   w_DEMAND     ="DEMA";
    public static String   w_DIAM       ="DIAM";
    public static String   w_DIFFUSIVITY="DIFF";
    public static String   w_DMNDCHARGE ="DEMAN";
    public static String   w_DRAINTIME  ="DRAI";
    public static String   w_DURATION   ="DURA";
    public static String   w_DW         ="D-W";
    public static String   w_EFFIC      ="EFFI";
    public static String   w_ELEV       ="ELEV";
    public static String   w_EMITTER    ="EMIT";

    public static String   w_ENERGY     ="ENER";
    public static String   w_FCV        ="FCV";
    public static String   w_FIFO       ="FIFO";

    public static String   w_FILE       ="FILE";
    public static String   w_FILLTIME   ="FILL";
    public static String   w_FLOW       ="FLOW";
    public static String   w_FLOWPACED  ="FLOWPACED";
    public static String   w_FULL       ="FULL";
    public static String   w_GLOBAL     ="GLOB";
    public static String   w_GPM        ="GPM";
    public static String   w_GPV        ="GPV";
    public static String   w_GRADE      ="GRADE";
    public static String   w_HEAD       ="HEAD";
    public static String   w_HEADLOSS   ="HEADL";
    public static String   w_HOURS      ="HOU";
    public static String   w_HTOL       ="HTOL";
    public static String   w_HW         ="H-W";
    public static String   w_HYDRAULIC  ="HYDR";

    public static String   w_IMGD       ="IMGD";
    public static String   w_IS         ="IS";
    public static String   w_JUNC       ="Junc";
    public static String   w_KPA        ="KPA";
    public static String   w_LEVEL      ="LEVEL";
    public static String   w_LIFO       ="LIFO";
    public static String   w_LIMITING   ="LIMIT";
    public static String   w_LINK       ="LINK";
    public static String   w_LPM        ="LPM";
    public static String   w_LPS        ="LPS";
    public static String   w_MAP        ="MAP";
    public static String   w_MASS       ="MASS";
    public static String   w_MAX        ="MAXIMUM";
    public static String   w_MAXCHECK   ="MAXCHECK";
    public static String   w_MESSAGES   ="MESS";
    public static String   w_METERS     ="METERS";
    public static String   w_MGD        ="MGD";
    public static String   w_MIN        ="MINIMUM";
    public static String   w_MINIMUM    ="MINI";
    public static String   w_MINUTES    ="MIN";
    public static String   w_MIXED      ="MIXED";
    public static String   w_MLD        ="MLD";
    public static String   w_MULTIPLY   ="MULT";
    public static String   w_NO         ="NO";
    public static String   w_NODE       ="NODE";
    public static String   w_NONE       ="NONE";
    public static String   w_NOT        ="NOT";
    public static String   w_OPEN       ="OPEN";
    public static String   w_ORDER      ="ORDER";
    public static String   w_PAGE       ="PAGE";
    public static String   w_PATTERN    ="PATT";
    public static String   w_PBV        ="PBV";
    public static String   w_PIPE       ="Pipe";
    public static String   w_PM         ="PM";
    public static String   w_POWER      ="POWE";

    public static String   w_PRECISION  ="PREC";
    public static String   w_PRESSURE   ="PRES";
    public static String   w_PRICE      ="PRICE";
    public static String   w_PRV        ="PRV";
    public static String   w_PSI        ="PSI";
    public static String   w_PSV        ="PSV";
    public static String   w_PUMP       ="Pump";
    public static String   w_QTOL       ="QTOL";
    public static String   w_QUALITY    ="QUAL";
    public static String   w_RANGE      ="RANGE";
    public static String   w_REPORT     ="REPO";
    public static String   w_RESERV     ="Reser";
    public static String   w_ROUGHNESS  ="ROUG";
    public static String   w_RQTOL      ="RQTOL";
    public static String   w_RULE       ="RULE";
    public static String   w_SAVE       ="SAVE";
    public static String   w_SECONDS    ="SEC";
    public static String   w_SEGMENTS   ="SEGM";
    public static String   w_SETPOINT   ="SETPOINT";
    public static String   w_SETTING    ="SETT";
    public static String   w_SI         ="SI";
    public static String   w_SPECGRAV   ="SPEC";
    public static String   w_SPEED      ="SPEE";
    public static String   w_START      ="STAR";
    public static String   w_STATISTIC  ="STAT";
    public static String   w_STATUS     ="STATUS";
    public static String   w_STOP       ="STOP";
    public static String   w_SUMMARY    ="SUMM";
    public static String   w_SYSTEM     ="SYST";

    public static String   w_TANK       ="Tank";
    public static String   w_TCV        ="TCV";
    public static String   w_TIME       ="TIME";

    public static String   w_TOLERANCE  ="TOLER";
    public static String   w_TRACE      ="TRACE";
    public static String   w_TRIALS     ="TRIAL";

    public static String   w_UNBALANCED ="UNBA";
    public static String   w_UNITS      ="UNIT";
    public static String   w_USE        ="USE";
    public static String   w_VALVE      ="Valve";
    public static String   w_VELOCITY   ="VELO";
    public static String   w_VERIFY     ="VERI";
    public static String   w_VISCOSITY  ="VISC";
    public static String   w_VOLUME     ="VOLU";
    public static String   w_WALL       ="WALL";
    public static String   w_YES        ="YES";

    public static String wr_ABOVE    ="ABOVE";
    public static String wr_ACTIVE   ="ACTIVE";
    public static String wr_AND      ="AND";
    public static String wr_BELOW    ="BELOW";
    public static String wr_CLOCKTIME="CLOCKTIME";
    public static String wr_CLOSED   ="CLOSED";
    public static String wr_DEMAND   ="DEMA";
    public static String wr_DRAINTIME="DRAI";
    public static String wr_ELSE     ="ELSE";
    public static String wr_FILLTIME ="FILL";
    public static String wr_FLOW     ="FLOW";
    public static String wr_GRADE    ="GRADE";
    public static String wr_HEAD     ="HEAD";
    public static String wr_IF       ="IF";
    public static String wr_IS       ="IS";
    public static String wr_JUNC     ="Junc";
    public static String wr_LEVEL    ="LEVEL";
    public static String wr_LINK     ="LINK";
    public static String wr_NODE     ="NODE";
    public static String wr_NOT      ="NOT";
    public static String wr_OPEN     ="OPEN";
    public static String wr_OR       ="OR";
    public static String wr_PIPE     ="Pipe";
    public static String wr_POWER    ="POWE";
    public static String wr_PRESSURE ="PRES";
    public static String wr_PRIORITY ="PRIO";
    public static String wr_PUMP     ="Pump";
    public static String wr_RESERV   ="Reser";
    public static String wr_RULE     ="RULE";
    public static String wr_SETTING  ="SETT";
    public static String wr_STATUS   ="STATUS";
    public static String wr_SYSTEM   ="SYST";
    public static String wr_TANK     ="Tank";
    public static String wr_THEN     ="THEN";
    public static String wr_TIME     ="TIME";
    public static String wr_VALVE    ="Valve";

}
