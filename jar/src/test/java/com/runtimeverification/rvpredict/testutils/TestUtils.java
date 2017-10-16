package com.runtimeverification.rvpredict.testutils;

import org.junit.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

public class TestUtils {
    private static String logDir = null;
    public static <T> T fromOptional(Optional<T> optional) {
        Assert.assertTrue(optional.isPresent());
        return optional.get();
    }

    public static synchronized String getLogDir() {
        if (logDir == null) {
            try {
                logDir = Files
                        .createTempDirectory(Paths.get(System.getProperty("java.io.tmpdir")), "rv-predict")
                        .toString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return logDir;
    }
}
