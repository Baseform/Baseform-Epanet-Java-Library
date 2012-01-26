package org.addition.epanetold;

import org.addition.epanetold.Types.AdjListItem;
import org.addition.epanetold.Types.Link;
import org.addition.epanetold.Types.EnumVariables.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public strictfp class SparseMatrix {

    private Epanet epanet;
    private Network net;

    int coeffsCount = 0;

    List<List<AdjListItem>> Adjlist;
    int [] Order;
    int [] Row;
    int [] Ndx;
    int [] Degree;

    SparseMatrix(Epanet epanet){
        this.epanet=epanet;
    }

    public void loadDependencies(){
        net = epanet.getNetwork();
    }

    public int getCoeffsCount() {
        return coeffsCount;
    }

    public void setCoeffsCount(int coeffsCount) {
        this.coeffsCount = coeffsCount;
    }

    // Creates sparse representation of coeff. matrix.   
    public int createSparse(){
        // Allocate data structures
        allocate();
        // Build node-link adjacency lists with parallel links removed.
        buildlists(true);
        // Remove parallel links
        xparalinks();
        // Find degree of each junction
        countdegree();

        coeffsCount = net.getMaxLinks();

        // Re-order nodes to minimize number of non-zero coeffs
        // in factorized solution matrix. At same time, adjacency
        // list is updated with links representing non-zero coeffs. 
        reordernodes();

        // Sort row indexes in NZSUB to optimize linsolve()
        storesparse(net.getSections(SectType._JUNCTIONS));

        //  Free memory used for adjacency lists and sort row indexes in NZSUB to optimize linsolve()
        freelists();

        ordersparse(net.getSections(SectType._JUNCTIONS));

        // Re-build adjacency lists without removing parallel links for use in future connectivity checking. 
        buildlists(false);

        return 0;
    }

    // frees memory used for nodal adjacency lists
    void  freelists()
    {
       int   i;

       for (i=0; i<=net.getMaxNodes(); i++)
       {
           Adjlist.get(i).clear();
       }
    }

    // Allocates memory for indexing the solution matrix.
    private void allocate(){
        Adjlist = new ArrayList<List<AdjListItem>>();
        for(int i = 0;i<=net.getMaxNodes();i++)
            Adjlist.add(new ArrayList<AdjListItem>());

        Order = new int [net.getMaxNodes()+1];
        Row = new int [net.getMaxNodes()+1];
        Ndx = new int [net.getMaxLinks()+1];
        Degree = new int [net.getMaxNodes() +1];
    }

    // Builds linked list of links adjacent to each node.
    private void  buildlists(boolean paraflag)
    {
        int    i,j,k;
        boolean pmark = false;
        AdjListItem  alink;

        for (k=1; k<=net.getMaxLinks(); k++)
        {
            Link lk = net.getLink(k);
            i = lk.getN1();
            j = lk.getN2();
            if (paraflag)
                pmark = paralink(i,j,k);

            // Include link in start node i's list
            alink = new AdjListItem(!pmark?j:0,k);
//            if (!pmark) alink.setNode(j);
//            else alink.setNode(0);
//            alink.setLink(k);

            Adjlist.get(i).add(0,alink);

            // Include link in end node j's list
            alink = new AdjListItem(!pmark?i:0,k);
//            if (!pmark)alink.setNode(i);
//            else alink.setNode(0);
//            alink.setLink(k);

            Adjlist.get(j).add(0,alink);
        }
    }


    // Checks for parallel links between nodes i and j.
    private boolean paralink(int i, int j, int k)
    {
        for(AdjListItem alink : Adjlist.get(i)){
            if (alink.getNode() == j){
                Ndx[k] = alink.getLink();
                return true;
            }
        }
        Ndx[k] = k;
        return false;
    }

    // Removes parallel links from nodal adjacency lists.
    void  xparalinks()
    {
        for (int i=1; i<=net.getMaxNodes(); i++)
        {
            AdjListItem alink=null;
            Iterator<AdjListItem> it = Adjlist.get(i).iterator();
            while( it.hasNext()){
                alink = it.next();
                if(alink.getNode() == 0)
                    it.remove();
            }
        }
    }

    // Counts number of nodes directly connected to each node 
    void  countdegree(){
        for (int i=1; i<=net.getSections(SectType._JUNCTIONS); i++){
            for(AdjListItem li : Adjlist.get(i))
                if (li.getNode() > 0) Degree[i]++;
        }
    }

    // Re-orders nodes to minimize # of non-zeros that will appear in factorized solution matrix
    void reordernodes()
    {
        int k, knode, m, n;

        for (k=1; k<=net.getMaxNodes(); k++){
            Row[k] = k;
            Order[k] = k;
        }

        n = net.getSections(SectType._JUNCTIONS);

        for (k=1; k<=n; k++)
        {
            m = mindegree(k,n);
            knode = Order[m];
            growlist(knode);
            Order[m] = Order[k];
            Order[k] = knode;
            Degree[knode] = 0;
        }

        for (k=1; k<=n; k++)
            Row[Order[k]] = k;
    }

    // Finds active node with fewest direct connections
    int  mindegree(int k, int n)
    {
        int i, m;
        int min = n,
                imin = n;

        for (i=k; i<=n; i++)
        {
            m = Degree[Order[i]];
            if (m < min)
            {
                min = m;
                imin = i;
            }
        }
        return(imin);
    }

    // Creates new entries in knode's adjacency list for all unlinked pairs of active nodes that are adjacent to knode.
    void growlist(int knode)
    {
        for(int i = 0;i<Adjlist.get(knode).size();i++)
        {
            AdjListItem alink = Adjlist.get(knode).get(i);
            int node = alink.getNode();
            if (Degree[node] > 0)
            {
                Degree[node]--;
                newlink(Adjlist.get(knode),i);
            }
        }
    }

    // Links end of current adjacent link to end nodes of all links that follow it on adjacency list.
    int  newlink(List<AdjListItem> list,int id){
        int   inode, jnode;
        AdjListItem blink = null;

        inode = list.get(id).getNode();
        for(int i = id+1;i<list.size();i++)
        {
            blink=list.get(i);
            jnode = blink.getNode();

            if (Degree[jnode] > 0)
            {
                if (!linked(inode,jnode))
                {
                    coeffsCount++;
                    addlink(inode,jnode,coeffsCount);
                    addlink(jnode,inode,coeffsCount);
                    Degree[inode]++;
                    Degree[jnode]++;
                }
            }
        }
        return(1);
    }

    // Checks if nodes i and j are already linked.
    boolean  linked(int i, int j){
        for(AdjListItem alink : Adjlist.get(i))
            if(alink.getNode() == j)
                return true;
        return false;
    }


    void  addlink(int i, int j, int n)
    {
        AdjListItem alink = new AdjListItem(j,n);
//        alink.setNode(j);
//        alink.setLink(n);
        Adjlist.get(i).add(0,alink);
    }


    int [] XLNZ;
    int [] NZSUB;
    int [] LNZ;

    int  storesparse(int n)
    {

        int   i, ii, j, k, l, m;
        int   errcode = 0;

        XLNZ  = new int [n+2];
        NZSUB = new int [coeffsCount+2];
        LNZ   = new int [coeffsCount+2];

        k = 0;
        XLNZ[1] = 1;
        for (i=1; i<=n; i++)
        {
            m = 0;
            ii = Order[i];

            for(AdjListItem alink: Adjlist.get(ii))
            {
                j = Row[alink.getNode()];
                l = alink.getLink();
                if (j > i && j <= n)
                {
                    m++;
                    k++;
                    NZSUB[k] = j;
                    LNZ[k] = l;
                }
            }
            XLNZ[i+1] = XLNZ[i] + m;
        }
        return(errcode);
    }


    int  ordersparse(int n)
    {
        int  i, k;

        int [] xlnzt  = new int[n+2];
        int [] nzsubt = new int[coeffsCount+2];
        int [] lnzt   = new int[coeffsCount+2];
        int [] nzt    = new int[n+2];


        for (i=1; i<=n; i++) nzt[i] = 0;
        for (i=1; i<=n; i++)
        {
            for (k=XLNZ[i]; k<XLNZ[i+1]; k++) nzt[NZSUB[k]]++;
        }
        xlnzt[1] = 1;
        for (i=1; i<=n; i++) xlnzt[i+1] = xlnzt[i] + nzt[i];

        transpose(n,XLNZ,NZSUB,LNZ,xlnzt,nzsubt,lnzt,nzt);
        transpose(n,xlnzt,nzsubt,lnzt,XLNZ,NZSUB,LNZ,nzt);

        return(0);
    }


    void  transpose(int n, int [] il, int [] jl, int [] xl, int [] ilt, int [] jlt,
                    int [] xlt, int [] nzt){
        int  i, j, k, kk;

        for (i=1; i<=n; i++) nzt[i] = 0;
        for (i=1; i<=n; i++)
        {
            for (k=il[i]; k<il[i+1]; k++)
            {
                j = jl[k];
                kk = ilt[j] + nzt[j];
                jlt[kk] = i;
                xlt[kk] = xl[k];
                nzt[j]++;
            }
        }
    }


    public int  linsolve(int n, double [] Aii, double [] Aij, double []B)
    {
        int    i, istop, istrt, isub, j, k, kfirst, newk;
        int    errcode = 0;
        double bj, diagj=0.0d, ljk;


        double [] temp = new double[n+1];
        int [] link = new int[n+1];
        int [] first = new int[n+1];

        for (j=1; j<=n; j++)
        {
            diagj = 0.0;
            newk = link[j];
            k = newk;
            while (k != 0)
            {

                newk = link[k];
                kfirst = first[k];
                ljk = Aij[LNZ[kfirst]];
                diagj += ljk*ljk;
                istrt = kfirst + 1;
                istop = XLNZ[k+1] - 1;
                if (istop >= istrt)
                {

                    first[k] = istrt;
                    isub = NZSUB[istrt];
                    link[k] = link[isub];
                    link[isub] = k;

                    for (i=istrt; i<=istop; i++)
                    {
                        isub = NZSUB[i];
                        temp[isub] += Aij[LNZ[i]]*ljk;
                    }
                }
                k = newk;
            }

            diagj = Aii[j] - diagj;
            if (diagj <= 0.0)
            {
                errcode = j;
                break;
            }
            diagj = Math.sqrt(diagj);
            Aii[j] = diagj;
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
                    bj = (Aij[LNZ[i]] - temp[isub])/diagj;
                    Aij[LNZ[i]] = bj;
                    temp[isub] = 0.0;
                }
            }
        }

        if (diagj > 0.0d){
            for (j=1; j<=n; j++)
            {
                bj = B[j]/Aii[j];
                B[j] = bj;
                istrt = XLNZ[j];
                istop = XLNZ[j+1] - 1;
                if (istop >= istrt)
                {
                    for (i=istrt; i<=istop; i++)
                    {
                        isub = NZSUB[i];
                        B[isub] -= Aij[LNZ[i]]*bj;
                    }
                }
            }


            for (j=n; j>=1; j--)
            {
                bj = B[j];
                istrt = XLNZ[j];
                istop = XLNZ[j+1] - 1;
                if (istop >= istrt)
                {
                    for (i=istrt; i<=istop; i++)
                    {
                        isub = NZSUB[i];
                        bj -= Aij[LNZ[i]]*B[isub];
                    }
                }
                B[j] = bj/Aii[j];
            }
        }

        return 0;
    }

}
