package org.addition.epanetold.ui;

import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;
import org.addition.epanetold.Epanet;
import org.addition.epanetold.Network;
import org.addition.epanetold.ProgressWindow;
import org.addition.epanetold.Types.*;
import org.addition.epanetold.Utilities;
import org.addition.epanetMSX.EpanetMSX;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.*;


public class EpanetUI implements ActionListener {

    private void browse(String url){

        if( !java.awt.Desktop.isDesktopSupported()){
            System.err.println( "Desktop is not supported" );return;
        }

        java.awt.Desktop desktop = java.awt.Desktop.getDesktop();

        if( !desktop.isSupported( java.awt.Desktop.Action.BROWSE)){
            System.err.println( "Desktop doesn't support the browse action" );
            System.exit(1);
        }

        try {
            java.net.URI uri = new java.net.URI( url );
            desktop.browse( uri );
        }
        catch ( Exception e ) {

            System.err.println( e.getMessage() );
        }

    }


    public static HydraulicUI hydraulicUI;
    private boolean simulationRunning;

    private ReportOptions reportOptions;

    private FileDialog fileChooser = null;

    private class MSXFilterAWT implements FilenameFilter {
        public boolean accept(File dir, String name) {
            String[] name_extension = name.split("[.]");
            if (name_extension.length >= 2) {
                if (name_extension[name_extension.length - 1].equals("msx"))
                    return true;
            }

            return false;
        }
    }

    private class INPFilterAWT implements FilenameFilter {
        public boolean accept(File dir, String name) {
            String[] name_extension = name.split("[.]");
            if (name_extension.length >= 2) {
                if (name_extension[name_extension.length - 1].equals("inp"))
                    return true;
            }

            return false;
        }
    }



    class NetworkPanel extends JPanel {

        public NetworkPanel() {
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int w = this.getWidth();
            int h = this.getHeight();

            if (epanet == null)
                return;

            Network net = epanet.getNetwork();
            NetworkImage.drawNetwork(g, w, h, net, checkPipes.isSelected(), checkTanks.isSelected(), checkNodes.isSelected());
        }

    }

    private JPanel root;
    private JButton openINPButton;
    private JButton runSimulationButton;
    private JPanel top;
    private JPanel right;
    private JLabel textDemand;
    private JLabel textQuality;
    private JPanel properties;
    private JPanel network;
    private JLabel textReservoirs;
    private JLabel textTanks;
    private JLabel textPipes;
    private JLabel textNodes;
    private JLabel textDuration;
    private JLabel textUnits;
    private JLabel textHeadloss;
    private JCheckBox checkNodes;
    private JCheckBox checkTanks;
    private JCheckBox checkPipes;
    private JPanel propertiesRoot;
    private JPanel left;
    private JPanel checks;
    private JButton logoB;
    private JLabel hr1;
    private JLabel inpName;
    private JButton openMSXButton;
    private JLabel textHydraulic;
    private JLabel msxName;
    private JPanel middle;
    private JPanel bottom;
    private JLabel textPattern;
    //private JButton runMSXButton;
    //private JButton saveReport;
    private JFrame frame;

    private Epanet epanet;

    private File inpFile;

    private File msxFile;

    private EpanetMSX msxEpanet;

    static final ResourceBundle errorBundle = PropertyResourceBundle.getBundle("Error");

    private void createUIComponents() {
        root = new JPanel();
        top = new JPanel();
        right = new JPanel();
        properties = new JPanel();
        propertiesRoot = new JPanel();
        network = new NetworkPanel();
    }


