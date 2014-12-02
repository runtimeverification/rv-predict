package rvpredict.db;

import rvpredict.trace.EventType;

/**
 * Class for representing an event as it is recorded in the log
 * @author TraianSF
 */
public class EventItem {
    /**
     * constant representing the size of the event item on disk (no. of bytes)
     */
    public static final long SIZEOF = 45;
    public final long GID;
    public final long TID;
    public final int ID;
    public final long ADDRL;
    public final long ADDRR;
    public final long VALUE;
    public final EventType TYPE;

    /**
     * Constructor of the EventItem class
     * @param gid global identifier / primary key of the event
     * @param tid thread identifier primary key (see {@link rvpredict.trace.TraceInfo#threadIdNamemap})
     * @param id object identifier
     * @param addrl statement location identifier (see {@link rvpredict.trace.TraceInfo#stmtIdSigMap})
     * @param addrr index (for arrays)
     * @param value value for events carrying a value
     * @param type type of event
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
}
