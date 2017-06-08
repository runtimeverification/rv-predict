package com.runtimeverification.rvpredict.testutils;

import org.junit.Assert;

import java.util.Optional;

public class TestUtils {
    public static <T> T fromOptional(Optional<T> optional) {
        Assert.assertTrue(optional.isPresent());
        return optional.get();
    }
}
