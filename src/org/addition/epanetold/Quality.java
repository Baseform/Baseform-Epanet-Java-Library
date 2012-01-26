package org.addition.epanetold;


import org.addition.epanetold.Types.EnumVariables.*;
import org.addition.epanetold.Types.Link;
import org.addition.epanetold.Types.Source;
import org.addition.epanetold.Types.Tank;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

public class Quality {


    public int getNperiods() {
        return Nperiods;
    }

    public long getQtime() {
        return Qtime;
    }

    // pipe segment record
    public class Seg{
        double  v;
        double  c;
    }

    Network net;
    Hydraulic hyd;
    Epanet epanet;
    Output out;

    List<Seg>[] FirstSeg;       // First (downstream) segment in each pipe
    char []     FlowDir;        // Flow direction for each pipe
    double []   VolIn;          // Total volume inflow to node
    double []   MassIn;         // Total mass inflow to node
    double      Sc;             // Schmidt Number
    double      Bucf;           // Bulk reaction units conversion factor
    double      Tucf;           // Tank reaction units conversion factor
    int         Nperiods;
    boolean     Reactflag;      // Reaction indicator
    double      Wbulk;          // Avg. bulk reaction rate
    double      Wwall;          // Avg. wall reaction rate
    double      Wtank;          // Avg. tank reaction rate
    double      Wsource;        // Avg. mass inflow
    long        Qtime;          // Current quality time (sec)

    double [] R;                // Pipe reaction rate
    double [] C;                // Node actual quality
    double [] X;                // General purpose array

    Quality(Epanet epanet){
        this.epanet = epanet;
    }

    public void loadDependencies(){
        net = epanet.getNetwork();
        hyd = epanet.getHydraulicsSolver();
        out = epanet.getOutput();
    }


    //  opens WQ solver system
    void  openqual(){
        int n;

        X = new double[Math.max(net.getMaxNodes()+1,net.getMaxLinks()+1)];
        R = new double[net.getMaxLinks()+1];
        C = new double[net.getMaxNodes()+1];

        n = net.getMaxLinks()+net.getMaxTanks()+1;

        FirstSeg = new List[n];
        for(int i = 0;i<n;i++)
            FirstSeg[i] = new ArrayList<Seg>();

        FlowDir  = new char [n];

        n        = net.getMaxNodes()+1;
        VolIn    = new double[n];
        MassIn   = new double[n];
    }

    //  Local method to compute unit conversion factor for bulk reaction rates
    private double getucf(double order)
    {
        if (order < 0.0) order = 0.0;
        if (order == 1.0) return(1.0);
        else return(1.0/Math.pow(Constants.LperFT3,(order-1.0)));
    }

    // Re-Initializes WQ solver system
    void  initqual()
    {
        for (int i=1; i<=net.getMaxNodes(); i++)
            C[i] = net.getNode(i).getC0();

        for (int i=1; i<=net.getMaxTanks(); i++){
            net.getTank(i).setConcentration( net.getNode(net.getTank(i).getNode()).getC0());
            net.getTank(i).setVolume( net.getTank(i).getV0());
        }

        for (int i=1; i<=net.getMaxNodes(); i++)
            if (net.getNode(i).getSource() != null)
                net.getNode(i).getSource().setSmass(0.0);

        Bucf = 1.0;
        Tucf = 1.0;
        Reactflag = false;

        if (net.Qualflag != QualType.NONE)
        {
            if (net.Qualflag == QualType.TRACE)
                C[net.TraceNode] = 100.0;

            if (net.Diffus > 0.0)
                Sc = net.Viscos/net.Diffus;
            else
                Sc = 0.0;

            Bucf = getucf(net.BulkOrder);
            Tucf = getucf(net.TankOrder);

            Reactflag = setReactflag();
        }


        Wbulk = 0.0;
        Wwall = 0.0;
        Wtank = 0.0;
        Wsource = 0.0;

        hyd.Htime = 0;
        hyd.Rtime = net.Rstart;
        Qtime = 0;
        Nperiods = 0;
    }

