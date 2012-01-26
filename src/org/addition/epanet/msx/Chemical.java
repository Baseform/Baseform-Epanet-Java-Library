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

import org.addition.epanet.msx.Solvers.*;
import org.addition.epanet.msx.Structures.ExprVariable;
import org.addition.epanet.msx.Structures.Pipe;

public class Chemical implements ExprVariable, JacobianInterface {

    public void loadDependencies(EpanetMSX epa) {
        this.MSX = epa.getNetwork();
    }

    Network MSX;
    rk5     rk5_solver;
    ros2    ros2_solver;
    Newton  newton;

    //  Constants
    private static final int MAXIT = 20;    // Max. number of iterations used in nonlinear equation solver
    private static final int NUMSIG = 3;    // Number of significant digits in nonlinear equation solver error

    private Pipe    TheSeg;                 // Current water quality segment
    private int     TheLink;                // Index of current link
    private int     TheNode;                // Index of current node
    private int     NumSpecies;             // Total number of species
    private int     NumPipeRateSpecies;     // Number of species with pipe rates
    private int     NumTankRateSpecies;     // Number of species with tank rates
    private int     NumPipeFormulaSpecies;  // Number of species with pipe formulas
    private int     NumTankFormulaSpecies;  // Number of species with tank formulas
    private int     NumPipeEquilSpecies;    // Number of species with pipe equilibria
    private int     NumTankEquilSpecies;    // Number of species with tank equilibria
    private int     []PipeRateSpecies;      // Species governed by pipe reactions
    private int     []TankRateSpecies;      // Species governed by tank reactions
    private int     []PipeEquilSpecies;     // Species governed by pipe equilibria
    private int     []TankEquilSpecies;     // Species governed by tank equilibria
    private int     []LastIndex;            // Last index of given type of variable
    private double  []Atol;                 // Absolute concentration tolerances
    private double  []Rtol;                 // Relative concentration tolerances
    private double  []Yrate;                // Rate species concentrations
    private double  []Yequil;               // Equilibrium species concentrations
    private double  []HydVar;               // Values of hydraulic variables


    /**
     * opens the multi-species chemistry system.
      */
    int  MSXchem_open()
    {
        int m;
        int numWallSpecies;
        int numBulkSpecies;
        int numTankExpr;
        int numPipeExpr;

        HydVar      = new double[EnumTypes.HydVarType.MAX_HYD_VARS.id];
        LastIndex   = new int[EnumTypes.ObjectTypes.MAX_OBJECTS.id];

        PipeRateSpecies = null;
        TankRateSpecies = null;
        PipeEquilSpecies = null;
        TankEquilSpecies = null;
        Atol = null;
        Rtol = null;
        Yrate = null;
        Yequil = null;
        NumSpecies = MSX.Nobjects[EnumTypes.ObjectTypes.SPECIES.id];
        m = NumSpecies + 1;
        PipeRateSpecies = new int[m];
        TankRateSpecies = new int [m];
        PipeEquilSpecies = new int [m];
        TankEquilSpecies = new int [m];
        Atol   = new double[m];
        Rtol   = new double[m];
        Yrate  = new double[m];
        Yequil = new double[m];

        // Assign species to each type of chemical expression
        setSpeciesChemistry();
        numPipeExpr = NumPipeRateSpecies + NumPipeFormulaSpecies + NumPipeEquilSpecies;
        numTankExpr = NumTankRateSpecies + NumTankFormulaSpecies + NumTankEquilSpecies;

        // Use pipe chemistry for tanks if latter was not supplied
        if ( numTankExpr == 0 )
        {
            setTankChemistry();
            numTankExpr = numPipeExpr;
        }

        // Check if enough equations were specified
        numWallSpecies = 0;
        numBulkSpecies = 0;
        for (m=1; m<=NumSpecies; m++)
        {
            if ( MSX.Species[m].getType() == EnumTypes.SpeciesType.WALL ) numWallSpecies++;
            if ( MSX.Species[m].getType() == EnumTypes.SpeciesType.BULK ) numBulkSpecies++;
        }
        if ( numPipeExpr != NumSpecies )       return EnumTypes.ErrorCodeType.ERR_NUM_PIPE_EXPR.id;
        if ( numTankExpr != numBulkSpecies   ) return EnumTypes.ErrorCodeType.ERR_NUM_TANK_EXPR.id;

        // Open the ODE solver;
        // arguments are max. number of ODE's,
        // max. number of steps to be taken,
        // 1 if automatic step sizing used (or 0 if not used)

        if ( MSX.Solver == EnumTypes.SolverType.RK5 )
        {
            rk5_solver = new rk5();
            rk5_solver.rk5_open(NumSpecies, 1000, 1);
        }
        if ( MSX.Solver == EnumTypes.SolverType.ROS2 )
        {
            ros2_solver = new ros2();
            ros2_solver.ros2_open(NumSpecies, 1);
        }

        // Open the algebraic eqn. solver
        m = Math.max(NumPipeEquilSpecies, NumTankEquilSpecies);
        newton = new Newton();
        newton.newton_open(m);

        // Assign entries to LastIndex array
        LastIndex[EnumTypes.ObjectTypes.SPECIES.id] = MSX.Nobjects[EnumTypes.ObjectTypes.SPECIES.id];
        LastIndex[EnumTypes.ObjectTypes.TERM.id] = LastIndex[EnumTypes.ObjectTypes.SPECIES.id] + MSX.Nobjects[EnumTypes.ObjectTypes.TERM.id];
        LastIndex[EnumTypes.ObjectTypes.PARAMETER.id] = LastIndex[EnumTypes.ObjectTypes.TERM.id] + MSX.Nobjects[EnumTypes.ObjectTypes.PARAMETER.id];
        LastIndex[EnumTypes.ObjectTypes.CONSTANT.id] = LastIndex[EnumTypes.ObjectTypes.PARAMETER.id] + MSX.Nobjects[EnumTypes.ObjectTypes.CONSTANT.id];

        return 0;
    }

