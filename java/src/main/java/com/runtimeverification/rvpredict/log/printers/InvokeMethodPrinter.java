package com.runtimeverification.rvpredict.log.printers;

import com.runtimeverification.rvpredict.log.EventPrinter;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

import java.util.OptionalLong;

public class InvokeMethodPrinter extends EventPrinter {
    public InvokeMethodPrinter() {
        super("invokemethod");
    }

    @Override
    protected String getEventContent(ReadonlyEventInterface event) {
        OptionalLong callSiteAddress = event.getCallSiteAddress();
        if (callSiteAddress.isPresent()) {
            return String.format(
                    " cfa:0x%016x callsite:0x%016x ",
                    event.getCanonicalFrameAddress(),
                    callSiteAddress.getAsLong());
        }
        return String.format(
                " cfa:0x%016x callsite:absent ",
                event.getCanonicalFrameAddress());
    }
}
