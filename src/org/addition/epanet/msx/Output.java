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

import org.addition.epanet.msx.EnumTypes.*;


import java.io.*;

public class Output {

    public void loadDependencies(EpanetMSX epa) {
        this.MSX = epa.getNetwork();
        quality = epa.getQuality();
    }

    Network  MSX;                       // MSX project data

    long  ResultsOffset;                // Offset byte where results begin
    long  NodeBytesPerPeriod;           // Bytes per time period used by all nodes
    static long  LinkBytesPerPeriod;    // Bytes per time period used by all links
    private Quality quality;

    DataOutputStream outStream;



    // opens an MSX binary output file.
    int MSXout_open(File output) throws IOException {
        // Close output file if already opened
        //MSX.OutFile.close();
        //
        //// Try to open the file
        //if(!MSX.OutFile.openAsBinaryWritter())
        //    return ErrorCodeType.ERR_OPEN_OUT_FILE.id;
        //
        //
        //// open a scratch output file for statistics
        //if ( MSX.Statflag == TstatType.SERIES ) //MSX.TmpOutFile.file = MSX.OutFile.file;
        //    MSX.TmpOutFile = MSX.OutFile;
        //else
        //if ( !MSX.TmpOutFile.openAsBinaryWritter())
        //    return ErrorCodeType.ERR_OPEN_OUT_FILE.id;

        outStream = new DataOutputStream(new FileOutputStream(output));

        // write initial results to file
        MSX.Nperiods = 0;
        MSXout_saveInitialResults(output);
        return 0;
    }

    void writeString(DataOutputStream stream,String str, int size )
    {
        char[] buff = null;
        if(str!=null)
            buff = str.toCharArray();
        for(int i = 0;i<size;i++)
        {
            try {
                if(buff!=null && i<buff.length)
                    stream.writeByte(buff[i]);
                else
                    stream.writeByte('\0');
            } catch (IOException e) {
                return;
            }
        }
    }

    // Saves general information to beginning of MSX binary output file.
    int MSXout_saveInitialResults(File output) throws IOException {
        //MSX.OutFile.close();
        //MSX.OutFile.openAsBinaryWritter(); // rewind

        //DataOutputStream dout = (DataOutputStream)MSX.OutFile.getFileIO();

       //try {
       //    outStream.writeInt(Constants.MAGICNUMBER);               //Magic number
       //    outStream.writeInt(Constants.VERSION);                   //Version number
       //    outStream.writeInt(MSX.Nobjects[ObjectTypes.NODE.id]);   //Number of nodes
       //    outStream.writeInt(MSX.Nobjects[ObjectTypes.LINK.id]);   //Number of links
       //    outStream.writeInt(MSX.Nobjects[ObjectTypes.SPECIES.id]);//Number of species
       //    outStream.writeInt((int)MSX.Rstep);                      //Reporting step size
       //
       //    for (int m=1; m<=MSX.Nobjects[ObjectTypes.SPECIES.id]; m++){
       //        int n = MSX.Species[m].getId().length();
       //        outStream.writeInt(n);                               //Length of species ID
       //        writeString(outStream,MSX.Species[m].getId(),n);     //Species ID string
       //    }
       //
       //    for (int m=1; m<=MSX.Nobjects[ObjectTypes.SPECIES.id]; m++){
       //        writeString(outStream,MSX.Species[m].getUnits(),Constants.MAXUNITS); //Species mass units
       //    }
       //
       //} catch (IOException e) {
       //    return 0;
       //}

        //outStream.close();
        ResultsOffset = 0;// output.length();
        outStream = new DataOutputStream(new FileOutputStream(output,true));


        NodeBytesPerPeriod = MSX.Nobjects[ObjectTypes.NODE.id]*MSX.Nobjects[ObjectTypes.SPECIES.id]*4;
        LinkBytesPerPeriod = MSX.Nobjects[ObjectTypes.LINK.id]*MSX.Nobjects[ObjectTypes.SPECIES.id]*4;

        return 0;
    }

