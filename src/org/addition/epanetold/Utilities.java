package org.addition.epanetold;

import org.addition.epanetold.Types.EnumVariables;

import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;


public strictfp class Utilities {

    private static boolean mConsoleMsg = false;
    private static final ResourceBundle errorBundle = PropertyResourceBundle.getBundle("Error");

    static void setConsoleMsg(boolean mConsoleMsg) {
        Utilities.mConsoleMsg = mConsoleMsg;
    }

    public static double interp(int n, List<Double> x, List<Double> y, double xx)
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

    public static int roundOff(double x)
    {
        return x>= 0 ? (int)(x+.5):(int)(x-0.5);
    }

    public static int pumpIndex(Network net, int id)
    {
        return roundOff(net.getLink(id).getDiameter());
    }


    public static int  getnodetype(Network net,int i)
    {
        int junctions = net.getSections(EnumVariables.SectType._JUNCTIONS);
        if (i <= junctions) return(0);
        if (net.getTank(i-junctions).getArea() == 0.0) return(1);
        return(2);
    }

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

    public static int  findMatch(String line, String [] keyword)
    {
        int count = 0;
        for (String k: keyword)
        {
            if(match(line,k))
                return count;
            count++;

        }
        return -1;
    }


    public static double  hour(String time, String units)
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

            if (Utilities.match(units,EnumVariables.w_SECONDS)) return(y[0]/3600.0);
            if (Utilities.match(units,EnumVariables.w_MINUTES)) return(y[0]/60.0);
            if (Utilities.match(units,EnumVariables.w_HOURS))   return(y[0]);
            if (Utilities.match(units,EnumVariables.w_DAYS))    return(y[0]*24.0);
        }

        if (n > 1) y[0] = y[0] + y[1]/60.0 + y[2]/3600.0;

        if (units.length() == '\0')
            return(y[0]);

        if (units.equalsIgnoreCase(EnumVariables.w_AM))
        {
            if (y[0] >= 13.0) return(-1.0);
            if (y[0] >= 12.0) return(y[0]-12.0);
            else return(y[0]);
        }
        if (units.equalsIgnoreCase(EnumVariables.w_PM))
        {
            if (y[0] >= 13.0) return(-1.0);
            if (y[0] >= 12.0) return(y[0]);
            else return(y[0]+12.0);
        }
        return(-1.0);
    }
    


    public static Double getFloat(String str){
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

    public static String clocktime(long seconds){
        long h,m,s;
        h = seconds/3600;
        m = (seconds % 3600) / 60;
        s = seconds - 3600*h - 60*m;
        return String.format("%02d:%02d:%02d",h,m,s);
    }

    public static void writecon(String s)
    {
        if(mConsoleMsg)
            System.out.print(s);
    }


    public static String geterrmsg(int errcode){
        return errorBundle.getString("ERR" + Integer.toString(errcode));
    }

    public static int ERRCODE(int errcode ,int x){
        return ((errcode>100) ? (errcode) : (x));

    }


}
