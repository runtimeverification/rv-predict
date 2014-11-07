package rvpredict.db;

import trace.AbstractNode;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
* Created by Traian on 04.11.2014.
*/
public class EventItem {
    private final long gid;
    private final long tid;
    private final int id;
    private final long addrl;
    private final long addrr;
    private final long value;
    private final AbstractNode.TYPE type;
    public long GID;
    public long TID;
    public int ID;
    public long ADDRL;
    public long ADDRR;
    public long VALUE;
    public AbstractNode.TYPE TYPE;

    public EventItem(long gid, long tid, int id, long addrl, long addrr, long value, AbstractNode.TYPE type) {
        this.gid = gid;
        this.tid = tid;
        this.id = id;
        this.addrl = addrl;
        this.addrr = addrr;
        this.value = value;
        this.type = type;
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
                AbstractNode.TYPE.of(stream.readByte()));

    }
}
