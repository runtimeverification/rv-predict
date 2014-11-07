package com.runtimeverification.rvpredict;

import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rvpredict.config.Util;
import org.junit.Assert;

/**
 * Helper class for testing the output of external commands against the expected output.
 * This code is currently copied over from rv-monitor.
 * Maybe we could consider a shared library if more duplication occurs.
 * @author TraianSF
 */
public class TestHelper {

    private final FileSystem fileSystem;
    private final File basePathFile;
    private final Path basePath;

    /**
     * Initializes the {@code basePath} field to the parent directory of the specified file path
     * @param filePath  path to the file which prompted this test, used to establish working dir
     */
    public TestHelper(String filePath)   {
        fileSystem = FileSystems.getDefault();
        this.basePath = fileSystem.getPath(filePath);
        basePathFile = basePath.toFile();

    }

    /**
     * Execute command, tests return code and potentially checks standard and
     * error output against expected content in files if
     * {@code expectedFilePrefix} not null.
     * 
     * @param expectedFilePrefix
     *            the prefix for the expected files, or null if output is not
     *            checked.
     * @param numOfRuns
     *            test this command up to a certain number of runs
     * @param command
     *            list of arguments describing the system command to be
     *            executed. @throws Exception
     */
    public void testCommand(String expectedFilePrefix, int numOfRuns, String... command) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command).inheritIO();
        processBuilder.directory(basePathFile);
        String testsPrefix;
        File expectedOutFile = null;
        File expectedErrFile = null;
        File actualOutFile = null;
        File actualErrFile = null;
        File inFile;
        
        /* run the command up to a certain number of times and gather the outputs */
        StringBuilder aggregatedOut = new StringBuilder();
        StringBuilder aggregatedErr = new StringBuilder();
        for (int i = 0; i < numOfRuns; i++) {
            if (expectedFilePrefix != null) {
                testsPrefix = basePath.toString() + "/" + expectedFilePrefix;
                expectedOutFile = new File(testsPrefix + ".expected.out");
                expectedErrFile = new File(testsPrefix + ".expected.err");
                actualOutFile = new File(testsPrefix + ".actual.out");
                actualErrFile = new File(testsPrefix + ".actual.err");
                inFile = new File(testsPrefix + ".in");
                processBuilder.redirectError(actualErrFile);
                processBuilder.redirectOutput(actualOutFile);
                if (inFile.exists() && !inFile.isDirectory()) {
                    processBuilder.redirectInput(inFile);
                }
            }
            Process process = processBuilder.start();
            if (expectedFilePrefix == null) {
                Util.redirectOutput(process.getInputStream(), null);
                Util.redirectOutput(process.getErrorStream(), null);
            }
            int returnCode = process.waitFor();
            Assert.assertEquals("Expected no error during " + Arrays.toString(command) + ".", 0, returnCode);
        
            // aggregate the outputs
            if (expectedFilePrefix != null) {
                aggregatedOut.append(Util.convertFileToString(actualOutFile));
                aggregatedErr.append(Util.convertFileToString(actualErrFile));
            }
        }
        
        if (expectedFilePrefix != null) {
            assertMatchPatterns(Util.convertFileToString(expectedOutFile), aggregatedOut.toString());
            assertMatchPatterns(Util.convertFileToString(expectedErrFile), aggregatedErr.toString());
        }
    }

    private void assertMatchPatterns(String expectedPatterns, String actualText) throws IOException {
        for (String pattern : expectedPatterns.split("(\n|\r)")) {
            Matcher m = Pattern.compile(pattern).matcher(actualText);
            Assert.assertTrue(String.format("Expected result to match regular expression:" +
                        "%n%s%n%nbut found:%n%s%n", pattern, actualText),m.find());
        }
    }

}

