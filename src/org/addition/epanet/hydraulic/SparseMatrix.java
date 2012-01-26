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

package org.addition.epanet.hydraulic;

import org.addition.epanet.hydraulic.structures.SimulationLink;
import org.addition.epanet.hydraulic.structures.SimulationNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Linear system solving support class.
 */
public class SparseMatrix {

    /**
     * Adjacent item
     */
    private static class AdjItem {
        private final int node;
        private final int link;

        public AdjItem(int node, int link) {
            this.node = node;
            this.link = link;
        }

        public int getNode() {
            return node;
        }

        public int getLink() {
            return link;
        }
    }

    /**
     * Number of coefficients(number of links)
     */
    private int coeffsCount;

    /**
     * Node-to-row of A.
     */
    private final int[] Order;
    /**
     * Row-to-node of A
     */
    private final int[] Row;
    /**
     * Index of link's coeff. in Aij
     */
    private final int[] Ndx;
    /**
     * Number of links adjacent to each node
     */
    private final int[] Degree;


    public int getOrder(int id) {
        return Order[id + 1] - 1;
    }

    public int getRow(int id) {
        return Row[id + 1] - 1;
    }

    public int getNdx(int id) {
        return Ndx[id + 1] - 1;
    }

    public int getCoeffsCount() {
        return coeffsCount;
    }

    /**
     * Creates sparse representation of coeff. matrix.
     */
    public SparseMatrix(List<SimulationNode> nodes, List<SimulationLink> links, int juncs) {

        Order = new int[nodes.size() + 1];
        Row = new int[nodes.size() + 1];
        Ndx = new int[links.size() + 1];
        Degree = new int[nodes.size() + 1];

        // For each node, builds an adjacency list that identifies all links connected to the node (see buildlists())
        List<List<AdjItem>> adjList = new ArrayList<List<AdjItem>>();
        for (int i = 0; i <= nodes.size(); i++)     // <= is necessary due to the array start index being 1
            adjList.add(new ArrayList<AdjItem>());

        buildlists(adjList,nodes, links, true);     // Build node-link adjacency lists with parallel links removed.
        xparalinks(adjList);           // Remove parallel links //,nodes.size()
        countdegree(adjList,juncs);                 // Find degree of each junction

        coeffsCount = links.size();

        // Re-order nodes to minimize number of non-zero coeffs
        // in factorized solution matrix. At same time, adjacency
        // list is updated with links representing non-zero coeffs.
        reordernodes(adjList, juncs);

        storesparse(adjList,juncs);             // Sort row indexes in NZSUB to optimize linsolve()
        ordersparse(juncs);
        buildlists(adjList,nodes, links, false); // Re-build adjacency lists without removing parallel links for use in future connectivity checking.
    }


    /**
     * Builds linked list of links adjacent to each node.
     * @param adjlist Nodes adjacency list.
     * @param nodes Collecion of hydraulic simulation nodes.
     * @param links Collection of hydraulic simulation links.
     * @param paraflag Remove parallel links.
     */
    private void buildlists( List<List<AdjItem>> adjlist,List<SimulationNode> nodes, List<SimulationLink> links, boolean paraflag) {

        boolean pmark = false;

        for(SimulationLink link : links){
            int k = link.getIndex() + 1;
            int i = link.getFirst().getIndex() + 1;
            int j = link.getSecond().getIndex() + 1;

            if (paraflag)
                pmark = paralink(adjlist,i, j, k);

            // Include link in start node i's list
            AdjItem alink = new AdjItem(!pmark ? j : 0, k);

            adjlist.get(i).add(0, alink);

            // Include link in end node j's list
            alink = new AdjItem(!pmark ? i : 0, k);

            adjlist.get(j).add(0, alink);
        }
    }


    /**
     * Checks for parallel links between nodes i and j.
     * @param adjlist Nodes adjacency list.
     * @param i First node index.
     * @param j Second node index.
     * @param k Link index.
     */
    private boolean paralink(List<List<AdjItem>> adjlist,int i, int j, int k) {
        for (AdjItem alink : adjlist.get(i)) {
            if (alink.getNode() == j) {
                Ndx[k] = alink.getLink();
                return true;
            }
        }
        Ndx[k] = k;
        return false;
    }

    /**
     * Removes parallel links from nodal adjacency lists.
     * @param adjlist Nodes adjacency list.
     */
    private void xparalinks(List<List<AdjItem>> adjlist) {
        for (int i = 1; i < adjlist.size(); i++) {
            Iterator<AdjItem> it = adjlist.get(i).iterator();
            while (it.hasNext()) {
                AdjItem alink = it.next();
                if (alink.getNode() == 0)
                    it.remove();
            }
        }
    }

    /**
     * Counts number of nodes directly connected to each node.
     * @param adjlist Nodes adjacency list.
     * @param Njuncs Number of junctions.
     */
    private void countdegree(List<List<AdjItem>> adjlist,int Njuncs) {

        Arrays.fill(Degree, 0);

        for (int i = 1; i <= Njuncs; i++) {
            for (AdjItem li : adjlist.get(i))
                if (li.getNode() > 0) Degree[i]++;
        }
    }

