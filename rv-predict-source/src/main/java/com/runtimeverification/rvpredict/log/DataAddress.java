package com.runtimeverification.rvpredict.log;

public interface DataAddress {
    DataAddress NULL_OBJECT = new DataAddress() {
        @Override
        public long getDataAddressOr0() {
            return 0;
        }

        @Override
        public int getObjectHashCode() {
            return 0;
        }

        @Override
        public int getFieldIdOrArrayIndex() {
            return 0;
        }
    };
    static DataAddress createPlainDataAddress(long address) {
        return new PlainDataAddress(address);
    }

    static DataAddress signalHandler(long signalNumber) {
        return new SignalHandlerDataAddress(signalNumber);
    }

    long getDataAddressOr0();
    int getObjectHashCode();
    int getFieldIdOrArrayIndex();
}

class PlainDataAddress implements DataAddress {
    private final long address;

     PlainDataAddress(long address) {
        this.address = address;
    }

    @Override
    public long getDataAddressOr0() {
        return address;
    }

    @Override
    public int getObjectHashCode() {
        return (int) (address >> 32);
    }

    @Override
    public int getFieldIdOrArrayIndex() {
        return (int) address;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(address);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PlainDataAddress)) {
            return false;
        }
        PlainDataAddress pda = (PlainDataAddress)obj;
        return address == pda.address;
    }

    @Override
    public String toString() {
        return Long.toString(address);
    }
}

class SignalHandlerDataAddress implements DataAddress {
    private final long signalNumber;

    SignalHandlerDataAddress(long signalNumber) {
        this.signalNumber = signalNumber;
    }

    @Override
    public long getDataAddressOr0() {
        return 0;
    }

    @Override
    public int getObjectHashCode() {
        return 0;
    }

    @Override
    public int getFieldIdOrArrayIndex() {
        return 0;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(signalNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SignalHandlerDataAddress)) {
            return false;
        }
        SignalHandlerDataAddress shd = (SignalHandlerDataAddress)obj;
        return signalNumber == shd.signalNumber;
    }

    @Override
    public String toString() {
        return String.format("SH(%s)", signalNumber);
    }
}
