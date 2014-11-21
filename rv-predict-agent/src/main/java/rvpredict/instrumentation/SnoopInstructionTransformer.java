package rvpredict.instrumentation;

import rvpredict.config.Configuration;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.CheckClassAdapter;

import rvpredict.config.Config;
import rvpredict.db.TraceCache;
import rvpredict.engine.main.Main;
import rvpredict.logging.DBEngine;
import rvpredict.logging.RecordRT;
import rvpredict.util.Logger;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Arrays;

public class SnoopInstructionTransformer implements ClassFileTransformer {

    private final Config config;
    private final GlobalStateForInstrumentation globalState;

    public SnoopInstructionTransformer(Config config, GlobalStateForInstrumentation globalState) {
        this.config = config;
        this.globalState = globalState;
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
        final GlobalStateForInstrumentation globalState = GlobalStateForInstrumentation.instance;
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
        final DBEngine db = new DBEngine(commandLine.outdir, commandLine.tableName, commandLine.async);
        if (commandLine.agentOnlySharing) {
            try {
                db.dropAll();
            } catch (Exception e) {
                commandLine.logger.report(
                        "Unexpected error while cleaning up the database:\n" + e.getMessage(),
                        Logger.MSGTYPE.ERROR);
                System.exit(1);
            }
        }
        // db.closeDB();
        // initialize RecordRT first
        RecordRT.init(db);

        inst.addTransformer(new SnoopInstructionTransformer(config, globalState));
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
        String[] tmp = config.excludeList;

        for (int i = 0; i < tmp.length; i++) {
            String s = tmp[i];
            if (cname.startsWith(s)) {
                toInstrument = false;
                break;
            }
        }
        tmp = config.includeList;
        if (tmp != null)
            for (int i = 0; i < tmp.length; i++) {
                String s = tmp[i];
                if (cname.startsWith(s)) {
                    toInstrument = true;
                    break;
                }
            }

        // TODO(YilongL): we need a more general mechanism
        // special handle java.io.File
        if (cname.equals("java/io/File"))
            toInstrument = true;

        if (toInstrument) {
            ClassReader cr = new ClassReader(cbuf);

            ClassWriter cw = new ClassWriter(cr, 0);
            ClassVisitor instrumentor = new SnoopInstructionClassAdapter(cw, config, globalState);
            CheckClassAdapter cv = new CheckClassAdapter(instrumentor);
            cr.accept(cv, 0);

            byte[] ret = cw.toByteArray();
            return ret;
        }
        return cbuf;
    }
}
