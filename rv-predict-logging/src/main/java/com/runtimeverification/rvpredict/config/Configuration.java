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
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Command line options class for rv-predict Used by JCommander to parse the
 * main program parameters.
 */
public class Configuration implements Constants {

    public static final String LOGGING_PHASE_COMPLETED = "Logging phase completed.";
    public static final String TRACE_LOGGED_IN = "\tTrace logged in: ";
    public static final String INSTRUMENTED_EXECUTION_TO_RECORD_THE_TRACE = "Instrumented execution to record the trace";

    /**
     * Packages/classes that are excluded from instrumentation by default. These are
     * configurable by the users through the <code>--exclude</code> command option.
     */
     private static String[] DEFAULT_EXCLUDES = new String[] {
            "javax.*",
            "sunw.*",
            "com.sun.*",
            "com.ibm.*",
            "com.apple.*",
            "apple.awt.*",
            "org.xml.*",
            "jdk.internal.*"
    };

     /**
      * Packages/classes that need to be excluded from instrumentation. These are
      * not configurable by the users because including them for instrumentation
      * almost certainly leads to crash.
      */
     public static List<Pattern> IGNORES;
     static {
         String [] ignores = new String[] {
                 COM_RUNTIMEVERIFICATION_RVPREDICT,

                // lz4 library cannot be repackaged because it hard-codes some
                // of its class names in the implementation
                 "net/jpountz/",

                 // array type
                 "[",

                 // Basics of the JDK that everything else is depending on
                 "sun/",
                 "java/"
         };
         IGNORES = getDefaultPatterns(ignores);
     }

     public static String[] MOCKS = new String[] {
         "java/util/Collection",
         "java/util/Map"

         /* YilongL: do not exclude Iterator because it's not likely to slow down
          * logging a lot; besides, I am interested in seeing what could happen */
         // "java/util/Iterator"
     };

    public static List<Pattern> MUST_INCLUDES;
    static {
        String[] mustIncludes = new String[] {
                "java/util/concurrent/Semaphore",
                "java/util/concurrent/CountDownLatch",
                "java/util/concurrent/CyclicBarrier",
                "java/util/concurrent/ArrayBlockingQueue",
                "java/util/concurrent/LinkedBlockingQueue"
        };
        MUST_INCLUDES = getDefaultPatterns(mustIncludes);
    }

    public final List<Pattern> includeList = new ArrayList<>();
    public final List<Pattern> excludeList = new ArrayList<>();

    private JCommander jCommander;

    private static Pattern createClassPattern(String pattern) {
        pattern = pattern.replace('.', '/');
        String escapeChars[] = new String[] {"$","["};
        for (String c : escapeChars) {
           pattern = pattern.replace(c, "\\"  + c);
        }
        return Pattern.compile(pattern.replace("*", ".*")+".*");
    }

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
            File parent = new File(decodedPath).getParentFile().getParentFile();
            return parent.getAbsolutePath();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void initIncludeList() {
        if (includes != null) {
            for (String include : includes.replace('.', '/').split(",")) {
                if (include.isEmpty()) continue;
                includeList.add(createClassPattern(include));
            }
        }
    }

    private void initExcludeList() {
        String excludes = Configuration.excludes;
        if (excludes == null) {
            excludeList.addAll(getDefaultPatterns(DEFAULT_EXCLUDES));
        } else {
            excludes = excludes.trim();
            if (excludes.charAt(0) == '+') { // initialize excludeList with default patterns
                excludes = excludes.substring(1);
                excludeList.addAll(getDefaultPatterns(DEFAULT_EXCLUDES));
            }
            for (String exclude : excludes.replace('.', '/').split(",")) {
                exclude = exclude.trim();
                if (!exclude.isEmpty())
                    excludeList.add(createClassPattern(exclude));
            }
        }
    }

    /**
     * Creates a {@link java.util.regex.Pattern} list from a String array
     * describing packages/classes using file pattern conventions ({@code *}
     * stands for a sequence of characters)
     *
     * @param patterns the array of package/class descriptions
     * @return A {@link java.util.regex.Pattern} list which matches
     *         names specified by the given argument
     */
    public static List<Pattern> getDefaultPatterns(String[] patterns) {
        List<Pattern> patternList = new ArrayList<>();
        for (String pattern : patterns) {
            patternList.add(createClassPattern(pattern));
        }
        return patternList;
    }

