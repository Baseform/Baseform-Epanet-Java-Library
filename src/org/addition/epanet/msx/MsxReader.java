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


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class MsxReader {
    private long nodeBytesPerPeriod;
    private long linkBytesPerPeriod;
    private long resultsOffset;
    private int nNodes;
    private int nLinks;

    RandomAccessFile ouputRaf;

    public MsxReader(int nodes, int links, int species, long resultsOffset) {
        this.nLinks = links;
        this.nNodes = nodes;
        this.resultsOffset = resultsOffset;
        nodeBytesPerPeriod = nNodes * species * 4;
        linkBytesPerPeriod = nLinks * species * 4;
    }

    public void open(File output) throws FileNotFoundException {
        ouputRaf = new RandomAccessFile(output,"r");

    }

    public void close() throws IOException {
        ouputRaf.close();
    }
    public float getNodeQual(int period, int node, int specie)
    {
        float c=0.0f;
        long bp = resultsOffset + period * (nodeBytesPerPeriod + linkBytesPerPeriod);
        bp += ((specie-1)*nNodes + (node-1)) * 4;

        try {
            ouputRaf.seek(bp);
            c = ouputRaf.readFloat();
        } catch (IOException e) {}

        return c;
    }

    // retrieves a result for a specific link from the MSX binary output file.
    public float getLinkQual(int period, int node, int specie)
    {
        float c=0.0f;
        long bp = resultsOffset + ((period+1)* nodeBytesPerPeriod) + (period* linkBytesPerPeriod);
        bp += ((specie-1)*nLinks + (node-1)) * 4;

        try {
            ouputRaf.seek(bp);
            c = ouputRaf.readFloat();
        } catch (IOException e) {}

        return c;
    }
}
