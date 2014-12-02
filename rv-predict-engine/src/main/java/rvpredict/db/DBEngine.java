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
import java.util.*;

import rvpredict.trace.*;

/**
 * Class for loading traces of events from disk.
 *
 * @author TraianSF
 *
 */
public class DBEngine {
    public static final String METADATA_BIN = "metadata.bin";
    private final TraceCache traceCache;
    private final ObjectInputStream metadataIS;

    /**
     * Reads the previously saved metadata into the structures given as parameters
     * @param threadIdNameMap  map associating thread identifiers to thread names
     * @param sharedVarIdSigMap map giving signatures for the shared variables
     * @param volatileAddresses  map giving locations for volatile variables
     * @param stmtIdSigMap  map giving signature/location information for events
     */
    @SuppressWarnings("unchecked")
    public void getMetadata(Set<Integer> volatileFieldIds, Map<Integer, String> locIdToStmtSig) {
        try {
            while (true) {
                try {
                    volatileFieldIds.addAll((Collection<Integer>) metadataIS.readObject());
                } catch (EOFException e) {
                    break;
                }
                List<Map.Entry<String, Integer>> stmtSigIdList = (List<Map.Entry<String, Integer>>) metadataIS
                        .readObject();
                for (Map.Entry<String, Integer> entry : stmtSigIdList) {
                    locIdToStmtSig.put(entry.getValue(), entry.getKey());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public DBEngine(String directory) {
        traceCache = new TraceCache(directory);
        ObjectInputStream metadataIS = null;
        try {
            metadataIS = new ObjectInputStream(
                    new BufferedInputStream(
                            new FileInputStream(Paths.get(directory, METADATA_BIN).toFile())));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.metadataIS = metadataIS;
    }

    /**
     * Checks that the trace was recorded properly
     * TODO: design and implement some proper checks.
     */
    public boolean checkLog() {
        return true;
    }

    /**
     *
     * @return total size (in events) of the recorded trace.
     */
    public long getTraceSize() {
        return traceCache.getTraceSize();
    }

    /**
     * load trace from event min to event max
     * Currently trace is assumed to be read sequentially,
     * in min-max segments, from beginning to end.
     * @see rvpredict.db.TraceCache#getEvent(long)
     *
     * @param min index where the trace segment to be read should start from
     * @param max index where the trace segment to be read should end
     * @return a {@link rvpredict.trace.Trace} representing the trace segment read
     */
    public Trace getTrace(long min, long max, Map<String, Long> initValues, TraceInfo info) {
        long traceSize = traceCache.getTraceSize();
        assert min <= traceSize : "This method should only be called with a valid min value";
        if (max > traceSize) max = traceSize; // resetting max to trace size.
        Trace trace = new Trace(initValues, info);
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
