package com.runtimeverification.rvpredict;

import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.runtimeverification.rvpredict.util.Util;

import org.junit.Assert;

/**
 * Helper class for testing the output of external commands against the expected output.
 * This code is currently copied over from rv-monitor.
 * Maybe we could consider a shared library if more duplication occurs.
 * @author TraianSF
 */
public class TestHelper {

    private final File basePathFile;
    private final Path basePath;

    /**
     * Initializes the {@code basePath} field to the parent directory of the specified file path
     * @param filePath  path to the file which prompted this test, used to establish working dir
     */
    public TestHelper(String filePath)   {
        FileSystem fileSystem = FileSystems.getDefault();
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
     * @return the number of runs before success
     * @throws IOException
     * @throws InterruptedException
     */
    public int testCommand(final String expectedFilePrefix, int numOfRuns, final String... command)
            throws IOException, InterruptedException {
        Assert.assertTrue(expectedFilePrefix != null);

        final String testsPrefix= basePath.toString() + "/" + expectedFilePrefix;
        final File inFile = new File(testsPrefix + ".in");

        final String[] error = new String[1];
        
        // compile regex patterns
        final List<Pattern> expectedPatterns = new ArrayList<>();
        for (String regex : Util.convertFileToString(new File(testsPrefix + ".expected.out")).split("(\n|\r)")) {
            if (!regex.isEmpty()) {
                expectedPatterns.add(Pattern.compile(regex));
            }
        }


        int n;
        /*
         * run the command up to a certain number of times and gather the
         * outputs
         */
        LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(4) {
            @Override
            public boolean offer(Runnable runnable) {
                try {
                    put(runnable);
                    return true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return false;
            }
        };
        ExecutorService pool = new ThreadPoolExecutor(4,4,0, TimeUnit.SECONDS, workQueue);
        for (n = 0; n < numOfRuns && !expectedPatterns.isEmpty(); n++) {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    if (expectedPatterns.isEmpty() || error[0] != null) return;
                    try {
                        long id = Thread.currentThread().getId();
                        File stdoutFile = new File(testsPrefix + id + ".actual.out");
                        File stderrFile = new File(testsPrefix + id + ".actual.err");
                        ProcessBuilder processBuilder = new ProcessBuilder(command).inheritIO();
                        processBuilder.directory(basePathFile);
                        processBuilder.redirectOutput(stdoutFile);
                        processBuilder.redirectError(stderrFile);
                        if (inFile.exists() && !inFile.isDirectory()) {
                            processBuilder.redirectInput(inFile);
                        }
                        Process process = processBuilder.start();
                        int returnCode = process.waitFor();
                        if (expectedPatterns.isEmpty() || error[0] != null) return;
                        if (returnCode != 0) {
                            error[0] = "Expected no error during " + Arrays.toString(command) + " but received " + returnCode
                                    + ".\n" + Util.convertFileToString(stderrFile);
                            return;
                        }
                        String output = Util.convertFileToString(stdoutFile);
                        synchronized (expectedPatterns) {
                            Iterator<Pattern> iter = expectedPatterns.iterator();
                            while (iter.hasNext()) {
                                if (iter.next().matcher(output).find()) {
                                    iter.remove();
                                }
                            }
                        }
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        pool.shutdown();
        while (!pool.isTerminated()) {
            pool.awaitTermination(1, TimeUnit.SECONDS);
        }
        
        if (error[0] != null) {
            Assert.fail(error[0]);
        }

        Assert.assertTrue("Unable to match regular expressions: \n\t" +
                        Joiner.on("\n\t").skipNulls().join(expectedPatterns),
                expectedPatterns.isEmpty());
        return n;
    }

}
