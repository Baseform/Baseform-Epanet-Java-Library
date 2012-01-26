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

import org.addition.epanet.msx.Structures.Pipe;
import org.addition.epanet.msx.EnumTypes.*;

public class TankMix {

    private Chemical chemical;
    private Network  MSX;
    private Quality  quality;

    public void loadDependencies(EpanetMSX epa){
        chemical = epa.getChemical();
        MSX = epa.getNetwork();
        quality = epa.getQuality();
    }

    /**
     * computes new WQ at end of time step in a completely mixed tank
     * (after contents have been reacted).
     */
    void  MSXtank_mix1(int i, double vIn, double cIn[], long dt)
    {
        int    k, m, n;
        double c;
        Pipe seg;

        // blend inflow with contents
        n = MSX.Tank[i].getNode();
        k = MSX.Nobjects[ObjectTypes.LINK.id] + i;
        seg = MSX.Segments[k].getFirst();//get(0);
        for (m=1; m<=MSX.Nobjects[ObjectTypes.SPECIES.id]; m++)
        {
            if ( MSX.Species[m].getType() != SpeciesType.BULK )
                continue;

            c = seg.getC()[m];

            if (MSX.Tank[i].getV() > 0.0)
                c += (cIn[m] - c)*vIn/MSX.Tank[i].getV();
            else
                c = cIn[m];

            c = Math.max(0.0, c);
            seg.getC()[m] = c;
            MSX.Tank[i].getC()[m] = c;
        }

        // update species equilibrium
        if ( vIn > 0.0 )
            chemical.MSXchem_equil(ObjectTypes.NODE, MSX.Tank[i].getC());
    }


    /**
     * 2-compartment tank model
      */
    void  MSXtank_mix2(int i, double vIn, double cIn[], long dt)
    {
        int     k, m, n;
        long    tstep,                     // Actual time step taken
                tstar;                     // Time to fill or drain a zone
        double  qIn,                       // Inflow rate
                qOut,                      // Outflow rate
                qNet;                      // Net flow rate
        double  c, c1, c2;                 // Species concentrations
        Pipe    seg1,                      // Mixing zone segment
                seg2;                      // Ambient zone segment

        // Find inflows & outflows
        n = MSX.Tank[i].getNode();
        qNet = MSX.D[n];
        qIn = vIn/(double)dt;
        qOut = qIn - qNet;

        // Get segments for each zone
        k = MSX.Nobjects[ObjectTypes.LINK.id] + i;
        seg1 = MSX.Segments[k].getFirst();//get(0);
        seg2 = MSX.Segments[k].getLast();//get(MSX.Segments[k].size()-1);

        // Case of no net volume change
        if ( Math.abs(qNet) < Constants.TINY )
            return;

        // Case of net filling (qNet > 0)
        else if (qNet > 0.0)
        {
            // Case where ambient zone empty & mixing zone filling
            if (seg2.getV() <= 0.0)
            {
                // Time to fill mixing zone
                tstar = (long) ((MSX.Tank[i].getvMix() - (seg1.getV()))/qNet);
                tstep = Math.min(dt, tstar);

                for (m=1; m<=MSX.Nobjects[ObjectTypes.SPECIES.id]; m++)
                {
                    if ( MSX.Species[m].getType() != SpeciesType.BULK ) continue;

                    // --- new quality in mixing zone
                    c = seg1.getC()[m];
                    if (seg1.getV() > 0.0) seg1.getC()[m] += qIn*tstep*(cIn[m]-c)/(seg1.getV());
                    else seg1.getC()[m] = cIn[m];
                    seg1.getC()[m] = Math.max(0.0, seg1.getC()[m]);
                    seg2.getC()[m] = 0.0;
                }

                // New volume of mixing zone
                seg1.setV(seg1.getV() + qNet*tstep);

                // Time during which ambient zone fills
                dt -= tstep;
            }

            // Case where mixing zone full & ambient zone filling
            if (dt > 1)
            {
                for (m=1; m<=MSX.Nobjects[ObjectTypes.SPECIES.id]; m++)
                {
                    if ( MSX.Species[m].getType() != SpeciesType.BULK ) continue;

                    // --- new quality in mixing zone
                    c1 = seg1.getC()[m];
                    seg1.getC()[m] += qIn * dt * (cIn[m] - c1) / (seg1.getV());
                    seg1.getC()[m] = Math.max(0.0, seg1.getC()[m]);

                    // --- new quality in ambient zone
                    c2 = seg2.getC()[m];
                    if (seg2.getV() <= 0.0)
                        seg2.getC()[m] = seg1.getC()[m];
                    else
                        seg2.getC()[m] += qNet * dt * ((seg1.getC()[m]) - c2) / (seg2.getV());
                    seg2.getC()[m] = Math.max(0.0, seg2.getC()[m]);
                }

                // New volume of ambient zone
                seg2.setV( seg2.getV() + qNet*dt);
            }
            if ( seg1.getV() > 0.0 ) chemical.MSXchem_equil(ObjectTypes.NODE, seg1.getC());
            if ( seg2.getV() > 0.0 ) chemical.MSXchem_equil(ObjectTypes.NODE, seg2.getC());
        }

        // Case of net emptying (qnet < 0)
        else if ( qNet < 0.0 && seg1.getV() > 0.0 )
        {
            // Case where mixing zone full & ambient zone draining
            if ((seg2.getV()) > 0.0)
            {

                // Time to drain ambient zone
                tstar = (long)(seg2.getV()/-qNet);
                tstep = Math.min(dt, tstar);

                for (m=1; m<=MSX.Nobjects[ObjectTypes.SPECIES.id]; m++)
                {
                    if ( MSX.Species[m].getType() != SpeciesType.BULK ) continue;
                    c1 = seg1.getC()[m];
                    c2 = seg2.getC()[m];

                    // New mizing zone quality (affected by both external inflow
                    // and drainage from the ambient zone
                    seg1.getC()[m] += (qIn*cIn[m] - qNet*c2 - qOut*c1)*tstep/(seg1.getV());
                    seg1.getC()[m] = Math.max(0.0, seg1.getC()[m]);
                }

                // New ambient zone volume
                seg2.setV(seg2.getV() + qNet*tstep);
                seg2.setV(Math.max(0.0, seg2.getV()));

                // Time during which mixing zone empties
                dt -= tstep;
            }

            // Case where ambient zone empty & mixing zone draining
            if (dt > 1)
            {
                for (m=1; m<=MSX.Nobjects[ObjectTypes.SPECIES.id]; m++)
                {
                    if ( MSX.Species[m].getType() != SpeciesType.BULK ) continue;

                    // New mixing zone quality (affected by external inflow only)
                    c = seg1.getC()[m];
                    seg1.getC()[m] += qIn*dt*(cIn[m]-c)/(seg1.getV());
                    seg1.getC()[m] = Math.max(0.0, seg1.getC()[m]);
                    seg2.getC()[m] = 0.0;
                }

                // New volume of mixing zone
                seg1.setV( seg1.getV() + qNet*dt);
                seg1.setV( Math.max(0.0, seg1.getV()));
            }
            if ( seg1.getV() > 0.0 ) chemical.MSXchem_equil(ObjectTypes.NODE, seg1.getC());
        }

        // Use quality of mixed compartment (seg1) to represent quality
        // of tank since this is where outflow begins to flow from
        for (m=1; m<=MSX.Nobjects[ObjectTypes.SPECIES.id]; m++)
            MSX.Tank[i].getC()[m] = seg1.getC()[m];
    }


