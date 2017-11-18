package com.runtimeverification.rvpredict.log.printers;

import com.runtimeverification.rvpredict.log.EventPrinter;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

public class SharedLibrarySegmentPrinter extends EventPrinter {
    public SharedLibrarySegmentPrinter() {
        super("shared-library-segment");
    }

    @Override
    protected String getEventContent(ReadonlyEventInterface event) {
        return event.getSharedLibraryId() + ": " + event.getSharedLibrarySegmentStart()
                + " -> " + event.getSharedLibrarySegmentEnd();
    }

}
