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

import org.addition.epanet.msx.Structures.*;
import org.addition.epanet.msx.EnumTypes.*;

import java.util.LinkedList;

// MSX PROJECT VARIABLES
public class Network {
    String          Title;              // Project title

    int []          Nobjects;           // Numbers of each type of object [MAX_OBJECTS]
    UnitSystemType  Unitsflag;          // Unit system flag
    FlowUnitsType   Flowflag;           // Flow units flag
    boolean         Rptflag;            // Report results flag
    CouplingType    Coupling;           // Degree of coupling for solving DAE's
    AreaUnitsType   AreaUnits;          // Surface area units
    RateUnitsType   RateUnits;          // Reaction rate time units
    SolverType      Solver;             // Choice of ODE solver

    int     PageSize;                   // Lines per page in report
    int     Nperiods;                   // Number of reporting periods
    int     ErrCode;                    // Error code

    long    Qstep;                      // Quality time step (sec)
    long    Pstep;                      // Time pattern time step (sec)
    long    Pstart;                     // Starting pattern time (sec)
    long    Rstep;                      // Reporting time step (sec)
    long    Rstart;                     // Time when reporting starts
    long    Rtime;                      // Next reporting time (sec)
    long    Htime;                      // Current hydraulic time (sec)
    long    Qtime;                      // Current quality time (sec)

    TstatType   Statflag;               // Reporting statistic flag
    long        Dur;                    // Duration of simulation (sec)

    float []D;                          // Node demands
    float []H;                          // Node heads
    float []Q;                          // Link flows

    double []   Ucf;                    // Unit conversion factors [MAX_UNIT_TYPES]
    double []   C0;						// Species initial quality vector
    double []   C1;                     // Species concentration vector

    double      DefRtol;                // Default relative error tolerance
    double      DefAtol;                // Default absolute error tolerance

    LinkedList<Pipe>[] Segments;              // First WQ segment in each pipe/tank

    Species []Species;                  // WQ species data
    Param   []Param;                    // Expression parameters
    Const   []Const;                    // Expression constants
    Term    []Term;                     // Intermediate terms
    Node    []Node;                     // Node data
    Link    []Link;                     // Link data
    Tank    []Tank;                     // Tank data
    Pattern []Pattern;                  // Pattern data

    String rptFilename;

    Network(){
        Nobjects = new int[ObjectTypes.MAX_OBJECTS.id];
        Ucf = new double[UnitsType.MAX_UNIT_TYPES.id];

    }

    public Species[] getSpecies() {
        return Species;
    }

    public Node [] getNodes(){
        return Node;
    }

    public Link [] getLinks(){
        return Link;
    }

    public int getNperiods() {
        return Nperiods;
    }

    public long getQstep() {
        return Qstep;
    }

    public long getQtime() {
        return Qtime;
    }

    public long getDuration() {
        return Dur;
    }

    public void setQstep(long qstep) {
        Qstep = qstep;
    }

    public void setDur(long dur) {
        Dur = dur;
    }

    public void setRstep(long rstep) {
        this.Rstep = rstep;
    }

    public long getRstart() {
        return Rstart;
    }

    public void setRstart(long rstart) {
        Rstart = rstart;
    }
}

