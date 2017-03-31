package com.runtimeverification.rvpredict.log.compact;

import java.nio.ByteBuffer;
import java.util.List;

public class ReadableAggregateData implements ReadableData {
    private List<ReadableData> childData;
    private int size;
    
    public void setData(List<ReadableData> childData) {
        this.childData = childData;
        this.size = childData.stream().mapToInt(ReadableData::size).sum();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public void read(ByteBuffer buffer) throws InvalidTraceDataException {
        for (ReadableData child : childData) {
            child.read(buffer);
        }
    }
}
