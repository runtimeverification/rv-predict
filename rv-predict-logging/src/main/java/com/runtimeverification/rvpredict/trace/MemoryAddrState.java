package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.log.Event;

public class MemoryAddrState {
    private Event lastWrite;
    private long initVal;
    private long reader1, reader2;
    private long writer1, writer2;

    public void setInitialValue(long initVal) {
        this.initVal = initVal;
    }

    public long initialValue() {
        return initVal;
    }

    public Event lastWrite() {
        return lastWrite;
    }

    public void touch(Event event) {
        long tid = event.getTID();
        if (event.isRead()) {
            if (reader1 == 0) {
                reader1 = tid;
            } else if (reader1 != tid && reader2 == 0) {
                reader2 = tid;
            }
        } else {
            if (lastWrite == null || lastWrite.getGID() < event.getGID()) {
                lastWrite = event;
            }
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