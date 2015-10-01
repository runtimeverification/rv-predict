package com.runtimeverification.rvpredict.instrument;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import com.google.common.io.Resources;
import com.runtimeverification.rvpredict.config.AgentConfiguration;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.engine.main.RVPredict;
import com.runtimeverification.rvpredict.log.ILoggingEngine;
import com.runtimeverification.rvpredict.log.PersistentLoggingEngine;
import com.runtimeverification.rvpredict.log.ProfilerLoggingEngine;
import com.runtimeverification.rvpredict.log.VolatileLoggingEngine;
import com.runtimeverification.rvpredict.runtime.RVPredictRuntime;

import org.apache.tools.ant.DirectoryScanner;
import org.objectweb.asm.ClassReader;

import com.runtimeverification.rvpredict.instrument.transformer.ClassTransformer;
import com.runtimeverification.rvpredict.instrument.transformer.TransformStrategy;
import com.runtimeverification.rvpredict.metadata.ClassFile;
import com.runtimeverification.rvpredict.util.Constants;
import com.runtimeverification.rvpredict.util.Logger;

public class Agent implements ClassFileTransformer, Constants {

    private static Instrumentation instrumentation;

    public static AgentConfiguration config;

    public static void premain(String agentArgs, Instrumentation inst) {
        instrumentation = inst;
        preinitializeClasses();
        processAgentArguments(agentArgs);
        initLoggingDirectory();
        printStartupInfo();

        ILoggingEngine loggingEngine;
        if (config.isProfiling()) {
            loggingEngine = new ProfilerLoggingEngine(RVPredictRuntime.metadata);
        } else {
            assert config.isLogging();
            loggingEngine = config.isOnlinePrediction() ?
                new VolatileLoggingEngine(config, RVPredictRuntime.metadata) :
                new PersistentLoggingEngine(config, RVPredictRuntime.metadata);
        }
        RVPredictRuntime.init(config, loggingEngine);

        inst.addTransformer(new Agent(), true);
        for (Class<?> c : inst.getAllLoadedClasses()) {
            if (inst.isModifiableClass(c)) {
                // YilongL: temporary hack to work around JVM crash bug JDK-8075318
                if (c.getName().startsWith("java.lang.invoke")) {
                    continue;
                }

                try {
                    inst.retransformClasses(c);
                } catch (UnmodifiableClassException e) {
                    // should not happen
                    e.printStackTrace();
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
        config.logger().report("Finished retransforming preloaded classes.", Logger.MSGTYPE.INFO);

        Runtime.getRuntime().addShutdownHook(RVPredict.getPredictionThread(config, loggingEngine));
    }

    /**
     * Pre-initialize certain classes to avoid error-prone class initialization
     * during {@link ClassFileTransformer#transform} .
     * <p>
     * Inspired by <a href=
     * "https://github.com/glowroot/glowroot/blob/master/core/src/main/java/org/glowroot/weaving/PreInitializeWeavingClasses.java"
     * >PreInitializeWeavingClasses</a> from Glowroot project.
     */
    @SuppressWarnings("rawtypes")
    private static void preinitializeClasses() {
        try {
            new java.util.LinkedHashMap().keySet().iterator();
            Class.forName(ThreadLocalRandom.class.getName());
            Class.forName(RVPredictRuntimeMethods.class.getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void processAgentArguments(String agentArgs) {
        if (agentArgs == null) {
            agentArgs = "";
        }
        if (agentArgs.startsWith("\"")) {
            assert agentArgs.endsWith("\"") : "Argument must be quoted";
            agentArgs = agentArgs.substring(1, agentArgs.length() - 1);
        }
        String[] args = agentArgs.split(" (?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        config = AgentConfiguration.instance(args);
    }

    private static void printStartupInfo() {
        config.logger().reportPhase(AgentConfiguration.INSTRUMENTED_EXECUTION_TO_RECORD_THE_TRACE);
        if (config.getLogDir() != null) {
            config.logger().report("Log directory: " + config.getLogDir(), Logger.MSGTYPE.INFO);
        }
        if (!config.includeList.isEmpty()) {
            config.logger().report("Including: " + config.includeList, Logger.MSGTYPE.INFO);
        }
        if (!config.excludeList.isEmpty()) {
            config.logger().report("Excluding: " + config.excludeList, Logger.MSGTYPE.INFO);
        }
        if (!config.suppressList.isEmpty()) {
            config.logger().report("Suppressing race on: " + config.suppressList, Logger.MSGTYPE.INFO);
        }
    }

    private static void initLoggingDirectory() {
        /* compute and set path to log directory if it is still unknown */
        String logDir = config.getLogDir();
        if (logDir == null) {
            try {
                Path baseDir = Paths.get(System.getProperty("java.io.tmpdir"),
                        config.getBaseDirName());
                if (!Files.exists(baseDir)) {
                    Files.createDirectory(baseDir);
                }
                logDir = Files.createTempDirectory(baseDir, "rv-predict").toString();
                config.setLogDir(logDir);
            } catch (IOException e) {
                System.err.println("Error while attempting to create log dir.");
                System.err.println(e.getMessage());
                System.exit(1);
            }
        }

        /* scan all trace files */
        DirectoryScanner scanner = new DirectoryScanner();
        scanner.setIncludes(new String[] { "*" + Configuration.TRACE_SUFFIX + "*" });
        scanner.setBasedir(logDir);
        scanner.scan();

        for (String fname : scanner.getIncludedFiles()) {
            try {
                Files.delete(Paths.get(logDir, fname));
            } catch (IOException e) {
                config.logger().report(
                        "Cannot delete trace file " + fname + "from dir. " + logDir,
                        Logger.MSGTYPE.ERROR);
            }
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String cname, Class<?> c, ProtectionDomain d,
            byte[] cbuf) throws IllegalClassFormatException {
        try {
            ClassReader cr = new ClassReader(cbuf);
            if (cname == null) {
                // cname could be null for class like java/lang/invoke/LambdaForm$DMH
                cname = cr.getClassName();
            }

            checkUninterceptedClassLoading(cname, c);

            if (cname.equals("java/lang/Thread")) {
                return ClassTransformer.transform(loader, cname, cbuf, config,
                        TransformStrategy.THREAD);
            } else if (cname.startsWith("java/util/concurrent/ForkJoinPool")
                    || cname.startsWith("java/util/concurrent/ForkJoinTask")
                    || cname.startsWith("java/util/concurrent/CountedCompleter")
                    || cname.startsWith("java/util/stream/AbstractTask")) {
                String AGENT_CLASS = "com/runtimeverification/rvpredict/instrument/Agent";
                return Resources.toByteArray(new URL(ClassLoader.getSystemClassLoader()
                        .getResource(AGENT_CLASS + ".class").toString().replace(AGENT_CLASS, cname)));
            } else if (!cname.startsWith(RVPREDICT_PKG_PREFIX)
                    || cname.startsWith(RVPREDICT_RUNTIME_PKG_PREFIX)) {
                ClassFile classFile = ClassFile.getInstance(loader, cname, cbuf);
                if (InstrumentUtils.needToInstrument(classFile)) {
                    byte[] transformed = ClassTransformer.transform(loader, cname, cbuf, config,
                            TransformStrategy.FULL);
                    return transformed;
                }
            }
            return null;
        } catch (Throwable e) {
            /* exceptions during class loading are silently suppressed by default */
            config.logger().debug("Cannot retransform " + cname);
            config.logger().debug(e);
            if (Configuration.debug) {
                // fail-fast strategy under debug mode
                System.exit(1);
            }
            throw new RuntimeException(e);
        }
    }

    private static final Set<String> loadedClasses = Collections.synchronizedSet(new HashSet<>());

    private static void checkUninterceptedClassLoading(String cname, Class<?> c) {
        if (Configuration.debug) {
//            if (c == null) {
//                System.err.println("[Java-agent] intercepted class load: " + cname);
//            } else {
//                System.err.println("[Java-agent] intercepted class redefinition/retransformation: " + c);
//            }

            loadedClasses.add(cname.replace("/", "."));
            for (Class<?> cls : instrumentation.getAllLoadedClasses()) {
                String name = cls.getName();
                if (loadedClasses.add(name) && !cls.isArray()
                        && !name.startsWith("com.runtimeverification.rvpredict")
                        && !name.startsWith("java.lang")
                        && !name.startsWith("sun.")) {
                    config.logger().debug("[Java-agent] missed to intercept class load: " + cls);
                }
            }
        }
    }

}
