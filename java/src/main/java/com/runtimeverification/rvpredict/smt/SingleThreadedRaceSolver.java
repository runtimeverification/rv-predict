package com.runtimeverification.rvpredict.smt;

import com.microsoft.z3.Context;
import com.microsoft.z3.Params;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import com.microsoft.z3.Z3Exception;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.engine.main.MaximalRaceDetector;
import com.runtimeverification.rvpredict.performance.Profiler;
import com.runtimeverification.rvpredict.performance.ProfilerToken;
import com.runtimeverification.rvpredict.smt.formula.BoolFormula;
import com.runtimeverification.rvpredict.smt.visitors.Z3Filter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

public class SingleThreadedRaceSolver implements RaceSolver {
    private static class Z3Initializer {
        Optional<Path> z3LibDir = Optional.empty();
        Path get() {
            if (z3LibDir.isPresent()) {
                return z3LibDir.get();
            }
            Path dir = getZ3LibDir();
            z3LibDir = Optional.of(dir);
            extractZ3Library(dir);
            return dir;
        }
    }
    private static Z3Initializer z3LibDir = new Z3Initializer();

    private final Z3Filter z3filter;

    private final com.microsoft.z3.Solver fastSolver;
    private final com.microsoft.z3.Solver soundSolver;

    private Optional<WindowData> currentWindowData = Optional.empty();


