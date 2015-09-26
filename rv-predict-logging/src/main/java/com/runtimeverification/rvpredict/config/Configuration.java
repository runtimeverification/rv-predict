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
import com.microsoft.z3.Context;
import com.runtimeverification.rvpredict.util.Constants;
import com.runtimeverification.rvpredict.util.Logger;

import java.io.*;
import java.lang.reflect.Field;
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
 * Command line options class for rv-predict Used by JCommander to parse the
 * main program parameters.
 */
public class Configuration implements Constants {

    public static final String LOGGING_PHASE_COMPLETED = "Logging phase completed.";
    public static final String TRACE_LOGGED_IN = "\tTrace logged in: ";
    public static final String INSTRUMENTED_EXECUTION_TO_RECORD_THE_TRACE = "Instrumented execution to record the trace";

    private static final String SEPARATOR = System.getProperty("file.separator");
    public static final String JAVA_EXECUTABLE = JavaEnvUtils.getJreExecutable("java");
    public static final String RV_PREDICT_JAR = Configuration.getBasePath() + SEPARATOR + "rv-predict.jar";

    public static final String TRACE_SUFFIX = "trace.bin";

    private static final String METADATA_BIN = "metadata.bin";

    /**
     * Packages/classes that need to be excluded from instrumentation. These are
     * not configurable by the users because including them for instrumentation
     * almost certainly leads to crash.
     */
    public static List<Pattern> IGNORES;
    static {
        String [] ignores = new String[] {
                RVPREDICT_PKG_PREFIX,

                // lz4 library cannot be repackaged because it hard-codes some
                // of its class names in the implementation
                "net/jpountz/",

                // z3 native library cannot be repackaged
                "com/microsoft/z3",

                // array type
                "[",

                // immutable classes
                "cOm/google/common/collect/Immutable".replace("O", "o"), // hack to trick the repackage tool
                "scala/collection/immutable/",

                // Basics of the JDK that everything else is depending on
                "sun/",
                "com/sun",
                "java/",
                "jdk/internal"
        };
        IGNORES = getDefaultPatterns(ignores);
    }

    public final static String[] MOCKS = new String[] {
        "java/util/Collection",
        "java/util/Map",
        "java/util/Iterator",
        // we don't want to instrument any ClassLoader or SecurityManager: issue#512
        "java/lang/ClassLoader",
        "java/lang/SecurityManager"
    };

    public final static Set<String> MUST_REPLACE = new HashSet<>(Arrays.asList(
            "java/util/concurrent/atomic/AtomicBoolean",
            "java/util/concurrent/atomic/AtomicInteger",
            // TODO: handle the other AtomicX classes
            "java/util/concurrent/locks/AbstractQueuedSynchronizer",
            "java/util/concurrent/locks/AbstractQueuedLongSynchronizer",
            "java/util/concurrent/locks/ReentrantLock",
            "java/util/concurrent/locks/ReentrantReadWriteLock",
            // TODO: handle StampedLock from Java 8
            "java/util/concurrent/ArrayBlockingQueue",
            "java/util/concurrent/LinkedBlockingQueue",
            "java/util/concurrent/PriorityBlockingQueue",
            "java/util/concurrent/SynchronousQueue",
            // TODO: handle the other BlockingQueue's
            "java/util/concurrent/Semaphore",
            "java/util/concurrent/CountDownLatch",
            "java/util/concurrent/CyclicBarrier",
            "java/util/concurrent/Exchanger",
            // TODO: handle Phaser
            "java/util/concurrent/FutureTask",
            // TODO: handle CompletableFuture from Java 8
            "java/util/concurrent/ThreadPoolExecutor",
            "java/util/concurrent/ScheduledThreadPoolExecutor",
            "java/util/concurrent/RejectedExecutionHandler",
            "java/util/concurrent/Executors"));

    public final static Pattern MUST_REPLACE_QUICK_TEST_PATTERN = Pattern
            .compile("java/util/concurrent");

    public final static List<Pattern> MUST_INCLUDES;
    static {
        String[] mustIncludes = new String[] {
            "java/security/cert/X509Certificate", "sun/security",   // fix issue #556

            /* fix issue #553: include as few classes as possible when removing false alarms */
            "sun/nio/ch/AsynchronousChannelGroupImpl",
            "sun/nio/ch/Port",
            "sun/nio/ch/ThreadPool",

            "com/runtimeverification/rvpredict/runtime/java/util/concurrent/CyclicBarrier"
        };
        MUST_INCLUDES = getDefaultPatterns(mustIncludes);
    }

