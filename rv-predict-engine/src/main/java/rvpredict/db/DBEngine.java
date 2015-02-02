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

import rvpredict.logging.LoggingFactory;
import rvpredict.trace.AbstractEvent;
import rvpredict.trace.Event;
import rvpredict.trace.Trace;
import rvpredict.trace.TraceInfo;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Class for loading metadata and traces of events from disk.
 *
 * @author TraianSF
 *
 */
public class DBEngine {
    private final TraceCache traceCache;
    private final Set<Integer> volatileFieldIds;
    private final Map<Integer, String> varIdToVarSig;
    private final Map<Integer, String> locIdToStmtSig;

    public DBEngine(LoggingFactory loggingFactory) throws IOException, ClassNotFoundException {
        traceCache = new TraceCache(loggingFactory);
        volatileFieldIds = loggingFactory.getVolatileFieldIds();
        varIdToVarSig = loggingFactory.getVarIdToVarSig();
        locIdToStmtSig = loggingFactory.getLocIdToStmtSig();
    }

    /**
     * Checks that the trace was recorded properly
     * TODO: design and implement some proper checks.
     */
    public boolean checkLog() {
        return true;
    }

    /**
     * Load trace segment from event {@code fromIndex} to event
     * {@code toIndex-1}. Event number is assumed to start from 1.
     *
     * @see rvpredict.db.TraceCache#getEvent(long)
     * @param fromIndex
     *            low endpoint (inclusive) of the trace segment
     * @param toIndex
     *            high endpoint (exclusive) of the trace segment
     * @return a {@link rvpredict.trace.Trace} representing the trace segment
     *         read
     */
    public Trace getTrace(long fromIndex, long toIndex, Trace.State initState, TraceInfo info) throws IOException, InterruptedException {
        Trace trace = new Trace(initState, info);
        for (long index = fromIndex; index < toIndex; index++) {
            EventItem eventItem = traceCache.getEvent(index);
            if (eventItem == null) 
                break;
            Event node = AbstractEvent.of(eventItem);
            trace.addRawEvent(node);
        }

        trace.finishedLoading();

        return trace;
    }

    public Set<Integer> getVolatileFieldIds() {
        return volatileFieldIds;
    }

    public Map<Integer, String> getVarIdToVarSig() {
        return varIdToVarSig;
    }

    public Map<Integer, String> getLocIdToStmtSig() {
        return locIdToStmtSig;
    }

}
