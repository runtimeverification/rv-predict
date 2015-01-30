package rvpredict.instrumentation;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import rvpredict.config.Config;
import rvpredict.config.Configuration;
import rvpredict.db.TraceCache;
import rvpredict.engine.main.Main;
import rvpredict.logging.LoggingEngine;
import rvpredict.instrumentation.transformer.ClassTransformer;
import rvpredict.runtime.RVPredictRuntime;
import rvpredict.util.Logger;

public class Agent implements ClassFileTransformer {

    private final Config config;

    /**
     * Packages/classes that need to be excluded from instrumentation. These are
     * not configurable by the users because including them for instrumentation
     * almost certainly leads to crash.
     */
    private static String[] IGNORES = new String[] {
        // rv-predict itself and the libraries we are using
        "rvpredict",
        /* TODO(YilongL): shall we repackage these libraries using JarJar? */
        "org.objectweb.asm",
        "com/beust",
        "org/apache/tools/ant",
        "org/apache/commons/collections4",

        // array type
        "[",

        // JDK classes used by the RV-Predict runtime library
        "java/io",
        "java/nio",
        "java/util/concurrent/atomic/AtomicLong",
        "java/util/concurrent/ConcurrentHashMap",
        "java/util/zip/GZIPOutputStream",
        "java/util/regex",

        // Basics of the JDK that everything else is depending on
        "sun",
        "java/lang",

        /* we provide complete mocking of the jucl package */
        "java/util/concurrent/locks"
    };

    private static String[] MOCKS = new String[] {
        "java/util/Collection",
        "java/util/Map"

        /* YilongL: do not exclude Iterator because it's not likely to slow down
         * logging a lot; besides, I am interested in seeing what could happen */
        // "java/util/Iterator"
    };

    private static String[] MUST_INCLUDES = new String[] {
        "java/util/Collections$Synchronized"
    };

    static Instrumentation instrumentation;

