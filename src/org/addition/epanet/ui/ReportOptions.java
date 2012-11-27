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

package org.addition.epanet.ui;

import org.addition.epanet.msx.ENToolkit2;
import org.addition.epanet.util.ENException;
import org.addition.epanet.hydraulic.HydraulicSim;
import org.addition.epanet.msx.EpanetMSX;
import org.addition.epanet.network.Network;
import org.addition.epanet.network.PropertiesMap;
import org.addition.epanet.network.io.input.InputParser;
import org.addition.epanet.quality.QualitySim;
import org.addition.epanet.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.logging.*;

/**
 * Configures and executes the epanet (Hydraulic/Quality) and MSX quality simulation through the UI.
 */
public class ReportOptions implements ActionListener {
    private JCheckBox hydraulicsCheckBox;
    private JCheckBox qualityCheckBox;
    private JCheckBox qualityMSXCheckBox;
    private JPanel root;
    private JPanel top;
    private JCheckBox showSummaryCheckBox;
    private JCheckBox showHydraulicSolverEventsCheckBox;
    private JButton runButton;
    private JPanel reportOptions2;
    private JPanel reportOptions1;
    private JPanel actions;
    private JPanel hydPanel;
    private JPanel qualityPanel;
    private JPanel qualityMSXPanel;
    private JComboBox reportPeriodBox;
    private JComboBox unitsBox;
    private JList speciesCheckList;
    private JList hydVariables;
    private JScrollPane hydScroll;
    private JScrollPane qualScroll;
    private JList qualityVariables;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JPanel progressPanel;
    private JButton cancelButton;
    private JTextField textSimulationDuration;
    private JComboBox qualComboBox;
    private JComboBox hydComboBox;
    private JTextField textReportStart;
    private JCheckBox transposeResultsCheckBox;

    /**
     * Custom logger messages formater.
     */
    private class HydLogFormater extends Formatter {
        public String format(LogRecord rec) {return formatMessage(rec)+"\n";}
        public String getHead(Handler h) {return "";}
        public String getTail(Handler h) {return "";}
    }

    Logger log;

    /**
     * This dialog reference.
     */
    private JDialog mainFrame;
    /**
     * The XLSX report file dialog.
     */
    private JFileChooser reportFileDialog;
    /**
     * Loaded INP network.
     */
    private Network netINP;
    /**
     * Loaded MSX simulation.
     */
    private EpanetMSX netMSX;
    /**
     * Epanet network config file.
     */
    private File fileINP;
    /**
     * MSX config file.
     */
    private File fileMSX;
    /**
     * Epanet toolkit for the MSX.
     */
    ENToolkit2 epanetTK;

    /**
     * Hydraulic simulator.
     */
    HydraulicSim hydSim = null;


    /**
     * Public enum to set the simulation time step duration.
     */
    public enum TimeSteps {
        STEP_1_MIN(0,60,"1 minute"),
        STEP_2_MIN(1,120,"2 minutes"),
        STEP_3_MIN(2,180,"3 minutes"),
        STEP_4_MIN(3,240,"4 minutes"),
        STEP_5_MIN(4,300,"5 minutes"),
        STEP_10_MIN(5,600,"10 minutes"),
        STEP_15_MIN(6,900,"15 minutes"),
        STEP_30_MIN(7,1800,"30 minutes"),
        STEP_1_HOUR(8,3600,"1 hour"),
        STEP_2_HOUR(9,7200,"2 hours"),
        STEP_4_HOUR(10,14400,"4 hours"),
        STEP_6_HOURS(11,21600,"6 hours"),
        STEP_12_HOURS(12,43200,"12 hours");

        /**
         * Entry sequencial ID
         */
        public final int id;
        /**
         * Entry name
         */
        public final String name;
        /**
         * Entry timestep duration.
         */
        public final int time;

        private TimeSteps(int id,int time, String name) {
            this.id = id;
            this.name = name;
            this.time = time;
        }

