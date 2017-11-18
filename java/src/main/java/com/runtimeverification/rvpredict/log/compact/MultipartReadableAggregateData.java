package com.runtimeverification.rvpredict.log.compact;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * Base class for multipart data objects. These are used when the data structure
 * is not fully known before reading it, e.g. when it has fields of variable size.
 */
public class MultipartReadableAggregateData {
    private List<ReadableAggregateDataPart> parts;
    private int currentPart = 0;

    /**
     * Must be called before starting to read the data object.
     */
    public void startReading(TraceHeader header) throws InvalidTraceDataException {
        currentPart = 0;
        parts.get(0).initialize(header);
    }

    /**
     * Returns true if the data object is not fully read.
     */
    public boolean stillHasPartsToRead() {
        return currentPart < parts.size();
    }

    /**
     * @return The size of the next part to be read.
     */
    public int nextPartSize() {
        return parts.get(currentPart).size();
    }

    /**
     * Reads the next data part, preparing to read the next one (if any).
     */
    public void readNextPartAndAdvance(TraceHeader header, ByteBuffer buffer) throws InvalidTraceDataException {
        parts.get(currentPart).read(buffer);
        currentPart++;
        if (currentPart < parts.size()) {
            parts.get(currentPart).initialize(header);
        }
    }

    /**
     * Configures the event parts that will be loaded by the current object.
     *
     * Normally it should be called once, in the constructor.
     */
    protected void setData(ReadableAggregateDataPart... parts) {
        this.parts = Arrays.asList(parts);
    }
}
