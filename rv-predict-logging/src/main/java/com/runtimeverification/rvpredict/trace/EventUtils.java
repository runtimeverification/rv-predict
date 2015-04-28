package com.runtimeverification.rvpredict.trace;

/**
 * Contains static utility methods for {@link Event}.
 *
 * @author YilongL
 */
public class EventUtils {

//    public static Event of(EventItem item) {
//        switch (item.TYPE) {
//            case READ:
//                return new ReadEvent(item.GID, item.TID, item.ID, item.ADDRL, item.ADDRR, item.VALUE);
//            case WRITE:
//                return new WriteEvent(item.GID, item.TID, item.ID, item.ADDRL, item.ADDRR, item.VALUE);
//            case WRITE_LOCK:
//            case WRITE_UNLOCK:
//            case READ_LOCK:
//            case READ_UNLOCK:
//            case WAIT_REL:
//            case WAIT_ACQ:
//            case START:
//            case JOIN:
//                long syncObj = (long)item.ADDRL << 32 | item.ADDRR & 0xFFFFFFFFL;
//                return new SyncEvent(item.GID, item.TID, item.ID, item.TYPE, syncObj);
//            case CLINIT_ENTER:
//            case CLINIT_EXIT:
//            case INVOKE_METHOD:
//            case FINISH_METHOD:
//                return new MetaEvent(item.GID, item.TID, item.ID, item.TYPE);
//            default:
//                assert false : "unexpected event type: " + item.TYPE;
//                return null;
//        }
//    }

}
