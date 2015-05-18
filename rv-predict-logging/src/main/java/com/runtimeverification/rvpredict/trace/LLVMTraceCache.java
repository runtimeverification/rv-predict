package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.metadata.Metadata;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Traian on 23.04.2015.
 */
public class LLVMTraceCache extends TraceCache {
    private BufferedReader traceFile = null;
    private final Metadata metadata;
    private static AtomicLong globalId = new AtomicLong(-1);
    public LLVMTraceCache(Configuration config, Metadata metadata) {
        super(config, metadata);
        this.metadata = metadata;
    }

    @Override
    public void setup() throws IOException {
        traceFile = new BufferedReader(new FileReader(config.getLogDir()));
    }


    @Override
    protected void readEvents(long fromIndex, long toIndex) throws IOException {
        for (long i = fromIndex; i < toIndex; i++) {
            Event event = getNextEvent();
            if (event == null) break;
            assert i == event.getGID();
            events[((int) (i % events.length))] = event;
        }
    }

    protected Event getNextEvent() throws IOException {
        String line;
        do {
            line = traceFile.readLine();
        } while (line != null && (line.contains("<null>") || !line.startsWith("<gid")));
        if (line == null) {
            return null;
        }
        String[] parts = line.substring(line.indexOf('<') + 1, line.lastIndexOf('>')).split(";");
        assert parts.length == 9;
        long gid = parseLong("gid", parts[0]);
        long tid = parseLong("tid", parts[1]);
        long id = parseLong("id", parts[2]);
        long addr = parseLong("addr", parts[3]);
        long value = parseLong("value", parts[4]);
        EventType type = parseType("type", parts[5]);
        String fn = parseString("fn", parts[6]);
        String file = parseString("file", parts[7]);
        int ln = (int) parseLong("line", parts[8]);
        System.out.printf("<gid:%d;tid:%d;id:%d;addr:%d;value:%d;type:%s;fn:%s;file:%s;line:%d>%n",
                gid, tid, id, addr, value, type.toString(), fn, file, ln);
        gid = globalId.incrementAndGet();
        id = metadata.getLocationId(String.format("<id:%d;fn:%s;file:%s;line:%d>", id, fn, file, ln));
        return new Event(gid, tid, (int) id, addr, value, type);

    }

    private EventType parseType(String attr, String part) {
        String[] parts = part.split(":");
        assert parts.length == 2;
        assert parts[0].equals(attr);
        return EventType.valueOf(parts[1]);
    }

    private String parseString(String attr, String part) {
        assert attr.equals(part.substring(0,part.indexOf(':')));
        return part.substring(part.indexOf(':') + 1);
    }

    private long parseLong(String attr, String part) {
        String[] parts = part.split(":");
        assert parts.length == 2;
        assert parts[0].equals(attr);
        return Long.valueOf(parts[1]);
    }


}
