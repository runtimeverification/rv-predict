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
package rvpredict.config;

import com.beust.jcommander.*;
import rvpredict.engine.main.Main;
import rvpredict.util.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


/**
 * Command line options class for rv-predict
 * Used by JCommander to parse the main program parameters.
 */
public class Configuration {

    public static final String LOGGING_PHASE_COMPLETED = "Logging phase completed.";
    public static final String TRACE_LOGGED_IN = "\tTrace logged in: ";
    public static final String INSTRUMENTED_EXECUTION_TO_RECORD_THE_TRACE = "Instrumented execution to record the trace";
    private JCommander jCommander;
    public static int stackSize = 1024;

    // Copyright (c) 2013-2014 K Team. All Rights Reserved.
    public enum OS {
        OSX(true, "osx"), UNIX(true, "linux"), UNKNOWN(false, null), WIN(false, "cygwin");

        private OS(boolean isPosix, String libDir) {
            this.isPosix = isPosix;
            String arch = System.getProperty("os.arch");
            this.libDir = Main.getBasePath() + File.separator + "lib" + File.separator + "native"
                    + File.separator + libDir + File.separator +
                    (arch.toLowerCase().contains("64") ? "64" : "32");
        }

        public final boolean isPosix;
        public final String libDir;

        public static OS current() {
            String osString = System.getProperty("os.name").toLowerCase();
            if (osString.contains("nix") || osString.contains("nux"))
                return OS.UNIX;
            else if (osString.contains("win"))
                return OS.WIN;
            else if (osString.contains("mac"))
                return OS.OSX;
            else
                return OS.UNKNOWN;
        }

        public File getNativeExecutable(String executable) {
            if (this == UNKNOWN) {
                System.err.println(
                        "Unknown OS type. " + System.getProperty("os.name") + " not recognized. " +
                                "Please contact K developers with details of your OS.");
                System.exit(1);
            }
            if (this == WIN) {
                executable = executable + ".exe";
            }
            File f = new File(libDir, executable);
            if (isPosix) {
                f.setExecutable(true, false);
            }
            return f;
        }
    }

    public static final String PROGRAM_NAME = "rv-predict";
    public static final String YES = "yes";
    public static final String NO = "no";
    @Parameter(description="<java_command_line>")
    public List<String> command_line;

    public final static String opt_only_log = "--log";
    @Parameter(names = opt_only_log, description = "Record execution in given directory (no prediction)", descriptionKey = "1000")
    public String log_dir = null;
    public boolean log = true;

    final static String opt_log_output = "--output";
    @Parameter(names = opt_log_output, description = "Output of the logged execution [yes|no|<file>]", hidden = true, descriptionKey = "1010")
    public String log_output = YES;

 	final static String opt_optlog = "--with-profile";
    @Parameter(names = opt_optlog, description = "Use profiling to optimize logging size", hidden = true, descriptionKey = "1020")
    public boolean optlog;

    public final static String opt_include = "--include";
    @Parameter(names = opt_include, validateWith = PackageValidator.class,
            description = "Comma separated list of packages to include", hidden = true, descriptionKey = "1025")
    public static String additionalIncludes;

    public final static String opt_exclude = "--exclude";
    @Parameter(names = opt_exclude, validateWith = PackageValidator.class,
            description = "Comma separated list of packages to exclude", hidden = true, descriptionKey = "1030")
    public static String additionalExcludes;

    public final static String opt_sharing_only = "--detectSharingOnly";
    @Parameter(names = opt_sharing_only, description = "Run agent only to detect shared variables", hidden = true, descriptionKey = "1040")
    public boolean agentOnlySharing;

    public final static String opt_only_predict = "--predict";
    @Parameter(names = opt_only_predict, description = "Run prediction on logs from given directory", descriptionKey = "2000")
    public String predict_dir = null;
    public boolean predict = true;

//	final static String opt_rmm_pso = "--pso";//for testing only
//    @Parameter(names = opt_rmm_pso, description = "PSO memory model", hidden = true)
    public boolean rmm_pso;

	final static String opt_max_len = "--maxlen";
    final static String default_max_len= "1000";
    @Parameter(names=opt_max_len, description = "Window size", hidden = true, descriptionKey = "2010")
    public long window_size = 1000;

//	final static String opt_no_schedule = "--noschedule";
//    @Parameter(names=opt_no_schedule, description = "not report schedule", hidden = true)
    //ok, let's make noschedule by default
    public boolean noschedule = true;

	final static String opt_no_branch = "--nobranch";
    @Parameter(names=opt_no_branch, description = "Use no branch model", hidden = true, descriptionKey = "2020")
    public boolean nobranch;

