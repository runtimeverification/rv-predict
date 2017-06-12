package com.runtimeverification.rvpredict.log.printers;

import com.runtimeverification.rvpredict.log.EventPrinter;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

public class EstablishSignalPrinter extends EventPrinter {
    public EstablishSignalPrinter(String type) {
        super(type);
    }

    @Override
    protected String getEventContent(ReadonlyEventInterface event) {
        return String.format(" <S%d>(mask:0x%016x,({0x%016x})) ",
                event.getSignalNumber(), event.getFullWriteSignalMask(), event.getSignalHandlerAddress());
    }
}
