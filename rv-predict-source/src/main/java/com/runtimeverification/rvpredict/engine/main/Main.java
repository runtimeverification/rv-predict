package com.runtimeverification.rvpredict.engine.main;

import static com.runtimeverification.rvpredict.config.Configuration.JAVA_EXECUTABLE;
import static com.runtimeverification.rvpredict.config.Configuration.RV_PREDICT_JAR;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.instrument.Agent;
import com.runtimeverification.rvpredict.util.Logger;

import com.runtimeverification.licensing.Licensing;

/**
 * @author TraianSF
 * @author YilongL
 */
public class Main {

    private static Configuration config;

    /**
     * The entry point of RV-Predict when it is started by script.
     */
    public static void main(String[] args) {
        Licensing licensingSystem = new Licensing(Agent.class.toString(), "predict");
        licensingSystem.promptForLicense();

        config = Configuration.instance(args);

        if (config.isLogging() || config.isProfiling()) {
            if (config.getJavaArguments().isEmpty()) {
                config.logger().report("You must provide a class or a jar to run.",
                        Logger.MSGTYPE.ERROR);
                config.usage();
                System.exit(1);
            }

            execApplication();
        } else {
            /* must be in only_predict or only_llvm_predict mode */
            assert config.isOfflinePrediction() || config.isLLVMPrediction();
            new RVPredict(config).start();
        }
    }

    /**
     * Executes the application in a subprocess.
     */
    private static void execApplication() {
        List<String> args = new ArrayList<>();
        args.add(JAVA_EXECUTABLE);
        args.add("-XX:hashCode=1"); // see #issue 500 identityHashCode collisions
        args.add("-javaagent:" + RV_PREDICT_JAR + "=" + createAgentArgs());
        args.addAll(config.getJavaArguments());

        Process process = null;
        try {
            process = new ProcessBuilder(args).start();
            StreamGobbler errorGobbler = StreamGobbler.spawn(process.getErrorStream(), System.err);
            StreamGobbler outputGobbler = StreamGobbler.spawn(process.getInputStream(), System.out);
            StreamGobbler.spawn(System.in, new PrintStream(process.getOutputStream()), true /* flush */);

            process.waitFor();

            errorGobbler.join();
            outputGobbler.join();
        } catch (IOException ignored) {
        } catch (InterruptedException e) {
            if (process != null) {
                process.destroy();
            }
            e.printStackTrace();
        }
    }

    /**
     * Formats the RV-Predict specific options from the command line
     * as a string of options which can be passed to the -javaagent
     * JVM option.
     *
     * It basically iterates through all arguments and builds a string out of them
     * taking care to wrap any argument containing spaces using
     * {@link #escapeString(String)}.
     *
     * @return the -javaagent options corresponding to the user command line
     */
    private static String createAgentArgs() {
        return createAgentArgs(Arrays.asList(config.getRVPredictArguments()));
    }

    static String createAgentArgs(Collection<String> rvPredictArguments) {
        StringBuilder agentOptions = new StringBuilder();
        for (String arg : rvPredictArguments) {
            agentOptions.append(escapeString(arg)).append(" ");
        }
        return agentOptions.toString();
    }

    private static String escapeString(String s) {
        return s.contains(" ") ? "\\\"" + s + "\\\"" : s;
    }

}
