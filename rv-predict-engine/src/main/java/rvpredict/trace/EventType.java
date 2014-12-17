package rvpredict.trace;

/**
* Enumeration of all types of events considered during logging and prediction.
* @author TraianSF
*/
public enum EventType {
    INIT, READ, WRITE, LOCK, UNLOCK, PRE_WAIT, WAIT, WAIT_INTERRUPTED, NOTIFY, NOTIFY_ALL, START, JOIN, BRANCH;
}
