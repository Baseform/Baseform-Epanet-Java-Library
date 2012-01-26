package org.addition.epanetold.Types;

import java.util.List;

/* Element of temp list for Pattern & Curve data */
public class Tmplist {
    private int         i;
    private String      ID;
    private List<Float> X;

    public Tmplist(int i, String ID) {
        this.i = i;
        this.ID = ID;
    }

    private List<Float> Y;

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    public String getID() {
        return ID;
    }

    public void setId(String ID) {
        this.ID = ID;
    }

    public List<Float> getX() {
        return X;
    }

    public void setX(List<Float> x) {
        X = x;
    }

    public List<Float> getY() {
        return Y;
    }

    public void setY(List<Float> y) {
        Y = y;
    }
}