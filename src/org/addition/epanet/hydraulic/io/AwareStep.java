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

package org.addition.epanet.hydraulic.io;


import org.addition.epanet.Constants;
import org.addition.epanet.hydraulic.HydraulicSim;
import org.addition.epanet.hydraulic.structures.SimulationLink;
import org.addition.epanet.hydraulic.structures.SimulationNode;
import org.addition.epanet.network.FieldsMap;
import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.network.structures.Field;
import org.addition.epanet.network.structures.Link;
import org.addition.epanet.network.structures.Node;
import org.addition.epanet.network.structures.Pump;
import org.addition.epanet.quality.QualitySim;
import org.addition.epanet.quality.structures.QualityLink;
import org.addition.epanet.quality.structures.QualityNode;
import org.addition.epanet.util.ENException;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.List;

/**
 * Aware compatible hydraulic step snapshot
 */
public class AwareStep {
    private double[] QN;
    private double[] QL;
    private double[] D;
    private double[] H;
    private double[] Q;
    private double[] DH;
    private long hydTime;
    private long hydStep;


    public static final int FORMAT_VERSION = 1;

    public static class HeaderInfo {
        public int version;
        public int nodes;
        public int links;
        public long rstart;
        public long rstep;
        public long duration;
    }

    public static void writeHeader(DataOutput outStream, HydraulicSim hydraulicSim, long rstart, long rstep, long duration) throws IOException, ENException {
        outStream.writeInt(FORMAT_VERSION);
        outStream.writeInt(hydraulicSim.getnNodes().size());
        outStream.writeInt(hydraulicSim.getnLinks().size());
        outStream.writeLong(rstart);
        outStream.writeLong(rstep);
        outStream.writeLong(duration);
    }

    public static HeaderInfo readHeader(DataInput in) throws IOException, ENException {
        HeaderInfo headerInfo = new HeaderInfo();
        headerInfo.version = in.readInt();
        headerInfo.nodes = in.readInt();
        headerInfo.links = in.readInt();
        headerInfo.rstart = in.readLong();
        headerInfo.rstep = in.readLong();
        headerInfo.duration = in.readLong();
        return headerInfo;
    }


    public static void write(DataOutput outStream, HydraulicSim hydraulicSim, long hydStep) throws IOException, ENException {

        List<SimulationNode> nodes = hydraulicSim.getnNodes();
        List<SimulationLink> links = hydraulicSim.getnLinks();
        long hydTime = hydraulicSim.getHtime();

        int nNodes = nodes.size();
        int nLinks = links.size();

        int baSize = (nNodes * 3 + nLinks * 3) * Double.SIZE / 8 +
                Long.SIZE * 2 / 8;
        ByteBuffer buf = ByteBuffer.allocate(baSize);


        for (SimulationNode node : nodes) {
            buf.putDouble(node.getSimDemand());
            buf.putDouble(node.getSimHead());
            buf.putDouble(0.0);
        }

        for (SimulationLink link : links) {
            buf.putDouble(link.getSimStatus().id <= Link.StatType.CLOSED.id ? 0d : link.getSimFlow());
            buf.putDouble((link.getFirst().getSimHead() - link.getSecond().getSimHead()));
            buf.putDouble(0.0);
        }

        buf.putLong(hydStep);
        buf.putLong(hydTime);

        buf.flip();
        outStream.write(buf.array());
    }

    public static void writeHydAndQual(DataOutput outStream, HydraulicSim hydraulicSim, QualitySim qualitySim, long step, long time) throws IOException, ENException {
        List<QualityNode> qNodes = qualitySim != null ? qualitySim.getnNodes() : null;
        List<QualityLink> qLinks = qualitySim != null ? qualitySim.getnLinks() : null;
        List<SimulationNode> nodes = hydraulicSim.getnNodes();
        List<SimulationLink> links = hydraulicSim.getnLinks();

        int nNodes = nodes.size();
        int nLinks = links.size();

        int baSize = (nNodes * 3 + nLinks * 3) * Double.SIZE / 8 + Long.SIZE * 2 / 8;
        ByteBuffer buf = ByteBuffer.allocate(baSize);

        int count = 0;
        for (SimulationNode node : nodes) {
            buf.putDouble(node.getSimDemand());
            buf.putDouble(node.getSimHead());
            buf.putDouble(qualitySim != null ? qNodes.get(count++).getQuality() : 0.0);
        }

        count = 0;
        for (SimulationLink link : links) {
            buf.putDouble(link.getSimStatus().id <= Link.StatType.CLOSED.id ? 0d : link.getSimFlow());
            buf.putDouble((link.getFirst().getSimHead() - link.getSecond().getSimHead()));
            buf.putDouble(qualitySim != null ? qLinks.get(count++).getAverageQuality(null) : 0);
        }

        buf.putLong(step);
        buf.putLong(time);

        buf.flip();
        outStream.write(buf.array());
    }

