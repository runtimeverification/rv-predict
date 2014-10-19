package rvpredict.util;

import rvpredict.config.Configuration;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Traian on 09.10.2014.
 */
public interface Metadata {
    ConcurrentHashMap<String, Integer> getUnsavedVariableIdMap();

    ConcurrentHashMap<String, Boolean> getUnsavedVolatileVariables();

    ConcurrentHashMap<String, Integer> getUnsavedStmtSigIdMap();

    int getVariableId(String sig);
}
