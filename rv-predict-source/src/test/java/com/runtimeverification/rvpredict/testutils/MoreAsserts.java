
package com.runtimeverification.rvpredict.testutils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

public class MoreAsserts {
    public static void assertNull(Object o) {
        assertNull("", o);
    }
    public static void assertNull(String message, Object o) {
        if (o == null) {
            return;
        }
        Assert.fail("Expected null object, but got " + o + ".\n" + message);
    }

    public static void assertNotNull(Object o) {
        assertNotNull("", o);
    }

    public static void assertNotNull(String message, Object o) {
        if (o != null) {
            return;
        }
        Assert.fail("Expected non-null object, but got a null one.\n" + message);
    }

    public static void assertSubstring(String substring, String container) {
        assertSubstring(
                "",
                substring, container);
    }

    public static void assertSubstring(String message, String substring, String container) {
        Assert.assertTrue(
                "Expecting that '" + container + "' contains '" + substring + "', but that is not true.\n"
                        + message,
                container.contains(substring));
    }

    public static void assertNotSubstring(String substring, String container) {
        assertNotSubstring(
                "",
                substring, container);
    }

    public static void assertNotSubstring(String message, String substring, String container) {
        Assert.assertFalse(
                "Expecting that '" + container + "' does not contain '" + substring + "', but it does.\n"
                        + message,
                container.contains(substring));
    }

    public interface ThrowingRunnable {
        void run() throws Throwable;
    }
    public static void assertException(ThrowingRunnable runnable) {
        assertException(Throwable.class, runnable);
    }
    public static void assertException(
            String explanation, String exceptionMessageSubstring, ThrowingRunnable runnable) {
        assertException(explanation, Throwable.class, exceptionMessageSubstring, runnable);
    }
    public static void assertException(Class exceptionClass, ThrowingRunnable runnable) {
        assertException(exceptionClass, null, runnable);
    }
    public static void assertException(String exceptionMessageSubstring, ThrowingRunnable runnable) {
        assertException(Throwable.class, exceptionMessageSubstring, runnable);
    }
    public static void assertException(
            String explanation, Class exceptionClass, ThrowingRunnable runnable) {
        assertException(explanation, exceptionClass, null, runnable);
    }
    public static void assertException(
            Class exceptionClass, String exceptionMessageSubstring, ThrowingRunnable runnable) {
        assertException("", exceptionClass, exceptionMessageSubstring, runnable);
    }
    public static void assertException(
            String explanation, Class exceptionClass, String exceptionMessageSubstring, ThrowingRunnable runnable) {
        boolean noException = false;
        try {
            runnable.run();
            noException = true;
        } catch (Throwable t) {
            if (!exceptionClass.isInstance(t)) {
                Assert.fail("Expected exception of type " + exceptionClass.getCanonicalName()
                        + " but got " + t.getClass().getCanonicalName() + ".\n"
                        + explanation + "\n" + t);
            }
            if (exceptionMessageSubstring != null && !t.getMessage().contains(exceptionMessageSubstring)) {
                Assert.fail("Expected exception message to contain '" + exceptionMessageSubstring
                        + "' but got '" + t.getMessage() + "'.\n"
                        + explanation + "\n" + t);
            }
        }
        if (noException) {
            Assert.fail(
                    "Expected exception of type " + exceptionClass.getCanonicalName() + " but none thrown.\n"
                            + explanation);
        }
    }

