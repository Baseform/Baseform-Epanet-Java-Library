package org.addition.epanetold;

import org.addition.epanetold.Types.EnumVariables.HdrType;
import org.addition.epanetold.Types.EnumVariables.QualType;
import org.addition.epanetold.Types.EnumVariables.ReportFlag;
import org.addition.epanetold.Types.EnumVariables.StatFlag;

import java.io.*;

public class Epanet {


    private InputReader reader; // Input reader.

    private Network network; // Network storage.
    private Hydraulic hydraulics; // Hydraulics solver.
    private Rules rules; // Rules parser and execution.
    private Report report; // Report.
    private SparseMatrix smatrix; // Sparse matrix.
    private Quality mQuality; // Quality solver.
    private Output mOutput; // Binary output generator.

    int Haltflag;       // Flag used to halt taking further time steps.
    char Warnflag;      // Warning flag

    File mHydFile;      // Abstract representation for the hydraulic file.
    File mRptFile;      // Abstract representation for the report file.
    File mOutFile;      // Abstract representation for the output file.
    File mInpFile;      // Abstract representation for the INP file.




    public int getHaltflag() {
        return Haltflag;
    }

    public void setHaltflag(int haltflag) {
        Haltflag = haltflag;
    }

    public void setWarnflag(char warnflag) {
        Warnflag = warnflag;
    }

    public Network getNetwork() {
        return network;
    }

    public InputReader getInputReader() {
        return reader;
    }

    public Hydraulic getHydraulicsSolver() {
        return hydraulics;
    }

    Rules getRules() {
        return rules;
    }

    public Report getReport() {
        return report;
    }

    SparseMatrix getSparseMatrix() {
        return smatrix;
    }

    public Output getOutput() {
        return mOutput;
    }

    public Quality getQuality() {
        return mQuality;
    }


    public Epanet() {
        prepareEpanet();
    }

    public Epanet(boolean console_msg) {
        prepareEpanet();
        Utilities.setConsoleMsg(console_msg);
    }


    private void prepareEpanet() {
        network = new Network(this);
        reader = new InputReader(this);
        hydraulics = new Hydraulic(this);
        rules = new Rules(this);
        report = new Report(this);
        smatrix = new SparseMatrix(this);
        mQuality = new Quality(this);
        mOutput = new Output(this);

        network.loadDependencies();
        reader.loadDependencies();
        hydraulics.loadDependencies();
        rules.loadDependencies();
        report.loadDependencies();
        smatrix.loadDependencies();
        mQuality.loadDependencies();
        mOutput.loadDependencies();
        network.setDefaults();
    }


    public int setReportFile(String filename) {
        mRptFile = new File(filename);
        return report.open(mRptFile);
    }

    public int setHydFilename(String filename) {
        mHydFile = new File(filename);
        return 0;
    }

    public int setOutFilename(String filename) {
        mOutFile = new File(filename);
        return 0;
    }

    public int loadINP(String filename) {
        int errCode = 0;
        mInpFile = new File(filename);
        Utilities.writecon(Report.textBundle.getString("FMT02"));
        if (mInpFile.exists() && mInpFile.canRead()) {
            errCode = Utilities.ERRCODE(errCode, reader.netsize(mInpFile));
            if (errCode == 200)
                return 200;
            errCode = Utilities.ERRCODE(errCode, network.allocdata());
            errCode = Utilities.ERRCODE(errCode, reader.getdata(mInpFile));
        } else
            errCode = 302;

        return errCode;
    }

    public int prepareNetwork() {
        int errCode = 0;

        network.adjustData();
        network.initUnits();

        network.Nodeflag = ReportFlag.TRUE;
        network.Linkflag = ReportFlag.TRUE;
        network.Summaryflag = true;
        network.Energyflag = true;
        network.Statflag = StatFlag.FULL;

        errCode = Utilities.ERRCODE(errCode, network.initTanks());

        if (errCode == 0)
            network.convertUnits();

        return errCode;
    }

