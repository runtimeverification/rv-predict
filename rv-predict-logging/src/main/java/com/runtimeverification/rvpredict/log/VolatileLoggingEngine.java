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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.LockSupport;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.engine.main.RaceDetector;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.trace.RawTrace;
import com.runtimeverification.rvpredict.trace.TraceState;
import com.runtimeverification.rvpredict.util.Constants;
import com.runtimeverification.rvpredict.util.Logger;

/**
 * Logging engine that processes events and then send them over for online
 * prediction.
 *
 * @author YilongL
 *
 */
public class VolatileLoggingEngine implements ILoggingEngine, Constants {

    private static final int INFINITY = Integer.MAX_VALUE / 2;

    private final Configuration config;

    private volatile boolean closed = false;

    private final TraceState crntState;

    private final AtomicInteger globalEventID = new AtomicInteger(0);

    private final LongAdder finalized = new LongAdder();

    private long base = 0;

    private final int windowSize;

    /**
     * Buffers that are alive so far.
     * <p>
     * TODO(YilongL): optimize for the case where the number of threads is large.
     */
    private final Set<Buffer> activeBuffers = Collections.synchronizedSet(Collections
            .newSetFromMap(new WeakHashMap<Buffer, Boolean>()));

    private final ThreadLocal<Buffer> threadLocalBuffer = new ThreadLocal<Buffer>() {
        @Override
        protected Buffer initialValue() {
            Buffer buffer = new Buffer(windowSize);
            activeBuffers.add(buffer);
            return buffer;
        }
    };

    private final RaceDetector detector;

    public VolatileLoggingEngine(Configuration config, Metadata metadata) {
        this.config = config;
        this.crntState = new TraceState(metadata);
        this.windowSize = config.windowSize;
        this.detector = new RaceDetector(config);
    }

    @Override
    public void finishLogging() {
        /* After we close this logging engine, the remaining events in the
         * buffer that have not acquired GIDs will be lost. */
        closed = true;
        int n = globalEventID.getAndAdd(windowSize); // block the GID counter for good
        if (n <= windowSize) {
            // CRITICAL SECTION BEGIN
            runRaceDetection(n);
            // CRITICAL SECTION END
        } else {
            /* wait for the prediction on the last batch of events to finish */
            while (globalEventID.get() < INFINITY) {
                LockSupport.parkNanos(1);
            }
        }

        if (detector.getRaces().isEmpty()) {
            config.logger.report("No races found.", Logger.MSGTYPE.INFO);
        }
    }

    /**
     * Claims next {@code n} consecutive GIDs.
     *
     * @param n
     *            the number of consecutive GIDs to claim
     * @return the first GID acquired
     */
    private long claimGID(int n) {
        int numOfEvents = globalEventID.getAndAdd(n);
        if (numOfEvents + n > windowSize) {  // running out of slots
            if (numOfEvents <= windowSize) {
                // CRITICAL SECTION BEGIN
                /* first thread that runs out of slots is responsible to run the prediction */
                runRaceDetection(numOfEvents);

                /* reset the counter */
                if (!closed) {
                    base += numOfEvents;
                    finalized.reset();
                    activeBuffers.forEach(Buffer::consume);
                    // CRITICAL SECTION END
                    globalEventID.set(n); // must be placed at the end of the critical section
                    return base;
                } else {
                    /* signal the end of the last prediction */
                    globalEventID.set(INFINITY);
                    return Integer.MIN_VALUE;
                }
            } else {
                /* busy waiting until the counter is reset */
                while (globalEventID.get() >= windowSize && !closed) {
                    LockSupport.parkNanos(1);
                }
                /* try again, small chance to fail and get into busy waiting again */
                return closed ? Integer.MIN_VALUE : claimGID(n);
            }
        } else {    // on success
            return base + numOfEvents;
        }
    }

    private void runRaceDetection(int numOfEvents) {
        /* makes sure that all the events have been finalized */
        while (finalized.sum() < numOfEvents) {
            LockSupport.parkNanos(1);
        }

        try {
            List<RawTrace> rawTraces = new ArrayList<>();
            activeBuffers.forEach(b -> {
                if (!b.isEmpty()) {
                    rawTraces.add(new RawTrace(b.start, b.cursor, b.events));
                }
            });
            detector.run(crntState.initNextTraceWindow(rawTraces));
        } catch (Throwable e) {
            e.printStackTrace();
            /* cannot use System.exit because it may lead to deadlock */
            Runtime.getRuntime().halt(1);
        }
    }

    @Override
    public void log(EventType eventType, int locId, int addr1, int addr2, long value1, long value2) {
        threadLocalBuffer.get().append(eventType, locId, addr1, addr2, value1, value2);
    }

