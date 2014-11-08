package rvpredict.db;

import rvpredict.trace.AbstractNode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
* Created by Traian on 04.11.2014.
*/
public class EventItem {
    public long GID;
    public long TID;
    public int ID;
    public long ADDRL;
    public long ADDRR;
    public long VALUE;
    public AbstractNode.TYPE TYPE;

    public EventItem(long gid, long tid, int sid, long addrl, long addrr, long value, AbstractNode.TYPE type) {
        this.GID = gid;
        this.TID = tid;
        this.ID = sid;
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
        stream.writeByte(TYPE.toByte());
    }

    public static EventItem fromStream(DataInputStream stream) throws IOException {
        return new EventItem(
                stream.readLong(),
                stream.readLong(),
                stream.readInt(),
                stream.readLong(),
                stream.readLong(),
                stream.readLong(),
                AbstractNode.TYPE.valueOf(stream.readByte()));

    }
}