	final static String opt_no_volatile = "--novolatile";
    @Parameter(names=opt_no_volatile, description = "Exclude volatile variables", hidden = true, descriptionKey = "2030")
    public boolean novolatile;

	final static String opt_allrace = "--allrace";
    @Parameter(names=opt_allrace, description = "Check all races", hidden = true, descriptionKey = "2040")
    public boolean allrace;

//	final static String opt_all_consistent = "--allconsistent";
//    @Parameter(names = opt_all_consistent, description = "require all read-write consistent", hidden = true)
    public boolean allconsistent;

//	final static String opt_constraint_outdir = "--outdir";
//    @Parameter(names = opt_constraint_outdir, description = "constraint file directory", hidden = true)
    public String constraint_outdir;

    public String tableName = "main";

    final static String opt_smt_solver = "--solver";
    @Parameter(names = opt_smt_solver, description = "Solver command to use (SMT-LIB v1.2)", hidden = true, descriptionKey = "2050")
    public String smt_solver = "\"" + OS.current().getNativeExecutable("z3") + "\"" + " -smt";

	final static String opt_solver_timeout = "--solver_timeout";
    @Parameter(names = opt_solver_timeout, description = "Solver timeout in seconds", hidden = true, descriptionKey = "2060")
    public long solver_timeout = 60;

	final static String opt_solver_memory = "--solver_memory";
//    @Parameter(names = opt_solver_memory, description = "solver memory size in MB", hidden = true)
    public long solver_memory = 8000;

	final static String opt_timeout = "--timeout";
    @Parameter(names = opt_timeout, description = "Rv-predict timeout in seconds", hidden = true, descriptionKey = "2070")
    public long timeout = 3600;

//    final static String opt_smtlib1 = "--smtlib1";
//    @Parameter(names = opt_smtlib1, description = "use constraint format SMT-LIB v1.2", hidden = true)
    public boolean smtlib1 = true;

	final static String opt_optrace = "--optrace";
//    @Parameter(names = opt_optrace, description = "optimize race detection", hidden = true)
    //by default optrace is true
    public boolean optrace = true;


	public final static String opt_outdir = "--dir";
    @Parameter(names = opt_outdir, description = "Output directory", hidden = true, descriptionKey = "8000")
    public String outdir = null;

	final static String short_opt_verbose = "-v";
    final static String opt_verbose = "--verbose";
    @Parameter(names = {short_opt_verbose, opt_verbose}, description = "Generate more verbose output", descriptionKey = "9000")
    public boolean verbose;

	final static String short_opt_help = "-h";
    final static String opt_help = "--help";
    @Parameter(names = {short_opt_help, opt_help}, description = "Print help info", help = true, descriptionKey = "9900")
    public boolean help;

    public final static String opt_java = "--";
    public Logger logger;
//    @Parameter(names = opt_java, description = "optional separator for java arguments")
//    public boolean javaSeparator;


