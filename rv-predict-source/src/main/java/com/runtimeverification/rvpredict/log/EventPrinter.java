package com.runtimeverification.rvpredict.log;

public class EventPrinter {
    private final String eventType;

    protected EventPrinter(String eventType) {
        this.eventType = eventType;
    }

    public String print(ReadonlyEventInterface event) {
        return String.format("%s%-50s  %s", getEventPrefix(event), getEventContent(event), getEventSuffix(event));
    }

    protected String getEventContent(ReadonlyEventInterface event) {
        return " ";
    }

    private String getEventPrefix(ReadonlyEventInterface event) {
        return String.format(
                "T%d.%d %9s, %20s",
                event.getOriginalThreadId(), event.getSignalDepth(), event.getEventId(),
                eventType);
    }
    private String getEventSuffix(ReadonlyEventInterface event) {
        return String.format("{0x%016x}", event.getLocationId());
    }
}
