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

    private static class DelegatedIterator implements Iterator<Integer> {

        private final Iterator<Integer> iter;

        private DelegatedIterator(Collection<Integer> collection) {
            iter = collection.iterator();
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public Integer next() {
            return iter.next() * 2;
        }
    }

    @RaceTest(expectRace = true,
            description = "customized implementation of iterator by delegation")
    public void delegatedIterator() {
        final Collection<Integer> ints = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ints.add(i);
        }
        final DelegatedIterator iter = new DelegatedIterator(ints);

        new ThreadRunner(2) {

            @Override
            public void thread1() {
                shortSleep();
                while (iter.hasNext()) {
                    iter.next();
                }
            }

            @Override
            public void thread2() {
                ints.add(0);
            }
        };
    }

    @RaceTest(expectRace = true,
            description = "Basic operations in Map interface")
    public void basicMapOps() {
        final java.util.Map<Integer, Integer> map = new java.util.HashMap<>();

        new ThreadRunner(2) {
            @Override
            public void thread1() {
                map.get(0);
                map.put(1, 1);
                map.putAll(Collections.singletonMap(2, 2));
                map.containsKey(1);
                map.containsValue(1);
                map.remove(1);
                map.clear();
            }

            @Override
            public void thread2() {
                map.put(0, 0);
            }
        };
    }

    @RaceTest(expectRace = true,
            description = "modifying collection views of a map")
    public void collectionViewsOfMap() {
        final java.util.Map<Integer, Integer> map = new java.util.HashMap<>();

        new ThreadRunner(2) {

            @Override
            public void setUp() {
                for (int i = 0; i < 10; i++) {
                    map.put(i, i);
                }
            }

            @Override
            public void thread1() {
                shortSleep();
                java.util.Set<Integer> keySet = map.keySet();
                keySet.remove(0);               // modify key set view
                for (Integer key : keySet) {};  // access key set view via iterator

                Collection<Integer> values = map.values();
                values.remove(0);               // modify value collection view
                for (Integer val : values) {};  // access value collection view via iterator

                java.util.Set<java.util.Map.Entry<Integer, Integer>> entrySet = map.entrySet();
                Iterator<java.util.Map.Entry<Integer, Integer>> iter = entrySet.iterator();
                java.util.Map.Entry<Integer, Integer> e = iter.next(); // read access via iterator
            }

            @Override
            public void thread2() {
                map.put(3, 3);
            }
        };
    }

    @ExcludedTest(reason = "not worth to instrument Map.Entry implementation")
    @RaceTest(expectRace = true,
            description = "Test instrumentation of map entry")
    public void mapEntry() {
        new ThreadRunner(2) {

            java.util.Map.Entry<Integer, Integer> entry;

            @Override
            public void setUp() {
                java.util.Map<Integer, Integer> m = new java.util.HashMap<>();
                m.put(0, 0);
                entry = m.entrySet().iterator().next();
            }

            @Override
            public void thread1() {
                entry.getValue();
            }

            @Override
            public void thread2() {
                entry.setValue(1);
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
            tests.delegatedIterator();
            tests.basicMapOps();
            tests.collectionViewsOfMap();
//            tests.mapEntry();
        } else {
            // negative tests
            tests.readOnlyIteration();
        }
    }
}
