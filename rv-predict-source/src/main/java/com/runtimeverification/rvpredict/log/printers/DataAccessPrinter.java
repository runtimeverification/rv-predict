package com.runtimeverification.rvpredict.log.printers;

import com.runtimeverification.rvpredict.log.EventPrinter;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

public class DataAccessPrinter extends EventPrinter {
    public DataAccessPrinter(String type) {
        super(type);
    }

    @Override
    protected String getEventContent(ReadonlyEventInterface event) {
        return String.format(" [0x%016x], 0x%-16x ", event.getObjectHashCode(), event.getDataValue());
    }
}