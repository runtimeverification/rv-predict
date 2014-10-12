package rvpredict.config;

import java.io.IOException;
import java.util.Properties;

public class Config {
    public static final java.lang.String PROGRAM_NAME = "rv-predict-agent";
    public static final Config instance = new Config();
    public static final String propFile ="rv.conf";

    public final Configuration commandLine = new Configuration();

    public final String LOG_FIELD_ACCESS = "logFieldAcc";
    public final String LOG_INIT_WRITE_ACCESS = "logInitialWrite";
    public final String LOG_ARRAY_ACCESS = "logArrayAcc";
    public final String LOG_LOCK_INSTANCE = "logLock";
    public final String LOG_LOCK_STATIC = "logStaticSyncLock";
    public final String LOG_UNLOCK_INSTANCE = "logUnlock";
    public final String LOG_UNLOCK_STATIC = "logStaticSyncUnlock";
    public final String LOG_BRANCH = "logBranch";
    public final String LOG_THREAD_START = "logStart";
    public final String LOG_THREAD_JOIN = "logJoin";
    public final String LOG_WAIT = "logWait";
    public final String LOG_NOTIFY = "logNotify";

    
    public final String DESC_LOG_FIELD_ACCESS = "(ILjava/lang/Object;ILjava/lang/Object;Z)V";
    public final String DESC_LOG_FIELD_ACCESS_DETECT_SHARING = "(IIZ)V";
    
    public final String DESC_LOG_INIT_WRITE_ACCESS = "(ILjava/lang/Object;ILjava/lang/Object;)V";
    public final String DESC_LOG_ARRAY_ACCESS = "(ILjava/lang/Object;ILjava/lang/Object;Z)V";
    public final String DESC_LOG_ARRAY_ACCESS_DETECT_SHARING ="(ILjava/lang/Object;IZ)V";
    
    public final String DESC_LOG_LOCK_INSTANCE = "(ILjava/lang/Object;)V";
    public final String DESC_LOG_LOCK_STATIC = "(II)V";
    public final String DESC_LOG_UNLOCK_INSTANCE = "(ILjava/lang/Object;)V";
    public final String DESC_LOG_UNLOCK_STATIC = "(II)V";
    public final String DESC_LOG_BRANCH = "(I)V";
    public final String DESC_LOG_THREAD_START = "(ILjava/lang/Object;)V";
    public final String DESC_LOG_THREAD_JOIN = "(ILjava/lang/Object;)V";
    public final String DESC_LOG_WAIT = "(ILjava/lang/Object;)V";
    public final String DESC_LOG_NOTIFY = "(ILjava/lang/Object;)V";

    public boolean verbose;

    public String[] excludeList;
    public String[] includeList;
    public String logClass;

    public Config() {
        try {
            Properties properties = new Properties();

           
    		properties.load( ClassLoader.getSystemClassLoader()//this.getClass().getClassLoader()
    	            .getResourceAsStream(propFile));
    		
            verbose = properties.getProperty("rv.verbose","false").equals("true");
            excludeList = properties.getProperty("rv.excludeList","").split(",");
            if (excludeList.length == 1 && excludeList[0].isEmpty()) {
                excludeList = null;
            }
            includeList = properties.getProperty("rv.includeList","").split(",");
            if (includeList.length == 1 && includeList[0].isEmpty()) {
                includeList = null;
            }
            logClass = properties.getProperty("rv.logClass", "rvpredict.logging.RecordRT").replace('.','/');

    	} catch (IOException ex) {
    		ex.printStackTrace();
        }
    }
}
