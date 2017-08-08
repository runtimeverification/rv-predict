package com.runtimeverification.rvpredict.trace.producers.base;

import com.runtimeverification.rvpredict.trace.ThreadInfo;
import com.runtimeverification.rvpredict.trace.ThreadInfos;
import com.runtimeverification.rvpredict.trace.ThreadType;
import com.runtimeverification.rvpredict.producerframework.LeafProducer;

import java.util.OptionalInt;
import java.util.OptionalLong;

public class ThreadInfosComponent extends LeafProducer<ThreadInfos> {
    public int getSignalDepth(int ttid) {
        return getThreadInfo(ttid).getSignalDepth();
    }

    public ThreadType getThreadType(int ttid) {
        return getThreadInfo(ttid).getThreadType();
    }

    public OptionalLong getSignalNumber(Integer ttid) {
        return getThreadInfo(ttid).getSignalNumber();
    }

    public ThreadInfo getThreadInfo(Integer ttid) {
        return get().getThreadInfo(ttid);
    }

    public OptionalInt getParentThread(int ttid) {
        return getThreadInfo(ttid).getParentTtid();
    }

    public long getOriginalThreadIdForTraceThreadId(int ttid) {
        return get().getThreadInfo(ttid).getOriginalThreadId();
    }
}
