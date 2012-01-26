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


import org.addition.epanet.Constants;
import org.addition.epanet.network.PropertiesMap;

public class NUConvert {
    public static double convertArea(PropertiesMap.UnitsType type,double value){
        if(type == PropertiesMap.UnitsType.SI)
            return value / (Constants.MperFT * Constants.MperFT);

        return value;
    }

    public static double convertDistance(PropertiesMap.UnitsType type,double value){
        if(type == PropertiesMap.UnitsType.SI)
            return value / Constants.MperFT;

        return value;
    }

    public static double convertFlow(PropertiesMap.FlowUnitsType flow,double value){
        switch (flow) {
            case CFS:
                return value / Constants.LPSperCFS;
            case GPM:
                return value / Constants.GPMperCFS;
            case MGD:
                return value / Constants.MGDperCFS;
            case IMGD:
                return value / Constants.IMGDperCFS;
            case AFD:
                return value / Constants.AFDperCFS;
            case LPS:
                return value / Constants.LPSperCFS;
            case LPM:
                return value / Constants.LPMperCFS;
            case MLD:
                return value / Constants.MLDperCFS;
            case CMH:
                return value / Constants.CMHperCFS;
            case CMD:
                return value / Constants.CMHperCFS;
        }
        return value;
    }

    public static double convertPower(PropertiesMap.UnitsType type,double value){
        if(type == PropertiesMap.UnitsType.SI)
            return value / Constants.KWperHP;

        return value;
    }

    public static double convertPressure(PropertiesMap.PressUnitsType type, double SpGrav,double value){
        switch (type) {
            case PSI:
                return value;
            case KPA:
                return value / (Constants.KPAperPSI*Constants.PSIperFT*SpGrav);
            case METERS:
                return value / (Constants.MperFT*SpGrav);
        }
        return value;
    }

    public static double convertVolume(PropertiesMap.UnitsType type,double value){
        if(type == PropertiesMap.UnitsType.SI)
            return value / (Constants.M3perFT3);

        return value;
    }

    public static double revertArea(PropertiesMap.UnitsType type,double value){
        if(type == PropertiesMap.UnitsType.SI)
            return value * (Constants.MperFT * Constants.MperFT);

        return value;
    }

    public static double revertDistance(PropertiesMap.UnitsType type,double value){
        if(type == PropertiesMap.UnitsType.SI)
            return value * Constants.MperFT;

        return value;
    }

    public static double revertDiameter(PropertiesMap.UnitsType type,double value)
    {
        if(type == PropertiesMap.UnitsType.SI)
            return value * Constants.MMperFT;
        return value * Constants.INperFT;
    }

    public static double revertFlow( PropertiesMap.FlowUnitsType flow,double value){
        switch (flow) {
            case CFS:
                return value * Constants.LPSperCFS;
            case GPM:
                return value * Constants.GPMperCFS;
            case MGD:
                return value * Constants.MGDperCFS;
            case IMGD:
                return value * Constants.IMGDperCFS;
            case AFD:
                return value * Constants.AFDperCFS;
            case LPS:
                return value * Constants.LPSperCFS;
            case LPM:
                return value * Constants.LPMperCFS;
            case MLD:
                return value * Constants.MLDperCFS;
            case CMH:
                return value * Constants.CMHperCFS;
            case CMD:
                return value * Constants.CMHperCFS;
        }
        return value;
    }

    public static double revertPower(PropertiesMap.UnitsType type,double value){
        if(type == PropertiesMap.UnitsType.SI)
            return value * Constants.KWperHP;

        return value;
    }

    public static double revertPressure(PropertiesMap.PressUnitsType type,double SpGrav,double value){
         switch (type) {
            case PSI:
                return value;
            case KPA:
                return value * (Constants.KPAperPSI*Constants.PSIperFT*SpGrav);
            case METERS:
                return value * (Constants.MperFT*SpGrav);
        }
        return value;
    }

    public static double revertVolume(PropertiesMap.UnitsType type,double value){
        if(type == PropertiesMap.UnitsType.SI)
            return value * (Constants.M3perFT3);

        return value;
    }


}
