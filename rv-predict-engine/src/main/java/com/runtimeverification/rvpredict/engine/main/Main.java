package com.runtimeverification.rvpredict.engine.main;

import com.runtimeverification.rvpredict.config.Configuration;

import org.apache.tools.ant.util.JavaEnvUtils;

import com.runtimeverification.rvpredict.log.OfflineLoggingFactory;
import com.runtimeverification.rvpredict.util.Logger;
import com.runtimeverification.rvpredict.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author TraianSF
 */
public class Main {

    public static final int WIDTH = 75;
    public static final char FILL = '-';

    public static void main(String[] args) {

        Configuration config = new Configuration();

        config.parseArguments(args, false);
        boolean logOutput = config.log_output.equalsIgnoreCase(Configuration.YES);

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

            String java = org.apache.tools.ant.util.JavaEnvUtils.getJreExecutable("java");
            String basePath = Configuration.getBasePath();
            String separator = System.getProperty("file.separator");
            String libPath = basePath + separator + "lib" + separator;
            String rvAgent = libPath + "rv-predict" + ".jar";

            String agentOptions = getAgentOptions(config);

            List<String> appArgList = new ArrayList<>();
            appArgList.add(java);
            appArgList.add("-ea");
            appArgList.add("-Xbootclasspath/a:" + rvAgent);
            appArgList.add("-javaagent:" + rvAgent + "=" + agentOptions);
            if (logOutput) {
                config.logger.report(
                        center(Configuration.INSTRUMENTED_EXECUTION_TO_RECORD_THE_TRACE),
                        Logger.MSGTYPE.INFO);
            }
            appArgList.addAll(config.command_line);

            runAgent(config, appArgList, false);
        }

