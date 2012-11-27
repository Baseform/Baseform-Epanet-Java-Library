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

import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;
import org.addition.epanet.util.ENException;
import org.addition.epanet.network.Network;
import org.addition.epanet.network.io.input.InputParser;
import org.addition.epanet.network.io.output.OutputComposer;
import org.addition.epanet.network.structures.Node;
import org.addition.epanet.network.structures.Tank;
import org.addition.epanet.util.ENLevels;
import org.addition.epanet.util.Utilities;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.*;

/**
 * The frontend UI class for the Aware-P Epanet application.
 */
public class EpanetUI implements ActionListener {

    private static final String WEBLINK = "http://www.baseform.org/?epaToolLink";
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
    private JLabel textHydraulic;
    private JLabel msxName;
    private JPanel middle;
    private JPanel bottom;
    private JLabel textPattern;
    private JButton saveButton;

    /**
     * Reference to the selected node in network map.
     */
    private Node selectedNode = null;
    /**
     * Window main frame.
     */
    private JFrame frame;
    /**
     * Abstract representation of the network file(INP/XLSX/XML).
     */
    private File inpFile;
    /**
     * Abstract representation of the msx config file.
     */
    private File msxFile;
    /**
     * Loaded hydraulic network.
     */
    private Network epanetNetwork;

    /**
     * Reference to the file chooser.
     */
    private JFileChooser fileChooser = null;

    /**
     * Reference to the report options window.
     */
    ReportOptions reportOptions;


    /**
     * Application title string.
     */
    private static final String APP_TITTLE = "Baseform Epanet Java";

    /**
     * Reference for the file>open entry.
     */
    private static JMenuItem openAction;
    /**
     * Reference for the file>save entry.
     */
    private static JMenuItem saveAction;
    /**
     * Reference for the file>run entry.
     */
    private static JMenuItem runAction;
    private Logger log;
    private static final String LOG_FILENAME = "log/epanet.log";

    /**
     * Hydraulic rendering Panel.
     */
    class NetworkPanel extends JPanel {

        public NetworkPanel() {
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (epanetNetwork == null)
                return;

            int w = this.getWidth();
            int h = this.getHeight();

            if (epanetNetwork == null)
                return;

            NetworkImage.drawNetwork(g, w, h, epanetNetwork, checkPipes.isSelected(), checkTanks.isSelected(), checkNodes.isSelected(), selectedNode);
        }
    }

    /**
     * Create the custom ui components.
     */
    private void createUIComponents() {
        root = new JPanel();
        top = new JPanel();
        right = new JPanel();
        properties = new JPanel();
        propertiesRoot = new JPanel();
        network = new NetworkPanel();
        network.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent mouseEvent) {
            }

            public void mousePressed(MouseEvent mouseEvent) {

                selectedNode = NetworkImage.peekNearest((int) network.getSize().getWidth(), (int) network.getSize().getHeight(),
                        mouseEvent.getX(), mouseEvent.getY(), epanetNetwork);
                network.repaint();
            }

            public void mouseReleased(MouseEvent mouseEvent) {
            }

            public void mouseEntered(MouseEvent mouseEvent) {
            }

