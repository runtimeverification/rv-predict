package rvpredict.main;

import com.beust.jcommander.Parameter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Traian on 13.05.2014.
 */
public class RVPredictOptions {
    @Parameter(names = {"--instrument"}, description = "Perform (only) instrumentation.")
    public boolean instrument = false;
    @Parameter(names = {"--log"}, description = "Perform (only) execution logging.")
    public boolean log = false;
    @Parameter(names = {"--predict"}, description = "Perform (only) the prediction phase.")
    public boolean predict = false;

}
