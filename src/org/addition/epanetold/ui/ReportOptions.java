package org.addition.epanetold.ui;

import org.addition.epanetold.ENToolkit;
import org.addition.epanetold.Epanet;
import org.addition.epanetold.Utilities;
import org.addition.epanetMSX.EpanetMSX;
import org.addition.epanetMSX.Structures.Species;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;


public class ReportOptions implements ActionListener {

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


    private JFileChooser reportFileDialog;

    //int minTimeStep;
    EpanetMSX epanetMSX;
    Epanet epanet;

    long epanetReportStep;
    long epanetStartTime;
    long epanetSimDuration;
    long epanetHydTStep;
    long epanetQualStep;

    JDialog mainFrame;

    public static final String [] hydArray = new String[]{"Node head", "Node actual demand","Link flow","Link status", "Link setting","Node pressure","Link velocity", "Link unit headloss", "Link friction factor"};
    public static final boolean [] hydDefaultSelection = new boolean[]{true,true,true,false,false,true,true,true,false};

    public static final String [] qualArray = new String[]{"Node quality", "Link quality","Link reaction rate"};
    public static final boolean [] qualDefaultSelecion = new boolean[]{true,false,false};

    private enum TimeSteps {
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

        public final int id;
        public final String name;
        public final int time;

        private TimeSteps(int id,int time, String name) {
            this.id = id;
            this.name = name;
            this.time = time;
        }

        public static String [] getPeriodsText(){
            String [] vec = new String[values().length];
            for(TimeSteps step : values())
                vec[step.id] = step.name;
            return vec;
        }
        public static TimeSteps getNearestStep(long time){
            for(TimeSteps step : values()){
                if(step.time>=time)
                    return step;

            }
            return STEP_12_HOURS;
        }
    }
    static final ResourceBundle errorBundle = PropertyResourceBundle.getBundle("Error");

    public ReportOptions(Epanet epa, EpanetMSX msx) {
        epanetMSX = msx;
        epanet = epa;
        reportFileDialog = new JFileChooser();
    }


