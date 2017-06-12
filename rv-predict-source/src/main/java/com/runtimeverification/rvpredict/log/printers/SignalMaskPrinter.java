package com.runtimeverification.rvpredict.log.printers;

import com.runtimeverification.rvpredict.log.EventPrinter;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

import java.util.function.Function;

public class SignalMaskPrinter extends EventPrinter {
    private final Function<ReadonlyEventInterface, Long> maskExtractor;

    public SignalMaskPrinter(String type, Function<ReadonlyEventInterface, Long> maskExtractor) {
        super(type);
        this.maskExtractor = maskExtractor;
    }

    @Override
    protected String getEventContent(ReadonlyEventInterface event) {
        return String.format(" 0x%016x ", maskExtractor.apply(event));
    }
}
