// Copyright (c) 2015 Runtime Verification Inc. (RV-Predict Team). All Rights Reserved.

package examples;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RaceOnSynchronizedMap {

    static Map<Integer, Integer> map = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) {
        new ThreadRunner() {
            @Override
            public void thread1() {
                map.put(1, 1);
            }

            @Override
            public void thread2() {
                Set<Integer> keySet = map.keySet();
                synchronized (keySet) {
                    for (int k : keySet) {
                        System.out.println("key = " + k);;
                    }
                }
            }
        };
    }
}
