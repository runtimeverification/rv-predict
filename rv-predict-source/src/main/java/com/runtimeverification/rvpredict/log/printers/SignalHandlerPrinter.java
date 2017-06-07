package com.runtimeverification.rvpredict.log.printers;

import com.runtimeverification.rvpredict.log.EventPrinter;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

public class SignalHandlerPrinter extends EventPrinter {
    public SignalHandlerPrinter(String type) {
        super(type);
    }

    @Override
    protected String getEventContent(ReadonlyEventInterface event) {
        return String.format(" <S%d>({0x%016x}) ", event.getSignalNumber(), event.getSignalHandlerAddress());
    }
}