    // Retrieves hydraulics for next hydraulic time step (at time t) and saves current results to file
    long [] runqual(DataInputStream stream)
    {
        long    t = Qtime;
        long    hydtime;
        long    hydstep;
        int     errcode = 0;

        if (Qtime == hyd.Htime)
        {
            long [] ret = gethyd(stream);
            errcode = (int)ret[0];
            hydtime = ret[1];
            hydstep = ret[2];
            hyd.Htime = hydtime + hydstep;
        }
        return( new long []{(int)errcode,t});
    }

    // Updates WQ conditions until next hydraulic solution occurs (after tstep secs.)
    long nextqual(DataInputStream in)
    {
        long    hydstep;
        long    tstep;

        hydstep = hyd.Htime - Qtime;

        if (net.Qualflag != QualType.NONE && hydstep > 0)
            transport(hydstep);

        tstep = hydstep;
        Qtime += hydstep;


       // if (epanetold.isSaveflag() && tstep == 0)   //TODO
        if( tstep == 0)
            out.savefinaloutput(in);
        return(tstep);
    }

    //updates WQ conditions over a single WQ time step
    long [] stepqual(DataInputStream stream,long tleft)
    {  long dt, hstep, tstep;
        int  errcode = 0;
        tstep = net.Qstep;
        do
        {
            dt = tstep;
            hstep = hyd.Htime - Qtime;
            if (hstep < dt)
            {
                dt = hstep;
                if (net.Qualflag != QualType.NONE) transport(dt);
                Qtime += dt;
                long [] ret = runqual(stream);
                errcode = (int)ret[0];
                Qtime = ret[1];
            }
            else
            {
                if (net.Qualflag != QualType.NONE)
                    transport(dt);
                Qtime += dt;
            }
            tstep -= dt;

        }  while (errcode!=0 && tstep > 0);
        tleft = net.Dur - Qtime;
        //if (errcode!=0 && epanetold.isSaveflag() && tleft == 0)
        //    errcode = savefinaloutput();
        return(new long []{errcode,tleft});
    }



    // Retrieves hydraulic solution and hydraulic time step for next hydraulic event
    long [] gethyd(DataInputStream stream)

    {
        Long hydtime = null;
        Long hydstep = null;
        int errcode = 0;


        if ( (hydtime = readhyd(stream)) == null) return(new long[]{307,0,0});          //TODO
        if ( (hydstep = readhydstep(stream)) == null) return new long[]{307,0,0};
        hyd.Htime = hydtime;


        //if (hyd.Htime >= hyd.Rtime)
        {
            //if (epanetold.isSaveflag()) //TODO
            {
                errcode = out.saveoutput();
                Nperiods++;
            }

            //hyd.Rtime += net.Rstep;
            hyd.Rtime += hyd.Htime;
        }


        if (net.Qualflag != QualType.NONE && Qtime < net.Dur)
        {
            if (Reactflag && net.Qualflag != QualType.AGE) ratecoeffs();

            if (Qtime == 0)
                initsegs();
            else
                reorientsegs();
        }
        return(new long[]{errcode,hydtime,hydstep});
    }

    //checks if reactive chemical being simulated
    boolean  setReactflag()

    {
        int  i;
        if      (net.Qualflag == QualType.TRACE) return(false);
        else if (net.Qualflag == QualType.AGE)   return(true);
        else
        {
            for (i=1; i<=net.getMaxLinks(); i++)
            {
                if (net.getLink(i).getType().ordinal() <= LinkType.PIPE.ordinal())
                {
                    if (net.getLink(i).getKb() != 0.0 || net.getLink(i).getKw() != 0.0) return(true);
                }
            }
            for (i=1; i<=net.getMaxTanks(); i++)
                if (net.getTank(i).getKb() != 0.0) return(true);
        }
        return(false);
    }

    //  Transports constituent mass through pipe network under a period of constant hydraulic conditions.
    void  transport(long tstep)

    {
        long   qtime, dt;

        //AllocSetPool(SegPool);                                                      //(2.00.11 - LR)
        qtime = 0;
        while (qtime < tstep)
        {
            dt = Math.min(net.Qstep,tstep-qtime);
            qtime += dt;
            if (Reactflag) updatesegs(dt);
            accumulate(dt);
            updatenodes(dt);
            sourceinput(dt);
            release(dt);
        }
        updatesourcenodes(tstep);
    }

    // initializes water quality segments
    void  initsegs()

