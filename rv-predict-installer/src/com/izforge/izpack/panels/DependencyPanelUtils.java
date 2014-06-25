/* Copyright 2014 Runtime Verification Inc.
 */

package com.izforge.izpack.panels;

import java.io.File;
import java.util.ArrayList;

import com.izforge.izpack.installer.AutomatedInstallData;
import com.izforge.izpack.installer.IzPanel;
import com.izforge.izpack.installer.ResourceManager;
import com.izforge.izpack.Panel;

/** General utilities and methods used by DependencyPanel
 *
 * @author Philip Daian
 */
public class DependencyPanelUtils {

    /**
     * Check for executable on system PATH (env variable PATH or path)
     *
     * @param executableName Name of executable being searched for
     * @return Absolute executable path if found or null if none exists
     */
    protected static File findExecutableOnPath(String executableName) {
        String systemPath = System.getenv("PATH");
        if (systemPath == null) {
            systemPath = System.getenv("path");
            if (systemPath == null) {
                return null;
            }
        }
        String[] pathDirs = systemPath.split(File.pathSeparator);
        File fullyQualifiedExecutable = null;
        for (String pathDir : pathDirs) {
            File file = new File(pathDir, executableName);
            if (file.isFile()) {
                fullyQualifiedExecutable = file;
                break;
            }
        }
        return fullyQualifiedExecutable;
    }

    /**
     * Get ID of current panel in specified installer
     *
     * @param idata Current installer
     * @return Current panel ID
     */
    protected static String getId(AutomatedInstallData idata) {
        Panel panel = (Panel) idata.panelsOrder.get(idata.curPanelNumber);
        final String id = panel.getPanelid();
        if (id == null) {
            throw new RuntimeException("Error! All DependencyPanels must have ID specified.");
        }
        return id;
    }

    /**
     * Get list of dependencies for current panel
     *
     * @param idata Current installer, id The ID of the current dependency
     * @return Current panel dependencies, as ArrayList
     */
    protected static ArrayList<String> getDependencies(AutomatedInstallData idata, String id) {
        int i = 1;
        ArrayList<String> dependencyList = new ArrayList<String>();
        String dependencyExecutable = idata.getVariable("DependencyPanel." + id + ".exe." + i);
        while (dependencyExecutable != null) {
            dependencyList.add(dependencyExecutable);
            i++;
            dependencyExecutable = idata.getVariable("DependencyPanel." + id + ".exe." + i);
        }
        return dependencyList;
    }

    /**
     * Get list of dependency tests for current panel
     *
     * @param idata Current installer, id The ID of the current dependency
     * @return Current dependency tests in wrapper class DependencyPanelTest
     */
    protected static ArrayList<DependencyPanelTest> getDependencyTests(AutomatedInstallData idata, String id) {
        int i = 1;
        ArrayList<DependencyPanelTest> testList = new ArrayList<DependencyPanelTest>();
        String testCommand = idata.getVariable("DependencyPanel." + id + ".test." + i + ".command");
        while (testCommand != null) {
            String testOutput = idata.getVariable("DependencyPanel." + id + ".test." + i + ".output_contains");
            testList.add(new DependencyPanelTest(testCommand, testOutput));
            i++;
            testCommand = idata.getVariable("DependencyPanel." + id + ".test." + i + ".command");
        }
        return testList;
    }

    /**
     * Get site of current panel's dependency
     *
     * @param idata Current installer data
     * @return Current panel's dependency install site
     */
    protected static String getDependencySite(AutomatedInstallData idata, String id) {
        final String dependencySite = idata.getVariable("DependencyPanel." + id + ".site");
        if (dependencySite == null) {
            throw new RuntimeException("Variable DependencyPanel." + id + ".site is undefined.");
        }
        return dependencySite;
    }

    /**
     * Get description HTML of current panel's dependency
     *
     * @param idata Current installer data
     * @return Current panel's dependency description HTML
     */
    protected static String getDependencyHTML(String id) {
        final String dependencyHTML;
        try {
            dependencyHTML = ResourceManager.getInstance().getTextResource("DependencyPanel." + id + ".html");
        }
        catch (Exception ex) {
            throw new RuntimeException("Resource DependencyPanel." + id + ".html is undefined.");
        }
        return dependencyHTML;
    }

    /**
     * Get description text of current panel's dependency
     *
     * @param idata Current installer data
     * @return Current panel's dependency description text
     */
    protected static String getDependencyText(String id) {
        return removeHTML(getDependencyHTML(id));
    }

