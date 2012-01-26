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

package org.addition.epanet.msx.Solvers;


public interface JacobianInterface {
    public static enum Operation{
        PIPES_DC_DT_CONCENTRATIONS,
        TANKS_DC_DT_CONCENTRATIONS,
        PIPES_EQUIL,
        TANKS_EQUIL,
    }

    public void solve(double t, double[] y, int n, double[] f, int off,Operation op);
}
