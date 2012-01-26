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

public class rk5 {

    int      Nmax;      // max. number of equations
    int      Itmax;     // max. number of integration steps
    int      Adjust;    // use adjustable step size
    double   []Ak;      // work arrays
    double   []K1;

    int K2off;
    int K3off;
    int K4off;
    int K5off;
    int K6off;

    double   []Ynew;    // updated solution


    /**
     * Opens the RK5 solver to solve system of n equations
      */
    public void rk5_open(int n, int itmax, int adjust)
    {
        int n1 = n+1;
        Nmax  = 0;
        Itmax = itmax;
        Adjust = adjust;
        Ynew  = new double [n1];
        Ak    = new double [n1*6];
        Nmax = n;
        K1 = (Ak);
        K2off = (n1);
        K3off = (2*n1);
        K4off = (3*n1);
        K5off = (4*n1);
        K6off = (5*n1);
    }


    //=============================================================================
    //Integrates system of equations dY/dt = F(t,Y) over a
    //    given interval.
    /*public int rk5_integrate(double y[], int n, double t, double tnext,
                      double [] htry, double atol[], double rtol[],
                      JacobianFunction func)

    {
        double c2=0.20, c3=0.30, c4=0.80, c5=8.0/9.0;
        double a21=0.20, a31=3.0/40.0, a32=9.0/40.0,
                a41=44.0/45.0, a42=-56.0/15.0, a43=32.0/9.0,
                a51=19372.0/6561.0, a52=-25360.0/2187.0, a53=64448.0/6561.0,
                a54=-212.0/729.0, a61=9017.0/3168.0, a62=-355.0/33.0,
                a63=46732.0/5247.0, a64=49.0/176.0, a65=-5103.0/18656.0,
                a71=35.0/384.0, a73=500.0/1113.0, a74=125.0/192.0,
                a75=-2187.0/6784.0, a76=11.0/84.0;
        double e1=71.0/57600.0, e3=-71.0/16695.0, e4=71.0/1920.0,
                e5=-17253.0/339200.0, e6=22.0/525.0, e7=-1.0/40.0;

        double tnew, h, hmax, hnew, ytol, err, sk, fac, fac11 = 1.0;
        int    i;

// --- parameters for step size control

        double UROUND = 2.3e-16;
        double SAFE = 0.90;
        double fac1 = 0.2;
        double fac2 = 10.0;
        double beta = 0.04;
        double facold = 1.e-4;
        double expo1 = 0.2 - beta*0.75;
        double facc1 = 1.0/fac1;
        double facc2 = 1.0/fac2;

// --- various counters

        int    nstep = 1;
        int    nfcn  = 0;
        int    naccpt = 0;
        int    nrejct = 0;
        int    reject = 0;
        int    adjust = Adjust;

// --- initial function evaluation

        func.solve(t, y, n, K1);
        nfcn++;

// --- initial step size
        h = htry[0];
        hmax = tnext - t;
        if (h == 0.0)
        {
            adjust = 1;
            h = tnext - t;
            for (i=1; i<=n; i++)
            {
                ytol = atol[i] + rtol[i]*Math.abs(y[i]);
                if (K1[i] != 0.0) h = Math.min(h, (ytol / Math.abs(K1[i])));
            }
        }
        h = Math.max(1.e-8, h);

// --- while not at end of time interval

        while (t < tnext)
        {
            // --- check for zero step size
            if (0.10*Math.abs(h) <= Math.abs(t)*UROUND) return -2;

            // --- adjust step size if interval exceeded
            if ((t + 1.01*h - tnext) > 0.0) h = tnext - t;

            tnew = t + c2*h;
            for (i=1; i<=n; i++)
                Ynew[i] = y[i] + h*a21*K1[i];
            func.solve(tnew, Ynew, n, K1, K2off);

            tnew = t + c3*h;
            for (i=1; i<=n; i++)
                Ynew[i] = y[i] + h*(a31*K1[i] + a32*K1[K2off+i]);    // y[i] + h*(a31*K1[i] + a32*K2[i]
            func.solve(tnew, Ynew, n, K1, K3off);

            tnew = t + c4*h;
            for (i=1; i<=n; i++)
                Ynew[i]=y[i] + h*(a41*K1[i] + a42*K1[K2off+i] + a43*K1[K3off+i]);//a42*K2[i] + a43*K3[i]);
            func.solve(tnew, Ynew, n, K1, K4off);

            tnew = t + c5*h;
            for (i=1; i<=n; i++)
                Ynew[i] = y[i] + h*(a51*K1[i] + a52*K1[K2off+i] + a53*K1[K3off+i]+a54*K1[K4off + i]);//a52*K2[i] + a53*K3[i]+a54*K4[i]);
            func.solve(tnew, Ynew, n, K1, K5off);

            tnew = t + h;
            for (i=1; i<=n; i++)
                Ynew[i] = y[i] + h*(a61*K1[i] + a62*K1[i+K2off] +
                        a63*K1[i+K3off] + a64*K1[i+K4off] + a65*K1[i+K5off]);
                //Ynew[i] = y[i] + h*(a61*K1[i] + a62*K2[i] +
                //        a63*K3[i] + a64*K4[i] + a65*K5[i]);
            func.solve(tnew, Ynew, n, K1,K6off);

            for (i=1; i<=n; i++)
                Ynew[i] = y[i] + h*(a71*K1[i] + a73*K1[i+K3off] +
                        a74*K1[i+K4off] + a75*K1[i+K5off] + a76*K1[i+K6off]);

               // Ynew[i] = y[i] + h*(a71*K1[i] + a73*K3[i] +
                //        a74*K4[i] + a75*K5[i] + a76*K6[i]);
            func.solve(tnew, Ynew, n, K1,K2off);
            nfcn += 6;

            // --- step size adjustment

            err = 0.0;
            hnew = h;
            if (adjust!=0)
            {
                for (i=1; i<=n; i++)
                    K1[i+K4off] = (e1*K1[i] + e3*K1[i+K3off] + e4*K1[i+K4off] + e5*K1[i+K5off] +
                            e6*K1[i+K6off] + e7*K1[i+K2off])*h;
                    //K4[i] = (e1*K1[i] + e3*K3[i] + e4*K4[i] + e5*K5[i] +
                    //        e6*K6[i] + e7*K2[i])*h;

                for (i=1; i<=n; i++)
                {
                    sk = atol[i] + rtol[i]*Math.max(Math.abs(y[i]), Math.abs(Ynew[i]));
                    sk = K1[i+K4off]/sk;//K4[i]/sk;
                    err = err + (sk*sk);
                }
                err = Math.sqrt(err/n);

                // --- computation of hnew
                fac11 = Math.pow(err, expo1);
                fac = fac11/Math.pow(facold, beta);               // LUND-stabilization
                fac = Math.max(facc2, Math.min(facc1, (fac/SAFE)));  // must have FAC1 <= HNEW/H <= FAC2
                hnew = h/fac;
            }

            // --- step is accepted

            if( err <= 1.0 )
            {
                facold = Math.max(err, 1.0e-4);
                naccpt++;
                for (i=1; i<=n; i++)
                {
                    K1[i] = K1[i+K2off];//K2[i];
                    y[i] = Ynew[i];
                }
                t = t + h;
                if ( adjust!=0 && t <= tnext ) htry[0] = h;
                if (Math.abs(hnew) > hmax) hnew = hmax;
                if (reject!=0) hnew = Math.min(Math.abs(hnew), Math.abs(h));
                reject = 0;
                //if (Report) Report(t, y, n);
            }

            // --- step is rejected

            else
            {
                if ( adjust !=0) hnew = h/Math.min(facc1, (fac11 / SAFE));
                reject = 1;
                if (naccpt >= 1) nrejct++;
            }

            // --- take another step

            h = hnew;
            if ( adjust !=0) htry[0] = h;
            nstep++;
            if (nstep >= Itmax)
                return -1;
        }
        return nfcn;
    }*/