        /**
         * Get available timestep periods.
         * @return Array of timesteps names to the UI.
         */
        public static String [] getPeriodsText(){
            String [] vec = new String[values().length];
            for(TimeSteps step : values())
                vec[step.id] = step.name;
            return vec;
        }

        /**
         * Get the nearest timestep period.
         * @param time
         * @return Nearest timestep, if the time is bigger than any timestep returns STEP_12_HOURS.
         */
        public static TimeSteps getNearestStep(long time){
            for(TimeSteps step : values()){
                if(step.time>=time)
                    return step;

            }
            return STEP_12_HOURS;
        }
    }


    /**
     * Report options dialog constructor.
     * @param inpFile
     * @param msxFile
     */
    public ReportOptions(File inpFile,File msxFile, Logger log) {
        this.log = log;

        if(inpFile!=null){
            fileINP = inpFile;
            netINP = new Network();


            //Logger log = Logger.getAnonymousLogger();//Logger.getLogger(this.getClass().toString());
            //log.setUseParentHandlers(false);
            //log.setLevel(Level.ALL);

            //LoggerFileHandler fh = null;

            try {
                //fh = new LoggerFileHandler(new File(logFile), false);
                //fh.setFormatter(new SimpleFormatter());
                //fh.setLevel(Level.ALL);

                Network.FileType netType = Network.FileType.INP_FILE;

                int lastDot = inpFile.getName().lastIndexOf(".");
                if(lastDot>0 && lastDot+1 < inpFile.getName().length()){
                    String extension = inpFile.getName().substring(lastDot+1).toLowerCase();
                    if(extension.equals("xlsx"))
                        netType = Network.FileType.EXCEL_FILE;
                    else if(extension.equals("inp"))
                        netType = Network.FileType.INP_FILE;
                }

                InputParser inpParser = InputParser.create(netType,log);
                inpParser.parse(netINP,inpFile);

            }
            catch (ENException en_ex){
                JOptionPane.showMessageDialog(root, Utilities.getError(Integer.toString(en_ex.getCodeID())) + "\nCheck epanet.log for detailed error description", "Error", JOptionPane.OK_OPTION);
                inpFile = null;
                return;
            }
            //catch (IOException e) {
            //    JOptionPane.showMessageDialog(root, "IO error while reading the INP file", "Error", JOptionPane.OK_OPTION);
            //    inpFile = null;
            //    return;
            //}

            if(msxFile!=null){
                fileMSX = msxFile;
                epanetTK = new ENToolkit2(netINP);
                netMSX = new EpanetMSX(epanetTK);
                try {
                    int ret = netMSX.load(fileMSX);
                    if(ret !=0){
                        JOptionPane.showMessageDialog(root, "MSX parsing error " + ret, "Error", JOptionPane.OK_OPTION);
                        fileMSX = null;
                        netMSX = null;
                        epanetTK = null;
                        return;
                    }
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(root, "IO error while reading the MSX file", "Error", JOptionPane.OK_OPTION);
                    fileMSX = null;
                    netMSX = null;
                    epanetTK = null;
                    return;
                }
            }
        }

        reportFileDialog = new JFileChooser();
    }

