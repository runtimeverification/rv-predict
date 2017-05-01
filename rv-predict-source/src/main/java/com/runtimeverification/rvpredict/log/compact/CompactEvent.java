package com.runtimeverification.rvpredict.log.compact;

import com.google.common.annotations.VisibleForTesting;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.log.ReadonlyEventDecorator;
import com.runtimeverification.rvpredict.log.ReadonlyEvent;
import com.runtimeverification.rvpredict.log.ReadonlyEventInterface;

public abstract class CompactEvent extends ReadonlyEvent {
    private final long eventId;
    private final long locationId;
    private final long threadId;
    private final int signalDepth;
    private final EventType type;

    CompactEvent(Context context, EventType type) {
        this(context.newId(), context.getPC(), context.getThreadId(), context.getSignalDepth(), type);
    }

    @VisibleForTesting
    public CompactEvent(long eventId, long locationId, long threadId, int signalDepth, EventType type) {
        this.eventId = eventId;
        this.locationId = locationId;
        this.threadId = threadId;
        this.signalDepth = signalDepth;
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
    public int getSignalDepth() {
        return signalDepth;
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
    long getPartialSignalMask() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }
    long getFullReadSignalMask() {
        throw new UnsupportedOperationException("Unsupported operation for " + getType());
    }
    long getFullWriteSignalMask() {
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
