package com.runtimeverification.rvpredict.testutils;

import com.runtimeverification.rvpredict.trace.RawTrace;
import com.runtimeverification.rvpredict.trace.ThreadInfo;
import com.runtimeverification.rvpredict.trace.producers.base.ThreadInfosComponent;
import com.runtimeverification.rvpredict.util.Constants;

import java.util.Arrays;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

public class ThreadInfosComponentUtils {
    public static void fillMockThreadInfosComponentFromTraces(
            ThreadInfosComponent mockThreadInfosComponent, RawTrace... rawTraces) {
        Arrays.stream(rawTraces).map(RawTrace::getThreadInfo).forEach(threadInfo ->
                addThreadInfoToMock(mockThreadInfosComponent, threadInfo));
    }

    public static void fillMockThreadInfosComponentFromThreadInfos(
            ThreadInfosComponent mockThreadInfosComponent, ThreadInfo... threadInfos) {
        when(mockThreadInfosComponent.getParentThread(anyInt())).thenReturn(OptionalInt.empty());
        Arrays.stream(threadInfos).forEach(threadInfo ->
                addThreadInfoToMock(mockThreadInfosComponent, threadInfo));
    }

    public static void clearMockThreadInfosComponent(ThreadInfosComponent mockThreadInfosComponent) {
        when(mockThreadInfosComponent.getThreadType(anyInt())).thenReturn(null);
        when(mockThreadInfosComponent.getThreadInfo(anyInt())).thenReturn(null);
        when(mockThreadInfosComponent.getOriginalThreadIdForTraceThreadId(anyInt()))
                .thenReturn(Constants.INVALID_THREAD_ID);
        when(mockThreadInfosComponent.getSignalDepth(anyInt())).thenReturn(-1);
        when(mockThreadInfosComponent.getParentThread(anyInt())).thenReturn(OptionalInt.empty());
        when(mockThreadInfosComponent.getSignalNumber(anyInt())).thenReturn(OptionalLong.empty());
    }

    private static void addThreadInfoToMock(ThreadInfosComponent mockThreadInfosComponent, ThreadInfo threadInfo) {
        when(mockThreadInfosComponent.getThreadType(threadInfo.getId())).thenReturn(threadInfo.getThreadType());
        when(mockThreadInfosComponent.getThreadInfo(threadInfo.getId())).thenReturn(threadInfo);
        when(mockThreadInfosComponent.getOriginalThreadIdForTraceThreadId(threadInfo.getId()))
                .thenReturn(threadInfo.getOriginalThreadId());
        when(mockThreadInfosComponent.getSignalDepth(threadInfo.getId())).thenReturn(threadInfo.getSignalDepth());
        when(mockThreadInfosComponent.getParentThread(threadInfo.getId())).thenReturn(threadInfo.getParentTtid());
        when(mockThreadInfosComponent.getSignalNumber(threadInfo.getId())).thenReturn(threadInfo.getSignalNumber());
    }
}