    public int simulateHydraulics(boolean progressbar) {
        int errCode = 0;

        report.writelogo();

        errCode = Utilities.ERRCODE(errCode, hydraulics.openhyd());
        errCode = Utilities.ERRCODE(errCode, hydraulics.open(mHydFile));

        if (network.Statflag != StatFlag.FALSE)
            report.writeheader(HdrType.STATHDR, false);

        if (errCode == 0) {
            long tstep;
            hydraulics.inithyd();//Constants.EN_SAVE / Constants.EN_INITFLOW);

            if (network.Summaryflag) report.writesummary();
            Utilities.writecon(Report.textBundle.getString("FMT14"));

            do {
                errCode = hydraulics.runhyd();

                if (errCode >= 101) {
                    report.errmsg(errCode);
                    break;
                }

                long[] ret = hydraulics.nexthyd();

                tstep = ret[0];
                errCode = (int) ret[1];

                if (errCode >= 101) {
                    report.errmsg(errCode);
                    break;
                }

            }
            while (tstep > 0);

        }

        report.writetime(Report.textBundle.getString("FMT105"));
        hydraulics.close();
        return (errCode);
    }

    public int simulateHydraulics2(BufferedWriter buf,boolean progressbar) {
        int errCode = 0;

        report.writelogo();

        errCode = Utilities.ERRCODE(errCode, hydraulics.openhyd());
        errCode = Utilities.ERRCODE(errCode, hydraulics.open(mHydFile));

        if (network.Statflag != StatFlag.FALSE)
            report.writeheader(HdrType.STATHDR, false);

        if (errCode == 0) {
            long tstep;
            hydraulics.inithyd();//Constants.EN_SAVE / Constants.EN_INITFLOW);

            if (network.Summaryflag) report.writesummary();
            Utilities.writecon(Report.textBundle.getString("FMT14"));

            do {
                errCode = hydraulics.runhyd2(buf);

                if (errCode >= 101) {
                    report.errmsg(errCode);
                    break;
                }

                long[] ret = hydraulics.nexthyd();

                tstep = ret[0];
                errCode = (int) ret[1];

                if (errCode >= 101) {
                    report.errmsg(errCode);
                    break;
                }

            }
            while (tstep > 0);

        }

        report.writetime(Report.textBundle.getString("FMT105"));
        hydraulics.close();
        return (errCode);
    }

    public int simulateQuality() {
        int errCode = 0;

        mQuality.openqual();
        mQuality.initqual();

        errCode = Utilities.ERRCODE(errCode, mOutput.open(mOutFile));
        if (network.Qualflag != QualType.NONE)
            Utilities.writecon(Report.textBundle.getString("FMT15"));
        else
            Utilities.writecon(Report.textBundle.getString("FMT16"));

        DataInputStream hydStream;
        try {
            hydStream = new DataInputStream(new BufferedInputStream(new FileInputStream(mHydFile)));
            hydStream.skipBytes(8 * 4);
        } catch (IOException e) {
            return 307;
        }

        if (errCode == 0) {
            long tstep;
            do {
                long[] ret = mQuality.runqual(hydStream);

                errCode = (int) ret[0];

                if (errCode >= 101) {
                    report.errmsg(errCode);
                    break;
                }

                tstep = mQuality.nextqual(hydStream);

                if (errCode >= 101) {
                    report.errmsg(errCode);
                    break;
                }
            }
            while (tstep > 0);

        }

        try {
            hydStream.close();
        } catch (IOException ignored) {

        }

        mOutput.close();

        return 0;
    }


    public int completeReport() {
        DataInputStream outStream;

        try {
            outStream = new DataInputStream(new BufferedInputStream(new FileInputStream(mOutFile)));
        } catch (IOException e) {
            return 304;
        }

        if (network.Energyflag)
            report.writeenergy();

        report.writeresults(outStream, mQuality.Nperiods);

        try {
            outStream.close();
        } catch (IOException ignored) {

        }

        return 0;
    }

    public int close() {
        report.close();
        return 0;
    }


    public File getHydFile() {
        return mHydFile;
    }

    public File getOutFile() {
        return mOutFile;
    }


    public File getInpFile() {
        return mInpFile;
    }
}
