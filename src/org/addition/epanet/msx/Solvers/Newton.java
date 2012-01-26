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

package org.addition.epanet.msx.Solvers;


import org.addition.epanet.msx.Utilities;

public class Newton {

    int      Nmax;      // max. number of equations
    int      []Indx;    // permutation vector of row indexes
    double   []F;       // function & adjustment vector
    double   []W;       // work vector
    double   [][]J;     // Jacobian matrix

    /**
     * opens the algebraic solver to handle a system of n equations.
      */
    public void newton_open(int n)
    {
        Nmax    = 0;
        Indx    = null;
        F       = null;
        W       = null;
        Indx	= new int[n+1];
        F   	= new double[n+1];
        W		= new double[n+1];
        J       = Utilities.createMatrix(n+1, n+1);
        Nmax = n;
    }


    //=============================================================================
    // uses newton-raphson iterations to solve n nonlinear eqns.
    /*public int newton_solve(double x[], int n, int maxit, int numsig,
                     JacobianFunction func)
    {
        int i, k;
        double errx, errmax, cscal, relconvg = Math.pow(10.0, -numsig);

        // --- check that system was sized adequetely

        if ( n > Nmax ) return -3;

        // --- use up to maxit iterations to find a solution

        for (k=1; k<=maxit; k++)
        {
            // --- evaluate the Jacobian matrix

            Utilities.jacobian(x, n, F, W, J, func);

            // --- factorize the Jacobian

            if ( Utilities.factorize(J, n, W, Indx) ==0 ) return -1;

            // --- solve for the updates to x (returned in F)

            for (i=1; i<=n; i++) F[i] = -F[i];
            Utilities.solve(J, n, Indx, F);

            // --- update solution x & check for convergence

            errmax = 0.0;
            for (i=1; i<=n; i++)
            {
                cscal = x[i];
                if (cscal < relconvg) cscal = relconvg;
                x[i] += F[i];
                errx = Math.abs(F[i]/cscal);
                if (errx > errmax) errmax = errx;
            }
            if (errmax <= relconvg) return k;
        }

        // --- return error code if no convergence

        return -2;
    }*/

    /**
     * uses newton-raphson iterations to solve n nonlinear eqns.
     */
    public int newton_solve(double x[], int n, int maxit, int numsig,
                     JacobianInterface jint, JacobianInterface.Operation op)
    {
        int i, k;
        double errx, errmax, cscal, relconvg = Math.pow(10.0, -numsig);

        // check that system was sized adequetely

        if ( n > Nmax ) return -3;

        // use up to maxit iterations to find a solution

        for (k=1; k<=maxit; k++)
        {
            // evaluate the Jacobian matrix

            Utilities.jacobian(x, n, F, W, J, jint,op);

            // factorize the Jacobian

            if ( Utilities.factorize(J, n, W, Indx) ==0 ) return -1;

            // solve for the updates to x (returned in F)

            for (i=1; i<=n; i++) F[i] = -F[i];
            Utilities.solve(J, n, Indx, F);

            // update solution x & check for convergence

            errmax = 0.0;
            for (i=1; i<=n; i++)
            {
                cscal = x[i];
                if (cscal < relconvg) cscal = relconvg;
                x[i] += F[i];
                errx = Math.abs(F[i]/cscal);
                if (errx > errmax) errmax = errx;
            }
            if (errmax <= relconvg) return k;
        }

        // return error code if no convergence

        return -2;
    }
}