    /**
     * computes reactions in all pipes and tanks.
      */
    int MSXchem_react(long dt)
    {
        int k, m;
        int errcode = 0;

        // Save tolerances of pipe rate species
        for (k=1; k<=NumPipeRateSpecies; k++)
        {
            m = PipeRateSpecies[k];
            Atol[k] = MSX.Species[m].getaTol();
            Rtol[k] = MSX.Species[m].getrTol();
        }

        // Examine each link
        for (k=1; k<=MSX.Nobjects[EnumTypes.ObjectTypes.LINK.id]; k++)
        {
            // Skip non-pipe links
            if ( MSX.Link[k].getLen() == 0.0 ) continue;

            // Evaluate hydraulic variables
            evalHydVariables(k);

            // Compute pipe reactions
            errcode = evalPipeReactions(k, dt);
            if ( errcode!=0 ) return errcode;
        }

        // Save tolerances of tank rate species
        for (k=1; k<=NumTankRateSpecies; k++)
        {
            m = TankRateSpecies[k];
            Atol[k] = MSX.Species[m].getaTol();
            Rtol[k] = MSX.Species[m].getrTol();
        }

        // Examine each tank
        for (k=1; k<=MSX.Nobjects[EnumTypes.ObjectTypes.TANK.id]; k++)
        {
            // Skip reservoirs
            if (MSX.Tank[k].getA() == 0.0) continue;

            // Compute tank reactions
            errcode = evalTankReactions(k, dt);
            if ( errcode!=0 ) return errcode;
        }
        return errcode;
    }


    // Computes equilibrium concentrations for a set of chemical species.
    int MSXchem_equil(EnumTypes.ObjectTypes zone, double [] c)
    {
        int errcode = 0;
        if ( zone == EnumTypes.ObjectTypes.LINK )
        {
            if ( NumPipeEquilSpecies > 0 ) errcode = evalPipeEquil(c);
            evalPipeFormulas(c);
        }
        if ( zone == EnumTypes.ObjectTypes.NODE )
        {
            if ( NumTankEquilSpecies > 0 ) errcode = evalTankEquil(c);
            evalTankFormulas(c);
        }
        return errcode;
    }

    /**
     * Determines which species are described by reaction rate expressions, equilibrium expressions, or simple formulas.
      */
    void setSpeciesChemistry()
    {
        int m;
        NumPipeRateSpecies = 0;
        NumPipeFormulaSpecies = 0;
        NumPipeEquilSpecies = 0;
        NumTankRateSpecies = 0;
        NumTankFormulaSpecies = 0;
        NumTankEquilSpecies = 0;
        for (m=1; m<=NumSpecies; m++)
        {
            switch ( MSX.Species[m].getPipeExprType() )
            {
                case RATE:
                    NumPipeRateSpecies++;
                    PipeRateSpecies[NumPipeRateSpecies] = m;
                    break;

                case FORMULA:
                    NumPipeFormulaSpecies++;
                    break;

                case EQUIL:
                    NumPipeEquilSpecies++;
                    PipeEquilSpecies[NumPipeEquilSpecies] = m;
                    break;
            }
            switch ( MSX.Species[m].getTankExprType() )
            {
                case RATE:
                    NumTankRateSpecies++;
                    TankRateSpecies[NumTankRateSpecies] = m;
                    break;

                case FORMULA:
                    NumTankFormulaSpecies++;
                    break;

                case EQUIL:
                    NumTankEquilSpecies++;
                    TankEquilSpecies[NumTankEquilSpecies] = m;
                    break;
            }
        }
    }


