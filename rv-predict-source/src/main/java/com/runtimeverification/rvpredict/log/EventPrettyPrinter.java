package com.runtimeverification.rvpredict.log;

import java.util.concurrent.locks.Condition;

public class EventPrettyPrinter {
    void append(StringBuffer sb, Event event) {
        switch (event.getType()) {
            case READ:
                break;
            case WRITE:
                break;
            case ATOMIC_READ:
                break;
            case ATOMIC_WRITE:
                break;
            case ATOMIC_READ_THEN_WRITE:
                break;
            case WRITE_LOCK:
                break;
                        WRITE_LOCK,

                        /**
                         * Event generated before releasing an intrinsic lock or write lock.
                         */
                        WRITE_UNLOCK,

                        /**
                         * Event generated after acquiring a read lock, i.e.,
                         * {@code ReadWriteLock#readLock()#lock()}.
                         */
                        READ_LOCK,

                        /**
                         * Event generated before releasing a read lock, i.e.,
                         * {@code ReadWriteLock#readLock()#unlock()}.
                         */
                        READ_UNLOCK,

                        /**
                         * Event generated before calling {@link Object#wait()} or
                         * {@link Condition#await()}.
                         */
                        WAIT_RELEASE,

                        /**
                         * Event generated after a thread is awakened from {@link Object#wait()} or
                         * {@link Condition#await()} for whatever reason (e.g., spurious wakeup,
                         * being notified, or being interrupted).
                         */
                        WAIT_ACQUIRE,

                        /**
                         * Event generated before calling {@code Thread#start()}.
                         */
                        START_THREAD,

                        /**
                         * Event generated after a thread is awakened from {@code Thread#join()}
                         * because the joining thread finishes.
                         */
                        JOIN_THREAD,

                        /**
                         * Event generated after entering the class initializer code, i.e.
                         * {@code <clinit>}.
                         */
                        CLINIT_ENTER,

                        /**
                         * Event generated right before exiting the class initializer code, i.e.
                         * {@code <clinit>}.
                         */
                        CLINIT_EXIT,

                        INVOKE_METHOD,

                        FINISH_METHOD,

                        PRE_LOCK,


                        BEGIN_THREAD,
                        END_THREAD,

                        ESTABLISH_SIGNAL,
                        DISESTABLISH_SIGNAL,
                        WRITE_SIGNAL_MASK,
                        READ_SIGNAL_MASK,
                        READ_WRITE_SIGNAL_MASK,
                        BLOCK_SIGNALS,
                        UNBLOCK_SIGNALS,

                        ENTER_SIGNAL,
                        EXIT_SIGNAL;
        }
    }
}
