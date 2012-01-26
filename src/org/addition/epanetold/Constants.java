package org.addition.epanetold;


public class Constants {

    public static double        PI = 3.141592654;

    public static int           MAXCOUNT = 10;              // Max. # of disconnected nodes listed
    public static int           MAXTITLE = 3;               // Max. # title lines
    public static final int     MAXERRS = 10;               // Max. input errors reported
    public static final int     MAXMSG = 79;
    public static final int     MAXID  = 31;
    public static final int MAXFNAME = 259;

    public static final int     CODEVERSION =20012;
    public static final int     MAGICNUMBER =516114521;
    public static final int     VERSION     =200;

    public static final double  QZERO  =1.e-6d;             // Equivalent to zero flow
    public static final double  CBIG   =1.e8d;              // Big coefficient
    public static final double  CSMALL =1.e-6d;             // Small coefficient

    public static final int     MAXITER  =200;              // Default max. # hydraulic iterations
    public static final double  HACC     =0.001;            // Default hydraulics convergence ratio
    public static final double  HTOL     =0.0005;           // Default hydraulic head tolerance (ft)

    public static final double  QTOL     =0.0001;           // Default flow rate tolerance (cfs)

    public static final double  AGETOL   =0.01;             // Default water age tolerance (hrs)
    public static final double  CHEMTOL  =0.01;             // Default concentration tolerance
    public static final int     PAGESIZE =0;                // Default uses no page breaks
    public static final double  SPGRAV   =1.0;              // Default specific gravity
    public static final double  EPUMP    =75;               // Default pump efficiency
    public static final String  DEFPATID ="1";              // Default demand pattern ID

    public static final double  RQTOL     =1E-7;            // Default low flow resistance tolerance
    public static final int     CHECKFREQ =2;               // Default status check frequency
    public static final int  MAXCHECK  =10;              // Default # iterations for status checks
    public static final double  DAMPLIMIT =0;               // Default damping threshold

    public static final int     MAXVAR = 21;                //Max. # types of network variables
    public static final double  BIG  =     1.E10d;
    public static final double  TINY =     1.E-6d;

    public static final double  MISSING =  -1.E10d;


    // Conversion factors
    public static final double  GPMperCFS = 448.831d;
    public static final double  AFDperCFS = 1.9837d;
    public static final double  MGDperCFS = 0.64632d;
    public static final double  IMGDperCFS= 0.5382d;
    public static final double  LPSperCFS = 28.317d;
    public static final double  LPMperCFS = 1699.0d;
    public static final double  CMHperCFS = 101.94d;
    public static final double  CMDperCFS = 2446.6d;
    public static final double  MLDperCFS = 2.4466d;
    public static final double  M3perFT3  = 0.028317d;
    public static final double  LperFT3   = 28.317d;
    public static final double  MperFT    = 0.3048d;
    public static final double  PSIperFT  = 0.4333d;
    public static final double  KPAperPSI = 6.895d;
    public static final double  KWperHP   = 0.7457d;
    public static final int     SECperDAY = 86400;

    public static final double  DIFFUS    = 1.3E-8d;
    public static final double  VISCOS    = 1.1E-5 ;

    public static final int     EN_NOSAVE =  0;
    public static final int     EN_SAVE = 1;
    public static final int     EN_INITFLOW = 10;
}
