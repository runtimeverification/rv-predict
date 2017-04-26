package com.runtimeverification.rvpredict.testutils;

import org.junit.Assert;

public class MoreAsserts {
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
                Assert.fail("Expected exception message to contain '" + exceptionClass.getCanonicalName()
                        + "' but got " + t.getMessage() + ".\n"
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
