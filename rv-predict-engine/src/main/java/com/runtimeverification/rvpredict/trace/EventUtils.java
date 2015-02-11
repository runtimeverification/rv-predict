package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.log.EventItem;

/**
 * Contains static utility methods for {@link Event}.
 *
 * @author YilongL
 */
public class EventUtils {

    public static Event of(EventItem item) {
        switch (item.TYPE) {
            case READ:
                return new ReadEvent(item.GID, item.TID, item.ID, item.ADDRL, item.ADDRR, item.VALUE);
            case WRITE:
                return new WriteEvent(item.GID, item.TID, item.ID, item.ADDRL, item.ADDRR, item.VALUE);
            case WRITE_LOCK:
            case WRITE_UNLOCK:
            case READ_LOCK:
            case READ_UNLOCK:
            case WAIT_REL:
            case WAIT_ACQ:
            case START:
            case PRE_JOIN:
            case JOIN:
            case JOIN_MAYBE_FAILED:
                long syncObj = (long)item.ADDRL << 32 | item.ADDRR & 0xFFFFFFFFL;
                return new SyncEvent(item.GID, item.TID, item.ID, item.TYPE, syncObj);
            case BRANCH:
                return new BranchEvent(item.GID, item.TID, item.ID);
            default:
                assert false : "unexpected event type: " + item.TYPE;
                return null;
        }
    }

}
