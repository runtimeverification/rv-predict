package com.runtimeverification.rvpredict.testutils;

import com.runtimeverification.rvpredict.trace.ThreadInfo;
import com.runtimeverification.rvpredict.trace.producers.base.OtidToSignalDepthToTtidAtWindowStart;

import java.util.Arrays;
import java.util.OptionalInt;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

public class OtidToSignalDepthToTtidAtWindowStartUtils {
    public static void clearMockOtidToSignalDepthToTtidAtWindowStart(
            OtidToSignalDepthToTtidAtWindowStart mockOtidToSignalDepthToTtidAtWindowStart) {
        when(mockOtidToSignalDepthToTtidAtWindowStart.getTtid(anyLong(), anyInt())).thenReturn(OptionalInt.empty());
    }

    public static void fillMockOtidToSignalDepthToTtidAtWindowStart(
            OtidToSignalDepthToTtidAtWindowStart mockOtidToSignalDepthToTtidAtWindowStart,
            ThreadInfo... threadInfos) {
        clearMockOtidToSignalDepthToTtidAtWindowStart(mockOtidToSignalDepthToTtidAtWindowStart);
        Arrays.stream(threadInfos).forEach(threadInfo ->
                when(mockOtidToSignalDepthToTtidAtWindowStart.getTtid(
                        threadInfo.getOriginalThreadId(), threadInfo.getSignalDepth()))
                        .thenReturn(OptionalInt.of(threadInfo.getId())));
    }
}
