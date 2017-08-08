package com.runtimeverification.rvpredict.producerframework;

import java.util.ArrayList;
import java.util.List;

public class ProducerModule {
    private final List<Producer> producers = new ArrayList<>();
    protected void reset() {
        producers.forEach(Producer::reset);
    }

    void addProducer(Producer producer) {
        producers.add(producer);
    }
}
