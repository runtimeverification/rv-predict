/*
 * IzPack - Copyright 2001-2008 Julien Ponge, All Rights Reserved.
 * 
 * http://izpack.org/
 * http://izpack.codehaus.org/
 * 
 * Copyright 2002 Jan Blok
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.izforge.izpack.panels;

import com.izforge.izpack.Info;
import com.izforge.izpack.gui.IzPanelLayout;
import com.izforge.izpack.gui.LabelFactory;
import com.izforge.izpack.gui.LayoutConstants;
import com.izforge.izpack.installer.InstallData;
import com.izforge.izpack.installer.InstallerFrame;
import com.izforge.izpack.installer.IzPanel;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;

import java.io.File;

import java.util.ArrayList;
import java.util.Properties;


/**
 * The RunPanel
 *
 * @author Patrick Meredith
 */
public class RunPanel extends IzPanel
{

    private static boolean runFlag = true;

    /**
     *
     */
    protected static final long serialVersionUID = 3257848774955905587L;

    private static String path = "";

    /**
     * The constructor.
     *
     * @param parent The parent.
     * @param idata  The installation data.
     */
    public RunPanel(InstallerFrame parent, InstallData idata)
    {
        this(parent, idata, new IzPanelLayout());
    }

    /**
     * Creates a new RunPanel object with the given layout manager. Valid layout manager are the
     * IzPanelLayout and the GridBagLayout. New panels should be use the IzPanelLaout. If lm is
     * null, no layout manager will be created or initialized.
     *
     * @param parent The parent IzPack installer frame.
     * @param idata  The installer internal data.
     * @param layout layout manager to be used with this IzPanel
     */

    public RunPanel(final InstallerFrame parent, InstallData idata, LayoutManager2 layout)
    {
        // Layout handling. This panel was changed from a mixed layout handling
        // with GridBagLayout and BoxLayout to IzPanelLayout. It can be used as an
        // example how to use the IzPanelLayout. For this there are some comments
        // which are excrescent for a "normal" panel.
        // Set a IzPanelLayout as layout for this panel.
        // This have to be the first line during layout if IzPanelLayout will be used.
        super(parent, idata, layout);

    }

    public void panelActivate(){
      parent.prevButton.setVisible(false);
      parent.nextButton.setVisible(false);
      parent.quitButton.setVisible(false);
      parent.prevButton.setEnabled(false);
      parent.nextButton.setEnabled(false);
      parent.quitButton.setEnabled(false);
      path = idata.getInstallPath(); 


        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));


        String str;
        str = "RV Predict Installation Complete!\n";
        JLabel welcomeLabel = new JLabel(str);
        panel.add(welcomeLabel);
        str = "To run RV Predict use: "; 
        welcomeLabel = new JLabel(str);
        panel.add(welcomeLabel);
        str = path + File.separator
            + "bin"  + File.separator + "rvpredict." + ((isWindows())? "bat" : "sh");

        welcomeLabel = new JLabel(str);
        panel.add(welcomeLabel);

        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        str = "To uninstall RV Predict use: ";         
        welcomeLabel = new JLabel(str);
        panel.add(welcomeLabel);
        str = path + File.separator
                   + "Uninstall"  + File.separator + "uninstall.jar,";
        welcomeLabel = new JLabel(str);
        panel.add(welcomeLabel);
        str = "which can be run as you would normally run a jar file";

        panel.add(Box.createRigidArea(new Dimension(0, 50)));

        JRadioButton runRVPredict = new JRadioButton("Run RV-Predict Now", true);
        runRVPredict.setActionCommand("runRVPredict");
        runRVPredict.addActionListener(
          new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
              if ("runRVPredict".equals(e.getActionCommand())){
                runFlag = true;
              }
            }
          });
        runRVPredict.setEnabled(true);
        panel.add(runRVPredict);

        JRadioButton doNotRunRVPredict = new JRadioButton("Do not run RV-Predict Now");
        doNotRunRVPredict.setActionCommand("doNotRunRVPredict");
        doNotRunRVPredict.addActionListener(
          new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
              if ("doNotRunRVPredict".equals(e.getActionCommand())){
                runFlag = false;
              }
            }
          });
        doNotRunRVPredict.setEnabled(true);
        panel.add(doNotRunRVPredict);

        ButtonGroup group = new ButtonGroup();
        group.add(runRVPredict);
        group.add(doNotRunRVPredict);

        panel.add(Box.createRigidArea(new Dimension(0, 50)));

        JButton quit = new JButton("Done");
        quit.setDefaultCapable(true);
        parent.getRootPane().setDefaultButton(quit);
        quit.setActionCommand("quit");
        quit.addActionListener(
          new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
              if ("quit".equals(e.getActionCommand())){
                if(runFlag){
                  Thread t = new Thread(){
                    @Override
                    public void run(){
                      parent.setVisible(false);
                      runRVPredict();
                    }
                  };
                  t.start();
                }
                else parent.exit();
              }
            }
          });
        quit.setEnabled(true);
        panel.add(quit);

        add(panel);
//        add(IzPanelLayout.createParagraphGap());

        // At end of layouting we should call the completeLayout method also they do nothing.
        getLayoutHelper().completeLayout();
    }

    private void runRVPredict(){
      try{
        String cmd = path + File.separator
                   + "bin" + File.separator + "rvpredict." + ((isWindows())? "bat" : "sh");
        Process p = Runtime.getRuntime().exec(cmd);
        parent.exit();
      } catch (Exception e){
        String stackTrace = "";
        for(StackTraceElement ste : e.getStackTrace()){
          stackTrace += e.toString() + "\n";
        }
        JOptionPane.showMessageDialog(null, "RV Predict failed to run: \n" + stackTrace); 
      }
    }

    private static boolean isWindows(){
      String os = System.getProperty("os.name").toLowerCase();
      return os.indexOf("win") >= 0;
    }
   

    /**
     * Indicates wether the panel has been validated or not.
     *
     */
    public boolean isValidated()
    {
        return true;
    }
}