    /**
     * Re-orders nodes to minimize # of non-zeros that will appear in factorized solution matrix
     * @param adjlist Nodes adjacency list.
     * @param Njuncs Number of junctions.
      */
    private void reordernodes(List<List<AdjItem>> adjlist, int Njuncs) {
        int k, knode, m, n;

        for (k = 1; k < adjlist.size(); k++) {
            Row[k] = k;
            Order[k] = k;
        }

        n = Njuncs;

        for (k = 1; k <= n; k++) {
            m = mindegree(k, n);
            knode = Order[m];
            growlist(adjlist,knode);
            Order[m] = Order[k];
            Order[k] = knode;
            Degree[knode] = 0;
        }

        for (k = 1; k <= n; k++)
            Row[Order[k]] = k;
    }

    /**
     * Finds active node with fewest direct connections
     * @param k Junction id.
     * @param n Number of junctions
     * @return Node id.
     */
    private int mindegree(int k, int n) {
        int i, m;
        int min = n,
                imin = n;

        for (i = k; i <= n; i++) {
            m = Degree[Order[i]];
            if (m < min) {
                min = m;
                imin = i;
            }
        }
        return (imin);
    }

    /**
     * Creates new entries in knode's adjacency list for all unlinked pairs of active nodes that are adjacent to knode.
     * @param adjlist Nodes adjacency list.
     * @param knode Node id.
      */
    private void growlist(List<List<AdjItem>> adjlist,int knode) {
        for (int i = 0; i < adjlist.get(knode).size(); i++) {
            AdjItem alink = adjlist.get(knode).get(i);
            int node = alink.getNode();
            if (Degree[node] > 0) {
                Degree[node]--;
                newlink(adjlist,adjlist.get(knode), i);
            }
        }
    }

    /**
     * Links end of current adjacent link to end nodes of all links that follow it on adjacency list.
     * @param adjList Nodes adjacency list.
     * @param list Adjacent links
     * @param id Link id
     */
    private void newlink(List<List<AdjItem>> adjList,List<AdjItem> list, int id) {
        int inode, jnode;

        inode = list.get(id).getNode();
        for (int i = id + 1; i < list.size(); i++) {
            AdjItem blink = list.get(i);
            jnode = blink.getNode();

            if (Degree[jnode] > 0) {
                if (!linked(adjList,inode, jnode)) {
                    coeffsCount++;
                    addlink(adjList,inode, jnode, coeffsCount);
                    addlink(adjList,jnode, inode, coeffsCount);
                    Degree[inode]++;
                    Degree[jnode]++;
                }
            }
        }
    }

    /**
     * Checks if nodes i and j are already linked.
     * @param adjlist Nodes adjacency list.
     * @param i Node i index.
     * @param j Node j index.
     * @return True if linked.
     */
    private boolean linked(List<List<AdjItem>> adjlist,int i, int j) {
        for (AdjItem alink : adjlist.get(i))
            if (alink.getNode() == j)
                return true;
        return false;
    }

    /**
     * Augments node i's adjacency list with node j.
     * @param adjList
     * @param i
     * @param j
     * @param n
     */
    private void addlink(List<List<AdjItem>> adjList,int i, int j, int n) {
        AdjItem alink = new AdjItem(j, n);
        adjList.get(i).add(0, alink);
    }

    /**
     * Start position of each column in NZSUB.
     */
    private int[] XLNZ;
    /**
     * Row index of each coeff. in each column
     */
    private int[] NZSUB;
    /**
     * Position of each coeff. in Aij array
     */
    private int[] LNZ;

    /**
     * Stores row indexes of non-zeros of each column of lower triangular portion of factorized matrix
     * @param adjlist Nodes adjacency list.
     * @param n junctions count.
     */
    private void storesparse(List<List<AdjItem>> adjlist,int n)
    {
        XLNZ = new int[n + 2];
        NZSUB = new int[coeffsCount + 2];
        LNZ = new int[coeffsCount + 2];

        int k = 0;
        XLNZ[1] = 1;
        for (int i = 1; i <= n; i++) {
            int m = 0;
            int ii = Order[i];

            for (AdjItem alink : adjlist.get(ii)) {
                int j = Row[alink.getNode()];
                int l = alink.getLink();
                if (j > i && j <= n) {
                    m++;
                    k++;
                    NZSUB[k] = j;
                    LNZ[k] = l;
                }
            }
            XLNZ[i + 1] = XLNZ[i] + m;
        }
    }

    /**
     * Puts row indexes in ascending order in NZSUB.
     * @param n Number of junctions.
     */
    private void ordersparse(int n) {
        int i, k;

        int[] xlnzt = new int[n + 2];
        int[] nzsubt = new int[coeffsCount + 2];
        int[] lnzt = new int[coeffsCount + 2];
        int[] nzt = new int[n + 2];

        for (i = 1; i <= n; i++) {
            for (k = XLNZ[i]; k < XLNZ[i + 1]; k++)
                nzt[NZSUB[k]]++;
        }
        xlnzt[1] = 1;
        for (i = 1; i <= n; i++)
            xlnzt[i + 1] = xlnzt[i] + nzt[i];

        transpose(n, XLNZ, NZSUB, LNZ, xlnzt, nzsubt, lnzt, nzt);
        transpose(n, xlnzt, nzsubt, lnzt, XLNZ, NZSUB, LNZ, nzt);

    }