    /**
    * Computes concentrations in the segments that form a
    * first-in-first-out (FIFO) tank model.
    */
    void  MSXtank_mix3(int i, double vIn, double cIn[], long dt)
    {
        int    k, m, n;
        double vNet, vOut, vSeg, vSum;
        Pipe   seg;

        // Find inflows & outflows

        k = MSX.Nobjects[ObjectTypes.LINK.id] + i;
        n = MSX.Tank[i].getNode();
        vNet = MSX.D[n]*dt;
        vOut = vIn - vNet;

        // Initialize outflow volume & concentration

        vSum = 0.0;
        for (m=1; m<=MSX.Nobjects[ObjectTypes.SPECIES.id]; m++) MSX.C1[m] = 0.0;

        // Withdraw flow from first segment
        while (vOut > 0.0)
        {
            if (MSX.Segments[k].size() == 0) break;

            // --- get volume of current first segment
            seg = MSX.Segments[k].getFirst();//.get(0);

            vSeg = seg.getV();
            vSeg = Math.min(vSeg, vOut);
            if ( seg ==  MSX.Segments[k].getLast() ) vSeg = vOut;  //.get(MSX.Segments[k].size()-1)       //TODO pode ser simplificado para getSize()==1

            // --- update mass & volume removed
            vSum += vSeg;
            for (m=1; m<=MSX.Nobjects[ObjectTypes.SPECIES.id]; m++){
                MSX.C1[m] += (seg.getC()[m])*vSeg;
            }

            // --- decrease vOut by volume of first segment
            vOut -= vSeg;

            // --- remove segment if all its volume is consumed
            if (vOut >= 0.0 && vSeg >= seg.getV()){
                MSX.Segments[k].pollFirst();//.remove(0);
            }

            // --- otherwise just adjust volume of first segment
            else  seg.setV(seg.getV()-vSeg);
        }

        // Use quality from first segment to represent overall
        // quality of tank since this is where outflow flows from

        for (m=1; m<=MSX.Nobjects[ObjectTypes.SPECIES.id]; m++)
        {
            if (vSum > 0.0) MSX.Tank[i].getC()[m] = MSX.C1[m]/vSum;
            else            MSX.Tank[i].getC()[m] = MSX.Segments[k].getFirst().getC()[m]; //MSX.Segments[k].get(0).getC()[m];
        }

        // Add new last segment for new flow entering tank
        if (vIn > 0.0){
            // Quality is the same, so just add flow volume to last seg
            k = MSX.Nobjects[ObjectTypes.LINK.id] + i;
            seg = null;
            if(MSX.Segments[k].size()>0)
                seg = MSX.Segments[k].getLast();//get(MSX.Segments[k].size()-1);

            if ( seg!=null && quality.MSXqual_isSame(seg.getC(), cIn) ) seg.setV(seg.getV() + vIn);

                // Otherwise add a new seg to tank
            else
            {
                seg = quality.createSeg(vIn, cIn);
                //quality.MSXqual_addSeg(k, seg);
                MSX.Segments[k].add(seg);
            }
        }
    }

