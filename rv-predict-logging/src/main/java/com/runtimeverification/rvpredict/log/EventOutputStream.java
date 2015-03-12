package com.runtimeverification.rvpredict.log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An event output stream lets an application to write {@link EventItem} to an
 * output stream in a portable way.
 *
 * @author TraianSF
 * @author YilongL
 */
public abstract class EventOutputStream extends DataOutputStream {

    public EventOutputStream(OutputStream out) {
        super(out);
    }

    public final void writeEvent(EventItem eventItem) throws IOException {
        eventItem.writeTo(this);
    }

}
