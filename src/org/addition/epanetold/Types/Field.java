package org.addition.epanetold.Types;

/* FIELD OBJECT of report table */
public class Field {
       String   name;
       String   units;
       boolean  enabled;
       int      precision;
       double   rptLim[]={0d,0d};

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public double getRptLim(int id) {
        return rptLim[id];
    }

    public void setRptLim(int id, double rptLim) {
        this.rptLim[id] = rptLim;
    }
}
