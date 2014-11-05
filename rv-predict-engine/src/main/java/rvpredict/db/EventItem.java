package rvpredict.db;

import java.io.Serializable;

/**
* Created by Traian on 04.11.2014.
*/
public class EventItem implements Serializable {
    public long GID;
    public long TID;
    public int ID;
    public String ADDR;
    public String VALUE;
    public byte TYPE;

    public EventItem(long gid, long tid, int sid, String addr, String value, byte type) {
        this.GID = gid;
        this.TID = tid;
        this.ID = sid;
        this.ADDR = addr;
        this.VALUE = value;
        this.TYPE = type;
    }
}
