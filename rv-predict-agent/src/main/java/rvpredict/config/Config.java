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
    public static final String LOG_MONITOR_ENTER = "logMonitorEnter";
    public static final String LOG_MONITOR_EXIT = "logMonitorExit";
    public static final String LOG_BRANCH = "logBranch";

    // Thread methods
    public static final String RVPREDICT_START = "rvPredictStart";
    public static final String RVPREDICT_JOIN = "rvPredictJoin";
    public static final String RVPREDICT_INTERRUPT = "rvPredictInterrupt";
    public static final String RVPREDICT_INTERRUPTED = "rvPredictInterrupted";
    public static final String RVPREDICT_IS_INTERRUPTED = "rvPredictIsInterrupted";
    public static final String RVPREDICT_SLEEP = "rvPredictSleep";

    // Object monitor methods
    public static final String RVPREDICT_WAIT = "rvPredictWait";

    // java.util.concurrent.locks.Lock methods
    // note that this doesn't provide mocks for methods specific in concrete lock implementation
    public static final String RVPREDICT_LOCK = "rvPredictLock";
    public static final String RVPREDICT_LOCK_INTERRUPTIBLY = "rvPredictLockInterruptibly";
    public static final String RVPREDICT_TRY_LOCK = "rvPredictTryLock";
    public static final String RVPREDICT_UNLOCK = "rvPredictUnlock";

    // java.util.concurrent.locks.ReadWriteLock methods
    public static final String RVPREDICT_RW_LOCK_READ_LOCK = "rvPredictReadWriteLockReadLock";
    public static final String RVPREDICT_RW_LOCK_WRITE_LOCK = "rvPredictReadWriteLockWriteLock";

    public static final String DESC_LOG_FIELD_ACCESS = "(ILjava/lang/Object;ILjava/lang/Object;ZZ)V";
    public static final String DESC_LOG_ARRAY_ACCESS = "(ILjava/lang/Object;ILjava/lang/Object;Z)V";
    public static final String DESC_LOG_FIELD_INIT = "(ILjava/lang/Object;ILjava/lang/Object;)V";
    public static final String DESC_LOG_ARRAY_INIT = "(ILjava/lang/Object;ILjava/lang/Object;)V";

    public static final String DESC_LOG_MONITOR_ENTER = "(ILjava/lang/Object;)V";
    public static final String DESC_LOG_MONITOR_EXIT = "(ILjava/lang/Object;)V";
    public static final String DESC_LOG_BRANCH = "(I)V";
    public static final String DESC_RVPREDICT_START = "(ILjava/lang/Thread;)V";
    public static final String DESC_RVPREDICT_JOIN = "(ILjava/lang/Thread;)V";
    public static final String DESC_RVPREDICT_JOIN_TIMEOUT = "(ILjava/lang/Thread;J)V";
    public static final String DESC_RVPREDICT_JOIN_TIMEOUT_NANO = "(ILjava/lang/Thread;JI)V";
    public static final String DESC_RVPREDICT_INTERRUPT = "(ILjava/lang/Thread;)V";
    public static final String DESC_RVPREDICT_INTERRUPTED = "(I)Z";
    public static final String DESC_RVPREDICT_IS_INTERRUPTED = "(ILjava/lang/Thread;)Z";
    public static final String DESC_RVPREDICT_SLEEP = "(IJ)V";
    public static final String DESC_RVPREDICT_SLEEP_NANOS = "(IJI)V";
    public static final String DESC_RVPREDICT_WAIT = "(ILjava/lang/Object;)V";
    public static final String DESC_RVPREDICT_WAIT_TIMEOUT = "(ILjava/lang/Object;J)V";
    public static final String DESC_RVPREDICT_WAIT_TIMEOUT_NANO = "(ILjava/lang/Object;JI)V";

    public static final String RVPREDICT_SYSTEM_ARRAYCOPY = "rvPredictSystemArraycopy";
    public static final String DESC_RVPREDICT_SYSTEM_ARRAYCOPY = "(ILjava/lang/Object;ILjava/lang/Object;II)V";

    public static final String DESC_RVPREDICT_LOCK = "(ILjava/util/concurrent/locks/Lock;)V";
    public static final String DESC_RVPREDICT_LOCK_INTERRUPTIBLY = "(ILjava/util/concurrent/locks/Lock;)V";
    public static final String DESC_RVPREDICT_TRY_LOCK = "(ILjava/util/concurrent/locks/Lock;)Z";
    public static final String DESC_RVPREDICT_TRY_LOCK_TIMEOUT = "(ILjava/util/concurrent/locks/Lock;JLjava/util/concurrent/TimeUnit;)Z";
    public static final String DESC_RVPREDICT_UNLOCK = "(ILjava/util/concurrent/locks/Lock;)V";

    public static final String DESC_RVPREDICT_RW_LOCK_READ_LOCK = "(ILjava/util/concurrent/locks/ReadWriteLock;)Ljava/util/concurrent/locks/Lock;";
    public static final String DESC_RVPREDICT_RW_LOCK_WRITE_LOCK = "(ILjava/util/concurrent/locks/ReadWriteLock;)Ljava/util/concurrent/locks/Lock;";
    public static final String DESC_RVPREDICT_REENTRANT_RW_LOCK_READ_LOCK = "(ILjava/util/concurrent/locks/ReentrantReadWriteLock;)Ljava/util/concurrent/locks/ReentrantReadWriteLock$ReadLock;";
    public static final String DESC_RVPREDICT_REENTRANT_RW_LOCK_WRITE_LOCK = "(ILjava/util/concurrent/locks/ReentrantReadWriteLock;)Ljava/util/concurrent/locks/ReentrantReadWriteLock$WriteLock;";

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
