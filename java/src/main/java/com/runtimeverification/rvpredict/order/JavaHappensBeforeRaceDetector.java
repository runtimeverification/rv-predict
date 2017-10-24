package com.runtimeverification.rvpredict.order;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.metadata.MetadataInterface;

/**
 * {@see OrderedRaceDetector} implementing the {@see JavaHappensBefore} ordering
 *
 * @author TraianSF
 */
public class JavaHappensBeforeRaceDetector extends OrderedRaceDetector {

    public JavaHappensBeforeRaceDetector(Configuration config, MetadataInterface metadata) {
        super(config, metadata, new JavaHappensBefore(metadata));
    }

}
