package com.runtimeverification.rvpredict;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.io.Files;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Helper class for testing the output of external commands against the expected output.
 * This code is currently copied over from rv-monitor.
 * Maybe we could consider a shared library if more duplication occurs.
 * @author TraianSF
 */
public class TestHelper {

    private static final int NUM_OF_WORKER_THREADS = 4;
    private final File basePathFile;
    private final Path basePath;
    private boolean inputTest;

    /**
     * Initializes the {@code basePath} field to the parent directory of the specified file path
     * @param modulePath  path to the file which prompted this test, used to establish working dir
     * @param inputTest whether this test uses a precomputed log (and thus it only requires prediction)
     */
    public TestHelper(Path modulePath, boolean inputTest)   {
        this.basePath = modulePath;
        this.inputTest = inputTest;
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
     * @throws Exception when the test fails.
     */
    public int testCommand(
            final String expectedFilePrefix, int numOfRuns, String rvRoot,
            final String... command)
            throws Exception {
        Assert.assertTrue(expectedFilePrefix != null);

        final String testsPrefix = basePath.toString() + "/" + expectedFilePrefix;
        final File inFile = new File(testsPrefix + ".in");

        // compile regex patterns
        String[] expected =  Files.toString(new File(testsPrefix + ".expected.out"),
                Charset.defaultCharset()).split("[\n\r]");

        List<Task> tasks = IntStream.range(0, numOfRuns).boxed()
                .map(id -> new Task(testsPrefix, id, rvRoot, basePathFile, inFile, inputTest, command))
                .collect(Collectors.toList());

        return testCommand(expected, tasks, command);
    }

    @VisibleForTesting
    static int testCommand(
            String[] expected,
            List<Task> tasks,
            final String... command)
            throws Exception {
        // compile regex patterns
        final List<Pattern> expectedPatterns = new ArrayList<>();
        for (String regex : expected) {
            regex = regex.trim();
            if (!regex.isEmpty()) {
                expectedPatterns.add(Pattern.compile(regex, Pattern.DOTALL));
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(NUM_OF_WORKER_THREADS);
        Map<Task, Future<Integer>> taskToFuture = tasks.stream()
                .collect(Collectors.toMap(x -> x, executor::submit));

        List<String> outputs = new ArrayList<>();
        int numOfDoneTasks = 0;
        while (!expectedPatterns.isEmpty() && !taskToFuture.isEmpty()) {
            List<Task> tasksDone = new ArrayList<>();
            for (Task task : taskToFuture.keySet()) {
                Future<Integer> future = taskToFuture.get(task);
                if (future.isDone()) {
                    numOfDoneTasks++;
                    int returnCode = future.get();
                    String output = task.output();
                    outputs.add(output);
                    if (returnCode != 0) {
                        Assert.fail("Expected no error during " + Arrays.toString(command)
                                + " but received " + returnCode + ".\n"
                                + output);
                    } else {
                        extractRaceReports(output).forEach(report ->
                                expectedPatterns.removeIf(p -> p.matcher(report).matches()));
                    }
                    tasksDone.add(task);
                }
            }
            taskToFuture.keySet().removeAll(tasksDone);
        }
        executor.shutdownNow();

        Assert.assertTrue("Unable to match regular expressions: \n\t" +
                        Joiner.on("\n\t").skipNulls().join(expectedPatterns) + "\n\t" + outputs,
                expectedPatterns.isEmpty());
        return numOfDoneTasks;
    }

    private static List<String> extractRaceReports(String output) {
        List<String> result = new ArrayList<>();
        int fromIdx = 0;
        while (true) {
            int posStartAnchor = output.indexOf("Data race on ", fromIdx);
            if (posStartAnchor < 0) {
                break;
            }
            int posEndAnchor = output.indexOf("Data race on ", posStartAnchor + 1);
            if (posEndAnchor < 0) {
                posEndAnchor = output.length();
            }
            result.add(output.substring(posStartAnchor, posEndAnchor));
            fromIdx = posEndAnchor;
        }
        if (result.isEmpty()) {
            result.add("No races found");
        }
        return result;
    }

    @VisibleForTesting
    static class Task implements Callable<Integer> {

        private final ProcessBuilder processBuilder;

        private final File stdoutFile;

        private final File stderrFile;

        private Task(
                String testsPrefix, int id,
                String rvRoot, File basePathFile, File inputFile, boolean inputTest,
                String... command) {
            stdoutFile = new File(testsPrefix + id + ".actual.out");
            stderrFile = new File(testsPrefix + id + ".actual.err");
            if (inputTest) {
                for (int i = 0; i < command.length; i++) {
                    if (command[i].equals("$in")) {
                        command[i] = inputFile.getAbsolutePath();
                    }
                }
            }
            processBuilder = new ProcessBuilder(command).inheritIO();
            processBuilder.directory(basePathFile);
            processBuilder.redirectOutput(stdoutFile);
            processBuilder.redirectError(stderrFile);
            processBuilder.environment().put("RV_ROOT", rvRoot);
            if (!inputTest && inputFile.exists() && !inputFile.isDirectory()) {
                processBuilder.redirectInput(inputFile);
            }
        }

        @Override
        public Integer call() throws Exception {
            Process process = processBuilder.start();
            try {
                return process.waitFor();
            } catch (InterruptedException e) {
                process.destroyForcibly();
                throw e;
            }
        }

        @VisibleForTesting
        String output() throws IOException {
            return Files.toString(stderrFile, Charset.defaultCharset());
        }

    }

}
