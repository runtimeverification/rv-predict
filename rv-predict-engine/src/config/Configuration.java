/*******************************************************************************
 * Copyright (c) 2013 University of Illinois
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package config;

import com.beust.jcommander.*;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;


/**
 * Command line options class for rv-predict
 * Used by JCommander to parse the main program parameters.
 */
public class Configuration {

    public static final String PROGRAM_NAME = "rv-predict";
    @Parameter(description="<command_line>")
    public List<String> command_line;

//	final static String opt_rmm_pso = "--pso";//for testing only
//    @Parameter(names = opt_rmm_pso, description = "PSO memory model", hidden = true)
    public boolean rmm_pso;

	final static String opt_max_len = "--maxlen";
    final static String default_max_len= "1000";
    @Parameter(names=opt_max_len, description = "window size", hidden = true)
    public long window_size = 1000;

//	final static String opt_no_schedule = "--noschedule";
//    @Parameter(names=opt_no_schedule, description = "not report schedule", hidden = true)
    //ok, let's make noschedule by default
    public boolean noschedule = true;

	final static String opt_no_branch = "--nobranch";
    @Parameter(names=opt_no_branch, description = "use no branch model", hidden = true)
    public boolean nobranch;

	final static String opt_no_volatile = "--novolatile";
    @Parameter(names=opt_no_volatile, description = "exclude volatile variables", hidden = true)
    public boolean novolatile;

	final static String opt_allrace = "--allrace";
    @Parameter(names=opt_allrace, description = "check all races", hidden = true)
    public boolean allrace;

//	final static String opt_all_consistent = "--allconsistent";
//    @Parameter(names = opt_all_consistent, description = "require all read-write consistent", hidden = true)
    public boolean allconsistent;

//	final static String opt_constraint_outdir = "--outdir";
//    @Parameter(names = opt_constraint_outdir, description = "constraint file directory", hidden = true)
    public String constraint_outdir;

 	public final static String opt_outdir = "--dir";
    @Parameter(names = opt_outdir, description = "output directory")
    public String outdir = null;

    public final static String opt_table_name = "--table";
    @Parameter(names = opt_table_name, description = "Name of the table (Default: jar main class)", hidden = true)
    public String tableName = null;

	final static String opt_solver_timeout = "--solver_timeout";
    @Parameter(names = opt_solver_timeout, description = "solver timeout in seconds", hidden = true)
    public long solver_timeout = 60;

	final static String opt_solver_memory = "--solver_memory";
    @Parameter(names = opt_solver_memory, description = "solver memory size in MB", hidden = true)
    public long solver_memory = 8000;

	final static String opt_timeout = "--timeout";
    @Parameter(names = opt_timeout, description = "rv-predict timeout in seconds")
    public long timeout = 3600;

	final static String opt_smtlib1 = "--smtlib1";
    @Parameter(names = opt_smtlib1, description = "use constraint format smtlib v1.2", hidden = true)
    public boolean smtlib1;

	final static String opt_optrace = "--optrace";
    @Parameter(names = opt_optrace, description = "optimize race detection", hidden = true)
    //by default optrace is true
    public boolean optrace = true;

 	final static String opt_optlog = "--optlog";
    @Parameter(names = opt_optlog, description = "optimize logging size", hidden = true)
    public boolean optlog;

    public final static String opt_only_log = "--agent";
    @Parameter(names = opt_only_log, description = "Run only the logging stage")
    public boolean agent;

    public final static String opt_only_predict = "--predict";
    @Parameter(names = opt_only_predict, description = "Run only the prediction stage")
    public boolean predict;


	final static String short_opt_verbose = "-v";
    final static String opt_verbose = "--verbose";
    @Parameter(names = {short_opt_verbose, opt_verbose}, description = "generate more verbose output")
    public boolean verbose;

	final static String short_opt_help = "-h";
    final static String opt_help = "--help";
    @Parameter(names = {short_opt_help, opt_help}, description = "print help info", help = true)
    public boolean help;

    public final static String opt_java = "--java";
    @Parameter(names = opt_java, description = "optional separator for java arguments")
    public boolean javaSeparator;


    public final static String opt_sharing_only = "--detectSharingOnly";
    @Parameter(names = opt_sharing_only, description = "Run agent only to detect shared variables.", hidden = true)
    public boolean agentOnlySharing;