    public final List<Pattern> includeList = new ArrayList<>();
    public final List<Pattern> excludeList = new ArrayList<>();
    public final List<String> suppressList = new ArrayList<>();
    public Pattern suppressPattern;

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
            File parent = new File(decodedPath).getParentFile();
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
        if (excludes != null) {
            for (String exclude : excludes.replace('.', '/').split(",")) {
                if (exclude.isEmpty()) continue;
                excludeList.add(createClassPattern(exclude));
            }
        }
    }

    private void initSuppressPattern() {
        suppressList.addAll(Arrays.asList(suppress.split(",")).stream().map(s -> s.trim())
                .filter(s -> !s.isEmpty()).collect(Collectors.toList()));
        suppressPattern = Pattern.compile(Joiner.on("|").join(suppressList));
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

    private static final String RV_PREDICT = "rv-predict";

    private String[] args;
    private String[] rvpredictArgs;
    @Parameter(description = "[java_options] <java_command_line>")
    private List<String> javaArgs = new ArrayList<>();

    private final static String ONLINE_PREDICTION = "ONLINE_PREDICTION";
    private final static String OFFLINE_PREDICTION = "OFFLINE_PREDICTION";
    private static final String LLVM_PREDICTION = "LLVM_PREDICTION";
    private String prediction;

    private boolean log;

    private String logDir;

    public final static String opt_offline = "--offline";
    @Parameter(names = opt_offline, description = "Run prediction offline", descriptionKey = "1000")
    private boolean offline;

    public final static String opt_only_log = "--log";
    @Parameter(names = opt_only_log, description = "Log execution trace without running prediction", descriptionKey = "1100")
    private boolean only_log = false;

    public final static String opt_only_predict = "--predict";
    @Parameter(names = opt_only_predict, description = "Run prediction on logs from the given directory", descriptionKey = "1200")
    private String predict_dir = null;

    public final static String opt_event_profile = "--profile";
    @Parameter(names = opt_event_profile, description = "Output event profiling statistics", hidden = true, descriptionKey = "1300")
    private boolean profile;

    public final static String opt_llvm_predict = "--llvm-predict";
    @Parameter(names = opt_llvm_predict, description = "Run prediction on given llvm trace", hidden = true, descriptionKey = "1400")
    public String llvm_trace_file = null;

    public final static String opt_dir_name = "--dir-name";
    @Parameter(names = opt_dir_name, description = "The name of the base directory where RV-Predict creates log directories", descriptionKey = "1500")
    private String dir_name = "";

    public final static String opt_include = "--include";
    @Parameter(names = opt_include, validateWith = PackageValidator.class, description = "Comma separated list of packages to include",
            descriptionKey = "2000")
    private String includes;

    public final static String opt_exclude = "--exclude";
    @Parameter(names = opt_exclude, validateWith = PackageValidator.class, description = "Comma separated list of packages to exclude",
            descriptionKey = "2100")
    private String excludes;

    final static String opt_window_size = "--window";
    @Parameter(names = opt_window_size, description = "Window size (must be >= 64)", descriptionKey = "2200")
    public int windowSize = 1000;
    private static int MIN_WINDOW_SIZE = 64;

    final static String opt_stacks = "--stacks";
    @Parameter(names = opt_stacks, description = "Record call stack events and compute stack traces in race report", descriptionKey = "2300")
    public boolean stacks = false;

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

    private static final String RVPREDICT_ARGS_TERMINATOR = "--";

    private final Logger logger = new Logger();

    public static Configuration instance(String[] args) {
        Configuration config = new Configuration();
        config.parseArguments(args);
        return config;
    }

    private Configuration() { }

    private void parseArguments(String[] args) {
        this.args = args;
        jCommander = new JCommander(this);
        jCommander.setProgramName(RV_PREDICT);

        /* collect all parameter names */
        Set<String> rvpredictOptionNames = new HashSet<>();
        for (ParameterDescription parameterDescription : jCommander.getParameters()) {
            Collections.addAll(rvpredictOptionNames, parameterDescription.getParameter().names());
        }

        /* attempt to separate rv-predict arguments as much as we can */
        int endIdx = args.length;
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-") && !rvpredictOptionNames.contains(args[i])
                    || RVPREDICT_ARGS_TERMINATOR.equals(args[i])) {
                /* stop as soon as we see an unknown option or the terminator */
                /* JCommander will throw a parsing error upon unknown option; so
                 * we first separate them manually and then let the JCommander
                 * deal with main parameter */
                endIdx = i;
                break;
            }
        }

        /* parse rv-predict arguments */
        try {
            jCommander.parse(Arrays.copyOf(args, endIdx));
            rvpredictArgs = Arrays.copyOf(args, endIdx - javaArgs.size());
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

        initExcludeList();
        initIncludeList();
        initSuppressPattern();

        /* Carefully handle the interaction between options:
         * 1) 5 different modes: only_profile, only_log, only_predict, only_llvm_predict, and log_then_predict;
         * 2) 2 types of prediction: online and offline;
         * 3) log directory can be specified or not.
         *
         * The following code computes 3 variables, e.g. log, prediction, and log_dir,
         * to represent the 3 choices above.
         */
        if (profile) {                          /* only profile */
            if (only_log) {
                exclusiveOptionsFailure(opt_event_profile, opt_only_log);
            }
            if (predict_dir != null) {
                exclusiveOptionsFailure(opt_event_profile, opt_only_predict);
            }
            if (llvm_trace_file != null) {
                exclusiveOptionsFailure(opt_event_profile, opt_llvm_predict);
            }
            if (offline) {
                exclusiveOptionsFailure(opt_event_profile, opt_offline);
            }
        } else if (only_log) {                  /* only log */
            if (predict_dir != null) {
                exclusiveOptionsFailure(opt_only_log, opt_only_predict);
            }
            if (llvm_trace_file != null) {
                exclusiveOptionsFailure(opt_only_log, opt_llvm_predict);
            }
            if (offline) {
                exclusiveOptionsFailure(opt_only_log, opt_offline);
            }
            log = true;
        } else if (predict_dir != null) {       /* only predict */
            if (llvm_trace_file != null) {
                exclusiveOptionsFailure(opt_only_predict, opt_llvm_predict);
            }
            setLogDir(Paths.get(predict_dir).toAbsolutePath().toString());
            prediction = OFFLINE_PREDICTION;
        }  else if (llvm_trace_file != null) {  /* only llvm_predict */
            setLogDir(Paths.get(llvm_trace_file).toAbsolutePath().getParent().toString());
            prediction = LLVM_PREDICTION;
        } else {                                /* log then predict */
            log = true;
            prediction = offline ? OFFLINE_PREDICTION : ONLINE_PREDICTION;
        }

        /* set window size */
        windowSize = Math.max(windowSize, MIN_WINDOW_SIZE);

        int startOfJavaArgs = endIdx;
        if (startOfJavaArgs < args.length
                && RVPREDICT_ARGS_TERMINATOR.equals(args[startOfJavaArgs])) {
            startOfJavaArgs++;
        }
        for (int i = startOfJavaArgs; i < args.length; i++) {
            javaArgs.add(args[i]);
        }
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
        String usageHeader = "Usage: " + RV_PREDICT
                + " [rv_predict_options] [--] "
                + jCommander.getMainParameterDescription() + "\n";
        String usage = usageHeader + "  Options:";
        String shortUsage = usageHeader + "  Common options (use -h -v for a complete list):";

        Map<String, String> usageMap = new TreeMap<>();
        Map<String, String> shortUsageMap = new TreeMap<>();
        int spacesBeforeCnt;
        int spacesAfterCnt;
        String description;
        for (ParameterDescription parameterDescription : jCommander.getParameters()) {
            if (parameterDescription.getNames().contains(opt_llvm_predict)) {
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

    private String getDefault(ParameterDescription parameterDescription) {
        Object aDefault = parameterDescription.getDefault();
        if (aDefault == null || aDefault.equals(Boolean.FALSE))
            return "";
        return "Default: " + aDefault;
    }

    public String[] getArgs() {
        return args;
    }

    public String[] getRVPredictArguments() {
        return rvpredictArgs;
    }

    public List<String> getJavaArguments() {
        return javaArgs;
    }

    public Logger logger() {
        return logger;
    }

    public String getBaseDirName() {
        return dir_name;
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

    public File getLLVMTraceFile() {
        return Paths.get(llvm_trace_file).toFile();
    }

    public boolean isProfiling() {
        return profile;
    }

    /**
     * Checks if the current RV-Predict instance needs to do logging.
     */
    public boolean isLogging() {
        return log;
    }

    public boolean isOnlinePrediction() {
        return prediction == ONLINE_PREDICTION;
    }

    public boolean isOfflinePrediction() {
        return prediction == OFFLINE_PREDICTION;
    }

    public boolean isLLVMPrediction() {
        return prediction == LLVM_PREDICTION;
    }

    public boolean noPrediction() {
        return prediction == null;
    }
}
