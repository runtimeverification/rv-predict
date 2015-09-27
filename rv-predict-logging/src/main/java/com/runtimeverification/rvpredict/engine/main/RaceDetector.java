package com.runtimeverification.rvpredict.engine.main;

import java.io.*;
import java.lang.reflect.Field;
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
import com.runtimeverification.rvpredict.util.Constants;
import com.runtimeverification.rvpredict.violation.Race;

/**
 * Detects data races from a given {@link Trace} object.
 *
 * @author YilongL
 */
public class RaceDetector implements Constants {

    private final Configuration config;

    private final Map<String, Race> sigToRealRace = new HashMap<>();

    private final List<String> reports = new ArrayList<>();

    private final Z3Filter z3filter;

    private final com.microsoft.z3.Solver solver;

    public RaceDetector(Configuration config) {
        this.config = config;
        Context z3Context = getZ3Context(config);
        this.z3filter = new Z3Filter(z3Context, config.windowSize);
        try {
            /* setup the solver */
            // mkSimpleSolver < mkSolver < mkSolver("QF_IDL")
            this.solver = z3Context.mkSimpleSolver();
            Params params = z3Context.mkParams();
            params.add("timeout", config.solver_timeout * 1000);
            solver.setParameters(params);
        } catch (Z3Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getRaceReports() {
        return reports;
    }

    private boolean isThreadSafeLocation(Trace trace, int locId) {
        String locationSig = trace.metadata().getLocationSig(locId);
        return locationSig.startsWith("java.util.concurrent")
            || locationSig.startsWith("java.util.stream")
            || locationSig.substring(locationSig.lastIndexOf('.')).startsWith(".class$");
    }

    private Map<String, List<Race>> computeUnknownRaceSuspects(Trace trace) {
        Map<String, List<Race>> sigToRaceCandidates = new HashMap<>();
        trace.eventsByThreadID().forEach((tid1, events1) -> {
            trace.eventsByThreadID().forEach((tid2, events2) -> {
                if (tid1 < tid2) {
                    events1.forEach(e1 -> {
                        events2.forEach(e2 -> {
                            if ((e1.isWrite() && e2.isReadOrWrite() ||
                                    e1.isReadOrWrite() && e2.isWrite())
                                    && e1.getAddr() == e2.getAddr()
                                    && !trace.metadata().isVolatile(e1.getAddr())
                                    && !isThreadSafeLocation(trace, e1.getLocId())
                                    && !trace.isInsideClassInitializer(e1)
                                    && !trace.isInsideClassInitializer(e2)) {
                                Race race = new Race(e1, e2, trace);
                                if (!config.suppressPattern.matcher(race.getRaceLocationSig())
                                        .matches()) {
                                    String raceSig = race.toString();
                                    if (!sigToRealRace.containsKey(raceSig)) {
                                        sigToRaceCandidates.computeIfAbsent(raceSig,
                                                x -> new ArrayList<>()).add(race);
                                    }
                                }
                            }
                        });
                    });
                }
            });
        });
        return sigToRaceCandidates;
    }

    public void run(Trace trace) {
        if (!trace.mayContainRaces()) {
            return;
        }

        Map<String, List<Race>> sigToRaceSuspects = computeUnknownRaceSuspects(trace);
        if (sigToRaceSuspects.isEmpty()) {
            return;
        }

        Map<String, Race> result = MaximalCausalModel.create(trace, z3filter, solver)
                .checkRaceSuspects(sigToRaceSuspects);
        sigToRealRace.putAll(result);
        result.forEach((sig, race) -> {
            String report = race.generateRaceReport();
            reports.add(report);
            config.logger().reportRace(report);
        });
    }

    public static String getNativeLibraryPath() {
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

    public static String getNativeLibraryName() {
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

    public Context getZ3Context(Configuration config) {
        String nativeLibraryName = getNativeLibraryName();
        String nativeLibraryPath = getNativeLibraryPath() + "/" + nativeLibraryName;
        Context context = null;
        try {
            String logDir = config.getLogDir();
            File nativeLibraryFile = new File(logDir, nativeLibraryName);
            if (!nativeLibraryFile.exists()) {
                nativeLibraryFile.deleteOnExit();
                InputStream in = getClass().getResourceAsStream(nativeLibraryPath);
                BufferedInputStream reader = new BufferedInputStream(in);
                byte[] buffer = new byte[8192];
                int read = -1;
                FileOutputStream fos = new FileOutputStream(nativeLibraryFile);
                BufferedOutputStream writer = new BufferedOutputStream(fos);

                while ((read = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, read);
                }
                reader.close();
                writer.close();
            }

            // Very dirty hack to add our native libraries dir to the array of system paths
            // dependent on the implementation of java.lang.ClassLoader (although that seems pretty consistent)
            //TODO: Might actually be better to alter and recompile the z3 java bindings
            Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
            sysPathsField.setAccessible(true);
            String[] sysPaths = (String[]) sysPathsField.get(null);
            String oldPath = sysPaths[0];
            sysPaths[0] = logDir;

            context = new Context();

            //restoring the previous system path
            sysPaths[0] = oldPath;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return context;
    }
}
