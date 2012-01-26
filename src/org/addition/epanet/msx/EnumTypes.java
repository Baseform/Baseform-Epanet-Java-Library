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

public class EnumTypes {

    // Object types
    public enum ObjectTypes{
        NODE        (0),
        LINK        (1),
        TANK        (2),
        SPECIES     (3),
        TERM        (4),
        PARAMETER   (5),
        CONSTANT    (6),
        PATTERN     (7),
        MAX_OBJECTS (8);

        public final int id;
        ObjectTypes(int val){id = val;}
    }

    // Type of source quality input
    public enum SourceType{
        CONCEN,             //    inflow concentration
        MASS,               //    mass inflow booster
        SETPOINT,           //    setpoint booster
        FLOWPACED           //    flow paced booster
    }

    // Unit system
    public enum UnitSystemType{
        US,                 //   US
        SI                  //   SI (metric)
    }

    // Flow units
    public enum FlowUnitsType{
        CFS(0),                //   cubic feet per second
        GPM(1),                //   gallons per minute
        MGD(2),                //   million gallons per day
        IMGD(3),               //   imperial million gal. per day
        AFD(4),                //   acre-feet per day
        LPS(5),                //   liters per second
        LPM(6),                //   liters per minute
        MLD(7),                //   megaliters per day
        CMH(8),                //   cubic meters per hour
        CMD(9);                 //   cubic meters per day
        public final int id;
        FlowUnitsType(int val){id = val;}
    }

    // Tank mixing regimes
    public enum MixType{
        MIX1(0),               //   1-compartment model
        MIX2(1),               //   2-compartment model
        FIFO(2),               //   First in, first out model
        LIFO(3);                //   Last in, first out model
        public final int id;
        MixType(int val){id = val;}
    }

    // Types of water quality species
    public enum SpeciesType{
        BULK,               //   bulk flow species
        WALL                //   pipe wall attached species
    }

    // Types of math expressions
    public enum ExpressionType{
        NO_EXPR,            //   no expression
        RATE,               //   reaction rate
        FORMULA,            //   simple formula
        EQUIL               //   equilibrium expression
    }

    // ODE solver options
    public enum SolverType{
        EUL,                //   Euler
        RK5,                //   5th order Runge-Kutta
        ROS2                //   2nd order Rosenbrock
    }

    // Degree of coupling for solving DAE's
    public enum CouplingType{
        NO_COUPLING,        //   no coupling between alg. & diff. eqns.
        FULL_COUPLING       //   full coupling between alg. &diff. eqns.
    }

    // Concentration mass units
    public enum MassUnitsType{
        MG,                 //   milligram
        UG,                 //   microgram
        MOLE,               //   mole
        MMOLE               //   millimole
    }

    // Pipe surface area units
    public enum AreaUnitsType{
        FT2(0),                //   square feet
        M2(1),                 //   square meters
        CM2(2);                 //   square centimeters
        public final int id;
        AreaUnitsType(int val){id = val;}
    }

    // Reaction rate time units
    public enum RateUnitsType{
        SECONDS(0),            //   seconds
        MINUTES(1),            //   minutes
        HOURS(2),              //   hours
        DAYS(3);                //   days
        public final int id;
        RateUnitsType(int val){id = val;}
    }

    // Measurement unit types
    public enum UnitsType{
        LENGTH_UNITS(0),       //   length
        DIAM_UNITS(1),         //   pipe diameter
        AREA_UNITS(2),         //   surface area
        VOL_UNITS(3),          //   volume
        FLOW_UNITS(4),         //   flow
        CONC_UNITS(5),         //   concentration volume
        RATE_UNITS(6),         //   reaction rate time units
        MAX_UNIT_TYPES(7);

        public final int id;
        UnitsType(int val){id = val;}
    }

    // Hydraulic variables
    public enum HydVarType{
        DIAMETER(1), /*= 1*/   //   link diameter
        FLOW(2),               //   link flow rate
        VELOCITY(3),           //   link flow velocity
        REYNOLDS(4),           //   Reynolds number
        SHEAR(5),              //   link shear velocity
        FRICTION(6),           //   friction factor
        AREAVOL(7),            //   area/volume
        ROUGHNESS(8),		   //   roughness
        MAX_HYD_VARS(9);
        public final int id;
        HydVarType(int val){id = val;}
    }

    // Time series statistics
    public enum TstatType{
        SERIES,             //   full time series
        AVGERAGE,           //   time-averages
        MINIMUM,            //   minimum values
        MAXIMUM,            //   maximum values
        RANGE               //   max - min values
    }

    // Analysis options
    public enum OptionType{
        AREA_UNITS_OPTION,
        RATE_UNITS_OPTION,
        SOLVER_OPTION,
        COUPLING_OPTION,
        TIMESTEP_OPTION,
        RTOL_OPTION,
        ATOL_OPTION
    }

    // File modes
    public enum FileModeType{
        SCRATCH_FILE,
        SAVED_FILE,
        USED_FILE
    }

    // Input data file sections
    public enum SectionType{
        s_TITLE(0),
        s_SPECIES(1),
        s_COEFF(2),
        s_TERM(3),
        s_PIPE(4),
        s_TANK(5),
        s_SOURCE(6),
        s_QUALITY(7),
        s_PARAMETER(8),
        s_PATTERN(9),
        s_OPTION(10),
        s_REPORT(11);
        public final int id;
        SectionType(int val){this.id = val;}
    }

    // Error codes (501-515)
    public enum ErrorCodeType{
        ERR_FIRST           (500),
        ERR_MEMORY          (501),
        ERR_NO_EPANET_FILE  (502),
        ERR_OPEN_MSX_FILE   (503),
        ERR_OPEN_HYD_FILE   (504),
        ERR_READ_HYD_FILE   (505),
        ERR_MSX_INPUT       (506),
        ERR_NUM_PIPE_EXPR   (507),
        ERR_NUM_TANK_EXPR   (508),
        ERR_INTEGRATOR_OPEN (509),
        ERR_NEWTON_OPEN     (510),
        ERR_OPEN_OUT_FILE   (511),
        ERR_IO_OUT_FILE     (512),
        ERR_INTEGRATOR      (513),
        ERR_NEWTON          (514),
        ERR_INVALID_OBJECT_TYPE     (515),
        ERR_INVALID_OBJECT_INDEX    (516),
        ERR_UNDEFINED_OBJECT_ID     (517),
        ERR_INVALID_OBJECT_PARAMS   (518),
        ERR_MSX_NOT_OPENED          (519),
        ERR_MSX_OPENED              (520),
        ERR_OPEN_RPT_FILE           (521),
        ERR_MAX                     (522);
        public final int id;
        ErrorCodeType(int val){this.id = val;}
    }


    public enum MSXConstants {
        MSX_NODE     (0),
        MSX_LINK     (1),
        MSX_TANK     (2),
        MSX_SPECIES  (3),
        MSX_TERM     (4),
        MSX_PARAMETER(5),
        MSX_CONSTANT (6),
        MSX_PATTERN  (7),

        MSX_BULK(0),
        MSX_WALL(1),

        MSX_NOSOURCE  (-1),
        MSX_CONCEN    (0),
        MSX_MASS      (1),
        MSX_SETPOINT  (2),
        MSX_FLOWPACED (3);

        public final int id;

        MSXConstants(int val){id = val;}
    }




}
