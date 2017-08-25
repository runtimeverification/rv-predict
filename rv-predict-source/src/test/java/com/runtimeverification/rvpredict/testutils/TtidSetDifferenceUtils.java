package com.runtimeverification.rvpredict.testutils;

import com.runtimeverification.rvpredict.trace.producers.base.TtidSetDifference;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

public class TtidSetDifferenceUtils {
    public static void fillMockTtidSetDifference(TtidSetDifference mockTtidSetDifference, int... ttids) {
        for (int ttid : ttids) {
            when(mockTtidSetDifference.contains(ttid)).thenReturn(true);
        }
        when(mockTtidSetDifference.getTtids()).thenReturn(Arrays.stream(ttids).boxed().collect(Collectors.toSet()));
    }

    public static void clearMockTtidSetDifference(TtidSetDifference mockTtidSetDifference) {
        when(mockTtidSetDifference.contains(anyInt())).thenReturn(false);
        when(mockTtidSetDifference.getTtids()).thenReturn(Collections.emptySet());
    }
}