    /**
     * Determines sparse storage scheme for transpose of a matrix.
     * @param n Number of junctions.
     * @param il sparse storage scheme for original matrix.
     * @param jl sparse storage scheme for original matrix.
     * @param xl sparse storage scheme for original matrix.
     * @param ilt sparse storage scheme for transposed matrix.
     * @param jlt sparse storage scheme for transposed matrix.
     * @param xlt sparse storage scheme for transposed matrix.
     * @param nzt work array.
     */
    private void transpose(int n, int[] il, int[] jl, int[] xl, int[] ilt, int[] jlt,
                           int[] xlt, int[] nzt) {
        int i, j, k, kk;

        for (i = 1; i <= n; i++)
            nzt[i] = 0;

        for (i = 1; i <= n; i++) {
            for (k = il[i]; k < il[i + 1]; k++) {
                j = jl[k];
                kk = ilt[j] + nzt[j];
                jlt[kk] = i;
                xlt[kk] = xl[k];
                nzt[j]++;
            }
        }
    }

    /**
     * Solves sparse symmetric system of linear equations using Cholesky factorization.
     * @param n Number of equations.
     * @param Aii Diagonal entries of solution matrix.
     * @param Aij Non-zero off-diagonal entries of matrix.
     * @param B Right hand side coeffs, after solving it's also used as the solution vector.
     * @return 0 if solution found, or index of equation causing system to be ill-conditioned.
     */
    public int linsolve(int n, double [] Aii, double [] Aij, double [] B)
    {
        int    i, istop, istrt, isub, j, k, kfirst, newk;
        double bj, diagj, ljk;

        double [] temp = new double[n+1];
        int [] link = new int[n+1];
        int [] first = new int[n+1];

        // Begin numerical factorization of matrix A into L
        // Compute column L(*,j) for j = 1,...n
        for (j=1; j<=n; j++)
        {
            // For each column L(*,k) that affects L(*,j):
            diagj = 0.0;
            newk = link[j];
            k = newk;
            while (k != 0)
            {

                // Outer product modification of L(*,j) by
                // L(*,k) starting at first[k] of L(*,k).
                newk = link[k];
                kfirst = first[k];
                ljk = Aij[LNZ[kfirst]-1];
                diagj += ljk*ljk;
                istrt = kfirst + 1;
                istop = XLNZ[k+1] - 1;
                if (istop >= istrt)
                {

                    // Before modification, update vectors 'first'
                    // and 'link' for future modification steps.
                    first[k] = istrt;
                    isub = NZSUB[istrt];
                    link[k] = link[isub];
                    link[isub] = k;

                    // The actual mod is saved in vector 'temp'.
                    for (i=istrt; i<=istop; i++)
                    {
                        isub = NZSUB[i];
                        temp[isub] += Aij[LNZ[i]-1]*ljk;
                    }
                }
                k = newk;
            }

            // Apply the modifications accumulated
            // in 'temp' to column L(*,j).
            diagj = Aii[j-1] - diagj;
            if (diagj <= 0.0)        // Check for ill-conditioning
            {
                return j;
            }
            diagj = Math.sqrt(diagj);
            Aii[j-1] = diagj;
            istrt = XLNZ[j];
            istop = XLNZ[j+1] - 1;
            if (istop >= istrt)
            {
                first[j] = istrt;
                isub = NZSUB[istrt];
                link[j] = link[isub];
                link[isub] = j;
                for (i=istrt; i<=istop; i++)
                {
                    isub = NZSUB[i];
                    bj = (Aij[LNZ[i]-1] - temp[isub])/diagj;
                    Aij[LNZ[i]-1] = bj;
                    temp[isub] = 0.0;
                }
            }
        }

        // Foward substitution
        for (j=1; j<=n; j++)
        {
            bj = B[j-1]/Aii[j-1];
            B[j-1] = bj;
            istrt = XLNZ[j];
            istop = XLNZ[j+1] - 1;
            if (istop >= istrt)
            {
                for (i=istrt; i<=istop; i++)
                {
                    isub = NZSUB[i];
                    B[isub-1] -= Aij[LNZ[i]-1]*bj;
                }
            }
        }

        // Backward substitution
        for (j=n; j>=1; j--)
        {
            bj = B[j-1];
            istrt = XLNZ[j];
            istop = XLNZ[j+1] - 1;
            if (istop >= istrt)
            {
                for (i=istrt; i<=istop; i++)
                {
                    isub = NZSUB[i];
                    bj -= Aij[LNZ[i]-1]*B[isub-1];
                }
            }
            B[j-1] = bj/Aii[j-1];
        }

        return(0);
    }

}


