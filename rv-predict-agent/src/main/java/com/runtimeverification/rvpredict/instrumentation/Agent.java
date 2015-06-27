package com.runtimeverification.rvpredict.instrumentation;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.engine.main.RVPredict;
import com.runtimeverification.rvpredict.log.ILoggingEngine;
import com.runtimeverification.rvpredict.log.PersistentLoggingEngine;
import com.runtimeverification.rvpredict.log.ProfilerLoggingEngine;
import com.runtimeverification.rvpredict.log.VolatileLoggingEngine;
import com.runtimeverification.rvpredict.runtime.RVPredictRuntime;

import org.apache.tools.ant.DirectoryScanner;
import org.objectweb.asm.ClassReader;

import com.runtimeverification.rvpredict.instrumentation.transformer.ClassTransformer;
import com.runtimeverification.rvpredict.metadata.ClassFile;
import com.runtimeverification.rvpredict.util.Constants;
import com.runtimeverification.rvpredict.util.Logger;

public class Agent implements ClassFileTransformer, Constants {

    private static Instrumentation instrumentation;

    public static Configuration config;

    public static void premain(String agentArgs, Instrumentation inst) {
        instrumentation = inst;
        preinitializeClasses();
        processAgentArguments(agentArgs);
        printStartupInfo();
        initLoggingDirectory();

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
        System.out.println("Finished retransforming preloaded classes.");

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
        config = Configuration.instance(args);
    }

    private static void printStartupInfo() {
        config.logger().reportPhase(Configuration.INSTRUMENTED_EXECUTION_TO_RECORD_THE_TRACE);
        if (config.getLogDir() != null) {
            config.logger().report("Log directory: " + config.getLogDir(), Logger.MSGTYPE.INFO);
        }
        if (config.includes != null) {
            config.logger().report("Including: " + config.includeList, Logger.MSGTYPE.INFO);
        }
        if (config.excludes != null) {
            config.logger().report("Excluding: " + config.excludeList, Logger.MSGTYPE.INFO);
        }
    }

    private static void initLoggingDirectory() {
        String directory = config.getLogDir();
        if (directory != null) {
            /* scan all trace files */
            DirectoryScanner scanner = new DirectoryScanner();
            scanner.setIncludes(new String[] { "*" + Configuration.TRACE_SUFFIX + "*" });
            scanner.setBasedir(directory);
            scanner.scan();

            for (String fname : scanner.getIncludedFiles()) {
                try {
                    Files.delete(Paths.get(directory, fname));
                } catch (IOException e) {
                    System.err.println("Cannot delete trace file " + fname + "from dir. " + directory);
                }
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

            if (!cname.startsWith(COM_RUNTIMEVERIFICATION_RVPREDICT) && !cname.startsWith("sun")
                    || cname.startsWith(COM_RUNTIMEVERIFICATION_RVPREDICT_RUNTIME)) {
                ClassFile classFile = ClassFile.getInstance(loader, cname, cbuf);
                if (InstrumentUtils.needToInstrument(classFile)) {
                    byte[] transformed = ClassTransformer.transform(loader, cbuf, config);
                    return transformed;
                }
            }
            return null;
        } catch (Throwable e) {
            /* exceptions during class loading are silently suppressed by default */
            config.logger().debug("Cannot retransform " + cname + ". Exception: " + e);
            if (Configuration.debug) {
                config.logger().debug(e);
                // fail-fast strategy under debug mode
                System.exit(1);
            }
            throw e;
        }
    }

    private static final Set<String> loadedClasses = new HashSet<>();

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
