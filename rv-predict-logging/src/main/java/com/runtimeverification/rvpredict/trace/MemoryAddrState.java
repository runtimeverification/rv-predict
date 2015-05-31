package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.log.Event;

public class MemoryAddrState {
    private Event lastAccess;
    private final long initVal;
    private long reader1, reader2;
    private long writer1, writer2;

    public MemoryAddrState(long initVal) {
        this.initVal = initVal;
    }

    public long initialValue() {
        return initVal;
    }

    public long finalValue() {
        /* use the value of the last access to update state, instead of
         * that of the last write, to recover from potential missing
         * write events */
        return lastAccess.getValue();
    }

    public void touch(Event event) {
        long tid = event.getTID();
        if (lastAccess == null || lastAccess.getGID() < event.getGID()) {
            lastAccess = event;
        }
        if (event.isRead()) {
            if (reader1 == 0) {
                reader1 = tid;
            } else if (reader1 != tid && reader2 == 0) {
                reader2 = tid;
            }
        } else {
            if (writer1 == 0) {
                writer1 = tid;
            } else if (writer1 != tid && writer2 == 0) {
                writer2 = tid;
            }
        }
    }

    public boolean isWriteShared() {
        return writer2 != 0 || writer1 != 0
                && (reader1 != 0 && reader1 != writer1 || reader2 != 0 && reader2 != writer1);
    }

}