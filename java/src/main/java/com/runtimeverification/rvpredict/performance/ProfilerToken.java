package com.runtimeverification.rvpredict.performance;

import java.io.Closeable;

public class ProfilerToken implements Closeable {
    private final ItemData itemData;

    ProfilerToken(ItemData itemData) {
        this.itemData = itemData;
    }

    @Override
    public void close() {
        itemData.end();
    }
}
