package com.runtimeverification.rvpredict.log.printers;

import com.runtimeverification.rvpredict.log.EventPrinter;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

public class ReadPrinter implements EventPrinter {
    @Override
    public String print(ReadonlyEventInterface event) {
        return String.format("%20d read([0x%016x], 0x%16x) {0x%016x}", event.getEventId(), event.get);
    }
}
