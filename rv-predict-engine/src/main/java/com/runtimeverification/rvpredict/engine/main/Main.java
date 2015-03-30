package com.runtimeverification.rvpredict.engine.main;

import com.runtimeverification.rvpredict.config.Configuration;

import org.apache.tools.ant.util.JavaEnvUtils;

import com.runtimeverification.rvpredict.log.LoggingEngine;
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

    private static final String JAVA_EXECUTABLE = JavaEnvUtils.getJreExecutable("java");
    private static final String SEPARATOR = System.getProperty("file.separator");
    private static final String RV_PREDICT_JAR = Configuration.getBasePath() + SEPARATOR + "lib"
            + SEPARATOR + "rv-predict.jar";

    public static void main(String[] args) {
        Configuration config = new Configuration();
        config.parseArguments(args, false);

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

            String agentOptions = getAgentOptions(config);

            List<String> appArgList = new ArrayList<>();
            appArgList.add(JAVA_EXECUTABLE);
            appArgList.add("-ea");
            appArgList.add("-Xbootclasspath/a:" + RV_PREDICT_JAR);
            appArgList.add("-javaagent:" + RV_PREDICT_JAR + "=" + agentOptions);
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
        if (config.log) {
            config.logger.reportPhase(Configuration.LOGGING_PHASE_COMPLETED);
        }

        if (config.predict) {
            new RVPredict(config, new OfflineLoggingFactory(config)).run();
        }
    }

    public static Thread getPredictionThread(final Configuration commandLine, LoggingEngine loggingEngine) {
        String[] args = commandLine.getArgs();
        ProcessBuilder processBuilder = null;
        if (commandLine.predict) {
            List<String> appArgList = new ArrayList<>();
            appArgList.add(JAVA_EXECUTABLE);
            appArgList.add("-cp");
            appArgList.add(RV_PREDICT_JAR);
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
        }

        final ProcessBuilder finalProcessBuilder = processBuilder;
        return new Thread("CleanUp Agent") {
            @Override
            public void run() {
                try {
                    loggingEngine.finishLogging();
                } catch (IOException e) {
                    System.err.println("Warning: I/O Error while logging the execution. The log might be unreadable.");
                    System.err.println(e.getMessage());
                } catch (InterruptedException e) {
                    System.err.println("Warning: Execution is being forcefully ended. Log data might be lost.");
                    System.err.println(e.getMessage());
                }

                if (commandLine.predict) {
                    if (commandLine.log) {
                        commandLine.logger.reportPhase(Configuration.LOGGING_PHASE_COMPLETED);
                    }

                    try {
                        Process process = finalProcessBuilder.start();
                        redirectOutput(process.getErrorStream(), System.err);
                        redirectOutput(process.getInputStream(), System.out);
                        redirectInput(process.getOutputStream(), System.in);

                        process.waitFor();
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    public static void runAgent(final Configuration config, final List<String> appArgList) {
        ProcessBuilder agentProcBuilder = new ProcessBuilder(appArgList.toArray(new String[appArgList
                .size()]));
        try {
            final Process agentProc = agentProcBuilder.start();
            Thread cleanupAgent = new Thread() {
                @Override
                public void run() {
                    agentProc.destroy();
                }
            };
            Runtime.getRuntime().addShutdownHook(cleanupAgent);
            redirectOutput(agentProc.getErrorStream(), System.err);
            redirectOutput(agentProc.getInputStream(), System.out);
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
