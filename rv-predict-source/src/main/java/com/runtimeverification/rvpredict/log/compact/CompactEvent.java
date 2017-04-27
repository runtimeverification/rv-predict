package com.runtimeverification.rvpredict.log.compact;

import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ReadonlyEventDecorator;
import com.runtimeverification.rvpredict.log.ReadonlyEvent;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

public abstract class CompactEvent extends ReadonlyEvent {
    private final long eventId;
    private final long locationId;
    private final long threadId;
    private final EventType type;

    CompactEvent(Context context, EventType type) {
        this.eventId = context.newId();
        this.locationId = context.getPC();
        this.threadId = context.getThreadId();
        this.type = type;
    }

    @Override
    public long getEventId() {
        return eventId;
    }

    // TODO(virgil): Convert getLocationId to long.
    @Override
    public int getLocationId() {
        return (int)locationId;
    }

    @Override
    public long getThreadId() {
        return threadId;
    }

    @Override
    public EventType getType() {return type;}

    int getDataSizeInBytes() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }

    @Override
    public long getDataAddress() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }

    @Override
    public long getDataValue() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }

    @Override
    public long getSyncedThreadId() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }

    @Override
    public long getSyncObject() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }

    long getSignalNumber() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }
    long getSignalMask() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }
    long getSignalHandlerAddress() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }

    @Override
    public String getLockRepresentation() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }

    @Override
    public long unsafeGetAddress() {
        return 0;
    }

    @Override
    public long unsafeGetDataValue() {
        return 0;
    }

    @Override
    public ReadonlyEvent copy() {
        return this;
    }

    @Override
    public ReadonlyEventInterface destructiveWithLocationId(int locationId) {
        return new ReadonlyEventDecorator(this) {
            @Override
            public int getLocationId() {
                return locationId;
            }
        };
    }

    @Override
    public ReadonlyEventInterface destructiveWithEventId(long locationId) {
        return new ReadonlyEventDecorator(this) {
            @Override
            public long getEventId() {
                return eventId;
            }
        };
    }
}
