package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

public class MemoryAddrState {
    private int reader1, reader2;
    private int writer1, writer2;

    public void touch(ReadonlyEventInterface event, int ttid) {
        if (event.isRead()) {
            if (reader1 == 0) {
                reader1 = ttid;
            } else if (reader1 != ttid && reader2 == 0) {
                reader2 = ttid;
            }
        } else {
            if (writer1 == 0) {
                writer1 = ttid;
            } else if (writer1 != ttid && writer2 == 0) {
                writer2 = ttid;
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