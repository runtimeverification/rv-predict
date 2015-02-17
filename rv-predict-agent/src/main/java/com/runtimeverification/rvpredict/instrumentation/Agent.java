package com.runtimeverification.rvpredict.instrumentation;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

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
import com.runtimeverification.rvpredict.metadata.ClassMetadata;
import com.runtimeverification.rvpredict.util.Constants;
import com.runtimeverification.rvpredict.util.InstrumentationUtils;
import com.runtimeverification.rvpredict.util.Logger;

public class Agent implements ClassFileTransformer, Constants {

    private static Instrumentation instrumentation;

    public final static Configuration config = new Configuration();

    public static void premain(String agentArgs, Instrumentation inst) {
        instrumentation = inst;
        InstrumentationUtils.setConfig(config);

        if (agentArgs == null) {
            agentArgs = "";
        }
        if (agentArgs.startsWith("\"")) {
            assert agentArgs.endsWith("\"") : "Argument must be quoted";
            agentArgs = agentArgs.substring(1, agentArgs.length() - 1);
        }
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

        preinitializeClasses();

        inst.addTransformer(new Agent(), true);
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

    /**
     * Pre-initialize certain classes to avoid error-prone class initialization
     * during {@link ClassFileTransformer#transform} .
     * <p>
     * Inspired by <a href=
     * "https://github.com/glowroot/glowroot/blob/master/core/src/main/java/org/glowroot/weaving/PreInitializeWeavingClasses.java"
     * >PreInitializeWeavingClasses</a> from Glowroot project.
     */
    private static void preinitializeClasses() {
        try {
            Class.forName(ThreadLocalRandom.class.getName());
            Class.forName(RVPredictRuntimeMethods.class.getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
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

            if (!cname.startsWith(COM_RUNTIMEVERIFICATION_RVPREDICT) && !cname.startsWith("sun")) {
                ClassMetadata classMetadata = ClassMetadata.getInstance(loader, cname, cbuf);
                if (InstrumentationUtils.needToInstrument(classMetadata, loader)) {
                    byte[] transformed = ClassTransformer.transform(loader, cbuf);
                    return transformed;
                }
            }
            return null;
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

}
