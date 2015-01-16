/* Copyright (c) 2014-2015 Runtime Verification Inc. All Rights Reserved. */

package jtsan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * Class containing tests of proper mocking of the Java Collection Framework.
 *
 * @author YilongL
 */
public class JUCollectionTests {

    //------------------ Positive tests ---------------------

    @RaceTest(expectRace = true,
            description = "Basic operations in Collection interface")
    public void basicCollectionOps() {
        final Collection<Integer> collection = new ArrayList<>();

        new ThreadRunner(2) {
            @Override
            public void thread1() {
                collection.add(1);
                collection.addAll(Collections.singleton(2));
                collection.remove(1);
                collection.removeAll(Collections.singleton(2));
                collection.contains(1);
                collection.containsAll(Collections.singleton(1));
                collection.retainAll(Collections.singleton(1));
                collection.clear();
                collection.toArray();
                collection.toArray(new Integer[2]);
            }

            @Override
            public void thread2() {
                collection.add(0);
            }
        };
    }

    @RaceTest(expectRace = true,
            description = "one thread iterating over Iterable using for-each loop; another thread writes")
    public void foreachLoop0() {
        final Collection<Integer> iterable = new ArrayList<>();

        new ThreadRunner(2) {

            @Override
            public void setUp() {
                for (int i = 0; i < 10; i++) {
                    iterable.add(i);
                }
            }

            @Override
            public void thread1() {
                shortSleep(); // avoid ConcurrentModificationException
                int sum = 0;
                for (int i : iterable) {
                    sum += i;
                }
            }

            @Override
            public void thread2() {
                iterable.add(0);
            }
        };
    }

    @RaceTest(expectRace = true,
            description = "two different iterators")
    public void foreachLoop1() {
        final Collection<Integer> iterable = new ArrayList<>();

        new ThreadRunner(2) {

            @Override
            public void setUp() {
                for (int i = 0; i < 10; i++) {
                    iterable.add(i);
                }
            }

            @Override
            public void thread1() {
                shortSleep(); // avoid ConcurrentModificationException
                int sum = 0;
                for (int i : iterable) {
                    sum += i;
                }
            }

            @Override
            public void thread2() {
                Iterator<Integer> iter = iterable.iterator();
                if (iter.hasNext()) {
                    iter.next();
                    iter.remove();
                }
            }
        };
    }

    @RaceTest(expectRace = false,
            description = "two threads iterating over an Iterable")
    public void readOnlyIteration() {
        final Collection<Integer> iterable = new ArrayList<>();

        new ThreadRunner(2) {

            @Override
            public void setUp() {
                for (int i = 0; i < 10; i++) {
                    iterable.add(i);
                }
            }

            @Override
            public void thread1() {
                int sum = 0;
                for (int i : iterable) { sum += i; }
            }

            @Override
            public void thread2() {
                int sum = 0;
                for (int i : iterable) { sum += i; }
            }
        };
    }

    public static void main(String[] args) {
        JUCollectionTests tests = new JUCollectionTests();
        // positive tests
        if (args[0].equals("positive")) {
            tests.basicCollectionOps();
            tests.foreachLoop0();
            tests.foreachLoop1();
        } else {
            // negative tests
            tests.readOnlyIteration();
        }
    }
}
