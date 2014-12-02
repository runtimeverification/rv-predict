/*******************************************************************************
 * Copyright (c) 2013 University of Illinois
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package rvpredict.db;

import java.io.*;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.List;

import rvpredict.trace.*;

/**
 * Engine for interacting with database.
 *
 * @author jeffhuang
 *
 */
public class DBEngine {

    private final String directory;

    private TraceCache traceCache=null;

    public void getMetadata(Map<Long, String> threadIdNameMap, Map<Integer, String> sharedVarIdSigMap, Map<Integer, String> volatileAddresses, Map<Integer, String> stmtIdSigMap) {
        try {
            ObjectInputStream metadataIS = new ObjectInputStream(
                    new BufferedInputStream(
                            new FileInputStream(Paths.get(directory, "metadata.bin").toFile())));
            while(true) {
                List<Map.Entry<Long, String>> threadTidList;
                try {
                    threadTidList = (List<Map.Entry<Long, String>>) metadataIS.readObject();
                } catch (EOFException _) { break;} // EOF should only happen for threadTid
                for (Map.Entry<Long,String> entry : threadTidList) {
                    threadIdNameMap.put(entry.getKey(), entry.getValue());
                }
                List<Map.Entry<String, Integer>> variableIdList = (List<Map.Entry<String, Integer>>) metadataIS.readObject();
                for (Map.Entry<String, Integer> entry : variableIdList) {
                    sharedVarIdSigMap.put(entry.getValue(), entry.getKey());
                }
                List<Map.Entry<String, Integer>> volatileVarList = (List<Map.Entry<String, Integer>>) metadataIS.readObject();
                for (Map.Entry<String, Integer> entry : volatileVarList) {
                    volatileAddresses.put(entry.getValue(), entry.getKey());
                }
                List<Map.Entry<String, Integer>> stmtSigIdList = (List<Map.Entry<String, Integer>>) metadataIS.readObject();
                for (Map.Entry<String, Integer> entry : stmtSigIdList) {
                    stmtIdSigMap.put(entry.getValue(), entry.getKey());
                }
            }
        }
        catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public DBEngine(String directory) {
        this.directory = directory;

    }

    /**
     * Checks that the trace was recorded properly
     * TODO: design and implement some proper checks.
     */
    public boolean checkLog() {
        return true;
    }

    public long getTraceSize() {
        if (traceCache == null) traceCache = new TraceCache(directory);
        return traceCache.getTraceSize();
    }

    /**
     * load trace from event min to event max
     *
     * @param min
     * @param max
     * @return
     * @throws Exception
     */
    public Trace getTrace(long min, long max, TraceInfo info) {
        if (traceCache == null) traceCache = new TraceCache(directory);
        long traceSize = traceCache.getTraceSize();
        assert min <= traceSize : "This method should only be called with a valid min value";
        if (max > traceSize) max = traceSize; // resetting max to trace size.
        Trace trace = new Trace(info);
        AbstractEvent node = null;
        for (long index = min; index <= max; index++) {
            rvpredict.db.EventItem eventItem = traceCache.getEvent(index);
            long GID = eventItem.GID;
            long TID = eventItem.TID;
            int ID = eventItem.ID;
            long ADDRL = eventItem.ADDRL;
            long ADDRR = eventItem.ADDRR;
            long VALUE = eventItem.VALUE;
            EventType TYPE = eventItem.TYPE;

            switch (TYPE) {
                case INIT:
                    node = new InitEvent(GID, TID, ID, ADDRL, ADDRR, VALUE);
                    break;
                case READ:
                    node = new ReadEvent(GID, TID, ID, ADDRL, ADDRR, VALUE);
                    break;
                case WRITE:
                    node = new WriteEvent(GID, TID, ID, ADDRL, ADDRR, VALUE);
                    break;
                case LOCK:
                    node = new SyncEvent(GID, TID, ID, EventType.LOCK, ADDRL);
                    break;
                case UNLOCK:
                    node = new SyncEvent(GID, TID, ID, EventType.UNLOCK, ADDRL);
                    break;
                case WAIT:
                    node = new SyncEvent(GID, TID, ID, EventType.WAIT, ADDRL);
                    break;
                case NOTIFY:
                    node = new SyncEvent(GID, TID, ID, EventType.NOTIFY, ADDRL);
                    break;
                case START:
                    node = new SyncEvent(GID, TID, ID, EventType.START, ADDRL);
                    break;
                case JOIN:
                    node = new SyncEvent(GID, TID, ID, EventType.JOIN, ADDRL);
                    break;
                case BRANCH:
                    node = new BranchNode(GID, TID, ID);
                    break;
                default:
                    System.out.println(TYPE);
                    break;
            }

            trace.addRawNode(node);

        }

        trace.finishedLoading();

        return trace;
    }

}