    // Saves computed species concentrations for each node and link at the
    // current time period to the temporary MSX binary output file (which
    // will be the same as the permanent MSX binary file if time series
    // values were specified as the reported statistic, which is the
    // default case).
    int MSXout_saveResults()
    {
        int   m, j;
        double  x;
        //DataOutputStream dout = (DataOutputStream)MSX.TmpOutFile.getFileIO();
        for (m=1; m<=MSX.Nobjects[ObjectTypes.SPECIES.id]; m++)
        {
            for (j=1; j<=MSX.Nobjects[ObjectTypes.NODE.id]; j++)
            {
                x = quality.MSXqual_getNodeQual(j, m);
                //if(j==462){
                //    System.out.println("462 : " + x);
                //}
                //if(j==79){
                //    System.out.println("79 : " + x);
                //}
                try {
                    outStream.writeFloat((float)x);//fwrite(&x, sizeof(REAL4), 1, MSX.TmpOutFile.file);
                } catch (IOException e) {}
            }
        }
        for (m=1; m<=MSX.Nobjects[ObjectTypes.SPECIES.id]; m++)
        {
            for (j=1; j<=MSX.Nobjects[ObjectTypes.LINK.id]; j++)
            {
                x = quality.MSXqual_getLinkQual(j, m);
                try{
                    outStream.writeFloat((float)x);//fwrite(&x, sizeof(REAL4), 1, MSX.TmpOutFile.file);
                }
                catch (IOException e){}
            }
        }
        return 0;
    }


    // Saves any statistical results plus the following information to the end
    //    of the MSX binary output file:
    //    - byte offset into file where WQ results for each time period begins,
    //    - total number of time periods written to the file,
    //    - any error code generated by the analysis (0 if there were no errors),
    //    - the Magic Number to indicate that the file is complete.
    int MSXout_saveFinalResults()
    {
        int  n;
        int  magic = Constants.MAGICNUMBER;
        int   err = 0;

        // Save statistical results to the file
        //if ( MSX.Statflag != TstatType.SERIES )
        //    err = saveStatResults(out);

        if ( err > 0 )
            return err;

        // Write closing records to the file
        try{
            n = (int)ResultsOffset;
            outStream.writeInt(n);
            n = (int)MSX.Nperiods;
            outStream.writeInt(n);
            n = MSX.ErrCode;
            outStream.writeInt(n);
            outStream.writeInt(magic);
        }
        catch (IOException e){}
        return 0;
    }

    //  retrieves a result for a specific node from the MSX binary output file.
    public float MSXout_getNodeQual(RandomAccessFile raf,int k, int j, int m)
    {
        float c=0.0f;
        long bp = ResultsOffset + k * (NodeBytesPerPeriod + LinkBytesPerPeriod);
        bp += ((m-1)*MSX.Nobjects[ObjectTypes.NODE.id] + (j-1)) * 4;

        try {
            raf.seek(bp);
            c = raf.readFloat();
        } catch (IOException e) {}

        return c;
    }

    // retrieves a result for a specific link from the MSX binary output file.
    public float MSXout_getLinkQual(RandomAccessFile raf,int k, int j, int m)
    {
        float c=0.0f;
        long bp = ResultsOffset + ((k+1)*NodeBytesPerPeriod) + (k*LinkBytesPerPeriod);
        bp += ((m-1)*MSX.Nobjects[ObjectTypes.LINK.id] + (j-1)) * 4;

        try {
            raf.seek(bp);
            c = raf.readFloat();
        } catch (IOException e) {}
        return c;
    }


