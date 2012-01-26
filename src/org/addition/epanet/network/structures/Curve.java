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

package org.addition.epanet.network.structures;

import org.addition.epanet.util.ENException;
import org.addition.epanet.network.FieldsMap;
import org.addition.epanet.util.DblList;
import org.addition.epanet.network.FieldsMap.*;
import java.util.List;

/**
 * 2D graph used to map volume, pump, efficiency and head loss curves.
 */
public class Curve {

    /**
     * Computed curve coefficients.
     */
    public static class Coeffs{
        public double h0;       // head at zero flow (y-intercept)

        public double r;        // dHead/dFlow (slope)
        public Coeffs(double h0, double r) {
            this.h0 = h0;
            this.r = r;
        }
    }

    // Type of curve
    static public enum CurveType{
        E_CURVE     (2),    // volume curve
        H_CURVE     (3),    // pump curve
        P_CURVE     (1),    // efficiency curve
        V_CURVE     (0);    // head loss curve

        public final int id;

        private CurveType(int val){id = val;}
    }
    /**
     * Curve name.
     */
    private String      id;
    /**
     * Curve type.
     */
    private CurveType   type;
    /**
     * Curve abscissa values.
     */
    private DblList     x;


    /**
     * Curve ordinate values.
     */
    private DblList     y;

    public Curve() {
        this.id = "";
        x = new DblList();
        y = new DblList();
    }

    /**
     * Computes intercept and slope of head v. flow curve at current flow.
     * @param q Flow value.
     * @return
     */
    public Coeffs getCoeff(FieldsMap fMap,double q) throws ENException {
        double h0;
        double r;
        int k1, k2, npts;

        q *= fMap.getUnits(Type.FLOW);

        npts = getNpts();

        k2 = 0;
        while (k2 < npts && x.get(k2) < q) k2++;
        if (k2 == 0) k2++;
        else if (k2 == npts) k2--;
        k1 = k2 - 1;

        r = (y.get(k2) - y.get(k1)) / (x.get(k2) - x.get(k1));
        h0 = y.get(k1) - (r) * x.get(k1);

        h0 = (h0) / fMap.getUnits(Type.HEAD);
        r = (r) * fMap.getUnits(Type.FLOW) / fMap.getUnits(Type.HEAD);

        return new Coeffs(h0, r);
    }

    public String getId() {
        return id;
    }

    /**
     * Get the number of points.
     * @return If the abscissa points count differ from the ordinate it returns -1, otherwise,
     * it returns the abscissa point count.
     */
    public int getNpts() {
        if(x.size()!=y.size()){
            return -1;
        }
        return x.size();
    }

    public CurveType getType() {
        return type;
    }

    public List<Double> getX(){
        return x;
    }

    public List<Double> getY(){
        return y;
    }

    public void setId(String Id) {
        this.id = Id;
    }

    public void setType(CurveType Type) {
        this.type = Type;
    }
}
