package org.addition.epanetold;



import java.awt.*;
import javax.swing.*;

public class ProgressWindow extends Thread{

    static Boolean useDispose = true;
    JFrame rootFrame;
    JPanel rootPanel;
    JProgressBar progressBar;
    Boolean runFlag = false;
    Integer progressValue = 0;

    public static void setDisposable(Boolean isDisposable){
        useDispose = isDisposable;
    }

    public void run() {
        rootFrame = new JFrame();
        rootFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        rootPanel= new JPanel();
        rootPanel.setLayout(new FlowLayout());

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);

        rootPanel.add(progressBar);
        rootFrame.getContentPane().add(rootPanel);

        rootFrame.pack();
        rootFrame.setSize(300, 60);
        rootFrame.setLocationRelativeTo(null);

        rootFrame.setVisible(true);
        rootPanel.setVisible(true);
        progressBar.setVisible(true);

        rootFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        rootFrame.requestFocus();

        progressBar.setValue(0);

        synchronized(runFlag){
            while(runFlag){
                progressBar.setValue(progressValue);
                try{Thread.sleep(100);}
                catch (InterruptedException e){}
            }
        }

        rootFrame.setVisible(false);
        if(useDispose)
            rootFrame.dispose();

        rootFrame = null;
        rootPanel = null;
        progressBar = null;

    }

    public void setTitle(String title){
        if(rootFrame!=null)
            rootFrame.setTitle(title);
    }


    public void setProgress(int value){
        progressValue = value;
    }


    public void begin(){
        runFlag = true;
        this.start();

        while(rootFrame==null || rootFrame.isVisible()== false)
            try {
                sleep(10);
            } catch (InterruptedException e) {

            }

        progressBar.setFocusable(true);
    }

    public void end(){
        runFlag = false;
        try {
            this.join();
        } catch (InterruptedException e) {
            return;
        }
        //System.out.println("terminated");
    }
}