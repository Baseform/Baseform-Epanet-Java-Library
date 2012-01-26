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

package org.addition.epanet.msx.Structures;

import org.addition.epanet.msx.Constants;
import org.addition.epanet.msx.EnumTypes.*;

// Chemical species object
public class Species {
    String          id;                 // name
    String          units;              // mass units code [MAXUNITS]
    double          aTol;               // absolute tolerance
    double          rTol;               // relative tolerance
    SpeciesType     type;               // BULK or WALL
    ExpressionType  pipeExprType;       // type of pipe chemistry
    ExpressionType  tankExprType;       // type of tank chemistry
    int             precision;          // reporting precision
    char            rpt;                // reporting flag
    MathExpr        pipeExpr;           // pipe chemistry expression
    MathExpr        tankExpr;           // tank chemistry expression

    public Species() {
        id="";
        units="";
        pipeExpr     = null;
        tankExpr     = null;
        pipeExprType = ExpressionType.NO_EXPR;
        tankExprType = ExpressionType.NO_EXPR;
        precision    = 2;
        rpt = 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
        if(this.units.length()> Constants.MAXUNITS)
            this.units = this.units.substring(0,Constants.MAXUNITS);
    }

    public double getaTol() {
        return aTol;
    }

    public void setaTol(double aTol) {
        this.aTol = aTol;
    }

    public double getrTol() {
        return rTol;
    }

    public void setrTol(double rTol) {
        this.rTol = rTol;
    }

    public SpeciesType getType() {
        return type;
    }

    public void setType(SpeciesType type) {
        this.type = type;
    }

    public ExpressionType getPipeExprType() {
        return pipeExprType;
    }

    public void setPipeExprType(ExpressionType pipeExprType) {
        this.pipeExprType = pipeExprType;
    }

    public ExpressionType getTankExprType() {
        return tankExprType;
    }

    public void setTankExprType(ExpressionType tankExprType) {
        this.tankExprType = tankExprType;
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public char getRpt() {
        return rpt;
    }

    public void setRpt(char rpt) {
        this.rpt = rpt;
    }

    public MathExpr getPipeExpr() {
        return pipeExpr;
    }

    public void setPipeExpr(MathExpr pipeExpr) {
        this.pipeExpr = pipeExpr;
    }

    public MathExpr getTankExpr() {
        return tankExpr;
    }

    public void setTankExpr(MathExpr tankExpr) {
        this.tankExpr = tankExpr;
    }
}
