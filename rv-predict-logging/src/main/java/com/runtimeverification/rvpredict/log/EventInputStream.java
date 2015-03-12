package com.runtimeverification.rvpredict.log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An event input stream lets an application to read {@link EventItem} from an
 * underlying input stream in a portable way.
 *
 * @author TraianSF
 * @author YilongL
 */
public abstract class EventInputStream extends DataInputStream {

    public EventInputStream(InputStream in) {
        super(in);
    }

    public final EventItem readEvent() throws IOException {
        return EventItem.readFrom(this);
    }

}
