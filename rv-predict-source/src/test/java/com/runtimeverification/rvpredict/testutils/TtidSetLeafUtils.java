package com.runtimeverification.rvpredict.testutils;

import com.runtimeverification.rvpredict.trace.producers.base.TtidSetLeaf;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

public class TtidSetLeafUtils {
    public static void fillMockTtidSetLeaf(TtidSetLeaf mockTtids, int... ttids) {
        when(mockTtids.contains(anyInt())).thenReturn(false);
        when(mockTtids.getTtids()).thenReturn(Arrays.stream(ttids).boxed().collect(Collectors.toSet()));
        Arrays.stream(ttids).forEach(ttid -> when(mockTtids.contains(ttid)).thenReturn(true));
    }
}
