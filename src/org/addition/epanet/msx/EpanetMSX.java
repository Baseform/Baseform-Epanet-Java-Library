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

package org.addition.epanet.msx;


import org.addition.epanet.util.ENException;
import org.addition.epanet.msx.Structures.Species;

import java.io.File;
import java.io.IOException;

public class EpanetMSX {
    InpReader mReader;
    Project mProject;
    Network mNetwork;
    Report mReport;
    TankMix mTankMix;

    Chemical mChemical;
    Quality mQuality;
    ENToolkit2 mENToolkit;
    Output mOutput;
    private transient boolean running;
    private transient Thread runningThread;


    public Network getNetwork() {
        return mNetwork;
    }

    public int getNPeriods() {
        return mNetwork.getNperiods();
    }

    public long getResultsOffset() {
        return mOutput.ResultsOffset;
    }

    public long getQTime() {
        return mNetwork.getQtime();
    }

    public String[] getSpeciesNames() {
        Species[] spe = mNetwork.getSpecies();
        String[] ret = new String[spe.length - 1];
        for (int i = 1; i < spe.length; i++) {
            ret[i - 1] = spe[i].getId();
        }
        return ret;
    }


    public EpanetMSX(ENToolkit2 toolkit) {
        mReader = new InpReader();
        mProject = new Project();
        mNetwork = new Network();
        mReport = new Report();
        mTankMix = new TankMix();

        mChemical = new Chemical();
        mQuality = new Quality();
        mENToolkit = toolkit;
        mOutput = new Output();

        mReader.loadDependencies(this);
        mProject.loadDependencies(this);
        mReport.loadDependencies(this);
        mTankMix.loadDependencies(this);

        mChemical.loadDependencies(this);
        mQuality.loadDependencies(this);
        mOutput.loadDependencies(this);
    }


    InpReader getReader() {
        return mReader;
    }

    Project getProject() {
        return mProject;
    }


    public Report getReport() {
        return mReport;
    }

    TankMix getTankMix() {
        return mTankMix;
    }

    Chemical getChemical() {
        return mChemical;
    }

    Quality getQuality() {
        return mQuality;
    }

    //public Epanet getEpanet() {
    //    return mEpanet;
    //}

    public ENToolkit2 getENToolkit() {
        return mENToolkit;
    }

    public Output getOutput() {
        return mOutput;
    }

    public int load(File msxFile) throws IOException {
        int err = 0;
        err = Utilities.CALL(err, mProject.MSXproj_open(msxFile));
        err = Utilities.CALL(err, mQuality.MSXqual_open());
        return err;
    }


    public int run(File outFile) throws IOException, ENException {
        int err = 0;
        boolean halted = false;
        if (running) throw new IllegalStateException("Already running");

        runningThread = Thread.currentThread();
        running = true;
        try {
            mQuality.MSXqual_init();

            mOutput.MSXout_open(outFile);

            long oldHour = -1, newHour = 0;

            long[] t_temp = new long[1];
            long[] t_left = new long[1];
            do {
                if (oldHour != newHour) {
                    //writeCon(String.format("\r  o Computing water quality at hour %-4d", newHour));
                    oldHour = newHour;
                }
                err = mQuality.MSXqual_step(t_temp, t_left);
                newHour = t_temp[0] / 3600;
                if(!running && t_left[0]>0)
                    halted = true;
            } while (running && err == 0 && t_left[0] > 0);


        } finally {
            running = false;
            runningThread = null;
        }

        if(halted)
            throw new ENException(1000);

        return err;
    }


    private void writeCon(String str) {
        System.out.print(str);
    }

    public void stopRunning() throws InterruptedException {
        running = false;
        if (runningThread != null && runningThread.isAlive())
            runningThread.join(1000);
    }
}
