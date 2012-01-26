package org.addition.epanetold;

import java.io.*;
import java.util.*;

import org.addition.epanetold.Types.*;
import org.addition.epanetold.Types.EnumVariables.*;

public strictfp class Hydraulic {
    public static final double QZERO = 1.e-6d;  // Equivalent to zero flow
    public static final double CBIG = 1.e8d;   // Big coefficient
    public static final double CSMALL = 1.e-6d;  // Small coefficient

    // Constants used for computing Darcy-Weisbach friction factor
    public static final double A1 = 0.314159265359e04; // 1000*PI
    public static final double A2 = 0.157079632679e04; // 500*PI
    public static final double A3 = 0.502654824574e02; // 16*PI
    public static final double A4 = 6.283185307;        // 2*PI
    public static final double A8 = 4.61841319859;      // 5.74*(PI/4)^.9
    public static final double A9 = -8.685889638e-01;   // -2/ln(10)
    public static final double AA = -1.5634601348;      // -2*.9*2/ln(10)
    public static final double AB = 3.28895476345e-03;  // 5.74/(4000^.9)
    public static final double AC = -5.14214965799e-03; // AA*AB

    private DataOutputStream HydFile; // Outputstream fro hydraulic simulation dump.

    long Htime;             // Current hyd. time (sec)
    long Rtime;             // Next reporting time

    double[] H;            // Node heads
    double[] D;            // Node actual demand
    double[] K;            // Link settings
    double[] Q;            // Link flows
    private double[] Aii;
    private double[] Aij;
    private double[] F;            // Right hand side coeffs
    private double[] E;            // Emitter flows
    private double[] P;            // Inverse headloss derivatives
    private double[] Y;            // Flow correction factors
    private double[] X;            // General purpose array
    StatType[] OldStat;
    StatType[] S;          // link status
    double Dsystem;         // Total system demand
    private double RelaxFactor;     //Relaxation factor used for updating flow changes


    private Epanet epanet;
    private Network net;
    private SparseMatrix smat;
    private Rules rules;
    private Report report;


    public Hydraulic(Epanet epanet) {
        this.epanet = epanet;
    }

    public void loadDependencies() {
        net = epanet.getNetwork();
        smat = epanet.getSparseMatrix();
        rules = epanet.getRules();
        report = epanet.getReport();
    }

    // Opens hydraulics solver system.
    public int openhyd() {
        smat.createSparse();
        allocmatrix();
        for (int i = 1; i <= net.getMaxLinks(); i++)
            initlinkflow(i, net.getLink(i).getStat(), net.getLink(i).getKc());
        return (0);
    }

    // Initializes hydraulics solver system.
    public void inithyd() {
        int i, j;

        for (i = 1; i <= net.getMaxTanks(); i++) {
            Tank tk = net.getTank(i);
            tk.setVolume(net.getTank(i).getV0());
            H[tk.getNode()] = tk.getH0();

            D[tk.getNode()] = 0.0;

            OldStat[net.getMaxLinks() + i] = StatType.TEMPCLOSED;
        }

        for (i = 1; i <= net.getSections(SectType._JUNCTIONS); i++)
            if (net.getNode(i).getKe() > 0.0) E[i] = 1.0;

        for (i = 1; i <= net.getMaxLinks(); i++) {
            S[i] = net.getLink(i).getStat();
            K[i] = net.getLink(i).getKc();

            if (
                    (net.getLink(i).getType() == LinkType.PRV || net.getLink(i).getType() == LinkType.PSV
                            || net.getLink(i).getType() == LinkType.FCV)
                            && (net.getLink(i).getKc() != Constants.MISSING)
                    ) S[i] = StatType.ACTIVE;


            if (S[i].ordinal() <= StatType.CLOSED.ordinal())
                Q[i] = Constants.QZERO;
            else if (Math.abs(Q[i]) <= Constants.QZERO )
                initlinkflow(i, S[i], K[i]);

            OldStat[i] = S[i];
        }

        for (i = 1; i <= net.getSections(SectType._PUMPS); i++) {
            for (j = 0; j < 6; j++) net.getPump(i).setEnergy(j, 0.0);
        }

        epanet.setHaltflag(0);
        Htime = 0;
        Rtime = net.Rstep;
    }

    // Solves network hydraulics in a single time period
    int runhyd() {
        int iter;
        int errcode;
        double relerr;

        demands();
        controls();

        double[] ret = netsolve();

        iter = (int) ret[0];
        relerr = ret[1];
        errcode = (int) ret[2];

        if (errcode == 0) {
            if (net.Statflag != StatFlag.FALSE)
                report.writehydstat(iter, relerr);

            if (relerr > net.Hacc && net.ExtraIter == -1) epanet.setHaltflag(1);
            errcode = report.writehydwarn(iter, relerr);
        }

        return (errcode);
    }

    int runhyd2(BufferedWriter buf) {
        int iter;
        int errcode;
        double relerr;

        demands();
        controls();

        double[] ret = netsolve();

        iter = (int) ret[0];
        relerr = ret[1];
        errcode = (int) ret[2];

        if (errcode == 0) {
            if (net.Statflag != StatFlag.FALSE)
                report.writehydstat2(buf,iter, relerr);

            if (relerr > net.Hacc && net.ExtraIter == -1) epanet.setHaltflag(1);
            errcode = report.writehydwarn2(buf,iter, relerr);
        }

        return (errcode);
    }

    //finds length of next time step & updates tank levels and rule-based contol actions
    long[] nexthyd() {
        long hydstep;
        int errcode = 0;


        //if (epanetold.isSaveflag())
        errcode = savehyd(Htime);

        if (epanet.getHaltflag() != 0) Htime = net.Dur;

        hydstep = 0;
        if (Htime < net.Dur) hydstep = timestep();
        //if (epanetold.isSaveflag())

        savehydstep(hydstep);


        if (net.Dur == 0) addenergy(0);
        else if (Htime < net.Dur) addenergy(hydstep);


        if (Htime < net.Dur) {
            Htime += hydstep;
            if (Htime >= Rtime) Rtime += net.Rstep;
        } else {
            Htime++;
        }

        return (new long[]{hydstep, errcode});
    }


    // allocates memory used for solution matrix coeffs.
    private void allocmatrix() {
        Aii = new double[net.getMaxNodes() + 1];
        Aij = new double[smat.getCoeffsCount() + 1];
        S = new StatType[net.getMaxLinks() + net.getMaxTanks() + 1];
        H = new double[net.getMaxNodes() + 1];
        F = new double[net.getMaxNodes() + 1];
        E = new double[net.getMaxNodes() + 1];
        P = new double[net.getMaxLinks() + 1];
        Y = new double[net.getMaxLinks() + 1];
        X = new double[Math.max(net.getMaxNodes() + 1, net.getMaxLinks() + 1)];
        OldStat = new StatType[net.getMaxLinks() + net.getMaxTanks() + 1];
        D = new double[net.getMaxNodes() + 1];
        //C    = new double[net.getMaxNodes()+1];
        Q = new double[net.getMaxLinks() + 1];
        K = new double[net.getMaxLinks() + 1];
    }

    //sets initial flow in link to QZERO if link is closed,
    //           to design flow for a pump, or to flow at velocity of
    //           1 fps for other links.
    void initlinkflow(int i, StatType s, double k) {
        if (s == StatType.CLOSED)
            Q[i] = Constants.QZERO;
        else if (net.getLink(i).getType().equals(LinkType.PUMP))
            Q[i] = k * net.getPump(Utilities.pumpIndex(net, i)).getQ0();
        else
            Q[i] = Constants.PI * Math.pow(net.getLink(i).getDiameter(), 2) / 4.0;
    }

    // Sets flow in link based on current headloss
//    void setlinkflow(int k, double dh) {
//        int i, p;
//        double h0;
//        double x, y;
//
//        Link link = net.getLink(k);
//        switch (link.getType()) {
//
//            case CV:
//            case PIPE:
//
//                if (net.Formflag == FormType.DW) {
//                    x = -Math.log(K[k] / 3.7 / link.getDiameter());
//                    y = Math.sqrt(Math.abs(dh) / link.getR() / 1.32547);
//                    Q[k] = x * y;
//                } else {
//                    x = Math.abs(dh) / link.getR();
//                    y = 1.0 / net.Hexp;
//                    Q[k] = Math.pow(x, y);
//                }
//
//                if (dh < 0.0) Q[k] = -Q[k];
//                break;
//
//            case PUMP:
//                dh = -dh;
//                p = Utilities.pumpIndex(net, k);
//                Pump pump = net.getPump(p);
//
//                if (pump.getPtype().equals(PumpType.CUSTOM)) {
//                    dh = -dh * net.getUcf(FieldType.HEAD) / Math.pow(K[k], 2);
//                    i = pump.getHcurve();
//                    Q[k] = Utilities.interp(
//                            net.getCurve(i).getNpts(),
//                            net.getCurve(i).getY(),
//                            net.getCurve(i).getX(),
//                            dh) * K[k] / net.getUcf(FieldType.FLOW);
//                } else {
//                    h0 = -Math.pow(K[k], 2) * pump.getH0();
//                    x = Math.pow(K[k], 2.0 - pump.getN());
//                    x = Math.abs(h0 - dh) / (pump.getR() * x);
//                    y = 1.0 / pump.getN();
//                    Q[k] = Math.pow(x, y);
//                }
//                break;
//        }
//    }


    // Sets link status to OPEN or CLOSED
    void setlinkstatus(int index, int value) {
        Link link = net.getLink(index);
        if (value == 1) {
            if (link.getType() == LinkType.PUMP) K[index] = 1.0;
            if (link.getType().ordinal() > LinkType.PUMP.ordinal()
                    && link.getType() != LinkType.GPV) K[index] = Constants.MISSING;

            S[index] = StatType.OPEN;
        } else if (value == 0) {
            if (link.getType() == LinkType.PUMP) K[index] = 0.0;

            if (link.getType().ordinal() > LinkType.PUMP.ordinal()
                    && link.getType() != LinkType.GPV) K[index] = Constants.MISSING;

            S[index] = StatType.CLOSED;
        }
    }

    // Sets pump speed or valve setting, adjusting link status and flow when necessary
    public void setlinksetting(int index, double value) {
        Link link = net.getLink(index);
        if (link.getType() == LinkType.PUMP) {
            K[index] = value;
            if (value > 0 && S[index].ordinal() <= StatType.CLOSED.ordinal()) {
                S[index] = StatType.OPEN;
            }
            if (value == 0 && S[index].ordinal() > StatType.CLOSED.ordinal()) {
                S[index] = StatType.CLOSED;
            }
        } else if (link.getType() == LinkType.FCV) {
            K[index] = value;
            S[index] = StatType.ACTIVE;
        } else {
            if (K[index] == Constants.MISSING && S[index].ordinal() <= StatType.CLOSED.ordinal()) {
                S[index] = StatType.OPEN;
            }
            K[index] = value;
        }
    }




    // Computes demands at nodes during current time period
    void demands() {
        int i, j, n;
        long k, p;
        double djunc, sum;

        p = (Htime + net.Pstart) / net.Pstep;

        Dsystem = 0.0;
        for (i = 1; i <= net.getSections(SectType._JUNCTIONS); i++) {
            sum = 0.0;
            for (Demand demand : net.getNode(i).getDemand()) {
                j = demand.getPattern();
                List<Double> factors = net.getPattern(j).getFactorsList();
                k = p % (long) factors.size();
                djunc = (demand.getBase()) * factors.get((int) k) * net.Dmult;
                if (djunc > 0.0) Dsystem += djunc;
                sum += djunc;
            }
            D[i] = sum;
        }

        for (n = 1; n <= net.getMaxTanks(); n++) {
            Tank tk = net.getTank(n);
            if (tk.getArea() == 0.0) {
                j = tk.getPattern();
                if (j > 0) {
                    List<Double> factors = net.getPattern(j).getFactorsList();
                    k = p % (long) factors.size();

                    i = tk.getNode();
                    H[i] = net.getNode(i).getElevation() * factors.get((int) k);
                }
            }
        }

        for (n = 1; n <= net.getSections(SectType._PUMPS); n++) {
            Pump pump = net.getPump(n);
            j = pump.getUpat();
            if (j > 0) {
                i = pump.getLink();
                List<Double> factors = net.getPattern(j).getFactorsList();
                k = p % (long) factors.size();
                setlinksetting(i, factors.get((int) k));
            }
        }
    }


    // implements simple controls based on time or tank levels
    int controls() {
        int i, k, n, reset, setsum;
        double h, vplus;
        double v1, v2;
        double k1, k2;
        StatType s1, s2;

        setsum = 0;
        for (i = 1; i <= net.getSections(SectType._CONTROLS); i++) {
            Control control = net.getControl(i);

            reset = 0;
            if ((k = control.getLink()) <= 0) continue;

            if ((n = control.getNode()) > 0 && n > net.getSections(SectType._JUNCTIONS)) {
                h = H[n];
                vplus = Math.abs(D[n]);
                v1 = tankvolume(n - net.getSections(SectType._JUNCTIONS), h);
                v2 = tankvolume(n - net.getSections(SectType._JUNCTIONS), control.getGrade());
                if (control.getType().equals(ControlType.LOWLEVEL) && v1 <= v2 + vplus)
                    reset = 1;
                if (control.getType().equals(ControlType.HILEVEL) && v1 >= v2 - vplus)
                    reset = 1;
            }

            if (control.getType().equals(ControlType.TIMER)) {
                if (control.getTime() == Htime) reset = 1;
            }

            if (control.getType().equals(ControlType.TIMEOFDAY)) {
                if ((Htime + net.Tstart) % Constants.SECperDAY == control.getTime()) reset = 1;
            }

            if (reset == 1) {
                if (S[k].ordinal() <= StatType.CLOSED.ordinal()) s1 = StatType.CLOSED;
                else s1 = StatType.OPEN;
                s2 = control.getStatus();
                k1 = K[k];
                k2 = k1;
                if (net.getLink(k).getType().ordinal() > LinkType.PIPE.ordinal()) k2 = control.getSetting();
                if (s1 != s2 || k1 != k2) {
                    S[k] = s2;
                    K[k] = k2;
                    if (net.Statflag != null) report.writecontrolaction(k, i);
                    setsum++;
                }
            }
        }
        return (setsum);
    }

    // computes time step to advance hydraulic simulation
    long timestep() {
        long n, t, tstep;


        tstep = net.Hstep;


        n = ((Htime + net.Pstart) / net.Pstep) + 1;
        t = n * net.Pstep - Htime;
        if (t > 0 && t < tstep) tstep = t;


        t = Rtime - Htime;
        if (t > 0 && t < tstep) tstep = t;

        tstep = tanktimestep(tstep);
        tstep = controltimestep(tstep);


        if (rules.getNRules() > 0) tstep = ruletimestep(tstep);
        else
            tanklevels(tstep);
        return (tstep);
    }


    // revises time step based on shortest time to fill or
    // drain a tank
    long tanktimestep(long tstep) {
        int i, n;
        double h, q, v;
        long t;

        for (i = 1; i <= net.getMaxTanks(); i++) {
            Tank tank = net.getTank(i);
            if (tank.getArea() == 0.0) continue;
            n = tank.getNode();
            h = H[n];
            q = D[n];
            if (Math.abs(q) <= QZERO) continue;
            if (q > 0.0 && h < tank.getHMax()) {
                v = tank.getVmax() - tank.getVolume();
            } else if (q < 0.0 && h > tank.getHMin()) {
                v = tank.getVmin() - tank.getVolume();
            } else continue;
            t = (long) Utilities.roundOff(v / q);
            if (t > 0 && t < tstep) tstep = t;
        }

        return tstep;
    }

    // Revises time step based on shortest time to activate
    // a simple control
    long controltimestep(long tstep) {
        int i, j, k, n;
        double h, q, v;
        long t, t1, t2;

        for (i = 1; i <= net.getSections(SectType._CONTROLS); i++) {
            Control control = net.getControl(i);
            t = 0;
            if ((n = net.getControl(i).getNode()) > 0) {
                if ((j = n - net.getSections(SectType._JUNCTIONS)) <= 0) continue;
                h = H[n];
                q = D[n];
                if (Math.abs(q) <= QZERO) continue;
                if
                        ((h < control.getGrade() &&
                        control.getType() == ControlType.HILEVEL &&
                        q > 0.0)
                        || (h > control.getGrade() &&
                        control.getType() == ControlType.LOWLEVEL &&
                        q < 0.0)
                        ) {
                    v = tankvolume(j, control.getGrade()) - net.getTank(j).getVolume();
                    t = (long) Utilities.roundOff(v / q);
                }
            }

            if (control.getType() == ControlType.TIMER) {
                if (control.getTime() > Htime)
                    t = control.getTime() - Htime;
            }

            if (control.getType() == ControlType.TIMEOFDAY) {
                t1 = (Htime + net.Tstart) % Constants.SECperDAY;
                t2 = control.getTime();
                if (t2 >= t1) t = t2 - t1;
                else t = Constants.SECperDAY - t1 + t2;
            }

            if (t > 0 && t < tstep) {

                k = control.getLink();
                if (
                        (net.getLink(k).getType().ordinal() > LinkType.PIPE.ordinal() && K[k] != control.getSetting()) ||
                                (S[k] != control.getStatus())
                        )
                    tstep = t;
            }
        }

        return tstep;
    }

    // updates next time step by checking if any rules
    // will fire before then; also updates tank levels.
    long ruletimestep(long tstep) {
        long tnow, tmax, dt, dt1;

        tnow = Htime;
        tmax = tnow + tstep;

        if (rules.getNRules() == 0) {
            dt = tstep;
            dt1 = dt;
        } else {
            dt = net.Rulestep;
            dt1 = net.Rulestep - (tnow % net.Rulestep);
        }


        dt = Math.min(dt, tstep);
        dt1 = Math.min(dt1, tstep);
        if (dt1 == 0) dt1 = dt;

        do {
            Htime += dt1;
            tanklevels(dt1);
            if (rules.checkrules(dt1) != 0) break;
            dt = Math.min(dt, tmax - Htime);
            dt1 = dt;
        } while (dt > 0);


        tstep = Htime - tnow;
        Htime = tnow;

        return tstep;
    }


    // accumulates pump energy usage
    void addenergy(long hstep) {
        int i, j, k;
        long m, n;
        double c0, c, f0, dt, q, psum = 0.0;


        if (net.Dur == 0) dt = 1.0;
        else if (Htime < net.Dur) dt = (double) hstep / 3600.0;
        else dt = 0.0;
        if (dt == 0.0) return;
        n = (Htime + net.Pstart) / net.Pstep;


        c0 = net.Ecost;
        f0 = 1.0;
        if (net.Epat > 0) {
            m = n % (long) net.getPattern(net.Epat).getFactorsList().size();
            f0 = net.getPattern(net.Epat).getFactorsList().get((int) m);
        }

        for (j = 1; j <= net.getSections(SectType._PUMPS); j++) {
            Pump pump = net.getPump(j);
            k = pump.getLink();
            if (S[k].ordinal() <= StatType.CLOSED.ordinal()) continue;
            q = Math.max(QZERO, Math.abs(Q[k]));


            if (pump.getEcost() > 0.0) c = pump.getEcost();
            else c = c0;

            if ((i = pump.getEpat()) > 0) {
                m = n % (long) net.getPattern(i).getFactorsList().size();
                c *= net.getPattern(i).getFactorsList().get((int) m);
            } else c *= f0;

            double[] pe = getenergy(k);
            psum += pe[0];

            pump.setEnergy(0, pump.getEnergy(0) + dt);
            pump.setEnergy(1, pump.getEnergy(1) + pe[1] * dt);
            pump.setEnergy(2, pump.getEnergy(2) + pe[0] / q * dt);
            pump.setEnergy(3, pump.getEnergy(3) + pe[0] * dt);
            pump.setEnergy(4, Math.max(pump.getEnergy(4), pe[0]));
            pump.setEnergy(5, pump.getEnergy(5) + c * pe[0] * dt);
        }

        net.Emax = Math.max(net.Emax, psum);
    }

    // computes flow energy associated with link k
    double[] getenergy(int k) {
        int i, j;
        double dh, q, e;
        double[] ret = {0, 0};

        if (S[k].ordinal() <= StatType.CLOSED.ordinal()) {
            return ret;
        }

        Link link = net.getLink(k);

        q = Math.abs(Q[k]);
        dh = Math.abs(H[link.getN1()] - H[link.getN2()]);

        if (link.getType() == LinkType.PUMP) {
            j = Utilities.pumpIndex(net, k);
            e = net.Epump;

            if ((i = net.getPump(j).getEcurve()) > 0) {
                Curve curve = net.getCurve(i);
                e = Utilities.interp(curve.getNpts(),
                        curve.getX(), curve.getY(), q * net.getUcf(FieldType.FLOW));
            }
            e = Math.min(e, 100.0);
            e = Math.max(e, 1.0);
            e /= 100.0;
        } else e = 1.0;

        ret[0] = dh * q * net.SpGrav / 8.814 / e * Constants.KWperHP;
        ret[1] = e;

        return ret;
    }

    // computes new water levels in tanks after current
    // time step
    void tanklevels(long tstep) {
        int i, n;
        double dv;

        for (i = 1; i <= net.getMaxTanks(); i++) {
            Tank tank = net.getTank(i);
            if (tank.getArea() == 0.0) continue;

            n = tank.getNode();
            dv = D[n] * tstep;
            tank.setVolume(tank.getVolume() + dv);


            if (tank.getVolume() + D[n] >= tank.getVmax()) tank.setVolume(tank.getVmax());
            if (tank.getVolume() - D[n] <= tank.getVmin()) tank.setVolume(tank.getVmin());

            H[n] = tankgrade(i, tank.getVolume());
        }
    }

    // finds water volume in tank i corresponding to elev. h.
    double tankvolume(int i, double h) {
        int j;
        Tank tank = net.getTank(i);

        j = tank.getVcurve();
        if (j == 0)
            return (tank.getVmin() + (h - tank.getHMin()) * tank.getArea());
        else {
            Curve curve = net.getCurve(j);
            return (Utilities.interp(curve.getNpts(), curve.getX(), curve.getY(),
                    (h - net.getNode(tank.getNode()).getElevation()) * net.getUcf(FieldType.HEAD) / net.getUcf(FieldType.VOLUME)));
        }

    }


    // finds water level in tank i corresponding to volume v.
    double tankgrade(int i, double v) {
        int j;
        Tank tank = net.getTank(i);
        j = tank.getVcurve();
        if (j == 0)
            return (tank.getHMin() + (v - tank.getVmin()) / tank.getArea());
        else {
            Node node = net.getNode(tank.getNode());
            Curve curve = net.getCurve(j);
            return (node.getElevation() +
                    Utilities.interp(curve.getNpts(), curve.getY(), curve.getX(),
                            v * net.getUcf(FieldType.VOLUME)) / net.getUcf(FieldType.HEAD));
        }
    }

    //solves network nodal equations for heads and flows using Todini's Gradient algorithm
    double[] netsolve() {
        int iter;
        double relerr = 0.0d;
        int i;
        int errcode = 0;
        int nextcheck;
        int maxtrials;
        double newerr;
        boolean valveChange;
        int statChange;

        nextcheck = net.CheckFreq;
        RelaxFactor = 1.0;

        if (net.Statflag == StatFlag.FULL) report.writerelerr(0, 0);
        maxtrials = net.MaxIter;
        if (net.ExtraIter > 0) maxtrials += net.ExtraIter;
        iter = 1;
        while (iter <= maxtrials) {
            newcoeffs();
            errcode = smat.linsolve(net.getSections(SectType._JUNCTIONS), Aii, Aij, F);

            if (errcode < 0) break;
            if (errcode > 0) {
                if (badvalve(smat.Order[errcode]) != 0) continue;
                else break;
            }

            for (i = 1; i <= net.getSections(SectType._JUNCTIONS); i++) {
                H[i] = F[smat.Row[i]];
            }

            newerr = newflows();
            relerr = newerr;

            if (net.Statflag == StatFlag.FULL)
                report.writerelerr(iter, relerr);

            RelaxFactor = 1.0;
            valveChange = false;
            if (net.DampLimit > 0.0) {
                if (relerr <= net.DampLimit) {
                    RelaxFactor = 0.6;
                    valveChange = valvestatus();
                }
            } else valveChange = valvestatus();

            if (relerr <= net.Hacc) {
                if (iter > net.MaxIter) break;

                statChange = 0;
                if (valveChange) statChange = 1;
                if (linkstatus()) statChange = 1;
                if (pswitch()) statChange = 1;
                if (statChange == 0) break;

                nextcheck = iter + net.CheckFreq;
            } else if (iter <= net.MaxCheck && iter == nextcheck) {
                linkstatus();
                nextcheck += net.CheckFreq;
            }
            iter++;
        }

        if (errcode < 0) errcode = 101;
        else if (errcode > 0) {
            report.writehyderr(smat.Order[errcode]);
            errcode = 110;
        }

        for (i = 1; i <= net.getSections(SectType._JUNCTIONS); i++) D[i] += E[i];

        double[] retvals = new double[3];
        retvals[0] = iter;
        retvals[1] = relerr;
        retvals[2] = errcode;
        return retvals;
    }


    // Determines if a node belongs to an active control valve
    // whose setting causes an inconsistent set of eqns. If so,
    // the valve status is fixed open and a warning condition
    // is generated.
    int badvalve(int n) {
        int i, k, n1, n2;
        for (i = 1; i <= net.getSections(SectType._VALVES); i++) {

            k = net.getValve(i).getLink();
            Link link = net.getLink(k);
            n1 = link.getN1();
            n2 = link.getN2();
            if (n == n1 || n == n2) {
                if (link.getType() == LinkType.PRV ||
                        link.getType() == LinkType.PSV ||
                        link.getType() == LinkType.FCV) {
                    if (S[k] == StatType.ACTIVE) {
                        if (net.Statflag == StatFlag.FULL) {

                            report.writeline(String.format(Report.textBundle.getString("FMT61"), Utilities.clocktime(Htime), link.getId()));
                        }
                        if (link.getType() == LinkType.FCV) S[k] = StatType.XFCV;
                        else
                            S[k] = StatType.XPRESSURE;
                        return (1);
                    }
                }
                return (0);
            }
        }
        return (0);
    }


    // updates status for PRVs & PSVs whose status
    //      is not fixed to OPEN/CLOSED
    boolean valvestatus() {
        boolean change = false;

        int i, k, n1, n2;

        StatType s;
        double hset;

        for (i = 1; i <= net.getSections(SectType._VALVES); i++) {

            k = net.getValve(i).getLink();
            if (K[k] == Constants.MISSING) continue;
            Link link = net.getLink(k);
            n1 = link.getN1();
            n2 = link.getN2();
            s = S[k];


            switch (link.getType()) {
                case PRV:
                    hset = net.getNode(n2).getElevation() + K[k];
                    S[k] = prvstatus(k, s, hset, H[n1], H[n2]);
                    break;
                case PSV:
                    hset = net.getNode(n1).getElevation() + K[k];
                    S[k] = psvstatus(k, s, hset, H[n1], H[n2]);
                    break;


                default:
                    continue;
            }

            if (s != S[k]) {
                if (net.Statflag == StatFlag.FULL) report.writestatchange(k, s, S[k]);
                change = true;
            }
        }
        return (change);
    }


    //  determines new status for pumps, CVs, FCVs & pipes
    // to tanks.
    boolean linkstatus() {
        boolean change = false;
        int k,
                n1,
                n2;
        double dh;
        StatType status;

        for (k = 1; k <= net.getMaxLinks(); k++) {
            Link link = net.getLink(k);
            n1 = link.getN1();
            n2 = link.getN2();
            dh = H[n1] - H[n2];

            status = S[k];
            if (status == StatType.XHEAD || status == StatType.TEMPCLOSED) S[k] = StatType.OPEN;

            if (link.getType() == LinkType.CV) S[k] = cvstatus(S[k], dh, Q[k]);
            if (link.getType() == LinkType.PUMP && S[k].ordinal() >= StatType.OPEN.ordinal() && K[k] > 0.0)
                S[k] = pumpstatus(k, -dh);


            if (link.getType() == LinkType.FCV && K[k] != Constants.MISSING)
                S[k] = fcvstatus(k, status, H[n1], H[n2]);


            int njuncs = net.getSections(SectType._JUNCTIONS);
            if (n1 > njuncs || n2 > njuncs) tankstatus(k, n1, n2);


            if (status != S[k]) {
                change = true;
                if (net.Statflag == StatFlag.FULL) report.writestatchange(k, status, S[k]);
            }
        }
        return (change);
    }

    // updates status of a check valve.
    StatType cvstatus(StatType s, double dh, double q) {
        if (Math.abs(dh) > net.Htol) {
            if (dh < -net.Htol) return (StatType.CLOSED);
            else if (q < -net.Qtol) return (StatType.CLOSED);
            else return (StatType.OPEN);
        } else {
            if (q < -net.Qtol) return (StatType.CLOSED);
            else
                return (s);
        }
    }

    //updates status of an open pump.
    StatType pumpstatus(int k, double dh) {
        int p;
        double hmax;


        p = Utilities.pumpIndex(net, k);
        Pump pump = net.getPump(p);
        if (pump.getPtype() == PumpType.CONST_HP)
            hmax = Constants.BIG;
        else
            hmax = (K[k] * K[k]) * pump.getHMax();

        if (dh > hmax + net.Htol)
            return (StatType.XHEAD);

        return (StatType.OPEN);
    }

    // updates status of a pressure reducing valve.
    StatType prvstatus(int k, StatType s, double hset, double h1, double h2) {
        StatType status;
        double hml;
        double htol = net.Htol;

        status = s;
        if (K[k] == Constants.MISSING) return (status);
        hml = net.getLink(k).getKm() * (Q[k] * Q[k]);


        switch (s) {
            case ACTIVE:
                if (Q[k] < -net.Qtol) status = StatType.CLOSED;
                else if (h1 - hml < hset - htol) status = StatType.OPEN;
                else status = StatType.ACTIVE;
                break;
            case OPEN:
                if (Q[k] < -net.Qtol) status = StatType.CLOSED;
                else if (h2 >= hset + htol) status = StatType.ACTIVE;
                else status = StatType.OPEN;
                break;
            case CLOSED:
                if (h1 >= hset + htol
                        && h2 < hset - htol) status = StatType.ACTIVE;
                else if (h1 < hset - htol
                        && h1 > h2 + htol) status = StatType.OPEN;
                else status = StatType.CLOSED;
                break;
            case XPRESSURE:
                if (Q[k] < -net.Qtol) status = StatType.CLOSED;
                break;
        }
        return (status);
    }

    // updates status of a pressure sustaining valve.
    StatType psvstatus(int k, StatType s, double hset, double h1, double h2) {
        StatType status;
        double hml;
        double htol = net.Htol;

        status = s;
        if (K[k] == Constants.MISSING) return (status);
        hml = net.getLink(k).getKm() * (Q[k] * Q[k]);

        switch (s) {
            case ACTIVE:
                if (Q[k] < -net.Qtol) status = StatType.CLOSED;
                else if (h2 + hml > hset + htol) status = StatType.OPEN;
                else status = StatType.ACTIVE;
                break;
            case OPEN:
                if (Q[k] < -net.Qtol) status = StatType.CLOSED;
                else if (h1 < hset - htol) status = StatType.ACTIVE;
                else status = StatType.OPEN;
                break;
            case CLOSED:
                if (h2 > hset + htol
                        && h1 > h2 + htol) status = StatType.OPEN;
                else if (h1 >= hset + htol
                        && h1 > h2 + htol) status = StatType.ACTIVE;
                else status = StatType.CLOSED;
                break;
            case XPRESSURE:
                if (Q[k] < -net.Qtol) status = StatType.CLOSED;
                break;
        }
        return (status);
    }

    // updates status of a flow control valve.
    StatType fcvstatus(int k, StatType s, double h1, double h2) {
        StatType status;
        status = s;
        if (h1 - h2 < -net.Htol) status = StatType.XFCV;
        else if (Q[k] < -net.Qtol) status = StatType.XFCV;
        else if (s == StatType.XFCV && Q[k] >= K[k]) status = StatType.ACTIVE;
        return (status);
    }


    // closes link flowing into full or out of empty tank
    void tankstatus(int k, int n1, int n2) {
        int i, n;
        double h, q;


        int njuncs = net.getSections(SectType._JUNCTIONS);

        q = Q[k];
        i = n1 - njuncs;
        if (i <= 0) {
            i = n2 - njuncs;
            if (i <= 0) return;
            n = n1;
            n1 = n2;
            n2 = n;
            q = -q;
        }
        h = H[n1] - H[n2];


        Tank tank = net.getTank(i);

        Link link = net.getLink(k);
        if (tank.getArea() == 0.0 || S[k].ordinal() <= StatType.CLOSED.ordinal()) return;

        if (H[n1] >= tank.getHMax() - net.Htol) {

            if (link.getType() == LinkType.PUMP) {
                if (link.getN2() == n1) S[k] = StatType.TEMPCLOSED;
            } else if (cvstatus(StatType.OPEN, h, q) == StatType.CLOSED) S[k] = StatType.TEMPCLOSED;
        }


        if (H[n1] <= tank.getHMin() + net.Htol) {


            if (link.getType() == LinkType.PUMP) {
                if (link.getN1() == n1) S[k] = StatType.TEMPCLOSED;
            } else if (cvstatus(StatType.CLOSED, h, q) == StatType.OPEN) S[k] = StatType.TEMPCLOSED;
        }
    }

    //adjusts settings of links controlled by junction
//           pressures after a hydraulic solution is found
    boolean pswitch() {
        boolean change, anychange = false;
        int i, k, n, reset;
        StatType s;


        for (i = 1; i <= net.getSections(SectType._CONTROLS); i++) {
            Control control = net.getControl(i);

            reset = 0;
            if ((k = control.getLink()) <= 0) continue;


            if ((n = control.getNode()) > 0 && n <= net.getSections(SectType._JUNCTIONS)) {

                if (control.getType() == ControlType.LOWLEVEL
                        && H[n] <= control.getGrade() + net.Htol)
                    reset = 1;
                if (control.getType() == ControlType.HILEVEL
                        && H[n] >= control.getGrade() - net.Htol)
                    reset = 1;
            }

            Link link = net.getLink(k);

            if (reset == 1) {

                change = false;
                s = S[k];
                if (link.getType() == LinkType.PIPE) {
                    if (s != control.getStatus()) change = true;
                }
                if (link.getType() == LinkType.PUMP) {
                    if (K[k] != control.getSetting()) change = true;
                }
                if (link.getType().ordinal() >= LinkType.PRV.ordinal()) {
                    if (K[k] != control.getSetting()) change = true;
                    else if (K[k] == Constants.MISSING &&
                            s != control.getStatus()) change = true;
                }


                if (change) {
                    S[k] = control.getStatus();
                    if (link.getType().ordinal() > LinkType.PIPE.ordinal()) K[k] = control.getSetting();
                    if (net.Statflag == StatFlag.FULL) report.writestatchange(k, s, S[k]);

                    anychange = true;
                }
            }
        }
        return (anychange);
    }

    // updates link flows after new nodal heads computed
    double newflows() {
        double dh,
                dq;
        double dqsum,
                qsum;
        int k, n, n1, n2;

        int Njuncs = net.getSections(SectType._JUNCTIONS);
        int Nnodes = net.getMaxNodes();
        int Nlinks = net.getMaxLinks();

        for (n = Njuncs + 1; n <= Nnodes; n++) D[n] = 0.0;

        qsum = 0.0;
        dqsum = 0.0;

        for (k = 1; k <= Nlinks; k++) {

            Link link = net.getLink(k);
            n1 = link.getN1();
            n2 = link.getN2();
            dh = H[n1] - H[n2];
            dq = Y[k] - P[k] * dh;

            dq *= RelaxFactor;

            if (link.getType() == LinkType.PUMP) {
                n = Utilities.pumpIndex(net, k);
                if (net.getPump(n).getPtype() == PumpType.CONST_HP && dq > Q[k]) dq = Q[k] / 2.0;
            }
            Q[k] -= dq;

            qsum += Math.abs(Q[k]);
            dqsum += Math.abs(dq);

            if (S[k].ordinal() > StatType.CLOSED.ordinal()) {
                if (n1 > Njuncs) D[n1] -= Q[k];
                if (n2 > Njuncs) D[n2] += Q[k];
            }

        }

        for (k = 1; k <= Njuncs; k++) {
            if (net.getNode(k).getKe() == 0.0) continue;
            dq = emitflowchange(k);
            E[k] -= dq;
            qsum += Math.abs(E[k]);
            dqsum += Math.abs(dq);
        }

        if (qsum > net.Hacc) return (dqsum / qsum);
        else
            return (dqsum);

    }

    //computes coefficients of linearized network eqns.
    void newcoeffs() {
        int Ncoeffs = smat.getCoeffsCount();


        for (int i = 0; i <= net.getMaxNodes(); i++) {
            Aii[i] = 0.0d;
            F[i] = 0.0d;
            X[i] = 0.0d;
        }

        for (int i = 0; i <= Ncoeffs; i++)
            Aij[i] = 0.0d;

        for (int i = 0; i <= net.getMaxLinks(); i++) {
            P[i] = 0.0d;
            Y[i] = 0.0d;
        }

        linkcoeffs();
        emittercoeffs();
        nodecoeffs();
        valvecoeffs();
    }

    // computes solution matrix coefficients for links
    void linkcoeffs() {
        int k, n1, n2;


        int maxLinks = net.getMaxLinks();
        int nJunctions = net.getSections(SectType._JUNCTIONS);
        for (k = 1; k <= maxLinks; k++) {
            Link link = net.getLink(k);
            n1 = link.getN1();
            n2 = link.getN2();

            switch (link.getType()) {
                case CV:
                case PIPE:
                    pipecoeff(k);
                    break;
                case PUMP:
                    pumpcoeff(k);
                    break;
                case PBV:
                    pbvcoeff(k);
                    break;
                case TCV:
                    tcvcoeff(k);
                    break;
                case GPV:
                    gpvcoeff(k);
                    break;
                case FCV:
                case PRV:
                case PSV:

                    if (K[k] == Constants.MISSING) valvecoeff(k);
                    else continue;
                    break;
                default:
                    continue;
            }


            X[n1] -= Q[k];
            X[n2] += Q[k];
            Aij[smat.Ndx[k]] -= P[k];
            if (n1 <= nJunctions) {
                Aii[smat.Row[n1]] += P[k];
                F[smat.Row[n1]] += Y[k];
            } else F[smat.Row[n2]] += (P[k] * H[n1]);
            if (n2 <= nJunctions) {
                Aii[smat.Row[n2]] += P[k];
                F[smat.Row[n2]] -= Y[k];
            } else F[smat.Row[n1]] += (P[k] * H[n2]);
        }
    }

    //completes calculation of nodal flow imbalance (X) flow correction (F) arrays
    void nodecoeffs() {
        for (int i = 1; i <= net.getSections(SectType._JUNCTIONS); i++) {
            X[i] -= D[i];
            F[smat.Row[i]] += X[i];
        }
    }

    //computes matrix coeffs. for PRVs, PSVs & FCVs whose status is not fixed to OPEN/CLOSED
    void valvecoeffs() {
        int i, k, n1, n2;

        for (i = 1; i <= net.getSections(SectType._VALVES); i++) {
            k = net.getValve(i).getLink();
            Link link = net.getLink(k);

            if (K[k] == Constants.MISSING) continue;
            n1 = link.getN1();
            n2 = link.getN2();
            switch (link.getType()) {
                case PRV:
                    prvcoeff(k, n1, n2);
                    break;
                case PSV:
                    psvcoeff(k, n1, n2);
                    break;
                case FCV:
                    fcvcoeff(k, n1, n2);
                    break;
            }
        }
    }

    // computes matrix coeffs. for emitters
    void emittercoeffs() {
        int i;
        double ke;
        double p;
        double q;
        double y;
        double z;
        for (i = 1; i <= net.getSections(SectType._JUNCTIONS); i++) {
            if (net.getNode(i).getKe() == 0.0) continue;
            ke = Math.max(CSMALL, net.getNode(i).getKe());
            q = E[i];
            z = ke * Math.pow(Math.abs(q), net.Qexp);
            p = net.Qexp * z / Math.abs(q);
            if (p < net.RQtol) p = 1.0 / net.RQtol;
            else p = 1.0 / p;
            y = SGN(q) * z * p;
            Aii[smat.Row[i]] += p;
            F[smat.Row[i]] += y + p * net.getNode(i).getElevation();
            X[i] -= q;
        }
    }

    // computes flow change at an emitter node
    double emitflowchange(int i) {
        double ke, p;
        ke = Math.max(CSMALL, net.getNode(i).getKe());
        p = net.Qexp * ke * Math.pow(Math.abs(E[i]), (net.Qexp - 1.0));
        if (p < net.RQtol)
            p = 1 / net.RQtol;
        else
            p = 1.0 / p;
        return (E[i] / net.Qexp - p * (H[i] - net.getNode(i).getElevation()));
    }

    //computes P & Y coefficients for pipe k
    void pipecoeff(int k) {
        double hpipe,
                hml,
                ml,
                p,
                q,
                r,
                r1,
                f;


        if (S[k].ordinal() <= StatType.CLOSED.ordinal()) {
            P[k] = 1.0 / CBIG;
            Y[k] = Q[k];
            return;
        }


        Link link = net.getLink(k);

        q = Math.abs(Q[k]);
        ml = link.getKm();
        r = link.getR();
        f = 1.0;
        if (net.Formflag == FormType.DW) {
            f = DWcoeff(k);
        }
        r1 = f * r + ml;


        if (r1 * q < net.RQtol) {
            P[k] = 1.0 / net.RQtol;
            Y[k] = Q[k] / net.Hexp;
            return;
        }


        if (net.Formflag == FormType.DW) {
            hpipe = r1 * (q * q);
            p = 2.0 * r1 * q;

            p = 1.0 / p;
            P[k] = p;
            Y[k] = SGN(Q[k]) * hpipe * p;
        } else {
            hpipe = r * Math.pow(q, net.Hexp);
            p = net.Hexp * hpipe;
            if (ml > 0.0) {
                hml = ml * q * q;
                p += 2.0 * hml;
            } else hml = 0.0;
            p = Q[k] / p;
            P[k] = Math.abs(p);
            Y[k] = p * (hpipe + hml);
        }
    }

    // computes Darcy-Weisbach friction factor
    double DWcoeff(int k) {
        double q,
                f;
        double x1, x2, x3, x4,
                y1, y2, y3,
                fa, fb, r;
        double s, w;

        Link link = net.getLink(k);

        if (link.getType().ordinal() > LinkType.PIPE.ordinal()) return (1.0);
        q = Math.abs(Q[k]);
        s = net.Viscos * link.getDiameter();
        w = q / s;
        if (w >= A1) {
            y1 = A8 / Math.pow(w, 0.9);
            y2 = link.getKc() / (3.7 * link.getDiameter()) + y1;
            y3 = A9 * Math.log(y2);
            f = 1.0 / (y3 * y3);
        } else if (w > A2) {
            y2 = link.getKc() / (3.7 * link.getDiameter()) + AB;
            y3 = A9 * Math.log(y2);
            fa = 1.0 / (y3 * y3);
            fb = (2.0 + AC / (y2 * y3)) * fa;
            r = w / A2;
            x1 = 7.0 * fa - fb;
            x2 = 0.128 - 17.0 * fa + 2.5 * fb;
            x3 = -0.128 + 13.0 * fa - (fb + fb);
            x4 = r * (0.032 - 3.0 * fa + 0.5 * fb);
            f = x1 + r * (x2 + r * (x3 + x4));
        } else if (w > A4) {
            f = A3 * s / q;
        } else {
            f = 8.0;

        }
        return f;
    }


    void pumpcoeff(int k) {
        int p;
        double h0, q, r, n;

        if (S[k].ordinal() <= StatType.CLOSED.ordinal() || K[k] == 0.0) {
            P[k] = 1.0 / CBIG;
            Y[k] = Q[k];
            return;
        }

        q = Math.abs(Q[k]);
        q = Math.max(q, Constants.TINY);
        p = Utilities.pumpIndex(net, k);

        Pump pump = net.getPump(p);


        if (pump.getPtype() == PumpType.CUSTOM) {

            double ret[] = curvecoeff(pump.getHcurve(), q / K[k]);
            h0 = ret[0];
            r = ret[1];


            pump.setH0(-h0);
            pump.setR(-r);
            pump.setN(1.0);
        }


        h0 = (K[k] * K[k]) * pump.getH0();
        n = pump.getN();
        r = pump.getR() * Math.pow(K[k], 2.0 - n);
        if (n != 1.0) r = n * r * Math.pow(q, n - 1.0);


        P[k] = 1.0 / Math.max(r, net.RQtol);
        Y[k] = Q[k] / n + P[k] * h0;
    }


    // computes intercept and slope of head v. flow curve
    //            at current flow.
    double[] curvecoeff(int i, double q) {
        double h0;
        double r;
        int k1, k2, npts;
        List<Double> x, y;

        q *= net.getUcf(FieldType.FLOW);
        x = net.getCurve(i).getX();
        y = net.getCurve(i).getY();
        npts = net.getCurve(i).getNpts();

        k2 = 0;
        while (k2 < npts && x.get(k2) < q) k2++;
        if (k2 == 0) k2++;
        else if (k2 == npts) k2--;
        k1 = k2 - 1;

        r = (y.get(k2) - y.get(k1)) / (x.get(k2) - x.get(k1));
        h0 = y.get(k1) - (r) * x.get(k1);

        h0 = (h0) / net.getUcf(FieldType.HEAD);
        r = (r) * net.getUcf(FieldType.FLOW) / net.getUcf(FieldType.HEAD);

        return new double[]{h0, r};
    }

    // computes P & Y coeffs. for general purpose valve
    void gpvcoeff(int k) {
        double q, h0, r;

        if (S[k] == StatType.CLOSED)
            valvecoeff(k);
        else {

            q = Math.abs(Q[k]);
            q = Math.max(q, Constants.TINY);

            double[] ret = curvecoeff(Utilities.roundOff(K[k]), q);
            h0 = ret[0];
            r = ret[1];

            P[k] = 1.0 / Math.max(r, net.RQtol);
            Y[k] = P[k] * (h0 + r * q) * SGN(Q[k]);
        }
    }

    // computes P & Y coeffs. for pressure breaker valve
    void pbvcoeff(int k) {
        Link link = net.getLink(k);
        if (K[k] == Constants.MISSING || K[k] == 0.0)
            valvecoeff(k);

        else {
            if (link.getKm() * (Q[k] * Q[k]) > K[k])
                valvecoeff(k);
            else {
                P[k] = CBIG;
                Y[k] = K[k] * CBIG;
            }
        }
    }

    //computes P & Y coeffs. for throttle control valve
    void tcvcoeff(int k) {
        double km;
        Link link = net.getLink(k);

        km = link.getKm();


        if (K[k] != Constants.MISSING)
            link.setKm(0.02517 * K[k] / Math.pow(link.getDiameter(), 4));//( SQR(Link[k].Diam)*SQR(Link[k].Diam)));


        valvecoeff(k);

        link.setKm(km);
    }

    // computes solution matrix coeffs. for pressure
    // reducing valves
    void prvcoeff(int k, int n1, int n2) {
        int i, j;
        double hset;
        i = smat.Row[n1];
        j = smat.Row[n2];
        hset = net.getNode(n2).getElevation() + K[k];

        if (S[k] == StatType.ACTIVE) {

            P[k] = 0.0;
            Y[k] = Q[k] + X[n2];
            F[j] += (hset * CBIG);
            Aii[j] += CBIG;
            if (X[n2] < 0.0) F[i] += X[n2];
            return;
        }

        valvecoeff(k);
        Aij[smat.Ndx[k]] -= P[k];
        Aii[i] += P[k];
        Aii[j] += P[k];
        F[i] += (Y[k] - Q[k]);
        F[j] -= (Y[k] - Q[k]);
    }

    // computes solution matrix coeffs. for pressure
    // sustaining valve
    void psvcoeff(int k, int n1, int n2) {
        int i, j;
        double hset;
        i = smat.Row[n1];
        j = smat.Row[n2];
        hset = net.getNode(n1).getElevation() + K[k];

        if (S[k] == StatType.ACTIVE) {
            P[k] = 0.0;
            Y[k] = Q[k] - X[n1];
            F[i] += (hset * CBIG);
            Aii[i] += CBIG;
            if (X[n1] > 0.0) F[j] += X[n1];
            return;
        }

        valvecoeff(k);
        Aij[smat.Ndx[k]] -= P[k];
        Aii[i] += P[k];
        Aii[j] += P[k];
        F[i] += (Y[k] - Q[k]);
        F[j] -= (Y[k] - Q[k]);
    }

    // computes solution matrix coeffs. for flow control
    // valve
    void fcvcoeff(int k, int n1, int n2) {
        int i, j;
        double q;
        q = K[k];
        i = smat.Row[n1];
        j = smat.Row[n2];

        if (S[k] == StatType.ACTIVE) {
            X[n1] -= q;
            F[i] -= q;
            X[n2] += q;
            F[j] += q;
            P[k] = 1.0 / CBIG;
            Aij[smat.Ndx[k]] -= P[k];
            Aii[i] += P[k];
            Aii[j] += P[k];
            Y[k] = Q[k] - q;
        } else {
            valvecoeff(k);
            Aij[smat.Ndx[k]] -= P[k];
            Aii[i] += P[k];
            Aii[j] += P[k];
            F[i] += (Y[k] - Q[k]);
            F[j] -= (Y[k] - Q[k]);
        }
    }


    // computes solution matrix coeffs. for a completely
    // open, closed, or throttled control valve.
    void valvecoeff(int k) {
        double p;

        // Valve is closed. Use a very small matrix coeff.
        if (S[k].ordinal() <= StatType.CLOSED.ordinal()) {
            P[k] = 1.0 / CBIG;
            Y[k] = Q[k];
            return;
        }

        // Account for any minor headloss through the valve
        if (net.getLink(k).getKm() > 0.0) {
            p = 2.0 * net.getLink(k).getKm() * Math.abs(Q[k]);
            if (p < net.RQtol) p = net.RQtol;
            P[k] = 1.0 / p;
            Y[k] = Q[k] / 2.0;
        } else {
            P[k] = 1.0 / net.RQtol;
            Y[k] = Q[k];
        }
    }

    static private double SGN(double val) {
        return val < 0 ? -1 : 1;
    }


    public int open(File f) {
        try {
            // File tempFile = File.createTempFile("epanetold",".hyd");
            HydFile = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
        } catch (IOException ex) {
            return 305;
        }

        int magic = Constants.MAGICNUMBER;
        int version = Constants.VERSION;
        int[] nsize = new int[6];

        nsize[0] = net.getMaxNodes();
        nsize[1] = net.getMaxLinks();
        nsize[2] = net.getMaxTanks();
        nsize[3] = net.getSections(SectType._PUMPS);
        nsize[4] = net.getSections(SectType._VALVES);
        nsize[5] = (int) net.Dur;

        try {
            HydFile.writeInt(new Integer(magic));
            HydFile.writeInt(version);
            for (int i = 0; i < 6; i++)
                HydFile.writeInt(nsize[i]);
        } catch (IOException ex) {
            return 308;
        }

        return 0;
    }

    // close hydraulic dump file.
    public void close() {
        if (HydFile == null) return;
        try {
            HydFile.close();
        } catch (IOException ignored) {
        }
    }

    // Save current step simulation results in the temp hydfile.
    private int savehyd(long htime) {
        int maxNodes = net.getMaxNodes();
        int maxLinks = net.getMaxLinks();

        if (HydFile == null) return 0;
        try {
            HydFile.writeInt((int) htime);

            for (int i = 1; i <= maxNodes; i++)
                HydFile.writeFloat((float) D[i]);

            for (int i = 1; i <= maxNodes; i++)
                HydFile.writeFloat((float) H[i]);


            for (int i = 1; i <= maxLinks; i++) {
                if (S[i].ordinal() <= StatType.CLOSED.ordinal())
                    HydFile.writeFloat((float) 0.0);

                else
                    HydFile.writeFloat((float) Q[i]);
            }

            for (int i = 1; i <= maxLinks; i++)
                HydFile.writeFloat((float) S[i].ordinal());

            for (int i = 1; i <= maxLinks; i++)
                HydFile.writeFloat((float) K[i]);
        } catch (IOException ex) {
            return 308;
        }

        return 0;
    }

    // Saves next hydraulic timestep to file HydFile in binary format.
    private int savehydstep(long hydstep) {
        if (HydFile == null) return 0;
        try {
            HydFile.writeInt((int) hydstep);
            if (hydstep == 0)
                HydFile.writeChar(0x1A);
        } catch (IOException ex) {
            return 308;
        }
        return 0;
    }

    public long getHtime() {
        return Htime;
    }

}
