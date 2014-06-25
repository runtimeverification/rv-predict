/* Copyright 2014 Runtime Verification Inc.
 */


package com.izforge.izpack.panels;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Properties;

import com.izforge.izpack.Panel;
import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.PanelConsole;
import com.izforge.izpack.installer.PanelConsoleHelper;

/** Console (command-line) installer helper for DependencyPanel
 *
 * @author Philip Daian
 */
public class DependencyPanelConsoleHelper extends PanelConsoleHelper implements PanelConsole {

    @Override
    public boolean runGeneratePropertiesFile(AutomatedInstallData installData,PrintWriter printWriter) {
        return true;
    }

    @Override
    public boolean runConsoleFromPropertiesFile(AutomatedInstallData installData, Properties p) {
        return true;
    }

    @Override
    public boolean runConsole(AutomatedInstallData idata) {
        final String dependencyId = DependencyPanelUtils.getId(idata);
        final String dependencySite = DependencyPanelUtils.getDependencySite(idata, dependencyId);
        final String dependencyText = DependencyPanelUtils.getDependencyText(dependencyId);
        final ArrayList<String> dependencyList = DependencyPanelUtils.getDependencies(idata, dependencyId);
        final ArrayList<DependencyPanelTest> dependencyTests = DependencyPanelUtils.getDependencyTests(idata, dependencyId);

        if (DependencyPanelUtils.isDependencySatisfied(dependencyList, dependencyTests)) {
            return true;
        }

        System.out.println(dependencyText);

        System.out.println("You must install this dependency from " + dependencySite +
            " and add it to your system path or the product may not work correctly!\n");

        int i = askEndOfConsolePanel();
        if (i == 1) {
            return true;
        }
        else if (i == 2) {
            return false;
        }
        else {
            return runConsole(idata);
        }
    }

}