    /**
     * Assigns pipe chemistry expressions to tank chemistry for each chemical species.
     */
    void setTankChemistry()
    {
        int m;
        for (m=1; m<=NumSpecies; m++)
        {
            MSX.Species[m].setTankExpr(MSX.Species[m].getPipeExpr());
            MSX.Species[m].setTankExprType(MSX.Species[m].getPipeExprType());
        }
        NumTankRateSpecies = NumPipeRateSpecies;
        for (m=1; m<=NumTankRateSpecies; m++)
        {
            TankRateSpecies[m] = PipeRateSpecies[m];
        }
        NumTankFormulaSpecies = NumPipeFormulaSpecies;
        NumTankEquilSpecies = NumPipeEquilSpecies;
        for (m=1; m<=NumTankEquilSpecies; m++)
        {
            TankEquilSpecies[m] = PipeEquilSpecies[m];
        }
    }

    /**
     * Retrieves current values of hydraulic variables for the current link being analyzed.
      */
    void evalHydVariables(int k)
    {
        double dh;                              // headloss in ft
        double diam = MSX.Link[k].getDiam();    // diameter in ft
        double av;                              // area per unit volume

        //  pipe diameter in user's units (ft or m)
        HydVar[EnumTypes.HydVarType.DIAMETER.id] = diam * MSX.Ucf[EnumTypes.UnitsType.LENGTH_UNITS.id];

        //  flow rate in user's units
        HydVar[EnumTypes.HydVarType.FLOW.id] = Math.abs(MSX.Q[k]) * MSX.Ucf[EnumTypes.UnitsType.FLOW_UNITS.id];

        //  flow velocity in ft/sec
        if ( diam == 0.0 ) HydVar[EnumTypes.HydVarType.VELOCITY.id] = 0.0;
        else HydVar[EnumTypes.HydVarType.VELOCITY.id] = Math.abs(MSX.Q[k]) * 4.0 / Constants.PI / (diam*diam);

        //  Reynolds number
        HydVar[EnumTypes.HydVarType.REYNOLDS.id] = HydVar[EnumTypes.HydVarType.VELOCITY.id] * diam / Constants.VISCOS;

        //  flow velocity in user's units (ft/sec or m/sec)
        HydVar[EnumTypes.HydVarType.VELOCITY.id] *= MSX.Ucf[EnumTypes.UnitsType.LENGTH_UNITS.id];

        //  Darcy Weisbach friction factor
        if ( MSX.Link[k].getLen() == 0.0 ) HydVar[EnumTypes.HydVarType.FRICTION.id] = 0.0;
        else
        {
            dh = Math.abs(MSX.H[MSX.Link[k].getN1()] - MSX.H[MSX.Link[k].getN2()]);
            HydVar[EnumTypes.HydVarType.FRICTION.id] = 39.725*dh*Math.pow(diam, 5)/
                    MSX.Link[k].getLen()/(MSX.Q[k]*MSX.Q[k]);
        }

        // Shear velocity in user's units (ft/sec or m/sec)
        HydVar[EnumTypes.HydVarType.SHEAR.id] = HydVar[EnumTypes.HydVarType.VELOCITY.id] * Math.sqrt(HydVar[EnumTypes.HydVarType.FRICTION.id] / 8.0);

        // Pipe surface area / volume in area_units/L
        HydVar[EnumTypes.HydVarType.AREAVOL.id] = 1.0;
        if ( diam > 0.0 )
        {
            av  = 4.0/diam;                // ft2/ft3
            av *= MSX.Ucf[EnumTypes.UnitsType.AREA_UNITS.id];     // area_units/ft3
            av /= Constants.LperFT3;                 // area_units/L
            HydVar[EnumTypes.HydVarType.AREAVOL.id] = av;
        }

        HydVar[EnumTypes.HydVarType.ROUGHNESS.id] = MSX.Link[k].getRoughness();   //Feng Shang, Bug ID 8,  01/29/2008
    }


