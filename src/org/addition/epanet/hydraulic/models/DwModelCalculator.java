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

package org.addition.epanet.hydraulic.models;


import org.addition.epanet.hydraulic.structures.SimulationLink;
import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.network.structures.Link;
import org.addition.epanet.network.structures.Link.LinkType;
import org.addition.epanet.util.ENException;

/**
 * Darcy-Weishbach model calculator.
 */
public class DwModelCalculator implements PipeHeadModel {

    // Constants used for computing Darcy-Weisbach friction factor
    public static final double A1 = 0.314159265359e04;  // 1000*PI
    public static final double A2 = 0.157079632679e04;  // 500*PI
    public static final double A3 = 0.502654824574e02;  // 16*PI
    public static final double A4 = 6.283185307;        // 2*PI
    public static final double A8 = 4.61841319859;      // 5.74*(PI/4)^.9
    public static final double A9 = -8.685889638e-01;   // -2/ln(10)
    public static final double AA = -1.5634601348;      // -2*.9*2/ln(10)
    public static final double AB = 3.28895476345e-03;  // 5.74/(4000^.9)
    public static final double AC = -5.14214965799e-03; // AA*AB

//    private static double computeResistance(SimulationLink simLink, double viscos) throws ENException {
//        double q,
//                f;
//        double x1, x2, x3, x4,
//                y1, y2, y3,
//                fa, fb, r;
//        double s, w;
//
//        long i1 = System.currentTimeMillis();
//
//        if (simLink.getType().id > LinkType.PIPE.id)
//            f = 1d;
//        else {
//            Link link = simLink.getLink();
//            double roughness = link.getRoughness();
//            double diameter = link.getDiameter();
//
//            q = Math.abs(simLink.getSimFlow());
//            s = viscos * diameter;
//
//            w = q / s;
//            if (w >= A1) {
//                y1 = A8 / Math.pow(w, 0.9d);
//                y2 = roughness / (3.7 * diameter) + y1;
//                y3 = A9 * Math.log(y2);
//                f = 1.0 / (y3 * y3);
//            } else if (w > A2) {
//                y2 = roughness / (3.7 * diameter) + AB;
//                y3 = A9 * Math.log(y2);
//                fa = 1.0 / (y3 * y3);
//                fb = (2.0 + AC / (y2 * y3)) * fa;
//                r = w / A2;
//                x1 = 7.0 * fa - fb;
//                x2 = 0.128 - 17.0 * fa + 2.5 * fb;
//                x3 = -0.128 + 13.0 * fa - (fb + fb);
//                x4 = r * (0.032 - 3.0 * fa + 0.5 * fb);
//                f = x1 + r * (x2 + r * (x3 + x4));
//            } else if (w > A4) {
//                f = A3 * s / q;
//            } else {
//                f = 8.0;
//
//            }
//        }
//
//        SimulationLink.T3 += System.currentTimeMillis() - i1;
//
//        return f;
//    }

//    public LinkCoeffs compute(PropertiesMap pMap, SimulationLink sL) throws ENException {
//        // Evaluate headloss coefficients
//        double q = Math.abs(sL.getSimFlow());               // Absolute flow
//        double ml = sL.getLink().getKm();                   // Minor loss coeff.
//        double r = sL.getLink().getFlowResistance();        // Resistance coeff.
//
//        double q1,
//                resistance;
//        double x1, x2, x3, x4,
//                y1, y2, y3,
//                fa, fb, r2;
//        double s, w;
//
//
//        if (sL.getType().id > LinkType.PIPE.id)
//            resistance = 1d;
//        else {
//            Link link = sL.getLink();
//            double roughness = link.getRoughness();
//            double diameter = link.getDiameter();
//
//            q1 = Math.abs(sL.getSimFlow());
//            s = pMap.getViscos() * diameter;
//
//            w = q1 / s;
//            if (w >= A1) {
//                y1 = A8 / Math.pow(w, 0.9d);
//                y2 = roughness / (3.7 * diameter) + y1;
//                y3 = A9 * Math.log(y2);
//                resistance = 1.0 / (y3 * y3);
//            } else if (w > A2) {
//                y2 = roughness / (3.7 * diameter) + AB;
//                y3 = A9 * Math.log(y2);
//                fa = 1.0 / (y3 * y3);
//                fb = (2.0 + AC / (y2 * y3)) * fa;
//                r2 = w / A2;
//                x1 = 7.0 * fa - fb;
//                x2 = 0.128 - 17.0 * fa + 2.5 * fb;
//                x3 = -0.128 + 13.0 * fa - (fb + fb);
//                x4 = r2 * (0.032 - 3.0 * fa + 0.5 * fb);
//                resistance = x1 + r2 * (x2 + r2 * (x3 + x4));
//            } else if (w > A4) {
//                resistance = A3 * s / q1;
//            } else {
//                resistance = 8d;
//
//            }
//        }
//
//        double r1 = resistance * r + ml;
//
//        // Use large P coefficient for small flow resistance product
//        double rQtol = pMap.getRQtol();
//        if (r1 * q < rQtol) {
//            return new LinkCoeffs(1d / rQtol, sL.getSimFlow() / pMap.getHexp());
//        }
//
//        // Compute P and Y coefficients
//        double hpipe = r1 * (q * q);    // Total head loss
//        double p = 2d * r1 * q;        // |dh/dQ|
//        p = 1d / p;
//        return new LinkCoeffs(p, sL.getSimFlow() < 0 ? -hpipe * p : hpipe * p);
//    }

//    static {
//        try{
//            System.loadLibrary("DwModelCalculator");
//        }catch (Throwable t){
//            t.printStackTrace();
//        }
//    }

