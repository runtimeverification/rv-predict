package com.runtimeverification.rvpredict.engine.main;

import com.runtimeverification.rvpredict.config.Configuration;

import org.apache.tools.ant.util.JavaEnvUtils;

import com.runtimeverification.rvpredict.log.OfflineLoggingFactory;
import com.runtimeverification.rvpredict.util.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author TraianSF
 */
public class Main {

    private static final String JAVA_EXECUTABLE = JavaEnvUtils.getJreExecutable("java");
    private static final String SEPARATOR = System.getProperty("file.separator");
    private static final String RV_PREDICT_JAR = Configuration.getBasePath() + SEPARATOR + "lib"
            + SEPARATOR + "rv-predict.jar";

    private static Configuration config;

    public static void main(String[] args) {
        config = Configuration.instance(args, false);

        if (config.log) {
            if (config.command_line.isEmpty()) {
                config.logger.report("You must provide a class or a jar to run.",
                        Logger.MSGTYPE.ERROR);
                config.usage();
                System.exit(1);
            }
            File outdirFile = new File(config.outdir);
            if (!(outdirFile.exists())) {
                outdirFile.mkdir();
            } else {
                if (!outdirFile.isDirectory()) {
                    config.logger.report(config.outdir + " is not a directory",
                            Logger.MSGTYPE.ERROR);
                    config.usage();
                    System.exit(1);
                }
            }

            execApplication();
            config.logger.reportPhase(Configuration.LOGGING_PHASE_COMPLETED);
        }

        if (config.predictAlgo.isOffline()) {
            new RVPredict(config, new OfflineLoggingFactory(config)).run();
        }
    }

    /**
     * Executes the application in a subprocess.
     */
    private static void execApplication() {
        List<String> args = new ArrayList<>();
        args.add(JAVA_EXECUTABLE);
        args.add("-ea");
        args.add("-Xbootclasspath/a:" + RV_PREDICT_JAR);
        args.add("-javaagent:" + RV_PREDICT_JAR + "=" + getAgentArgs());
        args.addAll(config.command_line);

        Process process = null;
        try {
            process = new ProcessBuilder(args).start();
            StreamRedirector.redirect(process);
            process.waitFor();
        } catch (IOException ignored) {
        } catch (InterruptedException e) {
            if (process != null) {
                process.destroy();
            }
            e.printStackTrace();
        }
    }

    /**
     * Formats the RV-Predict specific options from the command line
     * as a string of options which can be passed to the -javaagent
     * JVM option.
     *
     * It basically iterates through all arguments and builds a string out of them
     * taking care to wrap any argument containing spaces using
     * {@link #escapeString(String)}.
     *
     * As the default behavior of the agent is to run prediction upon completing
     * logging, the {@code --log} option must be passed to the agent.  Thus, if
     * the {@code --dir} option was used by the user, it would be replaced by
     * {@code --log}.  If neither  {@code --dir} nor {@code --log} were used, then
     * the {@code --log} option is added to make sure execution is logged in the
     * directory expected by prediction.
     *
     * @return the -javaagent options corresponding to the user command line
     */
    private static String getAgentArgs() {
        boolean hasLogDir = false;
        StringBuilder agentOptions = new StringBuilder();
        for (String arg : config.getRvArgs()) {
            if (arg.equals(Configuration.opt_outdir)) {
                arg = Configuration.opt_only_log;
            }
            if (arg.equals(Configuration.opt_only_log)) {
                hasLogDir = true;
            }
            agentOptions.append(escapeString(arg)).append(" ");
        }
        if (!hasLogDir) {
            agentOptions.insert(0, Configuration.opt_only_log + " " + escapeString(config.outdir) + " ");
        }
        return agentOptions.toString();
    }

    private static String escapeString(String s) {
        return (s.contains(" ") ? "\\\"" + s + "\\\"" : s);
    }

}
