package com.runtimeverification.rvpredict;

import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
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
     * Execute command, tests return code and potentially checks standard and error output against expected content
     * in files if {@code expectedFilePrefix} not null.
     * @param expectedFilePrefix the prefix for the expected files, or null if output is not checked.
     * @param regex
     *@param command  list of arguments describing the system command to be executed.  @throws Exception
     */
    public void testCommand(String expectedFilePrefix, boolean regex, String... command) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command).inheritIO();
        processBuilder.directory(basePathFile);
        String actualOutFile = null;
        String testsPrefix;
        String actualErrFile = null;
        String expectedOutFile = null;
        String expectedErrFile = null;
        String inFile = null;
        if (expectedFilePrefix != null) {
            testsPrefix = basePath.toString() + "/" + expectedFilePrefix;
            actualOutFile = testsPrefix + ".actual.out";
            actualErrFile = testsPrefix + ".actual.err";
            expectedOutFile = testsPrefix + ".expected.out";
            expectedErrFile = testsPrefix + ".expected.err";
            inFile = testsPrefix + ".in";
            processBuilder.redirectError(new File(actualErrFile));
            processBuilder.redirectOutput(new File(actualOutFile));
            File input = new File(inFile);
            if (input.exists() && !input.isDirectory()) {
                processBuilder.redirectInput(input);
            }
        }
        Process process = processBuilder.start();
        if (expectedFilePrefix == null) {
            Util.redirectOutput(process.getInputStream(), null);
            Util.redirectOutput(process.getErrorStream(), null);
        }
        int returnCode = process.waitFor();
        Assert.assertEquals("Expected no error during " + Arrays.toString(command) + ".", 0, returnCode);
        if (expectedFilePrefix != null) {
            if (regex) {
                assertMatchPatterns(expectedOutFile, actualOutFile);
                assertMatchPatterns(expectedErrFile, actualErrFile);
            } else {
                assertEqualFiles(expectedOutFile, actualOutFile);
                assertEqualFiles(expectedErrFile, actualErrFile);
            }
        }
    }

    private void assertMatchPatterns(String expectedFile, String actualFile) throws IOException {
        String expectedPatterns = Util.convertFileToString(expectedFile);
        String actualText = Util.convertFileToString(actualFile);
        for (String pattern : expectedPatterns.split("(\n|\r)")) {
            Matcher m = Pattern.compile(pattern).matcher(actualText);
            Assert.assertTrue(String.format("Expected result to match regular expression:" +
                        "%n%s%n%nbut found:%n%s%n", pattern, actualText),m.find());
        }
    }

    /**
     * Assert two files have equal content.
     * @param expectedFile The path to the file with the expected result.
     * @param actualFile The path to the file with the calculated result.
     */
    public void assertEqualFiles(String expectedFile, String actualFile) throws IOException {
        String expectedText = Util.convertFileToString(expectedFile);
        String actualText = Util.convertFileToString(actualFile);

        Assert.assertEquals(actualFile + " should match " + expectedFile, expectedText, actualText);
    }

    /**
     * Moves files from the current directory to the path pointed to by basePath
     * @param files  files to be relocated
     * @throws IOException
     */

    public void relocateFiles(String... files) throws IOException {
        for (String s : files) {
            Path path = fileSystem.getPath(basePath.toString(), s);
            Files.move(
                    fileSystem.getPath(s),
                    path
            );
        }
    }

    /**
     * Deletes files from the basePath, potentially failing if the files don't exist
     * @param fail  if true, it expects the files to exist and fails the test if they don't
     * @param files relative paths (to basePath) of the files to be deleted
     * @throws IOException
     */
    public void deleteFiles(boolean fail, String... files) throws IOException {
        for (String s : files) {
            Path toDelete = fileSystem.getPath(basePath.toString(), s);
            if (fail) {
                Files.delete(toDelete);
            } else {
                Files.deleteIfExists(toDelete);
            }
        }
    }

    /**
     * Computes the path obtained by adding the relative path specified by {@code path} to the
     * {@code basePath}
     * @param path  path relative to {@code basePath} to be computed
     * @return the path obtained by adding {@code path} to {@code basePath}
     */
    public Path getPath(String path) {
        return fileSystem.getPath(basePath.toString(), path);
    }


}