     /*public static void writeHybrid(DataOutput outStream,HydraulicSim hydraulicSim, double [] qN, double [] qL , long step, long time) throws IOException, ENException {

        List<SimulationNode> nodes = hydraulicSim.getnNodes();
        List<SimulationLink> links = hydraulicSim.getnLinks();

        int nNodes = nodes.size();
        int nLinks = links.size();

        int baSize = (nNodes * 3 + nLinks * 3) * Double.SIZE / 8 +
                Long.SIZE * 2 / 8;
        ByteBuffer buf = ByteBuffer.allocate(baSize);

        int count= 0;
        for (SimulationNode node : nodes) {
            buf.putDouble(node.getSimDemand());
            buf.putDouble(node.getSimHead());
            buf.putDouble(qN[count++]);
        }

        count = 0;
        for (SimulationLink link : links) {
            buf.putDouble(link.getSimStatus().id <= Link.StatType.CLOSED.id ? 0d : link.getSimFlow());
            buf.putDouble((link.getFirst().getSimHead() - link.getSecond().getSimHead()));
            buf.putDouble(qL[count++]);
        }

        buf.putLong(step);
        buf.putLong(time);

        buf.flip();
        outStream.write(buf.array());
    } */

    public AwareStep(DataInput inStream, HeaderInfo headerInfo) throws IOException {
        int nNodes = headerInfo.nodes;
        int nLinks = headerInfo.links;

        D = new double[nNodes];
        H = new double[nNodes];
        Q = new double[nLinks];
        DH = new double[nLinks];

        QN = new double[nNodes];
        QL = new double[nLinks];

        int baSize = (nNodes * 3 + nLinks * 3) * Double.SIZE / 8 +
                Long.SIZE * 2 / 8;
        byte[] ba = new byte[baSize];
        inStream.readFully(ba);
        ByteBuffer buf = ByteBuffer.wrap(ba);

        for (int i = 0; i < nNodes; i++) {
            D[i] = buf.getDouble();
            H[i] = buf.getDouble();
            QN[i] = buf.getDouble();
        }

        for (int i = 0; i < nLinks; i++) {
            Q[i] = buf.getDouble();
            DH[i] = buf.getDouble();
            QL[i] = buf.getDouble();
        }

        hydStep = buf.getLong();
        hydTime = buf.getLong();
    }


    public double getNodeDemand(int id, Node node, FieldsMap fMap) {
        try {
            return fMap != null ? fMap.revertUnit(FieldsMap.Type.DEMAND, D[id]) : D[id];
        } catch (ENException e) {
            return 0;
        }
    }

    public double getNodeHead(int id, Node node, FieldsMap fMap) {
        try {
            return fMap != null ? fMap.revertUnit(FieldsMap.Type.HEAD, H[id]) : H[id];
        } catch (ENException e) {
            return 0;
        }
    }

    public double getNodePressure(int id, Node node, FieldsMap fMap) {
        try {
            double P = (getNodeHead(id, node, null) - node.getElevation());

            return fMap != null ? fMap.revertUnit(FieldsMap.Type.PRESSURE, P) : P;
        } catch (ENException e) {
            return 0;
        }
    }

    public double getLinkFlow(int id, Link link, FieldsMap fMap) {
        try {
            return fMap != null ? fMap.revertUnit(FieldsMap.Type.FLOW, Q[id]) : Q[id];
        } catch (ENException e) {
            return 0;
        }
    }


    public double getLinkVelocity(int id, Link link, FieldsMap fMap) {
        try {
            double V;
            double flow = getLinkFlow(id, link, null);
            if (link instanceof Pump)
                V = 0;
            else
                V = (Math.abs(flow) / (Constants.PI * Math.pow(link.getDiameter(), 2) / 4.0));

            return fMap != null ? fMap.revertUnit(FieldsMap.Type.VELOCITY, V) : V;
        } catch (ENException e) {
            return 0;
        }
    }

    public double getLinkHeadLoss(int id, Link link, FieldsMap fMap) {
        try {
            if (getLinkFlow(id, link, null) == 0) {
                return 0.0;
            } else {
                double h = DH[id];
                if (!(link instanceof Pump))
                    h = Math.abs(h);

                if (link.getType().id <= Link.LinkType.PIPE.id)
                    return (1000 * h / link.getLenght());
                else
                    return fMap != null ? fMap.revertUnit(FieldsMap.Type.HEADLOSS, h) : h;
            }
        } catch (ENException e) {
            return 0;
        }
    }


    public double getLinkFriction(int id, Link link, FieldsMap fMap) {
        try {
            double F;

            double flow = getLinkFlow(id, link, null);
            if (link.getType().id <= Link.LinkType.PIPE.id && Math.abs(flow) > Constants.TINY) {


                double h = Math.abs(DH[id]);
                F = 39.725 * h * Math.pow(link.getDiameter(), 5) / link.getLenght() /
                        (flow * flow);
            } else
                F = 0;

            return fMap != null ? fMap.revertUnit(FieldsMap.Type.FRICTION, F) : F;
        } catch (ENException e) {
            return 0;
        }
    }

    public double getLinkAvrQuality(int id) {
        return QL[id];
    }

    public double getNodeQuality(int id) {
        return QN[id];
    }

    public long getStep() {
        return hydStep;
    }

    public long getTime() {
        return hydTime;
    }


}
