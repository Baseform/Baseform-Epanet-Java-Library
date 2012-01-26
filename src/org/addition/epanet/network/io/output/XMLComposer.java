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

package org.addition.epanet.network.io.output;

import com.thoughtworks.xstream.XStream;
import org.addition.epanet.util.ENException;
import org.addition.epanet.network.Network;
import org.addition.epanet.network.io.input.XMLParser;

import java.io.*;
import java.util.zip.GZIPOutputStream;

public class XMLComposer extends OutputComposer {
    private boolean gzip;

    public XMLComposer(boolean gzip) {
        super();
        this.gzip = gzip;
    }

    @Override
    public void composer(Network net, File f) throws ENException {
        XStream xStream = XMLParser.X_STREAM;
        try {
            OutputStream os = !gzip ? new FileOutputStream(f) : new GZIPOutputStream(new FileOutputStream(f));
            Writer w = new OutputStreamWriter(os,"UTF-8");
            xStream.toXML(net,w);
            w.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new ENException(308);
        }
    }
}