    public void showWindow(JFrame owner){


        mainFrame = new JDialog(owner,true);
        mainFrame.add(root);
        mainFrame.setTitle("Reporting options");
        mainFrame.setResizable(false);
        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);

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
            public void windowClosing(WindowEvent e) {/*System.exit(0);*/}
            public void windowClosed(WindowEvent e){}
            public void windowIconified(WindowEvent e){}
            public void windowDeiconified(WindowEvent e){}
            public void windowActivated(WindowEvent e){}
            public void windowDeactivated(WindowEvent e){}
        });


        if(epanetMSX==null || epanetMSX.getNetwork().getSpecies().length == 0){
            qualityMSXCheckBox.setEnabled(false);
            qualityCheckBox.setEnabled(true);
            textSimulationDuration.setText(Utilities.clocktime(epanetSimDuration));
            textReportStart.setText(Utilities.clocktime(epanetStartTime));
        }
        else{
            qualityMSXCheckBox.setEnabled(true);
            qualityCheckBox.setEnabled(false);
            textSimulationDuration.setText(Utilities.clocktime(epanetSimDuration));
            textReportStart.setText(Utilities.clocktime(epanetStartTime));
        }


        mainFrame.setVisible(true);


    }

    public static void main(String[] args) {
        ReportOptions rep = new ReportOptions(null,null);
        rep.showWindow(null);
    }

    private void createUIComponents() {
        String[] unitsString = { "SI", "US"};
        unitsBox = new JComboBox(unitsString);
        unitsBox.setSelectedIndex(0);

        //if(epanet!=null)
        //    minTimeStep = (int)epanet.getNetwork().getHstep();
        //int max = 1;
        //
        //if(minTimeStep<7200)
        //    max = (int)Math.ceil(Math.log(7200/minTimeStep)/Math.log(2));
        //
        //String[] reportPeriods = new String[max+1];
        //for(int i = 0;i<=max;i++){
        //    reportPeriods[i] =  Utilities.clocktime((long)(minTimeStep * Math.pow(2,i)));
        //}
        //reportPeriodBox = new JComboBox(reportPeriods);

        if(epanetMSX==null || epanetMSX.getNetwork().getSpecies().length == 0){
            epanetSimDuration =epanet.getNetwork().getDur();
            epanetStartTime = epanet.getNetwork().getRstart();
            epanetHydTStep = epanet.getNetwork().getHstep();
            epanetQualStep = epanet.getNetwork().getQstep();
            epanetReportStep = epanet.getNetwork().getRstep();
        }
        else{
            epanetStartTime = epanetMSX.getENToolkit().ENgettimeparam(ENToolkit.EN_REPORTSTART);
            epanetSimDuration = epanetMSX.getENToolkit().ENgettimeparam(ENToolkit.EN_DURATION);
            epanetQualStep = epanetMSX.getENToolkit().ENgettimeparam(ENToolkit.EN_QUALSTEP);
            epanetHydTStep = epanetMSX.getENToolkit().ENgettimeparam(ENToolkit.EN_HYDSTEP);
            epanetReportStep = epanetMSX.getENToolkit().ENgettimeparam(ENToolkit.EN_REPORTSTEP);
        }

        reportPeriodBox = new JComboBox(TimeSteps.getPeriodsText());
        hydComboBox = new JComboBox(TimeSteps.getPeriodsText());
        qualComboBox = new JComboBox(TimeSteps.getPeriodsText());

        reportPeriodBox.setSelectedIndex(TimeSteps.getNearestStep(epanetReportStep).id);
        hydComboBox.setSelectedIndex(TimeSteps.getNearestStep(epanetHydTStep).id);
        qualComboBox.setSelectedIndex(TimeSteps.getNearestStep(epanetQualStep).id);

        if(epanetMSX==null || epanetMSX.getNetwork().getSpecies().length == 0){
            speciesCheckList = new JList();
        }
        else if(epanetMSX !=null && epanetMSX.getNetwork().getSpecies().length>0 )
        {
            Species [] spArray = epanetMSX.getNetwork().getSpecies();
            String [] speciesNames = new String[spArray.length-1];
            for(int i = 1;i<spArray.length;i++)
                speciesNames[i-1] = (spArray[i].getId());

            speciesCheckList = new JList(createData(speciesNames));


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



        hydVariables = new JList(createData(hydArray));
        hydVariables.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        hydVariables.setCellRenderer(new CheckListRenderer());

        for(int i = 0;i<hydDefaultSelection.length;i++)
            ((CheckableItem)hydVariables.getModel().getElementAt(i)).setSelected(hydDefaultSelection[i]);

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


        qualityVariables = new JList(createData(qualArray));
        qualityVariables.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        qualityVariables.setCellRenderer(new CheckListRenderer());

        for(int i = 0;i<qualArray.length;i++)
            ((CheckableItem)qualityVariables.getModel().getElementAt(i)).setSelected(qualDefaultSelecion[i]);

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

    private boolean getHydVariableStatus(int id){
        CheckableItem item = (CheckableItem) hydVariables.getModel().getElementAt(id);
        return item.isSelected();
    }

    private boolean getSpeciesVariableStatus(int var){
        CheckableItem item = (CheckableItem) speciesCheckList.getModel().getElementAt(var);
        return item.isSelected();
    }

    //private int getReportPeriod(){
    //    return minTimeStep * (int)Math.pow(2,reportPeriodBox.getSelectedIndex());
    //}

    private boolean getSIUnits(){
        if(unitsBox.getSelectedIndex() == 0)
            return true;
        else return false;
    }

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
            double val = Utilities.hour(textReportStart.getText(),"") ;
            System.out.println(val);
        }
        else if(e.getSource().equals(textSimulationDuration)){
            double val = Utilities.hour(textSimulationDuration.getText(),"") ;
            System.out.println(val);
        }
        else if(e.getSource().equals(runButton))
        {


            double tmpValue;
            if( (tmpValue=Utilities.hour(textSimulationDuration.getText(),"") ) < 0){
                JOptionPane.showMessageDialog(mainFrame, "Invalid time expression for simulation duration", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if( (tmpValue=Utilities.hour(textReportStart.getText(),"") ) < 0){
                JOptionPane.showMessageDialog(mainFrame, "Invalid time expression for report start time", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            actions.setVisible(false);

            Thread repThread = new Thread() {

                @Override
                public void run() {



                    long reportPeriod = (long)TimeSteps.values()[reportPeriodBox.getSelectedIndex()].time;
                    long reportStartTime = (long)(Utilities.hour(textReportStart.getText(),"")*3600);
                    long hydTStep = TimeSteps.values()[hydComboBox.getSelectedIndex()].time;
                    long qualTStep = TimeSteps.values()[qualComboBox.getSelectedIndex()].time;
                    long durationTime = (long)(Utilities.hour(textSimulationDuration.getText(),"")*3600);

                    // File save dialog
                    FileDialog fdialog = new FileDialog(mainFrame,"Save xlsx file",FileDialog.SAVE);
                    fdialog.setFilenameFilter(new XLSXFilterAWT());

                    String fileTitle = epanet.getInpFile().getName();
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

                    // Create new instances of Epanet and EpanetMSX
                    final Epanet nEpanet = new Epanet();
                    final EpanetMSX nEpanetMSX = new EpanetMSX();

                    nEpanet.setHydFilename(epanet.getHydFile().getAbsolutePath());
                    nEpanet.setOutFilename(epanet.getOutFile().getAbsolutePath());

                    File tempHydEventsFile=null;
                    BufferedWriter tempHydEventsBuffer=null;

                    File tempSummaryFile=null;
                    BufferedWriter tempSummaryBuffer=null;

                    Long startTime=null;
                    Long endTime=null;
                    Long hydTime=null;

                    ReportGenerator gen;

                    if(showHydraulicSolverEventsCheckBox.isSelected()){
                        try {
                            tempHydEventsFile =  File.createTempFile("TempHydText","txt");
                            tempHydEventsBuffer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempHydEventsFile),"UTF8"));//(new FileWriter(tempHydEventsFile));

                        } catch (IOException e1) {
                            JOptionPane.showMessageDialog(mainFrame, "Unable to create temporary events file!", "Error", JOptionPane.ERROR_MESSAGE);
                            e1.printStackTrace();
                            return;
                        }
                    }

                    if(showSummaryCheckBox.isSelected()){
                        try {
                            tempSummaryFile =  File.createTempFile("TempSumaryText","txt");
                            tempSummaryBuffer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempSummaryFile),"UTF8"));//BufferedWriter(new FileWriter(tempSummaryFile));

                        } catch (IOException e1) {
                            JOptionPane.showMessageDialog(mainFrame, "Unable to create temporary summary file!", "Error", JOptionPane.ERROR_MESSAGE);
                            e1.printStackTrace();
                            return;
                        }
                    }

                    if(epanetMSX == null){
                        startTime = System.currentTimeMillis();
                        int errCode;

                        statusLabel.setText("Loading network");
                        errCode = nEpanet.loadINP(epanet.getInpFile().getAbsolutePath());

                        if (errCode == 0)errCode = nEpanet.prepareNetwork();
                        progressBar.setValue(10);

                        statusLabel.setText("Simulating hydraulics");

                        creatSPThread(10,30, new SPInterface() {
                            @Override
                            public long getTime() {
                                return nEpanet.getHydraulicsSolver().getHtime();
                            }
                            public double getValue() {
                                return (float)nEpanet.getHydraulicsSolver().getHtime()/(float)nEpanet.getNetwork().getDur();
                            }
                        });

                        nEpanet.getNetwork().setRstep(reportPeriod);
                        nEpanet.getNetwork().setHydTStep(hydTStep);
                        nEpanet.getNetwork().setDuration(durationTime);
                        nEpanet.getNetwork().setQualTStep(qualTStep);

                        if(showHydraulicSolverEventsCheckBox.isSelected()){
                            try {
                                tempHydEventsFile =  File.createTempFile("TempHydText","txt");
                                tempHydEventsBuffer = new BufferedWriter(new FileWriter(tempHydEventsFile));

                            } catch (IOException e1) {
                                JOptionPane.showMessageDialog(mainFrame, "Unable to create temporary file!", "Error", JOptionPane.ERROR_MESSAGE);
                                e1.printStackTrace();
                                return;
                            }

                            if (errCode == 0)
                                errCode = nEpanet.simulateHydraulics2(tempHydEventsBuffer,true);

                            try {
                                tempHydEventsBuffer.flush();
                                tempHydEventsBuffer.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }
                        else
                            errCode = nEpanet.simulateHydraulics(true);

                        hydTime = System.currentTimeMillis();

                        statusLabel.setText("Simulating quality");
                        creatSPThread(30,50, new SPInterface() {
                            @Override
                            public long getTime() {
                                return nEpanet.getQuality().getQtime();
                            }
                            public double getValue() {
                                return (float)nEpanet.getQuality().getQtime()/(float)nEpanet.getNetwork().getDur();
                            }
                        });

                        if (errCode == 0)
                            errCode = nEpanet.simulateQuality();

                        if (errCode == 0)
                            errCode = nEpanet.close();

                        if (errCode != 0) {
                            JOptionPane.showMessageDialog(mainFrame, errorBundle.getString("ERR" + errCode), "Error", JOptionPane.ERROR_MESSAGE);
                        }

                        if(tempSummaryBuffer!=null){
                            nEpanet.getReport().writesummary2(tempSummaryBuffer);
                            try {
                                tempSummaryBuffer.close();
                            } catch (IOException e1) {}
                        }

                        endTime = System.currentTimeMillis();

                        gen = new ReportGenerator(nEpanet,null);
                    }
                    else
                    {

                        startTime = System.currentTimeMillis();

                        statusLabel.setText("Loading network and MSX");
                        int code = 0;
                        if ( (code = nEpanetMSX.load(epanet.getInpFile().getAbsolutePath(),epanetMSX.getNetwork().getMsxFile().getFilename())) != 0) {
                            JOptionPane.showMessageDialog(mainFrame, "Epanet MSX parsing error : " + code, "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        statusLabel.setText("Solving MSX");
                        creatSPThread(20,50, new SPInterface() {
                            @Override
                            public long getTime() {
                                return nEpanetMSX.getNetwork().getQtime();
                            }

                            public double getValue() {
                                return (float)nEpanetMSX.getNetwork().getQtime()/(float)nEpanetMSX.getNetwork().getDuration();
                            }
                        });

                        nEpanetMSX.getNetwork().setDur(durationTime);
                        nEpanetMSX.getNetwork().setQstep(qualTStep);
                        nEpanetMSX.getNetwork().setRstep(reportPeriod);

                        nEpanetMSX.getENToolkit().getNetwork().setDuration(durationTime);
                        nEpanetMSX.getENToolkit().getNetwork().setHydTStep(hydTStep);
                        nEpanetMSX.getENToolkit().getNetwork().setRstep(reportPeriod);

                        long [] val = new long[1];

                        if(showHydraulicSolverEventsCheckBox.isSelected()){
                            if ( (code = nEpanetMSX.run(tempHydEventsBuffer,epanet.getHydFile().getAbsolutePath(),epanet.getOutFile().getAbsolutePath(),val)) != 0) {
                                JOptionPane.showMessageDialog(mainFrame, "Epanet MSX error : " + code, "Error", JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        }
                        else{
                            if ( (code = nEpanetMSX.run(epanet.getHydFile().getAbsolutePath(),epanet.getOutFile().getAbsolutePath(),val)) != 0) {
                                JOptionPane.showMessageDialog(mainFrame, "Epanet MSX error : " + code, "Error", JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        }

                        hydTime = val[0];

                        if(tempSummaryBuffer!=null){
                            nEpanetMSX.getENToolkit().getReport().writesummary2(tempSummaryBuffer);
                            try {
                                tempSummaryBuffer.close();
                            } catch (IOException e1) {}
                        }

                        endTime = System.currentTimeMillis();


                        gen = new ReportGenerator(null,nEpanetMSX);
                    }

                    gen.setReportStartTime(reportStartTime);
                    gen.setTimings(startTime,hydTime,endTime);

                    // Prepare Report
                    for(int i = 0;i<hydVariables.getModel().getSize();i++)
                        gen.setHydVariables(i, ((CheckableItem)hydVariables.getModel().getElementAt(i)).isSelected());
                    gen.setShowHydraulics(hydraulicsCheckBox.isSelected());

                    gen.setSiUnits(getSIUnits());

                    gen.setReportPeriod(TimeSteps.values()[reportPeriodBox.getSelectedIndex()].time); //getReportPeriod());

                    gen.setShowQuality(qualityCheckBox.isSelected());
                    for(int i = 0;i<qualityVariables.getModel().getSize();i++)
                        gen.setQualityVariables(i, ((CheckableItem)qualityVariables.getModel().getElementAt(i)).isSelected());

                    gen.setShowMSXQuality(qualityMSXCheckBox.isSelected());
                    for(int i = 0;i<speciesCheckList.getModel().getSize();i++)
                        gen.setSpeVariables(i, ((CheckableItem) speciesCheckList.getModel().getElementAt(i)).isSelected());

                    //
                    if(tempSummaryFile!=null)
                    {
                        statusLabel.setText("Writing summary");
                        gen.writeSummary(tempSummaryFile);
                        tempSummaryFile.delete();
                    }


                    //
                    try {
                        statusLabel.setText("Writing hydraulic report");
                        gen.writeHydraulics();
                        progressBar.setValue(60);

                        statusLabel.setText("Writing quality report");
                        gen.writeQuality();
                        progressBar.setValue(70);

                        gen.writeQualityMSX();
                        progressBar.setValue(80);

                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                    if(tempHydEventsFile!=null)
                    {
                        try {tempHydEventsBuffer.close();} catch (IOException e1) {}
                        statusLabel.setText("Writing hydraulic solver events");
                        gen.writeEvents(tempHydEventsFile);
                        tempHydEventsFile.delete();
                    }

                    try {
                        statusLabel.setText("Saving report to XLSX");
                        //SimpleDateFormat sformat = new SimpleDateFormat("yyMMddHHmm") ;

                        //if(showSaveDlg.isSelected()){
                        //FileDialog fdialog = new FileDialog(mainFrame,"Save xlsx file",FileDialog.SAVE);
                        //fdialog.setFilenameFilter(new XLSXFilterAWT());
                        //
                        //String fileTitle = epanet.getInpFile().getName();
                        //if(fileTitle.lastIndexOf(".")!=-1)
                        //    fileTitle = fileTitle.substring(0,fileTitle.lastIndexOf("."));
                        //
                        //fdialog.setFile("report_"+fileTitle);
                        //fdialog.setVisible(true);
                        //
                        //if(fdialog.getFile()!=null){
                        //    gen.saveReport(fdialog.getDirectory()+fdialog.getFile()+".xlsx");
                        //    progressBar.setValue(100);
                        //    JOptionPane.showMessageDialog(mainFrame, "Simulation report saved", "Success", JOptionPane.INFORMATION_MESSAGE);
                        //}
                        gen.saveReport(xlsxFile.getAbsolutePath());
                        progressBar.setValue(100);
                        JOptionPane.showMessageDialog(mainFrame, "Simulation report saved", "Success", JOptionPane.INFORMATION_MESSAGE);
                        //}
                        //else{
                        //    String fileTitle = epanet.getInpFile().getName();
                        //    if(fileTitle.lastIndexOf(".")!=-1)
                        //        fileTitle = fileTitle.substring(0,fileTitle.lastIndexOf("."));
                        //    gen.saveReport(epanet.getInpFile().getParent() + "/"+ "report_" + fileTitle + ".xlsx");
                        //    progressBar.setValue(100);
                        //    JOptionPane.showMessageDialog(mainFrame, "Simulation report saved", "Success", JOptionPane.INFORMATION_MESSAGE);
                        //}
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                    progressPanel.setVisible(false);
                    mainFrame.setVisible(false);

                }
            };

            repThread.start();
        }
    }

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


    }
    private abstract class SPInterface{
        public abstract double getValue();
        public long getTime(){return 0;};
    }

    private void creatSPThread(final int start,final int end, final SPInterface var){
        final Thread repThread = new Thread() {
            @Override
            public void run() {
                String initName = statusLabel.getText();
                while(true){
                    if(var.getTime()!=0)
                        statusLabel.setText(initName+ " (" + Utilities.clocktime(var.getTime()) + ")");
                    progressBar.setValue((int)(start*(1.0f-var.getValue()) + end*var.getValue()));
                    if(var.getValue()>0.9)
                        return;
                }
            }
        };
        repThread.start();
    }

    class CheckableItem {
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

    class CheckListRenderer extends JCheckBox implements ListCellRenderer {

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

    private CheckableItem[] createData(String[] strs) {
        int n = strs.length;
        CheckableItem[] items = new CheckableItem[n];
        for (int i=0;i<n;i++) {
            items[i] = new CheckableItem(strs[i]);
        }
        return items;
    }
}

