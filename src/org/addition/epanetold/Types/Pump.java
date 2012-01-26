package org.addition.epanetold.Types;


/* PUMP OBJECT */
public class Pump
{
    private int    Link;
    private EnumVariables.PumpType    Ptype;
    private double Q0;
    private double QMax;
    private double HMax;
    private double H0;
    private double R;
    private double N;
    private int    Hcurve;
    private int    Ecurve;
    private int    Upat;
    private int    Epat;
    private double Ecost;
    private double Energy[] = {0,0,0,0,0,0};


    public int getLink() {
        return Link;
    }

    public void setLink(int link) {
        Link = link;
    }

    public EnumVariables.PumpType getPtype() {
        return Ptype;
    }

    public void setPtype(EnumVariables.PumpType ptype) {
        Ptype = ptype;
    }

    public double getQ0() {
        return Q0;
    }

    public void setQ0(double q0) {
        Q0 = q0;
    }

    public double getQMax() {
        return QMax;
    }

    public void setQMax(double QMax) {
        this.QMax = QMax;
    }

    public double getHMax() {
        return HMax;
    }

    public void setHMax(double HMax) {
        this.HMax = HMax;
    }

    public double getH0() {
        return H0;
    }

    public void setH0(double h0) {
        H0 = h0;
    }

    public double getR() {
        return R;
    }

    public void setR(double r) {
        R = r;
    }

    public double getN() {
        return N;
    }

    public void setN(double n) {
        N = n;
    }

    public int getHcurve() {
        return Hcurve;
    }

    public void setHcurve(int hcurve) {
        Hcurve = hcurve;
    }

    public int getEcurve() {
        return Ecurve;
    }

    public void setEcurve(int ecurve) {
        Ecurve = ecurve;
    }

    public int getUpat() {
        return Upat;
    }

    public void setUpat(int upat) {
        Upat = upat;
    }

    public int getEpat() {
        return Epat;
    }

    public void setEpat(int epat) {
        Epat = epat;
    }

    public double getEcost() {
        return Ecost;
    }

    public void setEcost(double ecost) {
        Ecost = ecost;
    }

    public double getEnergy(int id) {
        return Energy[id];
    }

    public void setEnergy(int id, double energy) {
        Energy[id]=energy;
    }

}