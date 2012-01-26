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

package org.addition.epanet;

import java.util.logging.Level;

/**
 * Epanet constants.
 */
public class Constants {


    public static double        PI = 3.141592654;

    /**
     * Max. # of disconnected nodes listed
      */
    public static int           MAXCOUNT = 10;
    /**
     * Max. # title lines
     */
    public static int           MAXTITLE = 3;
    /**
     * Max. input errors reported
     */
    public static final int     MAXERRS = 10;
    //public static final int     MAXMSG = 79;
    //public static final int     MAXID  = 31;
    //public static final int     MAXFNAME = 259;

    /**
     * Epanet binary files code version
     */
    public static final int CODEVERSION=20012;

     /**
     * Epanet binary files ID
     */
    public static final int MAGICNUMBER=516114521;

     /**
     * Epanet binary files version
     */
    public static final int VERSION=200;

    /**
     * Equivalent to zero flow
      */
    public static final double QZERO=1.e-6d;
    /**
     * Big coefficient
     */
    public static final double CBIG=1.e8d;
    /**
     * Small coefficient
     */
    public static final double CSMALL=1.e-6d;

    /**
     * Default max. # hydraulic iterations
     */
    public static final int MAXITER=200;
    /**
     * Default hydraulics convergence ratio
     */
    public static final double  HACC=0.001d;
    /**
     * Default hydraulic head tolerance (ft)
     */
    public static final double  HTOL     =0.0005d;
    /**
     * Default flow rate tolerance (cfs)
     */
    public static final double  QTOL     =0.0001d;

    /**
     * Default water age tolerance (hrs)
     */
    public static final double  AGETOL   =0.01d;
    /**
     * Default concentration tolerance
     */
    public static final double  CHEMTOL  =0.01d;
    /**
     * Default uses no page breaks
     */
    public static final int     PAGESIZE =0;
    /**
     * Default specific gravity
     */
    public static final double  SPGRAV   =1.0d;
    /**
     * Default pump efficiency
     */
    public static final double  EPUMP    =75d;
    /**
     * Default demand pattern ID
     */
    public static final String  DEFPATID ="1";

    /**
     * Default low flow resistance tolerance
     */
    public static final double  RQTOL     =1E-7d;

    /**
     * Default status check frequency
     */
    public static final int     CHECKFREQ =2;
    /**
     * Default # iterations for status checks
     */
    public static final int     MAXCHECK  =10;
    /**
     *  Default damping threshold
     */
    public static final double  DAMPLIMIT =0;

    ///**
    // * Max. # types of network variables
    // */
    //public static final int     MAXVAR = 21;

    public static final double  BIG  =     1.E10d;
    public static final double  TINY =     1.E-6d;

    public static final double  MISSING =  -1.E10d;

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
    public static final double  MMperFT    = 304.8d;
    public static final double  INperFT  = 12.0d;
    public static final double  PSIperFT  = 0.4333d;
    public static final double  KPAperPSI = 6.895d;
    public static final double  KWperHP   = 0.7457d;
    public static final int     SECperDAY = 86400;

    public static final double  DIFFUS    = 1.3E-8d;
    public static final double  VISCOS    = 1.1E-5 ;
}
