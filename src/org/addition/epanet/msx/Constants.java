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


public class Constants {
    public static final int MAXUNITS = 16;
    public static final int MAXFNAME = 259; // Max. # characters in file name
    public static final int CODEVERSION =20012;

    public static final double TINY1 = 1.0e-20d;


    public static final int MAGICNUMBER =516114521;
    public static final int VERSION     =100000;
    public static final int MAXMSG      =1024;            // Max. # characters in message text
    public static final int MAXLINE     =1024;            // Max. # characters in input line
    //public static final int TRUE        =1;
    //public static final int FALSE       =0;
    public static final double BIG      =1.E10d;
    public static final double TINY     =1.E-6d;
    public static final double MISSING  =-1.E10d;
    public static final double PI       =3.141592654d;
    public static final double VISCOS   =1.1E-5d;          // Kinematic viscosity of water
    // @ 20 deg C (sq ft/sec)

    //-----------------------------------------------------------------------------
//  Various conversion factors
//-----------------------------------------------------------------------------
    public static final double M2perFT2    =0.09290304d;
    public static final double CM2perFT2   =929.0304d;
    public static final double DAYperSEC   =1.1574E-5d;
    public static final double HOURperSEC  =2.7778E-4d;
    public static final double MINUTEperSEC=0.016667d;
    public static final double GPMperCFS   =448.831d;
    public static final double AFDperCFS   =1.9837d;
    public static final double MGDperCFS   =0.64632d;
    public static final double IMGDperCFS  =0.5382d;
    public static final double LPSperCFS   =28.317d;
    public static final double LPMperCFS   =1699.0d;
    public static final double CMHperCFS   =101.94d;
    public static final double CMDperCFS   =2446.6d;
    public static final double MLDperCFS   =2.4466d;
    public static final double M3perFT3    =0.028317d;
    public static final double LperFT3     =28.317d;
    public static final double MperFT      =0.3048d;
    public static final double PSIperFT    =0.4333d;
    public static final double KPAperPSI   =6.895d;
    public static final double KWperHP     =0.7457d;
    public static final double SECperDAY   =86400d;


    static String [] Errmsg =
            {"unknown error code.",
                    "Error 501 - insufficient memory available.",
                    "Error 502 - no EPANET data file supplied.",
                    "Error 503 - could not open MSX input file.",
                    "Error 504 - could not open hydraulic results file.",
                    "Error 505 - could not read hydraulic results file.",
                    "Error 506 - could not read MSX input file.",
                    "Error 507 - too few pipe reaction expressions.",
                    "Error 508 - too few tank reaction expressions.",
                    "Error 509 - could not open differential equation solver.",
                    "Error 510 - could not open algebraic equation solver.",
                    "Error 511 - could not open binary results file.",
                    "Error 512 - read/write error on binary results file.",
                    "Error 513 - could not integrate reaction rate expressions.",
                    "Error 514 - could not solve reaction equilibrium expressions.",
                    "Error 515 - reference made to an unknown type of object.",
                    "Error 516 - reference made to an illegal object index.",
                    "Error 517 - reference made to an undefined object ID.",
                    "Error 518 - invalid property values were specified.",
                    "Error 519 - an MSX project was not opened.",
                    "Error 520 - an MSX project is already opened.",
                    "Error 521 - could not open MSX report file."};                           //(LR-11/20/07)


    static final String [] MsxSectWords = {"[TITLE", "[SPECIE",  "[COEFF",  "[TERM",
            "[PIPE",  "[TANK",    "[SOURCE", "[QUALITY",
            "[PARAM", "[PATTERN", "[OPTION",
            "[REPORT"};
    static final String [] ReportWords  = {"NODE", "LINK", "SPECIE", "FILE", "PAGESIZE"};
    static final String [] OptionTypeWords = {"AREA_UNITS", "RATE_UNITS", "SOLVER", "COUPLING",
            "TIMESTEP", "RTOL", "ATOL"};
    static final String [] SourceTypeWords = {"CONC", "MASS", "SETPOINT", "FLOW"};      //(FS-01/10/2008 To fix bug 11)
    static final String [] MixingTypeWords = {"MIXED", "2COMP", "FIFO", "LIFO"};
    static final String [] MassUnitsWords  = {"MG", "UG", "MOLE", "MMOL"};
    static final String [] AreaUnitsWords  = {"FT2", "M2", "CM2"};
    static final String [] TimeUnitsWords  = {"SEC", "MIN", "HR", "DAY"};
    static final String [] SolverTypeWords = {"EUL", "RK5", "ROS2"};
    static final String [] CouplingWords   = {"NONE", "FULL"};
    static final String [] ExprTypeWords   = {"", "RATE", "FORMULA", "EQUIL"};
    static final String [] HydVarWords     = {"", "D", "Q", "U", "Re","Us", "Ff", "Av", "Kc"};	/*Feng Shang 01/29/2008*/
    static final String YES  = "YES";
    static final String NO   = "NO";
    static final String ALL  = "ALL";
    static final String NONE = "NONE";
}