    {
        int     j,k;
        double   c,v;


        for (k=1; k<=net.getMaxLinks(); k++)
        {
            FlowDir[k] = '+';
            if (hyd.Q[k] < 0.) FlowDir[k] = '-';

            FirstSeg[k].clear();

            j = DOWN_NODE(k);
            if (j <= net.getSections(SectType._JUNCTIONS)) c = C[j];
            else
                c = net.getTank(j-net.getSections(SectType._JUNCTIONS)).getConcentration();


            addseg(k,LINKVOL(k),c);
        }


        for (j=1; j<=net.getMaxTanks(); j++)
        {
            Tank tk = net.getTank(j);

            if (tk.getArea() == 0.0
                    ||  tk.getMixModel() == MixType.MIX1) continue;


            k = net.getMaxLinks() + j;
            c = tk.getConcentration();

            FirstSeg[k].clear();


            if (tk.getMixModel() == MixType.MIX2)
            {
                v = Math.max(0, tk.getVolume() - tk.getV1max());
                addseg(k,v,c);
                v = tk.getVolume() - v;
                addseg(k,v,c);
            }


            else
            {
                v = tk.getVolume();
                addseg(k,v,c);
            }
        }
    }

    // re-orients segments (if flow reverses)
    void  reorientsegs()

    {
        Seg   seg, nseg, pseg;
        int    k;
        char   newdir;


        for (k=1; k<=net.getMaxLinks(); k++)
        {


            newdir = '+';
            if (hyd.Q[k] == 0.0)
                newdir = FlowDir[k];
            else if (hyd.Q[k] < 0.0)
                newdir = '-';



            if (newdir != FlowDir[k])
            {
                /*seg = FirstSeg[k];
                FirstSeg[k] = LastSeg[k];
                LastSeg[k] = seg;
                pseg = null;

                while (seg != null)
                {
                    nseg = seg->prev;
                    seg->prev = pseg;
                    pseg = seg;
                    seg = nseg;
                } */

                Collections.reverse(FirstSeg[k]);

                FlowDir[k] = newdir;
            }
        }
    }

    //reacts material in pipe segments up to time t
    void  updatesegs(long dt)

    {
        int    k;
        double  cseg, rsum, vsum;


        for (k=1; k<=net.getMaxLinks(); k++)
        {
            rsum = 0.0;
            vsum = 0.0;
            if (net.getLink(k).getLenght() == 0.0) continue;

            for(Seg seg : FirstSeg[k])
            {
                cseg = seg.c;
                seg.c = pipereact(k,seg.c,seg.v,dt);

                if (net.Qualflag == QualType.CHEM){
                    rsum += Math.abs((seg.c - cseg))*seg.v;
                    vsum += seg.v;
                }
            }

            if (vsum > 0.0) R[k] = rsum/vsum/dt*Constants.SECperDAY;
            else R[k] = 0.0;
        }
    }

    // Removes all segments in link k
    void  removesegs(int k){
         FirstSeg[k].clear();
    }

    // Adds a segment to start of link k (i.e., upstream of current last segment).
    void  addseg(int k, double v, double c)
    {
        Seg seg = new Seg();
        seg.c = c;
        seg.v = v;
        FirstSeg[k].add(seg);
    }

    // Accumulates mass flow at nodes and updates nodal quality
    void accumulate(long dt)
    {
        int i,j,k;
        double  cseg,v,vseg;
        Seg   seg;
        int Nnodes = net.getMaxNodes();

        Arrays.fill(VolIn,0.0);
        Arrays.fill(MassIn,0.0);
        Arrays.fill(X,0.0);

        for (k=1; k<=net.getMaxLinks(); k++)
        {
            j = DOWN_NODE(k);
            if (FirstSeg[k].size()>0)
            {
                MassIn[j] += FirstSeg[k].get(0).c;
                VolIn[j]++;
            }
            j = UP_NODE(k);
            if (FirstSeg[k].size()>0)
            {
                MassIn[j] += FirstSeg[k].get(FirstSeg[k].size()-1).c;
                VolIn[j]++;
            }
        }
        for (k=1; k<=Nnodes; k++)
            if (VolIn[k] > 0.0) X[k] = MassIn[k]/VolIn[k];


        Arrays.fill(VolIn,0.0);
        Arrays.fill(MassIn,0.0);

        for (k=1; k<=net.getMaxLinks(); k++)
        {
            i = UP_NODE(k);
            j = DOWN_NODE(k);
            v = Math.abs(hyd.Q[k])*dt;


            while (v > 0.0)
            {
                if(FirstSeg[k].size()==0) break;

                seg = FirstSeg[k].get(0);

                // Volume transported from this segment is
                // minimum of flow volume & segment volume
                // (unless leading segment is also last segment)
                vseg = seg.v;
                vseg = Math.min(vseg,v);
                if (FirstSeg[k].size()==1) vseg = v;


                cseg = seg.c;
                VolIn[j] += vseg;
                MassIn[j] += vseg*cseg;

                v -= vseg;

                // If all of segment's volume was transferred, then
                // replace leading segment with the one behind it
                // (Note that the current seg is recycled for later use.)
                if (v >= 0.0 && vseg >= seg.v){
                    FirstSeg[k].remove(0);
                }
                else{
                    seg.v -= vseg;
                }
            }
        }
    }

