package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.log.EventType;
import com.runtimeverification.rvpredict.metadata.Metadata;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.Map;

/**
 * Class reading the trace from an LLVM execution debug log.
 *
 * @author TraianSF
 */
public class ModuleTraceCache extends TraceCache {
    private long gid = 0;
    private long value = 0;
    private static final long INTERRUPT_TID = Long.MAX_VALUE;

    private BufferedReader traceFile = null;
    private final Metadata metadata;
    public ModuleTraceCache(Configuration config, Metadata metadata) {
        super(config, metadata);
        this.metadata = metadata;
    }


    class IndexedStack<Index,Value> {
        private Map<Index, Stack<Value>> indexedStack = new HashMap<>();
        public Value push(Index i, Value v) {
            Stack<Value> stack = indexedStack.get(i);
            if (stack == null) {
                stack = new Stack<Value>();
                indexedStack.put(i,stack);
            }
            return stack.push(v);
        }

        public Value pop(Index i) {
            return indexedStack.get(i).pop();
        }
    }

    private IndexedStack<Long,Integer> threadCallStack = new IndexedStack<>();

    @Override
    public void setup() throws IOException {
        File moduleTraceFile = config.getModuleTraceFile();
        if ("<stdin>".equals(moduleTraceFile.getName())) {
            traceFile = new BufferedReader(new InputStreamReader(System.in));
        } else {
            traceFile = new BufferedReader(new FileReader(moduleTraceFile));
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
        } while (!line.startsWith("<type"));
        String[] parts = line.substring(line.indexOf('<') + 1, line.lastIndexOf('>')).split(";");
        assert parts.length == 4;
        int i = 0;
        EventType type = parseType("type", parts[i++]);
        long tid = parseLong("tid", parts[i++]);
        if (tid == 0) {
            tid = INTERRUPT_TID;
        }
        String locationIdStr = parseString("pc", parts[i++]);
        String addrStr = parseString("addr", parts[i++]);
        int locationId = metadata.getLocationId(locationIdStr);
        int addr = -metadata.getVariableId("",addrStr);
        ++gid;
        config.logger().debug(String.format("<gid:%d;tid:%d;id:%d;addr:%d;value:%d;type:%s>%n",
                gid, tid, locationId, addr, value, type.toString()));
        if (type == EventType.INVOKE_METHOD) {
            threadCallStack.push(tid, locationId);
        }
        if (type == EventType.FINISH_METHOD) {
            locationId = threadCallStack.pop(tid);
        }
        return new Event(gid, tid, locationId, addr, value, type);

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
