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

public class ros2 {

    double[][]  A;                     // Jacobian matrix
    double[]    K1;                    // Intermediate solutions
    double[]    K2;
    double[]    Ynew;                  // Updated function values
    int[]       Jindx;                 // Jacobian column indexes
    int         Nmax;                  // Max. number of equations
    int         Adjust;                // use adjustable step size


    /**
     * Opens the ROS2 integrator.
      */
    public void ros2_open(int n, int adjust)
    {
        int n1 = n + 1;
        Nmax = 0;
        Adjust = adjust;

        K1    = new double[n1];
        K2    = new double[n1];
        Jindx = new int [n1];
        Ynew  = new double[n1];
        A = Utilities.createMatrix(n1, n1);
        Nmax = n;
    }

    /*public int ros2_integrate(double y[], int n, double t, double tnext,
                       double[] htry, double atol[], double rtol[],
                       JacobianFunction func)
    {
        double UROUND = 2.3e-16;
        double g, ghinv, ghinv1, dghinv, ytol;
        double h, hold, hmin, hmax, tplus;
        double ej, err, factor, facmax;
        int    nfcn, njac, naccept, nreject, j;
        int    isReject;
        int    adjust = Adjust;

// --- Initialize counters, etc.

        g = 1.0 + 1.0 / Math.sqrt(2.0);
        ghinv1 = 0.0;
        tplus = t;
        isReject = 0;
        naccept  = 0;
        nreject  = 0;
        nfcn     = 0;
        njac     = 0;

// --- Initial step size

        hmax = tnext - t;
        hmin = 1.e-8;
        h = htry[0];
        if ( h == 0.0 )
        {
            func.solve(t, y, n, K1);
            nfcn += 1;
            adjust = 1;
            h = tnext - t;
            for (j=1; j<=n; j++)
            {
                ytol = atol[j] + rtol[j]*Math.abs(y[j]);
                if (K1[j] != 0.0) h = Math.min(h, (ytol / Math.abs(K1[j])));
            }
        }
        h = Math.max(hmin, h);
        h = Math.min(hmax, h);

// --- Start the time loop

        while ( t < tnext )
        {
            // --- check for zero step size

            if (0.10*Math.abs(h) <= Math.abs(t)*UROUND) return -2;

            // --- adjust step size if interval exceeded

            tplus = t + h;
            if ( tplus > tnext )
            {
                h = tnext - t;
                tplus = tnext;
            }

            // --- Re-compute the Jacobian if step size accepted

            if ( isReject == 0 )
            {
                Utilities.jacobian(y, n, K1, K2, A, func);
                njac++;
                nfcn += 2*n;
                ghinv1 = 0.0;
            }

            // --- Update the Jacobian to reflect new step size

            ghinv = -1.0 / (g*h);
            dghinv = ghinv - ghinv1;
            for (j=1; j<=n; j++)
            {
                A[j][j] += dghinv;
            }
            ghinv1 = ghinv;
            if ( Utilities.factorize(A, n, K1, Jindx) ==0) return -1;

            // --- Stage 1 solution

            func.solve(t, y, n, K1);
            nfcn += 1;
            for (j=1; j<=n; j++) K1[j] *= ghinv;
            Utilities.solve(A, n, Jindx, K1);

            // --- Stage 2 solution

            for (j=1; j<=n; j++)
            {
                Ynew[j] = y[j] + h*K1[j];
            }
            func.solve(t, Ynew, n, K2);
            nfcn += 1;
            for (j=1; j<=n; j++)
            {
                K2[j] = (K2[j] - 2.0*K1[j])*ghinv;
            }
            Utilities.solve(A, n, Jindx, K2);

            // --- Overall solution

            for (j=1; j<=n; j++)
            {
                Ynew[j] = y[j] + 1.5*h*K1[j] + 0.5*h*K2[j];
            }

            // --- Error estimation

            hold = h;
            err = 0.0;
            if ( adjust !=0 )
            {
                for (j=1; j<=n; j++)
                {
                    ytol = atol[j] + rtol[j]*Math.abs(Ynew[j]);
                    ej = Math.abs(Ynew[j] - y[j] - h * K1[j])/ytol;
                    err = err + ej*ej;
                }
                err = Math.sqrt(err / n);
                err = Math.max(UROUND, err);

                // --- Choose the step size

                factor = 0.9 / Math.sqrt(err);
                if (isReject!=0) facmax = 1.0;
                else          facmax = 10.0;
                factor = Math.min(factor, facmax);
                factor = Math.max(factor, 1.0e-1);
                h = factor*h;
                h = Math.min(hmax, h);
            }

            // --- Reject/accept the step

            if ( err > 1.0 )
            {
                isReject = 1;
                nreject++;
                h = 0.5*h;
            }
            else
            {
                isReject = 0;
                for (j=1; j<=n; j++)
                {
                    y[j] = Ynew[j];
                    if ( y[j] <= UROUND ) y[j] = 0.0;
                }
                if ( adjust!=0 ) htry[0] = h;
                t = tplus;
                naccept++;
            }

// --- End of the time loop

        }
        return nfcn;
    } */

