package rvpredict.instrumentation;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;
import rvpredict.config.Configuration;
import rvpredict.db.TraceCache;
import rvpredict.engine.main.Main;
import rvpredict.instrumentation.transformer.ClassTransformer;
import rvpredict.logging.LoggingEngine;
import rvpredict.runtime.RVPredictRuntime;
import rvpredict.util.Logger;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class Agent implements ClassFileTransformer {

    /**
     * Packages/classes that need to be excluded from instrumentation. These are
     * not configurable by the users because including them for instrumentation
     * almost certainly leads to crash.
     */
    private static List<Pattern> IGNORES = new LinkedList<>();
    static {
        String [] ignores = new String[] {
                // rv-predict itself and the libraries we are using
                "rvpredict",
                /* TODO(YilongL): shall we repackage these libraries using JarJar? */
                "org.objectweb.asm",
                "com.beust",
                "org.apache.tools.ant",

                // array type
                "[",

                // JDK classes used by the RV-Predict runtime library
                "java.io",
                "java.nio",
                "java.util.concurrent.atomic.AtomicLong",
                "java.util.concurrent.ConcurrentHashMap",
                "java.util.zip.GZIPOutputStream",
                "java.util.regex",

                // Basics of the JDK that everything else is depending on
                "sun",
                "java.lang",

                /* we provide complete mocking of the jucl package */
                "java.util.concurrent.locks"
        };
        for (String ignore : ignores) {
            IGNORES.add(Configuration.createRegEx(ignore));
        }
    }
    
    /**
     * Packages/classes that are excluded from instrumentation by default. These are
     * configurable by the users through the <code>--exclude</code> command option.
     */
     private static String[] DEFAULT_EXCLUDES = new String[] {
            "java.*",
            "javax.*",
            "sun.*",
            "sunw.*",
            "com.sun.*",
            "com.ibm.*",
            "com.apple.*",
            "apple.awt.*",
            "org.xml.*",
            "jdk.internal.*"            
    };

    private static String[] MOCKS = new String[] {
        "java/util/Collection",
        "java/util/Map"
    };

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
        String excludes = Configuration.excludes;
        if (excludes == null) {
            config.excludeList = getDefaultExcludes();
        } else {
            excludes = excludes.trim();
            if (excludes.charAt(0) == '+') { // initialize excludeList with default patterns
                excludes = excludes.substring(1);
                config.excludeList = getDefaultExcludes();
            }
            for (String exclude : excludes.replace('.', '/').split(",")) {
                exclude = exclude.trim();
                if (!exclude.isEmpty())
                    config.excludeList.add(Configuration.createRegEx(exclude));
            }
            System.out.println("Excluding: " + config.excludeList);
        }
        if (Configuration.includes != null) {
            for (String include : Configuration.includes.replace('.', '/').split(",")) {
                if (include.isEmpty()) continue;
                config.includeList.add(Configuration.createRegEx(include));
            }
            System.out.println("Including: " + config.includeList);
        }

        TraceCache.removeTraceFiles(config.outdir);
        final LoggingEngine db = new LoggingEngine(config);
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
        Thread predict = Main.getPredictionThread(config, cleanupAgent, config.predict);
        Runtime.getRuntime().addShutdownHook(predict);

        if (config.predict) {
            if (logOutput) {
                config.logger.report(
                        Main.center(Configuration.INSTRUMENTED_EXECUTION_TO_RECORD_THE_TRACE),
                        Logger.MSGTYPE.INFO);
            }
        }
    }

    private static List<Pattern> getDefaultExcludes() {
        // Initialize excludeList with default values
        List<Pattern> excludeList = new LinkedList<>();
        for (String exclude : DEFAULT_EXCLUDES) {
            excludeList.add(Configuration.createRegEx(exclude));
        }
        return excludeList;
    }

    private static final Set<String> loadedClasses = new HashSet<>();

    @Override
    public byte[] transform(ClassLoader loader, String cname, Class<?> c, ProtectionDomain d,
            byte[] cbuf) throws IllegalClassFormatException {
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
        for (Pattern exclude : config.excludeList) {
            toInstrument = !exclude.matcher(cname).matches();
            if (!toInstrument) break;
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

        if (!toInstrument) {
        /* include list overrides the above */
            for (Pattern include : config.includeList) {
                toInstrument = include.matcher(cname).matches();
                if (toInstrument) break;
            }
        }

        if (toInstrument) {  //make sure we don't instrument IGNORES even if the user said so
            //        System.err.println(cname + " " + toInstrument);
            for (Pattern ignore : IGNORES) {
                toInstrument = !ignore.matcher(cname).matches();
                if (!toInstrument) break;
            }
        }
        
        if (toInstrument) {
            ClassReader cr = new ClassReader(cbuf);

            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
            ClassVisitor cv = new ClassTransformer(cw, config);
            ClassVisitor checker = new CheckClassAdapter(cv);
            try {
                cr.accept(checker, 0);
            } catch (Throwable e) {
                /* exceptions during class loading are silently suppressed by default */
                System.err.println("Cannot retransform " + cname + ". Exception: " + e);
                throw e;
            }

            return cw.toByteArray();
        }
        // no transformation happens
        return null;
    }
}
