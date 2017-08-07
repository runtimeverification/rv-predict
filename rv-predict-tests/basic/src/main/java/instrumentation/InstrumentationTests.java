package instrumentation;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Collection of instrumentation tests.
 *
 * @author YilongL
 */
public class InstrumentationTests {

    static class MyMap extends AbstractMap<Integer, Integer> {

        @Override
        public Integer get(Object key) {
            return super.get(key);
        }

        @Override
        public Set<Map.Entry<Integer, Integer>> entrySet() {
            Set<Map.Entry<Integer, Integer>> entrySet = new HashSet<>();
            entrySet.add(new AbstractMap.SimpleEntry<>(1, 1));
            return entrySet;
        }
    }

    /**
     * Tests if our instrumentation correctly casts the return value of
     * {@code RVPredictRuntime.rvPredictMapGet} back to
     * {@code MyMap.get(Object)}'s original return type, i.e. {@code Integer}.
     */
    private static void testStackFramesMap() {
        MyMap m = new MyMap();
        Integer i = m.get(1);
        i.intValue();
    }

    private static void testConstructors() {
        new ConstructorInstrumentation(
                new ConstructorInstrumentation(), new ConstructorInstrumentation(), new ConstructorInstrumentation());
    }

    static int x = 0;

    public static void main(String[] args) throws InterruptedException {
        testStackFramesMap();
        testConstructors();
        // This should be enough for testing that issue 458 is still fixed.
        // https://github.com/runtimeverification/rv-predict/issues/458
        ScalaInstrumentationTests.main(args);

        Thread t1 = new Thread(() -> x++);
        Thread t2 = new Thread(() -> x++);
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

}
