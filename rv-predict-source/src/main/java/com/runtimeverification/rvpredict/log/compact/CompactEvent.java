package com.runtimeverification.rvpredict.log.compact;

public abstract class CompactEvent {
    public enum Type {
        READ,
        WRITE,

        LOCK,
        UNLOCK,

        FORK,
        /**
         * Event generated after a thread is awakened from {@code Thread#join()}
         * because the joining thread finishes.
         */
        JOIN_THREAD,

        ESTABLISH_SIGNAL,
        DISESTABLISH_SIGNAL,

        ENTER_SIGNAL,
        EXIT_SIGNAL,

        ENTER_FUNCTION,
        EXIT_FUNCTION, BEGIN_THREAD, END_THREAD,
    }

    private final long id;
    private final long threadId;
    private final Type compactType;

    CompactEvent(Context context, Type compactType) {
        this.id = context.newId();
        this.threadId = context.getThreadId();
        this.compactType = compactType;
    }

    long getId() {
        return id;
    }

    long getThreadId() {
        return threadId;
    }

    Type getCompactType() {return compactType;}

    int getDataSizeInBytes() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long getDataAddress() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long getDataValue() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long getSignalNumber() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long getSignalMask() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long getSignalHandlerAddress() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long getOtherThreadId() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long getLockAddress() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
}
