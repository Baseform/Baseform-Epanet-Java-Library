package org.addition.epanetold.Types;


public class Control {

    private int             Link;
    private int             Node;
    private long            Time;
    private double          Grade;
    private double          Setting;
    private EnumVariables.StatType   Status;

    public int getLink() {
        return Link;
    }

    public void setLink(int link) {
        Link = link;
    }

    public int getNode() {
        return Node;
    }

    public void setNode(int node) {
        Node = node;
    }

    public long getTime() {
        return Time;
    }

    public void setTime(long time) {
        Time = time;
    }

    public double getGrade() {
        return Grade;
    }

    public void setGrade(double grade) {
        Grade = grade;
    }

    public double getSetting() {
        return Setting;
    }

    public void setSetting(double setting) {
        Setting = setting;
    }

    public EnumVariables.StatType getStatus() {
        return Status;
    }

    public void setStatus(EnumVariables.StatType status) {
        Status = status;
    }

    public EnumVariables.ControlType getType() {
        return Type;
    }

    public void setType(EnumVariables.ControlType type) {
        Type = type;
    }

    EnumVariables.ControlType  Type;

}
