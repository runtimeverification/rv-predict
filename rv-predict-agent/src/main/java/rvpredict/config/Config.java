package rvpredict.config;

import java.io.IOException;
import java.util.Properties;

import rvpredict.runtime.RVPredictRuntime;

public class Config {
    public static final java.lang.String PROGRAM_NAME = "rv-predict-agent";
    public static final Config instance = new Config();
    public static final String propFile = "rv.conf";

    public final Configuration commandLine = new Configuration();

    public static final String LOG_FIELD_ACCESS = "logFieldAcc";
    public static final String LOG_FIELD_INIT = "logFieldInit";
    public static final String LOG_ARRAY_ACCESS = "logArrayAcc";
    public static final String LOG_ARRAY_INIT = "logArrayInit";
    public static final String LOG_LOCK = "logLock";
    public static final String LOG_UNLOCK = "logUnlock";
    public static final String LOG_BRANCH = "logBranch";
    public static final String RVPREDICT_THREAD_START = "rvPredictStart";
    public static final String RVPREDICT_JOIN = "rvPredictJoin";
    public static final String RVPREDICT_INTERRUPT = "rvPredictInterrupt";
    public static final String RVPREDICT_INTERRUPTED = "rvPredictInterrupted";
    public static final String RVPREDICT_IS_INTERRUPTED = "rvPredictIsInterrupted";
    public static final String RVPREDICT_SLEEP = "rvPredictSleep";
    public static final String RVPREDICT_WAIT = "rvPredictWait";
    public static final String RVPREDICT_NOTIFY = "rvPredictNotify";
    public static final String RVPREDICT_NOTIFY_ALL = "rvPredictNotifyAll";

    public static final String DESC_LOG_FIELD_ACCESS = "(ILjava/lang/Object;ILjava/lang/Object;ZZ)V";
    public static final String DESC_LOG_ARRAY_ACCESS = "(ILjava/lang/Object;ILjava/lang/Object;Z)V";
    public static final String DESC_LOG_FIELD_INIT = "(ILjava/lang/Object;ILjava/lang/Object;)V";
    public static final String DESC_LOG_ARRAY_INIT = "(ILjava/lang/Object;ILjava/lang/Object;)V";

    public static final String DESC_LOG_LOCK = "(ILjava/lang/Object;)V";
    public static final String DESC_LOG_UNLOCK = "(ILjava/lang/Object;)V";
    public static final String DESC_LOG_BRANCH = "(I)V";
    public static final String DESC_RVPREDICT_THREAD_START = "(ILjava/lang/Thread;)V";
    public static final String DESC_RVPREDICT_JOIN = "(ILjava/lang/Thread;)V";
    public static final String DESC_RVPREDICT_JOIN_TIMEOUT = "(ILjava/lang/Thread;J)V";
    public static final String DESC_RVPREDICT_JOIN_TIMEOUT_NANO = "(ILjava/lang/Thread;JI)V";
    public static final String DESC_RVPREDICT_INTERRUPT = "(ILjava/lang/Thread;)V";
    public static final String DESC_RVPREDICT_INTERRUPTED = "(I)Z";
    public static final String DESC_RVPREDICT_IS_INTERRUPTED = "(ILjava/lang/Thread;)Z";
    public static final String DESC_RVPREDICT_SLEEP = "(J)V";
    public static final String DESC_RVPREDICT_SLEEP_NANOS = "(JI)V";
    public static final String DESC_RVPREDICT_WAIT = "(ILjava/lang/Object;)V";
    public static final String DESC_RVPREDICT_WAIT_TIMEOUT = "(ILjava/lang/Object;J)V";
    public static final String DESC_RVPREDICT_WAIT_TIMEOUT_NANO = "(ILjava/lang/Object;JI)V";
    public static final String DESC_RVPREDICT_NOTIFY = "(ILjava/lang/Object;)V";
    public static final String DESC_RVPREDICT_NOTIFY_ALL = "(ILjava/lang/Object;)V";

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
            logClass = properties.getProperty("rv.logClass", RVPredictRuntime.class.getName()).replace(
                    '.', '/');

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
