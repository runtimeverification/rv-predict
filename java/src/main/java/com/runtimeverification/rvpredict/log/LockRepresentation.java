package com.runtimeverification.rvpredict.log;

public class LockRepresentation {
    public enum LockType {
        READ_LOCK("ReadLock"),
        WRITE_LOCK("WriteLock"),
        MONITOR("Monitor"),
        SIGNAL_LOCK("SignalLock"),
        ATOMIC_LOCK("AtomicLock");

        private final String lockName;

        LockType(String lockName) {
            this.lockName = lockName;
        }

        public String getLockName() {
            return lockName;
        }
    }
    private final LockType lockType;
    private final long lockAddress;

    public LockRepresentation(LockType lockType, long lockAddress) {
        this.lockType = lockType;
        this.lockAddress = lockAddress;
    }

    public LockType getLockType() {
        return lockType;
    }

    public String getLockName() {
        return lockType.getLockName();
    }

    public long getLockAddress() {
        return lockAddress;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LockRepresentation)) {
            return false;
        }
        LockRepresentation other = (LockRepresentation) obj;
        return lockType == other.lockType && lockAddress == other.lockAddress;
    }

    @Override
    public String toString() {
        return getLockName() + "@" + getLockAddress();
    }
}