    /**
     * Create and show the window.
     * @param owner
     */
    public void showWindow(JFrame owner, boolean reset){

        if(reset){
            // Adjust widgets before showing the window.
            if(netINP!=null){
                try {
                    PropertiesMap pMap = netINP.getPropertiesMap();
                    if(pMap.getUnitsflag()== PropertiesMap.UnitsType.SI)
                        unitsBox.setSelectedIndex(0);
                    else
                        unitsBox.setSelectedIndex(1);
                    reportPeriodBox.setSelectedIndex(TimeSteps.getNearestStep(pMap.getRstep()).id);
                    hydComboBox.setSelectedIndex(TimeSteps.getNearestStep(pMap.getHstep()).id);
                    qualComboBox.setSelectedIndex(TimeSteps.getNearestStep(pMap.getQstep()).id);
                    textSimulationDuration.setText(Utilities.getClockTime(pMap.getDuration()));
                    textReportStart.setText(Utilities.getClockTime(pMap.getRstart()));

                    if(netINP.getPropertiesMap().getQualflag() != PropertiesMap.QualType.NONE)
                        qualityCheckBox.setEnabled(true);
                    else
                        qualityCheckBox.setEnabled(false);
                } catch (ENException e) {
                    e.printStackTrace();
                }
            }

            if(netMSX !=null && netMSX.getSpeciesNames().length>0 ){
                String [] speciesNames = netMSX.getSpeciesNames();

                speciesCheckList.setListData(createData(speciesNames));

                speciesCheckList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                speciesCheckList.setCellRenderer(new CheckListRenderer());

                speciesCheckList.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if(!speciesCheckList.isEnabled())
                            return;
                        int index = speciesCheckList.locationToIndex(e.getPoint());
                        CheckableItem item = (CheckableItem)speciesCheckList.getModel().getElementAt(index);
                        item.setSelected(! item.isSelected());
                        Rectangle rect = speciesCheckList.getCellBounds(index, index);
                        speciesCheckList.repaint(rect);
                    }
                });
            }
            else
                qualityMSXCheckBox.setEnabled(false);

            mainFrame = new JDialog(owner,true);
            mainFrame.add(root);
            mainFrame.setTitle("Reporting options");
            mainFrame.setResizable(false);
            mainFrame.pack();
            mainFrame.setLocationRelativeTo(null);
            mainFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

            hydraulicsCheckBox.addActionListener(this);
            qualityMSXCheckBox.addActionListener(this);
            runButton.addActionListener(this);
            qualityCheckBox.addActionListener(this);
            cancelButton.addActionListener(this);
            reportPeriodBox.addActionListener(this);
            hydComboBox.addActionListener(this);
            textSimulationDuration.addActionListener(this);
            textReportStart.addActionListener(this);

            mainFrame.addWindowListener(new WindowListener() {
                public void windowOpened(WindowEvent e){}
                public void windowClosing(WindowEvent e) {
                    if(hydSim!=null){
                        try {
                            hydSim.stopRunning();
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                    if(netMSX!=null){
                        try {
                            netMSX.stopRunning();
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
                public void windowClosed(WindowEvent e){}
                public void windowIconified(WindowEvent e){}
                public void windowDeiconified(WindowEvent e){}
                public void windowActivated(WindowEvent e){}
                public void windowDeactivated(WindowEvent e){}
            });
        }

        mainFrame.setVisible(true);
        progressPanel.setVisible(false);
        actions.setVisible(true);

        unlockInterface();
    }

    /**
     * Custom components creation.
     */
    private void createUIComponents() {
        String[] unitsString = { "SI", "US"};
        unitsBox = new JComboBox(unitsString);

        reportPeriodBox = new JComboBox(TimeSteps.getPeriodsText());
        hydComboBox = new JComboBox(TimeSteps.getPeriodsText());
        qualComboBox = new JComboBox(TimeSteps.getPeriodsText());
        speciesCheckList = new JList();

        hydVariables = new JList(createData(ReportGenerator.HydVariable.getNames()));
        hydVariables.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        hydVariables.setCellRenderer(new CheckListRenderer());

        for(int i = 0;i< ReportGenerator.HydVariable.values().length;i++)
            ((CheckableItem)hydVariables.getModel().getElementAt(i)).setSelected(true);

        hydVariables.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if(!hydVariables.isEnabled())
                    return;
                int index = hydVariables.locationToIndex(e.getPoint());
                CheckableItem item = (CheckableItem)hydVariables.getModel().getElementAt(index);
                item.setSelected(! item.isSelected());
                Rectangle rect = hydVariables.getCellBounds(index, index);
                hydVariables.repaint(rect);
            }
        });

        qualityVariables = new JList(createData(ReportGenerator.QualVariable.getNames()));
        qualityVariables.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        qualityVariables.setCellRenderer(new CheckListRenderer());

        for(int i = 0;i<ReportGenerator.QualVariable.values().length;i++)
            ((CheckableItem)qualityVariables.getModel().getElementAt(i)).setSelected(true);

        qualityVariables.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if(!qualityVariables.isEnabled())
                    return;
                int index = qualityVariables.locationToIndex(e.getPoint());
                CheckableItem item = (CheckableItem)qualityVariables.getModel().getElementAt(index);
                item.setSelected(! item.isSelected());
                Rectangle rect = qualityVariables.getCellBounds(index, index);
                qualityVariables.repaint(rect);
            }
        });
    }


    /**
     * Handle ui components events.
     * @param e
     */
    public void actionPerformed(ActionEvent e) {
        if(e.getSource().equals(hydraulicsCheckBox)){
            if(hydraulicsCheckBox.isSelected()){
                hydVariables.setEnabled(true);
            }
            else{
                hydVariables.setEnabled(false);
            }
        }
        else if(e.getSource().equals(qualityCheckBox)){
            if(qualityCheckBox.isSelected()){
                qualityVariables.setEnabled(true);
            }
            else{
                qualityVariables.setEnabled(false);
            }
        }
        else if(e.getSource().equals(qualityMSXCheckBox)){
            if(qualityMSXCheckBox.isSelected()){
                speciesCheckList.setEnabled(true);
            }
            else
                speciesCheckList.setEnabled(false);
        }
        else if(e.getSource().equals(cancelButton)){
            mainFrame.setVisible(false);
        }
        else if(e.getSource().equals(hydComboBox)){
            if(reportPeriodBox.getSelectedIndex()< hydComboBox.getSelectedIndex() )
                reportPeriodBox.setSelectedIndex(hydComboBox.getSelectedIndex());
        }
        else if(e.getSource().equals(reportPeriodBox)){
            if(reportPeriodBox.getSelectedIndex()< hydComboBox.getSelectedIndex() )
                reportPeriodBox.setSelectedIndex(hydComboBox.getSelectedIndex());
        }
        else if(e.getSource().equals(textReportStart)){
            double val = Utilities.getHour(textReportStart.getText(), "") ;
            System.out.println(val);
        }
        else if(e.getSource().equals(textSimulationDuration)){
            double val = Utilities.getHour(textSimulationDuration.getText(), "") ;
            System.out.println(val);
        }
        else if(e.getSource().equals(runButton))
        {
            double tmpValue;
            if( (tmpValue=Utilities.getHour(textSimulationDuration.getText(),"") ) < 0){
                JOptionPane.showMessageDialog(mainFrame, "Invalid time expression for simulation duration", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if( (tmpValue=Utilities.getHour(textReportStart.getText(),"") ) < 0){
                JOptionPane.showMessageDialog(mainFrame, "Invalid time expression for report start time", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

//            actions.setVisible(false);  bug in windows java 7(? general contract)

            Thread repThread = new Thread() {

                @Override
                public void run() {

                    StreamHandler simHandler = null;

                    try{
                        // File save dialog
                        FileDialog fdialog = new FileDialog(mainFrame,"Save xlsx file",FileDialog.SAVE);
                        fdialog.setFilenameFilter(new XLSXFilterAWT());

                        String fileTitle = fileINP.getName();
                        if(fileTitle.lastIndexOf(".")!=-1)
                            fileTitle = fileTitle.substring(0,fileTitle.lastIndexOf("."));

                        fdialog.setFile("report_"+fileTitle);
                        fdialog.setVisible(true);

                        File xlsxFile = null;

                        if(fdialog.getFile()==null){
                            mainFrame.setVisible(false);
                            return;
                        }
                        else
                            xlsxFile = new File(fdialog.getDirectory()+fdialog.getFile()+".xlsx");

                        lockInterface();
                        progressPanel.setVisible(true);
                        progressBar.setValue(0);
                        progressBar.setMaximum(100);
                        progressBar.setMinimum(0);

                        long reportPeriod       = (long)TimeSteps.values()[reportPeriodBox.getSelectedIndex()].time;
                        long reportStartTime    = (long)(Utilities.getHour(textReportStart.getText(),"")*3600);
                        long hydTStep           = TimeSteps.values()[hydComboBox.getSelectedIndex()].time;
                        long qualTStep          = TimeSteps.values()[qualComboBox.getSelectedIndex()].time;
                        long durationTime       = (long)(Utilities.getHour(textSimulationDuration.getText(),"")*3600);


                        final PropertiesMap pMap = netINP.getPropertiesMap();

                        if(showHydraulicSolverEventsCheckBox.isSelected()){
                            try {
                                simHandler = new StreamHandler(new FileOutputStream(fdialog.getDirectory() + "hydEvents.log"), new SimpleFormatter()){
                                    @Override
                                    public boolean isLoggable(LogRecord record) {
                                        return record.getLevel()==Level.WARNING;
                                    }
                                };
                            } catch (FileNotFoundException e1) {
                                e1.printStackTrace();
                            }

                            simHandler.setFormatter(new HydLogFormater());
                            log.addHandler(simHandler);
                        }


                        statusLabel.setText("Simulating hydraulics");

                        try {
                            pMap.setRstart(reportStartTime);
                            pMap.setRstep(reportPeriod);
                            pMap.setHstep(hydTStep);
                            pMap.setDuration(durationTime);

                            hydSim = new HydraulicSim(netINP,log);

                            creatSPThread(10, 30, new SPInterface() {
                                @Override
                                public long getTime() {
                                    return hydSim.getHtime();
                                }

                                public double getValue() {
                                    try {
                                        return (float) getTime() / (float) pMap.getDuration();
                                    } catch (ENException e1) {
                                        return 0;
                                    }
                                }
                            });

                            hydSim.simulate(new File("hydFile.bin"));

                        }
                        catch (ENException e1)
                        {
                            if(e1.getCodeID()==1000)
                                throw new InterruptedException();

                            JOptionPane.showMessageDialog(root, Utilities.getError(Integer.toString(e1.getCodeID())) + "\nCheck epanet.log for detailed error description", "Error", JOptionPane.OK_OPTION);
                            return;
                        }



                        if(fileMSX!=null && qualityMSXCheckBox.isSelected()){
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }

                            statusLabel.setText("Simulating MSX");
                            try {
                                // reload MSX
                                netMSX = new EpanetMSX(epanetTK);
                                netMSX.load(fileMSX);

                                netMSX.getNetwork().setRstep(reportPeriod);
                                netMSX.getNetwork().setQstep(qualTStep);
                                netMSX.getNetwork().setRstart(reportStartTime);
                                netMSX.getNetwork().setDur(durationTime);

                                epanetTK.open(new File("hydFile.bin"));
                                creatSPThread(30,50, new SPInterface() {
                                    @Override
                                    public long getTime() {
                                        return netMSX.getQTime();
                                    }

                                    public double getValue() {
                                        try {
                                            return (float)netMSX.getQTime()/(float)pMap.getDuration();
                                        } catch (ENException e1) {
                                            return 0;
                                        }
                                    }
                                });
                                netMSX.run(new File("msxFile.bin"));
                                epanetTK.close();
                            } catch (IOException e1) {

                            } catch (ENException e1) {
                                throw new InterruptedException();
                            }
                            //netMSX.getReport().MSXrpt_write(new File("msxFile.bin"));
                        }

                        if(qualityCheckBox.isSelected())
                        {
                            try {
                                final QualitySim qSim = new QualitySim(netINP,log);
                                statusLabel.setText("Simulating Quality");
                                creatSPThread(30,50, new SPInterface() {
                                    @Override
                                    public long getTime() {
                                        return qSim.getQtime();
                                    }

                                    public double getValue() {
                                        try {
                                            return (float)qSim.getQtime()/(float)pMap.getDuration();
                                        } catch (ENException e1) {
                                            return 0;
                                        }
                                    }
                                });
                                qSim.simulate(new File("hydFile.bin"),new File("qualFile.bin"));
                            } catch (ENException e1) {
                                JOptionPane.showMessageDialog(root, Utilities.getError(Integer.toString(e1.getCodeID())) + "\nCheck epanet.log for detailed error description", "Error", JOptionPane.OK_OPTION);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }

                        progressBar.setValue(50);
                        haltSPPeekThread();
                        statusLabel.setText("Writting XLSX");

                        final ReportGenerator gen = new ReportGenerator(xlsxFile);
                        //log
                        try {
                            log.fine("Starting xlsx write");
                            if(showSummaryCheckBox.isSelected())
                                gen.writeSummary(fileINP, netINP, fileMSX,netMSX);

                            if(transposeResultsCheckBox.isSelected())
                                gen.setTransposedMode(true);

                            // Write hydraulic spreadsheets
                            boolean [] values = new boolean[ReportGenerator.HydVariable.values().length];
                            for(int i = 0;i< ReportGenerator.HydVariable.values().length;i++)
                                values[i] = ((CheckableItem)hydVariables.getModel().getElementAt(i)).isSelected();

                            statusLabel.setText("Writing hydraulic report");
                            haltSPPeekThread();
                            creatSPThread(50,60, new SPInterface() {
                                @Override
                                public long getTime() {
                                    return gen.getRtime();
                                }

                                public double getValue() {
                                    try {
                                        return (float)(gen.getRtime()-pMap.getRstart())/(float)pMap.getDuration();
                                    } catch (ENException e1) {
                                        return 0;
                                    }
                                }
                            });

                            gen.createHydReport(new File("hydFile.bin"),netINP,values);

                            if(qualityCheckBox.isSelected()){
                                statusLabel.setText("Writing quality report");
                                haltSPPeekThread();
                                creatSPThread(60,70, new SPInterface() {
                                    @Override
                                    public long getTime() {
                                        return gen.getRtime();
                                    }

                                    public double getValue() {
                                        try {
                                            return (float)(gen.getRtime()-pMap.getRstart())/(float)pMap.getDuration();
                                        } catch (ENException e1) {
                                            return 0;
                                        }
                                    }
                                });
                                boolean nodes = ((CheckableItem)qualityVariables.getModel().getElementAt(0)).isSelected();
                                boolean links = ((CheckableItem)qualityVariables.getModel().getElementAt(1)).isSelected();
                                gen.createQualReport(new File("qualFile.bin"), netINP, nodes, links);
                            }

                            // Write MSX quality spreadsheets
                            if(fileMSX!=null && qualityMSXCheckBox.isSelected()){
                                boolean [] valuesMSX = new boolean[speciesCheckList.getModel().getSize()];
                                for(int i = 0;i<speciesCheckList.getModel().getSize();i++)
                                    valuesMSX[i] = ((CheckableItem)speciesCheckList.getModel().getElementAt(i)).isSelected();

                                gen.createMSXReport(new File("msxFile.bin"), netINP, netMSX, epanetTK,valuesMSX);
                            }
                            statusLabel.setText("Writing workbook");
                            gen.writeWorksheet();
                            log.fine("Ending xlsx write");
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        } catch (ENException e1) {
                            e1.printStackTrace();
                        }
                    }
                    catch (InterruptedException e){
                        System.out.println("Simulation aborted !");
                        //JOptionPane.showMessageDialog(root, "Simulation aborted !", "Error", JOptionPane.OK_OPTION);
                    }

                    if(simHandler!=null)
                        log.removeHandler(simHandler);

                    haltSPPeekThread();

                    progressPanel.setVisible(false);
                    mainFrame.setVisible(false);
                    unlockInterface();
                }
            };

            repThread.start();
        }
    }

    /**
     * Lock the interface during the simulation.
     */
    private void lockInterface(){
        hydraulicsCheckBox.setEnabled(false);
        qualityCheckBox.setEnabled(false);
        qualityMSXCheckBox.setEnabled(false);
        hydVariables.setEnabled(false);
        qualityVariables.setEnabled(false);
        speciesCheckList.setEnabled(false);
        showSummaryCheckBox.setEnabled(false);
        showHydraulicSolverEventsCheckBox.setEnabled(false);
        reportPeriodBox.setEnabled(false);
        unitsBox.setEnabled(false);
        hydComboBox.setEnabled(false);
        textSimulationDuration.setEnabled(false);
        qualComboBox.setEnabled(false);
        textReportStart.setEnabled(false);
        transposeResultsCheckBox.setEnabled(false);
    }

    /**
     * Unlock the interface during the simulation.
     */
    private void unlockInterface(){
        hydraulicsCheckBox.setEnabled(true);
         transposeResultsCheckBox.setEnabled(true);

        if(netMSX!=null && netMSX.getSpeciesNames().length>0){
            qualityMSXCheckBox.setEnabled(true);
            if(qualityMSXCheckBox.isSelected())
                speciesCheckList.setEnabled(true);
        }

        hydVariables.setEnabled(true);
        try {
            if(netINP.getPropertiesMap().getQualflag()!=PropertiesMap.QualType.NONE){
                if(qualityCheckBox.isSelected()){
                    qualityVariables.setEnabled(true);
                    qualityCheckBox.setEnabled(true);
                }
            }
        } catch (ENException e) {
            e.printStackTrace();
        }

        showSummaryCheckBox.setEnabled(true);
        showHydraulicSolverEventsCheckBox.setEnabled(true);
        reportPeriodBox.setEnabled(true);
        unitsBox.setEnabled(true);
        hydComboBox.setEnabled(true);
        textSimulationDuration.setEnabled(true);
        qualComboBox.setEnabled(true);
        textReportStart.setEnabled(true);
    }

    private void haltSPPeekThread(){
        SPThreadState = false;
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Simple state variable to help terminate the progress peek thread.
     */
    private transient boolean SPThreadState  = false;

    /**
     * Simple abstract class used to peek the simulation progress into the progress bar.
     */
    private abstract class SPInterface{
        public abstract double getValue();
        public long getTime(){return 0;};
    }

    /**
     * This method with the SPInterface class peeks the progress of the simulation.
     * @param start Progress bar start value
     * @param end Progress bar end value
     * @param var New instance of a child class of SPInterface to peek the progress.
     */
    private Thread creatSPThread(final int start,final int end, final SPInterface var){
        SPThreadState = true;
        final Thread repThread = new Thread() {
            @Override
            public void run() {
                String initName = statusLabel.getText();
                while(true){
                    if(var.getTime()!=0)
                        statusLabel.setText(initName+ " (" + Utilities.getClockTime(var.getTime()) + ")");
                    progressBar.setValue((int)(start*(1.0f-var.getValue()) + end*var.getValue()));
                    if(var.getValue()>0.9)
                        return;
                    if(!SPThreadState)
                        return;
                }
            }
        };
        repThread.start();
        return repThread;
    }

    /**
     * List checkbox item.
     */
    private class CheckableItem {
        private String str;
        private boolean isSelected;

        public CheckableItem(String str) {
            this.str = str;
            isSelected = true;
        }

        public void setSelected(boolean b) {
            isSelected = b;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public String toString() {
            return str;
        }
    }

    /**
     * Checkbox list renderer.
     */
    private class CheckListRenderer extends JCheckBox implements ListCellRenderer {

        public CheckListRenderer() {
            setBackground(UIManager.getColor("List.textBackground"));
            setForeground(UIManager.getColor("List.textForeground"));
        }

        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected, boolean hasFocus) {
            setEnabled(list.isEnabled());
            setSelected(((CheckableItem)value).isSelected());
            setFont(list.getFont());
            setText(value.toString());
            return this;
        }
    }

    /**
     * Create checkbox entries from string array.
     * @param strs Entries strings.
     * @return CheckableItem array.
     */
    private CheckableItem[] createData(String[] strs) {
        int n = strs.length;
        CheckableItem[] items = new CheckableItem[n];
        for (int i=0;i<n;i++) {
            items[i] = new CheckableItem(strs[i]);
        }
        return items;
    }


    /**
     * XLSX File filter for the file dialog.
     */
    private class XLSXFilterAWT implements FilenameFilter {
        public boolean accept(File dir, String name) {
            String[] name_extension = name.split("[.]");
            if (name_extension.length >= 2) {
                if (name_extension[name_extension.length - 1].equals("xlsx"))
                    return true;
            }

            return false;
        }
    }


}

