package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.log.Event;

public class MemoryAddrState {
    private long reader1, reader2;
    private long writer1, writer2;

    public void touch(Event event) {
        long tid = event.getTID();
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
        if (writer1 == 0) { // most common case: no write at all
            return false;
        } else {
            return writer2 != 0 || reader1 != 0 && reader1 != writer1 || reader2 != 0;
        }
    }

}