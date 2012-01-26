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

import org.addition.epanet.network.structures.Link.*;
import org.addition.epanet.network.io.Keywords;

/**
 * Control statement
 */
public class Control
{
    /**
     * Control condition type
     */
    static public enum ControlType{
        HILEVEL     (1,Keywords.w_ABOVE),       // act when grade below set level
        LOWLEVEL    (0, Keywords.w_BELOW),       // act when grade above set level
        TIMEOFDAY   (3,Keywords.w_CLOCKTIME),        // act when set time reached
        TIMER       (2,Keywords.w_TIME);   // act when time of day occurs

        public final int    id;
        public final String parseStr;

        private ControlType(int val, String str){id = val;parseStr=str;}
    }

    /**
     * Control grade.
     */
    private double      Grade;
    /**
     * Assigned link reference.
     */
    private Link        Link;
    /**
     * Assigned node reference.
     */
    private Node        Node;
    /**
     * New link setting.
     */
    private double      Setting;
    /**
     * New link status.
     */
    private StatType    Status;
    /**
     * Control time (in seconds).
     */
    private long        Time;
    /**
     * Control type
     */
    private ControlType Type;


    public double getGrade() {
        return Grade;
    }

    public Link getLink() {
        return Link;
    }

    public Node getNode() {
        return Node;
    }

    public double getSetting() {
        return Setting;
    }

    public StatType getStatus() {
        return Status;
    }

    public long getTime() {
        return Time;
    }

    public ControlType getType() {
        return Type;
    }

    public void setGrade(double grade) {
        Grade = grade;
    }

    public void setLink(Link link) {
        Link = link;
    }

    public void setNode(Node node) {
        Node = node;
    }

    public void setSetting(double setting) {
        Setting = setting;
    }

    public void setStatus(StatType status) {
        Status = status;
    }

    public void setTime(long time) {
        Time = time;
    }

    public void setType(ControlType type) {
        Type = type;
    }

}