    /**
    * Updates species concentrations in each WQ segment of a pipe after reactions occur over time step dt.
    */
    int evalPipeReactions(int k, long dt)
    {
        int i, m;
        int errcode = 0, ierr = 0;
        double tstep = (double)dt / MSX.Ucf[EnumTypes.UnitsType.RATE_UNITS.id];
        double c, dc;
        double [] dh = new double[1];
        // Start with the most downstream pipe segment

        TheLink = k;
        for(Pipe seg : MSX.Segments[TheLink])
        {
            TheSeg = seg;
            // Store all segment species concentrations in MSX.C1

            for (m=1; m<=NumSpecies; m++) MSX.C1[m] = TheSeg.getC()[m];
            ierr = 0;

            // React each reacting species over the time step

            if ( dt > 0.0 )
            {
                // Euler integrator

                if ( MSX.Solver == EnumTypes.SolverType.EUL )
                {
                    for (i=1; i<=NumPipeRateSpecies; i++)
                    {
                        m = PipeRateSpecies[i];

                        //dc = mathexpr_eval(MSX.Species[m].getPipeExpr(),
                        //        getPipeVariableValue) * tstep;
                        dc = MSX.Species[m].getPipeExpr().evaluatePipeExp(this)*tstep;
                        //dc = MSX.Species[m].getPipeExpr().evaluate(new VariableInterface() {
                        //    public double getValue(int id) {return getPipeVariableValue(id);}
                        //    public int getIndex(String id) {return 0;}
                        //})* tstep;

                        c = TheSeg.getC()[m] + dc;
                        TheSeg.getC()[m] = Math.max(c, 0.0);
                    }
                }

                // Other integrators
                else
                {
                    // Place current concentrations of species that react in vector Yrate

                    for (i=1; i<=NumPipeRateSpecies; i++)
                    {
                        m = PipeRateSpecies[i];
                        Yrate[i] = TheSeg.getC()[m];
                    }
                    dh[0] = TheSeg.getHstep();

                    // integrate the set of rate equations

                    // Runge-Kutta integrator
                    if ( MSX.Solver == EnumTypes.SolverType.RK5 )
                        ierr = rk5_solver.rk5_integrate(Yrate, NumPipeRateSpecies, 0, tstep,
                                dh, Atol, Rtol, this,Operation.PIPES_DC_DT_CONCENTRATIONS);
                                //new JacobianFunction(){
                               //    public void solve(double t, double[] y, int n, double[] f){getPipeDcDt(t,y,n,f);}
                               //    public void solve(double t, double[] y, int n, double[] f, int off) {getPipeDcDt(t,y,n,f,off);}
                               //});

                    // Rosenbrock integrator
                    if ( MSX.Solver == EnumTypes.SolverType.ROS2 )
                        ierr = ros2_solver.ros2_integrate(Yrate, NumPipeRateSpecies, 0, tstep,
                                dh, Atol, Rtol,this,Operation.PIPES_DC_DT_CONCENTRATIONS);
                                //new JacobianFunction() {
                                //    public void solve(double t, double[] y, int n, double[] f) {getPipeDcDt(t, y, n, f);}
                                //    public void solve(double t, double[] y, int n, double[] f, int off) {getPipeDcDt(t,y,n,f,off);}
                                //});

                    // save new concentration values of the species that reacted

                    for (m=1; m<=NumSpecies; m++) TheSeg.getC()[m] = MSX.C1[m];
                    for (i=1; i<=NumPipeRateSpecies; i++)
                    {
                        m = PipeRateSpecies[i];
                        TheSeg.getC()[m] = Math.max(Yrate[i], 0.0);
                    }
                    TheSeg.setHstep(dh[0]);
                }
                if ( ierr < 0 )
                    return EnumTypes.ErrorCodeType.ERR_INTEGRATOR.id;
            }

            // Compute new equilibrium concentrations within segment

            errcode = MSXchem_equil(EnumTypes.ObjectTypes.LINK, TheSeg.getC());

            if ( errcode!=0 )
                return errcode;

            // Move to the segment upstream of the current one

            //TheSeg = TheSeg->prev;
        }
        return errcode;
    }

