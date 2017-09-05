package com.runtimeverification.rvpredict.order;

import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.metadata.Metadata;

import java.util.HashMap;
import java.util.Map;

public class HappensBefore {
    private Map<Long, VectorClock> unlocks = new HashMap<>();
    private Map<Long, VectorClock> volatileWrite = new HashMap<>();
    private Map<Long, VectorClock> threadStarts = new HashMap<>();
    private Map<Long, VectorClock> threadCurrent = new HashMap<>();

    private ThreadLocal<VectorClock> localClock = ThreadLocal.withInitial(() -> {
        long tid = Thread.currentThread().getId();
        VectorClock clock = new VectorClock(threadStarts.get(tid));
        clock.increment(tid);
        threadCurrent.put(tid, clock);
        return clock;
    });


    private final Metadata metadata;

    public HappensBefore(Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Incorporates current event into the Happens-Before ordering.
     *
     * Note: Assumes events are sent in increasing order of their gid.
     *
     * @param eventType  type of the event being handled
     * @param tid        id of the thread emitting the event
     * @param address    id of concurrent object affected by the event (dependent on eventType)
     * @return           the clocks of the current thread upon processing the event
     */
    public VectorClock log(EventType eventType, long tid, long address) {
        VectorClock clock = localClock.get();
        clock.increment(tid);
        switch (eventType) {
        case READ:
            if (metadata.isVolatile(address)) {
                clock.update(volatileWrite.get(address));
            }
            break;
        case WRITE:
            if (metadata.isVolatile(address)) {
                updatePreviousClock(address, clock, volatileWrite);
            }
            break;
        case WRITE_LOCK:
        case READ_LOCK:
            clock.update(unlocks.get(address));
            break;
        case WRITE_UNLOCK:
        case READ_UNLOCK:
            updatePreviousClock(address, clock, unlocks);
            break;
        case WAIT_ACQUIRE:
        case WAIT_RELEASE:
            break;
        case START_THREAD:
            threadStarts.put(tid, clock);
            break;
        case JOIN_THREAD:
            clock.update(threadCurrent.get(address));
            break;
        case CLINIT_ENTER:
        case CLINIT_EXIT:
            break;
        case INVOKE_METHOD:
        case FINISH_METHOD:
            break;
        default:
            assert false;
        }
        return clock;
    }

    private void updatePreviousClock(long address, VectorClock clock, Map<Long, VectorClock> previousClocks) {
        VectorClock vClock = previousClocks.get(address);
        if (vClock != null) {
            vClock.update(clock);
        } else {
            vClock = new VectorClock(clock);
        }
        previousClocks.put(address, vClock);
    }


}
