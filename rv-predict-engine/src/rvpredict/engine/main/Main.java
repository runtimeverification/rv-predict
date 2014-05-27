package rvpredict.engine.main;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
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
        JCommander jc = new JCommander(config);
        jc.setProgramName(Configuration.PROGRAM_NAME);

        // Collect all parameter names.  It would be nice if JCommander provided this directly.
        Set<String> options = new HashSet<String>();
        for (ParameterDescription parameterDescription : jc.getParameters()) {
            for (String name : parameterDescription.getParameter().names()) {
                options.add(name);
            }
        }

        // remove the _opt option added by the rv-predict script to all options
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-") && args[i].endsWith("_opt")) {
                args[i] = args[i].substring(0, args[i].length() - 4);
            }
        }

       // Detecting where program options start
        int max;
        for(max = 0; max < args.length; max++) {
           if (args[max].startsWith("-")  && !options.contains(args[max]))
               break; // the index of the first unknown command
        }

        // get all rv-predict & java arguments and (potentially) the first unnamed program arguments
        String[] rvArgs = Arrays.copyOf(args, max);
         // program specific options starting with the first named (and unknown) one
        String[] pgmArgs = Arrays.copyOfRange(args, max, args.length);

        config.parseArguments(rvArgs, jc);

        if (!config.agent && ! config.predict) {
            config.agent = config.predict = true;
        }

        DBEngine db;
        if (config.agent) {
            db = new DBEngine(config.appname);
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
            String classpath = "";
            if (config.javaOptions.appClassPath != null) {
                classpath = config.javaOptions.appClassPath;
            }
            classpath = rvAgent + System.getProperty("path.separator") + classpath;
            List<String> appArgList = new ArrayList<String>();
            appArgList.add(java);
            appArgList.add("-cp");
            appArgList.add(classpath);
            appArgList.add("-javaagent:" + iagent);
            appArgList.addAll(config.command_line);
            for (String appArg : pgmArgs) appArgList.add(appArg);

            ProcessBuilder processBuilder =
                    new ProcessBuilder(appArgList.toArray(new String[appArgList.size()]));
            processBuilder.inheritIO();
            try {
                if (config.verbose) {
                    System.out.println("Started logging " + config.appname + ".");
                }
                Process process = processBuilder.start();
                process.waitFor();
            } catch (IOException e) {
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        db = new DBEngine(config.appname);
        try {
            if (!db.checkTables()) {
                System.err.print("Trace was not recorded properly. ");
                if (config.agent) {
                    System.err.println("Please check the classpath.");
                } else {
                    System.err.println("Please run " + Configuration.PROGRAM_NAME + " with the " + Configuration.opt_only_log +
                            "option enabled.");
                }
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Unexpected database error while checking the trace was recorded.");
            System.err.println(e.getMessage());
            System.exit(1);
        } finally {
            db.closeDB();
        }

        if (config.agent && config.verbose) {
            System.out.println("\nDone logging " + config.appname + ".");
        }

        if (config.predict) {

            NewRVPredict.run(config);
        }
    }


    private static String getBasePath() {
        String path = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath();
//        if (!path.endsWith(".jar"))
//            path = new File(path) //bin
//                    .getParentFile() //src
//                    .getAbsolutePath() + "/";
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
