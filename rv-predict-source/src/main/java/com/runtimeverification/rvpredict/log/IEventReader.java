package com.runtimeverification.rvpredict.log;

import java.io.Closeable;
import java.io.IOException;
/**
 * An interface for reading events; to be used in TraceCache
 * @author EricPtS
 *
 */

public interface IEventReader extends Closeable {

    Event readEvent() throws IOException;

    public Event lastReadEvent();
}