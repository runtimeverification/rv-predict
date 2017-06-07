package com.runtimeverification.rvpredict.log.printers;

import com.runtimeverification.rvpredict.log.EventPrinter;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

public class LockPrinter extends EventPrinter {
    public LockPrinter(String type) {
        super(type);
    }

    @Override
    protected String getEventContent(ReadonlyEventInterface event) {
        // TODO(virgil): Separate formatting for atomic locks.
        return String.format(" [0x%016x] ", event.getLockId());
    }
}
