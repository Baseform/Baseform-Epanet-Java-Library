package org.addition.epanetold.Types;

import java.util.ArrayList;
import java.util.List;

/* LINK OBJECT */
public class Link
{
    String   ID;
    int      N1;
    int      N2;
    double   diameter;
    double   len;
    double   kc;
    double   km;
    double   kb;
    double   kw;
    double   r;
    EnumVariables.LinkType type;
    EnumVariables.StatType stat;
    boolean  rptFlag;
    List<Point> vertices;

    public Link() {
        vertices = new ArrayList<Point>();
    }

    public List<Point> getVertices() {
        return vertices;
    }

    public String getId() {
        return ID;
    }

    public void setId(String id) {
        ID=id;
    }

    public int getN1() {
        return N1;
    }

    public void setN1(int n1) {
        N1 = n1;
    }

    public int getN2() {
        return N2;
    }

    public void setN2(int n2) {
        N2 = n2;
    }

    public double getDiameter() {
        return diameter;
    }

    public void setDiameter(double diameter) {
        this.diameter = diameter;
    }

    public double getLenght() {
        return len;
    }

    public void setLenght(double len) {
        this.len = len;
    }

    public double getKc() {
        return kc;
    }

    public void setKc(double kc) {
        this.kc = kc;
    }

    public double getKm() {
        return km;
    }

    public void setKm(double km) {
        this.km = km;
    }

    public double getKb() {
        return kb;
    }

    public void setKb(double kb) {
        this.kb = kb;
    }

    public double getKw() {
        return kw;
    }

    public void setKw(double kw) {
        this.kw = kw;
    }

    public double getR() {
        return r;
    }

    public void setR(double r) {
        this.r = r;
    }

    public EnumVariables.LinkType getType() {
        return type;
    }

    public void setType(EnumVariables.LinkType type) {
        this.type = type;
    }

    public EnumVariables.StatType getStat() {
        return stat;
    }

    public void setStatus(EnumVariables.StatType stat) {
        this.stat = stat;
    }

    public boolean isRptFlag() {
        return rptFlag;
    }

    public void setReportFlag(boolean rptFlag) {
        this.rptFlag = rptFlag;
    }

}
