package rvpredict.log;

import java.io.DataOutputStream;
import java.io.IOException;

import rvpredict.trace.EventType;

/**
 * Class for representing an event as it is recorded in the log
 * @author TraianSF
 */
public class EventItem {
    public final long GID;
    public final long TID;
    public final int ID;
    public final int ADDRL;
    public final int ADDRR;
    public final long VALUE;
    public final EventType TYPE;

    public static final int SIZEOF_LONG = 8;
    public static final int SIZEOF_INT = 4;
    public static final int SIZEOF_EVENT_TYPE = 1;

    /**
     * constant representing the size of the event item on disk (no. of bytes).
     * This should be updated whenever structure of the class is changed.
     */
    public static final int SIZEOF
            = SIZEOF_LONG       //GID
            + SIZEOF_LONG       //TID
            + SIZEOF_INT        //ID
            + SIZEOF_INT        //ADDRL
            + SIZEOF_INT        //ADDRR
            + SIZEOF_LONG       //VALUE
            + SIZEOF_EVENT_TYPE //TYPE
            ;
    /**
     * Constructor of the EventItem class
     * @param gid global identifier / primary key of the event
     * @param tid thread identifier primary key
     * @param id statement location identifier
     * @param addrl object identifier
     * @param addrr index (for arrays)
     * @param value value for events carrying a value
     * @param type type of event
     */
    public EventItem(long gid, long tid, int id, int addrl, int addrr, long value, EventType type) {
        this.GID = gid;
        this.TID = tid;
        this.ID = id;
        this.ADDRL = addrl;
        this.ADDRR = addrr;
        this.VALUE = value;
        this.TYPE = type;
    }

    public void writeTo(DataOutputStream out) throws IOException {
        out.writeLong(GID);
        out.writeLong(TID);
        out.writeInt(ID);
        out.writeInt(ADDRL);
        out.writeInt(ADDRR);
        out.writeLong(VALUE);
        out.writeByte(TYPE.ordinal());
    }

    public static EventItem readFrom(EventInputStream in) throws IOException {
        return new EventItem(
                in.readLong(),
                in.readLong(),
                in.readInt(),
                in.readInt(),
                in.readInt(),
                in.readLong(),
                EventType.values()[in.readByte()]);
    }
}
