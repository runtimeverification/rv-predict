package com.runtimeverification.rvpredict.smt;

public class VariableValue {
    private final String value;
    public VariableValue(String value) {
        this.value = value;
    }

    public int asInt() {
        return Integer.parseInt(value);
    }
}
