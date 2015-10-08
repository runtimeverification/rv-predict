package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.metadata.Metadata;
import com.runtimeverification.rvpredict.util.Logger;

import java.io.*;
import java.util.*;

/**
 * Class reading the trace from an LLVM execution debug log.
 *
 * @author TraianSF
 */
public class LLVMTraceCache extends TraceCache {
    private BufferedReader traceFile = null;
    private final Metadata metadata;
    public LLVMTraceCache(Configuration config, Metadata metadata) {
        super(config, metadata);
        this.metadata = metadata;
    }

    @Override
    public void setup() throws IOException {
        File llvmTraceFile = config.getLLVMTraceFile();
        if ("<stdin>".equals(llvmTraceFile.getName())) {
            traceFile = new BufferedReader(new InputStreamReader(System.in));
        } else {
            traceFile = new BufferedReader(new FileReader(llvmTraceFile));
        }
    }


    @Override
    protected List<RawTrace> readEvents(long fromIndex, long toIndex) throws IOException {
        Map<Long, List<Event>> rawTracesTable = new HashMap<>();

        for (long i = fromIndex; i < toIndex; i++) {
            Event event = getNextEvent();
            if (event == null) break;
            List<Event> events = rawTracesTable.get(event.getTID());
            if (events == null) {
                events = new ArrayList<>();
                rawTracesTable.put(event.getTID(), events);
            }
            events.add(event);
        }
        List<RawTrace> rawTraces =  new ArrayList<>();
        for (List<Event> events : rawTracesTable.values()) {
            int length = getNextPowerOfTwo(events.size());
            rawTraces.add(new RawTrace(0, events.size(), events.toArray(new Event[length])));
        }
        return rawTraces;
    }

    protected Event getNextEvent() throws IOException {
        String line;
        do {
            line = traceFile.readLine();
            if (line == null) {
                return null;
            }
            if (line.startsWith("<locId")) {
                parseLocationInfo(line);
                continue;
            }
            if (line.startsWith("<varId")) {
                parseVarInfo(line);
                continue;
            }
        } while (!line.startsWith("<gid"));
        String[] parts = line.substring(line.indexOf('<') + 1, line.lastIndexOf('>')).split(";");
        assert parts.length == 6;
        long gid = parseLong("gid", parts[0]);
        long tid = parseLong("tid", parts[1]);
        int locationId = (int) parseLong("id", parts[2]);
        long addr = parseLong("addr", parts[3]);
        long value = parseLong("value", parts[4]);
        EventType type = parseType("type", parts[5]);
        config.logger().debug(String.format("<gid:%d;tid:%d;id:%d;addr:%d;value:%d;type:%s>%n",
                gid, tid, locationId, addr, value, type.toString()));
        if (type == EventType.START) {
            metadata.addThreadCreationInfo(value, tid, locationId);
        }
        if (type == EventType.START || type == EventType.JOIN) {
            addr = value;
            value = 0;
        }
        return new Event(gid, tid, locationId, addr, value, type);

    }

    private void parseVarInfo(String line) {
        String[] parts = line.substring(line.indexOf('<') + 1, line.lastIndexOf('>')).split(";");
        assert parts.length == 2;
        int varId = (int) parseLong("varId", parts[0]);
        String desc = parseString("desc", parts[1]);
        metadata.setVariableSig(varId, desc);
    }

    private void parseLocationInfo(String line) {
        String[] parts = line.substring(line.indexOf('<') + 1, line.lastIndexOf('>')).split(";");
        assert parts.length == 4;
        int locId = (int) parseLong("locId", parts[0]);
        String fn = parseString("fn", parts[1]);
        String file = parseString("file", parts[2]);
        file = file.substring(file.lastIndexOf('/') + 1);
        int ln = (int) parseLong("line", parts[3]);
        metadata.setLocationSig(locId, String.format("<fn:%s;file:%s;line:%d>", fn, file, ln));
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
