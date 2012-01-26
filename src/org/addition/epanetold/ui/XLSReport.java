package org.addition.epanetold.ui;


import org.addition.epanetold.Epanet;
import org.addition.epanetold.Network;
import org.addition.epanetold.Types.EnumVariables;
import org.addition.epanetold.Types.Link;
import org.addition.epanetold.Types.Node;

import java.io.*;

public class XLSReport {

    public static final int CELLS_COUNT = 500000;

    String filename = "";
    public static void main(String[] args) throws Exception {
        Epanet epanet = new Epanet();

        epanet.loadINP("files/verylargenetwork.inp");
        epanet.setHydFilename("out/HYD.bin");
        epanet.prepareNetwork();
        epanet.simulateHydraulics(false);
        DataInputStream hydStream = new DataInputStream(new BufferedInputStream(new FileInputStream(epanet.getHydFile())));
        hydStream.skipBytes(8 * 4);

        reportXlsx(epanet, hydStream, epanet.getHydFile());
        epanet.close();
    }

    private static void reportXlsx(Epanet epanet, DataInputStream hydStream, File file) throws Exception {
        int Nnodes = epanet.getNetwork().getMaxNodes();
        int Nlinks = epanet.getNetwork().getMaxLinks();

        SimpleXLSXFile xlsxFile = new SimpleXLSXFile();

        SimpleXLSXFile.Spreadsheet d = xlsxFile.newSpreadsheet("D");
        SimpleXLSXFile.Spreadsheet h = xlsxFile.newSpreadsheet("H");
        SimpleXLSXFile.Spreadsheet q = xlsxFile.newSpreadsheet("Q");
        SimpleXLSXFile.Spreadsheet s = xlsxFile.newSpreadsheet("S");
        SimpleXLSXFile.Spreadsheet k = xlsxFile.newSpreadsheet("K");

        Object[] nodesHead = new String[Nnodes + 1];
        nodesHead[0] = "Timestep/node";
        for (int i = 1; i <= Nnodes; i++) {
            Node node = epanet.getNetwork().getNode(i);
            nodesHead[i] = node.getId();
        }

        Object[] linksHead = new String[Nlinks + 1];
        linksHead[0] = "Timestep/link";
        for (int i = 1; i <= Nlinks; i++) {
            Link link = epanet.getNetwork().getLink(i);
            linksHead[i] = link.getId();
        }


        d.addRow(nodesHead);
        h.addRow(nodesHead);

        q.addRow(linksHead);
        s.addRow(linksHead);
        k.addRow(linksHead);

        long tstep;

        Object nodeRow[] = new Object[Nnodes + 1];
        Object linkRow[] = new Object[Nlinks + 1];
        do {
            HydraulicTS hyd = readhyd(hydStream, epanet.getNetwork());
            tstep = hyd.hydstep;
            System.out.printf("TS %d\t%g\n", hyd.hydtime, hyd.K[1]);

            nodeRow[0] = hyd.hydtime;
            for (int i = 1; i <= Nnodes; i++) {
                nodeRow[i] = (double) hyd.D[i];
            }
            d.addRow(nodeRow);

            for (int i = 1; i <= Nnodes; i++)
                nodeRow[i] = (double) hyd.H[i];
            h.addRow(nodeRow);

            linkRow[0] = hyd.hydtime;

            for (int i = 1; i <= Nlinks; i++)
                linkRow[i] = (double) hyd.Q[i];
            q.addRow(linkRow);

            for (int i = 1; i <= Nlinks; i++)
                linkRow[i] = (double) hyd.S[i].ordinal();
            s.addRow(linkRow);

            for (int i = 1; i <= Nlinks; i++)
                linkRow[i] = (double) hyd.K[i];
            k.addRow(linkRow);
        }
        while (tstep > 0);
        //xlsxFile.save(new FileOutputStream(new File(file.getParent(), "simple.xlsx").getPath()));
        //xlsxFile.finish();
    }


    private static HydraulicTS readhyd(DataInputStream file, Network net) throws IOException {
        int Nnodes = net.getMaxNodes();
        int Nlinks = net.getMaxLinks();
        HydraulicTS hyd = new HydraulicTS(Nnodes, Nlinks);


        hyd.hydtime = file.readInt();


        for (int i = 0; i < Nnodes; i++)
            hyd.D[i + 1] = file.readFloat();

        for (int i = 0; i < Nnodes; i++)
            hyd.H[i + 1] = file.readFloat();

        for (int i = 0; i < Nlinks; i++)
            hyd.Q[i + 1] = file.readFloat();

        for (int i = 0; i < Nlinks; i++)
            hyd.S[i + 1] = EnumVariables.StatType.values()[(int) file.readFloat()];

        for (int i = 0; i < Nlinks; i++)
            hyd.K[i + 1] = file.readFloat();

        hyd.hydstep = file.readInt();

        return hyd;
    }

    private static class HydraulicTS {
        public float[] D;
        public float[] H;
        public float[] Q;
        public EnumVariables.StatType[] S;
        public float[] K;
        public long hydtime;
        public int hydstep;

        public HydraulicTS(int nnodes, int nlinks) {
            D = new float[nnodes + 1];
            H = new float[nnodes + 1];
            Q = new float[nlinks + 1];
            S = new EnumVariables.StatType[nlinks + 1];
            K = new float[nlinks + 1];
        }
    }
}