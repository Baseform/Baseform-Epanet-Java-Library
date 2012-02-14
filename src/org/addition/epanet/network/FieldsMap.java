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

package org.addition.epanet.network;


import org.addition.epanet.Constants;
import org.addition.epanet.util.ENException;
import org.addition.epanet.network.io.Keywords;
import org.addition.epanet.network.structures.Field;
import org.addition.epanet.util.Utilities;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * Units report properties & conversion support class
 */
public class FieldsMap {

    /**
     * Network variables
     */
    static public enum Type {
        ELEV        (0,Keywords.t_ELEV     ),   // nodal water quality
        DEMAND      (1,Keywords.t_DEMAND   ),   // nodal demand flow
        HEAD        (2,Keywords.t_HEAD     ),   // link flow velocity
        PRESSURE    (3,Keywords.t_PRESSURE ),   // avg. reaction rate in link
        QUALITY     (4,Keywords.t_QUALITY  ),   // link friction factor
        LENGTH      (5,Keywords.t_LENGTH   ),   // avg. water quality in link
        DIAM        (6,Keywords.t_DIAM     ),   // nodal hydraulic head
        FLOW        (7,Keywords.t_FLOW     ),   // link diameter

        VELOCITY    (8,Keywords.t_VELOCITY ),   // time to fill a tank
        HEADLOSS    (9,Keywords.t_HEADLOSS ),   // link head loss
        LINKQUAL    (10,Keywords.t_LINKQUAL ),   // link status

        STATUS      (11,Keywords.t_STATUS   ),   // tank volume
        SETTING     (12,Keywords.t_SETTING  ),   // simulation time
        REACTRATE   (13,Keywords.t_REACTRATE),   // pump power output
        FRICTION    (14,Keywords.t_FRICTION ),   // link flow rate
        POWER       (15),   // pump/valve setting
        TIME        (16),   // simulation time of day
        VOLUME      (17),   // time to drain a tank
        CLOCKTIME   (18),   // nodal elevation
        FILLTIME    (19),   // link length
        DRAINTIME   (20);   // nodal pressure

        /**
         * Get field type from string.
         * @param text String to be parsed.
         * @return Parsed Field, null the string doesn't match any of the possible types.
         */
        public static Type parse(String text){
            for (Type type : Type.values())
                if (Utilities.match(text, type.parseStr)) return type;
            return null;
        }

        /**
         * Field sequencial id.
         */
        public final int id;

        /**
         * Field string.
         */
        public final String parseStr;
        private Type(int id){this.id = id;this.parseStr = "";}

        private Type(int id, String pStr){this.id = id;this.parseStr = pStr;}
    }

    /**
     * Report fields properties.
     */
    private Map<Type,Field> fields;

    /**
     * Fields units values.
     */
    private Map<Type,Double> units;

    /**
     * Init fields default configuration
     */
    public FieldsMap()
    {
        try{
            fields = new LinkedHashMap<Type,Field>();
            units = new LinkedHashMap<Type,Double>();

            for(Type type: Type.values())
                setField(type,new Field(type.parseStr));

            getField(Type.FRICTION).setPrecision(3);

            for (int i= Type.DEMAND.id;i<= Type.QUALITY.id; i++)
                getField(Type.values()[i]).setEnabled(true);

            for (int i= Type.FLOW.id;i<= Type.HEADLOSS.id; i++)
                getField(Type.values()[i]).setEnabled(true);
        }
        catch (ENException e){
            e.printStackTrace();
        }
    }

    /**
     * Get report field properties from type.
     * @param type Field type.
     * @return Report field.
     * @throws org.addition.epanet.util.ENException If specified type not found.
     */
    public Field getField(Type type) throws ENException {
        Object obj = fields.get(type);
        if(obj==null)
            throw new ENException(201,type.parseStr);
        else
            return (Field)obj;
    }

    /**
     * Get conversion value from field type.
     * @param type Field type.
     * @return Conversion units value (from user units to system units)
     * @throws ENException If specified type not found.
     */
    public Double getUnits(Type type) throws ENException {
        Object obj = units.get(type);
        if(obj==null)
            throw new ENException(201,type.parseStr);
        else
            return (Double)obj;
    }

