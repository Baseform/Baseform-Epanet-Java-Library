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

package org.addition.epanet.hydraulic.structures;


import org.addition.epanet.Constants;
import org.addition.epanet.network.FieldsMap;
import org.addition.epanet.network.FieldsMap.Type;
import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.network.io.Keywords;
import org.addition.epanet.network.structures.Control;
import org.addition.epanet.network.structures.Control.ControlType;
import org.addition.epanet.network.structures.Link.LinkType;
import org.addition.epanet.network.structures.Link.StatType;
import org.addition.epanet.util.ENException;
import org.addition.epanet.util.Utilities;

import java.util.List;
import java.util.logging.Logger;

public class SimulationControl {
    private final Control control;
    private  SimulationLink link;
    private SimulationNode node=null;

    public SimulationControl(List<SimulationNode> nodes, List<SimulationLink> links, Control ref) {
        if (ref.getNode() != null) {
            String nid = ref.getNode().getId();
            for (SimulationNode simulationNode : nodes) {
                if (simulationNode.getId().equals(nid)) {
                    node = simulationNode;
                    break;
                }
            }
        }

        if (ref.getLink() != null)
        {
            String linkId = ref.getLink().getId();
            for (SimulationLink simulationLink : links) {
                if(simulationLink.getLink().getId().equals(linkId))
                {
                    link = simulationLink;break;
                }
            }
        }

        control = ref;
    }

    public SimulationLink getLink() {
        return link;
    }

    public SimulationNode getNode() {
        return node;
    }


    public long getTime() {
        return control.getTime();
    }


    public double getGrade() {
        return control.getGrade();
    }


    public double getSetting() {
        return control.getSetting();
    }


    public StatType getStatus() {
        return control.getStatus();
    }

    public ControlType getType() {
        return control.getType();
    }

    /**
     * Get the shortest time step to activate the control.
     *
     * @param fMap
     * @param pMap
     * @param htime
     * @param tstep
     * @return
     * @throws ENException
     */
    private long getRequiredTimeStep(FieldsMap fMap, PropertiesMap pMap, long htime, long tstep) throws ENException {

        long t = 0;

        // Node control
        if (getNode() != null) {

            if (!(getNode() instanceof SimulationTank)) // Check if node is a tank
                return tstep;

            double h = node.getSimHead();           // Current tank grade
            double q = node.getSimDemand();         // Flow into tank

            if (Math.abs(q) <= Constants.QZERO)
                return tstep;

            if ((h < getGrade() && getType() == ControlType.HILEVEL && q > 0.0)  // Tank below hi level & filling
                    || (h > getGrade() && getType() == ControlType.LOWLEVEL && q < 0.0)) // Tank above low level & emptying
            {
                SimulationTank tank = ((SimulationTank) getNode());
                double v = tank.findVolume(fMap, getGrade()) - tank.getSimVolume();
                t = Math.round(v / q); // Time to reach level
            }
        }

        // Time control
        if (getType() == ControlType.TIMER) {
            if (getTime() > htime)
                t = getTime() - htime;
        }

        // Time-of-day control
        if (getType() == ControlType.TIMEOFDAY) {
            long t1 = (htime + pMap.getTstart()) % Constants.SECperDAY;
            long t2 = getTime();
            if (t2 >= t1) t = t2 - t1;
            else t = Constants.SECperDAY - t1 + t2;
        }

        // Revise time step
        if (t > 0 && t < tstep) {
            SimulationLink link = getLink();

            // Check if rule actually changes link status or setting
            if (link != null && (link.getType().id > LinkType.PIPE.id && link.getSimSetting() != getSetting())
                    || (link.getSimStatus() != getStatus()))
                tstep = t;
        }

        return tstep;
    }

    // Revises time step based on shortest time to fill or drain a tank
    public static long minimumTimeStep(FieldsMap fMap, PropertiesMap pMap, List<SimulationControl> controls,
                                       long htime, long tstep) throws ENException {
        long newTStep = tstep;
        for (SimulationControl control : controls)
            newTStep = control.getRequiredTimeStep(fMap, pMap, htime, newTStep);
        return newTStep;
    }


    // Implements simple controls based on time or tank levels
    public static int stepActions(Logger log,
                                  FieldsMap fMap,
                                  PropertiesMap pMap,
                                  List<SimulationControl> controls,
                                  long htime) throws ENException {
        int setsum = 0;

        // Examine each control statement
        for (SimulationControl control : controls) {
            boolean reset = false;

            // Make sure that link is defined
            if (control.getLink() == null)
                continue;

            // Link is controlled by tank level
            if (control.getNode() != null && control.getNode() instanceof SimulationTank) {

                double h = control.getNode().getSimHead();
                double vplus = Math.abs(control.getNode().getSimDemand());

                SimulationTank tank = (SimulationTank) control.getNode();

                double v1 = tank.findVolume(fMap, h);
                double v2 = tank.findVolume(fMap, control.getGrade());

                if (control.getType() == ControlType.LOWLEVEL && v1 <= v2 + vplus)
                    reset = true;
                if (control.getType() == ControlType.HILEVEL && v1 >= v2 - vplus)
                    reset = true;
            }

            // Link is time-controlled
            if (control.getType() == ControlType.TIMER) {
                if (control.getTime() == htime)
                    reset = true;
            }

            //  Link is time-of-day controlled
            if (control.getType() == ControlType.TIMEOFDAY) {
                if ((htime + pMap.getTstart()) % Constants.SECperDAY == control.getTime())
                    reset = true;
            }

            // Update link status & pump speed or valve setting
            if (reset) {
                StatType s1, s2;
                SimulationLink link = control.getLink();

                if (link.getSimStatus().id <= StatType.CLOSED.id)
                    s1 = StatType.CLOSED;
                else
                    s1 = StatType.OPEN;

                s2 = control.getStatus();

                double k1 = link.getSimSetting();
                double k2 = k1;

                if (control.getLink().getType().id > LinkType.PIPE.id)
                    k2 = control.getSetting();

                if (s1 != s2 || k1 != k2) {
                    link.setSimStatus(s2);
                    link.setSimSetting(k2);
                    if (pMap.getStatflag() != null)
                        logControlAction(log, control, htime);
                    setsum++;
                }
            }
        }

        return (setsum);
    }