    public void parseArguments(String[] args) {
        String pathSeparator = System.getProperty("path.separator");
        String fileSeparator = System.getProperty("file.separator");
        JCommander jc = new JCommander(this);
        jc.setProgramName(PROGRAM_NAME);

        // Collect all parameter names.  It would be nice if JCommander provided this directly.
        Set<String> options = new HashSet<String>();
        for (ParameterDescription parameterDescription : jc.getParameters()) {
            for (String name : parameterDescription.getParameter().names()) {
                options.add(name);
            }
        }

        // Detecting a candidate for program options start
        int max = Arrays.asList(args).indexOf(Configuration.opt_java);
        if (max != -1) { // --java was used. Using it as a separator for java command line
            max++;
        } else { // --java was not specified.  Look for the first unknown option
            for (max = 0; max < args.length; max++) {
                if (args[max].startsWith("-") && !options.contains(args[max]))
                    break; // the index of the first unknown command
            }
        }

        // get all rv-predict arguments and (potentially) the first unnamed program arguments
        String[] rvArgs = Arrays.copyOf(args, max);
        try {
            jc.parse(rvArgs);
        } catch (ParameterException e) {
            System.err.println("Error: Cannot parse command line arguments.");
            System.err.println(e.getMessage());
            System.exit(1);
        }


        try {
            if (outdir == null) {
                outdir = Files.createTempDirectory(
                        Paths.get(System.getProperty("java.io.tmpdir")), "rv-predict").toString();
            }
        } catch (IOException e) {
            System.err.println("Error while attempting to create log dir.");
            System.err.println(e.getMessage());
            System.exit(1);
        }

        constraint_outdir = outdir + fileSeparator + "z3";

        if (command_line != null) { // if there are unnamed options they should all be at the end
            int i = rvArgs.length - 1;
            for (String command : command_line) {
                if (!command.equals(rvArgs[i--])) {
                    System.err.println("Error: Unexpected argument " + command + " among rv-predict options.");
                    System.err.println("The " + opt_java + " option can be used to separate the java command.");
                    System.exit(1);
                }
            }
        }

        if (help) {
            usage(jc);
            System.exit(0);
        }

        List<String> argList = Arrays.asList(Arrays.copyOfRange(args, rvArgs.length, args.length));
        int idxCp = -1;
        if (command_line == null) { // otherwise the program has already started
            command_line = new ArrayList<String>(argList);
            if (command_line.isEmpty()) {
                System.err.println("Error: Java command line is empty.");
                usage(jc);
                System.exit(1);
            }
        } else {
            command_line.addAll(argList);
        }
        if (tableName == null) {
            tableName = "main";
        }
    }

    public void usage(JCommander jc) {
/*
--java can be used as a separator for the java command line
the remaining arguments are what one would pass to the java executable to
execute the class/jar
The --java option is only required in the less frequent case when some of
the java or program options used have the same name as some of the
rv-predict options (including --java).

Moreover, in the unlikely case when the program takes as options -cp or -jar
and is run as a class (i.e., not using -jar) then the java -cp option must
be used explicitly for disambiguation.
*/

        // computing names maximum length
        int max_option_length = 0;
        for (ParameterDescription parameterDescription : jc.getParameters()) {
            if (parameterDescription.getNames().length() > max_option_length) {
                max_option_length = parameterDescription.getNames().length();
            }
        }

        // Computing usage
        max_option_length++;
        String usageHeader = "Usage: " + PROGRAM_NAME + " [rv_predict_options] [java_options] "
                + jc.getMainParameterDescription() + "\n";
        String usage = usageHeader
                + "  Options:" + "\n";
        String shortUsage = usageHeader
                + "  Common options (use -h -v for a complete list):" + "\n";

        Map<String, String> usageMap = new TreeMap<String, String>();
        Map<String, String> shortUsageMap = new TreeMap<String, String>();
        for (ParameterDescription parameterDescription : jc.getParameters()) {
                String description = spaces(4) + parameterDescription.getNames()
                        + spaces(max_option_length - parameterDescription.getNames().length())
                        + parameterDescription.getDescription()
                        + " [" + parameterDescription.getDefault() + "]";
                usageMap.put(parameterDescription.getLongestName(), description);
            if (!parameterDescription.getParameter().hidden()) {
                shortUsageMap.put(parameterDescription.getLongestName(), description);
            }

        }

        if (verbose) {
            System.out.println(usage);
            for (String usageCase : usageMap.values()) System.out.println(usageCase);
        } else {
            System.out.println(shortUsage);
            for (String usageCase : shortUsageMap.values()) System.out.println(usageCase);
        }
    }

    private static String spaces(int i) {
        char[] spaces = new char[i];
        Arrays.fill(spaces, ' ');
        return new String(spaces);
    }


}
