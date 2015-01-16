/*******************************************************************************
 * Copyright (c) 2013 University of Illinois
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package rvpredict.logging;

import java.util.concurrent.atomic.AtomicLong;

import rvpredict.config.Configuration;
import rvpredict.db.EventItem;
import rvpredict.trace.EventType;

/**
 * Class encapsulating functionality for recording events
 *
 * @author TraianSF
 *
 */
public class LoggingEngine {

    private final AtomicLong globalEventID  = new AtomicLong(0);
    private final LoggingServer loggingServer;
    private final Configuration config;
    private volatile boolean shutdown = false;


    /**
     * Method invoked at the end of the logging task, to insure that
     * all data is recorded before concluding.
     */
    public void finishLogging() {
        shutdown = true;
        loggingServer.finishLogging();
    }

    public LoggingEngine(Configuration config) {
        this.config = config;
        loggingServer = startLogging();
    }

    public Configuration getConfig() {
        return config;
    }

    private LoggingServer startLogging() {
        final LoggingServer loggingServer = new LoggingServer(this);
        Thread loggingServerThread = new Thread(loggingServer);
        loggingServerThread.setDaemon(true);
        loggingServerThread.start();
        return loggingServer;
    }


    /**
     * Checks if we are in the process of class loading or instrumentation.
     * <p>
     * Note that this method would not be necessary if we had mock for
     * fundamental JDK classes that are used in class loading and
     * instrumentation, i.e, {@code ArrayList}, {@code HashMap}, {@code Vector},
     * etc.
     */
    private boolean skipSavingEvent() {
        StackTraceElement[] stackTraceElems = Thread.currentThread().getStackTrace();
        if (stackTraceElems.length == 0) {
            return false;
        }

        // TODO(YilongL): implement a profiling mechanism to see the top
        // event-producing classes

        String entryClass = stackTraceElems[stackTraceElems.length - 1].getClassName();
        /* YilongL: note that we cannot skip saving event by checking
         * entryClass.startsWith("java") because java.lang.Thread.run() */
        if (entryClass.startsWith("sun")) {
            // sun.instrument.InstrumentationImpl.loadClassAndCallPremain
            return true;
        }

        /* a typical stack trace of class loading plus agent instrumentation:
         *      ...
         *      at rvpredict.instrumentation.Agent.transform(Agent.java:144)
         *      at sun.instrument.TransformerManager.transform(TransformerManager.java:188)
         *      at sun.instrument.InstrumentationImpl.transform(InstrumentationImpl.java:428)
         *      at java.lang.ClassLoader.defineClass1(Native Method)
         *      at java.lang.ClassLoader.defineClass(ClassLoader.java:760)
         *      at java.security.SecureClassLoader.defineClass(SecureClassLoader.java:142)
         *      at java.net.URLClassLoader.defineClass(URLClassLoader.java:455)
         *      at java.net.URLClassLoader.access$100(URLClassLoader.java:73)
         *      at java.net.URLClassLoader$1.run(URLClassLoader.java:367)
         *      at java.net.URLClassLoader$1.run(URLClassLoader.java:361)
         *      at java.security.AccessController.doPrivileged(Native Method)
         *      at java.net.URLClassLoader.findClass(URLClassLoader.java:360)
         *      at java.lang.ClassLoader.loadClass(ClassLoader.java:424)
         *      at sun.misc.Launcher$AppClassLoader.loadClass(Launcher.java:308)
         *      at java.lang.ClassLoader.loadClass(ClassLoader.java:357)
         *      ...
         */
        for (StackTraceElement e : stackTraceElems) {
            String className = e.getClassName();
            if (className.startsWith("java.lang.ClassLoader") || className.startsWith("sun")) {
                return true;
            }
        }

        return false;
    }

    /**
     * Logs an {@link rvpredict.db.EventItem} to the trace.
     *
     * @see rvpredict.db.EventItem#EventItem(long, long, int, long, long, long, rvpredict.trace.EventType)
     *      for a more elaborate description of the parameters.
     *
     * @param eventType  type of event being recorded
     * @param id location id of the event
     * @param addrl  additional information identifying the event
     * @param addrr additional information identifying the event
     * @param value data involved in the event
     */
    public void saveEvent(EventType eventType, int id, long addrl, long addrr, long value) {
        if (shutdown || skipSavingEvent()) return;

        long gid = globalEventID.incrementAndGet();
        long tid = Thread.currentThread().getId();
        EventItem e = new EventItem(gid, tid, id, addrl, addrr, value, eventType);
        loggingServer.getOutputStream().writeEvent(e);
    }

    /**
     * Wrapper for {@link #saveEvent(rvpredict.trace.EventType, int, long, long, long)}
     * The missing arguments default to 0.
     */
    public void saveEvent(EventType eventType, int locId, long arg) {
        saveEvent(eventType, locId, arg, 0, 0);
    }

    /**
     * Wrapper for {@link #saveEvent(rvpredict.trace.EventType, int, long, long, long)}
     * The missing arguments default to 0.
     */
    public void saveEvent(EventType eventType, int locId) {
        saveEvent(eventType, locId, 0, 0, 0);
    }

    public long getGlobalEventID() {
        return globalEventID.get();
    }
}
