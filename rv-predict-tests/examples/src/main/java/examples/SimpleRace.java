// Copyright (c) 2015 Runtime Verification Inc. (RV-Predict Team). All Rights Reserved.

package examples;

public class SimpleRace {

    static int sharedVar;

    public static void main(String[] args) {
        new ThreadRunner() {
            @Override
            public void thread1() {
                sharedVar++;
            }

            @Override
            public void thread2() {
                sharedVar++;
            }
        };
    }
}
