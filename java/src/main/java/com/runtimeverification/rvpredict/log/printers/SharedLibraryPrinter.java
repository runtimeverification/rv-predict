package com.runtimeverification.rvpredict.log.printers;

import com.runtimeverification.rvpredict.log.EventPrinter;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

public class SharedLibraryPrinter extends EventPrinter {
    public SharedLibraryPrinter() {
        super("shared-library");
    }

    @Override
    protected String getEventContent(ReadonlyEventInterface event) {
        return event.getSharedLibraryId() + " " + event.getSharedLibraryName();
    }
}
