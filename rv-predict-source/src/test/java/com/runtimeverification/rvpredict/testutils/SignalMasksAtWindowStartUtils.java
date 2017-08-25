package com.runtimeverification.rvpredict.testutils;

import com.runtimeverification.rvpredict.signals.SignalMask;
import com.runtimeverification.rvpredict.trace.producers.signals.SignalMaskAtWindowStart;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

public class SignalMasksAtWindowStartUtils {
    public static void clearMockSignalMasksAtWindowStart(SignalMaskAtWindowStart mockSignalMaskAtWindowStart) {
        when(mockSignalMaskAtWindowStart.getSignalMasks()).thenReturn(Collections.emptyMap());
        when(mockSignalMaskAtWindowStart.getMask(anyInt())).thenReturn(Optional.empty());
    }

    public static void fillMockSignalMasksAtWindowStart(
            SignalMaskAtWindowStart mockSignalMaskAtWindowStart, Map<Integer, SignalMask> ttidToSignalMaskAtStart) {
        when(mockSignalMaskAtWindowStart.getSignalMasks()).thenReturn(ttidToSignalMaskAtStart);
        when(mockSignalMaskAtWindowStart.getMask(anyInt())).thenReturn(Optional.empty());
        ttidToSignalMaskAtStart.forEach((ttid, mask) ->
                when(mockSignalMaskAtWindowStart.getMask(ttid)).thenReturn(Optional.of(mask)));
    }
}