    // Updates concentration at all nodes to mixture of accumulated inflow from connecting pipes.
    void updatenodes(long dt){
        for (int i=1; i<=net.getSections(SectType._JUNCTIONS); i++)
        {
            if (hyd.D[i] < 0.0) VolIn[i] -= hyd.D[i]*dt;
            if (VolIn[i] > 0.0) C[i] = MassIn[i]/VolIn[i];
            else                C[i] = X[i];
        }

        updatetanks(dt);

        if (net.Qualflag == QualType.TRACE) C[net.TraceNode] = 100.0;
    }

    // Computes contribution (if any) of mass additions from WQ sources at each node.
    void sourceinput(long dt)

    {
        int   j,n;
        double massadded = 0.0, s, volout;
        double qout, qcutoff;
        Source source;

        qcutoff = 10.0*Constants.TINY;

        Arrays.fill(X,0.0);

        if (net.Qualflag != QualType.CHEM) return;

        for (n=1; n<=net.getMaxNodes(); n++){

            source = net.getNode(n).getSource();
            if (source == null) continue;
            if (source.getC0() == 0.0) continue;

            if (n <= net.getSections(SectType._JUNCTIONS))
                volout = VolIn[n];
            else
                volout = VolIn[n] - hyd.D[n]*dt;

            qout = volout / (double) dt;

            if (qout > qcutoff){

                s = sourcequal(source);
                switch(source.getType())
                {
                    case CONCEN:
                        if (hyd.D[n] < 0.0)
                        {
                            massadded = -s*hyd.D[n]*dt;
                            if (n > net.getSections(SectType._JUNCTIONS))
                                C[n] = 0.0;
                        }
                        else
                            massadded = 0.0;
                        break;
                    case MASS:
                        massadded = s*dt;
                        break;
                    case SETPOINT:
                        if (s > C[n])
                            massadded = (s-C[n])*volout;
                        else
                            massadded = 0.0;
                        break;
                    case FLOWPACED:
                        massadded = s*volout;
                        break;
                }

                X[n] = massadded/volout;

                source.setSmass(source.getSmass() + massadded);
                if (hyd.Htime >= net.Rstart) Wsource += massadded;
            }
        }

        if (hyd.Htime >= net.Rstart)
        {
            for (j=1; j<=net.getMaxTanks(); j++)
            {
                if (net.getTank(j).getArea() == 0.0)
                {
                    n = net.getSections(SectType._JUNCTIONS) + j;
                    volout = VolIn[n] - hyd.D[n]*dt;
                    if (volout > 0.0) Wsource += volout*C[n];
                }
            }
        }
    }

    // creates new segments in outflow links from nodes.
    void release(long dt)
    {
        int    k,n;
        double  c,q,v;
        Seg   seg;

        for (k=1; k<=net.getMaxLinks(); k++)
        {
            if (hyd.Q[k] == 0.0) continue;

            n = UP_NODE(k);
            q = Math.abs(hyd.Q[k]);
            v = q*dt;

            c = C[n] + X[n];

            if (FirstSeg[k].size()>0){
                seg =  FirstSeg[k].get(FirstSeg[k].size()-1);

                if (Math.abs(seg.c - c) < net.Ctol){
                    seg.c = (seg.c*seg.v + c*v) / (seg.v + v);
                    seg.v += v;
                }

                else
                    addseg(k,v,c);
            }
            else
                addseg(k,LINKVOL(k),c);
        }
    }

