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

package org.addition.epanet.network.io.input;

import org.addition.epanet.util.ENException;
import org.addition.epanet.network.Network;

import java.io.File;
import java.util.logging.Logger;

/**
 * Network conversion units only class.
 */
public class NullParser extends InputParser{
    protected NullParser(Logger log) {
        super(log);
    }

    @Override
    public Network parse(Network net, File f) throws ENException {
        adjust(net);
        net.getFieldsMap().prepare(net.getPropertiesMap().getUnitsflag(),
                net.getPropertiesMap().getFlowflag(),
                net.getPropertiesMap().getPressflag(),
                net.getPropertiesMap().getQualflag(),
                net.getPropertiesMap().getChemUnits(),
                net.getPropertiesMap().getSpGrav(),
                net.getPropertiesMap().getHstep());
        convert(net);
        return net;
    }
}