    /**
     * Integrates a system of ODEs over a specified time interval.
     */
    public int ros2_integrate(double y[], int n, double t, double tnext,
                       double[] htry, double atol[], double rtol[],
                       JacobianInterface jInt,JacobianInterface.Operation op)
    {
        double UROUND = 2.3e-16;
        double g, ghinv, ghinv1, dghinv, ytol;
        double h, hold, hmin, hmax, tplus;
        double ej, err, factor, facmax;
        int    nfcn, njac, naccept, nreject, j;
        int    isReject;
        int    adjust = Adjust;

        // Initialize counters, etc.
        g = 1.0 + 1.0 / Math.sqrt(2.0);
        ghinv1 = 0.0;
        tplus = t;
        isReject = 0;
        naccept  = 0;
        nreject  = 0;
        nfcn     = 0;
        njac     = 0;

        // Initial step size
        hmax = tnext - t;
        hmin = 1.e-8;
        h = htry[0];
        if ( h == 0.0 )
        {
            jInt.solve(t, y, n, K1,0,op);
            nfcn += 1;
            adjust = 1;
            h = tnext - t;
            for (j=1; j<=n; j++)
            {
                ytol = atol[j] + rtol[j]*Math.abs(y[j]);
                if (K1[j] != 0.0) h = Math.min(h, (ytol / Math.abs(K1[j])));
            }
        }
        h = Math.max(hmin, h);
        h = Math.min(hmax, h);

        // Start the time loop
        while ( t < tnext )
        {
            // Check for zero step size

            if (0.10*Math.abs(h) <= Math.abs(t)*UROUND) return -2;

            // Adjust step size if interval exceeded

            tplus = t + h;
            if ( tplus > tnext )
            {
                h = tnext - t;
                tplus = tnext;
            }

            // Re-compute the Jacobian if step size accepted

            if ( isReject == 0 )
            {
                Utilities.jacobian(y, n, K1, K2, A, jInt,op);
                njac++;
                nfcn += 2*n;
                ghinv1 = 0.0;
            }

            // Update the Jacobian to reflect new step size
            ghinv = -1.0 / (g*h);
            dghinv = ghinv - ghinv1;
            for (j=1; j<=n; j++)
            {
                A[j][j] += dghinv;
            }
            ghinv1 = ghinv;
            if ( Utilities.factorize(A, n, K1, Jindx) ==0) return -1;

            // Stage 1 solution

            jInt.solve(t, y, n, K1,0,op);
            nfcn += 1;
            for (j=1; j<=n; j++) K1[j] *= ghinv;
            Utilities.solve(A, n, Jindx, K1);

            // Stage 2 solution

            for (j=1; j<=n; j++)
            {
                Ynew[j] = y[j] + h*K1[j];
            }
            jInt.solve(t, Ynew, n, K2,0,op);
            nfcn += 1;
            for (j=1; j<=n; j++)
            {
                K2[j] = (K2[j] - 2.0*K1[j])*ghinv;
            }
            Utilities.solve(A, n, Jindx, K2);

            // Overall solution

            for (j=1; j<=n; j++)
            {
                Ynew[j] = y[j] + 1.5*h*K1[j] + 0.5*h*K2[j];
            }

            // Error estimation
            hold = h;
            err = 0.0;
            if ( adjust !=0 )
            {
                for (j=1; j<=n; j++)
                {
                    ytol = atol[j] + rtol[j]*Math.abs(Ynew[j]);
                    ej = Math.abs(Ynew[j] - y[j] - h * K1[j])/ytol;
                    err = err + ej*ej;
                }
                err = Math.sqrt(err / n);
                err = Math.max(UROUND, err);

                // Choose the step size

                factor = 0.9 / Math.sqrt(err);
                if (isReject!=0) facmax = 1.0;
                else          facmax = 10.0;
                factor = Math.min(factor, facmax);
                factor = Math.max(factor, 1.0e-1);
                h = factor*h;
                h = Math.min(hmax, h);
            }

            // Reject/accept the step
            if ( err > 1.0 )
            {
                isReject = 1;
                nreject++;
                h = 0.5*h;
            }
            else
            {
                isReject = 0;
                for (j=1; j<=n; j++)
                {
                    y[j] = Ynew[j];
                    if ( y[j] <= UROUND ) y[j] = 0.0;
                }
                if ( adjust!=0 ) htry[0] = h;
                t = tplus;
                naccept++;
            }

            // End of the time loop

        }
        return nfcn;
    }
}
