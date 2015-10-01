package com.runtimeverification.rvpredict.config;

import com.beust.jcommander.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Agent-specific options class for RV-Predict Used by JCommander to parse the
 * the options passed to the agent.
 */
public class AgentConfiguration extends Configuration {

    public static AgentConfiguration instance(String[] args) {
        AgentConfiguration config = new AgentConfiguration();
        config.parseArguments(args);
        return config;
    }

    private static final String RV_PREDICT = "RV-Predict";

    public final static String opt_offline = "--offline";
    @Parameter(names = opt_offline, description = "Run prediction offline", descriptionKey = "2000")
    private boolean offline;

    public final static String opt_only_log = "--log";
    @Parameter(names = opt_only_log, description = "Log execution trace without running prediction", hidden = true, descriptionKey = "1100")
    private boolean only_log = false;

    public final static String opt_event_profile = "--profile";
    @Parameter(names = opt_event_profile, description = "Output event profiling statistics", hidden = true, descriptionKey = "1300")
    private boolean profile;

    public final static String opt_dir_name = "--dir-name";
    @Parameter(names = opt_dir_name, description = "The name of the base directory where RV-Predict creates log directories", descriptionKey = "1500")
    private String dir_name = "";

    public final static String opt_include = "--include";
    @Parameter(names = opt_include, validateWith = PackageValidator.class, description = "Comma separated list of packages to include",
            descriptionKey = "1800")
    private String includes;

    public final static String opt_exclude = "--exclude";
    @Parameter(names = opt_exclude, validateWith = PackageValidator.class, description = "Comma separated list of packages to exclude",
            descriptionKey = "1900")
    private String excludes;


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
    public static final String LOGGING_PHASE_COMPLETED = "Logging phase completed.";
    public static final String TRACE_LOGGED_IN = "\tTrace logged in: ";
    public static final String INSTRUMENTED_EXECUTION_TO_RECORD_THE_TRACE = "Instrumented execution to record the trace";

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




    /**
     * Packages/classes that need to be excluded from instrumentation. These are
     * not configurable by the users because including them for instrumentation
     * almost certainly leads to crash.
     */
    public static List<Pattern> IGNORES;

    static Pattern createClassPattern(String pattern) {
        pattern = pattern.replace('.', '/');
        String escapeChars[] = new String[] {"$","["};
        for (String c : escapeChars) {
           pattern = pattern.replace(c, "\\"  + c);
        }
        return Pattern.compile(pattern.replace("*", ".*")+".*");
    }

    /**
     * Creates a {@link Pattern} list from a String array
     * describing packages/classes using file pattern conventions ({@code *}
     * stands for a sequence of characters)
     *
     * @param patterns the array of package/class descriptions
     * @return A {@link Pattern} list which matches
     *         names specified by the given argument
     */
    public static List<Pattern> getDefaultPatterns(String[] patterns) {
        List<Pattern> patternList = new ArrayList<>();
        for (String pattern : patterns) {
            patternList.add(createClassPattern(pattern));
        }
        return patternList;
    }

    public final List<Pattern> includeList = new ArrayList<>();
    public final List<Pattern> excludeList = new ArrayList<>();

    public static String createAgentArgs(Collection<String> rvPredictArguments) {
        StringBuilder agentOptions = new StringBuilder();
        for (String arg : rvPredictArguments) {
            agentOptions.append(escapeString(arg)).append(" ");
        }
        return agentOptions.toString();
    }

    private static String escapeString(String s) {
        return s.contains(" ") ? "\\\"" + s + "\\\"" : s;
    }


    private void initIncludeList() {
        if (includes != null) {
            for (String include : includes.replace('.', '/').split(",")) {
                if (include.isEmpty()) continue;
                includeList.add(AgentConfiguration.createClassPattern(include));
            }
        }
    }

    private void initExcludeList() {
        if (excludes != null) {
            for (String exclude : excludes.replace('.', '/').split(",")) {
                if (exclude.isEmpty()) continue;
                excludeList.add(AgentConfiguration.createClassPattern(exclude));
            }
        }
    }


    public String getBaseDirName() {
        return dir_name;
    }

    public boolean isProfiling() {
        return profile;
    }

    @Override
    protected void parseArguments(String[] args) {
        super.parseArguments(args);
        initExcludeList();
        initIncludeList();


        /* Carefully handle the interaction between options:
         * 1) 3 different modes: only_profile, only_log, and log_then_predict;
         * 2) 2 types of prediction: online and offline;
         */
        if (profile) {                          /* only profile */
            if (only_log) {
                exclusiveOptionsFailure(opt_event_profile, opt_only_log);
            }
            if (offline) {
                exclusiveOptionsFailure(opt_event_profile, opt_offline);
            }
        } else if (only_log) {                  /* only log */
            if (offline) {
                exclusiveOptionsFailure(opt_only_log, opt_offline);
            }
        } else {
            prediction = offline ? OFFLINE_PREDICTION : ONLINE_PREDICTION;
        }
    }

    @Override
    protected String getProgramName() {
        return RV_PREDICT;
    }

    @Override
    protected String getUsageHeader() {
        return "Usage: java -javaagent:" + getBasePath() + "/rv-predict.jar=\"[rv_predict_options]\"  [java_options] <java_command_line>" + "\n";
    }

    @Override
    public boolean isLogging() {
        return true;
    }
}