    public enum OS {
        OSX(true, "osx"), UNIX(true, "linux"), UNKNOWN(false, null), WIN(false, "cygwin");

        private OS(boolean isPosix, String libDir) {
            this.isPosix = isPosix;
            String arch = System.getProperty("os.arch");
            this.libDir = getBasePath() + File.separator + "lib" + File.separator + "native"
                    + File.separator + libDir + File.separator
                    + (arch.toLowerCase().contains("64") ? "64" : "32");
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
                System.err.println("Unknown OS type. " + System.getProperty("os.name")
                        + " not recognized. "
                        + "Please contact RV-Predict developers with details of your OS.");
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
    @Parameter(description = "<java_command_line>")
    public List<String> command_line;

    private String[] args;
    private String[] rvArgs;

    public final static String opt_only_log = "--log";
    @Parameter(names = opt_only_log, description = "Record execution in given directory (no prediction)", descriptionKey = "1000")
    public String log_dir = null;
    public boolean log = true;

    public final static String opt_include = "--include";
    @Parameter(names = opt_include, validateWith = PackageValidator.class, description = "Comma separated list of packages to include." +
            "\nPrefix with + to add to the default included packages", hidden = true, descriptionKey = "1025")
    public static String includes;

    public final static String opt_exclude = "--exclude";
    @Parameter(names = opt_exclude, validateWith = PackageValidator.class, description = "Comma separated list of packages to exclude." +
            "\nPrefix with + to add to the default excluded packages", hidden = true, descriptionKey = "1030")
    public static String excludes;

    final static String opt_event_profile = "--profile";
    @Parameter(names = opt_event_profile, description = "Output event profiling statistics", hidden = true, descriptionKey = "1070")
    public static boolean profile;

    public final static String opt_only_predict = "--predict";
    @Parameter(names = opt_only_predict, description = "Run prediction on logs from given directory", descriptionKey = "2000")
    public String predict_dir = null;
    public boolean predict = true;

    public final static String opt_online = "--online";
    @Parameter(names = opt_online, description = "Run prediction online", hidden = true, descriptionKey = "2005")
    private static boolean online = false;
    public static PredictionAlgorithm prediction;

    public enum PredictionAlgorithm {
        ONLINE, OFFLINE, NONE;

        public boolean isOnline() {
            return this == ONLINE;
        }
        
        public boolean isOffline() {
            return this == OFFLINE;
        }
    };

    final static String opt_max_len = "--maxlen";
    @Parameter(names = opt_max_len, description = "Window size", hidden = true, descriptionKey = "2010")
    public int windowSize = 1000;

    final static String opt_volatile = "--volatile";
    @Parameter(names = opt_volatile, description = "Check unordered conflict accesses on volatile variables", hidden = true, descriptionKey = "2030")
    public boolean checkVolatile;

    final static String opt_smt_solver = "--solver";
    @Parameter(names = opt_smt_solver, description = "SMT solver to use. <solver> is one of [z3].", hidden = true, descriptionKey = "2050")
    public String smt_solver = "z3";

    final static String opt_solver_timeout = "--solver-timeout";
    @Parameter(names = opt_solver_timeout, description = "Solver timeout in seconds", hidden = true, descriptionKey = "2060")
    public long solver_timeout = 60;

    final static String opt_timeout = "--timeout";
    @Parameter(names = opt_timeout, description = "RV-Predict timeout in seconds", hidden = true, descriptionKey = "2070")
    public long timeout = 3600;

    final static String opt_simple_report = "--simple-report";
    @Parameter(names = opt_simple_report, description = "Output simple data race report", hidden = true, descriptionKey = "2080")
    public boolean simple_report = false;

    final static String opt_debug = "--debug";
    @Parameter(names = opt_debug, description = "Output developer debugging information", hidden = true, descriptionKey = "2090")
    public static boolean debug = false;

    public final static String opt_outdir = "--dir";
    @Parameter(names = opt_outdir, description = "Output directory", hidden = true, descriptionKey = "8000")
    public String outdir = null;

    final static String short_opt_verbose = "-v";
    final static String opt_verbose = "--verbose";
    @Parameter(names = { short_opt_verbose, opt_verbose }, description = "Generate more verbose output", descriptionKey = "9000")
    public static boolean verbose;

    final static String short_opt_help = "-h";
    final static String opt_help = "--help";
    @Parameter(names = { short_opt_help, opt_help }, description = "Print help info", help = true, descriptionKey = "9900")
    public boolean help;

    public final static String opt_java = "--";
    public Logger logger;

    public void parseArguments(String[] args, boolean checkJava) {
        this.args = args;
        jCommander = new JCommander(this);
        jCommander.setProgramName(PROGRAM_NAME);

        // Collect all parameter names. It would be nice if JCommander provided
        // this directly.
        Set<String> options = new HashSet<>();
        for (ParameterDescription parameterDescription : jCommander.getParameters()) {
            Collections.addAll(options, parameterDescription.getParameter().names());
        }

        // Detecting a candidate for program options start
        int max = Arrays.asList(args).indexOf(opt_java);
        if (max != -1) { // -- was used. Using it as a separator for java
                         // command line
            rvArgs = Arrays.copyOf(args, max);
            max++;
        } else { // -- was not specified. Look for the first unknown option
            for (max = 0; max < args.length; max++) {
                if (args[max].startsWith("-") && !options.contains(args[max]))
                    break; // the index of the first unknown command
            }
            rvArgs = Arrays.copyOf(args, max);
        }

        // get all rv-predict arguments and (potentially) the first unnamed
        // program arguments
        try {
            jCommander.parse(rvArgs);
        } catch (ParameterException e) {
            System.err.println("Error: Cannot parse command line arguments.");
            System.err.println(e.getMessage());
            System.exit(1);
        }


        initExcludeList();
        initIncludeList();

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
        } else {
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
                            Paths.get(System.getProperty("java.io.tmpdir")), "rv-predict")
                            .toString();
                } catch (IOException e) {
                    System.err.println("Error while attempting to create log dir.");
                    System.err.println(e.getMessage());
                    System.exit(1);
                }
            }
        }

        if (!predict) {
            prediction = PredictionAlgorithm.NONE;
        } else {
            prediction = online ? PredictionAlgorithm.ONLINE : PredictionAlgorithm.OFFLINE;
        }

        if (command_line != null) { // if there are unnamed options they should
                                    // all be at the end
            int i = rvArgs.length - 1;
            for (String command : command_line) {
                if (!command.equals(rvArgs[i--])) {
                    System.err.println("Error: Unexpected argument " + command
                            + " among rv-predict options.");
                    System.err.println("The options terminator '" + opt_java
                            + "' can be used to separate the java command.");
                    System.exit(1);
                }
            }
        }

        if (help) {
            usage();
            System.exit(0);
        }

        List<String> argList = Arrays.asList(Arrays.copyOfRange(args, max, args.length));
        if (command_line == null) { // otherwise the java command has already
                                    // started
            command_line = new ArrayList<>(argList);
            if (command_line.isEmpty() && log && checkJava) {
                System.err.println("Error: Java command line is empty.");
                usage();
                System.exit(1);
            }
        } else {
            command_line.addAll(argList);
        }
        logger = new Logger();
    }

    public void exclusiveOptionsFailure(String opt1, String opt2) {
        System.err.println("Error: Options " + opt1 + " and " + opt2 + " are mutually exclusive.");
        System.exit(1);
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
        String usageHeader = "Usage: " + PROGRAM_NAME
                + " [rv_predict_options] [--] [java_options] "
                + jCommander.getMainParameterDescription() + "\n";
        String usage = usageHeader + "  Options:";
        String shortUsage = usageHeader + "  Common options (use -h -v for a complete list):";

        Map<String, String> usageMap = new TreeMap<>();
        Map<String, String> shortUsageMap = new TreeMap<>();
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
            description += Strings.repeat(" ", spacesBeforeCnt)
                    + parameterDescription.getNames()
                    + Strings.repeat(" ", spacesAfterCnt)
                    + Joiner.on("\n" + Strings.repeat(" ", 4 + max_option_length)).join(
                            parameterDescription.getDescription().split("\\n"))
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

    private String getDefault(ParameterDescription parameterDescription) {
        Object aDefault = parameterDescription.getDefault();
        if (aDefault == null || aDefault.equals(Boolean.FALSE))
            return "";
        return "Default: " + aDefault;
    }

    public String[] getArgs() {
        return args;
    }

    public String[] getRvArgs() {
        return rvArgs;
    }
}
