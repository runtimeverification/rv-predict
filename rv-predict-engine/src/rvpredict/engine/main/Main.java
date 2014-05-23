package rvpredict.engine.main;

import config.Configuration;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;

/**
 * @author TraianSF
 */
public class Main {
    public static void main(String[] args) {


        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                args[i] = args[i].substring(0, args[i].length() - 4);
            }
        }

        Configuration config = new Configuration(args);

        String java = org.apache.tools.ant.util.JavaEnvUtils.getJreExecutable("java");
        String basePath = getBasePath();
        String separator = System.getProperty("file.separator");
        String libPath = basePath + separator + "lib" + separator;
        String iagent = libPath + "iagent.jar";
        String rvAgent = libPath + "rv-predict-agent.jar";
        String classpath = "";
        if (config.appClassPath != null) {
            classpath = config.appClassPath;
        }
        classpath = rvAgent + System.getProperty("path.separator") + classpath;
        String[] appArgs = new String[]{java, "-cp",
                classpath,
                "-javaagent:" + iagent,
                config.appname};
        System.out.println(Arrays.toString(appArgs));
        ProcessBuilder processBuilder =
                new ProcessBuilder(appArgs);
        processBuilder.inheritIO();
        try {
            System.out.print("Starting " + config.appname + "...");
            Process process = processBuilder.start();
            process.waitFor();
            System.out.println("done.");
        } catch (IOException e) {
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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
