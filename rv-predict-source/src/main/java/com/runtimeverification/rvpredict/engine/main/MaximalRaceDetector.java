package com.runtimeverification.rvpredict.engine.main;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.microsoft.z3.Context;
import com.microsoft.z3.Params;
import com.microsoft.z3.Z3Exception;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.smt.MaximalCausalModel;
import com.runtimeverification.rvpredict.smt.visitors.Z3Filter;
import com.runtimeverification.rvpredict.trace.Trace;
import com.runtimeverification.rvpredict.violation.Race;

/**
 * Detects data races from a given {@link Trace} object.
 *
 * @author YilongL
 */
public class MaximalRaceDetector implements RaceDetector {

    private final Configuration config;

    private final Map<String, Race> sigToRealRace = new HashMap<>();

    private final List<String> reports = new ArrayList<>();

    private final Z3Filter z3filter;

    private final com.microsoft.z3.Solver fastSolver;
    private final com.microsoft.z3.Solver soundSolver;

    public MaximalRaceDetector(Configuration config) {
        this.config = config;
        Context z3Context;
        z3Context = getZ3Context();
        this.z3filter = new Z3Filter(z3Context, config.windowSize);
        try {
            /* setup the solver */
            // mkSimpleSolver < mkSolver < mkSolver("QF_IDL")
            this.fastSolver = z3Context.mkSimpleSolver();
            Params params = z3Context.mkParams();
            params.add("timeout", config.solver_timeout * 900);
            fastSolver.setParameters(params);
            this.soundSolver = z3Context.mkSimpleSolver();
            params = z3Context.mkParams();
            params.add("timeout", config.solver_timeout * 100);
            soundSolver.setParameters(params);
        } catch (Z3Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getRaceReports() {
        return reports;
    }

    private boolean isThreadSafeLocation(Trace trace, long locId) {
        String locationSig = trace.metadata().getLocationSig(locId);
        if (locationSig.startsWith("java.util.concurrent")
            || locationSig.startsWith("java.util.stream")) {
            return true;
        } else {
            int index = locationSig.lastIndexOf('.');
            if (index != -1) {
                return locationSig.substring(index).startsWith(".class$");
            }
        }
        return false;
    }

    private Map<String, List<Race>> computeUnknownRaceSuspects(Trace trace) {
        Map<String, List<Race>> sigToRaceCandidates = new HashMap<>();
        trace.eventsByThreadID().forEach((ttid1, events1) ->
                trace.eventsByThreadID().forEach((ttid2, events2) -> {
                    if (ttid1 >= ttid2 || !trace.threadsCanOverlap(ttid1, ttid2)) {
                        return;
                    }
                    events1.forEach(e1 -> events2.forEach(e2 -> {
                        if ((e1.isWrite() && e2.isReadOrWrite() || e1.isReadOrWrite() && e2.isWrite())
                                && e1.getDataInternalIdentifier() == e2.getDataInternalIdentifier()
                                // TODO(virgil): Metadata should work with external identifiers.
                                // This code works fine for Java events and we don't handle volatile data
                                // for compact events, so this is fine for now.
                                && !trace.metadata().isVolatile(e1.getDataInternalIdentifier())
                                && !isThreadSafeLocation(trace, e1.getLocationId())
                                && !trace.isInsideClassInitializer(e1)
                                && !trace.isInsideClassInitializer(e2)) {
                            Race race = new Race(e1, e2, trace, config);
                            if (!config.suppressPattern.matcher(race.getRaceDataSig())
                                    .matches()) {
                                String raceSig = race.toString();
                                if (!sigToRealRace.containsKey(raceSig)) {
                                    sigToRaceCandidates.computeIfAbsent(raceSig,
                                            x -> new ArrayList<>()).add(race);
                                }
                            }
                        }
                    }));
                }));
        return sigToRaceCandidates;
    }

    @Override
    public void run(Trace trace) {
        if (!trace.mayContainRaces()) {
            return;
        }

        Map<String, List<Race>> sigToRaceSuspects = computeUnknownRaceSuspects(trace);
        if (sigToRaceSuspects.isEmpty()) {
            return;
        }

        Map<String, Race> result =
                MaximalCausalModel.create(
                        trace, z3filter, fastSolver, soundSolver, config.detectInterruptedThreadRace())
                        .checkRaceSuspects(sigToRaceSuspects);
        sigToRealRace.putAll(result);
        result.forEach((sig, race) -> {
            String report = race.generateRaceReport();
            reports.add(report);
            config.logger().reportRace(report);
        });
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

    private static Path z3LibDir = getZ3LibDir();

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
    public static Context getZ3Context() {
        extractZ3Library(z3LibDir);
        Context context = null;
        try {
            // Very dirty hack to add our native libraries dir to the array of system paths
            // dependent on the implementation of java.lang.ClassLoader (although that seems pretty consistent)
            Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
            sysPathsField.setAccessible(true);
            String[] sysPaths = (String[]) sysPathsField.get(null);
            String oldPath = sysPaths[0];
            sysPaths[0] = z3LibDir.toString();

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
}
