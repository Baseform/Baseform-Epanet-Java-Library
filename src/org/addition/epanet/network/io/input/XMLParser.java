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

package org.addition.epanet.network.io.input;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.addition.epanet.util.ENException;
import org.addition.epanet.network.FieldsMap;
import org.addition.epanet.network.Network;
import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.network.structures.*;
import org.addition.epanet.util.DblList;

import java.io.*;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

public class XMLParser extends InputParser {
    public static final XStream X_STREAM = new XStream();

    public static class DoubleListConverter implements Converter {

        public boolean canConvert(Class clazz) {
            return clazz.equals(DblList.class);
        }

        public void marshal(Object value, HierarchicalStreamWriter writer,
                            MarshallingContext context) {
            DblList l = (DblList) value;
            writer.startNode("doubles");

            StringBuilder b = new StringBuilder();
            for (int i = 0, lSize = l.size(); i < lSize; i++) {
                Double aDouble = l.get(i);
                b.append(aDouble).append(",");

            }
            if (b.length() > 0)
                writer.setValue(b.substring(0, b.length() - 1));
            writer.endNode();
        }

        public Object unmarshal(HierarchicalStreamReader reader,
                                UnmarshallingContext context) {
            DblList l = new DblList();
            reader.moveDown();//doubles
            String value = reader.getValue();
            if (value.length() > 0) {
                String[] split = value.split(",");
                for (String s : split) {
                    l.add(Double.parseDouble(s));
                }
            }
            reader.moveUp();//doubles
            return l;
        }

    }

    static {
        X_STREAM.setMode(XStream.ID_REFERENCES);
        X_STREAM.registerConverter(new DoubleListConverter());
        X_STREAM.alias("Node", Node.class);
        X_STREAM.alias("Link", Link.class);
        X_STREAM.alias("Pattern", Pattern.class);
        X_STREAM.alias("Curve", Curve.class);

        X_STREAM.alias("Tank", Tank.class);
        X_STREAM.alias("Control", Control.class);
        X_STREAM.alias("Demand", Demand.class);
        X_STREAM.alias("Field", Field.class);
        X_STREAM.alias("Label", Label.class);
        X_STREAM.alias("Point", Point.class);
        X_STREAM.alias("Pump", Pump.class);
        X_STREAM.alias("Point", Point.class);
        X_STREAM.alias("Rule", Rule.class);
        X_STREAM.alias("Source", Source.class);
        X_STREAM.alias("Valve", Valve.class);
        X_STREAM.alias("Network", Network.class);
        X_STREAM.alias("FieldsMap", FieldsMap.class);
        X_STREAM.alias("FieldsMapType", FieldsMap.Type.class);
        X_STREAM.alias("PropertiesMap", PropertiesMap.class);

        X_STREAM.alias("FlowUnitsType", PropertiesMap.FlowUnitsType.class);
        X_STREAM.alias("FormType", PropertiesMap.FormType.class);
        X_STREAM.alias("Hydtype", PropertiesMap.Hydtype.class);
        X_STREAM.alias("PressUnitsType", PropertiesMap.PressUnitsType.class);
        X_STREAM.alias("QualType", PropertiesMap.QualType.class);
        X_STREAM.alias("ReportFlag", PropertiesMap.ReportFlag.class);
        X_STREAM.alias("StatFlag", PropertiesMap.StatFlag.class);
        X_STREAM.alias("TstatType", PropertiesMap.TstatType.class);
        X_STREAM.alias("UnitsType", PropertiesMap.UnitsType.class);


    }

    private boolean gzipped;

    public XMLParser(Logger log, boolean gzipped) {
        super(log);
        this.gzipped = gzipped;
    }

    @Override
    public Network parse(Network net, File f) throws ENException {
        try {
            InputStream is = !gzipped ? new FileInputStream(f) : new GZIPInputStream(new FileInputStream(f));
            InputStreamReader r = new InputStreamReader(is, "UTF-8");
            return (Network) X_STREAM.fromXML(r);
        } catch (IOException e) {
            throw new ENException(302);
        }
    }
}