    // Adjusts settings of links controlled by junction pressures after a hydraulic solution is found
    public static boolean pSwitch(Logger log, PropertiesMap pMap, FieldsMap fMap, List<SimulationControl> controls) throws ENException {
        boolean anychange = false;

        for (SimulationControl control : controls) {
            boolean reset = false;
            if (control.getLink() == null)
                continue;

            // Determine if control based on a junction, not a tank
            if (control.getNode() != null && !(control.getNode() instanceof SimulationTank)) {

                // Determine if control conditions are satisfied
                if (control.getType() == ControlType.LOWLEVEL && control.getNode().getSimHead() <= control.getGrade() + pMap.getHtol())
                    reset = true;

                if (control.getType() == ControlType.HILEVEL && control.getNode().getSimHead() >= control.getGrade() - pMap.getHtol())
                    reset = true;
            }

            SimulationLink link = control.getLink();

            //  Determine if control forces a status or setting change
            if (reset) {
                boolean change = false;

                StatType s = link.getSimStatus();

                if (link.getType() == LinkType.PIPE) {
                    if (s != control.getStatus()) change = true;
                }

                if (link.getType() == LinkType.PUMP) {
                    if (link.getSimSetting() != control.getSetting()) change = true;
                }

                if (link.getType().id >= LinkType.PRV.id) {
                    if (link.getSimSetting() != control.getSetting())
                        change = true;
                    else if (link.getSimSetting() == Constants.MISSING &&
                            s != control.getStatus()) change = true;
                }

                // If a change occurs, update status & setting
                if (change) {
                    link.setSimStatus(control.getStatus());
                    if (link.getType().id > LinkType.PIPE.id)
                        link.setSimSetting(control.getSetting());
                    if (pMap.getStatflag() == PropertiesMap.StatFlag.FULL)
                        logStatChange(log, fMap, link, s);

                    anychange = true;
                }
            }
        }
        return (anychange);
    }

    private static void logControlAction(Logger log, SimulationControl control, long Htime) {
        SimulationNode n = control.getNode();
        SimulationLink l = control.getLink();
        String Msg = "";
        switch (control.getType()) {

            case LOWLEVEL:
            case HILEVEL: {
                String type = Keywords.w_JUNC;//  NodeType type= NodeType.JUNC;
                if (n instanceof SimulationTank) {
                    if (((SimulationTank) n).isReservoir())
                        type = Keywords.w_RESERV;
                    else
                        type = Keywords.w_TANK;
                }
                Msg = String.format(Utilities.getText("FMT54"), Utilities.getClockTime(Htime), l.getType().parseStr,
                        l.getLink().getId(), type, n.getId());
                break;
            }
            case TIMER:
            case TIMEOFDAY:
                Msg = String.format(Utilities.getText("FMT55"), Utilities.getClockTime(Htime), l.getType().parseStr,
                        l.getLink().getId());
                break;
            default:
                return;
        }
        log.warning(Msg);
    }

    private static void logStatChange(Logger log, FieldsMap fMap, SimulationLink link, StatType oldstatus) {
        StatType s1 = oldstatus;
        StatType s2 = link.getSimStatus();
        try {
            if (s2 == s1) {
                double setting = link.getSimSetting();
                switch (link.getType()) {
                    case PRV:
                    case PSV:
                    case PBV:
                        setting *= fMap.getUnits(Type.PRESSURE);
                        break;
                    case FCV:
                        setting *= fMap.getUnits(Type.FLOW);
                }
                log.warning(String.format(Utilities.getText("FMT56"), link.getType().parseStr, link.getLink().getId(), setting));
                return;
            }

            StatType j1, j2;

            if (s1 == StatType.ACTIVE)
                j1 = StatType.ACTIVE;
            else if (s1.ordinal() <= StatType.CLOSED.ordinal())
                j1 = StatType.CLOSED;
            else
                j1 = StatType.OPEN;
            if (s2 == StatType.ACTIVE) j2 = StatType.ACTIVE;
            else if (s2.ordinal() <= StatType.CLOSED.ordinal())
                j2 = StatType.CLOSED;
            else
                j2 = StatType.OPEN;

            if (j1 != j2) {
                log.warning(String.format(Utilities.getText("FMT57"), link.getType().parseStr,
                        link.getLink().getId(), j1.reportStr, j2.reportStr));
            }
        } catch (ENException e) {
        }
    }
}
