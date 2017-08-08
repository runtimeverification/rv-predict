package com.runtimeverification.rvpredict.trace.producers.base;

import com.google.common.collect.ImmutableMap;
import com.runtimeverification.rvpredict.producerframework.LeafProducerWrapper;
import com.runtimeverification.rvpredict.producerframework.TestProducerModule;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.OptionalInt;

public class OtidToMainTtidTest {
    private TestProducerModule module = new TestProducerModule();

    @Test
    public void findsTtids() {
        LeafProducerWrapper<Map<Long, Integer>, OtidToMainTtid> otidToMainTtid =
                new LeafProducerWrapper<>(new OtidToMainTtid(), module);
        module.reset();
        otidToMainTtid.set(ImmutableMap.of(5L, 3, 4L, 2));
        Assert.assertEquals(OptionalInt.of(3), otidToMainTtid.getComputed().getTtid(5L));
        Assert.assertEquals(OptionalInt.of(2), otidToMainTtid.getComputed().getTtid(4L));
        Assert.assertFalse(otidToMainTtid.getComputed().getTtid(3L).isPresent());
    }
}
