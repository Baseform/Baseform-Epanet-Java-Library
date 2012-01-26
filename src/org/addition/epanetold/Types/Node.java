package org.addition.epanetold.Types;

import java.util.ArrayList;
import java.util.List;

// NODE OBJECT
public class Node
{
    private String id;
    private double elevation;
    private List<Demand> demand;
    private Source source;
    private double C0;
    private double Ke;
    private boolean rptFlag;
    private Point position;

    public Node() {
        demand = new ArrayList<Demand>();
        position = new Point();
    }

    public Point getPosition() {
        return position;
    }

    public void setPosition(Point position) {
        this.position = position;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getElevation() {
        return elevation;
    }

    public void setElevation(double elevation) {
        this.elevation = elevation;
    }

    public List<Demand> getDemand() {
        return demand;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public double getC0() {
        return C0;
    }

    public void setC0(double c0) {
        C0 = c0;
    }

    public double getKe() {
        return Ke;
    }

    public void setKe(double ke) {
        Ke = ke;
    }

    public boolean getRptFlag() {
        return rptFlag;
    }

    public void setReportFlag(boolean rptFlag) {
        this.rptFlag = rptFlag;
    }

}