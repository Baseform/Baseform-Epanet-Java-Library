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

package org.addition.epanet.network.io.output;

import org.addition.epanet.Constants;
import org.addition.epanet.network.io.Keywords;
import org.addition.epanet.util.ENException;
import org.addition.epanet.util.Utilities;
import org.addition.epanet.network.FieldsMap;
import org.addition.epanet.network.FieldsMap.*;
import org.addition.epanet.network.Network;
import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.network.structures.*;
import org.addition.epanet.network.structures.Control.*;
import org.addition.epanet.network.structures.Link.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * INP file composer.
 */
public class InpComposer extends OutputComposer{

    private static final String JUNCS_SUBTITLE      = ";ID\tElev\tDemand\tPattern";
    private static final String RESERVOIRS_SUBTITLE = ";ID\tHead\tPattern";
    private static final String TANK_SUBTITLE       = ";ID\tElevation\tInitLevel\tMinLevel\tMaxLevel\tDiameter\tMinVol\tVolCurve";
    private static final String PUMPS_SUBTITLE      = ";ID\tNode1\tNode2\tParameters";
    private static final String VALVES_SUBTITLE     = ";ID\tNode1\tNode2\tDiameter\tType\tSetting\tMinorLoss";
    private static final String DEMANDS_SUBTITLE    = ";Junction\tDemand\tPattern\tCategory";
    private static final String STATUS_SUBTITLE     = ";ID\tStatus/Setting";
    private static final String PIPES_SUBTITLE      = ";ID\tNode1\tNode2\tLength\tDiameter\tRoughness\tMinorLoss\tStatus";
    private static final String PATTERNS_SUBTITLE   = ";ID\tMultipliers";
    private static final String EMITTERS_SUBTITLE   = ";Junction\tCoefficient";
    private static final String CURVE_SUBTITLE      = ";ID\tX-Value\tY-Value";
    private static final String QUALITY_SUBTITLE    = ";Node\tInitQual";
    private static final String SOURCE_SUBTITLE     = ";Node\tType\tQuality\tPattern";
    private static final String MIXING_SUBTITLE     = ";Tank\tModel";
    private static final String REACTIONS_SUBTITLE  = ";Type\tPipe/Tank";
    private static final String COORDINATES_SUBTITLE = ";Node\tX-Coord\tY-Coord";

    BufferedWriter buffer;

    public InpComposer() {
    }