    /**
    *Last In-First Out (LIFO) tank model
    */
    void  MSXtank_mix4(int i, double vIn, double cIn[], long dt)
    {
        int    k, m, n;
        double vOut, vNet, vSum, vSeg;
        Pipe   seg;

        // Find inflows & outflows

        k = MSX.Nobjects[ObjectTypes.LINK.id] + i;
        n = MSX.Tank[i].getNode();
        vNet = MSX.D[n]*dt;
        vOut = vIn - vNet;

        // keep track of total volume & mass removed from tank

        vSum = 0.0;
        for (m=1; m<=MSX.Nobjects[ObjectTypes.SPECIES.id]; m++) MSX.C1[m] = 0.0;

        // if tank filling, then create a new last segment
        if ( vNet > 0.0 )
        {

            // inflow quality = last segment quality so just expand last segment

            seg = null;
            if(MSX.Segments[k].size()>0)
                seg = MSX.Segments[k].getLast();//.get(MSX.Segments[k].size()-1);

            if ( seg != null && quality.MSXqual_isSame(seg.getC(), cIn) ) seg.setV( seg.getV() + vNet );

                // otherwise add a new last segment to tank

            else
            {
                seg = quality.createSeg(vNet, cIn);
                MSX.Segments[k].add(seg);
            }

            // quality of tank is that of inflow

            for (m=1; m<=MSX.Nobjects[ObjectTypes.SPECIES.id]; m++) MSX.Tank[i].getC()[m] = cIn[m];

        }

        // if tank emptying then remove last segments until vNet consumed

        else if (vNet < 0.0)
        {

            // keep removing volume from last segments until vNet is removed
            vNet = -vNet;
            while (vNet > 0.0)
            {

                // --- get volume of current last segment
                seg = null;
                if(MSX.Segments[k].size()>0)
                    seg = MSX.Segments[k].getLast();//get(MSX.Segments[k].size()-1);

                if ( seg == null ) break;

                vSeg = seg.getV();
                vSeg = Math.min(vSeg, vNet);
                if ( seg == MSX.Segments[k].getFirst() ) vSeg = vNet;

                // update mass & volume removed
                vSum += vSeg;
                for (m=1; m<=MSX.Nobjects[ObjectTypes.SPECIES.id]; m++)
                    MSX.C1[m] += (seg.getC()[m])*vSeg;

                // reduce vNet by volume of last segment
                vNet -= vSeg;

                // remove segment if all its volume is used up
                if (vNet >= 0.0 && vSeg >= seg.getV())
                {
                    MSX.Segments[k].pollLast();//remove(MSX.Segments[k].size()-1);
                    //MSX.LastSeg[k] = seg->prev;
                    //if ( MSX.LastSeg[k] == NULL ) MSX.Segments[k] = NULL;
                    //MSXqual_removeSeg(seg);
                }

                // otherwise just reduce volume of last segment
                else
                {
                    seg.setV( seg.getV() - vSeg );
                }
            }

            // tank quality is mixture of flow released and any inflow

            for (m=1; m<=MSX.Nobjects[ObjectTypes.SPECIES.id]; m++)
            {
                vSum = vSum + vIn;
                if (vSum > 0.0)
                    MSX.Tank[i].getC()[m] = (MSX.C1[m] + cIn[m]*vIn) / vSum;
            }
        }
    }


}
