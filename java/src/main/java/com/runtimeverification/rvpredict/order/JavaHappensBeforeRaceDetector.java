package com.runtimeverification.rvpredict.order;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.metadata.MetadataInterface;
import com.runtimeverification.rvpredict.violation.RaceSerializer;

/**
 * {@see OrderedRaceDetector} implementing the {@see JavaHappensBefore} ordering
 *
 * @author TraianSF
 */
public class JavaHappensBeforeRaceDetector extends OrderedRaceDetector {

    public JavaHappensBeforeRaceDetector(Configuration config, MetadataInterface metadata, RaceSerializer serializer) {
        super(config, metadata, serializer, new JavaHappensBefore(metadata));
    }

}
