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

package org.addition.epanet.hydraulic.structures;


import java.util.Arrays;

public class LSVariables {
    private double[] nodalInflows;         // Epanet 'X[n]' variable
    private double[] matrixOffDiagonal;    // Epante Aij[n] variable
    private double[] matrixDiagonal;       // Epanet Aii[n] variable
    private double[] rightHandSideCoeffs;  // Epanet F[n] variable

    public void clear() {
        Arrays.fill(nodalInflows, 0);
        Arrays.fill(matrixOffDiagonal, 0);
        Arrays.fill(matrixDiagonal, 0);
        Arrays.fill(rightHandSideCoeffs, 0);
    }

    public LSVariables(int nodes, int coeffs) {
        nodalInflows = new double[nodes];
        matrixDiagonal = new double[nodes];
        matrixOffDiagonal = new double[coeffs];
        rightHandSideCoeffs = new double[nodes];
        clear();
    }

    public void addRHSCoeff(int id, double value) {
        rightHandSideCoeffs[id] += value;
    }

    public double getRHSCoeff(int id) {
        return rightHandSideCoeffs[id];
    }


    public void addNodalInFlow(int id, double value) {
        nodalInflows[id] += value;
    }

    public double getNodalInFlow(int id) {
        return nodalInflows[id];
    }


    public void addNodalInFlow(SimulationNode id, double value) {
        nodalInflows[id.getIndex()] += value;
    }

    public double getNodalInFlow(SimulationNode id) {
        return nodalInflows[id.getIndex()];
    }


    public void addAii(int id, double value) {
        matrixDiagonal[id] += value;
    }

    public double getAii(int id) {
        return matrixDiagonal[id];
    }

    public void addAij(int id, double value) {
        matrixOffDiagonal[id] += value;
    }

    public double getAij(int id) {
        return matrixOffDiagonal[id];
    }

    public double[] getAiiVector() {
        return matrixDiagonal;
    }

    public double[] getAijVector() {
        return matrixOffDiagonal;
    }

    public double[] getRHSCoeffs() {
        return rightHandSideCoeffs;
    }
}

/**
 * Linear System Variables that feed the sparse matrix for each iteration.

 public class LSVariablesSim {
 private double [] nodalInflows;         // Epanet 'X[n]' variable
 private double [] matrixOffDiagonal;    // Epante Aij[n] variable
 private double [] matrixDiagonal;       // Epanet Aii[n] variable
 private double [] rightHandSideCoeffs;  // Epanet F[n] variable

 public void clear(){
 Arrays.fill(nodalInflows,0);
 Arrays.fill(matrixOffDiagonal,0);
 Arrays.fill(matrixDiagonal,0);
 Arrays.fill(rightHandSideCoeffs,0);
 }

 public LSVariables(int nodes, int coeffs) {
 nodalInflows = new double[nodes];
 matrixDiagonal = new double[nodes];
 matrixOffDiagonal = new double[coeffs];
 rightHandSideCoeffs = new double[nodes];
 clear();
 }

 public void addRHSCoeff(int id, double value){
 synchronized (rightHandSideCoeffs){
 rightHandSideCoeffs[id]+=value;
 }
 }

 public double getRHSCoeff(int id){
 synchronized (rightHandSideCoeffs){
 return rightHandSideCoeffs[id];
 }
 }


 public void addNodalInFlow(int id, double value){
 synchronized (nodalInflows){
 nodalInflows[id]+=value;
 }
 }

 public double getNodalInFlow(int id){
 synchronized (nodalInflows){
 return nodalInflows[id];
 }
 }


 public void addNodalInFlow(SimulationNode id, double value){
 synchronized (nodalInflows){
 nodalInflows[id.getIndex()]+=value;
 }
 }

 public double getNodalInFlow(SimulationNode id){
 synchronized (nodalInflows){
 return nodalInflows[id.getIndex()];
 }
 }


 public void addAii(int id, double value){
 synchronized (matrixDiagonal){
 matrixDiagonal[id]+=value;
 }
 }

 public double getAii(int id){
 synchronized (matrixDiagonal){
 return matrixDiagonal[id];
 }
 }

 public void addAij(int id, double value){
 synchronized (matrixDiagonal){
 matrixOffDiagonal[id]+=value;
 }
 }

 public double getAij(int id){
 synchronized (matrixDiagonal){
 return matrixOffDiagonal[id];
 }
 }

 public double [] getAiiVector(){
 return matrixDiagonal;
 }

 public double [] getAijVector(){
 return matrixOffDiagonal;
 }

 public double [] getRHSCoeffs(){
 return rightHandSideCoeffs;
 }
 }
 */