    /**
     * Updates species concentrations in a given storage tank after reactions occur over time step dt.
      */
    int evalTankReactions(int k, long dt)
    {
        int i, m;
        int errcode = 0, ierr = 0;
        double tstep = (double)dt / MSX.Ucf[EnumTypes.UnitsType.RATE_UNITS.id];
        double c, dc;
        double [] dh = new double[1];

        // evaluate each volume segment in the tank

        TheNode = MSX.Tank[k].getNode();
        i = MSX.Nobjects[EnumTypes.ObjectTypes.LINK.id] + k;
        //TheSeg = MSX.Segments[i];
        //while ( TheSeg )
        for(Pipe seg : MSX.Segments[i])
        {
            TheSeg = seg;

            // store all segment species concentrations in MSX.C1
            for (m=1; m<=NumSpecies; m++) MSX.C1[m] = TheSeg.getC()[m];
            ierr = 0;

            // react each reacting species over the time step
            if ( dt > 0.0 )
            {
                if ( MSX.Solver == EnumTypes.SolverType.EUL )
                {
                    for (i=1; i<=NumTankRateSpecies; i++)
                    {
                        m = TankRateSpecies[i];
                        //dc = tstep * mathexpr_eval(MSX.Species[m].getTankExpr(),
                        //        getTankVariableValue);
                        dc = tstep * MSX.Species[m].getTankExpr().evaluateTankExp(this);
                        //dc = tstep * MSX.Species[m].getTankExpr().evaluate(
                        //        new VariableInterface(){
                        //            public double getValue(int id) {return getTankVariableValue(id);}
                        //            public int getIndex(String id) {return 0;}
                        //        });
                        c = TheSeg.getC()[m] + dc;
                        TheSeg.getC()[m] = Math.max(c, 0.0);
                    }
                }

                else
                {
                    for (i=1; i<=NumTankRateSpecies; i++)
                    {
                        m = TankRateSpecies[i];
                        Yrate[i] = MSX.Tank[k].getC()[m];
                    }
                    dh[0] = MSX.Tank[k].getHstep();

                    if ( MSX.Solver == EnumTypes.SolverType.RK5 )
                        ierr = rk5_solver.rk5_integrate(Yrate, NumTankRateSpecies, 0, tstep,
                                dh, Atol, Rtol, this,Operation.TANKS_DC_DT_CONCENTRATIONS);
                                //new JacobianFunction() {
                                //    public void solve(double t, double[] y, int n, double[] f) {getTankDcDt(t,y,n,f);}
                                //    public void solve(double t, double[] y, int n, double[] f, int off) {getTankDcDt(t,y,n,f,off);}
                                //} );

                    if ( MSX.Solver == EnumTypes.SolverType.ROS2 )
                        ierr = ros2_solver.ros2_integrate(Yrate, NumTankRateSpecies, 0, tstep,
                                dh, Atol, Rtol, this,Operation.TANKS_DC_DT_CONCENTRATIONS);
                                //new JacobianFunction() {
                                //    public void solve(double t, double[] y, int n, double[] f) {getTankDcDt(t,y,n,f);}
                                //    public void solve(double t, double[] y, int n, double[] f, int off) {getTankDcDt(t,y,n,f,off);}
                                //} );

                    for (m=1; m<=NumSpecies; m++) TheSeg.getC()[m] = MSX.C1[m];
                    for (i=1; i<=NumTankRateSpecies; i++)
                    {
                        m = TankRateSpecies[i];
                        TheSeg.getC()[m] = Math.max(Yrate[i], 0.0);
                    }
                    TheSeg.setHstep(dh[0]);
                }
                if ( ierr < 0 )
                    return EnumTypes.ErrorCodeType.ERR_INTEGRATOR.id;
            }

            // compute new equilibrium concentrations within segment
            errcode = MSXchem_equil(EnumTypes.ObjectTypes.NODE, TheSeg.getC());

            if ( errcode!=0 )
                return errcode;
        }
        return errcode;
    }

    /**
     * computes equilibrium concentrations for water in a pipe segment.
      */
    int evalPipeEquil(double [] c)
    {
        int i, m;
        int errcode;
        for (m=1; m<=NumSpecies; m++) MSX.C1[m] = c[m];
        for (i=1; i<=NumPipeEquilSpecies; i++)
        {
            m = PipeEquilSpecies[i];
            Yequil[i] = c[m];
        }
        errcode = newton.newton_solve(Yequil, NumPipeEquilSpecies, MAXIT, NUMSIG,
                this,Operation.PIPES_EQUIL);
                //new JacobianFunction() {
                //    public void solve(double t, double[] y, int n, double[] f) {
                //        getPipeEquil(t,y,n,f);
                //    }
                //
                //    public void solve(double t, double[] y, int n, double[] f, int off) {
                //        System.out.println("Jacobian Unused");
                //    }
                //});
        if ( errcode < 0 ) return EnumTypes.ErrorCodeType.ERR_NEWTON.id;
        for (i=1; i<=NumPipeEquilSpecies; i++)
        {
            m = PipeEquilSpecies[i];
            c[m] = Yequil[i];
            MSX.C1[m] = c[m];
        }
        return 0;
    }