    public Agent(Config config) {
        this.config = config;
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        instrumentation = inst;

        if (agentArgs == null) {
            agentArgs = "";
        }
        if (agentArgs.startsWith("\"")) {
            assert agentArgs.endsWith("\"") : "Argument must be quoted";
            agentArgs = agentArgs.substring(1, agentArgs.length() - 1);
        }
        final Config config = Config.instance;
        final Configuration commandLine = config.commandLine;
        String[] args = agentArgs.split(" (?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        commandLine.parseArguments(args, false);

        final boolean logOutput = commandLine.log_output.equalsIgnoreCase(Configuration.YES);
        commandLine.logger.report("Log directory: " + commandLine.outdir, Logger.MSGTYPE.INFO);
        if (Configuration.additionalExcludes != null) {
            String[] excludes = Configuration.additionalExcludes.replace('.', '/').split(",");
            if (config.excludeList == null) {
                config.excludeList = excludes;
            } else {
                String[] array = new String[config.excludeList.length + excludes.length];
                System.arraycopy(config.excludeList, 0, array, 0, config.excludeList.length);
                System.arraycopy(excludes, 0, array, config.excludeList.length, excludes.length);
                config.excludeList = array;
            }
            System.out.println("Excluding: " + Arrays.toString(config.excludeList));
        }
        if (Configuration.additionalIncludes != null) {
            String[] includes = Configuration.additionalIncludes.replace('.', '/').split(",");
            if (config.includeList == null) {
                config.includeList = includes;
            } else {
                String[] array = new String[config.includeList.length + includes.length];
                System.arraycopy(config.includeList, 0, array, 0, config.includeList.length);
                System.arraycopy(includes, 0, array, config.includeList.length, includes.length);
                config.includeList = array;
            }
            System.out.println("Including: " + Arrays.toString(config.includeList));
        }

        TraceCache.removeTraceFiles(commandLine.outdir);
        final LoggingEngine db = new LoggingEngine(commandLine);
        RVPredictRuntime.init(db);

        inst.addTransformer(new Agent(config), true);
        for (Class<?> c : inst.getAllLoadedClasses()) {
            if (!c.isInterface() && inst.isModifiableClass(c)) {
//                String className = c.getName();
//                if (!className.startsWith("sun") && !className.startsWith("java.lang")
//                        && !className.startsWith("java.io") && !className.startsWith("java.nio")) {
//                    System.err.println("Preloaded class: " + className);
//                }
                try {
                    inst.retransformClasses(c);
                } catch (UnmodifiableClassException e) {
                    System.err.println("Cannot retransform class. Exception: " + e);
                    System.exit(1);
                }
            }
        }
        System.out.println("Finished retransforming preloaded classes.");

        final Main.CleanupAgent cleanupAgent = new Main.CleanupAgent() {
            @Override
            public void cleanup() {
                try {
                    db.finishLogging();
                } catch (IOException e) {
                    System.err.println("Warning: I/O Error while logging the execution. The log might be unreadable.");
                    System.err.println(e.getMessage());
                } catch (InterruptedException e) {
                    System.err.println("Warning: Execution is being forcefully ended. Log data might be lost.");
                    System.err.println(e.getMessage());
                }
            }
        };
        Thread predict = Main.getPredictionThread(commandLine, cleanupAgent, commandLine.predict);
        Runtime.getRuntime().addShutdownHook(predict);

        if (commandLine.predict) {
            if (logOutput) {
                commandLine.logger.report(
                        Main.center(Configuration.INSTRUMENTED_EXECUTION_TO_RECORD_THE_TRACE),
                        Logger.MSGTYPE.INFO);
            }
        }
    }

    private static final Set<String> loadedClasses = new HashSet<>();

    @Override
    public byte[] transform(ClassLoader loader, String cname, Class<?> c, ProtectionDomain d,
            byte[] cbuf) throws IllegalClassFormatException {
        try {
            ClassReader cr = new ClassReader(cbuf);
            if (cname == null) {
                // cname could be null for class like java/lang/invoke/LambdaForm$DMH
                cname = cr.getClassName();
            }
            MetaData.setSuperclass(cname, cr.getSuperName());
            MetaData.setInterfaces(cname, cr.getInterfaces());

            if (config.verbose) {
                if (c == null) {
                    System.err.println("[Java-agent] intercepted class load: " + cname);
                } else {
                    System.err.println("[Java-agent] intercepted class redefinition/retransformation: " + c);
                }

                loadedClasses.add(cname.replace("/", "."));
                for (Class<?> cls : instrumentation.getAllLoadedClasses()) {
                    if (loadedClasses.add(cls.getName())) {
                        System.err.println("[Java-agent] missed to intercept class load: " + cls);
                    }
                }
            }

            boolean toInstrument = true;
            if (config.excludeList != null) {
                for (String exclude : config.excludeList) {
                    if (cname.startsWith(exclude)) {
                        toInstrument = false;
                        break;
                    }
                }
            }

            // System.err.println(cname + " " + toInstrument);
            for (String ignore : IGNORES) {
                toInstrument = toInstrument && !cname.startsWith(ignore);
            }
            if (toInstrument) {
                for (String mock : MOCKS) {
                    if (Utility.isSubclassOf(cname, mock)) {
                        toInstrument = false;
                        if (config.verbose) {
                            /* TODO(YilongL): this may cause missing data races if
                             * the mock for interface/superclass does not contain
                             * methods specific to this implementation. This could
                             * be a big problem if the application makes heavy use
                             * of helper methods specific in some high-level
                             * concurrency library (e.g. Guava) while most of the
                             * classes are simply excluded here */
                            System.err.println("[Java-agent] excluded " + c
                                    + " from instrumentation because we are mocking " + mock);
                        }
                        break;
                    }
                }
            }

            /* include list overrides the above */
            if (config.includeList != null) {
                for (String include : config.includeList) {
                    if (cname.startsWith(include)) {
                        toInstrument = true;
                        break;
                    }
                }
            }
            if (!toInstrument) {
                for (String include : MUST_INCLUDES) {
                    if (cname.startsWith(include)) {
                        toInstrument = true;
                        break;
                    }
                }
            }

            return toInstrument ? ClassTransformer.transform(cbuf, config) : null;
        } catch (Throwable e) {
            /* exceptions during class loading are silently suppressed by default */
            System.err.println("Cannot retransform " + cname + ". Exception: " + e);
            throw e;
        }
    }
}