    // Updates quality at source nodes.
    void  updatesourcenodes(long dt)

    {
        int i,n;
        Source source;

        if (net.Qualflag != QualType.CHEM) return;


        for (n=1; n<=net.getMaxNodes(); n++)
        {
            source = net.getNode(n).getSource();
            if (source == null) continue;


            C[n] += X[n];


            if (n > net.getSections(SectType._JUNCTIONS))
            {
                i = n - net.getSections(SectType._JUNCTIONS);
                if (net.getTank(i).getArea() > 0.0) C[n] = net.getTank(i).getConcentration();
            }


            source.setSmass(source.getSmass() / (double)dt);
        }
    }

    // Updates tank volumes & concentrations
    void  updatetanks(long dt)
    {
        int   i,n;

        for (i=1; i<=net.getMaxTanks(); i++)
        {
            if (net.getTank(i).getArea() == 0.0){
                n = net.getTank(i).getNode();
                C[n] = net.getNode(n).getC0();
            }
            else
                switch(net.getTank(i).getMixModel())
                {
                    case MIX2: tankmix2(i,dt); break;
                    case FIFO: tankmix3(i,dt); break;
                    case LIFO: tankmix4(i,dt); break;
                    default:   tankmix1(i,dt); break;
                }
        }
    }



    // Complete mix tank model
    void  tankmix1(int i, long dt)
    {
        Tank tk = net.getTank(i);
        int   n;
        double cin;
        double c, cmax, vold, vin;

        c = tankreact(tk.getConcentration(),tk.getVolume(),tk.getKb(),dt);

        vold = tk.getVolume();
        n = tk.getNode();
        tk.setVolume( tk.getVolume() + hyd.D[n]*dt);
        vin  = VolIn[n];


        if (vin > 0.0) cin = MassIn[n]/vin;
        else           cin = 0.0;
        cmax = Math.max(c, cin);


        if (vin > 0.0) c = (c*vold + cin*vin)/(vold + vin);
        c = Math.min(c, cmax);
        c = Math.max(c, 0.0);
        tk.setConcentration(c);
        C[n] = tk.getConcentration();
    }

    // New version of tankmix2
    void  tankmix2(int i, long dt)
    {
        int     k,n;
        double  cin,
                vin,
                vt,
                vnet,
                v1max;
        Seg     seg1,seg2;
        Tank tk = net.getTank(i);

        k = net.getMaxLinks() + i;
        seg1 = FirstSeg[k].get(FirstSeg[k].size()-1);
        seg2 = FirstSeg[k].get(0);
        if (seg1 == null || seg2 == null) return;

        seg1.c = tankreact(seg1.c,seg1.v,tk.getKb(),dt);
        seg2.c = tankreact(seg2.c,seg2.v,tk.getKb(),dt);

        n = tk.getNode();
        vnet = hyd.D[n]*dt;
        vin = VolIn[n];
        if (vin > 0.0) cin = MassIn[n]/vin;
        else           cin = 0.0;
        v1max = tk.getV1max();


        vt = 0.0;
        if (vnet > 0.0)
        {
            vt = Math.max(0.0, (seg1.v + vnet - v1max));
            if (vin > 0.0)
            {
                seg1.c = ((seg1.c)*(seg1.v) + cin*vin) / (seg1.v + vin);
            }
            if (vt > 0.0)
            {
                seg2.c = ((seg2.c)*(seg2.v) + (seg1.c)*vt) / (seg2.v + vt);
            }
        }


        if (vnet < 0.0)
        {
            if (seg2.v > 0.0)
            {
                vt = Math.min(seg2.v, (-vnet));
            }
            if (vin + vt > 0.0)
            {
                seg1.c = ((seg1.c)*(seg1.v) + cin*vin + (seg2.c)*vt)/(seg1.v + vin + vt);
            }
        }


        if (vt > 0.0)
        {
            seg1.v = v1max;
            if (vnet > 0.0) seg2.v += vt;
            else            seg2.v = Math.max(0.0, ((seg2.v)-vt));
        }
        else
        {
            seg1.v += vnet;
            seg1.v = Math.min(seg1.v, v1max);
            seg1.v = Math.max(0.0, seg1.v);
            seg2.v = 0.0;
        }
        tk.setVolume( tk.getVolume() +  vnet);
        tk.setVolume( Math.max(tk.getVolume(),0.0)) ;




        tk.setConcentration(seg1.c);
        C[n] = tk.getConcentration();
    }

