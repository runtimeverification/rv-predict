package com.runtimeverification.rvpredict.log.printers;

import com.runtimeverification.rvpredict.log.EventPrinter;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

public class SignalNumberPrinter extends EventPrinter {
    public SignalNumberPrinter(String type) {
        super(type);
    }

    @Override
    protected String getEventContent(ReadonlyEventInterface event) {
        return String.format(" S<%d> ", event.getSignalNumber());
    }
}
