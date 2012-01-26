package org.addition.epanetold;

import java.io.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public strictfp class HydReaderOLD {


    private static final DecimalFormat PERC = new DecimalFormat("0.000%");

    private static final String inpFolder = "/Users/luisloureiro/Desktop/";
    private static final String compareFolder = "/Users/luisloureiro/Desktop/";
    private static final String reportFolder =  "/Users/luisloureiro/Desktop/";

    public static void main(String[] args) throws Exception {
        HydReaderOLD hydreader = new HydReaderOLD();
        hydreader.loadINP("verylargenetwork.inp");
        hydreader.Compare("HYD_VERYLARGE_NETWORK_WIN.out", "epanet2.hyd", "compare.txt");
    }

    public static int MAGIC_NUMBER = 516114521;

    DataInputStream mStream_1;
    DataInputStream mStream_2;
    BufferedWriter mWritter;
    Epanet epanet;

    int mVersionNumber;
    int mMaxNodes;
    int mMaxLinks;
    int mMaxTanks;
    int mMaxPumps;
    int mMaxValves;
    int mMaxDuration;

    void loadINP(String filename)
    {
        epanet = new Epanet();
        epanet.loadINP(inpFolder+filename);
        //epanetold.ENopen(inpFolder+filename,"temp1","temp2");
    }

    float floatCompare(float f1, float f2) {
        BigDecimal bd1 = new BigDecimal(f1, MathContext.DECIMAL32);
        BigDecimal bd2 = new BigDecimal(f2, MathContext.DECIMAL32);
        if (bd1.equals(bd2)) return 0;
        BigDecimal diff = bd1.subtract(bd2).abs();
        BigDecimal epsilon = bd1.abs().max(bd2.abs());
        BigDecimal relativeDiff = diff.divide(epsilon, 6).setScale(6, BigDecimal.ROUND_DOWN);
        if (relativeDiff.doubleValue() == 0d)
            return 0f;
        else
            return relativeDiff.floatValue();
    }

    int readInt(DataInputStream stream, boolean LittleEndian) throws IOException {
        if (LittleEndian) {
            byte[] buff = new byte[4];
            stream.readFully(buff);
            return ((buff[3] & 0xff) << 24 | (buff[2] & 0xff) << 16 | (buff[1] & 0xff) << 8 | buff[0] & 0xff);
        } else
            return stream.readInt();

    }

    float readFloat(DataInputStream stream, boolean LittleEndian) throws IOException {
        if (LittleEndian) {
            byte[] buff = new byte[4];

            stream.readFully(buff);
            ByteBuffer bbuff = ByteBuffer.wrap(new byte[]{buff[3], buff[2], buff[1], buff[0]});

            return bbuff.getFloat();
        } else
            return stream.readFloat();
    }

    void Compare(String file1, String file2, String report) throws IOException {
        boolean little_endian_1 = false;
        boolean little_endian_2 = false;

        try {
            mWritter = new BufferedWriter(new FileWriter(reportFolder+report));

            mStream_1 = new DataInputStream(new BufferedInputStream(new FileInputStream(new File(compareFolder + file1))));
            mStream_2 = new DataInputStream(new BufferedInputStream(new FileInputStream(new File(compareFolder + file2))));

            int magic1 = mStream_1.readInt();
            int magic2 = mStream_2.readInt();

            if (magic1 != MAGIC_NUMBER) little_endian_1 = true;
            if (magic2 != MAGIC_NUMBER) little_endian_2 = true;

            mVersionNumber = readInt(mStream_1, little_endian_1);
            mMaxNodes = readInt(mStream_1, little_endian_1);
            mMaxLinks = readInt(mStream_1, little_endian_1);
            mMaxTanks = readInt(mStream_1, little_endian_1);
            mMaxPumps = readInt(mStream_1, little_endian_1);
            mMaxValves = readInt(mStream_1, little_endian_1);
            mMaxDuration = readInt(mStream_1, little_endian_1);

            System.out.println("Version Number : " + mVersionNumber + ", " + readInt(mStream_2, little_endian_2));
            System.out.println("Max Nodes : " + mMaxNodes + ", " + readInt(mStream_2, little_endian_2));
            System.out.println("Max Links : " + mMaxLinks + ", " + readInt(mStream_2, little_endian_2));
            System.out.println("Max Tanks : " + mMaxTanks + ", " + readInt(mStream_2, little_endian_2));
            System.out.println("Max Pumps : " + mMaxPumps + ", " + readInt(mStream_2, little_endian_2));
            System.out.println("Max Valves : " + mMaxValves + ", " + readInt(mStream_2, little_endian_2));
            System.out.println("Max Duration : " + mMaxDuration + ", " + readInt(mStream_2, little_endian_2));
        } catch (IOException e) {
            System.out.println("Ficheiro não existe !");
            return;
        }


        mWritter.write("time1\ttime2\ttype\tid\tv1\tv2\terror\n");
        int hydTime1 = 0;
        int hydTime2 = 0;
        List<Object[]> errList = new ArrayList<Object[]>();
        while (Math.max(hydTime1, hydTime2) < mMaxDuration) {
            try {
                hydTime1 = readInt(mStream_1, little_endian_1);
                hydTime2 = readInt(mStream_2, little_endian_2);
                float temp_error = 0.0f;

                // Node actual demand
                errList.clear();
                for (int i = 1; i <= mMaxNodes; i++) {
                    float val1 = readFloat(mStream_1, little_endian_1);
                    float val2 = readFloat(mStream_2, little_endian_2);
                    temp_error = floatCompare(val1, val2);

                    if (temp_error != 0) {
                        String id = Integer.toString(i);
                        if(epanet!=null) id = epanet.getNetwork().getNode(i).getId();
                        errList.add(new Object[]{id, val1, val2, temp_error});
                    }
                }
                Collections.sort(errList, new Comparator<Object[]>() {
                    public int compare(Object[] o1, Object[] o2) {
                        return -((Comparable) o1[3]).compareTo(o2[3]);
                    }
                });
                if (errList.size() > 20) errList = errList.subList(0, 20);
                for (Object[] o : errList)
                    mWritter.write(String.format("%d\t%d\t%s\t%s\t%g\t%g\t%g\n", hydTime1, hydTime2, "Node Demand", o[0], o[1], o[2], o[3]));


                // Node heads
                errList.clear();
                for (int i = 1; i <= mMaxNodes; i++) {
                    float val1 = readFloat(mStream_1, little_endian_1);
                    float val2 = readFloat(mStream_2, little_endian_2);
                    temp_error = floatCompare(val1, val2);

                    if (temp_error != 0) {
                        String id = Integer.toString(i);
                        if(epanet!=null) id = epanet.getNetwork().getNode(i).getId();
                        errList.add(new Object[]{id, val1, val2, temp_error});
                    }
                }
                Collections.sort(errList, new Comparator<Object[]>() {
                    public int compare(Object[] o1, Object[] o2) {
                        return -((Comparable) o1[3]).compareTo(o2[3]);
                    }
                });
                if (errList.size() > 20) errList = errList.subList(0, 20);
                for (Object[] o : errList)
                    mWritter.write(String.format("%d\t%d\t%s\t%s\t%g\t%g\t%g\n", hydTime1, hydTime2, "Node Head", o[0], o[1], o[2], o[3]));


                // Link flows
                errList.clear();
                for (int i = 1; i <= mMaxLinks; i++) {
                    float val1 = readFloat(mStream_1, little_endian_1);
                    float val2 = readFloat(mStream_2, little_endian_2);
                    temp_error = floatCompare(val1, val2);
                    if (temp_error != 0) {
                        String id = Integer.toString(i);
                        if(epanet!=null) id = epanet.getNetwork().getLink(i).getId();
                        errList.add(new Object[]{id, val1, val2, temp_error});
                    }
                }
                Collections.sort(errList, new Comparator<Object[]>() {
                    public int compare(Object[] o1, Object[] o2) {
                        return -((Comparable) o1[3]).compareTo(o2[3]);
                    }
                });
                if (errList.size() > 20) errList = errList.subList(0, 20);
                for (Object[] o : errList)
                    mWritter.write(String.format("%d\t%d\t%s\t%s\t%g\t%g\t%g\n", hydTime1, hydTime2, "Link Flows", o[0], o[1], o[2], o[3]));

                // Link status
                errList.clear();
                for (int i = 1; i <= mMaxLinks; i++) {
                    float val1 = readFloat(mStream_1, little_endian_1);
                    float val2 = readFloat(mStream_2, little_endian_2);
                    temp_error = floatCompare(val1, val2);
                    if (temp_error != 0) {
                        String id = Integer.toString(i);
                        if(epanet!=null) id = epanet.getNetwork().getLink(i).getId();
                        errList.add(new Object[]{id, val1, val2, temp_error});
                    }
                }
                Collections.sort(errList, new Comparator<Object[]>() {
                    public int compare(Object[] o1, Object[] o2) {
                        return -((Comparable) o1[3]).compareTo(o2[3]);
                    }
                });
                if (errList.size() > 20) errList = errList.subList(0, 20);
                for (Object[] o : errList)
                    mWritter.write(String.format("%d\t%d\t%s\t%s\t%g\t%g\t%g\n", hydTime1, hydTime2, "Link Status", o[0], o[1], o[2], o[3]));


                // Link settings
                errList.clear();
                for (int i = 1; i <= mMaxLinks; i++) {
                    float val1 = readFloat(mStream_1, little_endian_1);
                    float val2 = readFloat(mStream_2, little_endian_2);
                    temp_error = floatCompare(val1, val2);
                    if (temp_error != 0) {
                        String id = Integer.toString(i);
                        if(epanet!=null) id = epanet.getNetwork().getLink(i).getId();
                        errList.add(new Object[]{id, val1, val2, temp_error});
                    }
                }
                Collections.sort(errList, new Comparator<Object[]>() {
                    public int compare(Object[] o1, Object[] o2) {
                        return -((Comparable) o1[3]).compareTo(o2[3]);
                    }
                });
                if (errList.size() > 20) errList = errList.subList(0, 20);
                for (Object[] o : errList)
                    mWritter.write(String.format("%d\t%d\t%s\t%s\t%g\t%g\t%g\n", hydTime1, hydTime2, "Link Settings", o[0], o[1], o[2], o[3]));


                int hydStep1 = readInt(mStream_1, little_endian_1);
                int hydStep2 = readInt(mStream_2, little_endian_2);

                System.out.println(hydTime1);
            }
            catch (IOException e) {
                e.printStackTrace();
            }



        }
        mWritter.close();

    }
}