    //First-In-First-Out (FIFO) tank model
    void  tankmix3(int i, long dt)
    {
        int   k,n;
        double vin,vnet,vout,vseg;
        double cin,vsum,csum;

        Tank tk = net.getTank(i);

        k = net.getMaxLinks() + i;
        if (FirstSeg[k].size() == 0) return;


        if (Reactflag)
        {
            for(Seg seg : FirstSeg[k]){
                seg.c = tankreact(seg.c,seg.v,tk.getKb(),dt);
            }
        }


        n = net.getTank(i).getNode();
        vnet = hyd.D[n]*dt;
        vin = VolIn[n];
        vout = vin - vnet;
        if (vin > 0.0) cin = MassIn[n]/VolIn[n];
        else           cin = 0.0;
        tk.setVolume( tk.getVolume()  + vnet );
        tk.setVolume( Math.max(tk.getVolume(),0.0)) ;


        vsum = 0.0;
        csum = 0.0;
        while (vout > 0.0)
        {
            Seg seg;
            if (FirstSeg[k].size() == 0) break;
            seg = FirstSeg[k].get(0);
            vseg = seg.v;
            vseg = Math.min(vseg,vout);
            if (FirstSeg[k].size() == 1) vseg = vout;
            vsum += vseg;
            csum += (seg.c)*vseg;
            vout -= vseg;
            if (vout >= 0.0 && vseg >= seg.v)
            {
                if(FirstSeg[k].size()>1)
                    FirstSeg[k].remove(0);
            }
            else{
                seg.v -= vseg;
            }
        }

        if (vsum > 0.0) tk.setConcentration( csum/vsum );
        else            tk.setConcentration( FirstSeg[k].get(0).c);

        C[n] = tk.getConcentration();

        if (vin > 0.0)
        {
            if ( FirstSeg[k].size()> 0 ){
                Seg seg =FirstSeg[k].get(FirstSeg[k].size()-1);

                if (Math.abs(seg.c - cin) < net.Ctol)
                    seg.v += vin;
                else
                    addseg(k,vin,cin);
            }
            else
                addseg(k,vin,cin);
        }
    }

    // Last In-First Out (LIFO) tank model
    void  tankmix4(int i, long dt)      // TODO, verificar o funcionamento desta LIFO

    {
        int   k, n;
        double vin, vnet, cin, vsum, csum, vseg;
        Seg   tmpseg;

        k = net.getMaxLinks() + i;
        if (FirstSeg[k].size()==0) return;

        Tank tk = net.getTank(i);

        if (Reactflag)
        {
            for(int j = FirstSeg[k].size()-1;j>=0;j++)
            {
                Seg seg = FirstSeg[k].get(j);
                seg.c = tankreact(seg.c,seg.v,tk.getKb(),dt);
            }
        }


        n = tk.getNode();
        vnet = hyd.D[n]*dt;
        vin = VolIn[n];
        if (vin > 0.0) cin = MassIn[n]/VolIn[n];
        else           cin = 0.0;
        tk.setVolume( tk.getVolume() + vnet );
        tk.setVolume(  Math.max(0.0, tk.getVolume()));                                            //(2.00.12 - LR)
        tk.setConcentration(FirstSeg[k].get(FirstSeg[k].size()-1).c);


        if (vnet > 0.0)
        {
            if ( FirstSeg[k].size() > 0)
            {
                Seg seg = FirstSeg[k].get(FirstSeg[k].size()-1);
                if (Math.abs(seg.c - cin) < net.Ctol) seg.v += vnet;



                else
                {
                    ///tmpseg = seg;
                    //FirstSeg[k].remove(FirstSeg[k].size()-1);
                    //LastSeg[k] = NULL;
                    addseg(k,vnet,cin);
                    //LastSeg[k]->prev = tmpseg;
                }
            }


            else addseg(k,vnet,cin);


            tk.setConcentration(FirstSeg[k].get(FirstSeg[k].size()-1).c);
        }


        else if (vnet < 0.0)
        {
            vsum = 0.0;
            csum = 0.0;
            vnet = -vnet;
            Seg seg = FirstSeg[k].get(FirstSeg[k].size()-1);
            while (vnet > 0.0)
            {
                //seg = LastSeg[k];
                if (seg == null) break;
                vseg = seg.v;
                vseg = Math.min(vseg,vnet);
                if (seg == FirstSeg[k]) vseg = vnet;
                vsum += vseg;
                csum += (seg.c)*vseg;
                vnet -= vseg;
                if (vnet >= 0.0 && vseg >= seg.v)
                {
                    if (FirstSeg[k].size()>1)                                                     //(2.00.12 - LR)
                    {
                        FirstSeg[k].remove(FirstSeg[k].size()-1);                                                           //(2.00.12 - LR)
                        //LastSeg[k] = seg->prev;
                        //if (LastSeg[k] == NULL) FirstSeg[k] = NULL;                   //(2.00.12 - LR)
                        //seg->prev = FreeSeg;
                        //FreeSeg = seg;
                    }                                                                  //(2.00.12 - LR)
                }
                else
                {
                    seg.v -= vseg;
                }
            }

            tk.setConcentration( (csum + MassIn[n])/(vsum + vin) );
        }
        C[n] = tk.getConcentration();
    }

