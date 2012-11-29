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

package org.addition.epanet.util;



import org.addition.epanet.Constants;
import org.addition.epanet.network.io.Keywords;

import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Aware-P Epanet utilities methods.
 */
public class Utilities {

    /**
     * Convert string to double.
     * @param str Number string.
     * @return Reference to the converted Double number, null if the conversion was unsuccessful.
     */
    public static Double getDouble(String str){
        Double val;
        try{
            val = Double.parseDouble(str);
            return val;
        }
        catch(NumberFormatException ex)
        {
            return null;
        }
    }

    /**
     * Check if two strings match (case independent), based on the shortest string length.
     * @param a String A.
     * @param b String B.
     * @return Boolean is the two strings are similar.
     */
    public static boolean match(String a, String b){
        if(a.length()==0||b.length()==0)
            return false;

        if(a.length()>b.length())
        {
            String tmp = a.substring(0,b.length());
            if(tmp.equalsIgnoreCase(b))
                return true;
        }
        else
        {
            String tmp = b.substring(0,a.length());
            if(a.equalsIgnoreCase(tmp))
                return true;
        }
        return false;
    }

    /**
     * Parse time string to a double number.
     * @param time  Time string.
     * @param units Units format (PM or AM)
     * @return Time in hours (1.0 is 3600 seconds)
     */
    public static double getHour(String time, String units)
    {
        int    n=0;
        double  [] y = {0.0d,0.0d,0.0d};
        String [] s = time.split(":");


        for(int i = 0;i<s.length && i<=3;i++)
        {
            try{y[i] = Double.parseDouble(s[i]);}catch(NumberFormatException ex){return(-1.0d);}
            n++;
        }

        if (n == 1)
        {
            if (units.length() == 0)
                return(y[0]);

            if (Utilities.match(units, Keywords.w_SECONDS)) return(y[0]/3600.0);
            if (Utilities.match(units,Keywords.w_MINUTES)) return(y[0]/60.0);
            if (Utilities.match(units,Keywords.w_HOURS))   return(y[0]);
            if (Utilities.match(units,Keywords.w_DAYS))    return(y[0]*24.0);
        }

        if (n > 1) y[0] = y[0] + y[1]/60.0 + y[2]/3600.0;

        if (units.length() == 0)
            return(y[0]);

        if (units.equalsIgnoreCase(Keywords.w_AM))
        {
            if (y[0] >= 13.0) return(-1.0);
            if (y[0] >= 12.0) return(y[0]-12.0);
            else return(y[0]);
        }
        if (units.equalsIgnoreCase(Keywords.w_PM))
        {
            if (y[0] >= 13.0) return(y[0]-12.0);
            if (y[0] >= 12.0) return(y[0]);
            else return(y[0]+12.0);
        }
        return(-1.0);
    }

    /**
     * Compute the linear interpolation of a 2d cartesian graph.
     * @param n Number of points.
     * @param x X axis values array (abscissa).
     * @param y Y axis values array (ordinate).
     * @param xx The abscissa value.
     * @return The interpolated value.
     */
    public static double linearInterpolator(int n, List<Double> x, List<Double> y, double xx)
    {
        int    k,m;
        double  dx,dy;

        m = n - 1;
        if (xx <= x.get(0)) return(y.get(0));
        for (k=1; k<=m; k++)
        {
            if (x.get(k) >= xx)
            {
                dx = x.get(k)-x.get(k-1);
                dy = y.get(k)-y.get(k-1);
                if (Math.abs(dx) < Constants.TINY) return(y.get(k));
                else return(y.get(k) - (x.get(k)-xx)*dy/dx);
            }
        }
        return(y.get(m));
    }

    /**
     * Convert time to a string.
     * @param seconds Time to convert, in seconds.
     * @return Time string in epanet format.
     */
    public static String getClockTime(long seconds){
        long h,m,s;
        h = seconds/3600;
        m = (seconds % 3600) / 60;
        s = seconds - 3600*h - 60*m;
        return String.format("%02d:%02d:%02d",h,m,s);
    }

    /**
     * Computes coeffs. for pump curve.
     * @param h0 shutoff head
     * @param h1 design head
     * @param h2 head at max. flow
     * @param q1 design flow
     * @param q2 max. flow
     * @param a pump curve coeffs. (H = a-bQ^c),
     * @param b pump curve coeffs. (H = a-bQ^c),
     * @param c pump curve coeffs. (H = a-bQ^c),
     * @return Returns true if sucessful, false otherwise.
     */
    public static boolean  getPowerCurve(double h0, double h1, double h2, double q1,
                                         double q2, double [] a, double [] b, double [] c)
    {
        double h4,h5;
        if (
                h0      < Constants.TINY ||
                        h0 - h1 < Constants.TINY ||
                        h1 - h2 < Constants.TINY ||
                        q1      < Constants.TINY ||
                        q2 - q1 < Constants.TINY
                ) return false;
        a[0] = h0;
        h4 = h0 - h1;
        h5 = h0 - h2;
        c[0] = Math.log(h5/h4)/Math.log(q2/q1);
        if (c[0] <= 0.0 || c[0] > 20.0) return false;
        b[0] = -h4/Math.pow(q1,c[0]);

        if (b[0] >= 0.0)
            return(false);

        return(true);
    }

    /**
     * Get value signal, if bigger than 0 returns 1, -1 otherwise.
     * @param val Any real number.
     * @return -1 or 1
     */
    public static double getSignal(double val) {
        return val < 0 ? -1d : 1d;
    }

    /**
     * Text messages bundle.
     */
    public static final ResourceBundle textBundle = PropertyResourceBundle.getBundle("Text");

    /**
     * Error messages bundle.
     */
    public static final ResourceBundle errorBundle = PropertyResourceBundle.getBundle("Error");

    /**
     * Get epanet text info string from bundle.
     * @param text String id.
     * @return Info string.
     */
    public static String getText(String text) {
        return textBundle.getString(text);
    }

    /**
     * Get epanet error string from bundle.
     * @param text String id.
     * @return Error string.
     */
    public static String getError(String text) {
        return errorBundle.getString(text);
    }

    /**
     * Get float from byte array.
     * @param array Byte array.
     * @param id Float start byte.
     * @return Parsed float.
     */
    public static float getFloatFromBytes(byte [] array, int id){
        return Float.intBitsToFloat((array[id] << 24) |
                ((array[id+1] & 0xff) << 16) |
                ((array[id+2] & 0xff) << 8 |
                        (array[id+3] & 0xff)));
    }
    /**
     * Get integer from byte array.
     * @param array Byte array.
     * @param id Integer start byte.
     * @return Parsed Integer.
     */
    public static int getIntegerFromBytes(byte [] array, int id){
        return (array[id] << 24) |
                ((array[id+1] & 0xff) << 16) |
                ((array[id+2] & 0xff) << 8 |
                        (array[id+3] & 0xff));
    }

    /**
     * Get file extension string.
     * @param filename
     * @return
     */
    public static String getFileExtension(String filename){
        int lastDot = filename.lastIndexOf(".");
        if(lastDot>0 && lastDot+1< filename.length()){
            return filename.substring(lastDot+1).toLowerCase();
        }

        return "";
    }
    /**
     * Check if the applicationn is running on MacOS
     * @return
     */
    public static boolean isMac(){
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf( "mac" ) >= 0);
    }


}