    public static SingleThreadedRaceSolver createRaceSolver(Configuration config) {
        Context z3Context;
        z3Context = getZ3Context(z3LibDir.get());
        Z3Filter z3filter = new Z3Filter(z3Context, config.windowSize);
        try {
            /* setup the solver */
            // mkSimpleSolver < mkSolver < mkSolver("QF_IDL")
            com.microsoft.z3.Solver fastSolver = z3Context.mkSimpleSolver();
            Params params = z3Context.mkParams();
            params.add("timeout", config.solver_timeout * 900);
            fastSolver.setParameters(params);
            com.microsoft.z3.Solver soundSolver = z3Context.mkSimpleSolver();
            params = z3Context.mkParams();
            params.add("timeout", config.solver_timeout * 100);
            soundSolver.setParameters(params);
            return new SingleThreadedRaceSolver(z3filter, fastSolver, soundSolver);
        } catch (Z3Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SingleThreadedRaceSolver(Z3Filter z3filter, Solver fastSolver, Solver soundSolver) {
        this.z3filter = z3filter;
        this.fastSolver = fastSolver;
        this.soundSolver = soundSolver;
    }

    @Override
    public void checkRace(
            WindowData windowData,
            BoolFormula assertion,
            SolutionReporter solutionReporter) throws Exception {
        startWindowIfNeeded(windowData);
        try (ProfilerToken ignored1 = Profiler.instance().start("Main solver loop")) {
            fastSolver.push();
            fastSolver.add(z3filter.filter(assertion));
            if (fastSolver.check() == Status.SATISFIABLE) {
                try (ProfilerToken ignored2 = Profiler.instance().start("Secondary solver loop")) {
                    soundSolver.push();
                    soundSolver.add(z3filter.filter(assertion));
                    if (soundSolver.check() == Status.SATISFIABLE) {
                        solutionReporter.solution(soundSolver.getModel());
                    }
                    soundSolver.pop();
                }
            }
            fastSolver.pop();
        }
    }

    @Override
    public void generateSolution(WindowData windowData, SolutionReporter solutionReporter) throws Exception {
        startWindowIfNeeded(windowData);
        soundSolver.push();
        if (soundSolver.check() == Status.SATISFIABLE) {
            solutionReporter.solution(soundSolver.getModel());
        }
        soundSolver.pop();
    }

    @Override
    public void finishAllWork() {
    }

    @Override
    public void close() {
        endWindow();
    }

    private void startWindowIfNeeded(WindowData windowData) throws Exception {
        if (currentWindowData.isPresent()) {
            if (currentWindowData.get().getWindowId() != windowData.getWindowId()) {
                endWindow();
                currentWindowData = Optional.of(windowData);
            }
        } else {
            currentWindowData = Optional.of(windowData);
        }
        fastSolver.push();
        soundSolver.push();
        /* translate our formula into Z3 AST format */
        fastSolver.add(z3filter.filter(windowData.getUnsoundButFastPhiTau()));
        soundSolver.add(z3filter.filter(windowData.getSoundPhiTau()));
        for (BoolFormula entry : windowData.getPhiConc()) {
            fastSolver.add(z3filter.filter(entry));
            soundSolver.add(z3filter.filter(entry));
        }
    }

    private void endWindow() {
        if (currentWindowData.isPresent()) {
            fastSolver.pop();
            soundSolver.pop();
            z3filter.clear();
            currentWindowData = Optional.empty();
        }
    }

    private static Context getZ3Context(Path z3LibPath) {
        Context context = null;
        try {
            // Very dirty hack to add our native libraries dir to the array of system paths
            // dependent on the implementation of java.lang.ClassLoader (although that seems pretty consistent)
            Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
            sysPathsField.setAccessible(true);
            String[] sysPaths = (String[]) sysPathsField.get(null);
            String oldPath = sysPaths[0];
            sysPaths[0] = z3LibPath.toString();

            try {
                context = new Context();
            } catch (UnsatisfiedLinkError error) {
                if (Configuration.OS.current() == Configuration.OS.WINDOWS) {
                    String binDir = "'" + Configuration.getBasePath() + "\\bin'";
                    System.err.println("[Error]  RV-Predict must be on the PATH for prediction to run.\n" +
                            "\t Please add " + binDir + " to the PATH.");
                    System.exit(1);
                } else throw error;
            }

            //restoring the previous system path
            sysPaths[0] = oldPath;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return context;
    }

    private static Path getZ3LibDir() {
        Path tmpPath = Paths.get(System.getProperty("java.io.tmpdir"));
        try {
            Path libPath = Files.createTempDirectory(tmpPath, "rvp-libz3-");
            libPath.toFile().deleteOnExit();
            return libPath;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static void extractZ3Library(Path tmpPath) {
        String z3LibraryName = getNativeLibraryName();
        Path z3LibraryTarget = tmpPath.resolve(z3LibraryName);
        z3LibraryTarget.toFile().deleteOnExit();
        if (Files.exists(z3LibraryTarget)) return;
        String z3LibraryPath = getNativeLibraryPath() + "/" + z3LibraryName;
        try {
            z3LibraryTarget.getParent().toFile().mkdirs();
            Path z3LibraryTempPath = Files.createTempFile(z3LibraryTarget.getParent(), "rvpredict-z3-", ".library");
            File z3LibraryTempFile = z3LibraryTempPath.toFile();
            InputStream in = MaximalRaceDetector.class.getResourceAsStream(z3LibraryPath);
            BufferedInputStream reader = new BufferedInputStream(in);
            byte[] buffer = new byte[8192];
            int read;
            FileOutputStream fos = new FileOutputStream(z3LibraryTempFile);
            BufferedOutputStream writer = new BufferedOutputStream(fos);

            while ((read = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, read);
            }
            reader.close();
            writer.close();
            if (!Files.exists(z3LibraryTarget)) {
                Files.move(z3LibraryTempPath, z3LibraryTarget, StandardCopyOption.ATOMIC_MOVE);
            }
        } catch (IOException e) {
            if (!Files.exists(z3LibraryTarget)) {
                throw new RuntimeException(e);
            }
        }
    }

    private static String getNativeLibraryPath() {
        String nativePath = "/native";
        Configuration.OS os = Configuration.OS.current();
        String property = System.getProperty("os.arch");
        String arch = property.endsWith("86") ? "32" : "64";
        switch (os) {
            case OSX:
                nativePath += "/osx";
                break;
            case WINDOWS:
                nativePath += "/windows" + arch;
                break;
            default:
                nativePath += "/linux" + arch;
        }
        return nativePath;
    }

    private static String getNativeLibraryName() {
        Configuration.OS os = Configuration.OS.current();
        switch (os) {
            case OSX:
                return "libz3java.dylib";
            case WINDOWS:
                return "z3java.dll";
            default:
                return "libz3java.so";
        }
    }
}
