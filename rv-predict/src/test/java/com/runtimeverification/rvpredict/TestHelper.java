package com.runtimeverification.rvpredict;

import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
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
     *            the prefix for the expected files
     * @param numOfRuns
     *            test this command up to a certain number of runs
     * @param command
     *            list of arguments describing the system command to be
     *            executed.
     * @throws IOException
     * @throws InterruptedException
     */
    public void testCommand(String expectedFilePrefix, int numOfRuns, String... command)
            throws IOException, InterruptedException {
        Assert.assertTrue(expectedFilePrefix != null);

        String testsPrefix= basePath.toString() + "/" + expectedFilePrefix;
        File stdoutFile = new File(testsPrefix + ".actual.out");
        File stderrFile = new File(testsPrefix + ".actual.err");
        File inFile = new File(testsPrefix + ".in");
        
        // compile regex patterns
        List<Pattern> expectedPatterns = new ArrayList<>();
        for (String regex : Util.convertFileToString(new File(testsPrefix + ".expected.out")).split("(\n|\r)")) {
            if (!regex.isEmpty()) {
                expectedPatterns.add(Pattern.compile(regex));
            }
        }
        
        ProcessBuilder processBuilder = new ProcessBuilder(command).inheritIO();
        processBuilder.directory(basePathFile);
        processBuilder.redirectOutput(stdoutFile);
        processBuilder.redirectError(stderrFile);
        if (inFile.exists() && !inFile.isDirectory()) {
            processBuilder.redirectInput(inFile);
        }
        
        /*
         * run the command up to a certain number of times and gather the
         * outputs
         */
        for (int i = 0; i < numOfRuns && !expectedPatterns.isEmpty(); i++) {
            Process process = processBuilder.start();
            int returnCode = process.waitFor();
            Assert.assertEquals("Expected no error during " + Arrays.toString(command) + ".", 0, returnCode);
            
            Iterator<Pattern> iter = expectedPatterns.iterator();
            while (iter.hasNext()) {
                if (iter.next().matcher(Util.convertFileToString(stdoutFile)).find()) {
                    iter.remove();
                }
            }
        }
        
        Assert.assertTrue("Unable to match regular expressions: \n\t" +
                        Joiner.on("\n\t").skipNulls().join(expectedPatterns),
                expectedPatterns.isEmpty());
    }

}
