package com.runtimeverification.rvpredict.trace;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

public class ThreadInfos {
    /**
     * Map from a thread ID to the information about that thread.
     */
    private final Map<Integer, ThreadInfo> ttidToThreadInfo = new HashMap<>();
    private final Map<Long, Integer> otidToTtid = new HashMap<>();

    OptionalInt getTtidFromOtid(long originalThreadId) {
        Integer ttid = otidToTtid.get(originalThreadId);
        if (ttid == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(ttid);
    }

    public void registerThreadInfo(ThreadInfo info) {
        ttidToThreadInfo.put(info.getId(), info);
        if (info.getSignalDepth() == 0) {
            otidToTtid.put(info.getOriginalThreadId(), info.getId());
        }
    }

    public ThreadInfo getThreadInfo(int ttid) {
        return ttidToThreadInfo.get(ttid);
    }

    public Map<Long, Integer> getOtidToTtid() {
        return otidToTtid;
    }
}
