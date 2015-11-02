// Copyright (c) 2015 Runtime Verification Inc. (RV-Predict Team). All Rights Reserved.

package examples;

import java.util.ArrayList;
import java.util.List;

public class RaceOnArrayList {

    static List<Integer> list = new ArrayList<>();

    public static void main(String[] args) {
        new ThreadRunner() {
            @Override
            public void thread1() {
                list.add(0);
            }

            @Override
            public void thread2() {
                list.add(1);
            }
        };
    }
}
