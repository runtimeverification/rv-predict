package rvpredict.db;

import rvpredict.trace.EventType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Class for representing an event as it is recorded in the log
 * @author TraianSF
 */
public class EventItem {
    public long GID;
    public long TID;
    public int ID;
    public long ADDRL;
    public long ADDRR;
    public long VALUE;
    public EventType TYPE;

    /**
     * Constructor of the EventItem class
     * @param gid global identifier / primary key of the event
     * @param tid thread identifier primary key (see {@link rvpredict.trace.TraceInfo#threadIdNamemap})
     * @param id object identifier
     * @param addrl location identifier (see {@link }
     * @param addrr
     * @param value
     * @param type
     */
    public EventItem(long gid, long tid, int id, long addrl, long addrr, long value, EventType type) {
        this.GID = gid;
        this.TID = tid;
        this.ID = id;
        this.ADDRL = addrl;
        this.ADDRR = addrr;
        this.VALUE = value;
        this.TYPE = type;
    }

    public void toStream(DataOutputStream stream) throws IOException {
        stream.writeLong(GID);
        stream.writeLong(TID);
        stream.writeInt(ID);
        stream.writeLong(ADDRL);
        stream.writeLong(ADDRR);
        stream.writeLong(VALUE);
        stream.writeByte(TYPE.ordinal());
    }

    public static EventItem fromStream(DataInputStream stream) throws IOException {
        return new EventItem(
                stream.readLong(),
                stream.readLong(),
                stream.readInt(),
                stream.readLong(),
                stream.readLong(),
                stream.readLong(),
                EventType.values()[stream.readByte()]);

    }
}
