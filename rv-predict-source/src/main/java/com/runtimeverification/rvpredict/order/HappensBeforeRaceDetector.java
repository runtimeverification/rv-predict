package com.runtimeverification.rvpredict.order;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.metadata.MetadataInterface;

public class HappensBeforeRaceDetector extends OrderedRaceDetector {

    public HappensBeforeRaceDetector(Configuration config, MetadataInterface metadata) {
        super(config, metadata, new HappensBefore(metadata));
    }

}