    public void parseArguments(String[] args, boolean checkJava) {
        String pathSeparator = System.getProperty("path.separator");
        String fileSeparator = System.getProperty("file.separator");
        jCommander = new JCommander(this);
        jCommander.setProgramName(PROGRAM_NAME);

        // Collect all parameter names.  It would be nice if JCommander provided this directly.
        Set<String> options = new HashSet<String>();
        for (ParameterDescription parameterDescription : jCommander.getParameters()) {
            for (String name : parameterDescription.getParameter().names()) {
                options.add(name);
            }
        }

        // Detecting a candidate for program options start
        int max = Arrays.asList(args).indexOf(Configuration.opt_java);
        String[] rvArgs;
        if (max != -1) { // -- was used. Using it as a separator for java command line
            rvArgs = Arrays.copyOf(args, max);
            max++;
        } else { // -- was not specified.  Look for the first unknown option
            for (max = 0; max < args.length; max++) {
                if (args[max].startsWith("-") && !options.contains(args[max]))
                    break; // the index of the first unknown command
            }
            rvArgs = Arrays.copyOf(args, max);
        }

        // get all rv-predict arguments and (potentially) the first unnamed program arguments
        try {
            jCommander.parse(rvArgs);
        } catch (ParameterException e) {
            System.err.println("Error: Cannot parse command line arguments.");
            System.err.println(e.getMessage());
            System.exit(1);
        }

        if (log_dir != null) {
            if (predict_dir != null) {
                exclusiveOptionsFailure(opt_only_log, opt_only_predict);
            } else {
                if (outdir != null) {
                    exclusiveOptionsFailure(opt_only_log, opt_outdir);
                }
                outdir = Paths.get(log_dir).toAbsolutePath().toString();
                predict = false;
            }
        } else  {
            if (predict_dir != null) {
                if (outdir != null) {
                    exclusiveOptionsFailure(opt_only_predict, opt_outdir);
                }
                outdir = Paths.get(predict_dir).toAbsolutePath().toString();
                log = false;
            } else if (outdir != null) {
                outdir = Paths.get(outdir).toAbsolutePath().toString();
            } else {
                try {
                    outdir = Files.createTempDirectory(
                            Paths.get(System.getProperty("java.io.tmpdir")), "rv-predict").toString();
                } catch (IOException e) {
                    System.err.println("Error while attempting to create log dir.");
                    System.err.println(e.getMessage());
                    System.exit(1);
                }
            }
        }

        constraint_outdir = outdir + fileSeparator + "smt";

        if (command_line != null) { // if there are unnamed options they should all be at the end
            int i = rvArgs.length - 1;
            for (String command : command_line) {
                if (!command.equals(rvArgs[i--])) {
                    System.err.println("Error: Unexpected argument " + command + " among rv-predict options.");
                    System.err.println("The options terminator '" + opt_java + "' can be used to separate the java command.");
                    System.exit(1);
                }
            }
        }

        if (help) {
            usage();
            System.exit(0);
        }

        List<String> argList = Arrays.asList(Arrays.copyOfRange(args, max, args.length));
        if (command_line == null) { // otherwise the java command has already started
            command_line = new ArrayList<String>(argList);
            if (command_line.isEmpty() && log && checkJava) {
                System.err.println("Error: Java command line is empty.");
                usage();
                System.exit(1);
            }
        } else {
            command_line.addAll(argList);
        }
        if (tableName == null) {
            tableName = "main";
        }
        logger = new Logger(this);
    }

    public void exclusiveOptionsFailure(String opt1, String opt2) {
        System.err.println("Error: Options " + opt1 + " and " + opt2 + " are mutually exclusive.");
        System.exit(1);
    }

    public void usage() {
/*
-- can be used as a terminator for the rv-predict specific options.
The remaining arguments are what one would pass to the java executable to
execute the class/jar
The -- option is only required in the less frequent case when some of
the java or program options used have the same name as some of the
rv-predict options (including --).

Moreover, in the unlikely case when the program takes as options -cp or -jar
and is run as a class (i.e., not using -jar) then the java -cp option must
be used explicitly for disambiguation.
*/

        // computing names maximum length
        int max_option_length = 0;
        for (ParameterDescription parameterDescription : jCommander.getParameters()) {
            if (parameterDescription.getNames().length() > max_option_length) {
                max_option_length = parameterDescription.getNames().length();
            }
        }

        // Computing usage
        max_option_length++;
        String usageHeader = "Usage: " + PROGRAM_NAME + " [rv_predict_options] [--] [java_options] "
                + jCommander.getMainParameterDescription() + "\n";
        String usage = usageHeader
                + "  Options:";
        String shortUsage = usageHeader
                + "  Common options (use -h -v for a complete list):";

        Map<String, String> usageMap = new TreeMap<String, String>();
        Map<String, String> shortUsageMap = new TreeMap<String, String>();
        int spacesBeforeCnt;
        int spacesAfterCnt;
        String description;
        for (ParameterDescription parameterDescription : jCommander.getParameters()) {
            Parameter parameter = parameterDescription.getParameter().getParameter();
            String descriptionKey = parameter.descriptionKey();
            description = "\n";
            spacesBeforeCnt = 2;
            spacesAfterCnt = max_option_length - parameterDescription.getNames().length() + 2;
            if (!descriptionKey.endsWith("00")) {
                spacesBeforeCnt += 2;
                spacesAfterCnt -= 2;
                description = "";
            }

            String aDefault = getDefault(parameterDescription);
            description += Util.spaces(spacesBeforeCnt) + parameterDescription.getNames()
                    + Util.spaces(spacesAfterCnt)
                    + parameterDescription.getDescription()
                    + (aDefault.isEmpty() ? "" : "\n" + Util.spaces(4) + Util.spaces(max_option_length) + aDefault);
            usageMap.put(descriptionKey, description);
            if (!parameter.hidden()) {
                shortUsageMap.put(descriptionKey, description);
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

    private String getDefault(ParameterDescription parameterDescription) {
        Object aDefault = parameterDescription.getDefault();
        if (aDefault == null || aDefault.equals(Boolean.FALSE)) return "";
        return "Default: " + aDefault;
    }


}
