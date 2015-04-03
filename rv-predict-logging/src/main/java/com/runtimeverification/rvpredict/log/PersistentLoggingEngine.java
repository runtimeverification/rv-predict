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
package com.runtimeverification.rvpredict.log;

import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.trace.EventType;
import com.runtimeverification.rvpredict.util.Constants;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class encapsulating functionality for recording events
 *
 * @author TraianSF
 *
 */
public class PersistentLoggingEngine implements ILoggingEngine, Constants {

    private volatile boolean shutdown = false;

    /**
     * Global ID of the next event.
     */
    private final AtomicLong globalEventID = new AtomicLong(1);

    private final LoggingFactory loggingFactory;

    private final Metadata metadata;

    private final List<EventWriter> eventWriters = new ArrayList<>();

    private final ThreadLocalEventWriter threadLocalEventWriter = new ThreadLocalEventWriter();

    public PersistentLoggingEngine(LoggingFactory loggingFactory, Metadata metadata) {
        this.loggingFactory = loggingFactory;
        this.metadata = metadata;
    }

    /**
     * Method invoked at the end of the logging task, to insure that
     * all data is recorded before concluding.
     * @throws IOException
     */
    @Override
    public void finishLogging() throws IOException {
        shutdown = true;

        synchronized (eventWriters) {
            for (EventWriter writer : eventWriters) {
                writer.close();
            }
        }

        try (ObjectOutputStream os = loggingFactory.createMetadataOS()) {
            os.writeObject(metadata);
        }
    }

    /**
     * Logs an event item to the trace.
     *
     * @see {@link EventItem} for a more elaborate description of the
     *      parameters.
     */
    @Override
    public void log(EventType eventType, int locId, int addrl, int addrr, long value1, long value2) {
        long tid = Thread.currentThread().getId();
        long gid;
        switch (eventType) {
        case ATOMIC_READ:
            gid = globalEventID.getAndAdd(3);
            log(EventType.WRITE_LOCK,   gid,     tid, locId, ATOMIC_LOCK_C, addrl, 0);
            log(EventType.READ,         gid + 1, tid, locId, addrl,         addrr, value1);
            log(EventType.WRITE_UNLOCK, gid + 2, tid, locId, ATOMIC_LOCK_C, addrl, 0);
            break;
        case ATOMIC_WRITE:
            gid = globalEventID.getAndAdd(3);
            log(EventType.WRITE_LOCK,   gid,     tid, locId, ATOMIC_LOCK_C, addrl, 0);
            log(EventType.WRITE,        gid + 1, tid, locId, addrl,         addrr, value1);
            log(EventType.WRITE_UNLOCK, gid + 2, tid, locId, ATOMIC_LOCK_C, addrl, 0);
            break;
        case ATOMIC_READ_THEN_WRITE:
            gid = globalEventID.getAndAdd(4);
            log(EventType.WRITE_LOCK,   gid,     tid, locId, ATOMIC_LOCK_C, addrl, 0);
            log(EventType.READ,         gid + 1, tid, locId, addrl,         addrr, value1);
            log(EventType.WRITE,        gid + 2, tid, locId, addrl,         addrr, value2);
            log(EventType.WRITE_UNLOCK, gid + 3, tid, locId, ATOMIC_LOCK_C, addrl, 0);
            break;
        case READ:
        case WRITE:
        case WRITE_LOCK:
        case WRITE_UNLOCK:
        case READ_LOCK:
        case READ_UNLOCK:
        case WAIT_REL:
        case WAIT_ACQ:
        case START:
        case JOIN:
        case CLINIT_ENTER:
        case CLINIT_EXIT:
        case INVOKE_METHOD:
        case FINISH_METHOD:
            gid = globalEventID.getAndIncrement();
            log(eventType, gid, tid, locId, addrl, addrr, value1);
            break;
        default:
            assert false;
        }
    }

    private void log(EventType eventType, long gid, long tid, int locId, int addrl, int addrr,
            long value) {
        EventWriter writer = threadLocalEventWriter.get();
        if (writer != null) {
            try {
                writer.write(gid, tid, locId, addrl, addrr, value, eventType);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class ThreadLocalEventWriter extends ThreadLocal<EventWriter> {
        @Override
        protected EventWriter initialValue() {
            synchronized (eventWriters) {
                if (shutdown) {
                    System.err.printf("[Warning] JVM exits before %s finishes;"
                            + " no trace from this thread is logged.%n",
                            Thread.currentThread().getName());
                    return null;
                } else {
                    try {
                        EventWriter eventWriter = loggingFactory.createEventWriter();
                        eventWriters.add(eventWriter);
                        return eventWriter;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
       }
    }

}
