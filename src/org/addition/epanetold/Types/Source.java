package org.addition.epanetold.Types;

public class Source
{
    private double C0;
    private int pattern;
    private double Smass;
    private EnumVariables.SourceType type;

    public double getC0() {

        return C0;
    }

    public void setC0(double c0) {
        C0 = c0;
    }

    public int getPattern() {
        return pattern;
    }

    public void setPattern(int pattern) {
        this.pattern = pattern;
    }

    public double getSmass() {
        return Smass;
    }

    public void setSmass(double sMass) {
        this.Smass = sMass;
    }

    public EnumVariables.SourceType getType() {
        return type;
    }

    public void setType(EnumVariables.SourceType type) {
        this.type = type;
    }
}