package com.runtimeverification.rvpredict.asm;

public class ClassA {

    static int staticVariable;

    static int shadowedStaticVariable;

    protected int x;

}

class ClassB extends ClassA {

    static int shadowedStaticVariable;

    void test() {
        x = 0;
        super.x = 0;

        staticVariable = 0;
        ClassA.staticVariable = 0;

        shadowedStaticVariable = 0;
        ClassA.shadowedStaticVariable = 0;
    }
}