    // Determines source concentration in current time period.
    double  sourcequal(Source source)
    {
        int   i;
        long  k;
        double c;

        c = source.getC0();

        if (source.getType() == SourceType.MASS)
            c /= 60.0;
        else
            c /= net.getUcf(FieldType.QUALITY);


        i = source.getPattern();
        if (i == 0) return(c);
        k = ((Qtime+net.Pstart)/net.Pstep) % (long)net.getPattern(i).getFactorsList().size();
        return(c*net.getPattern(i).getFactorsList().get((int)k));
    }

    // Computes average quality in link k .
    double  avgqual(int k)
    {
        double  vsum = 0.0,
                msum = 0.0;

        if (net.Qualflag == QualType.NONE) return(0.);

        for(Seg seg : FirstSeg[k])
        {
            vsum += seg.v;
            msum += (seg.c)*(seg.v);
        }
        if (vsum > 0.0) return(msum/vsum);
        else return( (C[net.getLink(k).getN1()] + C[net.getLink(k).getN2()])/2.0);
    }

    // Determines wall reaction coeff. for each pipe
    void  ratecoeffs()

    {
        int   k;
        double kw;

        for (k=1; k<=net.getMaxLinks(); k++)
        {
            kw = net.getLink(k).getKw();
            if (kw != 0.0) kw = piperate(k);
            net.getLink(k).setR(kw);
            R[k] = 0.0;
        }
    }

    // Finds wall reaction rate coeffs.
    double piperate(int k)
    {
        double a,d,u,kf,kw,y,Re,Sh;

        d = net.getLink(k).getDiameter();

        if (Sc == 0.0)
        {
            if (net.WallOrder == 0.0) return(Constants.BIG);
            else return(net.getLink(k).getKw()*(4.0/d)/net.getUcf(FieldType.ELEV));
        }

        a = Constants.PI*d*d/4.0;
        u = Math.abs(hyd.Q[k])/a;
        Re = u*d/net.Viscos;

        if (Re < 1.0)
            Sh = 2.0;

        else if (Re >= 2300.0)
            Sh = 0.0149*Math.pow(Re,0.88)*Math.pow(Sc,0.333);



        else
        {
            y = d/net.getLink(k).getLenght()*Re*Sc;
            Sh = 3.65+0.0668*y/(1.0+0.04*Math.pow(y,0.667));
        }


        kf = Sh*net.Diffus/d;


        if (net.WallOrder == 0.0) return(kf);


        kw = net.getLink(k).getKw()/net.getUcf(FieldType.ELEV);
        kw = (4.0/d)*kw*kf/(kf+Math.abs(kw));
        return(kw);
    }

    // Computes new quality in a pipe segment after reaction occurs
    double  pipereact(int k, double c, double v, long dt)

