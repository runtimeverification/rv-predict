package com.runtimeverification.rvpredict.config;

import com.beust.jcommander.*;
import com.runtimeverification.rvpredict.util.Logger;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by traian on 30.09.2015.
 */
public class PredictionConfiguration extends Configuration {

    @Parameter(description = "Prediction location")
    private List<String> predict_dir = new ArrayList<>();

    public final static String opt_llvm_predict = "--llvm-predict";
    @Parameter(names = opt_llvm_predict, description = "Run prediction on given llvm trace", hidden = true, descriptionKey = "1400")
    public boolean is_llvm;

    public static PredictionConfiguration instance(String[] args) {
        PredictionConfiguration config = new PredictionConfiguration();
        config.parseArguments(args);
        return config;
    }


    @Override
    protected void parseArguments(String[] args) {
        super.parseArguments(args);
        if (predict_dir.isEmpty()) {
            logger().report("You must provide the location of the log", Logger.MSGTYPE.ERROR);
            usage();
            System.exit(1);
        }
        if (is_llvm) {
            setLogDir(Paths.get(predict_dir.get(0)).toAbsolutePath().getParent().toString());
        } else {
            setLogDir(Paths.get(predict_dir.get(0)).toAbsolutePath().toString());
            prediction = OFFLINE_PREDICTION;
        }
    }

    @Override
    protected String getProgramName() {
        return "java -jar " + getBasePath() + "/rv-predict.jar";
    }

    @Override
    protected String getUsageHeader() {
        return "Usage: " + getProgramName() + " <log_location> [options]" + "\n";
    }

    @Override
    public boolean isLogging() {
        return false;
    }

    public boolean isLLVMPrediction() {
        return is_llvm;
    }

    public File getLLVMTraceFile() {
        return Paths.get(predict_dir.get(0)).toFile();
    }

}
