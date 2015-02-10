package com.runtimeverification.rvpredict.log;

import com.runtimeverification.rvpredict.trace.EventType;

/**
 * Class for representing an event as it is recorded in the log
 * @author TraianSF
 */
public class EventItem {
    public final long GID;
    public final long TID;
    public final int ID;
    public final long ADDRL;
    public final int ADDRR;
    public final long VALUE;
    public final EventType TYPE;

    public static final long SIZEOF_LONG = 8;
    public static final long SIZEOF_INT = 4;
    public static final long SIZEOF_EVENT_TYPE = 1;

    /**
     * constant representing the size of the event item on disk (no. of bytes).
     * This should be updated whenever structure of the class is changed.
     */
    public static final long SIZEOF
            = SIZEOF_LONG       //GID
            + SIZEOF_LONG       //TID
            + SIZEOF_INT        //ID
            + SIZEOF_LONG       //ADDRL
            + SIZEOF_INT        //ADDRR
            + SIZEOF_LONG       //VALUE
            + SIZEOF_EVENT_TYPE //TYPE
            ;
    /**
     * Constructor of the EventItem class
     * @param gid global identifier / primary key of the event
     * @param tid thread identifier primary key
     * @param id object identifier
     * @param addrl statement location identifier
     * @param addrr index (for arrays)
     * @param value value for events carrying a value
     * @param type type of event
     */
    public EventItem(long gid, long tid, int id, long addrl, int addrr, long value, EventType type) {
        this.GID = gid;
        this.TID = tid;
        this.ID = id;
        this.ADDRL = addrl;
        this.ADDRR = addrr;
        this.VALUE = value;
        this.TYPE = type;
    }
}
