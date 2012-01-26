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


import org.addition.epanet.msx.Solvers.JacobianInterface;

public class Utilities {
    public static int CALL(int err, int f){
        return  (err>100) ? (err) : (f);
    }

    // performs case insensitive comparison of two strings.
    public static  boolean  MSXutils_strcomp(String s1, String s2)
    {
        return s1.equalsIgnoreCase(s2);
    }

    //=============================================================================
    // finds a match between a string and an array of keyword strings.
    public static int  MSXutils_findmatch(String s, String [] keyword)
    {
        int i = 0;
        //while (keyword[i] != NULL)
        for (String key : keyword)
        {
            if (MSXutils_match(s, key)) return(i);
            i++;
        }
        return(-1);
    }

    //=============================================================================
    // sees if a sub-string of characters appears in a string
    public static boolean  MSXutils_match(String a, String b)
    {
        int i,j;

        a.trim();
        b.trim();

        // --- fail if substring is empty
        if (b.length()==0) return(false);


        // --- skip leading blanks of str
        //for (i=0; str[i]; i++)
        //    if (str[i] != ' ') break;

        // --- check if substr matches remainder of str
        //for (i=i,j=0; substr[j]; i++,j++)
        //    if (!str[i] || UCHAR(str[i]) != UCHAR(substr[j]))
        //        return(false);

        if(a.toLowerCase().contains(b.toLowerCase()))
            return true;


        return(false);
    }

    //=============================================================================
    // converts a string in either decimal hours or hr:min:sec
    //    format to number of seconds.
    public static boolean MSXutils_strToSeconds(String s, long [] seconds)
    {
        //int [] hr = new int [1], min = new int [1], sec = new int [1];
        double [] hours= new double[1];
        seconds[0] = 0;
        if ( MSXutils_getDouble(s, hours) )
        {
            seconds[0] = (long)(3600.0*hours[0]);
            return true;
        }
        //n = sscanf(s, "%d:%d:%d", hr, min, sec);
        s.trim();
        String [] elements = s.split("[.]");

        if ( elements.length == 0 ) return false;
        seconds[0] = Integer.parseInt(elements[0])*3600 + Integer.parseInt(elements[1])*60 +Integer.parseInt(elements[2]);  //3600*hr + 60*min + sec;
        return true;
    }

    //=============================================================================
    // Converts a string to an integer number.
    public static boolean  MSXutils_getInt(String s, int [] y)
    {
        double x;
        //if ( MSXutils_getDouble(s, &x) )
        //{
        //    if ( x < 0.0 ) x -= 0.01;
        //    else x += 0.01;
        //    *y = (int)x;
        //    return 1;
        //}

        try{
            y[0] = Integer.parseInt(s);
            return true;
        }
        catch(Exception e){
            y[0] = 0;
            return false;
        }

    }

    //=============================================================================
    // Converts a string to a single precision floating point number.
    public static boolean  MSXutils_getFloat(String s, float [] y){
        try{
            y[0] = Float.parseFloat(s);
            return true;
        }
        catch(Exception e){
            y[0] = 0.0f;
            return false;
        }
    }

    //=============================================================================
    // converts a string to a double precision floating point number.
    public static boolean  MSXutils_getDouble(String s, double [] y)
    {
        try{
            y[0] = Double.parseDouble(s);
            return true;
        }
        catch(Exception e){
            y[0] = 0.0;
            return false;
        }
    }

    //=============================================================================
    // allocates memory for a 2-dimensional array of doubles.
    public static double [][] createMatrix(int nrows, int ncols)
    {
        //int i,j;
        //double **a;
        //
        //// --- allocate pointers to rows
        //
        //a = (double **) malloc(nrows * sizeof(double *));
        //if ( !a ) return NULL;
        //
        //// --- allocate rows and set pointers to them
        //
        //a[0] = (double *) malloc (nrows * ncols * sizeof(double));
        //if ( !a[0] ) return NULL;
        //for ( i = 1; i < nrows; i++ ) a[i] = a[i-1] + ncols;
        //
        //for ( i = 0; i < nrows; i++)
        //{
        //    for ( j = 0; j < ncols; j++) a[i][j] = 0.0;
        //}
        //
        //// --- return pointer to array of pointers to rows
        //
        //return a;
        return new double[nrows][ncols];
    }

//=============================================================================

