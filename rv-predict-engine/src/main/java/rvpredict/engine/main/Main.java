package rvpredict.engine.main;

import config.Configuration;
import config.Util;
import db.DBEngine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

/**
 * @author TraianSF
 */
public class Main {

    public static final int WIDTH = 75;
    public static final char FILL = '-';
    public static final String GROUP_ID = "com.runtimeverification.rvpredict";
    public static final String ARTEFACT_ID = "rv-predict-engine";

    public static void main(String[] args) {

        Configuration config = new Configuration();

        config.parseArguments(args);
        boolean logOutput = config.log_output.equalsIgnoreCase(Configuration.YES);

        DBEngine db;
        if (config.log) {
            if (config.command_line.isEmpty()) {
                System.err.println("You must provide a class or a jar to run.");
                System.exit(1);
            }
            File outdirFile = new File(config.outdir);
            if(!(outdirFile.exists())) {
                outdirFile.mkdir();
            } else {
                if (!outdirFile.isDirectory()) {
                    System.err.println(config.outdir + " is not a directory");
                    System.exit(1);
                }
            }
            db = new DBEngine(config.outdir, config.tableName);
            try {
                db.dropAll();
            } catch (Exception e) {
                System.err.println("Unexpected error while cleaning up the database:");
                System.err.println(e.getMessage());
                System.exit(1);
            }
            db.closeDB();

            String java = org.apache.tools.ant.util.JavaEnvUtils.getJreExecutable("java");
            String basePath = getBasePath();
            String separator = System.getProperty("file.separator");
            String libPath = basePath + separator + "lib" + separator;
            String version = getVersion();
            String rvAgent = libPath + "rv-predict-agent" + (version == null ? "" : "-" + version) + ".jar";
            String sharingAgentOptions = config.opt_outdir + " " + escapeString(config.outdir);
            if (config.additionalExcludes != null) {
                sharingAgentOptions += " " + Configuration.opt_exclude + " " + escapeString(config.additionalExcludes);
            }
            sharingAgentOptions += " " + config.opt_table_name + " " + escapeString(config.tableName);
            String noSharingAgentOptions = sharingAgentOptions;
            sharingAgentOptions += " " + config.opt_sharing_only;

            List<String> appArgList = new ArrayList<String>();
            appArgList.add(java);
//            appArgList.add("-Xbootclasspath/a:" + rvAgent);
            int agentIds = appArgList.size();
            if (config.optlog || config.agentOnlySharing) {
                if (logOutput) {
                    if (config.optlog) {
                        System.out.println(center("First pass: Instrumented execution to detect shared variables"));
                    } else {
                        System.out.println(center("Instrumented execution to detect shared variables"));

                    }
                }
                appArgList.add("-javaagent:" + rvAgent + "=" + sharingAgentOptions);
            } else {
                appArgList.add("-javaagent:" + rvAgent + "=" + noSharingAgentOptions);
                if (logOutput) {
                    System.out.println(center("Instrumented execution to record the trace"));
                }
            }
            appArgList.addAll(config.command_line);

            runAgent(config, appArgList);
            if (config.optlog) {
                appArgList.set(agentIds, "-javaagent:" + rvAgent + "=" + noSharingAgentOptions);
                if (logOutput) {
                    System.out.println(center("Second pass: Instrumented execution to record the trace"));

                }
                runAgent(config, appArgList);
            }
        }

        db = new DBEngine(config.outdir, config.tableName);
        try {
            if (! db.checkTables()) {
                System.err.print("Trace was not recorded properly. ");
                if (config.log) {
                    System.err.println("Please check the classpath.");
                } else {
                    System.err.println("Please run " + Configuration.PROGRAM_NAME + " with " + Configuration.opt_only_log +
                            " " + config.outdir + " first.");
                }
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Unexpected database error while checking whether the trace was recorded.");
            System.err.println(e.getMessage());
            System.exit(1);
        } finally {
            db.closeDB();
        }

        if (config.log && (config.verbose || logOutput)) {
            System.out.println(center("Logging phase completed."));
            if (config.verbose) {
                System.out.println("\tTrace logged in: " + config.outdir);
            }
        }

        if (config.predict) {
            NewRVPredict.run(config);
        }
    }

    private static String getVersion() {
        String version = null;
        try {
            Properties p = new Properties();
            InputStream is = Main.class.getResourceAsStream("/META-INF/maven/" + GROUP_ID + "/" + ARTEFACT_ID +
                    "/pom.properties");
            if (is != null) {
                p.load(is);
                version = p.getProperty("version", null);
            }
        } catch (Exception e) {
            // ignore
        }
        return version;
    }

    public static String center(String msg) {
        return Util.center(msg, WIDTH, FILL);
    }

    public static void runAgent(Configuration config, List<String> appArgList) {
        ProcessBuilder processBuilder =
                new ProcessBuilder(appArgList.toArray(new String[appArgList.size()]));
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
            if (config.verbose) {
                System.out.println("Executing command: ");
                System.out.print("   ");
                for (String arg : appArgList) {
                    if (arg.contains(" ")) {
                        System.out.print(" \"" + arg + "\"");
                    } else {
                        System.out.print(" " + arg);
                    }
                }
                System.out.println();
            }
            Process process = processBuilder.start();
            if (logToScreen) {
                Util.redirectOutput(process.getErrorStream(), System.err);
                Util.redirectOutput(process.getInputStream(), System.out);
            } else if (file == null) {
                Util.redirectOutput(process.getErrorStream(), null);
                Util.redirectOutput(process.getInputStream(), null);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0 && config.failOnFail) {
                System.err.println("Error: Logging phase returned non-zero exit code " + exitCode);
                System.exit(exitCode);
            }
        } catch (IOException e) {
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String escapeString(String s) {
        return (s.contains(" ") ? "\\\"" + s + "\\\"" : s);
    }

    public static String getBasePath() {
        String path = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath();
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
