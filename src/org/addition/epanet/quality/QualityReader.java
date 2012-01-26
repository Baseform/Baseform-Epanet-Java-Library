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

package org.addition.epanet.quality;


import org.addition.epanet.network.FieldsMap;
import org.addition.epanet.util.ENException;

import java.io.*;
import java.util.Iterator;

/**
 * Binary quality file reader class.
 */
public class QualityReader implements Iterable<QualityReader.Step> {

    /**
     * Single step of the quality simulation, with the quality value for each
     * link and node.
     */
    public class Step {
        /**
         * Species quality values in the links
         */
        private final float[] linkQ;

        /**
         * Species quality values in the nodes
         */
        private final float[] nodeQ;

        /**
         * Constructor
         *
         * @param linkCount number of links
         * @param nodeCount number of nodes
         */
        private Step(int linkCount, int nodeCount) {
            linkQ = new float[linkCount];
            nodeQ = new float[nodeCount];
        }

        /**
         * Get link quality values in user units.
         *
         * @param id Link sequential identification number.
         * @return Species concentration, trace or age value in user units.
         */
        public float getLinkQuality(int id) throws ENException {
            return (float) fMap.revertUnit(FieldsMap.Type.QUALITY, linkQ[id]);
        }

        /**
         * Get node quality values in user units.
         *
         * @param id Link sequential identification number.
         * @return Specie concentration, trace or age value in user units.
         */
        public float getNodeQuality(int id) throws ENException {
            return (float) fMap.revertUnit(FieldsMap.Type.QUALITY, nodeQ[id]);
        }

        /**
         * Read quality data from file stream.
         *
         * @throws IOException
         */
        private void read() throws IOException {
            for (int i = 0; i < nodeQ.length; i++)
                nodeQ[i] = inputStream.readFloat();

            for (int i = 0; i < linkQ.length; i++)
                linkQ[i] = inputStream.readFloat();

        }


        /**
         * Get node quality values in user units.
         *
         * @param id Link sequential identification number.
         * @return Species concentration, trace or age value in simulation units.
         */
//        public float getRawNodeQuality(int id) throws ENException {
//            return (float) fMap.revertUnit(FieldsMap.Type.QUALITY, nodeQ[id]);
//        }

        /**
         * Get link quality values in user units.
         *
         * @param id Link sequential identification number.
         * @return Species concentration, trace or age value in simulation units.
         */
//        public float getRawLinkQuality(int id) throws ENException {
//            return (float) fMap.revertUnit(FieldsMap.Type.QUALITY, linkQ[id]);
//        }

    }

    /**
     * Step snapshot iterator class
     */
    private class StepIterator implements Iterator<Step> {
        /**
         * Current step number.
         */
        int current;

        /**
         * Initialize iterator.
         */
        private StepIterator() {
            current = 0;
        }

        /**
         * Check for available steps to read.
         *
         * @return Step availability.
         */
		public boolean hasNext() {
            return current < nPeriods;
        }

        /**
         * Read step data and advance to the next.
         *
         * @return Reference to step snapshot.
         */
		public Step next() {
            current++;
            try {
                qStep.read();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return qStep;
        }

        /**
         * Not implemented.
         */
		public void remove() {
            throw new RuntimeException("not implemented");
        }

    }

    /**
     * Units conversion map.
     */
    private final FieldsMap fMap;

    /**
     * input stream
     */
    private DataInputStream inputStream;

    /**
     * Number of links.
     */
    private int linkCount;

    /**
     * Number of nodes.
     */
    private int nodeCount;

    /**
     * Number of report periods stored in this file.
     */
    private int nPeriods;

    /**
     * Current quality step snapshot.
     */
    private Step qStep;

    /**
     * Class constructor
     */
    public QualityReader(FieldsMap fMap) {
        this.fMap = fMap;
    }

    /**
     * Close the inputStream.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        inputStream.close();
    }

    /**
     * Get the number of links in the file.
     *
     * @return Number of links.
     */
    public int getLinks() {
        return linkCount;
    }

    /**
     * Get the number of nodes in the file.
     *
     * @return Number of nodes.
     */
    public int getNodes() {
        return nodeCount;
    }


    /**
     * Get the number of reported quality step snapshots in the file.
     *
     * @return Number of periods.
     */
    public int getPeriods() {
        return nPeriods;
    }

    /**
     * Get quality step snapshot iterator.
     *
     * @return StepSnapshot iterator.
     */
	public Iterator<Step> iterator() {
        return new StepIterator();
    }

    /**
     * @param qualFile Abstract representation of the quality file.
     */
    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public void open(File qualFile) throws IOException {

        // Read the last 4 bytes which contain the number of periods
        inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(qualFile)));
        inputStream.skip(qualFile.length() - Integer.SIZE / 8);
        nPeriods = inputStream.readInt();
        inputStream.close();

        inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(qualFile)));
        nodeCount = inputStream.readInt();
        linkCount = inputStream.readInt();
        qStep = new Step(linkCount, nodeCount);
    }
}