            public void mouseExited(MouseEvent mouseEvent) {
            }
        });
    }


    /**
     * Aware-P Epanet frontend constructor.
     */
    public EpanetUI() {
        initLogger();

        frame = new JFrame();
        frame.setTitle(APP_TITTLE);
        frame.add(root);

        if (!Utilities.isMac()) {
            JMenuBar menuBar = new JMenuBar();

            JMenu fileMenu = new JMenu("File");
            menuBar.add(fileMenu);

            openAction = new JMenuItem("Open");
            saveAction = new JMenuItem("Save");
            runAction = new JMenuItem("Run");

            fileMenu.add(openAction);
            fileMenu.add(openAction);
            fileMenu.add(runAction);

            frame.setJMenuBar(menuBar);
        }

        openAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                openEvent();
                network.repaint();
            }
        });

        saveAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                saveEvent();
            }
        });

        runAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                runSimulation();
            }
        });

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
        saveButton.addActionListener(this);
        //runMSXButton.addActionListener(this);
        //saveReport.addActionListener(this);

        frame.addWindowListener(new WindowListener() {
            public void windowOpened(WindowEvent e) {
            }

            public void windowClosing(WindowEvent e) {
                for (Handler handler : log.getHandlers()) {
                    handler.flush();
                }
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

    private void initLogger() {
        log = Logger.getLogger(this.getClass().getName());
        log.setUseParentHandlers(false);
        log.setLevel(Level.ALL);

        try {
            new File(LOG_FILENAME).getParentFile().mkdirs();
            StreamHandler handle = new StreamHandler(new FileOutputStream(LOG_FILENAME), new SimpleFormatter()) {
                @Override
                public boolean isLoggable(LogRecord record) {
                    return true;//record.getLevel() == ENLevels.ERROR;
                }

                @Override
                public void publish(LogRecord record) {
                    super.publish(record);
                }
            };

            log.addHandler(handle);

        } catch (IOException e) {

        }
    }

    /**
     * Handle ui components events.
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(saveButton)) {

            //fileChooser.setTitle("Save File");
            //fileChooser.setFilenameFilter(new InputFilterAWT());
            //fileChooser.setVisible(true);
            //fileChooser.setMode(FileDialog.SAVE);
            //OutputComposer excelComp = OutputComposer.create(Network.FileType.EXCEL_FILE);

            saveEvent();
        } else if (e.getSource().equals(openINPButton)) {
            openEvent();
        } else if (e.getSource().equals(runSimulationButton)) {
            runSimulation();
        } else if (e.getSource() == logoB) {
            browse(WEBLINK);
        }
        network.repaint();
    }

    /**
     * Show report options window to configure and run the simulation.
     */
    private void runSimulation() {
        if (reportOptions == null) {
            reportOptions = new ReportOptions(inpFile, msxFile, log);
            reportOptions.showWindow(frame, true);
        } else
            reportOptions.showWindow(frame, false);
    }

    /**
     * Show the save dialog to save the network file.
     */
    private void saveEvent() {
        JFileChooser fileSaver = new JFileChooser(inpFile.getParent());
        fileSaver.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileSaver.setAcceptAllFileFilterUsed(false);
        fileSaver.addChoosableFileFilter(new XLSXFilter());
        fileSaver.addChoosableFileFilter(new XMLFilter());
        fileSaver.addChoosableFileFilter(new INPFilter());

        fileSaver.showSaveDialog(frame);

        if (fileSaver.getSelectedFile() == null)
            return;

        Network.FileType netType = Network.FileType.INP_FILE;


        if (fileSaver.getFileFilter() instanceof XLSXFilter) {
            netType = Network.FileType.EXCEL_FILE;
        } else if (fileSaver.getFileFilter() instanceof XMLFilter) {
            netType = Network.FileType.XML_FILE;
            JOptionPane.showMessageDialog(frame, "Not supported yet !", "Error", JOptionPane.OK_OPTION);
            return;
        }

        OutputComposer compose = OutputComposer.create(netType);

        String extension = "";

        if (Utilities.getFileExtension(fileSaver.getSelectedFile().getName()).equals(""))
            switch (netType) {

                case INP_FILE:
                    extension = ".inp";
                    break;
                case EXCEL_FILE:
                    extension = ".xlsx";
                    break;
                case XML_FILE:
                    extension = ".xml";
                    break;
            }

        try {
            compose.composer(epanetNetwork, new File(fileSaver.getSelectedFile().getAbsolutePath() + extension));
        } catch (ENException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Show the open dialog and open the INP/XLSX and XML files.
     */
    private void openEvent() {
        if (fileChooser == null) {
            //fileChooser = new FileDialog(frame);
            fileChooser = new JFileChooser(System.getProperty("user.dir"));
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.addChoosableFileFilter(new XLSXFilter());
            fileChooser.addChoosableFileFilter(new XMLFilter());
            fileChooser.addChoosableFileFilter(new MSXFilter());
            fileChooser.addChoosableFileFilter(new INPFilter());
            fileChooser.addChoosableFileFilter(new AllSuportedFilesFilter());
        }

        if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            File netFile = fileChooser.getSelectedFile();
            String fileExtension = Utilities.getFileExtension(netFile.getName());

            if (fileExtension.equals("xlsx") || fileExtension.equals("inp") || fileExtension.equals("xml")) {
                inpFile = netFile;
                msxFile = null;
                msxName.setText("");

                Network.FileType netType = Network.FileType.INP_FILE;
                if (fileExtension.equals("xlsx"))
                    netType = Network.FileType.EXCEL_FILE;
                else if (fileExtension.equals("xml")) {
                    netType = Network.FileType.XML_FILE;
                    JOptionPane.showMessageDialog(frame, "Not supported yet !", "Error", JOptionPane.OK_OPTION);
                    return;
                }


                epanetNetwork = new Network();
                InputParser inpParser = InputParser.create(netType, log);
                try {
                    inpParser.parse(epanetNetwork, inpFile);
                } catch (ENException en_ex) {
                    JOptionPane.showMessageDialog(frame, en_ex.toString() + "\nCheck epanet.log for detailed error description", "Error", JOptionPane.OK_OPTION);
                    clearInterface();
                    inpFile = null;
                    return;
                } catch (Exception egen) {
                    JOptionPane.showMessageDialog(frame, "Unable to parse network configuration file", "Error", JOptionPane.OK_OPTION);
                    log.log(ENLevels.ERROR, "openEvent", egen);
                    clearInterface();
                    inpFile = null;

                    return;
                }

                int resrvCount = 0;
                int tanksCount = 0;


                for (Tank tank : epanetNetwork.getTanks())
                    if (tank.getArea() == 0.0)
                        resrvCount++;
                    else
                        tanksCount++;


                textReservoirs.setText(Integer.toString(resrvCount));
                textTanks.setText(Integer.toString(tanksCount));
                textPipes.setText(Integer.toString(epanetNetwork.getLinks().size()));
                textNodes.setText(Integer.toString(epanetNetwork.getNodes().size()));
                try {
                    textDuration.setText(Utilities.getClockTime(epanetNetwork.getPropertiesMap().getDuration()));
                    textUnits.setText(epanetNetwork.getPropertiesMap().getUnitsflag().name());
                    textHeadloss.setText(epanetNetwork.getPropertiesMap().getFormflag().name());
                    textQuality.setText(epanetNetwork.getPropertiesMap().getQualflag().name());
                    textDemand.setText(epanetNetwork.getPropertiesMap().getDmult().toString());
                    textHydraulic.setText(Utilities.getClockTime(epanetNetwork.getPropertiesMap().getHstep()));
                    textPattern.setText(Utilities.getClockTime(epanetNetwork.getPropertiesMap().getPstep()));
                } catch (ENException ex) {
                }
                frame.setTitle(APP_TITTLE + inpFile.getName());
                inpName.setText(inpFile.getName());
                runSimulationButton.setEnabled(true);

                saveButton.setEnabled(true);
                reportOptions = null;
            } else if (fileExtension.equals("msx")) {
                if (inpFile == null) {
                    JOptionPane.showMessageDialog(frame, "Load an INP or XLSX file with network configuration before opening the MSX file.", "Error", JOptionPane.OK_OPTION);
                    return;
                }

                msxFile = netFile;
                msxName.setText(fileChooser.getSelectedFile().getName());//fileChooser.getFile());
                reportOptions = null;
            }

            saveAction.setEnabled(true);
            runAction.setEnabled(true);

        }
    }

    /**
     * Reset the interface layout
     */
    private void clearInterface() {
        epanetNetwork = null;
        inpFile = null;
        frame.setTitle(APP_TITTLE);
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
        saveAction.setEnabled(false);
        runAction.setEnabled(false);
        runSimulationButton.setEnabled(false);
    }

    /**
     * Aware-P Epanet application entry point
     *
     * @param args
     * @throws UnsupportedLookAndFeelException
     *
     */
    public static void main(String[] args) throws UnsupportedLookAndFeelException {
//        if (Utilities.isMac()) {
//            System.setProperty("apple.laf.useScreenMenuBar", "true");
//            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Aware-P Epanet");
//
//            try {
//
//                Class<?> appCl = Class.forName("com.apple.eawt.Application");
//                Object app = appCl.getMethod("getApplication", new Class[]{}).invoke(null);
//                Image dockImage = Toolkit.getDefaultToolkit().getImage(EpanetUI.class.getResource("/uiresources/ae.png"));
//                appCl.getMethod("setDockIconImage",java.awt.Image.class).invoke(app, dockImage);
//                JMenuBar menuBar = new JMenuBar();
//                JMenu fileMenu = new JMenu("File");
//                menuBar.add(fileMenu);
//
//                openAction = new JMenuItem("Open");
//                saveAction = new JMenuItem("Save");
//                runAction = new JMenuItem("Run");
//
//                fileMenu.add(openAction);
//                fileMenu.add(saveAction);
//                fileMenu.add(runAction);
//                appCl.getMethod("setDefaultMenuBar",javax.swing.JMenuBar.class).invoke(app,menuBar);
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }
        if (Utilities.isMac()) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Aware-P Epanet");

            try {

                Class<?> appCl = Class.forName("com.apple.eawt.Application");
                Object app = appCl.getMethod("getApplication", new Class[]{}).invoke(null);
                Image dockImage = Toolkit.getDefaultToolkit().getImage(EpanetUI.class.getResource("/uiresources/ae.png"));
                appCl.getMethod("setDockIconImage",java.awt.Image.class).invoke(app, dockImage);
                JMenuBar menuBar = new JMenuBar();
                JMenu fileMenu = new JMenu("File");
                menuBar.add(fileMenu);

                openAction = new JMenuItem("Open");
                saveAction = new JMenuItem("Save");
                runAction = new JMenuItem("Run");

                fileMenu.add(openAction);
                fileMenu.add(saveAction);
                fileMenu.add(runAction);
//                appCl.getMethod("setDefaultMenuBar",javax.swing.JMenuBar.class).invoke(app,menuBar);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        try {
            UIManager.setLookAndFeel(new Plastic3DLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        new EpanetUI();

    }

    /**
     * Open the aware-p webpage in the browser.
     *
     * @param url
     */
    private void browse(String url) {
        if (!java.awt.Desktop.isDesktopSupported()) {
            System.err.println("Desktop is not supported");
            return;
        }

        java.awt.Desktop desktop = java.awt.Desktop.getDesktop();

        if (!desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
            System.err.println("Desktop doesn't support the browse action");
            System.exit(1);
        }

        try {
            java.net.URI uri = new java.net.URI(url);
            desktop.browse(uri);
        } catch (Exception e) {

            System.err.println(e.getMessage());
        }
    }

    /**
     * INP file filter.
     */
    private class INPFilter extends FileFilter {

        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }

            if (Utilities.getFileExtension(f.getName()).equals("inp"))
                return true;
            return false;
        }

        public String getDescription() {
            return "Epanet INP network file (*.inp)";
        }
    }

    /**
     * XLSX file filter.
     */
    private class XLSXFilter extends FileFilter {

        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }

            if (Utilities.getFileExtension(f.getName()).equals("xlsx"))
                return true;
            return false;
        }

        public String getDescription() {
            return "Epanet XLSX network file (*.xlsx)";
        }
    }

    /**
     * XML file filter.
     */
    private class XMLFilter extends FileFilter {

        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }

            if (Utilities.getFileExtension(f.getName()).equals("xml"))
                return true;
            return false;
        }

        public String getDescription() {
            return "Epanet XML network file (*.xml)";
        }
    }

    /**
     * MSX file filter.
     */
    private class MSXFilter extends FileFilter {

        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }

            if (Utilities.getFileExtension(f.getName()).equals("msx"))
                return true;
            return false;
        }

        public String getDescription() {
            return "Epanet MSX quality file (*.msx)";
        }
    }

    private class AllSuportedFilesFilter extends FileFilter {

        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }

            String extension = Utilities.getFileExtension(f.getName());

            if (extension.equals("msx") || extension.equals("inp") || extension.equals("xlsx"))
                return true;
            return false;
        }

        public String getDescription() {
            return "Supported files (*.msx, *.inp, *.xlsx, *.xml)";
        }
    }
}


