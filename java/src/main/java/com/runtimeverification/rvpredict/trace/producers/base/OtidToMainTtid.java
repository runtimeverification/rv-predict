package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.producerframework.LeafProducer;

import java.util.Map;
import java.util.OptionalInt;

/**
 * Map from a thread's ID to the internal id assigned when preparing the trace.
 *
 * A signal interrupting a thread has the same original thread ID as the interrupted thread,
 * so this is restricted only to normal threads.
 */
public class OtidToMainTtid extends LeafProducer<Map<Long, Integer>> {
    public OptionalInt getTtid(long otid) {
        Integer ttid = get().get(otid);
        if (ttid == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(ttid);
    }
}