    @SafeVarargs
    public static <T> Matcher<Collection<T>> containsExactly(T... expectedElements) {
        return new BaseMatcher<Collection<T>>() {
            @Override
            public boolean matches(Object item) {
                Optional<List<Object>> maybeCollection = copyCollectionToList(item, Optional.empty());
                if (!maybeCollection.isPresent()) {
                    return false;
                }
                List<Object> actualNotMatched = new ArrayList<>();
                List<T> expectedNotMatched = new ArrayList<>();
                extractDifferences(maybeCollection.get(), actualNotMatched, expectedNotMatched);
                return actualNotMatched.isEmpty() && expectedNotMatched.isEmpty();
            }

            @Override
            public void describeTo(Description description) {
                if (expectedElements.length == 0) {
                    description.appendText("empty collection");
                    return;
                }
                description.appendText("collection should contain exactly {");
                boolean first = true;
                for (T expectedElement : expectedElements) {
                    if (first) {
                        first = false;
                    } else {
                        description.appendText(",");
                    }
                    description.appendText(expectedElement.toString());
                }
                description.appendText("}");
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                Optional<List<Object>> maybeCollection = copyCollectionToList(item, Optional.of(description));
                if (!maybeCollection.isPresent()) {
                    return;
                }
                Collection<Object> actual = maybeCollection.get();
                List<Object> actualNotMatched = new ArrayList<>();
                List<T> expectedNotMatched = new ArrayList<>();
                extractDifferences(actual, actualNotMatched, expectedNotMatched);
                if (!expectedNotMatched.isEmpty()) {
                    appendCollection(expectedNotMatched, description);
                    if (expectedNotMatched.size() == 1) {
                        description.appendText("was");
                    } else {
                        description.appendText("were");
                    }
                    description.appendText(" not found in the actual value");
                    if (!actualNotMatched.isEmpty()) {
                        description.appendText("; ");
                        appendCollection(actualNotMatched, description);
                        if (actualNotMatched.size() == 1) {
                            description.appendText("was");
                        } else {
                            description.appendText("were");
                        }
                        description.appendText(" found instead");
                    } else {
                        if (actual.isEmpty()) {
                            description.appendText(", which was empty");
                        }
                    }
                } else {
                    if (!actualNotMatched.isEmpty()) {
                        appendCollection(actualNotMatched, description);
                        if (actualNotMatched.size() == 1) {
                            description.appendText("was");
                        } else {
                            description.appendText("were");
                        }
                        description.appendText(" found without being expected");
                    }
                }
            }

            private void extractDifferences(
                    Collection<Object> actualElements,
                    List<Object> actualNotMatched,
                    List<T> expectedNotMatched) {
                boolean[] expectedElementWasMatched = new boolean[expectedElements.length];
                for (Object actualElement : actualElements) {
                    boolean matched = false;
                    for (int i = 0; i < expectedElements.length; i++) {
                        if (expectedElementWasMatched[i]) {
                            continue;
                        }
                        if (actualElement == null) {
                            if (expectedElements[i] == null) {
                                expectedElementWasMatched[i] = true;
                                matched = true;
                                break;
                            }
                            continue;
                        }
                        if (expectedElements[i] == null) {
                            continue;
                        }
                        if (actualElement.equals(expectedElements[i])) {
                            expectedElementWasMatched[i] = true;
                            matched = true;
                        }
                    }
                    if (!matched) {
                        actualNotMatched.add(actualElement);
                    }
                }
                for (int i = 0; i < expectedElementWasMatched.length; i++) {
                    if (!expectedElementWasMatched[i]) {
                        expectedNotMatched.add(expectedElements[i]);
                    }
                }
            }
        };
    }

