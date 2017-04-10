package com.runtimeverification.rvpredict.log.compact.readers;

import com.runtimeverification.rvpredict.log.compact.CompactEventReader;
import com.runtimeverification.rvpredict.log.compact.datatypes.Generation;

public class ChangeOfGenerationReader {
    public static CompactEventReader.Reader createReader() {
        return new SimpleDataReader<>(
                Generation::new,
                (context, compactEventFactory, generation) ->
                        compactEventFactory.changeOfGeneration(context, generation.getAsLong()));
    }
}