    /**
     * Thread-local buffer used to store events generated by a thread.
     * <p>
     * Besides the thread associated with this buffer, thread responsible for
     * running race detection at the end of each window also accesses this
     * buffer through {@link Buffer#consume()} and {@link Buffer#isEmpty()}.
     * <p>
     * The key to avoid data-race is to use a sufficiently large circular array
     * to store events so that:
     * <li>{@link Buffer#start} is only accessed by the race detection thread;</li>
     * <li>{@link Buffer#end} is only accessed by the logging thread;</li>
     * <li>{@code start} and {@code end} never overlap.</li>
     * <p>
     * This way, the conflict accesses can only occur at {@link Buffer#cursor}
     * between {@link Buffer#finalizeEvents()} and
     * {@code Buffer.consume()/isEmpty()}. However, the race detection thread
     * only accesses {@code cursor} after blocking the GID counter and all
     * on-going {@code finalizeEvents()} finish, while the logging thread only
     * accesses {@code cursor} after successfully acquiring GIDs and before
     * incrementing the {@link finalized} counter. Therefore, it is impossible
     * for the two threads to access {@code cursor} concurrently.
     *
     * @author YilongL
     */
    private class Buffer {

        /**
         * Maximum number of events that can be delayed to acquire GID.
         */
        static final int THRESHOLD = 16;

        final long tid;

        /**
         * Circular array used to store events.
         */
        final Event[] events;

        /**
         * The (inclusive) start index of the events in circular array.
         */
        int start;

        /**
         * The (exclusive) end index of the events that have acquired GIDs
         * (i.e., finalized events), and the (inclusive) start index of the
         * events that have not yet acquired GIDs.
         */
        int cursor;

        /**
         * The (exclusive) end index of the events in circular array.
         */
        int end;

        Buffer(int bound) {
            tid = Thread.currentThread().getId();
            events = new Event[1 << (32 - Integer.numberOfLeadingZeros(bound + THRESHOLD - 1))];
            for (int i = 0; i < events.length; i++) {
                events[i] = new Event();
                events[i].setTID(tid);
            }
        }

        /**
         * Returns if there is more finalized events.
         */
        boolean isEmpty() {
            return cursor == start;
        }

        /**
         * Consumes all finalized events.
         */
        void consume() {
            start = cursor;
        }

        /**
         * Appends a new event to the end of the circular array and finalizes
         * pending events when necessary.
         * <p>
         * Basically, there are two types of events that can be delayed to be
         * finalized without compromising the soundness of our model:
         * <li>thread-local events that have no effects outside its thread;</li>
         * <li>events that are logged only after they happen.</li>
         * <p>
         * Theoretically speaking, these two types of events can be delayed as
         * much as we like. But in practice, we use the {@link #THRESHOLD} to
         * limit the maximum number of events that can be delayed to make the
         * logged trace look closer to the execution.
         */
        void append(EventType eventType, int locId, int addr1, int addr2, long value1, long value2) {
            switch (eventType) {
            case READ:
            case WRITE_LOCK:
            case READ_LOCK:
            case JOIN:
            case CLINIT_ENTER:
            case CLINIT_EXIT:
            case INVOKE_METHOD:
            case FINISH_METHOD:
                log(eventType, locId, addr1, addr2, value1);
                if (numOfUnfinalizedEvents() >= THRESHOLD) {
                    finalizeEvents();
                }
                break;
            case WRITE:
            case WRITE_UNLOCK:
            case READ_UNLOCK:
            case START:
                log(eventType, locId, addr1, addr2, value1);
                finalizeEvents();
                break;
            case ATOMIC_READ:
                log(EventType.WRITE_LOCK,   locId, ATOMIC_LOCK_C, addr1, 0);
                log(EventType.READ,         locId, addr1, addr2, value1);
                log(EventType.WRITE_UNLOCK, locId, ATOMIC_LOCK_C, addr1, 0);
                finalizeEvents();
                break;
            case ATOMIC_WRITE:
                log(EventType.WRITE_LOCK,   locId, ATOMIC_LOCK_C, addr1, 0);
                log(EventType.WRITE,        locId, addr1, addr2, value1);
                log(EventType.WRITE_UNLOCK, locId, ATOMIC_LOCK_C, addr1, 0);
                finalizeEvents();
                break;
            case ATOMIC_READ_THEN_WRITE:
                log(EventType.WRITE_LOCK,   locId, ATOMIC_LOCK_C, addr1, 0);
                log(EventType.READ,         locId, addr1, addr2, value1);
                log(EventType.WRITE,        locId, addr1, addr2, value2);
                log(EventType.WRITE_UNLOCK, locId, ATOMIC_LOCK_C, addr1, 0);
                finalizeEvents();
                break;
            default:
                assert false;
            }
        }

        private void log(EventType eventType, int locId, int addr1, int addr2, long value) {
            Event event = events[end];
            end = inc(end);
            event.setLocId(locId);
            event.setAddr((long) addr1 << 32 | addr2 & 0xFFFFFFFFL);
            event.setValue(value);
            event.setType(eventType);
        }

        void finalizeEvents() {
            int d = numOfUnfinalizedEvents();
            long gid = claimGID(d);
            int p = cursor;
            cursor = end;
            for (int i = 0; i < d; i++) {
                events[p].setGID(gid++);
                p = inc(p);
            }
            finalized.add(d);
        }

        int inc(int p) {
            return (p + 1 == events.length ? 0 : p + 1);
        }

        int numOfUnfinalizedEvents() {
            int d = end - cursor;
            return d >= 0 ? d : d + events.length;
        }

    }

}
