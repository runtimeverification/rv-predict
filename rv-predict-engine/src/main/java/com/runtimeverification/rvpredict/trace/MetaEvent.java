package com.runtimeverification.rvpredict.trace;

/**
 * Event that carries additional meta-information about the trace.
 * <p>
 * {@code MetaEvent}'s are not used in building constraint directly.
 *
 * @author YilongL
 */
public class MetaEvent extends AbstractEvent {

    public MetaEvent(long GID, long TID, int ID, EventType type) {
        super(GID, TID, ID, type);
        assert type == EventType.CLINIT_ENTER || type == EventType.CLINIT_EXIT
                || type == EventType.INVOKE_METHOD || type == EventType.FINISH_METHOD;
    }

}