    // Saves time statistic results (average, min., max., or range) for each
    // node and link to the permanent binary output file.
    //int  saveStatResults(RandomAccessFile raf,DataOutputStream dout)
    //{
    //    int     m, err = 0;
    //    float []  x;
    //    double[] stats1;
    //    double[] stats2;
    //
    //    // Create arrays used to store statistics results
    //
    //    if ( MSX.Nperiods <= 0 ) return err;
    //    m = Math.max(MSX.Nobjects[ObjectTypes.NODE.id], MSX.Nobjects[ObjectTypes.LINK.id]);
    //    x = new float[m+1];
    //    stats1 = new double[m+1];
    //    stats2 = new double[m+1];
    //
    //    // Get desired statistic for each node & link and save to binary file
    //
    //    //if ( x && stats1 && stats2 )
    //    {
    //        for (m = 1; m <= MSX.Nobjects[ObjectTypes.SPECIES.id]; m++ )
    //        {
    //            getStatResults(raf,ObjectTypes.NODE, m, stats1, stats2, x);
    //            //fwrite(x+1, sizeof(REAL4), MSX.Nobjects[ObjectTypes.NODE.id], MSX.OutFile.file);
    //            for(int ij = 0;ij<MSX.Nobjects[ObjectTypes.NODE.id];ij++)
    //                try {
    //                    dout.writeFloat(x[ij+1]);
    //                } catch (IOException e) {
    //
    //                }
    //
    //        }
    //        for (m = 1; m <= MSX.Nobjects[ObjectTypes.SPECIES.id]; m++)
    //        {
    //            getStatResults(raf,ObjectTypes.LINK, m, stats1, stats2, x);
    //            //fwrite(x+1, sizeof(REAL4), MSX.Nobjects[ObjectTypes.LINK.id], MSX.OutFile.file);
    //            for(int ij = 0;ij<MSX.Nobjects[ObjectTypes.LINK.id];ij++)
    //                try {
    //                    dout.writeFloat(x[ij+1]);
    //                } catch (IOException e) {
    //
    //                }
    //        }
    //        MSX.Nperiods = 1;
    //    }
    //
    //    return err;
    //}

    // Reads all results for a given type of object from the temporary
    //    binary output file and computes the required statistic (average,
    //    min., max., or range) for each object.
    //void getStatResults(RandomAccessFile raf,ObjectTypes objType, int m, double [] stats1, double [] stats2,
    //                    float [] x)
    //{
    //    int  j, k;
    //    int  n = MSX.Nobjects[objType.id];
    //    long bp;
    //
    //    // Initialize work arrays
    //    for (j = 1; j <= n; j++)
    //    {
    //        stats1[j] = 0.0;
    //        stats2[j] = 0.0;
    //    }
    //
    //    // For all time periods
    //    byte [] readBuff = new byte[n*4];
    //    ByteBuffer buffWrapper = ByteBuffer.wrap(readBuff);
    //    for (k = 0; k < MSX.Nperiods; k++)
    //    {
    //
    //        // position file at start of time period
    //        bp = k*(NodeBytesPerPeriod + LinkBytesPerPeriod);
    //        if ( objType == ObjectTypes.NODE )
    //        {
    //            bp += (m-1) * MSX.Nobjects[ObjectTypes.NODE.id] * 4;
    //        }
    //        if ( objType == ObjectTypes.LINK)
    //        {
    //            bp += NodeBytesPerPeriod +
    //                    (m-1) * MSX.Nobjects[ObjectTypes.LINK.id] * 4;
    //        }
    //
    //        // read concentrations and update stats for all objects
    //        try{
    //            raf.seek(bp);
    //            raf.read(readBuff);
    //
    //            for(int i = 0;i<n;i++)
    //                x[i+1] = buffWrapper.getFloat(i<<2);
    //        }
    //        catch(IOException e){
    //            continue;
    //        }
    //
    //
    //        if (MSX.Statflag == TstatType.AVGERAGE){
    //            for (j = 1; j <= n; j++) stats1[j] += x[j];
    //        }
    //        else
    //            for (j = 1; j <= n; j++)
    //            {
    //                stats1[j] = Math.min(stats1[j], x[j]);
    //                stats2[j] = Math.max(stats2[j], x[j]);
    //            }
    //    }
    //
    //    // Place final stat value for each object in x
    //    if (MSX.Statflag == TstatType.AVGERAGE){
    //        for ( j = 1; j <= n; j++) stats1[j] /= (double)MSX.Nperiods;
    //    }
    //
    //    if (MSX.Statflag == TstatType.RANGE){
    //        for ( j = 1; j <= n; j++)
    //            stats1[j] = Math.abs(stats2[j] - stats1[j]);
    //    }
    //
    //    if(MSX.Statflag == TstatType.MAXIMUM){
    //        for ( j = 1; j <= MSX.Nobjects[ObjectTypes.NODE.id]; j++) stats1[j] = stats2[j];
    //    }
    //
    //    for (j = 1; j <= n; j++)
    //        x[j] = (float)stats1[j];
    //}

}