    @SafeVarargs
    public static <T> Matcher<Collection<T>> containsInOrder(T... expectedElements) {
        return new BaseMatcher<Collection<T>>() {
            @Override
            public boolean matches(Object item) {
                Optional<List<Object>> maybeCollection = copyCollectionToList(item, Optional.empty());
                if (!maybeCollection.isPresent()) {
                    return false;
                }
                List<Object> collection = maybeCollection.get();
                if (expectedElements.length != maybeCollection.get().size()) {
                    return false;
                }
                for (int i = 0; i < expectedElements.length; i++) {
                    if (expectedElements[i] == null) {
                        if (collection.get(i) != null) {
                            return false;
                        }
                    } else if (collection.get(i) == null) {
                        return false;
                    } else if (!expectedElements[i].equals(collection.get(i))) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("collection should contain, in order, exactly {");
                boolean first = true;
                for (T expectedElement : expectedElements) {
                    if (first) {
                        first = false;
                    } else {
                        description.appendText(",");
                    }
                    description.appendText(expectedElement.toString());
                }
                description.appendText("}");
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                Optional<List<Object>> maybeCollection = copyCollectionToList(item, Optional.of(description));
                if (!maybeCollection.isPresent()) {
                    return;
                }
                List<Object> collection = maybeCollection.get();

                boolean mismatch = false;
                if (expectedElements.length != maybeCollection.get().size()) {
                    description.appendText("actual collection has different size: " + collection.size() + "; ");
                    mismatch = true;
                }
                for (int i = 0; i < Math.min(expectedElements.length, collection.size()); i++) {
                    if (expectedElements[i] == null) {
                        if (collection.get(i) != null) {
                            description.appendText(
                                    "first difference found at index " + i + ": expected null but got "
                                            + collection.get(i) + "; ");
                            mismatch = true;
                            break;
                        }
                    } else if (collection.get(i) == null) {
                        description.appendText(
                                "first difference found at index " + i + ": expected " + expectedElements[i]
                                        + " but got null; ");
                        mismatch = true;
                        break;
                    } else if (!expectedElements[i].equals(collection.get(i))) {
                        description.appendText(
                                "first difference found at index " + i + ": expected " + expectedElements[i]
                                        + " but got " + collection.get(i) + "; ");
                        mismatch = true;
                        break;
                    }
                }
                if (mismatch) {
                    description.appendText("actual collection: ");
                    appendCollection(collection, description);
                    description.appendText(".");
                }
            }
        };
    }

    public static <T> Matcher<Collection<T>> hasSize(int size) {
        return new BaseMatcher<Collection<T>>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("collection should have " + size + " element");
                if (size >= 1) {
                    description.appendText("s");
                }
                description.appendText(".");
            }

            @Override
            public boolean matches(Object item) {
                Optional<List<Object>> maybeCollection = copyCollectionToList(item, Optional.empty());
                return maybeCollection.isPresent() && maybeCollection.get().size() == size;
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                Optional<List<Object>> maybeCollection = copyCollectionToList(item, Optional.of(description));
                if (!maybeCollection.isPresent()) {
                    return;
                }
                List<Object> collection = maybeCollection.get();
                description.appendText("expected " + size + " elements, but got " + collection.size() + ": ");
                appendCollection(collection, description);

            }
        };
    }

    public static <K, V> Matcher<Map<K, V>> hasMapSize(int size) {
        return new BaseMatcher<Map<K, V>>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("map should have " + size + " element");
                if (size >= 1) {
                    description.appendText("s");
                }
                description.appendText(".");
            }

            @Override
            public boolean matches(Object item) {
                Optional<Map<Object, Object>> maybeMap = copyToMap(item, Optional.empty());
                return maybeMap.isPresent() && maybeMap.get().size() == size;
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                Optional<Map<Object, Object>> maybeMap = copyToMap(item, Optional.of(description));
                if (!maybeMap.isPresent()) {
                    return;
                }
                Map<Object, Object> map = maybeMap.get();
                description.appendText("expected " + size + " elements, but got " + maybeMap.get().size() + ": ");
                appendMap(map, description);

            }
        };
    }

    public static <T> Matcher<Collection<T>> isEmpty() {
        return new BaseMatcher<Collection<T>>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("collection should be empty.");
            }

            @Override
            public boolean matches(Object item) {
                Optional<List<Object>> maybeCollection = copyCollectionToList(item, Optional.empty());
                return maybeCollection.isPresent() && maybeCollection.get().isEmpty();
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                Optional<List<Object>> maybeCollection = copyCollectionToList(item, Optional.of(description));
                if (!maybeCollection.isPresent()) {
                    return;
                }
                List<Object> collection = maybeCollection.get();
                description.appendText("collection has " + collection.size() + " element");
                if (collection.size() > 1) {
                    description.appendText("s");
                }
                description.appendText(": ");
                appendCollection(collection, description);

            }
        };
    }

    public static <K, V> Matcher<Map<K, V>> isEmptyMap() {
        return new BaseMatcher<Map<K, V>>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("map should be empty.");
            }

            @Override
            public boolean matches(Object item) {
                Optional<Map<Object, Object>> maybeMap = copyToMap(item, Optional.empty());
                return maybeMap.isPresent() && maybeMap.get().isEmpty();
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                Optional<Map<Object, Object>> maybeMap = copyToMap(item, Optional.of(description));
                if (!maybeMap.isPresent()) {
                    return;
                }
                Map map = maybeMap.get();
                if (!map.isEmpty()) {
                    description.appendText("map had " + map.size() + "elements.");
                }
            }
        };
    }

    public static <T> Matcher<Optional<T>> isPresentWithValue(T value) {
        Assert.assertNotNull(value);
        String optionalWithType = "Optional<" + value.getClass().getCanonicalName() + ">";
        return new BaseMatcher<Optional<T>>() {
            @Override
            public void describeTo(Description description) {
                description.appendText(optionalWithType + " should contain " + value + ".");
            }

            @Override
            public boolean matches(Object item) {
                if (item == null) {
                    return false;
                }
                if (!(item instanceof Optional)) {
                    return false;
                }
                Optional maybeItem = (Optional) item;
                if (!maybeItem.isPresent()) {
                    return false;
                }
                Object presentItem = maybeItem.get();
                return value.equals(presentItem);
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                if (item == null) {
                    description.appendText("Unexpected null object.");
                    return;
                }
                if (!(item instanceof Optional)) {
                    description.appendText(
                            "Object type is " + item.getClass().getCanonicalName() + ".");
                    return;
                }
                Optional maybeItem = (Optional) item;
                if (!maybeItem.isPresent()) {
                    description.appendText(optionalWithType + " is empty.");
                    return;
                }
                Object presentItem = maybeItem.get();
                if (!value.equals(presentItem)) {
                    description.appendText(optionalWithType + " contains " + presentItem + ".");
                }
            }
        };
    }

    public static Matcher<OptionalInt> isPresentWithIntValue(int value) {
        String optionalWithType = "OptionalInt";
        return new BaseMatcher<OptionalInt>() {
            @Override
            public void describeTo(Description description) {
                description.appendText(optionalWithType + " should contain " + value + ".");
            }

            @Override
            public boolean matches(Object item) {
                if (item == null) {
                    return false;
                }
                if (!(item instanceof OptionalInt)) {
                    return false;
                }
                OptionalInt maybeItem = (OptionalInt) item;
                return maybeItem.isPresent() && value == maybeItem.getAsInt();
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                if (item == null) {
                    description.appendText("Unexpected null object.");
                    return;
                }
                if (!(item instanceof OptionalInt)) {
                    description.appendText(
                            "Object type is " + item.getClass().getCanonicalName() + ".");
                    return;
                }
                OptionalInt maybeItem = (OptionalInt) item;
                if (!maybeItem.isPresent()) {
                    description.appendText(optionalWithType + " is empty.");
                    return;
                }
                if (value != maybeItem.getAsInt()) {
                    description.appendText(optionalWithType + " contains " + maybeItem.getAsInt() + ".");
                }
            }
        };
    }

    public static Matcher<OptionalLong> isPresentWithLongValue(long value) {
        String optionalWithType = "OptionalLong";
        return new BaseMatcher<OptionalLong>() {
            @Override
            public void describeTo(Description description) {
                description.appendText(optionalWithType + " should contain " + value + ".");
            }

            @Override
            public boolean matches(Object item) {
                if (item == null) {
                    return false;
                }
                if (!(item instanceof OptionalLong)) {
                    return false;
                }
                OptionalLong maybeItem = (OptionalLong) item;
                return maybeItem.isPresent() && value == maybeItem.getAsLong();
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                if (item == null) {
                    description.appendText("Unexpected null object.");
                    return;
                }
                if (!(item instanceof OptionalLong)) {
                    description.appendText(
                            "Object type is " + item.getClass().getCanonicalName() + ".");
                    return;
                }
                OptionalLong maybeItem = (OptionalLong) item;
                if (!maybeItem.isPresent()) {
                    description.appendText(optionalWithType + " is empty.");
                    return;
                }
                if (value != maybeItem.getAsLong()) {
                    description.appendText(optionalWithType + " contains " + maybeItem.getAsLong() + ".");
                }
            }
        };
    }

    public static Matcher<Optional> isAbsent() {
        String optionalWithType = "Optional<>";
        return new BaseMatcher<Optional>() {
            @Override
            public void describeTo(Description description) {
                description.appendText(optionalWithType + " should be empty.");
            }

            @Override
            public boolean matches(Object item) {
                if (item == null) {
                    return false;
                }
                if (!(item instanceof Optional)) {
                    return false;
                }
                Optional maybeItem = (Optional) item;
                return !maybeItem.isPresent();
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                if (item == null) {
                    description.appendText("Unexpected null object.");
                    return;
                }
                if (!(item instanceof Optional)) {
                    description.appendText(
                            "Object type is " + item.getClass().getCanonicalName() + ".");
                    return;
                }
                Optional maybeItem = (Optional) item;
                if (maybeItem.isPresent()) {
                    description.appendText(optionalWithType + " contains " + maybeItem.get() + ".");
                }
            }
        };
    }

    public static Matcher<OptionalInt> isAbsentInt() {
        String optionalWithType = "OptionalInt";
        return new BaseMatcher<OptionalInt>() {
            @Override
            public void describeTo(Description description) {
                description.appendText(optionalWithType + " should be empty.");
            }

            @Override
            public boolean matches(Object item) {
                if (item == null) {
                    return false;
                }
                if (!(item instanceof OptionalInt)) {
                    return false;
                }
                OptionalInt maybeItem = (OptionalInt) item;
                return !maybeItem.isPresent();
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                if (item == null) {
                    description.appendText("Unexpected null object.");
                    return;
                }
                if (!(item instanceof OptionalInt)) {
                    description.appendText(
                            "Object type is " + item.getClass().getCanonicalName() + ".");
                    return;
                }
                OptionalInt maybeItem = (OptionalInt) item;
                if (maybeItem.isPresent()) {
                    description.appendText(optionalWithType + " contains " + maybeItem.getAsInt() + ".");
                }
            }
        };
    }

    public static Matcher<OptionalLong> isAbsentLong() {
        String optionalWithType = "OptionalLong";
        return new BaseMatcher<OptionalLong>() {
            @Override
            public void describeTo(Description description) {
                description.appendText(optionalWithType + " should be empty.");
            }

            @Override
            public boolean matches(Object item) {
                if (item == null) {
                    return false;
                }
                if (!(item instanceof OptionalLong)) {
                    return false;
                }
                OptionalLong maybeItem = (OptionalLong) item;
                return !maybeItem.isPresent();
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                if (item == null) {
                    description.appendText("Unexpected null object.");
                    return;
                }
                if (!(item instanceof OptionalLong)) {
                    description.appendText(
                            "Object type is " + item.getClass().getCanonicalName() + ".");
                    return;
                }
                OptionalLong maybeItem = (OptionalLong) item;
                if (maybeItem.isPresent()) {
                    description.appendText(optionalWithType + " contains " + maybeItem.getAsLong() + ".");
                }
            }
        };
    }

    private static Optional<List<Object>> copyCollectionToList(Object item, Optional<Description> maybeDescription) {
        if (item == null) {
            maybeDescription.ifPresent(description -> description.appendText("actual collection was null"));
            return Optional.empty();
        }
        if (!(item instanceof Collection)) {
            if (maybeDescription.isPresent()) {
                maybeDescription.get().appendText(item.toString());
                maybeDescription.get().appendText(
                        " is " + item.getClass().getCanonicalName()
                                + ", not a " + Collection.class.getCanonicalName());
            }
            return Optional.empty();
        }
        ImmutableList.Builder<Object> typedCollection = ImmutableList.builder();
        for (Object o : (Collection) item) {
            typedCollection.add(o);
        }
        return Optional.of(typedCollection.build());
    }

    private static Optional<Map<Object, Object>> copyToMap(Object item, Optional<Description> maybeDescription) {
        if (item == null) {
            maybeDescription.ifPresent(description -> description.appendText("actual map was null"));
            return Optional.empty();
        }
        if (!(item instanceof Map)) {
            if (maybeDescription.isPresent()) {
                maybeDescription.get().appendText(item.toString());
                maybeDescription.get().appendText(
                        " is " + item.getClass().getCanonicalName()
                                + ", not a " + Map.class.getCanonicalName());
            }
            return Optional.empty();
        }
        Map mapItem = (Map)item;
        return Optional.of(ImmutableMap.builder().putAll(mapItem).build());
    }

    private static void appendCollection(Collection<?> collection, Description description) {
        description.appendText("{");
        boolean first = true;
        for (Object element : collection) {
            if (first) {
                first = false;
            } else {
                description.appendText(",");
            }
            description.appendText(element.toString());
        }
        description.appendText("} ");
    }

    private static void appendMap(Map<?, ?> map, Description description) {
        description.appendText("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (first) {
                first = false;
            } else {
                description.appendText(", ");
            }
            description.appendText(entry.getKey().toString());
            description.appendText(" : ");
            description.appendText(entry.getValue().toString());
        }
        description.appendText("} ");
    }
}
