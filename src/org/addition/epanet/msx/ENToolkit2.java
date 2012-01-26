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


import org.addition.epanet.hydraulic.io.HydraulicReader;
import org.addition.epanet.hydraulic.io.AwareStep;
import org.addition.epanet.network.FieldsMap;
import org.addition.epanet.network.Network;
import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.network.structures.Link;
import org.addition.epanet.network.structures.Node;
import org.addition.epanet.network.structures.Pump;
import org.addition.epanet.network.structures.Tank;
import org.addition.epanet.util.ENException;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Bridge between the hydraulic network properties and the multi-species simulation MSX class.
 */
public class ENToolkit2 {


    public static final int EN_INITVOLUME = 14;
    public static final int EN_MIXMODEL = 15;
    public static final int EN_MIXZONEVOL = 16;


    public static final int EN_DIAMETER = 0;
    public static final int EN_LENGTH = 1;
    public static final int EN_ROUGHNESS = 2;


    public static final int EN_DURATION = 0;    // Time parameters
    public static final int EN_HYDSTEP = 1;
    public static final int EN_QUALSTEP = 2;
    public static final int EN_PATTERNSTEP = 3;
    public static final int EN_PATTERNSTART = 4;
    public static final int EN_REPORTSTEP = 5;
    public static final int EN_REPORTSTART = 6;
    public static final int EN_STATISTIC = 8;
    public static final int EN_PERIODS = 9;

    public static final int EN_NODECOUNT = 0;    // Component counts
    public static final int EN_TANKCOUNT = 1;
    public static final int EN_LINKCOUNT = 2;
    public static final int EN_PATCOUNT = 3;
    public static final int EN_CURVECOUNT = 4;
    public static final int EN_CONTROLCOUNT = 5;

    public static final int EN_JUNCTION = 0;    // Node types
    public static final int EN_RESERVOIR = 1;
    public static final int EN_TANK = 2;


    private final List<Link> links;
    private final List<Node> nodes;
    private final Network net;

    private HydraulicReader dseek;