        try {
            checkAndPredict(config);
        } catch (IOException e) {
            System.err.println("Error while reading the logs.");
            System.err.println(e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("Error: Metadata file corrupted.");
            System.err.println(e.getMessage());
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
     * @param config
     * @return the -javaagent options corresponding to the user command line
     */
    private static String getAgentOptions(Configuration config) {
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

    private static void checkAndPredict(Configuration config) throws IOException, ClassNotFoundException {
        boolean logOutput = config.log_output.equalsIgnoreCase(Configuration.YES);

        if (config.log && (Configuration.verbose || logOutput)) {
            config.logger
                    .report(center(Configuration.LOGGING_PHASE_COMPLETED), Logger.MSGTYPE.INFO);
            config.logger.report(Configuration.TRACE_LOGGED_IN + config.outdir,
                    Logger.MSGTYPE.VERBOSE);
        }

        if (config.predict && !Configuration.online) {
            new RVPredict(config, new OfflineLoggingFactory(config)).run();
        }
    }

    public static String center(String msg) {
        return Util.center(msg, WIDTH, FILL);
    }

    public static Thread getPredictionThread(final Configuration commandLine,
            final CleanupAgent cleanupAgent, final boolean predict) {
        String[] args = commandLine.getArgs();
        final boolean logOutput = commandLine.log_output.equalsIgnoreCase(Configuration.YES);
        ProcessBuilder processBuilder = null;
        boolean logToScreen = false;
        String file = null;
        if (predict) {
            String java = JavaEnvUtils.getJreExecutable("java");
            String basePath = Configuration.getBasePath();
            String separator = System.getProperty("file.separator");
            String libPath = basePath + separator + "lib" + separator;
            String rvEngine = libPath + "rv-predict" + ".jar";
            List<String> appArgList = new ArrayList<>();
            appArgList.add(java);
            appArgList.add("-cp");
            appArgList.add(rvEngine);
            appArgList.add(Main.class.getName());
            int rvIndex = appArgList.size();
            appArgList.addAll(Arrays.asList(args));

            int index = appArgList.indexOf(Configuration.opt_outdir);
            if (index != -1) {
                appArgList.set(index, Configuration.opt_only_predict);
            } else {
                appArgList.add(rvIndex, Configuration.opt_only_predict);
                appArgList.add(rvIndex+1, commandLine.outdir);
            }

            processBuilder = new ProcessBuilder(appArgList.toArray(args));
            String logOutputString = commandLine.log_output;
            if (logOutputString.equalsIgnoreCase(Configuration.YES)) {
                logToScreen = true;
            } else if (!logOutputString.equals(Configuration.NO)) {
                file = logOutputString;
                String actualOutFile = file + ".out";
                String actualErrFile = file + ".err";
                processBuilder.redirectError(new File(actualErrFile));
                processBuilder.redirectOutput(new File(actualOutFile));
            }
            StringBuilder commandMsg = new StringBuilder();
            commandMsg.append("Executing command: \n");
            commandMsg.append("   ");
            for (String arg : args) {
                if (arg.contains(" ")) {
                    commandMsg.append(" \"").append(arg).append("\"");
                } else {
                    commandMsg.append(" ").append(arg);
                }
            }
            commandLine.logger.report(commandMsg.toString(), Logger.MSGTYPE.VERBOSE);
        }

        final boolean finalLogToScreen = logToScreen;
        final String finalFile = file;
        final ProcessBuilder finalProcessBuilder = processBuilder;
        return new Thread("CleanUp Agent") {
            @Override
            public void run() {
                cleanupAgent.cleanup();
                if (predict) {
                    if (commandLine.log && (Configuration.verbose || logOutput)) {
                        commandLine.logger.report(center(Configuration.LOGGING_PHASE_COMPLETED),
                                Logger.MSGTYPE.INFO);
                        commandLine.logger.report(Configuration.TRACE_LOGGED_IN
                                + commandLine.outdir, Logger.MSGTYPE.VERBOSE);
                    }

                    try {
                        Process process = finalProcessBuilder.start();
                        if (finalLogToScreen) {
                            Util.redirectOutput(process.getErrorStream(), System.err);
                            Util.redirectOutput(process.getInputStream(), System.out);
                        } else if (finalFile == null) {
                            Util.redirectOutput(process.getErrorStream(), null);
                            Util.redirectOutput(process.getInputStream(), null);
                        }
                        Util.redirectInput(process.getOutputStream(), System.in);

                        process.waitFor();
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    public interface CleanupAgent {
        public void cleanup();
    }

    public static void runAgent(final Configuration config, final List<String> appArgList,
            final boolean finalRun) {
        ProcessBuilder processBuilder = new ProcessBuilder(appArgList.toArray(new String[appArgList
                .size()]));
        String logOutputString = config.log_output;
        boolean logToScreen = false;
        String file = null;
        if (logOutputString.equalsIgnoreCase(Configuration.YES)) {
            logToScreen = true;
        } else if (!logOutputString.equals(Configuration.NO)) {
            file = logOutputString;
            String actualOutFile = file + ".out";
            String actualErrFile = file + ".err";
            processBuilder.redirectError(new File(actualErrFile));
            processBuilder.redirectOutput(new File(actualOutFile));
        }
        try {
            final StringBuilder commandMsg = new StringBuilder();
            commandMsg.append("Executing command: \n");
            commandMsg.append("   ");
            for (String arg : appArgList) {
                if (arg.contains(" ")) {
                    commandMsg.append(" \"").append(arg).append("\"");
                } else {
                    commandMsg.append(" ").append(arg);
                }
            }
            config.logger.report(commandMsg.toString(), Logger.MSGTYPE.VERBOSE);
            final Process process = processBuilder.start();
            Thread cleanupAgent = new Thread() {
                @Override
                public void run() {
                    process.destroy();
                    if (finalRun) {
                        config.logger.report("Warning: Logging interrupted by user. \n"
                                + "Please run the following command to resume prediction:"
                                + commandMsg.toString(), Logger.MSGTYPE.INFO);
                    }
                }
            };
            Runtime.getRuntime().addShutdownHook(cleanupAgent);
            if (logToScreen) {
                Util.redirectOutput(process.getErrorStream(), System.err);
                Util.redirectOutput(process.getInputStream(), System.out);
            } else if (file == null) {
                Util.redirectOutput(process.getErrorStream(), null);
                Util.redirectOutput(process.getInputStream(), null);
            }
            Util.redirectInput(process.getOutputStream(), System.in);

            process.waitFor();
            Runtime.getRuntime().removeShutdownHook(cleanupAgent);
        } catch (IOException ignored) {
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String escapeString(String s) {
        return (s.contains(" ") ? "\\\"" + s + "\\\"" : s);
    }

}