    /**
     * Integrates system of equations dY/dt = F(t,Y) over a given interval.
      */
    public int rk5_integrate(double y[], int n, double t, double tnext,
                      double [] htry, double atol[], double rtol[],
                      JacobianInterface jInt,JacobianInterface.Operation op)

    {
        double c2=0.20, c3=0.30, c4=0.80, c5=8.0/9.0;
        double a21=0.20, a31=3.0/40.0, a32=9.0/40.0,
                a41=44.0/45.0, a42=-56.0/15.0, a43=32.0/9.0,
                a51=19372.0/6561.0, a52=-25360.0/2187.0, a53=64448.0/6561.0,
                a54=-212.0/729.0, a61=9017.0/3168.0, a62=-355.0/33.0,
                a63=46732.0/5247.0, a64=49.0/176.0, a65=-5103.0/18656.0,
                a71=35.0/384.0, a73=500.0/1113.0, a74=125.0/192.0,
                a75=-2187.0/6784.0, a76=11.0/84.0;
        double e1=71.0/57600.0, e3=-71.0/16695.0, e4=71.0/1920.0,
                e5=-17253.0/339200.0, e6=22.0/525.0, e7=-1.0/40.0;

        double tnew, h, hmax, hnew, ytol, err, sk, fac, fac11 = 1.0;
        int    i;

        // parameters for step size control
        double UROUND = 2.3e-16;
        double SAFE = 0.90;
        double fac1 = 0.2;
        double fac2 = 10.0;
        double beta = 0.04;
        double facold = 1.e-4;
        double expo1 = 0.2 - beta*0.75;
        double facc1 = 1.0/fac1;
        double facc2 = 1.0/fac2;

        // various counters
        int    nstep = 1;
        int    nfcn  = 0;
        int    naccpt = 0;
        int    nrejct = 0;
        int    reject = 0;
        int    adjust = Adjust;

        // initial function evaluation
        jInt.solve(t, y, n, K1,0,op);
        nfcn++;

        // initial step size
        h = htry[0];
        hmax = tnext - t;
        if (h == 0.0)
        {
            adjust = 1;
            h = tnext - t;
            for (i=1; i<=n; i++)
            {
                ytol = atol[i] + rtol[i]*Math.abs(y[i]);
                if (K1[i] != 0.0) h = Math.min(h, (ytol / Math.abs(K1[i])));
            }
        }
        h = Math.max(1.e-8, h);

        // while not at end of time interval
        while (t < tnext)
        {
            // --- check for zero step size
            if (0.10*Math.abs(h) <= Math.abs(t)*UROUND) return -2;

            // --- adjust step size if interval exceeded
            if ((t + 1.01*h - tnext) > 0.0) h = tnext - t;

            tnew = t + c2*h;
            for (i=1; i<=n; i++)
                Ynew[i] = y[i] + h*a21*K1[i];
            jInt.solve(tnew, Ynew, n, K1, K2off,op);

            tnew = t + c3*h;
            for (i=1; i<=n; i++)
                Ynew[i] = y[i] + h*(a31*K1[i] + a32*K1[K2off+i]);    // y[i] + h*(a31*K1[i] + a32*K2[i]
            jInt.solve(tnew, Ynew, n, K1, K3off,op);

            tnew = t + c4*h;
            for (i=1; i<=n; i++)
                Ynew[i]=y[i] + h*(a41*K1[i] + a42*K1[K2off+i] + a43*K1[K3off+i]);//a42*K2[i] + a43*K3[i]);
            jInt.solve(tnew, Ynew, n, K1, K4off,op);

            tnew = t + c5*h;
            for (i=1; i<=n; i++)
                Ynew[i] = y[i] + h*(a51*K1[i] + a52*K1[K2off+i] + a53*K1[K3off+i]+a54*K1[K4off + i]);//a52*K2[i] + a53*K3[i]+a54*K4[i]);
            jInt.solve(tnew, Ynew, n, K1, K5off,op);

            tnew = t + h;
            for (i=1; i<=n; i++)
                Ynew[i] = y[i] + h*(a61*K1[i] + a62*K1[i+K2off] +
                        a63*K1[i+K3off] + a64*K1[i+K4off] + a65*K1[i+K5off]);
                //Ynew[i] = y[i] + h*(a61*K1[i] + a62*K2[i] +
                //        a63*K3[i] + a64*K4[i] + a65*K5[i]);
            jInt.solve(tnew, Ynew, n, K1,K6off,op);

            for (i=1; i<=n; i++)
                Ynew[i] = y[i] + h*(a71*K1[i] + a73*K1[i+K3off] +
                        a74*K1[i+K4off] + a75*K1[i+K5off] + a76*K1[i+K6off]);

               // Ynew[i] = y[i] + h*(a71*K1[i] + a73*K3[i] +
                //        a74*K4[i] + a75*K5[i] + a76*K6[i]);
            jInt.solve(tnew, Ynew, n, K1,K2off,op);
            nfcn += 6;

            // step size adjustment

            err = 0.0;
            hnew = h;
            if (adjust!=0)
            {
                for (i=1; i<=n; i++)
                    K1[i+K4off] = (e1*K1[i] + e3*K1[i+K3off] + e4*K1[i+K4off] + e5*K1[i+K5off] +
                            e6*K1[i+K6off] + e7*K1[i+K2off])*h;
                    //K4[i] = (e1*K1[i] + e3*K3[i] + e4*K4[i] + e5*K5[i] +
                    //        e6*K6[i] + e7*K2[i])*h;

                for (i=1; i<=n; i++)
                {
                    sk = atol[i] + rtol[i]*Math.max(Math.abs(y[i]), Math.abs(Ynew[i]));
                    sk = K1[i+K4off]/sk;//K4[i]/sk;
                    err = err + (sk*sk);
                }
                err = Math.sqrt(err/n);

                // computation of hnew
                fac11 = Math.pow(err, expo1);
                fac = fac11/Math.pow(facold, beta);               // LUND-stabilization
                fac = Math.max(facc2, Math.min(facc1, (fac/SAFE)));  // must have FAC1 <= HNEW/H <= FAC2
                hnew = h/fac;
            }

            // step is accepted

            if( err <= 1.0 )
            {
                facold = Math.max(err, 1.0e-4);
                naccpt++;
                for (i=1; i<=n; i++)
                {
                    K1[i] = K1[i+K2off];//K2[i];
                    y[i] = Ynew[i];
                }
                t = t + h;
                if ( adjust!=0 && t <= tnext ) htry[0] = h;
                if (Math.abs(hnew) > hmax) hnew = hmax;
                if (reject!=0) hnew = Math.min(Math.abs(hnew), Math.abs(h));
                reject = 0;
                //if (Report) Report(t, y, n);
            }

            // step is rejected

            else
            {
                if ( adjust !=0) hnew = h/Math.min(facc1, (fac11 / SAFE));
                reject = 1;
                if (naccpt >= 1) nrejct++;
            }

            // take another step

            h = hnew;
            if ( adjust !=0) htry[0] = h;
            nstep++;
            if (nstep >= Itmax)
                return -1;
        }
        return nfcn;
    }
}
