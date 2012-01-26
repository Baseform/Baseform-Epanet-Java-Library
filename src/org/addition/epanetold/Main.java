package org.addition.epanetold;


public class Main {


    public static void main(String[] args) throws Exception
    {

        if(args.length==0){
            System.out.println("args: file.inp runtimes (optional)");
            return;
        }

        for (int i = 0; i < (args.length>1?Integer.parseInt(args[1]):1); i++) {
            long startTime = System.currentTimeMillis();
            int errCode = 0;

            ProgressWindow.setDisposable(true);

            Epanet epanet = new Epanet(true);

            errCode = Utilities.ERRCODE(errCode,epanet.setReportFile("epanetold.txt"));
            errCode = Utilities.ERRCODE(errCode,epanet.setHydFilename("epanetold.hyd"));
            errCode = Utilities.ERRCODE(errCode,epanet.setOutFilename("epanetold.bin"));

            errCode = Utilities.ERRCODE(errCode,epanet.loadINP(args[0]));

            errCode = Utilities.ERRCODE(errCode,epanet.prepareNetwork());

            errCode = Utilities.ERRCODE(errCode,epanet.simulateHydraulics(true));
            errCode = Utilities.ERRCODE(errCode,epanet.simulateQuality());

            errCode = Utilities.ERRCODE(errCode,epanet.completeReport());

            errCode = Utilities.ERRCODE(errCode,epanet.close());


            if(errCode>0)
                System.out.println(errCode);

            System.out.println("\n  o Execution time : "  + (System.currentTimeMillis() - startTime) + " ms.\n");

        }
    }

}
