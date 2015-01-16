package rvpredict.config;

import java.io.IOException;
import java.util.Properties;

public class Config {
    public static final java.lang.String PROGRAM_NAME = "rv-predict-agent";
    public static final Config instance = new Config();
    public static final String propFile = "rv.conf";

    public final Configuration commandLine = new Configuration();

    public boolean verbose;

    public String[] excludeList;
    public String[] includeList;

    public static boolean shutDown = false;

    public Config() {
        try {
            Properties properties = new Properties();

            properties.load(ClassLoader.getSystemClassLoader()// this.getClass().getClassLoader()
                    .getResourceAsStream(propFile));

            verbose = properties.getProperty("rv.verbose", "false").equals("true");
            excludeList = properties.getProperty("rv.excludeList", "").split(",");
            if (excludeList.length == 1 && excludeList[0].isEmpty()) {
                excludeList = null;
            }
            includeList = properties.getProperty("rv.includeList", "").split(",");
            if (includeList.length == 1 && includeList[0].isEmpty()) {
                includeList = null;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
