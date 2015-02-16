package com.runtimeverification.rvpredict.instrumentation;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.engine.main.Main;
import com.runtimeverification.rvpredict.engine.main.RVPredict;
import com.runtimeverification.rvpredict.log.LoggingEngine;
import com.runtimeverification.rvpredict.log.LoggingFactory;
import com.runtimeverification.rvpredict.log.OnlineLoggingFactory;
import com.runtimeverification.rvpredict.runtime.RVPredictRuntime;
import org.objectweb.asm.ClassReader;
import com.runtimeverification.rvpredict.instrumentation.transformer.ClassTransformer;
import com.runtimeverification.rvpredict.log.OfflineLoggingFactory;
import com.runtimeverification.rvpredict.util.Logger;

public class Agent implements ClassFileTransformer {


    /**
     * Packages/classes that need to be excluded from instrumentation. These are
     * not configurable by the users because including them for instrumentation
     * almost certainly leads to crash.
     */
    private static List<Pattern> IGNORES;
    static {
        String [] ignores = new String[] {
                // rv-predict itself and the libraries we are using
                "com/runtimeverification/rvpredict",

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
        IGNORES = Configuration.getDefaultPatterns(ignores);
    }

    private static String[] MOCKS = new String[] {
        "java/util/Collection",
        "java/util/Map"

        /* YilongL: do not exclude Iterator because it's not likely to slow down
         * logging a lot; besides, I am interested in seeing what could happen */
        // "java/util/Iterator"
    };

    private static List<Pattern> MUST_INCLUDES = Configuration.getDefaultPatterns(new String[] {
            "java/util/Collections$Synchronized"
    });

    static Instrumentation instrumentation;
    private final Configuration config;

    public Agent(Configuration config) {
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
        final Configuration config = new Configuration();
        String[] args = agentArgs.split(" (?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        config.parseArguments(args, false);

        final boolean logOutput = config.log_output.equalsIgnoreCase(Configuration.YES);
        config.logger.report("Log directory: " + config.outdir, Logger.MSGTYPE.INFO);
        if (Configuration.includes != null) {
            config.logger.report("Including: " + config.includeList, Logger.MSGTYPE.INFO);
        }
        if (Configuration.excludes != null) {
            config.logger.report("Excluding: " + config.excludeList, Logger.MSGTYPE.INFO);
        }

        if (!Configuration.online) {
            OfflineLoggingFactory.removeTraceFiles(config.outdir);
        }
        LoggingFactory loggingFactory;
        RVPredict predictionServer = null;
        if (Configuration.online) {
            loggingFactory = new OnlineLoggingFactory();
            try {
                predictionServer = new RVPredict(config, loggingFactory);
            } catch (IOException | ClassNotFoundException e) {
                assert false : "These exceptions should only be thrown for offline prediction";
            }
        } else {
            loggingFactory = new OfflineLoggingFactory(config);
        }
        final LoggingEngine loggingEngine = new LoggingEngine(config, loggingFactory, predictionServer);
        RVPredictRuntime.init(loggingEngine);
        loggingEngine.startLogging();
        if (Configuration.online) {
            loggingEngine.startPredicting();
        }

        inst.addTransformer(new Agent(config), true);
        RVPredictRuntimeMethods.forceClassInitialization();
        for (Class<?> c : inst.getAllLoadedClasses()) {
            if (inst.isModifiableClass(c)) {
                try {
                    inst.retransformClasses(c);
                } catch (UnmodifiableClassException e) {
                    // should not happen
                }
            } else {
                /* TODO(YilongL): Shall(can) we register fields of these
                 * unmodifiable classes too? We know for sure that primitive
                 * classes and array class are unmodifiable. And if these are
                 * the only unmodifiable classes then there is no field for us
                 * to register (even the `length' field of an array object is
                 * accessed by a specific bytecode instruction `arraylength'. */
            }
        }
        System.out.println("Finished retransforming preloaded classes.");

        final Main.CleanupAgent cleanupAgent = new Main.CleanupAgent() {
            @Override
            public void cleanup() {
                try {
                    loggingEngine.finishLogging();
                } catch (IOException e) {
                    System.err.println("Warning: I/O Error while logging the execution. The log might be unreadable.");
                    System.err.println(e.getMessage());
                } catch (InterruptedException e) {
                    System.err.println("Warning: Execution is being forcefully ended. Log data might be lost.");
                    System.err.println(e.getMessage());
                }
            }
        };
        Thread predict = Main.getPredictionThread(config, cleanupAgent, config.predict && !Configuration.online);
        Runtime.getRuntime().addShutdownHook(predict);

        if (config.predict) {
            if (logOutput) {
                config.logger.report(
                        Main.center(Configuration.INSTRUMENTED_EXECUTION_TO_RECORD_THE_TRACE),
                        Logger.MSGTYPE.INFO);
            }
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String cname, Class<?> c, ProtectionDomain d,
            byte[] cbuf) throws IllegalClassFormatException {
        try {
            checkUninterceptedClassLoading(cname, c);

            ClassReader cr = new ClassReader(cbuf);
            if (cname == null) {
                // cname could be null for class like java/lang/invoke/LambdaForm$DMH
                cname = cr.getClassName();
            }
            Metadata.initClassMetadata(cname, cbuf);

            if (instrumentClass(loader, cname, c)) {
                byte[] transformed = ClassTransformer.transform(loader, cbuf, config);
                return transformed;
            } else {
                return null;
            }
        } catch (Throwable e) {
            /* exceptions during class loading are silently suppressed by default */
            System.err.println("Cannot retransform " + cname + ". Exception: " + e);
            e.printStackTrace();
            throw e;
        }
    }

    private static final Set<String> loadedClasses = new HashSet<>();

    private static void checkUninterceptedClassLoading(String cname, Class<?> c) {
        if (Configuration.verbose) {
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
    }

    private boolean instrumentClass(ClassLoader loader, String cname, Class<?> c) {
        if (c != null && c.isInterface()) {
            return false;
        }

        boolean toInstrument = true;
        for (Pattern exclude : config.excludeList) {
            toInstrument = !exclude.matcher(cname).matches();
            if (!toInstrument) break;
        }

        if (toInstrument) {
            for (String mock : MOCKS) {
                if (InstrumentationUtils.isSubclassOf(loader, cname, mock)) {
                    toInstrument = false;
                    if (Configuration.verbose) {
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

        if (!toInstrument) {
            /* include list overrides the above */
            for (Pattern include : config.includeList) {
                toInstrument = include.matcher(cname).matches();
                if (toInstrument) break;
            }
        }

        /* make sure we don't instrument IGNORES even if the user said so */
        if (toInstrument) {
            for (Pattern ignore : IGNORES) {
                toInstrument = !ignore.matcher(cname).matches();
                if (!toInstrument) break;
            }
        }

        if (!toInstrument) {
            for (Pattern include : MUST_INCLUDES) {
                toInstrument = include.matcher(cname).matches();
                if (toInstrument) break;
            }
        }

        return toInstrument;
    }

}