    /**
     * computes equilibrium concentrations for water in a tank.
      */
    int evalTankEquil(double [] c)
    {
        int i, m;
        int errcode;
        for (m=1; m<=NumSpecies; m++) MSX.C1[m] = c[m];
        for (i=1; i<=NumTankEquilSpecies; i++)
        {
            m = TankEquilSpecies[i];
            Yequil[i] = c[m];
        }
        errcode = newton.newton_solve(Yequil, NumTankEquilSpecies, MAXIT, NUMSIG,
                this,Operation.TANKS_EQUIL);
                //new JacobianFunction() {
                //    public void solve(double t, double[] y, int n, double[] f) {getTankEquil(t,y,n,f);}
                //    public void solve(double t, double[] y, int n, double[] f, int off) {
                //        System.out.println("Jacobian Unused");}
                //});
        if ( errcode < 0 ) return EnumTypes.ErrorCodeType.ERR_NEWTON.id;
        for (i=1; i<=NumTankEquilSpecies; i++)
        {
            m = TankEquilSpecies[i];
            c[m] = Yequil[i];
            MSX.C1[m] = c[m];
        }
        return 0;
    }

    /**
     * Evaluates species concentrations in a pipe segment that are simple
     * formulas involving other known species concentrations.
      */
    void evalPipeFormulas(double [] c)
    {
        int m;
        for (m=1; m<=NumSpecies; m++) MSX.C1[m] = c[m];
        for (m=1; m<=NumSpecies; m++)
        {
            if ( MSX.Species[m].getPipeExprType() == EnumTypes.ExpressionType.FORMULA )
            {
                c[m] = MSX.Species[m].getPipeExpr().evaluatePipeExp(this);
                //c[m] = MSX.Species[m].getPipeExpr().evaluate( new VariableInterface(){
                //    public double getValue(int id){return getPipeVariableValue(id);}
                //    public int getIndex(String id){return 0;}
                //});
            }
        }
    }

    /**
     * Evaluates species concentrations in a tank that are simple
     * formulas involving other known species concentrations.
      */
    void evalTankFormulas(double [] c)
    {
        int m;
        for (m=1; m<=NumSpecies; m++) MSX.C1[m] = c[m];
        for (m=1; m<=NumSpecies; m++)
        {
            if ( MSX.Species[m].getTankExprType() == EnumTypes.ExpressionType.FORMULA )
            {
                c[m] = MSX.Species[m].getPipeExpr().evaluateTankExp(this);
                //c[m] = MSX.Species[m].getPipeExpr().evaluate(new VariableInterface(){
                //    public double getValue(int id){return getTankVariableValue(id);}
                //    public int getIndex(String id){return 0;}
                //});
            }
        }
    }

    /**
     * Finds the value of a species, a parameter, or a constant for the pipe link being analyzed.
      */
    public double getPipeVariableValue(int i)
    {
        // WQ species have index i between 1 & # of species
        // and their current values are stored in vector MSX.C1
        if ( i <= LastIndex[EnumTypes.ObjectTypes.SPECIES.id] )
        {
            // If species represented by a formula then evaluate it
            if ( MSX.Species[i].getPipeExprType() == EnumTypes.ExpressionType.FORMULA )
            {
                return MSX.Species[i].getPipeExpr().evaluatePipeExp(this);
                //return MSX.Species[i].getPipeExpr().evaluate(new VariableInterface(){
                //    public double getValue(int id){return getPipeVariableValue(id);}
                //    public int getIndex(String id){return 0;}
                //});
            }
            else // otherwise return the current concentration
                return MSX.C1[i];
        }
        else if ( i <= LastIndex[EnumTypes.ObjectTypes.TERM.id] )   // intermediate term expressions come next
        {
            i -= LastIndex[EnumTypes.ObjectTypes.TERM.id-1];
            return MSX.Term[i].getExpr().evaluatePipeExp(this);
            //return MSX.Term[i].getExpr().evaluate(new VariableInterface(){
            //    public double getValue(int id){return getPipeVariableValue(id);}
            //    public int getIndex(String id){return 0;}
            //});
        }
        else if ( i <= LastIndex[EnumTypes.ObjectTypes.PARAMETER.id] ) // reaction parameter indexes come after that
        {
            i -= LastIndex[EnumTypes.ObjectTypes.PARAMETER.id-1];
            return MSX.Link[TheLink].getParam()[i];
        }
        else if ( i <= LastIndex[EnumTypes.ObjectTypes.CONSTANT.id] ) // followed by constants
        {
            i -= LastIndex[EnumTypes.ObjectTypes.CONSTANT.id-1];
            return MSX.Const[i].getValue();
        }
        else  // and finally by hydraulic variables
        {
            i -= LastIndex[EnumTypes.ObjectTypes.CONSTANT.id];
            if (i < EnumTypes.HydVarType.MAX_HYD_VARS.id) return HydVar[i];
            else return 0.0;
        }
    }

