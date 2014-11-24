package rvpredict.config;

import java.io.IOException;
import java.util.Properties;

public class Config {
    public static final java.lang.String PROGRAM_NAME = "rv-predict-agent";
    public static final Config instance = new Config();
    public static final String propFile = "rv.conf";

    public final Configuration commandLine = new Configuration();

    public static final String LOG_FIELD_ACCESS = "logFieldAcc";
    public static final String LOG_INIT_WRITE_ACCESS = "logInitialWrite";
    public static final String LOG_ARRAY_ACCESS = "logArrayAcc";
    public static final String LOG_LOCK = "logLock";
    public static final String LOG_UNLOCK = "logUnlock";
    public static final String LOG_BRANCH = "logBranch";
    public static final String LOG_THREAD_START = "logStart";
    public static final String LOG_THREAD_JOIN = "logJoin";
    public static final String LOG_WAIT = "logWait";
    public static final String LOG_NOTIFY = "logNotify";

    public static final String DESC_LOG_FIELD_ACCESS = "(ILjava/lang/Object;ILjava/lang/Object;Z)V";

    public static final String DESC_LOG_INIT_WRITE_ACCESS = "(ILjava/lang/Object;ILjava/lang/Object;)V";
    public static final String DESC_LOG_ARRAY_ACCESS = "(ILjava/lang/Object;ILjava/lang/Object;Z)V";

    public static final String DESC_LOG_LOCK = "(ILjava/lang/Object;)V";
    public static final String DESC_LOG_UNLOCK = "(ILjava/lang/Object;)V";
    public static final String DESC_LOG_BRANCH = "(I)V";
    public static final String DESC_LOG_THREAD_START = "(ILjava/lang/Object;)V";
    public static final String DESC_LOG_THREAD_JOIN = "(ILjava/lang/Object;)V";
    public static final String DESC_LOG_WAIT = "(ILjava/lang/Object;)V";
    public static final String DESC_LOG_NOTIFY = "(ILjava/lang/Object;)V";

    public boolean verbose;

    public String[] excludeList;
    public String[] includeList;
    public String logClass;

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
            logClass = properties.getProperty("rv.logClass", "rvpredict.logging.RecordRT").replace(
                    '.', '/');

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
