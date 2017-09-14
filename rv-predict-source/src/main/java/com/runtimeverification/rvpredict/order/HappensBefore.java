package com.runtimeverification.rvpredict.order;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.metadata.MetadataInterface;

import java.util.HashMap;
import java.util.Map;

public class HappensBefore implements VectorClockOrderInterface {
    private Map<Long, VectorClock> unlocks = new HashMap<>();
    private Map<Long, VectorClock> volatileWrite = new HashMap<>();
    private Map<Integer, VectorClock> threadStarts = new HashMap<>();
    private Map<Integer, VectorClock> threadCurrent = new HashMap<>();

    private final MetadataInterface metadata;

    public HappensBefore(MetadataInterface metadata) {
        this.metadata = metadata;
    }

    private VectorClock getClock(int tid) {
        VectorClock clock = threadCurrent.get(tid);
        if (clock == null) {
            clock = new VectorClock(threadStarts.get(tid));
            threadCurrent.put(tid, clock);
        }
        return clock;
    }

    @Override
    public VectorClock log(ReadonlyEventInterface event) {
        int tid = (int)event.getOriginalThreadId();
        VectorClock clock = getClock(tid);
        clock.increment(tid);
        switch (event.getType()) {
        case READ:
            if (metadata.isVolatile(event.getDataInternalIdentifier())) {
                clock.update(volatileWrite.get(event.getDataInternalIdentifier()));
            }
            break;
        case WRITE:
            if (metadata.isVolatile(event.getDataInternalIdentifier())) {
                updatePreviousClock(event.getDataInternalIdentifier(), clock, volatileWrite);
            }
            break;
        case WRITE_LOCK:
        case READ_LOCK:
            clock.update(unlocks.get(event.getSyncObject()));
            break;
        case WRITE_UNLOCK:
        case READ_UNLOCK:
            updatePreviousClock(event.getSyncObject(), clock, unlocks);
            break;
        case WAIT_ACQUIRE:
        case WAIT_RELEASE:
            break;
        case START_THREAD:
            threadStarts.put((int)event.getSyncedThreadId(), new VectorClock(clock));
            break;
        case JOIN_THREAD:
            clock.update(threadCurrent.get((int)event.getSyncedThreadId()));
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

    private void updatePreviousClock(long clockId, VectorClock clock, Map<Long, VectorClock> previousClocks) {
        VectorClock vClock = previousClocks.get(clockId);
        if (vClock != null) {
            vClock.update(clock);
        } else {
            vClock = new VectorClock(clock);
            previousClocks.put(clockId, vClock);
        }
    }

}