    public AwareStep getStep(int htime) {
        try {
            return (AwareStep) dseek.getStep(htime);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ENToolkit2(Network net) {
        this.net = net;

        Collection<Link> cLinks = net.getLinks();
        links = Arrays.asList(cLinks.toArray(new Link[cLinks.size()]));

        Collection<Node> cNodes = net.getNodes();
        nodes = Arrays.asList(cNodes.toArray(new Node[cNodes.size()]));
    }

    public void open(File hydFile) throws ENException, IOException {
        dseek = new HydraulicReader(new RandomAccessFile(hydFile, "r"));

    }

    public void close() throws IOException {
        dseek.close();
    }

    public String ENgetlinkid(int j) {
        return links.get(j - 1).getId();
    }

    public String ENgetnodeid(int j) {
        return nodes.get(j - 1).getId();
    }

    public int ENgetnodeindex(String s, int[] tmp) {
        Node n = net.getNode(s);
        tmp[0] = nodes.indexOf(n) + 1;

        if (tmp[0] == 0)
            return (203);
        return 0;
    }

    public int ENgetlinkindex(String s, int[] tmp) {
        Link l = net.getLink(s);
        tmp[0] = links.indexOf(l) + 1;
        if (tmp[0] == 0)
            return (204);
        return 0;
    }

    public int ENgetflowunits() {
        try {
            return net.getPropertiesMap().getFlowflag().ordinal();
        } catch (ENException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int ENgetnodetype(int i) throws Exception {
        Node n = nodes.get(i - 1);
        if (n instanceof Tank) {
            if (((Tank) n).getArea() == 0)
                return EN_RESERVOIR;
            else
                return EN_TANK;
        }

        return EN_JUNCTION;
    }

    public float ENgetlinkvalue(int index, int code) throws Exception {
        FieldsMap fMap = net.getFieldsMap();

        double v;

        if (index <= 0 || index > links.size())
            throw new ENException(204);

        Link link = links.get(index - 1);

        switch (code) {
            case EN_DIAMETER:
                if (link instanceof Pump)
                    v = 0.0;
                else
                    v = fMap.revertUnit(FieldsMap.Type.DIAM, link.getDiameter());
                break;

            case EN_LENGTH:
                v = fMap.revertUnit(FieldsMap.Type.ELEV, link.getLenght());
                break;

            case EN_ROUGHNESS:
                if (link.getType().id <= Link.LinkType.PIPE.id) {
                    if (net.getPropertiesMap().getFormflag() == PropertiesMap.FormType.DW)
                        v = fMap.revertUnit(FieldsMap.Type.ELEV, link.getRoughness() * 1000.00);
                    else
                        v = link.getRoughness();
                } else
                    v = 0.0;
                break;
            default:
                throw new ENException(251);
        }
        return ((float) v);
    }

    public int ENgetcount(int code) {
        int count = 0;
        switch (code) {
            case EN_NODECOUNT:
                count = nodes.size();
                break;
            case EN_TANKCOUNT:
                count = net.getTanks().size();
                break;
            case EN_LINKCOUNT:
                count = links.size();
                break;
            case EN_PATCOUNT:
                count = net.getPatterns().size();
                break;
            case EN_CURVECOUNT:
                count = net.getCurves().size();
                break;
            case EN_CONTROLCOUNT:
                count = net.getControls().length;
                break;
            default:
                return 0;
        }
        return (count);
    }

    public long ENgettimeparam(int code) {
        long value = 0;
        if (code < EN_DURATION || code > EN_STATISTIC)//EN_PERIODS)
            return (251);
        try {
            switch (code) {
                case EN_DURATION:
                    value = net.getPropertiesMap().getDuration();
                    break;
                case EN_HYDSTEP:
                    value = net.getPropertiesMap().getHstep();
                    break;
                case EN_QUALSTEP:
                    value = net.getPropertiesMap().getQstep();
                    break;
                case EN_PATTERNSTEP:
                    value = net.getPropertiesMap().getPstep();
                    break;
                case EN_PATTERNSTART:
                    value = net.getPropertiesMap().getPstart();
                    break;
                case EN_REPORTSTEP:
                    value = net.getPropertiesMap().getRstep();
                    break;
                case EN_REPORTSTART:
                    value = net.getPropertiesMap().getRstart();
                    break;
                case EN_STATISTIC:
                    value = net.getPropertiesMap().getTstatflag().ordinal();
                    break;
                case EN_PERIODS:
                    throw new UnsupportedOperationException();//value = dseek.getAvailableSteps().size();                 break;
            }
        } catch (ENException e) {

        }
        return (value);
    }

    public float ENgetnodevalue(int index, int code) throws Exception {
        double v = 0.0;

        FieldsMap fMap = net.getFieldsMap();

        if (index <= 0 || index > nodes.size())
            return (203);

        switch (code) {
            case EN_INITVOLUME:
                v = 0.0;
                if (nodes.get(index - 1) instanceof Tank)
                    v = fMap.revertUnit(FieldsMap.Type.VOLUME, ((Tank) nodes.get(index - 1)).getV0());
                break;

            case EN_MIXMODEL:
                v = Tank.MixType.MIX1.id;
                if (nodes.get(index - 1) instanceof Tank)
                    v = ((Tank) nodes.get(index - 1)).getMixModel().id;
                break;


            case EN_MIXZONEVOL:
                v = 0.0;
                if (nodes.get(index - 1) instanceof Tank)
                    v = fMap.revertUnit(FieldsMap.Type.VOLUME, ((Tank) nodes.get(index - 1)).getV1max());
                break;

            default:
                throw new ENException(251);
        }
        return (float) v;
    }

    public int[] ENgetlinknodes(int index) throws Exception {
        if (index < 1 || index > links.size())
            throw new ENException(204);

        Link l = links.get(index - 1);

        return new int[]{nodes.indexOf(l.getFirst()) + 1, nodes.indexOf(l.getSecond()) + 1};
    }
}
