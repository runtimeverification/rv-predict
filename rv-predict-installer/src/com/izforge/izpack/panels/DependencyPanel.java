/* Copyright 2014 Runtime Verification Inc.
 *
 * IzPack - Copyright 2001-2008 Julien Ponge, All Rights Reserved.
 *
 * http://izpack.org/
 * http://izpack.codehaus.org/
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

import com.izforge.izpack.gui.IzPanelLayout;
import com.izforge.izpack.installer.InstallData;
import com.izforge.izpack.installer.InstallerFrame;
import com.izforge.izpack.installer.IzPanel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.ArrayList;
import javax.swing.*;

/** Installer panel that checks whether one of several possible
 *  binary dependencies are installed (present on system PATH).
 *  If not, the user is prompted to download the binary from
 *  a website.  Customizable HTML is displayed.  All parameters
 *  are specified as variables and resources in the IzPack 
 *  install configuration (see RV-Predict installer for examples).
 *
 * @author Philip Daian
 */
public class DependencyPanel extends IzPanel implements ActionListener {

    private static final long serialVersionUID = 3257848774955905587L;
    private JCheckBox checkBox;
    private ArrayList<String> dependencyList;
    private ArrayList<DependencyPanelTest> dependencyTests;
    private boolean initialized = false;
    private JPanel panel;

    /**
     * The constructor
     *
     * @param parent The parent
     * @param idata  The installation data
     */
    public DependencyPanel(InstallerFrame parent, InstallData idata) {
        this(parent, idata, new IzPanelLayout());
    }

    /**
     * Creates a new DependencyPanel object with the given layout manager. Valid layout manager are the
     * IzPanelLayout and the GridBagLayout. New panels should be use the IzPanelLaout. If lm is
     * null, no layout manager will be created or initialized.
     *
     * @param parent The parent IzPack installer frame
     * @param idata  The installer internal data
     * @param layout layout manager to be used with this IzPanel
     */
    public DependencyPanel(InstallerFrame parent, InstallData idata, LayoutManager2 layout) {
        super(parent, idata, layout);
        JPanel panel = new JPanel();
        add(panel);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        this.panel = panel;
    }

    /**
     * Open system's browser to given URL using native methods
     *
     * @param url URL to navigate to
     */
    private void openBrowser(String url) {
        if (Desktop.isDesktopSupported()){
            try {
                Desktop desktop = Desktop.getDesktop();
                desktop.browse(new URI(url));
            } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Failed to launch the link, " +
                "your computer is likely misconfigured.",
                "Cannot Launch Link",JOptionPane.WARNING_MESSAGE);
            }
        }
        else {
            JOptionPane.showMessageDialog(null,
                "Java is not able to launch links on your computer.",
                "Cannot Launch Link",JOptionPane.WARNING_MESSAGE);
        }
    }

    @Override
    public void panelActivate() {
        if (isHidden()) {
            parent.skipPanel();
        }
        if ((checkBox == null) || !(checkBox.isSelected())) {
            parent.lockNextButton();
        }
        if (initialized) {
            return;
        }
        initialized = true;
        final String dependencyId = DependencyPanelUtils.getId(idata);
        final String dependencySite = DependencyPanelUtils.getDependencySite(idata, dependencyId);
        final String dependencyHTML = DependencyPanelUtils.getDependencyHTML(dependencyId);
        dependencyList = DependencyPanelUtils.getDependencies(idata, dependencyId);
        dependencyTests = DependencyPanelUtils.getDependencyTests(idata, dependencyId);

        if (DependencyPanelUtils.isDependencySatisfied(dependencyList, dependencyTests)) {
            setHidden(true);
            return;
        }

        JLabel htmlLabel = new JLabel(dependencyHTML);
        panel.add(htmlLabel, NEXT_LINE);
        panel.add(IzPanelLayout.createParagraphGap());
        panel.add(Box.createRigidArea(new Dimension(0,15)));

        panel.add(new JLabel("Click here to download the dependency:"));
        final JLabel label = new JLabel("<html><font color=\"blue\"><u>" + dependencySite + "</u></font><br><br></html>");
        panel.add(label);
        label.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if(evt.getClickCount() > 0){
                    label.setText("<html><font color=\"red\"><u>" + dependencySite + "</u></font><br><br></html>");
                    openBrowser(dependencySite);
                }
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                setCursor(Cursor.getDefaultCursor());
            }
        });

        checkBox = new JCheckBox("<html> I have installed the dependency and added it to the PATH "
            + "<br> (failure to do so may lead to errors in the installed packages). </html>", false);
        panel.add(checkBox, NEXT_LINE);
        checkBox.addActionListener(this);

        getLayoutHelper().completeLayout();
    }

    @Override
    public void panelDeactivate() {
        if (!isHidden() && dependencyList != null && checkBox.isSelected()) {
            if (!DependencyPanelUtils.isDependencySatisfied(dependencyList, dependencyTests)) {
                emitWarning("Warning!", "<html> We have detected that the dependency is still not satisfied"
                    + " in a way <br> that would make the installed program execute correctly. <br><br> Continuing anyway "
                    + "may make it impossible for the installed application to correctly execute. </html>");
            }
        }
    }

    @Override
    public boolean isValidated() {
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (checkBox.isSelected()) {
            parent.unlockNextButton();
        }
        else {
            parent.lockNextButton();
        }
    }

}
