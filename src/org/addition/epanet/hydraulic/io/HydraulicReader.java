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

package org.addition.epanet.hydraulic.io;


import org.addition.epanet.network.FieldsMap;
import org.addition.epanet.util.ENException;

import java.io.*;
import java.util.Iterator;

/**
 * Hydraulic binary file reader class.
 */
public class HydraulicReader implements Iterable<AwareStep> {

    private AwareStep.HeaderInfo headerInfo;


    /**
     * Current hydraulic step snapshot.
     */
    private AwareStep curStep;

    /**
     * File input stream.
     */
    private DataInput inputStream;


    public HydraulicReader(DataInput inputStream) throws IOException, ENException {
        this.inputStream = inputStream;
        headerInfo = AwareStep.readHeader(inputStream);
    }



    /**
     * Read step data from file with a given time instant — it assumes the requested timestep is the same or after the current one.
     *
     * @param time Step instant.
     * @return Reference to step snapshot.
     * @throws IOException
     */
    public AwareStep getStep(long time) throws IOException {
        if (curStep != null) {
            if (curStep.getTime() == time) return curStep;
        }
        while (curStep==null || curStep.getTime() < time)
            curStep = new AwareStep(inputStream, headerInfo);
        return curStep.getTime() >= time ? curStep : null;

    }

    /**
     * Close the inputStream.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        if (inputStream instanceof Closeable) {
            ((Closeable) inputStream).close();
        }
    }


    /**
     * Get the epanet hydraulic file version.
     *
     * @return Version number.
     */
    public int getVersion() {
        return headerInfo.version;
    }

    /**
     * Get the number of nodes in the file.
     *
     * @return Number of nodes.
     */
    public int getNodes() {
        return headerInfo.nodes;
    }

    /**
     * Get the number of links in the file.
     *
     * @return Number of links.
     */
    public int getLinks() {
        return headerInfo.links;
    }

    /**
     *
     * @return
     */
    public long getReportStart() {
        return headerInfo.rstart;
    }

    /**
     *
     * @return
     */
    public long getReportStep() {
        return headerInfo.rstep;
    }

    /**
     *
     * @return
     */
    public long getDuration() {
        return headerInfo.duration;
    }

    /**
     * Get step snapshot iterator.
     *
     * @return StepSnapshot iterator.
     */
    public Iterator<AwareStep> iterator() {
        return new SnapshotIterator();
    }

    /**
     * Step snapshot iterator class
     */
    private class SnapshotIterator implements Iterator<AwareStep> {

        /**
         * Initialize snapshot iterator.
         */
        private SnapshotIterator() {
        }

        /**
         * Checks for available steps.
         *
         * @return Step availability.
         */
        public boolean hasNext() {
            return curStep == null || curStep.getStep() != 0;
        }

        /**
         * Return step and advance to the next one.
         *
         * @return Reference to step snapshot.
         */
        public AwareStep next() {
            try {
                return curStep = new AwareStep(inputStream, headerInfo);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * not implemented
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }


}