    /**
     * Check if any of a list of dependencies is currently installed on system
     *
     * @param dependencyList Dependency list to check
     * @return true if any of dependencies exist in system PATH
     */
    protected static boolean isDependencySatisfied(ArrayList<String> dependencyList, ArrayList<DependencyPanelTest> testList) {
        boolean isOnPath = true;
        for (String executable : dependencyList) {
            if (findExecutableOnPath(executable) != null) {
                isOnPath = true;
                break;
            }
            isOnPath = false;
        }
        if (!isOnPath)
            return false;
        for (DependencyPanelTest test : testList) {
            if (!test.passes()) {
                return false;
            }
        }
        // All tests pass and all required executables are on PATH
        return true;
    }

    /**
     * Replace HTML in a String with native formatting
     * From  https://github.com/izpack/izpack/blob/4.3/src/lib/com/izforge/izpack/panels/HTMLLicencePanelConsoleHelper.java
     *
     * @param source the HTML-formatted String to remove HTML from
     * @return source with all HTML formatting removed
     */
    protected static String removeHTML(String source) {
        String result = "";
        try {
            // chose to keep newline (\n) instead of carriage return (\r) for line breaks.

             // Replace line breaks with space
            result = source.replaceAll("\r", " ");
            // Remove step-formatting
            result = result.replaceAll("\t", "");
            // Remove repeating spaces because browsers ignore them
            result = result.replaceAll("( )+", " ");

            result = result.replaceAll("<( )*head([^>])*>","<head>");
            result = result.replaceAll("(<( )*(/)( )*head( )*>)","</head>");
            result = result.replaceAll("(<head>).*(</head>)", "");
            result = result.replaceAll("<( )*script([^>])*>","<script>");
            result = result.replaceAll("(<( )*(/)( )*script( )*>)","</script>");
            result = result.replaceAll("(<script>).*(</script>)","");

           // remove all styles (prepare first by clearing attributes)
            result = result.replaceAll("<( )*style([^>])*>","<style>");
            result = result.replaceAll("(<( )*(/)( )*style( )*>)","</style>");
            result = result.replaceAll("(<style>).*(</style>)","");

            result = result.replaceAll("(<( )*(/)( )*sup( )*>)","</sup>");
            result = result.replaceAll("<( )*sup([^>])*>","<sup>");
            result = result.replaceAll("(<sup>).*(</sup>)", "");

           // insert tabs in spaces of <td> tags
            result = result.replaceAll("<( )*td([^>])*>","\t");

           // insert line breaks in places of <BR> and <LI> tags
            result = result.replaceAll("<( )*br( )*>","\r");
            result = result.replaceAll("<( )*li( )*>","\r");

            // insert line paragraphs (double line breaks) in place
            // if <P>, <DIV> and <TR> tags
            result = result.replaceAll("<( )*div([^>])*>","\r\r");
            result = result.replaceAll("<( )*tr([^>])*>","\r\r");

            result = result.replaceAll("(<) h (\\w+) >","\r");
            result = result.replaceAll("(\\b) (</) h (\\w+) (>) (\\b)","");
            result = result.replaceAll("<( )*p([^>])*>","\r\r");

            // Remove remaining tags like <a>, links, images,
            // comments etc - anything that's enclosed inside < >
            result = result.replaceAll("<[^>]*>","");

            result = result.replaceAll("&bull;"," * ");
            result = result.replaceAll("&lsaquo;","<");
            result = result.replaceAll("&rsaquo;",">");
            result = result.replaceAll("&trade;","(tm)");
            result = result.replaceAll("&frasl;","/");
            result = result.replaceAll("&lt;","<");
            result = result.replaceAll("&gt;",">");

            result = result.replaceAll("&copy;","(c)");
            result = result.replaceAll("&reg;","(r)");
            result = result.replaceAll("&(.{2,6});","");

            // Remove extra line breaks and tabs:
            // replace over 2 breaks with 2 and over 4 tabs with 4.
            // Prepare first to remove any whitespaces in between
            // the escaped characters and remove redundant tabs in between line breaks
            result = result.replaceAll("(\r)( )+(\r)","\r\r");
            result = result.replaceAll("(\t)( )+(\t)","\t\t");
            result = result.replaceAll("(\t)( )+(\r)","\t\r");
            result = result.replaceAll("(\r)( )+(\t)","\r\t");
            result = result.replaceAll("(\r)(\t)+(\\r)","\r\r");
            result = result.replaceAll("(\r)(\t)+","\r\t");
         } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

}
