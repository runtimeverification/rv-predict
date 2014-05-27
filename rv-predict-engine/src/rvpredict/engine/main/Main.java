package rvpredict.engine.main;

import com.beust.jcommander.JCommander;
import config.Configuration;
import db.DBEngine;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author TraianSF
 */
public class Main {
    public static void main(String[] args) {


        Configuration config = new Configuration();
        int max = args.length;
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                args[i] = args[i].substring(0, args[i].length() - 4);
                if (max == args.length && !config.options.contains(args[i])) {
                    max = i;
                }
            }
        }
        System.out.println(config.options);
        String[] rvArgs = Arrays.copyOf(args, max);
        System.out.println(Arrays.toString(rvArgs));
        String[] pgmArgs = Arrays.copyOfRange(args, max, args.length);
        System.out.println(Arrays.toString(pgmArgs));

        JCommander jc = new JCommander(config, rvArgs);
        jc.setProgramName("rv-predict");

        if (config.help) {
            jc.usage();
            System.exit(0);
        }

        if (config.command_line == null) {
            System.out.println("Main class missing.");
            jc.usage();
            System.exit(1);
        }

        config.appname = config.command_line.get(0);


        DBEngine db = new DBEngine(config.appname);
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
        appArgList.add( "-javaagent:" + iagent);
        appArgList.addAll(config.command_line);
        for (String appArg : pgmArgs) appArgList.add(appArg);

        ProcessBuilder processBuilder =
                new ProcessBuilder(appArgList.toArray(new String[appArgList.size()]));
        processBuilder.inheritIO();
        try {
            System.out.println("Started logging " + config.appname + ".");
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException e) {
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        db = new DBEngine(config.appname);
        try {
            if (! db.checkTables()) {
                System.err.println("Trace was not recorded properly. Please check the classpath.");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Unexpected database error while recording the trace.");
            System.err.println(e.getMessage());
            System.exit(1);
        } finally {
            db.closeDB();
        }

        System.out.println("\nDone logging " + config.appname + ".");
        NewRVPredict.run(config);
    }

    private static String getBasePath() {
        String path = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath();
        if (!path.endsWith(".jar"))
            path = new File(path) //Main
                    .getParentFile() //main
                    .getParentFile() //engine
                    .getParentFile() //rvpredict
                    .getParentFile() //src
                    .getAbsolutePath() + "/";
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
