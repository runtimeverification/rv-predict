package com.runtimeverification.rvpredict.engine.main;

import com.google.common.base.Strings;
import com.runtimeverification.rvpredict.config.Configuration;

import org.apache.tools.ant.util.JavaEnvUtils;

import com.runtimeverification.rvpredict.log.OfflineLoggingFactory;
import com.runtimeverification.rvpredict.util.Logger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * @author TraianSF
 */
public class Main {

    public static final int WIDTH = 75;
    public static final String DASH = "-";

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

            runAgent(config, appArgList);
        }
        checkAndPredict(config);
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

    private static void checkAndPredict(Configuration config) {
        boolean logOutput = config.log_output.equalsIgnoreCase(Configuration.YES);

        if (config.log && (Configuration.verbose || logOutput)) {
            config.logger
                    .report(center(Configuration.LOGGING_PHASE_COMPLETED), Logger.MSGTYPE.INFO);
            config.logger.report(Configuration.TRACE_LOGGED_IN + config.outdir,
                    Logger.MSGTYPE.VERBOSE);
        }

        if (config.predict) {
            new RVPredict(config, new OfflineLoggingFactory(config)).run();
        }
    }

    public static String center(String msg) {
        int fillWidth = WIDTH - msg.length();
        return "\n" + Strings.repeat(DASH, fillWidth / 2) + msg
                + Strings.repeat(DASH, (fillWidth + 1) / 2);
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
                            redirectOutput(process.getErrorStream(), System.err);
                            redirectOutput(process.getInputStream(), System.out);
                        } else if (finalFile == null) {
                            redirectOutput(process.getErrorStream(), null);
                            redirectOutput(process.getInputStream(), null);
                        }
                        redirectInput(process.getOutputStream(), System.in);

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

    public static void runAgent(final Configuration config, final List<String> appArgList) {
        ProcessBuilder agentProcBuilder = new ProcessBuilder(appArgList.toArray(new String[appArgList
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
            agentProcBuilder.redirectError(new File(actualErrFile));
            agentProcBuilder.redirectOutput(new File(actualOutFile));
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
            final Process agentProc = agentProcBuilder.start();
            Thread cleanupAgent = new Thread() {
                @Override
                public void run() {
                    agentProc.destroy();
                }
            };
            Runtime.getRuntime().addShutdownHook(cleanupAgent);
            if (logToScreen) {
                redirectOutput(agentProc.getErrorStream(), System.err);
                redirectOutput(agentProc.getInputStream(), System.out);
            } else if (file == null) {
                redirectOutput(agentProc.getErrorStream(), null);
                redirectOutput(agentProc.getInputStream(), null);
            }
            redirectInput(agentProc.getOutputStream(), System.in);

            agentProc.waitFor();
            Runtime.getRuntime().removeShutdownHook(cleanupAgent);
        } catch (IOException ignored) {
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String escapeString(String s) {
        return (s.contains(" ") ? "\\\"" + s + "\\\"" : s);
    }

    public static void redirectOutput(final InputStream outputStream, final PrintStream redirect) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Scanner scanner = new Scanner(outputStream);
                while (scanner.hasNextLine()) {
                    String s = scanner.nextLine();
                    if (redirect != null) {
                        redirect.println(s);
                    }
                }
                scanner.close();
            }
        }).start();
    }

    public static Thread redirectInput(final OutputStream inputStream, final InputStream redirect) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (redirect != null) {
                    try {
                        int ret = -1;
                        while ((ret = redirect.read()) != -1) {
                            inputStream.write(ret);
                            inputStream.flush();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

}
