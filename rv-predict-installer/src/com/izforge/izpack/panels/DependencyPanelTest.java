/* Copyright 2014 Runtime Verification Inc.
 */

package com.izforge.izpack.panels;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/** Dependency test specified in install definition
 *
 * @author Philip Daian
 */
public class DependencyPanelTest {

    public String inputCommand;
    private String outputContains;

    /**
     * Construct a new dependency test (in which a system command is run and the output checked)
     *
     * @param inputCommand Command to run and check for output/exceptions
     * @param outputContains String to check for in command output, or null if no check required
     * @return Absolute executable path if found or null if none exists
     */
    public DependencyPanelTest(String inputCommand, String outputContains) {
        this.inputCommand = inputCommand;
        if (outputContains != null)
            this.outputContains = outputContains;
        else
            this.outputContains = "";
    }

    /**
     * Check if test passes, running command and checking output
     *
     * @return true if output contains appropriate message and no exception generated, false otherwise
     */
    public boolean passes() {
        String output = "";
        try {
            // https://stackoverflow.com/questions/792024/how-to-execute-system-commands-linux-bsd-using-java
            Runtime r = Runtime.getRuntime();
            Process p = r.exec(inputCommand);
            p.waitFor();
            BufferedReader b = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            while ((line = b.readLine()) != null) {
                output += line;
            }
            b.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return output.contains(outputContains);
    }

}
