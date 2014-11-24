package rvpredict.engine.main;

import org.apache.tools.ant.util.JavaEnvUtils;
import rvpredict.config.Configuration;
import rvpredict.config.Util;
import rvpredict.db.DBEngine;
import rvpredict.util.Logger;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.util.*;

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
            String basePath = getBasePath();
            String separator = System.getProperty("file.separator");
            String libPath = basePath + separator + "lib" + separator;
            String rvAgent = libPath + "rv-predict" + ".jar";

            // TODO(Traian): there should be only one agentOptions
            String sharingAgentOptions = Configuration.opt_only_log + " "
                    + escapeString(config.outdir);
            if (Configuration.additionalExcludes != null) {
                Configuration.additionalExcludes.replaceAll(" ", "");
                sharingAgentOptions += " " + Configuration.opt_exclude + " "
                        + escapeString(Configuration.additionalExcludes);
            }
            if (Configuration.additionalIncludes != null) {
                Configuration.additionalIncludes.replaceAll(" ", "");
                sharingAgentOptions += " " + Configuration.opt_include + " "
                        + escapeString(Configuration.additionalIncludes);
            }
            String noSharingAgentOptions = sharingAgentOptions;

            List<String> appArgList = new ArrayList<String>();
            appArgList.add(java);
            appArgList.add("-Xbootclasspath/a:" + rvAgent);
            int agentIds = appArgList.size();
            if (config.optlog) {
                if (logOutput) {
                    if (config.optlog) {
                        config.logger
                                .report(center("First pass: Instrumented execution to detect shared variables"),
                                        Logger.MSGTYPE.INFO);
                    } else {
                        config.logger.report(
                                center("Instrumented execution to detect shared variables"),
                                Logger.MSGTYPE.INFO);

                    }
                }
                appArgList.add("-javaagent:" + rvAgent + "=" + sharingAgentOptions);
            } else {
                appArgList.add("-javaagent:" + rvAgent + "=" + noSharingAgentOptions);
                if (logOutput) {
                    config.logger.report(
                            center(Configuration.INSTRUMENTED_EXECUTION_TO_RECORD_THE_TRACE),
                            Logger.MSGTYPE.INFO);
                }
            }
            appArgList.addAll(config.command_line);

            if (config.optlog) {
                runAgent(config, appArgList, false);
                appArgList.set(agentIds, "-javaagent:" + rvAgent + "=" + noSharingAgentOptions);
                if (logOutput) {
                    config.logger.report(
                            center("Second pass: Instrumented execution to record the trace"),
                            Logger.MSGTYPE.INFO);
                }
                runAgent(config, appArgList, false);
            } else {
                runAgent(config, appArgList, false);

            }
        }

        checkAndPredict(config);
    }

    private static void checkAndPredict(Configuration config) {
        boolean logOutput = config.log_output.equalsIgnoreCase(Configuration.YES);
        DBEngine db;
        db = new DBEngine(config.outdir, config.tableName);
        if (!db.checkLog()) {
            config.logger.report("Trace was not recorded properly. ", Logger.MSGTYPE.ERROR);
            if (config.log) {
                // config.logger.report("Please check the classpath.",
                // Logger.MSGTYPE.ERROR);
            } else {
                config.logger.report("Please run " + Configuration.PROGRAM_NAME + " with "
                        + Configuration.opt_only_log + " " + config.outdir + " first.",
                        Logger.MSGTYPE.ERROR);
            }
            db.closeDB();
            System.exit(1);
        }

        if (config.log && (config.verbose || logOutput)) {
            config.logger
                    .report(center(Configuration.LOGGING_PHASE_COMPLETED), Logger.MSGTYPE.INFO);
            config.logger.report(Configuration.TRACE_LOGGED_IN + config.outdir,
                    Logger.MSGTYPE.VERBOSE);
        }

        if (config.predict) {
            NewRVPredict predictor = new NewRVPredict();
            predictor.initPredict(config);
            predictor.addHooks();
            predictor.run();
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
            String basePath = getBasePath();
            String separator = System.getProperty("file.separator");
            String libPath = basePath + separator + "lib" + separator;
            String rvEngine = libPath + "rv-predict" + ".jar";
            List<String> appArgList = new ArrayList<>();
            appArgList.add(java);
            appArgList.add("-cp");
            appArgList.add(rvEngine);
            appArgList.add("rvpredict.engine.main.Main");
            appArgList.addAll(Arrays.asList(args));

            int index = appArgList.indexOf(Configuration.opt_outdir);
            if (index != -1) {
                appArgList.set(index, Configuration.opt_only_predict);
            } else {
                appArgList.add(Configuration.opt_only_predict);
                appArgList.add(commandLine.outdir);
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
                    commandMsg.append(" \"" + arg + "\"");
                } else {
                    commandMsg.append(" " + arg);
                }
            }
            commandLine.logger.report(commandMsg.toString(), Logger.MSGTYPE.VERBOSE);
        }

        final boolean finalLogToScreen = logToScreen;
        final String finalFile = file;
        final ProcessBuilder finalProcessBuilder = processBuilder;
        return new Thread() {
            @Override
            public void run() {
                cleanupAgent.cleanup();
                if (predict) {
                    if (commandLine.log && (commandLine.verbose || logOutput)) {
                        commandLine.logger.report(center(Configuration.LOGGING_PHASE_COMPLETED),
                                Logger.MSGTYPE.INFO);
                        commandLine.logger.report(Configuration.TRACE_LOGGED_IN
                                + commandLine.outdir, Logger.MSGTYPE.VERBOSE);
                    }

                    Process process = null;
                    try {
                        process = finalProcessBuilder.start();
                        if (finalLogToScreen) {
                            Util.redirectOutput(process.getErrorStream(), System.err);
                            Util.redirectOutput(process.getInputStream(), System.out);
                        } else if (finalFile == null) {
                            Util.redirectOutput(process.getErrorStream(), null);
                            Util.redirectOutput(process.getInputStream(), null);
                        }
                        Util.redirectInput(process.getOutputStream(), System.in);

                        process.waitFor();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
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
                    commandMsg.append(" \"" + arg + "\"");
                } else {
                    commandMsg.append(" " + arg);
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
        } catch (IOException e) {
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String escapeString(String s) {
        return (s.contains(" ") ? "\\\"" + s + "\\\"" : s);
    }

    public static String getBasePath() {
        CodeSource codeSource = Main.class.getProtectionDomain().getCodeSource();
        String path;
        if (codeSource == null) {
            path = ClassLoader.getSystemClassLoader()
                    .getResource(Main.class.getName().replace('.', '/') + ".class").toString();
            path = path.substring(path.indexOf("file:"), path.indexOf('!'));
            URL url = null;
            try {
                url = new URL(path);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            path = url.getPath();
        } else {
            path = codeSource.getLocation().getPath();
        }
        path = new File(path).getAbsolutePath();
        try {
            String decodedPath = URLDecoder.decode(path, "UTF-8");
            File parent = new File(decodedPath).getParentFile().getParentFile();
            return parent.getAbsolutePath();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

}
