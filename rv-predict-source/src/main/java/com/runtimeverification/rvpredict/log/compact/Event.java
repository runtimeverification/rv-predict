package com.runtimeverification.rvpredict.log.compact;

public class Event {
    public enum Type {
        BEGIN(0),  // start of a thread
        LEGEND(BEGIN.intValue()),  // alias for 'begin'
        END(1),  // thread termination

        // load: 1, 2, 4, 8, 16 bytes wide.
        LOAD1(2),
        LOAD2(3),
        LOAD4(4),
        LOAD8(5),
        LOAD16(6),

        // store: 1, 2, 4, 8, 16 bytes wide.
        STORE1(7),
        STORE2(8),
        STORE4(9),
        STORE8(10),
        STORE16(11),

        FORK(12),  // create a new thread
        JOIN(13),  // join an existing thread
        ACQUIRE(14),  // acquire lock
        RELEASE(15),  // release lock

        ENTERFN(16),  // enter a function
        EXITFN(17),  // exit a function
        SWITCH(18),  // switch thread context

        // atomic read-modify-write: 1, 2, 4, 8, 16 bytes wide.
        ATOMIC_RMW1(19),
        ATOMIC_RMW2(20),
        ATOMIC_RMW4(21),
        ATOMIC_RMW8(22),
        ATOMIC_RMW16(23),

        // atomic load: 1, 2, 4, 8, 16 bytes wide.
        ATOMIC_LOAD1(24),
        ATOMIC_LOAD2(25),
        ATOMIC_LOAD4(26),
        ATOMIC_LOAD8(27),
        ATOMIC_LOAD16(28),

        // atomic store: 1, 2, 4, 8, 16 bytes wide.
        ATOMIC_STORE1(29),
        ATOMIC_STORE2(30),
        ATOMIC_STORE4(31),
        ATOMIC_STORE8(32),
        ATOMIC_STORE16(33),

        COG(34),  // change of generation
        SIGEST(35),  // establish signal action

        // signal delivery
        ENTERSIG(36),
        EXITSIG(37),

        SIGDIS(38),  // disestablish signal action.
        SIGMASKMEMO(39),  // establish a new number -> mask mapping (memoize mask).
        MASKSIGS(40),  // mask signals
        // Set the number of signals running concurrently on the current thread.  Note that
        // this is a level of "concurrency," not a signal "depth," because the wrapper function for signal
        // handlers is reentrant, and it may race with itself to increase the
        // number of interrupts outstanding ("depth").
        SIGOUTST(41),
        // TODO(virgil): I think that this should always be max+1.
        NOPS(42);

        private final int intValue;
        Type(int intValue) {
            this.intValue = intValue;
        }
        public int intValue() {
            return intValue;
        }
    }
}