    public EpanetUI() {



        // Don't dispose progress bar windows
        ProgressWindow.setDisposable(false);

        frame = new JFrame();
        frame.setTitle("aware.epanetold");
        frame.add(root);
        frame.pack();
        frame.setMinimumSize(new Dimension(848, 500));
        frame.setLocationRelativeTo(null);

        clearInterface();

        frame.setVisible(true);

        openINPButton.addActionListener(this);
        runSimulationButton.addActionListener(this);
        logoB.addActionListener(this);
        checkTanks.addActionListener(this);
        checkNodes.addActionListener(this);
        checkPipes.addActionListener(this);
        openMSXButton.addActionListener(this);
        //runMSXButton.addActionListener(this);
        //saveReport.addActionListener(this);

        frame.addWindowListener(new WindowListener() {
            public void windowOpened(WindowEvent e) {
            }

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }

            public void windowClosed(WindowEvent e) {
            }

            public void windowIconified(WindowEvent e) {
            }

            public void windowDeiconified(WindowEvent e) {
            }

            public void windowActivated(WindowEvent e) {
            }

            public void windowDeactivated(WindowEvent e) {
            }
        });

    }

    public void actionPerformed(ActionEvent e) {
        if(e.getSource().equals(openMSXButton)){

            if(fileChooser == null){
                fileChooser = new FileDialog(frame);
            }

            fileChooser.setTitle("Open MSX File");
            fileChooser.setFilenameFilter(new MSXFilterAWT());
            fileChooser.setVisible(true);

            if (fileChooser.getFile() != null) {

                File fMSX = new File(fileChooser.getDirectory() +fileChooser.getFile());

                if(!fMSX.canRead())
                {
                    JOptionPane.showMessageDialog(frame, "Unable to open the MSX file.", "Error", JOptionPane.OK_OPTION);
                    return;
                }

                msxName.setText(fMSX.getName());
                msxFile = fMSX;

                //runMSXButton.setEnabled(true);
                //saveReport.setEnabled(false);

                msxEpanet = new EpanetMSX();
                int errCode = msxEpanet.load(inpFile.getAbsolutePath(),msxFile.getAbsolutePath());
                if (errCode != 0) {
                    JOptionPane.showMessageDialog(frame, "Error code :" + errCode, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }


        }
        else if (e.getSource().equals(openINPButton)) {
            if(fileChooser == null){
                fileChooser = new FileDialog(frame);
            }

            fileChooser.setTitle("Open INP File");
            fileChooser.setFilenameFilter(new INPFilterAWT());
            fileChooser.setVisible(true);

            if (fileChooser.getFile() != null) {
                inpFile = new File(fileChooser.getDirectory() + fileChooser.getFile());
                String filetitle = inpFile.getName().split("[.]")[0];
                epanet = new Epanet();


                int ret_value;
                if ((ret_value = epanet.loadINP(inpFile.getAbsolutePath())) != 0) {
                    JOptionPane.showMessageDialog(frame, errorBundle.getString("ERR" + ret_value), "Error", JOptionPane.OK_OPTION);
                    clearInterface();
                    return;
                }

                Network net = epanet.getNetwork();
                textReservoirs.setText(Integer.toString(net.getSections(EnumVariables.SectType._RESERVOIRS)));
                textTanks.setText(Integer.toString(net.getSections(EnumVariables.SectType._TANKS)));
                textPipes.setText(Integer.toString(net.getSections(EnumVariables.SectType._PIPES)));
                textNodes.setText(Integer.toString(net.getMaxNodes()));
                textDuration.setText(Utilities.clocktime(net.getDur()));//Long.toString(net.getDur() / 3600) + " hrs");
                textUnits.setText(net.getUnitsflag().name());
                textHeadloss.setText(net.getFormflag().name());
                textQuality.setText(net.getQualityFlag().name());
                textDemand.setText(Double.toString(net.getDemandMultiplier()));
                textHydraulic.setText(Utilities.clocktime(net.getHstep()));
                textPattern.setText(Utilities.clocktime(net.getPstep()));
                frame.setTitle("aware.epanetold : " + inpFile.getName());
                inpName.setText(inpFile.getName());
                runSimulationButton.setEnabled(true);

                msxEpanet = null;
                msxName.setText("");
                openMSXButton.setEnabled(true);

                epanet.setOutFilename(inpFile.getParent() + "/out_" + filetitle + ".bin");
                epanet.setHydFilename(inpFile.getParent() + "/hyd_" + filetitle + ".bin");
            }


        }
        else if(e.getSource().equals(runSimulationButton)){
            reportOptions = new ReportOptions(epanet,msxEpanet);
            reportOptions.showWindow(frame);
        }
        /*else if (e.getSource().equals(runSimulationButton)) {
            if (epanetold == null || inpFile == null)
                return;

            runSimulationButton.setEnabled(false);
            simulationRunning = true;

            simulatingInfo.setText("Warming up...");
            epanetold = new Epanet();
            epanetold.loadINP(inpFile.getPath());


            Thread simThread = new Thread() {
                @Override
                public void run() {
                    String filetitle = inpFile.getName().split("[.]")[0];
                    //epanetold.setReportFile(inpFile.getParent() + "/rep_" + filetitle + ".txt");
                    epanetold.setOutFilename(inpFile.getParent() + "/out_" + filetitle + ".bin");
                    epanetold.setHydFilename(inpFile.getParent() + "/hyd_" + filetitle + ".bin");


                    int errCode;
                    errCode = epanetold.prepareNetwork();
                    if (errCode == 0)
                        errCode = epanetold.simulateHydraulics(true);
                    if (errCode == 0)
                        errCode = epanetold.simulateHydraulics(true);
                    if (errCode == 0)
                        errCode = epanetold.simulateQuality();
                    //if (errCode == 0)
                    //    errCode = epanetold.completeReport();
                    if (errCode == 0)
                        errCode = epanetold.close();
                    if (errCode != 0) {
                        JOptionPane.showMessageDialog(frame, errorBundle.getString("ERR" + errCode), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    simulationRunning = false;
                    saveReport.setEnabled(true);
                    runSimulationButton.setEnabled(true);
                }
            };
            simThread.start();

            new Thread() {
                @Override
                public void run() {
                    long start = System.currentTimeMillis();
                    while (simulationRunning) {
                        simulatingInfo.setText("Run time: "+(System.currentTimeMillis()-start)/1000f+" seconds");
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException ignored) {
                        }
                    }

                    JOptionPane.showMessageDialog(frame, "Report files saved next to the INP!", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            }.start();



        }   */
        /* else if (e.getSource().equals(runMSXButton)) {
            if (msxFile == null || inpFile==null)
                return;

            runMSXButton.setEnabled(false);
            simulationRunning = true;

            simulatingInfo.setText("Running ...");


            Thread simThread = new Thread() {
                @Override
                public void run() {
                    String filetitle = msxFile.getName().split("[.]")[0];
                    String report_file = msxFile.getParent() + "/rep_" + filetitle + ".txt";
                    String outfile = msxFile.getParent() + "/out_" + filetitle + ".bin";
                    String hydfile = msxFile.getParent() + "/hyd_" + filetitle + ".bin";

                    msxEpanet = new EpanetMSX();
                    int errCode = msxEpanet.Execute(inpFile.getAbsolutePath(),msxFile.getAbsolutePath(),hydfile,outfile,report_file);
                    if (errCode != 0) {
                        JOptionPane.showMessageDialog(frame, "Error code :" + errCode, "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    simulationRunning = false;
                    saveReport.setEnabled(true);
                }
            };
            simThread.start();

            new Thread() {
                @Override
                public void run() {
                    long start = System.currentTimeMillis();
                    while (simulationRunning) {
                        simulatingInfo.setText("Run time: "+(System.currentTimeMillis()-start)/1000f+" seconds");
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException ignored) {
                        }
                    }

                    JOptionPane.showMessageDialog(frame, "Report files saved next to the INP!", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            }.start();

            runMSXButton.setEnabled(true);

        } */
        else if(e.getSource()==logoB){
            /*if(EpanetUI.hydraulicUI ==null){
                EpanetUI.hydraulicUI = new HydraulicUI();
                EpanetUI.hydraulicUI.frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
            }
            EpanetUI.hydraulicUI.frame.setVisible(true); */

            browse("http://www.aware-p.org/");

        }
        /*else if(e.getSource().equals(saveReport)){
            reportOptions = new ReportOptions(epanetold,msxEpanet);
            reportOptions.showWindow();
        } */
        network.repaint();
    }

    private void clearInterface() {
        epanet = null;
        inpFile = null;
        frame.setTitle("aware.epanetold");
        textReservoirs.setText("0");
        textTanks.setText("0");
        textPipes.setText("0");
        textNodes.setText("0");
        textDuration.setText("00:00:00");
        textHydraulic.setText("00:00:00");
        textPattern.setText("00:00:00");
        textUnits.setText("NONE");
        textHeadloss.setText("NONE");
        textQuality.setText("NONE");
        textDemand.setText("0.0");
    }


    public static void main(String[] args) throws UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(new Plastic3DLookAndFeel());

        new EpanetUI();
    }


}
