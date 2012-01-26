package org.addition.epanetold.Types;


/* TANK OBJECT */
public class Tank
{
    private int Node;
    private double A;
    private double HMin;
    private double HMax;
    private double H0;
    private double Vmin;
    private double Vmax;
    private double V0;
    private double Kb;
    private double V;
    private double C;
    private int pattern;
    private int Vcurve;
    private EnumVariables.MixType MixModel;
    private double  V1max;

    public int getNode() {
        return Node;
    }

    public void setNode(int node) {
        Node = node;
    }

    public double getArea() {
        return A;
    }

    public void setArea(double a) {
        A = a;
    }

    public double getHMin() {
        return HMin;
    }

    public void setHMin(double HMin) {
        this.HMin = HMin;
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

    public double getVmin() {
        return Vmin;
    }

    public void setVmin(double vmin) {
        Vmin = vmin;
    }

    public double getVmax() {
        return Vmax;
    }

    public void setVmax(double vmax) {
        Vmax = vmax;
    }

    public double getV0() {
        return V0;
    }

    public void setV0(double v0) {
        V0 = v0;
    }

    public double getKb() {
        return Kb;
    }

    public void setKb(double kb) {
        Kb = kb;
    }

    public double getVolume() {
        return V;
    }

    public void setVolume(double v) {
        V = v;
    }

    public double getConcentration() {
        return C;
    }

    public void setConcentration(double c) {
        C = c;
    }

    public int getPattern() {
        return pattern;
    }

    public void setPattern(int pattern) {
        this.pattern = pattern;
    }

    public int getVcurve() {
        return Vcurve;
    }

    public void setVcurve(int vcurve) {
        Vcurve = vcurve;
    }

    public EnumVariables.MixType getMixModel() {
        return MixModel;
    }

    public void setMixModel(EnumVariables.MixType mixModel) {
        MixModel = mixModel;
    }

    public double getV1max() {
        return V1max;
    }

    public void setV1max(double vLmax) {
        this.V1max = vLmax;
    }
}