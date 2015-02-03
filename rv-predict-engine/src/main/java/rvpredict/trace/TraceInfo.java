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
package rvpredict.trace;

import rvpredict.log.LoggingFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * This class keeps the information associated with the trace such as the trace
 * statistics, shared variable signature, etc.
 *
 * @author jeffhuang
 *
 */
public class TraceInfo {

    private final LoggingFactory loggingFactory;

    HashSet<String> sharedAddresses = new HashSet<String>();
    HashSet<Long> threads = new HashSet<Long>();
    int num_br, num_sync, num_rw_shared, num_rw_local, num_w_init, num_prop;
    long traceLength = 0;

    public TraceInfo(LoggingFactory loggingFactory) {
        this.loggingFactory = loggingFactory;
    }

    public void addSharedAddresses(Set<String> s) {
        sharedAddresses.addAll(s);
    }

    public void addThreads(Set<Long> s) {
        threads.addAll(s);
    }

    public int getTraceThreadNumber() {
        return threads.size();
    }

    public int getTraceSharedVariableNumber() {
        return sharedAddresses.size();
    }

    public boolean isVolatileAddr(int varId) {
        return loggingFactory.isVolatile(varId);
    }

    public void incrementBranchNumber() {
        num_br++;
    }

    public void incrementSharedReadWriteNumber() {
        num_rw_shared++;
    }

    public void incrementSyncNumber() {
        num_sync++;
    }

    public void incrementLocalReadWriteNumber() {
        num_rw_local++;
    }

    public void incrementInitWriteNumber() {
        num_w_init++;
    }

    public int getTraceBranchNumber() {
        return num_br;
    }

    public int getTraceSharedReadWriteNumber() {
        return num_rw_shared;
    }

    public int getTraceLocalReadWriteNumber() {
        return num_rw_local;
    }

    public int getTraceInitWriteNumber() {
        return num_w_init;
    }

    public int getTraceSyncNumber() {
        return num_sync;
    }

    public int getTracePropertyNumber() {
        return num_prop;
    }

    public void incrementTraceLength(int size) {
        traceLength += size;
    }

    public long getTraceLength() {
        return traceLength;
    }

    public LoggingFactory getLoggingFactory() {
        return loggingFactory;
    }
}