    public LinkCoeffs compute(PropertiesMap pMap, SimulationLink sL) throws ENException {
        double viscos = pMap.getViscos();
        double rQtol = pMap.getRQtol();
        double hexp = pMap.getHexp();

        Link link = sL.getLink();
        double simFlow = sL.getSimFlow();
        double q = Math.abs(simFlow);               // Absolute flow
        double km = link.getKm();                   // Minor loss coeff.
        double flowResistance = link.getFlowResistance();        // Resistance coeff.
        double roughness = link.getRoughness();
        double diameter = link.getDiameter();
        boolean isOne = sL.getType().id > LinkType.PIPE.id;

        LinkCoeffs linkCoeffs = innerDwCalc(viscos, rQtol, hexp, simFlow, q, km, flowResistance, roughness, diameter, isOne);
        return linkCoeffs;
    }

    private static LinkCoeffs innerDwCalc(double viscos, double rQtol, double hexp, double simFlow, double q, double km, double flowResistance, double roughness, double diameter, boolean one) {
        double
                resistance;
        double x1, x2, x3, x4,
                y1, y2, y3,
                fa, fb, r2;
        double s, w;


        if (one)
            resistance = 1d;
        else {
            s = viscos * diameter;

            w = q / s;
            if (w >= A1) {
                y1 = A8 / Math.pow(w, 0.9d);
                y2 = roughness / (3.7 * diameter) + y1;
                y3 = A9 * Math.log(y2);
                resistance = 1.0 / (y3 * y3);
            } else if (w > A2) {
                y2 = roughness / (3.7 * diameter) + AB;
                y3 = A9 * Math.log(y2);
                fa = 1.0 / (y3 * y3);
                fb = (2.0 + AC / (y2 * y3)) * fa;
                r2 = w / A2;
                x1 = 7.0 * fa - fb;
                x2 = 0.128 - 17.0 * fa + 2.5 * fb;
                x3 = -0.128 + 13.0 * fa - (fb + fb);
                x4 = r2 * (0.032 - 3.0 * fa + 0.5 * fb);
                resistance = x1 + r2 * (x2 + r2 * (x3 + x4));
            } else if (w > A4) {
                resistance = A3 * s / q;
            } else {
                resistance = 8d;

            }
        }

        double r1 = resistance * flowResistance + km;
        LinkCoeffs linkCoeffs;
        // Use large P coefficient for small flow resistance product
        if (r1 * q < rQtol) {

            linkCoeffs = new LinkCoeffs(1d / rQtol, simFlow / hexp);
        } else {
            // Compute P and Y coefficients
            double hpipe = r1 * (q * q);    // Total head loss
            double p = 2d * r1 * q;        // |dh/dQ|
            p = 1d / p;
            linkCoeffs = new LinkCoeffs(p, simFlow < 0 ? -hpipe * p : hpipe * p);
        }
        return linkCoeffs;
    }

//    static native LinkCoeffs innerDwCalcNative(double viscos, double rQtol, double hexp, double simFlow, double q, double km, double flowResistance, double roughness, double diameter, boolean one);


}