    /**
     * Update fields and units, after loading the INP.
     * @param targetUnits
     * @param flowFlag
     * @param pressFlag
     * @param qualFlag
     * @param ChemUnits
     * @param SpGrav
     * @param Hstep
     */
    public void prepare(PropertiesMap.UnitsType targetUnits,
                        PropertiesMap.FlowUnitsType flowFlag,
                        PropertiesMap.PressUnitsType pressFlag,
                        PropertiesMap.QualType qualFlag,
                        String ChemUnits,
                        Double SpGrav,
                        Long Hstep) throws ENException
    {
        double  dcf,
                ccf,
                qcf,
                hcf,
                pcf,
                wcf;

        if (targetUnits == PropertiesMap.UnitsType.SI)
        {
            getField(Type.DEMAND).setUnits(flowFlag.parseStr);
            getField(Type.ELEV).setUnits(Keywords.u_METERS);
            getField(Type.HEAD).setUnits(Keywords.u_METERS);

            if (pressFlag == PropertiesMap.PressUnitsType.METERS)
                getField(Type.PRESSURE).setUnits(Keywords.u_METERS);
            else
                getField(Type.PRESSURE).setUnits(Keywords.u_KPA);

            getField(Type.LENGTH).setUnits(Keywords.u_METERS);
            getField(Type.DIAM).setUnits(Keywords.u_MMETERS);
            getField(Type.FLOW).setUnits(flowFlag.parseStr);
            getField(Type.VELOCITY).setUnits(Keywords.u_MperSEC);
            getField(Type.HEADLOSS).setUnits("m"+Keywords.u_per1000M);
            getField(Type.FRICTION).setUnits("");
            getField(Type.POWER).setUnits(Keywords.u_KW);

            dcf = 1000.0* Constants.MperFT;
            qcf = Constants.LPSperCFS;
            if (flowFlag == PropertiesMap.FlowUnitsType.LPM) qcf = Constants.LPMperCFS;
            if (flowFlag == PropertiesMap.FlowUnitsType.MLD) qcf = Constants.MLDperCFS;
            if (flowFlag == PropertiesMap.FlowUnitsType.CMH) qcf = Constants.CMHperCFS;
            if (flowFlag == PropertiesMap.FlowUnitsType.CMD) qcf = Constants.CMDperCFS;
            hcf = Constants.MperFT;
            if (pressFlag == PropertiesMap.PressUnitsType.METERS) pcf = Constants.MperFT*SpGrav;
            else pcf = Constants.KPAperPSI*Constants.PSIperFT*SpGrav;
            wcf = Constants.KWperHP;
        }
        else
        {
            getField(Type.DEMAND).setUnits(flowFlag.parseStr);
            getField(Type.ELEV).setUnits(Keywords.u_FEET);
            getField(Type.HEAD).setUnits(Keywords.u_FEET);

            getField(Type.PRESSURE).setUnits(Keywords.u_PSI);
            getField(Type.LENGTH).setUnits(Keywords.u_FEET);
            getField(Type.DIAM).setUnits(Keywords.u_INCHES);
            getField(Type.FLOW).setUnits(flowFlag.parseStr);
            getField(Type.VELOCITY).setUnits(Keywords.u_FTperSEC);
            getField(Type.HEADLOSS).setUnits("ft"+Keywords.u_per1000FT);
            getField(Type.FRICTION).setUnits("");
            getField(Type.POWER).setUnits(Keywords.u_HP);


            dcf = 12.0;
            qcf = 1.0;
            if (flowFlag == PropertiesMap.FlowUnitsType.GPM) qcf = Constants.GPMperCFS;
            if (flowFlag == PropertiesMap.FlowUnitsType.MGD) qcf = Constants.MGDperCFS;
            if (flowFlag == PropertiesMap.FlowUnitsType.IMGD)qcf = Constants.IMGDperCFS;
            if (flowFlag == PropertiesMap.FlowUnitsType.AFD) qcf = Constants.AFDperCFS;
            hcf = 1.0;
            pcf = Constants.PSIperFT*SpGrav;
            wcf = 1.0;
        }
        getField(Type.QUALITY).setUnits("");
        ccf = 1.0;
        if (qualFlag == PropertiesMap.QualType.CHEM)
        {
            ccf = 1.0/Constants.LperFT3;
            getField(Type.QUALITY).setUnits(ChemUnits);
            getField(Type.REACTRATE).setUnits(ChemUnits + Keywords.t_PERDAY);
        }
        else if (qualFlag == PropertiesMap.QualType.AGE)
            getField(Type.QUALITY).setUnits(Keywords.u_HOURS);
        else if (qualFlag == PropertiesMap.QualType.TRACE)
            getField(Type.QUALITY).setUnits(Keywords.u_PERCENT);

        setUnits(Type.DEMAND,qcf);
        setUnits(Type.ELEV,hcf);
        setUnits(Type.HEAD,hcf);
        setUnits(Type.PRESSURE,pcf);
        setUnits(Type.QUALITY,ccf);
        setUnits(Type.LENGTH,hcf);
        setUnits(Type.DIAM,dcf);
        setUnits(Type.FLOW,qcf);
        setUnits(Type.VELOCITY,hcf);
        setUnits(Type.HEADLOSS,hcf);
        setUnits(Type.LINKQUAL,ccf);
        setUnits(Type.REACTRATE,ccf);
        setUnits(Type.FRICTION,1.0);
        setUnits(Type.POWER,wcf);
        setUnits(Type.VOLUME,hcf*hcf*hcf);

        if (Hstep < 1800)
        {
            setUnits(Type.TIME,1.0/60.0);
            getField(Type.TIME).setUnits(Keywords.u_MINUTES);
        }
        else
        {
            setUnits(Type.TIME,1.0/3600.0);
            getField(Type.TIME).setUnits(Keywords.u_HOURS);
        }
    }

    /**
     * Revert system units to user units.
     * @param type Field type.
     * @param value Value to be converted.
     * @return Value in user units.
     * @throws ENException
     */
    public double revertUnit(Type type,double value) throws ENException {
        return type!=null?value*getUnits(type):value;
    }

    public double convertUnitToSystem(Type type,double value) throws ENException {
        return value/getUnits(type);
    }

    /**
     * Set field properties.
     * @param type Field type.
     * @param value Report field reference.
     */
    private void setField(Type type,Field value) {
        fields.put(type,value);
    }

    /**
     * Set conversion value from field type.
     * @param type Field type.
     * @param value Field value.
     */
    private void setUnits(Type type,Double value) {
        units.put(type,value);
    }

}