    /**
     * Finds the value of a species, a parameter, or a constant for the current node being analyzed.
      */
    public double getTankVariableValue(int i)
    {
        int j;
        // WQ species have index i between 1 & # of species and their current values are stored in vector MSX.C1
        if ( i <= LastIndex[EnumTypes.ObjectTypes.SPECIES.id] )
        {
            // If species represented by a formula then evaluate it
            if ( MSX.Species[i].getTankExprType() == EnumTypes.ExpressionType.FORMULA )
            {
                return MSX.Species[i].getTankExpr().evaluateTankExp(this);
                //return MSX.Species[i].getTankExpr().evaluate(new VariableInterface() {
                //    public double getValue(int id) {return getTankVariableValue(id);}
                //    public int getIndex(String id) {return 0;}});
            }
            else // Otherwise return the current concentration
                return MSX.C1[i];
        }
        else if ( i <= LastIndex[EnumTypes.ObjectTypes.TERM.id] ) // Intermediate term expressions come next
        {
            i -= LastIndex[EnumTypes.ObjectTypes.TERM.id-1];
            return MSX.Term[i].getExpr().evaluateTankExp(this);
            //return MSX.Term[i].getExpr().evaluate(new VariableInterface(){
            //    public double getValue(int id) {return getTankVariableValue(id);}
            //    public int getIndex(String id) {return 0;}
            //});
        }
        else if (i <= LastIndex[EnumTypes.ObjectTypes.PARAMETER.id] ) // Next come reaction parameters associated with Tank nodes
        {
            i -= LastIndex[EnumTypes.ObjectTypes.PARAMETER.id-1];
            j = MSX.Node[TheNode].getTank();
            if ( j > 0 )
            {
                return MSX.Tank[j].getParam()[i];
            }
            else
                return 0.0;
        }
        else if (i <= LastIndex[EnumTypes.ObjectTypes.CONSTANT.id] ) // and then come constants
        {
            i -= LastIndex[EnumTypes.ObjectTypes.CONSTANT.id-1];
            return MSX.Const[i].getValue();
        }
        else
            return 0.0;
    }


    /**
     * finds reaction rate (dC/dt) for each reacting species in a pipe.
      */

    void getPipeDcDt(double t, double y[], int n, double deriv[])
    {
        int i, m;

        // Assign species concentrations to their proper positions in the global concentration vector MSX.C1
        for (i=1; i<=n; i++)
        {
            m = PipeRateSpecies[i];
            MSX.C1[m] = y[i];
        }

        // Update equilibrium species if full coupling in use
        if ( MSX.Coupling == EnumTypes.CouplingType.FULL_COUPLING )
        {
            if ( MSXchem_equil(EnumTypes.ObjectTypes.LINK, MSX.C1) > 0 )     // check for error condition
            {
                for (i=1; i<=n; i++) deriv[i] = 0.0;
                return;
            }
        }

        // Evaluate each pipe reaction expression
        for (i=1; i<=n; i++)
        {
            m = PipeRateSpecies[i];
            //deriv[i] = mathexpr_eval(MSX.Species[m].getPipeExpr(), getPipeVariableValue);
            deriv[i] = MSX.Species[m].getPipeExpr().evaluatePipeExp(this);
            //deriv[i] = MSX.Species[m].getPipeExpr().evaluate(new VariableInterface(){
            //    public double getValue(int id) {return getPipeVariableValue(id);}
            //    public int getIndex(String id) {return 0;}
            //});
        }
    }


    void getPipeDcDt(double t, double y[], int n, double deriv[], int off)
    {
        int i, m;

        // Assign species concentrations to their proper positions in the global concentration vector MSX.C1
        for (i=1; i<=n; i++)
        {
            m = PipeRateSpecies[i];
            MSX.C1[m] = y[i];
        }

        // Update equilibrium species if full coupling in use

        if ( MSX.Coupling == EnumTypes.CouplingType.FULL_COUPLING )
        {
            if ( MSXchem_equil(EnumTypes.ObjectTypes.LINK, MSX.C1) > 0 )     // check for error condition
            {
                for (i=1; i<=n; i++) deriv[i+off] = 0.0;
                return;
            }
        }

        // evaluate each pipe reaction expression
        for (i=1; i<=n; i++)
        {
            m = PipeRateSpecies[i];
            //deriv[i+off] = mathexpr_eval(MSX.Species[m].getPipeExpr(), getPipeVariableValue);
            deriv[i+off] = MSX.Species[m].getPipeExpr().evaluatePipeExp(this);
            //deriv[i+off] = MSX.Species[m].getPipeExpr().evaluate(new VariableInterface(){
            //    public double getValue(int id) {return getPipeVariableValue(id);}
            //    public int getIndex(String id) {return 0;}
            //});
        }
    }


