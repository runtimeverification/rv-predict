package com.runtimeverification.rvpredict.instrumentation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.metadata.ClassFile;

public class InstrumentUtils implements Opcodes {

    public static final Type OBJECT_TYPE    = Type.getObjectType("java/lang/Object");
    public static final Type CLASS_TYPE     = Type.getObjectType("java/lang/Class");
    public static final Type JL_FLOAT_TYPE  = Type.getObjectType("java/lang/Float");
    public static final Type JL_DOUBLE_TYPE = Type.getObjectType("java/lang/Double");
    public static final Type JL_SYSTEM_TYPE = Type.getObjectType("java/lang/System");
    public static final Type RVPREDICT_RUNTIME_TYPE = Type
            .getObjectType("com/runtimeverification/rvpredict/runtime/RVPredictRuntime");

    public static void printTransformedClassToFile(String cname, byte[] cbuf, String dir) {
        String fileName = dir + "/" + cname.substring(cname.lastIndexOf("/") + 1) + ".class";
        File f = new File(fileName);

        try {
            OutputStream out = new FileOutputStream(f);
            out.write(cbuf);
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Boolean> instrumentClass = new ConcurrentHashMap<>();

    /**
     * Checks if we should instrument a class or interface.
     *
     * @param classFile
     *
     * @return {@code true} if we should instrument it; otherwise, {@code false}
     */
    public static boolean needToInstrument(ClassFile classFile) {
        String cname = classFile.getClassName();
        ClassLoader loader = classFile.getLoader();

        Boolean toInstrument = instrumentClass.get(cname);
        if (toInstrument != null) {
            return toInstrument;
        }

        toInstrument = true;
        for (Pattern exclude : Agent.config.excludeList) {
            toInstrument = !exclude.matcher(cname).matches();
            if (!toInstrument) break;
        }

        if (toInstrument) {
            for (String mock : Configuration.MOCKS) {
                if (ClassFile.isSubtypeOf(loader, cname, mock)) {
                    toInstrument = false;
                    if (Configuration.verbose) {
                        /* TODO(YilongL): this may cause missing data races if
                         * the mock for interface/superclass does not contain
                         * methods specific to this implementation. This could
                         * be a big problem if the application makes heavy use
                         * of helper methods specific in some high-level
                         * concurrency library (e.g. Guava) while most of the
                         * classes are simply excluded here */
                        System.err.println("[Java-agent] excluded " + cname
                                + " from instrumentation because we are mocking " + mock);
                    }
                    break;
                }
            }
        }

        if (!toInstrument) {
            /* include list overrides the above */
            for (Pattern include : Agent.config.includeList) {
                toInstrument = include.matcher(cname).matches();
                if (toInstrument) break;
            }
        }

        /* make sure we don't instrument IGNORES even if the user said so */
        if (toInstrument) {
            for (Pattern ignore : Configuration.IGNORES) {
                toInstrument = !ignore.matcher(cname).matches();
                if (!toInstrument) break;
            }
        }

        /* the only exception to the IGNORES list */
        if (!toInstrument) {
            for (Pattern mustInclude : Configuration.MUST_INCLUDES) {
                if (mustInclude.matcher(cname).matches()) {
                    toInstrument = true;
                    break;
                }
            }
        }

        instrumentClass.put(cname, toInstrument);
        return toInstrument;
    }

}
