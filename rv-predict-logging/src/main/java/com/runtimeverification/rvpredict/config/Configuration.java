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
package com.runtimeverification.rvpredict.config;

import com.beust.jcommander.*;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.runtimeverification.rvpredict.util.Constants;
import com.runtimeverification.rvpredict.util.Logger;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.tools.ant.util.JavaEnvUtils;

/**
 * Common options class for RV-Predict Used by JCommander to parse the
 * common program parameters.
 * This class contains the common options. It is specialized by {@link AgentConfiguration}
 * for agent-specific options and by {@link PredictionConfiguration} for prediction-only options.
 */
public abstract class Configuration implements Constants {

    private static final String SEPARATOR = System.getProperty("file.separator");
    public static final String JAVA_EXECUTABLE = JavaEnvUtils.getJreExecutable("java");
    public static final String RV_PREDICT_JAR = Configuration.getBasePath() + SEPARATOR + "rv-predict.jar";

    public static final String TRACE_SUFFIX = "trace.bin";

    private static final String METADATA_BIN = "metadata.bin";


    private JCommander jCommander;

    public static String getBasePath() {
        CodeSource codeSource = Configuration.class.getProtectionDomain().getCodeSource();
        String path;
        if (codeSource == null) {
            path = ClassLoader.getSystemClassLoader()
                    .getResource(Configuration.class.getName().replace('.', '/') + ".class").toString();
            path = path.substring(path.indexOf("file:"), path.indexOf('!'));
            URL url = null;
            try {
                url = new URL(path);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            path = url.getPath();
        } else {
            path = codeSource.getLocation().getPath();
        }
        path = new File(path).getAbsolutePath();
        try {
            String decodedPath = URLDecoder.decode(path, "UTF-8");
            File parent = new File(decodedPath).getParentFile();
            return parent.getAbsolutePath();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public enum OS {
        OSX(true), LINUX(true), UNKNOWN(false), WINDOWS(false);

        private OS(boolean isPosix) {
            this.isPosix = isPosix;
        }

        public final boolean isPosix;

        public static OS current() {
            String osString = System.getProperty("os.name").toLowerCase();
            if (osString.contains("nix") || osString.contains("nux"))
                return OS.LINUX;
            else if (osString.contains("win"))
                return OS.WINDOWS;
            else if (osString.contains("mac"))
                return OS.OSX;
            else
                return OS.UNKNOWN;
        }

        public String getNativeExecutable(String executable) {
            if (this == UNKNOWN) {
                System.err.println("Unknown OS type. " + System.getProperty("os.name")
                        + " not recognized. "
                        + "Please contact RV-Predict developers with details of your OS.");
                System.exit(1);
            }
            if (this == WINDOWS) {
                executable = executable + ".exe";
            }
            return executable;
        }

        public String getLibraryPathEnvVar() {
            if (this == WINDOWS) {
                return "PATH";
            } else if (this == OSX) {
                return "DYLD_LIBRARY_PATH";
            } else {
                return "LD_LIBRARY_PATH";
            }
        }
    }

    private String[] args;

    protected final static String ONLINE_PREDICTION = "ONLINE_PREDICTION";
    protected final static String OFFLINE_PREDICTION = "OFFLINE_PREDICTION";
    protected String prediction;

    private String logDir;

    // Prediction options

    final static String opt_window_size = "--window";
    @Parameter(names = opt_window_size, description = "Window size (must be >= 64)", descriptionKey = "2200")
    public int windowSize = 1000;
    private static int MIN_WINDOW_SIZE = 64;

    final static String opt_no_stacks = "--no-stacks";
    @Parameter(names = opt_no_stacks, description = "Do not record call stack events and compute stack traces in race report", hidden = true, descriptionKey = "2300")
    private boolean nostacks = false;

    public final static String opt_suppress = "--suppress";
    @Parameter(names = opt_suppress, description = "Suppress race reports on the fields that match the given (comma-separated) list of regular expressions", descriptionKey = "2400")
    private String suppress = "";

    /*
    final static String opt_smt_solver = "--solver";
    @Parameter(names = opt_smt_solver, description = "SMT solver to use. <solver> is one of [z3].", hidden = true, descriptionKey = "2500")
    public String smt_solver = "z3";
    */

    final static String opt_solver_timeout = "--solver-timeout";
    @Parameter(names = opt_solver_timeout, description = "Solver timeout in seconds", hidden = true, descriptionKey = "2600")
    public int solver_timeout = 60;


    final static String opt_debug = "--debug";
    @Parameter(names = opt_debug, description = "Output developer debugging information", hidden = true, descriptionKey = "3000")
    public static boolean debug = false;

    final static String short_opt_verbose = "-v";
    final static String opt_verbose = "--verbose";
    @Parameter(names = { short_opt_verbose, opt_verbose }, description = "Generate more verbose output", descriptionKey = "9000")
    public static boolean verbose;

    final static String opt_version = "--version";
    @Parameter(names = opt_version, description = "Print product version and exit", descriptionKey = "9100")
    public static boolean display_version;

    final static String short_opt_help = "-h";
    final static String opt_help = "--help";
    @Parameter(names = { short_opt_help, opt_help }, description = "Print help info", help = true, descriptionKey = "9900")
    public boolean help;

    private final Logger logger = new Logger();

    public final List<String> suppressList = new ArrayList<>();
    public Pattern suppressPattern;

   protected void parseArguments(String[] args) {
        this.args = args;
        jCommander = new JCommander(this);
        jCommander.setProgramName(getProgramName());

        /* parse rv-predict arguments */
        try {
            jCommander.parse(args);
        } catch (ParameterException e) {
            System.err.println("Error: Cannot parse command line arguments.");
            System.err.println(e.getMessage());
            System.exit(1);
        }

        if (help) {
            usage();
            System.exit(0);
        }
        if (display_version) {
            System.out.println("RV-Predict version "
                    + this.getClass().getPackage().getImplementationVersion());
            System.exit(0);
        }

        initSuppressPattern();

        /* set window size */
        windowSize = Math.max(windowSize, MIN_WINDOW_SIZE);

    }

    protected abstract String getProgramName();

    private void initSuppressPattern() {
        suppressList.addAll(Arrays.asList(suppress.split(",")).stream().map(s -> s.trim())
                .filter(s -> !s.isEmpty()).collect(Collectors.toList()));
        suppressPattern = Pattern.compile(Joiner.on("|").join(suppressList));
    }


    public void exclusiveOptionsFailure(String opt1, String opt2) {
        System.err.println("Error: Options " + opt1 + " and " + opt2 + " are mutually exclusive.");
        System.exit(1);
    }

    private String lineWrap(String text,int lineWidth) {
        StringBuilder builder = new StringBuilder();
        Scanner scanner = new Scanner(text);
        while (scanner.hasNextLine()) {
            int spaceLeft = lineWidth;
            int spaceWidth = 2;
            String line = scanner.nextLine();
            StringTokenizer st=new StringTokenizer(line);
            while (st.hasMoreTokens()) {
                String word = st.nextToken();
                if ((word.length() + spaceWidth) > spaceLeft) {
                    builder.append("\n");
                    spaceLeft = lineWidth - word.length();
                } else {
                    spaceLeft -= (word.length() + spaceWidth);
                }
                builder.append(word);
                builder.append(' ');
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    public void usage() {
        /*
         * -- can be used as a terminator for the rv-predict specific options.
         * The remaining arguments are what one would pass to the java
         * executable to execute the class/jar The -- option is only required in
         * the less frequent case when some of the java or program options used
         * have the same name as some of the rv-predict options (including --).
         *
         * Moreover, in the unlikely case when the program takes as options -cp
         * or -jar and is run as a class (i.e., not using -jar) then the java
         * -cp option must be used explicitly for disambiguation.
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
        String usageHeader = getUsageHeader();
        String usage = usageHeader + "  Options:";
        String shortUsage = usageHeader + "  Common options (use -h -v for a complete list):";

        Map<String, String> usageMap = new TreeMap<>();
        Map<String, String> shortUsageMap = new TreeMap<>();
        int spacesBeforeCnt;
        int spacesAfterCnt;
        String description;
        for (ParameterDescription parameterDescription : jCommander.getParameters()) {
            if (parameterDescription.getNames().contains(PredictionConfiguration.opt_llvm_predict)) {
                //Omit llvm prediction from the list of options (for now)
                continue;
            }
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
            description += Strings.repeat(" ", spacesBeforeCnt)
                    + parameterDescription.getNames()
                    + Strings.repeat(" ", spacesAfterCnt)
                    + Joiner.on("\n" + Strings.repeat(" ", 4 + max_option_length)).join(
                            lineWrap(parameterDescription.getDescription(),80-max_option_length).split("\\n"))
                    + (aDefault.isEmpty() ? "" : "\n" + Strings.repeat(" ", 4)
                            + Strings.repeat(" ", max_option_length) + aDefault);
            usageMap.put(descriptionKey, description);
            if (!parameter.hidden()) {
                shortUsageMap.put(descriptionKey, description);
            }

        }

        if (verbose) {
            System.out.println(usage);
            for (String usageCase : usageMap.values())
                System.out.println(usageCase);
        } else {
            System.out.println(shortUsage);
            for (String usageCase : shortUsageMap.values())
                System.out.println(usageCase);
        }
    }

    protected abstract String getUsageHeader();

    private String getDefault(ParameterDescription parameterDescription) {
        Object aDefault = parameterDescription.getDefault();
        if (aDefault == null || aDefault.equals(Boolean.FALSE))
            return "";
        return "Default: " + aDefault;
    }

    public String[] getArgs() {
        return args;
    }

    public Logger logger() {
        return logger;
    }

    public void setLogDir(String logDir) {
        this.logDir = logDir;
        try {
            logger.setLogDir(logDir);
        } catch (FileNotFoundException e) {
            System.err.println("Error while attempting to create the logger: directory " + logDir
                    + " not found");
            System.exit(1);
        }
    }

    /**
     * Returns the directory to read and/or write log files.
     */
    public String getLogDir() {
        return logDir;
    }

    public Path getMetadataPath() {
        return Paths.get(logDir, METADATA_BIN);
    }

    public Path getTraceFilePath(int id) {
        return Paths.get(logDir, id + "_" + TRACE_SUFFIX);
    }

    /**
     * Checks if the current RV-Predict instance needs to do logging.
     */
    public abstract boolean isLogging();

    public boolean isOnlinePrediction() {
        return prediction == ONLINE_PREDICTION;
    }

    public boolean isOfflinePrediction() {
        return prediction == OFFLINE_PREDICTION;
    }

    public boolean noPrediction() {
        return prediction == null;
    }

    public boolean stacks() {
        return !nostacks;
    }
}