    {
        double cnew, dc, dcbulk, dcwall, rbulk, rwall;


        if (net.Qualflag == QualType.AGE) return(c+(double)dt/3600.0);

        Link lk = net.getLink(k);
        rbulk = bulkrate(c,lk.getKb(),net.BulkOrder)*Bucf;
        rwall = wallrate(c,lk.getDiameter(),lk.getKw(),lk.getR());


        dcbulk = rbulk*(double)dt;
        dcwall = rwall*(double)dt;


        if (hyd.Htime >= net.Rstart)
        {
            Wbulk += Math.abs(dcbulk)*v;
            Wwall += Math.abs(dcwall)*v;
        }


        dc = dcbulk + dcwall;
        cnew = c + dc;
        cnew = Math.max(0.0,cnew);
        return(cnew);
    }

    // Computes new quality in a tank after reaction occurs
    double  tankreact(double c, double v, double kb, long dt)
    {
        double cnew, dc, rbulk;

        if (!Reactflag)
            return(c);

        if (net.Qualflag == QualType.AGE)
            return(c + (double)dt/3600.0);

        rbulk = bulkrate(c,kb,net.TankOrder)*Tucf;

        dc = rbulk*(double)dt;
        if (hyd.Htime >= net.Rstart) Wtank += Math.abs(dc)*v;
        cnew = c + dc;
        cnew = Math.max(0.0,cnew);
        return(cnew);
    }

    // Computes bulk reaction rate (mass/volume/time)
    double  bulkrate(double c, double kb, double order){
        double c1;

        if (order == 0.0)
            c = 1.0;
        else if (order < 0.0)
        {
            c1 = net.Climit + SGN(kb)*c;
            if (Math.abs(c1) < Constants.TINY) c1 = SGN(c1)*Constants.TINY;
            c = c/c1;
        }
        else
        {
            if (net.Climit == 0.0)
                c1 = c;
            else
                c1 = Math.max(0.0, SGN(kb)*(net.Climit-c));

            if (order == 1.0)
                c = c1;
            else if (order == 2.0)
                c = c1*c;
            else
                c = c1*Math.pow(Math.max(0.0,c),order-1.0);
        }


        if (c < 0) c = 0;
        return(kb*c);
    }

    // Computes wall reaction rate
    double  wallrate(double c, double d, double kw, double kf){
        if (kw == 0.0 || d == 0.0) return(0.0);
        if (net.WallOrder == 0.0)
        {
            kf = SGN(kw)*c*kf;
            kw = kw*Math.pow(net.getUcf(FieldType.ELEV),2);
            if (Math.abs(kf) < Math.abs(kw))
                kw = kf;
            return(kw*4.0/d);
        }
        else
            return(c*kf);
    }

    static private double SGN(double val){
        return val<0?-1:1;
    }

    private int UP_NODE(int x){
        return ( (FlowDir[(x)]=='+') ? net.getLink(x).getN1() : net.getLink(x).getN2() );
    }

    private int DOWN_NODE(int x){
        return ( (FlowDir[(x)]=='+') ? net.getLink(x).getN2() : net.getLink(x).getN1() );
    }

    private double LINKVOL(int k){
        Link lk = net.getLink(k);
        return ( 0.785398*lk.getLenght()*(lk.getDiameter()*lk.getDiameter()) );
    }



    // Reads hydraulic solution from file HydFile
    Long  readhyd(DataInputStream file)
    {
        long hydtime;
        int Nnodes = net.getMaxNodes();
        int Nlinks = net.getMaxLinks();

        try{
            hydtime = file.readInt();


            for(int i = 0;i<Nnodes;i++)
                hyd.D[i+1] = file.readFloat();

            for(int i = 0;i<Nnodes;i++)
                hyd.H[i+1] = file.readFloat();

            for(int i = 0;i<Nlinks;i++)
                hyd.Q[i+1] = file.readFloat();

            for(int i = 0;i<Nlinks;i++)
                hyd.S[i+1] = StatType.values()[(int)file.readFloat()];

            for(int i = 0;i<Nlinks;i++)
                hyd.K[i+1] = file.readFloat();

        }
        catch(IOException e)
        {
            return null;
        }

        return hydtime;
    }

    // reads hydraulic time step from file HydFile
    Long readhydstep(DataInputStream file)
    {
        long hydstep;
        try{
            hydstep = file.readInt();
        }
        catch(IOException e){
            return null;
        }

        return hydstep;
    }
}
