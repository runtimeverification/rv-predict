package com.runtimeverification.rvpredict.order;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.metadata.MetadataInterface;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of the happens-before ordering.
 * Should implement the ordering described by the Java memory model.
 * https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4.5
 */
public class JavaHappensBefore implements VectorClockOrderInterface {
    private Map<Long, Map<Integer,VectorClock>> unlocks = new HashMap<>();
    private Map<Long, Map<Integer, VectorClock>> volatileWrites = new HashMap<>();
    private Map<Integer, VectorClock> threadStarts = new HashMap<>();
    private Map<Integer, VectorClock> threadCurrent = new HashMap<>();

    private final MetadataInterface metadata;

    public JavaHappensBefore(MetadataInterface metadata) {
        this.metadata = metadata;
    }

    private VectorClock getClock(int tid) {
        // An action that starts a thread synchronizes-with the first action in the thread it starts.
        return threadCurrent.computeIfAbsent(tid, k -> new VectorClock(threadStarts.get(k)));
    }

    @Override
    public VectorClock log(ReadonlyEventInterface event) {
        int tid = Math.toIntExact(event.getOriginalThreadId());
        VectorClock clock = getClock(tid);
        clock.increment(tid);
        switch (event.getType()) {
        case READ:
            if (metadata.isVolatile(event.getDataInternalIdentifier())) {
                // A write to a volatile variable v (§8.3.1.4) synchronizes-with all subsequent reads of v by any thread
                updateReadClock(event.getDataInternalIdentifier(), tid, clock, volatileWrites);
            }
            break;
        case WRITE:
            if (metadata.isVolatile(event.getDataInternalIdentifier())) {
                volatileWrites.computeIfAbsent(event.getDataInternalIdentifier(), (k) -> new HashMap<>())
                        .put(tid, new VectorClock(clock));
            }
            break;
        case WRITE_LOCK:
        case READ_LOCK:
            // An unlock action on monitor m synchronizes-with all subsequent lock actions on m
            updateReadClock(event.getSyncObject(), tid, clock, unlocks);
            break;
        case WRITE_UNLOCK:
        case READ_UNLOCK:
            unlocks.computeIfAbsent(event.getSyncObject(), (k) -> new HashMap<>()).put(tid, new VectorClock(clock));
            break;
        case WAIT_ACQUIRE:
        case WAIT_RELEASE:
            break;
        case START_THREAD:
            threadStarts.put(Math.toIntExact(event.getSyncedThreadId()), new VectorClock(clock));
            break;
        case JOIN_THREAD:
            // The final action in a thread T1 synchronizes-with any action in another thread T2 that detects that
            // T1 has terminated.
            // TODO(traiansf): handle isAlive()
            clock.update(threadCurrent.get(Math.toIntExact(event.getSyncedThreadId())));
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

    private void updateReadClock(
            long object, int tid, VectorClock clock, Map<Long, Map<Integer, VectorClock>> writeClocks) {
        writeClocks.computeIfAbsent(object, (k) -> new HashMap<>()).forEach((writeTid, writeClock) -> {
            if (tid != writeTid) clock.update(writeClock);
        });
    }

}
