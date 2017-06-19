package com.runtimeverification.rvpredict.log;

import com.runtimeverification.rvpredict.log.printers.DataAccessPrinter;
import com.runtimeverification.rvpredict.log.printers.EstablishSignalPrinter;
import com.runtimeverification.rvpredict.log.printers.InvokeMethodPrinter;
import com.runtimeverification.rvpredict.log.printers.LockPrinter;
import com.runtimeverification.rvpredict.log.printers.ReadWriteSignalMaskPrinter;
import com.runtimeverification.rvpredict.log.printers.SignalHandlerPrinter;
import com.runtimeverification.rvpredict.log.printers.SignalMaskPrinter;
import com.runtimeverification.rvpredict.log.printers.SignalNumberPrinter;
import com.runtimeverification.rvpredict.log.printers.ThreadEventPrinter;

import java.util.concurrent.locks.Condition;

/**
 * Enumeration of all types of events considered during logging and prediction.
 *
 * @author TraianSF
 */
public enum EventType {
    READ(new DataAccessPrinter("read")),
    WRITE(new DataAccessPrinter("write")),

    /**
     * Atomic events that are used only in the front-end.
     */
    ATOMIC_READ(new DataAccessPrinter("atomicread")),
    ATOMIC_WRITE(new DataAccessPrinter("atomicwrite")),
    ATOMIC_READ_THEN_WRITE(new DataAccessPrinter("atomicrw")),

    /**
     * Event generated after acquiring an intrinsic lock or write lock.
     */
    WRITE_LOCK(new LockPrinter("writelock")),

    /**
     * Event generated before releasing an intrinsic lock or write lock.
     */
    WRITE_UNLOCK(new LockPrinter("writeunlock")),

    /**
     * Event generated after acquiring a read lock, i.e.,
     * {@code ReadWriteLock#readLock()#lock()}.
     */
    READ_LOCK(new LockPrinter("readlock")),

    /**
     * Event generated before releasing a read lock, i.e.,
     * {@code ReadWriteLock#readLock()#unlock()}.
     */
    READ_UNLOCK(new LockPrinter("readunlock")),

    /**
     * Event generated before calling {@link Object#wait()} or
     * {@link Condition#await()}.
     */
    WAIT_RELEASE(new LockPrinter("waitrelease")),

    /**
     * Event generated after a thread is awakened from {@link Object#wait()} or
     * {@link Condition#await()} for whatever reason (e.g., spurious wakeup,
     * being notified, or being interrupted).
     */
    WAIT_ACQUIRE(new LockPrinter("waitacquire")),

    /**
     * Event generated before calling {@code Thread#start()}.
     */
    START_THREAD(new ThreadEventPrinter("startthread")),

    /**
     * Event generated after a thread is awakened from {@code Thread#join()}
     * because the joining thread finishes.
     */
    JOIN_THREAD(new ThreadEventPrinter("jointhread")),

    /**
     * Event generated after entering the class initializer code, i.e.
     * {@code <clinit>}.
     */
    CLINIT_ENTER(new EventPrinter("classinitenter")),

    /**
     * Event generated right before exiting the class initializer code, i.e.
     * {@code <clinit>}.
     */
    CLINIT_EXIT(new EventPrinter("classinitexit")),

    INVOKE_METHOD(new InvokeMethodPrinter()),

    FINISH_METHOD(new EventPrinter("finishmethod")),

    /**
     * Event generated before acquiring of any type of lock is attempted.
     * Required by, and only used for, deadlock detection, where the intention
     * to acquire a lock is more relevant than actually the acquisition itself.
     */
    //TODO(TraianSF): Consider moving this with the other SYNC events
    PRE_LOCK(new EventPrinter("prelock")),


    BEGIN_THREAD(new EventPrinter("beginthread")),
    END_THREAD(new EventPrinter("endthread")),

    ESTABLISH_SIGNAL(new EstablishSignalPrinter("establishsignal")),
    DISESTABLISH_SIGNAL(new SignalNumberPrinter("disestablishsignal")),
    WRITE_SIGNAL_MASK(new SignalMaskPrinter("writesignalmask", ReadonlyEventInterface::getFullWriteSignalMask)),
    READ_SIGNAL_MASK(new SignalMaskPrinter("readsignalmask", ReadonlyEventInterface::getFullReadSignalMask)),
    READ_WRITE_SIGNAL_MASK(new ReadWriteSignalMaskPrinter("readwritesignalmask")),
    BLOCK_SIGNALS(new SignalMaskPrinter("blocksignals", ReadonlyEventInterface::getPartialSignalMask)),
    UNBLOCK_SIGNALS(new SignalMaskPrinter("unblocksignals", ReadonlyEventInterface::getPartialSignalMask)),

    ENTER_SIGNAL(new SignalHandlerPrinter("entersignal")),
    EXIT_SIGNAL(new SignalNumberPrinter("exitsignal"));

    private final EventPrinter printer;

    EventType(EventPrinter printer) {
        this.printer = printer;
    }

    public boolean isSyncType() {
        return (WRITE_LOCK.ordinal() <= this.ordinal() && this.ordinal() <= JOIN_THREAD.ordinal())
                || this == PRE_LOCK
                || (BEGIN_THREAD.ordinal() <= this.ordinal() && this.ordinal() <= END_THREAD.ordinal());
    }

    public boolean isMetaType() {
        return CLINIT_ENTER.ordinal() <= this.ordinal() && this.ordinal() <= FINISH_METHOD.ordinal();
    }

    public boolean isSignalType() {
        return ESTABLISH_SIGNAL.ordinal() <= this.ordinal() && this.ordinal() <= EXIT_SIGNAL.ordinal();
    }

    public EventPrinter getPrinter() {
        return printer;
    }
}
