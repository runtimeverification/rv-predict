package rvpredict.engine.main;

import config.Configuration;
import db.DBEngine;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

/**
 * @author TraianSF
 */
public class Main {
    public static void main(String[] args) {

        Configuration config = new Configuration();

         config.parseArguments(args);
        if (config.command_line.isEmpty()) {
            System.err.println("You must provide a class or a jar to run.");
            System.exit(1);
        }

        if (!config.agent && ! config.predict) {
            config.agent = config.predict = true;
        }

        DBEngine db;
        if (config.agent) {
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
            String iagent = libPath + "iagent.jar";
            String rvAgent = libPath + "rv-predict-agent.jar";
            String sharingAgentOptions = config.opt_outdir + " " + escapeString(config.outdir);
            sharingAgentOptions += " " + config.opt_table_name + " " + escapeString(config.tableName);
            String noSharingAgentOptions = sharingAgentOptions;
            sharingAgentOptions += " " + config.opt_sharing_only;

            List<String> appArgList = new ArrayList<String>();
            appArgList.add(java);
            int agentIds = appArgList.size();
            appArgList.add("-Xbootclasspath/a:" + rvAgent);
            if (config.optlog || config.agentOnlySharing) {
                appArgList.add("-javaagent:" + iagent + "=" + sharingAgentOptions);
            } else {
                appArgList.add("-javaagent:" + iagent + "=" + noSharingAgentOptions);
            }
            appArgList.addAll(config.command_line);

            runAgent(config, appArgList);
            if (config.optlog) {
                appArgList.set(agentIds, "-javaagent:" + iagent + "=" + noSharingAgentOptions);
                runAgent(config, appArgList);
            }
        }

        db = new DBEngine(config.outdir, config.tableName);
        try {
            if (! db.checkTables()) {
                System.err.print("Trace was not recorded properly. ");
                if (config.agent) {
                    System.err.println("Please check the classpath.");
                } else {
                    System.err.println("Please run " + Configuration.PROGRAM_NAME + " with the " + Configuration.opt_only_log +
                            " option enabled.");
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

        if (config.agent && config.verbose) {
            System.out.println("\nDone executing and logging.");
        }

        if (config.predict) {
            NewRVPredict.run(config);
        }
    }

    public static void runAgent(Configuration config, List<String> appArgList) {
        ProcessBuilder processBuilder =
                new ProcessBuilder(appArgList.toArray(new String[appArgList.size()]));
        processBuilder.inheritIO();
        try {
            if (config.verbose) {
                System.out.println("Executing and logging command: ");
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
            process.waitFor();
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
