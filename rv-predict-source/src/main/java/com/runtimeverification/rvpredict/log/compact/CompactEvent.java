package com.runtimeverification.rvpredict.log.compact;

import com.runtimeverification.rvpredict.log.EventType;

public abstract class CompactEvent {
    /*
    public enum Type {
        READ,
        WRITE,


        / **
         * Event generated after acquiring an intrinsic lock or write lock.
         * /
        WRITE_LOCK,

        /**
         * Event generated before releasing an intrinsic lock or write lock.
         * /
        WRITE_UNLOCK,

        /**
         * Event generated after acquiring a read lock, i.e.,
         * {@code ReadWriteLock#readLock()#lock()}.
         * /
        READ_LOCK,

        /**
         * Event generated before releasing a read lock, i.e.,
         * {@code ReadWriteLock#readLock()#unlock()}.
         * /
        READ_UNLOCK,

        /**
         * Event generated before calling {@link Object#wait()} or
         * {@link Condition#await()}.
         * /
        WAIT_RELEASE,

        /**
         * Event generated after a thread is awakened from {@link Object#wait()} or
         * {@link Condition#await()} for whatever reason (e.g., spurious wakeup,
         * being notified, or being interrupted).
         * /
        WAIT_ACQUIRE,

        /**
         * Event generated before calling {@code Thread#start()}.
         * /
        START_THREAD,

        /**
         * Event generated after a thread is awakened from {@code Thread#join()}
         * because the joining thread finishes.
         * /
        JOIN_THREAD,

        /**
         * Event generated after entering the class initializer code, i.e.
         * {@code <clinit>}.
         * /
        CLINIT_ENTER,

        /**
         * Event generated right before exiting the class initializer code, i.e.
         * {@code <clinit>}.
         * /
        CLINIT_EXIT,

        INVOKE_METHOD,
        FINISH_METHOD,

        /**
         * Event generated before acquiring of any type of lock is attempted.
         * Required by, and only used for, deadlock detection, where the intention
         * to acquire a lock is more relevant than actually the acquisition itself.
         * /
        //TODO(TraianSF): Consider moving this with the other SYNC events
        PRE_LOCK,
    }*/

    private final long id;
    private final long threadId;
    private final EventType compactType;

    CompactEvent(Context context, EventType compactType) {
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

    EventType getCompactType() {return compactType;}

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