    @Override
    public void composer(Network net, File f) throws ENException {
        try{
            buffer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f),"ISO-8859-1"));

            composeHeader(net);
            composeJunctions(net);
            composeReservoirs(net);
            composeTanks(net);
            composePipes(net);
            composePumps(net);
            composeValves(net);
            composeDemands(net);
            composeEmitters(net);
            composeStatus(net);
            composePatterns(net);
            composeCurves(net);
            composeControls(net);
            composeQuality(net);
            composeSource(net);
            composeMixing(net);
            composeReaction(net);
            composeEnergy(net);
            composeTimes(net);
            composeOptions(net);
            composeExtraOptions(net);
            composeReport(net);
            composeLabels(net);
            composeCoordinates(net);
            composeVertices(net);
            composeRules(net);

            buffer.write(Network.SectType.END.parseStr);
            buffer.close();
        }
        catch (IOException e){return;}
    }

    public void composeHeader(Network net) throws IOException {
        if(net.getTitleText().size()==0)
            return;

        buffer.write(Network.SectType.TITLE.parseStr);
        buffer.newLine();

        for(String str : net.getTitleText()){
            buffer.write(str);
            buffer.newLine();
        }

        buffer.newLine();
    }


    private void composeJunctions(Network net) throws IOException, ENException {
        FieldsMap fMap = net.getFieldsMap();
        PropertiesMap pMap = net.getPropertiesMap();

        if(net.getJunctions().size()==0)
            return;


        buffer.write(Network.SectType.JUNCTIONS.parseStr);
        buffer.newLine();
        buffer.write(JUNCS_SUBTITLE);
        buffer.newLine();

        for(Node node : net.getJunctions()){
            buffer.write(String.format(" %s\t%s",node.getId(),fMap.revertUnit(Type.ELEV, node.getElevation())));

            //if(node.getDemand()!=null && node.getDemand().size()>0 && !node.getDemand().get(0).getPattern().getId().equals(""))
            //    buffer.write("\t"+node.getDemand().get(0).getPattern().getId());

            if(node.getDemand().size()>0){
                Demand d = node.getDemand().get(0);
                buffer.write(String.format("\t%s",fMap.revertUnit(FieldsMap.Type.DEMAND, d.getBase())));

                if (!d.getPattern().getId().equals("") && !pMap.getDefPatId().equals(d.getPattern().getId()))
                    buffer.write("\t" + d.getPattern().getId());
            }

            if(node.getComment().length()!=0)
                buffer.write("\t;"+node.getComment());
            buffer.newLine();
        }


        buffer.newLine();
    }

    private void composeReservoirs(Network net) throws IOException, ENException {
        FieldsMap fMap = net.getFieldsMap();
        if(net.getTanks().size()==0)
            return;

        List<Tank> reservoirs = new ArrayList<Tank>();
        for(Tank tank : net.getTanks())
            if(tank.getArea() == 0)
                reservoirs.add(tank);

        if(reservoirs.size()==0)
            return;

        buffer.write(Network.SectType.RESERVOIRS.parseStr);
        buffer.newLine();
        buffer.write(RESERVOIRS_SUBTITLE);
        buffer.newLine();

        for(Tank r : reservoirs){
            buffer.write(String.format(" %s\t%s",r.getId(),fMap.revertUnit(Type.ELEV, r.getElevation())));


            if (r.getPattern()!=null)
                buffer.write(String.format("\t%s",r.getPattern().getId()));


            if(r.getComment().length()!=0)
                buffer.write("\t;"+r.getComment());
            buffer.newLine();
        }

        buffer.newLine();
    }

    private void composeTanks(Network net) throws IOException, ENException {
        FieldsMap fMap = net.getFieldsMap();

        List<Tank> tanks = new ArrayList<Tank>();
        for(Tank tank : net.getTanks())
            if(tank.getArea()!= 0)
                tanks.add(tank);

        if(tanks.size()==0)
            return;

        buffer.write(Network.SectType.TANKS.parseStr);
        buffer.newLine();
        buffer.write(TANK_SUBTITLE);
        buffer.newLine();

        for(Tank tank : tanks){
            double Vmin = tank.getVmin();
            if(Math.round(Vmin/tank.getArea()) == Math.round(tank.getHmin()-tank.getElevation()))
                Vmin = 0;

            buffer.write(String.format(" %s\t%s\t%s\t%s\t%s\t%s\t%s",
                    tank.getId(),
                    fMap.revertUnit(Type.ELEV, tank.getElevation()),
                    fMap.revertUnit(Type.ELEV, tank.getH0() - tank.getElevation()),
                    fMap.revertUnit(Type.ELEV, tank.getHmin() - tank.getElevation()),
                    fMap.revertUnit(Type.ELEV, tank.getHmax() - tank.getElevation()),
                    fMap.revertUnit(Type.ELEV, 2*Math.sqrt(tank.getArea() / Constants.PI)),
                    fMap.revertUnit(Type.VOLUME,Vmin)));

            if(tank.getVcurve()!=null)
                buffer.write(" "+tank.getVcurve().getId());

            if(tank.getComment().length()!=0)
                buffer.write("\t;"+tank.getComment());
            buffer.newLine();
        }

        buffer.newLine();
    }

    private void composePipes(Network net) throws IOException, ENException {
        FieldsMap fMap = net.getFieldsMap();
        PropertiesMap pMap = net.getPropertiesMap();

        if(net.getLinks().size()==0)
            return;

        List<Link> pipes = new ArrayList<Link>();
        for(Link link : net.getLinks())
            if(link.getType() == LinkType.PIPE || link.getType() == LinkType.CV)
                pipes.add(link);


        buffer.write(Network.SectType.PIPES.parseStr);
        buffer.newLine();
        buffer.write(PIPES_SUBTITLE);
        buffer.newLine();

        for(Link link : pipes){
            double d = link.getDiameter();
            double kc = link.getRoughness();
            if (pMap.getFormflag() == PropertiesMap.FormType.DW)
                kc = fMap.revertUnit(Type.ELEV,kc*1000.0);

            double km = link.getKm()*Math.pow(d,4.0)/0.02517;

            buffer.write(String.format(" %s\t%s\t%s\t%s\t%s",
                    link.getId(),
                    link.getFirst().getId(),
                    link.getSecond().getId(),
                    fMap.revertUnit(Type.LENGTH, link.getLenght()),
                    fMap.revertUnit(Type.DIAM, d)));

            //if (pMap.getFormflag() == FormType.DW)
            buffer.write(String.format(" %s\t%s", kc, km));

            if (link.getType() == LinkType.CV)
                buffer.write(" CV");
            else if (link.getStat() == StatType.CLOSED)
                buffer.write(" CLOSED");
            else if (link.getStat() == StatType.OPEN)
                buffer.write(" OPEN");

            if(link.getComment().length()!=0)
                buffer.write("\t;" + link.getComment());
            buffer.newLine();
        }

        buffer.newLine();
    }

    private void composePumps(Network net) throws IOException, ENException{
        FieldsMap fMap = net.getFieldsMap();
        List<Pump> pumps = new ArrayList<Pump>(net.getPumps());
        if(pumps.size()==0)
            return;

        buffer.write(Network.SectType.PUMPS.parseStr);
        buffer.newLine();
        buffer.write(PUMPS_SUBTITLE);
        buffer.newLine();

        for(Pump pump : pumps)
        {
            buffer.write(String.format(" %s\t%s\t%s", pump.getId(),
                    pump.getFirst().getId(), pump.getSecond().getId()));


            // Pump has constant power
            if (pump.getPtype() == Pump.Type.CONST_HP)
                buffer.write(String.format(" POWER %s", pump.getKm()));
                // Pump has a head curve
            else if (pump.getHcurve()!=null)
                buffer.write(String.format(" HEAD %s", pump.getHcurve().getId()));
                // Old format used for pump curve
            else
            {
                buffer.write(String.format(" %s\t%s\t%s\t0.0\t%s",
                        fMap.revertUnit(Type.HEAD,-pump.getH0()),
                        fMap.revertUnit(Type.HEAD,-pump.getH0() - pump.getFlowCoefficient()*Math.pow(pump.getQ0(),pump.getN())),
                        fMap.revertUnit(Type.FLOW,pump.getQ0()),
                        fMap.revertUnit(Type.FLOW,pump.getQmax())
                ));
                continue;
            }

            if ( pump.getUpat()!=null)
                buffer.write(String.format(" PATTERN %s", pump.getUpat().getId()));


            if (pump.getRoughness() != 1.0)
                buffer.write(String.format(" SPEED %s", pump.getRoughness()));

            if(pump.getComment().length()!=0)
                buffer.write("\t;"+pump.getComment());
            buffer.newLine();
        }

        buffer.newLine();
    }

    private void composeValves(Network net) throws IOException, ENException{
        FieldsMap fMap = net.getFieldsMap();
        List<Valve> valves = new ArrayList<Valve>(net.getValves());
        if(valves.size()==0)
            return;

        buffer.write(Network.SectType.VALVES.parseStr);
        buffer.newLine();
        buffer.write(VALVES_SUBTITLE);
        buffer.newLine();

        for(Valve valve : valves)
        {
            double d = valve.getDiameter();
            double kc = valve.getRoughness();
            if (kc == Constants.MISSING)
                kc = 0.0;

            switch (valve.getType())
            {
                case FCV: kc = fMap.revertUnit(Type.FLOW,kc) ; break;
                case PRV:
                case PSV:
                case PBV: kc = fMap.revertUnit(Type.PRESSURE,kc) ; break;
            }

            double km = valve.getKm()*Math.pow(d,4)/0.02517;

            buffer.write(String.format(" %s\t%s\t%s\t%s\t%5s",
                    valve.getId(),
                    valve.getFirst().getId(),
                    valve.getSecond().getId(),
                    fMap.revertUnit(Type.DIAM,d),
                    valve.getType().parseStr));

            if (valve.getType() == LinkType.GPV && valve.getCurve() != null)
                buffer.write(String.format(" %s\t%s", valve.getCurve().getId(), km));
            else
                buffer.write(String.format(" %s\t%s",kc,km));

            if(valve.getComment().length()!=0)
                buffer.write("\t;"+valve.getComment());
            buffer.newLine();
        }
        buffer.newLine();
    }

    private void composeDemands(Network net) throws IOException, ENException {
        FieldsMap fMap = net.getFieldsMap();

        if(net.getJunctions().size()==0)
            return;

        buffer.write(Network.SectType.DEMANDS.parseStr);
        buffer.newLine();
        buffer.write(DEMANDS_SUBTITLE);
        buffer.newLine();

        double ucf = fMap.getUnits(Type.DEMAND);

        for(Node node : net.getJunctions()){
            if (node.getDemand().size() > 1)
                for(int i = 0;i<node.getDemand().size();i++){
                    Demand demand = node.getDemand().get(i);
                    buffer.write(String.format("%s\t%s",node.getId(), ucf * demand.getBase()));
                    if (demand.getPattern() != null)
                        buffer.write("\t"+demand.getPattern().getId());
                    buffer.newLine();
                }
        }

        buffer.newLine();
    }

    private void composeEmitters(Network net) throws IOException, ENException {
        if(net.getNodes().size()==0)
            return;

        buffer.write(Network.SectType.EMITTERS.parseStr);
        buffer.newLine();
        buffer.write(EMITTERS_SUBTITLE);
        buffer.newLine();

        double uflow = net.getFieldsMap().getUnits(Type.FLOW);
        double upressure = net.getFieldsMap().getUnits(Type.PRESSURE);
        double Qexp = net.getPropertiesMap().getQexp();

        for(Node node : net.getJunctions()){
            if(node.getKe()==0.0) continue;
            double ke = uflow/Math.pow(upressure * node.getKe(), (1.0 / Qexp));
            buffer.write(String.format(" %s\t%s",node.getId(),ke));
            buffer.newLine();
        }

        buffer.newLine();
    }

    private void composeStatus(Network net) throws IOException, ENException {

        if(net.getLinks().size()==0)
            return;

        buffer.write(Network.SectType.STATUS.parseStr);
        buffer.newLine();
        buffer.write(STATUS_SUBTITLE);
        buffer.newLine();

        for(Link link : net.getLinks())
        {
            if (link.getType().id <= LinkType.PUMP.id)
            {
                if (link.getStat() == StatType.CLOSED)
                    buffer.write(String.format(" %s\t%s\n",link.getId(),StatType.CLOSED.parseStr));

                    // Write pump speed here for pumps with old-style pump curve input
                else if (link.getType() == LinkType.PUMP){
                    Pump pump = (Pump)link;
                    if (pump.getHcurve() == null &&
                            pump.getPtype() != Pump.Type.CONST_HP &&
                            pump.getRoughness() != 1.0)
                        buffer.write(String.format(" %s\t%s\n", link.getId(), link.getRoughness()));
                }
            }
            // Write fixed-status PRVs & PSVs (setting = MISSING)
            else if (link.getRoughness() == Constants.MISSING)
            {
                if (link.getStat() == StatType.OPEN)
                    buffer.write(String.format(" %s\t%s\n",link.getId(),StatType.OPEN.parseStr));
                if (link.getStat() == StatType.CLOSED)
                    buffer.write(String.format(" %s\t%s\n", link.getId(), StatType.CLOSED.parseStr));

            }

        }

        buffer.newLine();
    }

    private void composePatterns(Network net) throws IOException, ENException {

        List<Pattern> pats = new ArrayList<Pattern>(net.getPatterns());

        if(pats.size()<=1)
            return;

        buffer.write(Network.SectType.PATTERNS.parseStr);
        buffer.newLine();
        buffer.write(PATTERNS_SUBTITLE);
        buffer.newLine();

        for(int i = 1;i<pats.size();i++)
        {
            Pattern pat = pats.get(i);
            List<Double> F = pat.getFactorsList();
            for (int j=0; j<pats.get(i).getLength(); j++)
            {
                if (j % 6 == 0)
                    buffer.write(String.format(" %s",pat.getId()));
                buffer.write(String.format(" %s",F.get(j)));

                if (j % 6 == 5)
                    buffer.newLine();
            }
            buffer.newLine();
        }

        buffer.newLine();
    }

    private void composeCurves(Network net) throws IOException, ENException {

        List<Curve> curves = new ArrayList<Curve>(net.getCurves());

        if(curves.size()==0)
            return;

        buffer.write(Network.SectType.CURVES.parseStr);
        buffer.newLine();
        buffer.write(CURVE_SUBTITLE);
        buffer.newLine();

        for(Curve c : curves)
        {
            for(int i = 0;i < c.getNpts();i++){
                buffer.write(String.format(" %s\t%s\t%s",
                        c.getId(),c.getX().get(i),c.getY().get(i)));
                buffer.newLine();
            }
        }

        buffer.newLine();
    }

    private void composeControls(Network net) throws IOException, ENException {
        Control [] controls = net.getControls();
        FieldsMap fmap = net.getFieldsMap();

        if(controls.length == 0)
            return;

        buffer.write(Network.SectType.CONTROLS.parseStr);
        buffer.newLine();


        for(Control control : controls)
        {
            // Check that controlled link exists
            if (control.getLink()==null) continue;

            // Get text of control's link status/setting
            if (control.getSetting() == Constants.MISSING)
                buffer.write(String.format(" LINK %s %s ", control.getLink().getId(), control.getStatus().parseStr));
            else
            {
                Double kc = control.getSetting();
                switch(control.getLink().getType())
                {
                    case PRV:
                    case PSV:
                    case PBV: kc = fmap.revertUnit(Type.PRESSURE,kc); break;
                    case FCV: kc = fmap.revertUnit(Type.FLOW,kc);     break;
                }
                buffer.write(String.format(" LINK %s %s",control.getLink().getId(), kc));
            }


            switch (control.getType())
            {
                // Print level control
                case LOWLEVEL:
                case HILEVEL:
                    double kc = control.getGrade() - control.getNode().getElevation();
                    if (control.getNode() instanceof Tank) kc = fmap.revertUnit(Type.HEAD,kc);
                    else
                        kc = fmap.revertUnit(Type.PRESSURE,kc);
                    buffer.write(String.format(" IF NODE %s %s %s",
                            control.getNode().getId(), control.getType().parseStr, kc));
                    break;

                // Print timer control
                case TIMER:
                    buffer.write(String.format(" AT %s %s HOURS",
                            ControlType.TIMER.parseStr, control.getTime()/3600.0f));
                    break;

                // Print time-of-day control
                case TIMEOFDAY:
                    buffer.write(String.format(" AT %s %s", ControlType.TIMEOFDAY.parseStr, Utilities.getClockTime(control.getTime())));
                    break;
            }
            buffer.newLine();
        }
        buffer.newLine();
    }

    private void composeQuality(Network net) throws IOException, ENException {
        Collection<Node> nodes = net.getNodes();
        FieldsMap fmap = net.getFieldsMap();
        if(nodes.size() == 0)
            return;

        buffer.write(Network.SectType.QUALITY.parseStr);
        buffer.newLine();
        buffer.write(QUALITY_SUBTITLE);
        buffer.newLine();

        for(Node node : nodes)
        {
            if (node.getC0().length == 1 ){
                if(node.getC0()[0] == 0.0 ) continue;
                buffer.write(String.format(" %s\t%s",node.getId(),fmap.revertUnit(Type.QUALITY,node.getC0()[0])));
            }
            buffer.newLine();
        }
        buffer.newLine();
    }

    private void composeSource(Network net) throws IOException {
        Collection<Node> nodes = net.getNodes();

        if(nodes.size() == 0)
            return;

        buffer.write(Network.SectType.SOURCES.parseStr);
        buffer.newLine();
        buffer.write(SOURCE_SUBTITLE);
        buffer.newLine();


        for(Node node : nodes){
            Source source = node.getSource();
            if (source == null)
                continue;
            buffer.write(String.format(" %s\t%s\t%s",
                    node.getId(),
                    source.getType().parseStr,
                    source.getC0()));
            if (source.getPattern()!=null)
                buffer.write(" " + source.getPattern().getId());
            buffer.newLine();
        }
        buffer.newLine();
    }


    private void composeMixing(Network net) throws IOException{
        if(net.getTanks().size()==0)
            return;

        buffer.write(Network.SectType.MIXING.parseStr);
        buffer.newLine();
        buffer.write(MIXING_SUBTITLE);
        buffer.newLine();

        for(Tank tank : net.getTanks())
        {
            if (tank.getArea() == 0.0) continue;
            buffer.write(String.format(" %s\t%s\t%s",
                    tank.getId(),tank.getMixModel().parseStr,
                    (tank.getV1max() / tank.getVmax())));
            buffer.newLine();
        }
        buffer.newLine();
    }

    private void composeReaction(Network net) throws IOException, ENException {
        PropertiesMap pMap = net.getPropertiesMap();

        buffer.write(Network.SectType.REACTIONS.parseStr);
        buffer.newLine();
        buffer.write(REACTIONS_SUBTITLE);
        buffer.newLine();


        buffer.write(String.format("ORDER BULK %s\n", pMap.getBulkOrder()));
        buffer.write(String.format("ORDER WALL %s\n", pMap.getWallOrder()));
        buffer.write(String.format("ORDER TANK %s\n", pMap.getTankOrder()));
        buffer.write(String.format("GLOBAL BULK %s\n", pMap.getKbulk() * Constants.SECperDAY));
        buffer.write(String.format("GLOBAL WALL %s\n", pMap.getKwall() * Constants.SECperDAY));

        //if (pMap.getClimit() > 0.0)
        buffer.write(String.format("LIMITING POTENTIAL %s\n", pMap.getClimit()));

        //if (pMap.getRfactor() != Constants.MISSING && pMap.getRfactor() != 0.0)
        buffer.write(String.format("ROUGHNESS CORRELATION %s\n", pMap.getRfactor()));


        for(Link link : net.getLinks())
        {
            if (link.getType().id > LinkType.PIPE.id)
                continue;

            if (link.getKb() != pMap.getKbulk())
                buffer.write(String.format("BULK %s %s\n", link.getId(), link.getKb() * Constants.SECperDAY));
            if (link.getKw() != pMap.getKwall())
                buffer.write(String.format("WALL %s %s\n", link.getId(), link.getKw() * Constants.SECperDAY));
        }

        for(Tank tank : net.getTanks())
        {
            if (tank.getArea() == 0.0) continue;
            if (tank.getKb() != pMap.getKbulk())
                buffer.write(String.format("TANK %s %s\n",tank.getId(),tank.getKb()*Constants.SECperDAY));
        }
        buffer.newLine();
    }

    private void composeEnergy(Network net) throws IOException, ENException {
        PropertiesMap pMap = net.getPropertiesMap();

        buffer.write(Network.SectType.ENERGY.parseStr);
        buffer.newLine();

        if (pMap.getEcost() != 0.0)
            buffer.write(String.format("GLOBAL PRICE %s\n", pMap.getEcost()));
        if (!pMap.getEpatId().equals(""))
            buffer.write(String.format("GLOBAL PATTERN %s\n",  pMap.getEpatId()));
        buffer.write(String.format("GLOBAL EFFIC %s\n", pMap.getEpump()));
        buffer.write(String.format("DEMAND CHARGE %s\n", pMap.getDcost()));
        for(Pump p : net.getPumps())
        {
            if (p.getEcost() > 0.0)
                buffer.write(String.format("PUMP %s PRICE %s\n",p.getId(),p.getEcost()));
            if (p.getEpat() != null)
                buffer.write(String.format("PUMP %s PATTERN %s\n",
                        p.getId(),p.getEpat().getId()));
            if (p.getEcurve() !=null)
                buffer.write(String.format("PUMP %s EFFIC %s\n",
                        p.getId(),p.getEcurve().getId()));
        }
        buffer.newLine();

    }

    private void composeTimes(Network net) throws IOException, ENException {
        PropertiesMap pMap = net.getPropertiesMap();
        buffer.write(Network.SectType.TIMES.parseStr);
        buffer.newLine();
        buffer.write(String.format("DURATION %s\n", Utilities.getClockTime(pMap.getDuration())));
        buffer.write(String.format("HYDRAULIC TIMESTEP %s\n", Utilities.getClockTime(pMap.getHstep())));
        buffer.write(String.format("QUALITY TIMESTEP %s\n", Utilities.getClockTime(pMap.getQstep())));
        buffer.write(String.format("REPORT TIMESTEP %s\n", Utilities.getClockTime(pMap.getRstep())));
        buffer.write(String.format("REPORT START %s\n", Utilities.getClockTime(pMap.getRstart())));
        buffer.write(String.format("PATTERN TIMESTEP %s\n", Utilities.getClockTime(pMap.getPstep())));
        buffer.write(String.format("PATTERN START %s\n", Utilities.getClockTime(pMap.getPstart())));
        buffer.write(String.format("RULE TIMESTEP %s\n", Utilities.getClockTime(pMap.getRulestep())));
        buffer.write(String.format("START CLOCKTIME %s\n", Utilities.getClockTime(pMap.getTstart())));
        buffer.write(String.format("STATISTIC %s\n", pMap.getTstatflag().parseStr));
        buffer.newLine();
    }

    private void composeOptions(Network net) throws IOException,ENException{
        PropertiesMap pMap = net.getPropertiesMap();
        FieldsMap fMap = net.getFieldsMap();
        buffer.write(Network.SectType.OPTIONS.parseStr);
        buffer.newLine();
        buffer.write(String.format("UNITS %s\n", pMap.getFlowflag().parseStr));
        buffer.write(String.format("PRESSURE %s\n", pMap.getPressflag().parseStr));
        buffer.write(String.format("HEADLOSS %s\n", pMap.getFormflag().parseStr));

        if (!pMap.getDefPatId().equals(""))
            buffer.write(String.format("PATTERN %s\n", pMap.getDefPatId()));
        if (pMap.getHydflag() == PropertiesMap.Hydtype.USE)
            buffer.write(String.format("HYDRAULICS USE %s\n", pMap.getHydFname()));
        if (pMap.getHydflag() == PropertiesMap.Hydtype.SAVE)
            buffer.write(String.format("HYDRAULICS SAVE %s\n", pMap.getHydFname()));
        if (pMap.getExtraIter() == -1)
            buffer.write("UNBALANCED STOP\n");
        if (pMap.getExtraIter() >= 0)
            buffer.write(String.format("UNBALANCED CONTINUE %d\n", pMap.getExtraIter()));

        if (pMap.getQualflag() == PropertiesMap.QualType.CHEM)
            buffer.write(String.format("QUALITY %s %s\n", pMap.getChemName(), pMap.getChemUnits()));
        if (pMap.getQualflag() == PropertiesMap.QualType.TRACE)
            buffer.write(String.format("QUALITY TRACE %s\n", pMap.getTraceNode()));
        if (pMap.getQualflag() == PropertiesMap.QualType.AGE)
            buffer.write("QUALITY AGE\n");
        if (pMap.getQualflag() == PropertiesMap.QualType.NONE)
            buffer.write("QUALITY NONE\n");

        buffer.write(String.format("DEMAND MULTIPLIER %s\n", pMap.getDmult()));
        buffer.write(String.format("EMITTER EXPONENT %s\n", 1.0/pMap.getQexp()));
        buffer.write(String.format("VISCOSITY %s\n", pMap.getViscos()/Constants.VISCOS));
        buffer.write(String.format("DIFFUSIVITY %s\n", pMap.getDiffus()/Constants.DIFFUS));
        buffer.write(String.format("SPECIFIC GRAVITY %s\n", pMap.getSpGrav()));
        buffer.write(String.format("TRIALS %d\n",   pMap.getMaxIter()));
        buffer.write(String.format("ACCURACY %s\n", pMap.getHacc()));
        buffer.write(String.format("TOLERANCE %s\n", fMap.revertUnit(Type.QUALITY,pMap.getCtol())));
        buffer.write(String.format("CHECKFREQ %d\n", pMap.getCheckFreq()));
        buffer.write(String.format("MAXCHECK  %d\n", pMap.getMaxCheck()));
        buffer.write(String.format("DAMPLIMIT %s\n", pMap.getDampLimit()));
        buffer.newLine();
    }

    private void composeExtraOptions(Network net) throws ENException, IOException {
        PropertiesMap pMap = net.getPropertiesMap();
        List<String> otherObjsNames = pMap.getObjectsNames(true);

        if(otherObjsNames.size()==0)
            return;

        for(String objName : otherObjsNames){
            Object objVal = pMap.get(objName);
            buffer.write(objName + " " + objVal);
            buffer.newLine();
        }
        buffer.newLine();

    }

    private void composeReport(Network net) throws IOException,ENException{

        buffer.write(Network.SectType.REPORT.parseStr);
        buffer.newLine();

        PropertiesMap pMap = net.getPropertiesMap();
        FieldsMap fMap = net.getFieldsMap();
        buffer.write(String.format("PAGESIZE %d\n",pMap.getPageSize()));
        buffer.write(String.format("STATUS %s\n", pMap.getStatflag().parseStr));
        buffer.write(String.format("SUMMARY %s\n", pMap.getSummaryflag() ? Keywords.w_YES:Keywords.w_NO));
        buffer.write(String.format("ENERGY %s\n", pMap.getEnergyflag() ? Keywords.w_YES:Keywords.w_NO));

        switch (pMap.getNodeflag()){
            case FALSE:
                buffer.write("NODES NONE\n");
                break;
            case TRUE:
                buffer.write("NODES ALL\n");
                break;
            case SOME:
            {
                int j = 0;
                for(Node node : net.getNodes()){
                    if(node.isRptFlag()){
                        if (j % 5 == 0) buffer.write("NODES \n");
                        buffer.write(String.format("%s ", node.getId()));
                        j++;
                    }
                }
                break;
            }
        }

        switch (pMap.getLinkflag()){
            case FALSE:
                buffer.write("LINKS NONE\n");
                break;
            case TRUE:
                buffer.write("LINKS ALL\n");
                break;
            case SOME:
            {
                int j = 0;
                for(Link link : net.getLinks()){
                    if(link.isRptFlag()){
                        if (j % 5 == 0) buffer.write("LINKS \n");
                        buffer.write(String.format("%s ", link.getId()));
                        j++;
                    }
                }
                break;
            }
        }

        for(int i = 0;i< Type.FRICTION.id;i++){
            Type t = null;
            for (Type type : Type.values()) {
                if(type.id==i){
                    t=type;break;
                }
            }

            Field f = fMap.getField(t);
            if(f.isEnabled()){
                buffer.write(String.format("%-19s PRECISION %d\n", f.getName(), f.getPrecision()));
                if (f.getRptLim(Field.RangeType.LOW) < Constants.BIG)
                    buffer.write(String.format("%-19s BELOW %s\n", f.getName(), f.getRptLim(Field.RangeType.LOW)));
                if (f.getRptLim(Field.RangeType.HI) > -Constants.BIG)
                    buffer.write(String.format("%-19s ABOVE %s", f.getName(), f.getRptLim(Field.RangeType.HI)));
            }
            else
                buffer.write(String.format("%-19s NO\n", f.getName()));
        }

        buffer.newLine();
    }

    private void composeCoordinates(Network net) throws IOException {
        buffer.write(Network.SectType.COORDS.parseStr);
        buffer.newLine();
        buffer.write(COORDINATES_SUBTITLE);
        buffer.newLine();

        for(Node node : net.getNodes()){
            if(node.getPosition()!=null){
                buffer.write(String.format(" %s\t%s\t%s\n",node.getId(),node.getPosition().getX(),node.getPosition().getY()));
            }
        }
        buffer.newLine();
    }


    private void composeLabels(Network net) throws IOException {
        buffer.write(Network.SectType.LABELS.parseStr);
        buffer.newLine();
        buffer.write(";X-Coord\tY-Coord\tLabel & Anchor Node");
        buffer.newLine();

        for(Label label : net.getLabels()){
            buffer.write(String.format(" %s\t%s\t\"%s\"\n", label.getPosition().getX(), label.getPosition().getY(), label.getText()));
        }
        buffer.newLine();
    }

    private void composeVertices(Network net) throws IOException {
        buffer.write(Network.SectType.VERTICES.parseStr);
        buffer.newLine();
        buffer.write(";Link\tX-Coord\tY-Coord");
        buffer.newLine();

        for(Link link : net.getLinks()){
            for(Point p : link.getVertices()){
                buffer.write(String.format(" %s\t%s\t%s\n",link.getId(),p.getX(),p.getY()));
            }
        }

        buffer.newLine();
    }

    private void composeRules(Network net) throws IOException, ENException {
        buffer.write(Network.SectType.RULES.parseStr);
        buffer.newLine();

        for(Rule r : net.getRules()){
            buffer.write("RULE " + r.getLabel()+"\n");
            for(String s : r.getCode().split("\n"))
                buffer.write(s+"\n");
            buffer.newLine();
        }
        buffer.newLine();
    }
}
