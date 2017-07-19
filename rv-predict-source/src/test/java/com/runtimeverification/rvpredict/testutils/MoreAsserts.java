
package com.runtimeverification.rvpredict.testutils;

import com.google.common.collect.ImmutableList;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

    private static Optional<List<Object>> copyCollectionToList(Object item, Optional<Description> description) {
        if (item == null) {
            if (description.isPresent()) {
                description.get().appendText("actual collection was null");
            }
            return Optional.empty();
        }
        if (!(item instanceof Collection)) {
            if (description.isPresent()) {
                description.get().appendText(item.toString());
                description.get().appendText(
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
}
