package com.runtimeverification.rvpredict.log.printers;

import com.runtimeverification.rvpredict.log.EventPrinter;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

public class ReadWriteSignalMaskPrinter extends EventPrinter {
    public ReadWriteSignalMaskPrinter(String type) {
        super(type);
    }

    @Override
    protected String getEventContent(ReadonlyEventInterface event) {
        return String.format(
                " (read:0x%016x write:0x%016x) ", event.getFullReadSignalMask(), event.getFullWriteSignalMask());
    }
}
