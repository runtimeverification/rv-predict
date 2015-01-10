package rvpredict.instrumentation;

import rvpredict.config.Configuration;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import rvpredict.config.Config;
import rvpredict.db.TraceCache;
import rvpredict.engine.main.Main;
import rvpredict.instrumentation.transformer.ClassTransformer;
import rvpredict.logging.DBEngine;
import rvpredict.runtime.RVPredictRuntime;
import rvpredict.util.Logger;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.Arrays;

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
        "org.objectweb.asm",
        "com/beust",
        "org/apache/tools/ant/util/JavaEnvUtils",

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

    public Agent(Config config) {
        this.config = config;
    }

    public static void premain(String agentArgs, Instrumentation inst) {
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
                System.out.println("Includes: " + Arrays.toString(includes));
                int length = config.includeList.length;
                String[] array = new String[length + includes.length];
                System.arraycopy(includes, 0, array, length, includes.length);
                config.includeList = array;
            }
            System.out.println("Including: " + Arrays.toString(config.includeList));
        }

        TraceCache.removeTraceFiles(commandLine.outdir);
        final DBEngine db = new DBEngine(commandLine);
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
                db.finishLogging();
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

    @Override
    public byte[] transform(ClassLoader loader, String cname, Class<?> c, ProtectionDomain d,
            byte[] cbuf) throws IllegalClassFormatException {
        boolean toInstrument = true;
        if (config.excludeList != null) {
            for (String exclude : config.excludeList) {
                if (cname.startsWith(exclude)) {
                    toInstrument = false;
                    break;
                }
            }
        }
        if (config.includeList != null) {
            for (String include : config.includeList) {
                if (cname.startsWith(include)) {
                    toInstrument = true;
                    break;
                }
            }
        }

//        System.err.println(cname + " " + toInstrument);
        for (String ignore : IGNORES) {
            toInstrument = toInstrument && !cname.startsWith(ignore);
        }
        if (toInstrument) {
            ClassReader cr = new ClassReader(cbuf);

            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
            ClassVisitor instrumentor = new ClassTransformer(cw, config);
            ClassVisitor cv = new CheckClassAdapter(instrumentor);
            cr.accept(cv, 0);

            byte[] ret = cw.toByteArray();
            return ret;
        } else {
            return cbuf;
        }
    }
}
