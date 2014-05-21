// Copyright (c) 2014 Runtime Verification Inc. All Rights Reserved.
package rvpredict.main;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import config.PredictionOptions;
import rvpredict.engine.main.NewRVPredict;
import rvpredict.instrumentation.InstrumentationOptions;
import rvpredict.shared.RVPredictSharedOptions;

/**
 * Wrapper class to run all rvpredict stages: instrumentation, logging, prediction.
 * @author TraianSF
 */
public class Main {
    public static void main(String[] args) {
        RVPredictOptions options = new RVPredictOptions();
        InstrumentationOptions instrumentationOptions = new InstrumentationOptions();
        RVPredictSharedOptions sharedOptions = new RVPredictSharedOptions();
        PredictionOptions predictionOptions = new PredictionOptions();
        JCommander jCommander;
        try {
            jCommander = new JCommander(new Object[]{sharedOptions, options, instrumentationOptions, predictionOptions}, args);
            jCommander.setProgramName("rvpredict");
            if (sharedOptions.command.size() > 1) {
                throw new ParameterException("Expecting exactly one class to exercise");
            }
            if (sharedOptions.help) {
                jCommander.usage();
                return;
            }
            if (!options.instrument && !options.log && !options.predict) {
                options.instrument = options.log = options.predict = true;
            }
            if (options.instrument) {
                rvpredict.instrumentation.Main.run(sharedOptions, instrumentationOptions);
            }
            if (options.log) {
                edu.uiuc.run.Main.run(sharedOptions);
            }
            if (options.predict) {
                NewRVPredict.run(sharedOptions, predictionOptions);
            }

        } catch (ParameterException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
