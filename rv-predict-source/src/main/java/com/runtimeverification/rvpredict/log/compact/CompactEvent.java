package com.runtimeverification.rvpredict.log.compact;

import com.runtimeverification.rvpredict.log.compact.readers.AtomicReadModifyWriteReader;
import com.runtimeverification.rvpredict.log.compact.readers.ChangeOfGenerationReader;
import com.runtimeverification.rvpredict.log.compact.readers.DataManipulationReader;
import com.runtimeverification.rvpredict.log.compact.readers.LockManipulationReader;
import com.runtimeverification.rvpredict.log.compact.readers.NoDataReader;
import com.runtimeverification.rvpredict.log.compact.readers.SignalDisestablishReader;
import com.runtimeverification.rvpredict.log.compact.readers.SignalEnterReader;
import com.runtimeverification.rvpredict.log.compact.readers.SignalEstablishReader;
import com.runtimeverification.rvpredict.log.compact.readers.SignalMaskMemoizationReader;
import com.runtimeverification.rvpredict.log.compact.readers.SignalMaskReader;
import com.runtimeverification.rvpredict.log.compact.readers.SignalOutstandingDepthReader;
import com.runtimeverification.rvpredict.log.compact.readers.ThreadBeginReader;
import com.runtimeverification.rvpredict.log.compact.readers.ThreadSyncReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public abstract class CompactEvent {

    /*
    public enum Atomicity {
        ATOMIC,
        NOT_ATOMIC,
    }

    public enum DataManipulationType {
        LOAD,
        STORE,
    }

    public enum ThreadSyncType {
        FORK,
        JOIN,
        SWITCH,
    }
    public enum LockManipulationType {
        LOCK,
        UNLOCK,
    }
    */

    public enum CompactType {
        READ,
        WRITE,

        // ATOMIC_READ,
        // ATOMIC_WRITE,
        ATOMIC_READ_THEN_WRITE,

        LOCK,

        UNLOCK,

        /**
         * Event generated before calling {@code Thread#start()}.
         */
        START,

        /**
         * Event generated after a thread is awakened from {@code Thread#join()}
         * because the joining thread finishes.
         */
        JOIN,

        INVOKE_METHOD,
        FINISH_METHOD, ENTER_SIGNAL, ESTABLISH_SIGNAL, EXIT_FUNCTION, ENTER_FUNCTION, DISESTABLISH_SIGNAL, EXIT_SIGNAL, FORK,
    }

    private final long id;
    private final long threadId;
    private final CompactType compactType;

    CompactEvent(Context context, CompactType compactType) {
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
    CompactType getCompactType() {return compactType;}
    int dataSizeInBytes() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long dataAddress() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long value() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long signalNumber() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long signalMask() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long signalHandlerAddress() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long otherThreadId() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
    long lockAddress() {
        throw new UnsupportedOperationException("Unsupported operation for " + getCompactType());
    }
}
