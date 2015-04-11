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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.engine.main.RaceDetectorTask;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.trace.EventType;
import com.runtimeverification.rvpredict.trace.EventUtils;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.trace.TraceState;
import com.runtimeverification.rvpredict.util.Constants;
import com.runtimeverification.rvpredict.violation.Violation;

/**
 * Logging engine that processes events and then send them over for online
 * prediction.
 *
 * @author YilongL
 *
 */
public class VolatileLoggingEngine implements ILoggingEngine, Constants {

    private final Configuration config;

    private final Metadata metadata;

    private volatile boolean closed = false;

    private final TraceState crntState;

    private final Set<Violation> violations = new HashSet<>();

    private final AtomicInteger globalEventID = new AtomicInteger(0);

    private final LongAdder published = new LongAdder();

    private long base = 0;

    private final EventItem[] items;

    public VolatileLoggingEngine(Configuration config, Metadata metadata) {
        this.config = config;
        this.metadata = metadata;
        this.crntState = new TraceState(metadata);
        this.items = new EventItem[config.windowSize * 2];
        for (int i = 0; i < items.length; i++) {
            items[i] = new EventItem();
        }
    }

    @Override
    public void finishLogging() {
        closed = true;
        int n = globalEventID.getAndAdd(config.windowSize);
        if (n < config.windowSize) {
            // CRITICAL SECTION BEGIN
            runRaceDetection(n);
            // CRITICAL SECTION END
        }
    }

    private int claim(int next) {
        int nextPos = globalEventID.getAndAdd(next);
        if (nextPos + next > config.windowSize) {  // running out of slots
            if (nextPos <= config.windowSize) {
                // CRITICAL SECTION BEGIN
                /* first thread that runs out of slots is responsible to run the prediction */
                runRaceDetection(nextPos);

                /* reset the counter */
                if (!closed) {
                    base += nextPos;
                    globalEventID.set(next);
                    published.reset();
                }
                return 0;
                // CRITICAL SECTION END
            } else {
                /* busy waiting until the counter is reset */
                while (globalEventID.get() >= config.windowSize && !closed) {
                    LockSupport.parkNanos(1);
                }
                /* try again, small chance to fail and get into busy waiting again */
                return closed ? 0 : claim(next);
            }
        } else {    // on success
            return nextPos;
        }
    }

    private void publish(int n) {
        published.add(n);
    }

    private void runRaceDetection(int numOfEventItems) {
        /* makes sure that all the claimed slots have been published */
        while (published.sum() < numOfEventItems) {
            LockSupport.parkNanos(1);
        }

        try {
            Trace trace = new Trace(crntState, config.windowSize);
            crntState.setCurrentTraceWindow(trace);
            for (int i = 0; i < numOfEventItems; i++) {
                trace.addRawEvent(EventUtils.of(items[i]));
            }
            trace.finishedLoading();
            if (trace.hasSharedMemAddr()) {
                new RaceDetectorTask(config, metadata, trace, violations).run();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void log(EventType eventType, int locId, int addrl, int addrr, long value1, long value2) {
        long tid = Thread.currentThread().getId();
        int off;
        switch (eventType) {
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
            off = claim(1);
            log(eventType, off, tid, locId, addrl, addrr, value1);
            publish(1);
            break;
        case ATOMIC_READ:
            off = claim(3);
            log(EventType.WRITE_LOCK,   off,     tid, locId, ATOMIC_LOCK_C, addrl, 0);
            log(EventType.READ,         off + 1, tid, locId, addrl,         addrr, value1);
            log(EventType.WRITE_UNLOCK, off + 2, tid, locId, ATOMIC_LOCK_C, addrl, 0);
            publish(3);
            break;
        case ATOMIC_WRITE:
            off = claim(3);
            log(EventType.WRITE_LOCK,   off,     tid, locId, ATOMIC_LOCK_C, addrl, 0);
            log(EventType.WRITE,        off + 1, tid, locId, addrl,         addrr, value1);
            log(EventType.WRITE_UNLOCK, off + 2, tid, locId, ATOMIC_LOCK_C, addrl, 0);
            publish(3);
            break;
        case ATOMIC_READ_THEN_WRITE:
            off = claim(4);
            log(EventType.WRITE_LOCK,   off,     tid, locId, ATOMIC_LOCK_C, addrl, 0);
            log(EventType.READ,         off + 1, tid, locId, addrl,         addrr, value1);
            log(EventType.WRITE,        off + 2, tid, locId, addrl,         addrr, value2);
            log(EventType.WRITE_UNLOCK, off + 3, tid, locId, ATOMIC_LOCK_C, addrl, 0);
            publish(4);
            break;
        default:
            assert false;
        }
    }

    private void log(EventType eventType, int offset, long tid, int locId, int addrl, int addrr,
            long value) {
        EventItem item = items[offset];
        item.GID = base + offset;
        item.TID = tid;
        item.ID = locId;
        item.ADDRL = addrl;
        item.ADDRR = addrr;
        item.VALUE = value;
        item.TYPE = eventType;
    }

}