    /**
     * finds reaction rate (dC/dt) for each reacting species in a tank.
      */
    void getTankDcDt(double t, double y[], int n, double deriv[])
    {
        int i, m;

        // Assign species concentrations to their proper positions in the global concentration vector MSX.C1
        for (i=1; i<=n; i++)
        {
            m = TankRateSpecies[i];
            MSX.C1[m] = y[i];
        }

        // Update equilibrium species if full coupling in use
        if ( MSX.Coupling == EnumTypes.CouplingType.FULL_COUPLING )
        {
            if ( MSXchem_equil(EnumTypes.ObjectTypes.NODE, MSX.C1) > 0 )     // check for error condition
            {
                for (i=1; i<=n; i++) deriv[i] = 0.0;
                return;
            }
        }

        // Evaluate each tank reaction expression
        for (i=1; i<=n; i++)
        {
            m = TankRateSpecies[i];
            deriv[i] = MSX.Species[m].getTankExpr().evaluateTankExp(this);
            //deriv[i] = MSX.Species[m].getTankExpr().evaluate(new VariableInterface() {
            //    public double getValue(int id) {return getTankVariableValue(id); }
            //    public int getIndex(String id) {return 0;}
            //}); //mathexpr_eval(MSX.Species[m].getTankExpr(), getTankVariableValue);
        }
    }

    void getTankDcDt(double t, double y[], int n, double deriv[], int off)
    {
        int i, m;

        // Assign species concentrations to their proper positions in the global concentration vector MSX.C1

        for (i=1; i<=n; i++)
        {
            m = TankRateSpecies[i];
            MSX.C1[m] = y[i];
        }

        // Update equilibrium species if full coupling in use
        if ( MSX.Coupling == EnumTypes.CouplingType.FULL_COUPLING )
        {
            if ( MSXchem_equil(EnumTypes.ObjectTypes.NODE, MSX.C1) > 0 )     // check for error condition
            {
                for (i=1; i<=n; i++) deriv[i+off] = 0.0;
                return;
            }
        }

        // Evaluate each tank reaction expression
        for (i=1; i<=n; i++)
        {
            m = TankRateSpecies[i];
            //deriv[i+off] = mathexpr_eval(MSX.Species[m].getTankExpr(), getTankVariableValue);
            deriv[i+off] = MSX.Species[m].getTankExpr().evaluateTankExp(this);
            //deriv[i+off] = MSX.Species[m].getTankExpr().evaluate(new VariableInterface() {
            //    public double getValue(int id) {return getTankVariableValue(id); }
            //    public int getIndex(String id) {return 0;}
            //});
        }
    }


    /**
     * Evaluates equilibrium expressions for pipe chemistry.
     */
    void getPipeEquil(double t, double y[], int n, double f[])
    {
        int i, m;

        // Assign species concentrations to their proper positions in the global
        // concentration vector MSX.C1

        for (i=1; i<=n; i++)
        {
            m = PipeEquilSpecies[i];
            MSX.C1[m] = y[i];
        }

        // Evaluate each pipe equilibrium expression

        for (i=1; i<=n; i++)
        {
            m = PipeEquilSpecies[i];
            f[i] = MSX.Species[m].getPipeExpr().evaluatePipeExp(this);
            //f[i] = MSX.Species[m].getPipeExpr().evaluate(new VariableInterface() {
            //    public double getValue(int id){return getPipeVariableValue(id);}
            //    public int getIndex(String id){return 0;}
            //});
        }
    }


    /**
     * Evaluates equilibrium expressions for tank chemistry.
      */
    void getTankEquil(double t, double y[], int n, double f[])
    {
        int i, m;

        // Assign species concentrations to their proper positions in the global concentration vector MSX.C1
        for (i=1; i<=n; i++)
        {
            m = TankEquilSpecies[i];
            MSX.C1[m] = y[i];
        }

        // Evaluate each tank equilibrium expression
        for (i=1; i<=n; i++)
        {
            m = TankEquilSpecies[i];
            f[i] = MSX.Species[m].getTankExpr().evaluateTankExp(this);
            //f[i] = MSX.Species[m].getTankExpr().evaluate(new VariableInterface() {
            //    public double getValue(int id) {return getTankVariableValue(id);}
            //    public int getIndex(String id) {return 0;}
            //});
        }
    }


    public void solve(double t, double[] y, int n, double[] f, int off, Operation op) {
        switch (op) {

            case PIPES_DC_DT_CONCENTRATIONS:
                getPipeDcDt(t,y,n,f,off);
                break;
            case TANKS_DC_DT_CONCENTRATIONS:
                getTankDcDt(t,y,n,f,off);
                break;
            case PIPES_EQUIL:
                getPipeEquil(t,y,n,f);
                break;
            case TANKS_EQUIL:
                getTankDcDt(t,y,n,f);
                break;
        }
    }
}
