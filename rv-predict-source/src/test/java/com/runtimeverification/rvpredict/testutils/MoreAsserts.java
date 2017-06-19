
package com.runtimeverification.rvpredict.testutils;

import com.runtimeverification.rvpredict.trace.RawTrace;
import org.junit.Assert;

import java.util.List;

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
}
