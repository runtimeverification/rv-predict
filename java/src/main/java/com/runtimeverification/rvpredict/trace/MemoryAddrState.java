package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;
import com.runtimeverification.rvpredict.util.Constants;

public class MemoryAddrState {
    private int reader1 = Constants.INVALID_TTID, reader2 = Constants.INVALID_TTID;
    private int writer1 = Constants.INVALID_TTID, writer2 = Constants.INVALID_TTID;

    void touch(ReadonlyEventInterface event, int ttid) {
        if (event.isRead()) {
            if (reader1 == Constants.INVALID_TTID) {
                reader1 = ttid;
            } else if (reader1 != ttid && reader2 == Constants.INVALID_TTID) {
                reader2 = ttid;
            }
        } else {
            if (writer1 == Constants.INVALID_TTID) {
                writer1 = ttid;
            } else if (writer1 != ttid && writer2 == Constants.INVALID_TTID) {
                writer2 = ttid;
            }
        }
    }

    boolean isWriteShared() {
        if (writer1 == Constants.INVALID_TTID) { // most common case: no write at all
            return false;
        }
        return writer2 != Constants.INVALID_TTID
                || (reader1 != Constants.INVALID_TTID && reader1 != writer1)
                || reader2 != Constants.INVALID_TTID;
    }

}