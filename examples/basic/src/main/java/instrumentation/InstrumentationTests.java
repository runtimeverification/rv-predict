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

    public static void main(String[] args) {
        testStackFramesMap();
    }

}