    // performs an LU decomposition of a matrix.
    public static int factorize(double [][]a, int n, double []w, int []indx)
    {
        int    i, imax, j, k;
        double big, dum, sum, temp;

        for (i = 1; i <= n; i++)
        {
            //Loop over rows to get the implicit scaling information.
            big = 0.0;
            for (j = 1;j <= n;j++)
                if ((temp = Math.abs(a[i][j])) > big) big = temp;
            if (big == 0.0)
                return 0;  // Warning for singular matrix
            //No nonzero largest element.
            w[i] = 1.0/big; //Save the scaling.
        }
        for (j = 1;j <= n;j++) //for each column
        {
            //This is the loop over columns of Croutís method.
            for (i = 1; i < j; i++)
            {
                //Up from the diagonal
                sum = a[i][j];
                for (k = 1;k < i;k++)
                    sum -= a[i][k]*a[k][j];
                a[i][j] = sum;
            }
            big = 0.0; //Initialize for the search for largest pivot element.
            imax = j;
            for (i = j; i <= n; i++)
            {
                sum = a[i][j];
                for (k = 1; k < j; k++)
                    sum -= a[i][k]*a[k][j];
                a[i][j] = sum;
                if ( (dum = w[i]*Math.abs(sum)) >= big)
                {
                    big = dum;
                    imax = i;
                }
            }
            if (j != imax)
            {
                //Do we need to interchange rows?
                for (k = 1; k <= n; k++)
                {
                    //Yes,do so...
                    dum = a[imax][k];
                    a[imax][k] = a[j][k];
                    a[j][k] = dum;
                }
                w[imax] = w[j];// interchange the scale factor.
            }
            indx[j] = imax;
            if (a[j][j] == 0.0) a[j][j] = Constants.TINY1;
            if (j != n) // divide by the pivot element.
            {
                dum = 1.0/(a[j][j]);
                for (i = j+1;i <= n;i++) a[i][j] *= dum;
            }
        }
        return 1;
    }

//=============================================================================
    // solves linear equations AX = B after LU decomposition of A.
    public static void solve(double [][]a, int n, int []indx, double b[])
    {
        int i, ii=0, ip, j;
        double sum=0.0d;

        //forward substitution
        for (i=1; i<=n; i++)
        {
            ip=indx[i];
            sum=b[ip];
            b[ip]=b[i];
            if (ii!=0)
                for (j=ii; j<=i-1; j++)
                    sum -= a[i][j]*b[j];
            else if (sum!=0) ii=i;
            b[i]=sum;
        }

       // back substitution
        for (i=n; i>=1; i--)
        {
            sum=b[i];
            for (j=i+1; j<=n; j++)
                sum -= a[i][j]*b[j];
            b[i]=sum/a[i][i];
        }
    }


    //=============================================================================
    // computes Jacobian matrix of F(t,X) at given X
    /*public static void jacobian(double [] x, int n, double [] f, double [] w, double [][]a, JacobianFunction func)
                  //void (*func)(double, double*, int, double*))

    {

        int    i, j;
        double temp, eps = 1.0e-7, eps2;

        for (j=1; j<=n; j++)
        {
            temp = x[j];
            x[j] = temp + eps;
            func.solve(0.0, x, n, f);
            if ( temp == 0.0 )
            {
                x[j] = temp;
                eps2 = eps;
            }
            else
            {
                x[j] = temp - eps;
                eps2 = 2.0*eps;
            }
            func.solve(0.0, x, n, w);
            for (i=1; i<=n; i++) a[i][j] = (f[i] - w[i]) / eps2;
            x[j] = temp;
        }

    }*/

    // computes Jacobian matrix of F(t,X) at given X
    public static void jacobian(double [] x, int n, double [] f, double [] w, double [][]a, JacobianInterface jint, JacobianInterface.Operation op){
        int    i, j;
        double temp, eps = 1.0e-7, eps2;

        for (j=1; j<=n; j++)
        {
            temp = x[j];
            x[j] = temp + eps;
            jint.solve(0.0, x, n, f,0,op);
            if ( temp == 0.0 )
            {
                x[j] = temp;
                eps2 = eps;
            }
            else
            {
                x[j] = temp - eps;
                eps2 = 2.0*eps;
            }
            jint.solve(0.0, x, n, w,0,op);
            for (i=1; i<=n; i++) a[i][j] = (f[i] - w[i]) / eps2;
            x[j] = temp;
        }

    }
}
