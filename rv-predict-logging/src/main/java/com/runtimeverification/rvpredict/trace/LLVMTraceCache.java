package com.runtimeverification.rvpredict.trace;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.log.Event;
import com.runtimeverification.rvpredict.log.LLVMEventReader;
import com.runtimeverification.rvpredict.metadata.Metadata;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

/**
 * Class reading the trace from an LLVM execution debug log.
 *
 * @author TraianSF
 * @author EricPtS
 */
public class LLVMTraceCache extends TraceCache {
    private final Metadata metadata;
    
    private final List<LLVMEventReader> LLVMReaders = new ArrayList<> ();
    
    public LLVMTraceCache(Configuration config, Metadata metadata) {
        super(config, metadata);
        this.metadata = metadata;
    }
   
    private void parseVarInfo() throws IOException {
    	BinaryParser in = new BinaryParser(config.getLLVMMetadataFile("var"));
    	while(true) {
	    	try {
	    		int varId = in.readLong().intValue();
		    	String sig = in.readString();
          System.out.println("Processed var: " + Integer.toString(varId) + sig);
		    	metadata.setVariableSig(varId, sig);
	    	} catch (EOFException e) {
	    		break;
	    	}
	    }
    	in.close();
    }
    
    private void parseLocInfo() throws IOException {
    	BinaryParser in = new BinaryParser(config.getLLVMMetadataFile("loc"));
    	while(true) {
	    	try {
	    		int locId = in.readLong().intValue();
		    	String sig = in.readString();
          System.out.println("Processed loc: " + Integer.toString(locId) + sig);
		    	metadata.setLocationSig(locId, sig);
	    	} catch (EOFException e) {
	    		break;
	    	}
    	}
    	in.close();
    }
    
    private void readMetadata() throws IOException {
    	parseVarInfo();
    	parseLocInfo();
    }

    @Override
    public void setup() throws IOException {
    	int logId = 0;
    	Path path = config.getTraceFilePath(logId);
    	while(path.toFile().exists()) {
    		LLVMReaders.add(new LLVMEventReader(path));
    		++logId;
    		path = config.getTraceFilePath(logId);
    	}
    	readMetadata();
    }


    @Override
    protected List<RawTrace> readEvents(long fromIndex, long toIndex) throws IOException {
       List<RawTrace> rawTraces = new ArrayList<>();
       LLVMReaders.sort((r1, r2) -> r1.lastReadEvent().compareTo(r2.lastReadEvent()));
       
       Iterator<LLVMEventReader> iter = LLVMReaders.iterator();
       while(iter.hasNext()) {
    	   LLVMEventReader reader = iter.next();
    	   
    	   if(reader.lastReadEvent().getGID() >= toIndex)
    	   		break;
    	   
    	   Event event = reader.lastReadEvent();
    	   
    	   assert(event.getGID() >= fromIndex);
    	   
    	   int capacity = getNextPowerOfTwo(config.windowSize - 1);
    	   if(config.stacks())
    		   capacity *= 2;
    	   
    	   List<Event> events = new ArrayList<>(capacity);
    	   
    	   do {
           System.out.println("Added event" + event.toString());
    		   events.add(event);
    		   try {
    			   event = reader.readEvent();
    		   } catch (EOFException e) {
    			   iter.remove();
    			   break;
    		   }
    	   } while(event.getGID() < toIndex);
    	   
    	   int len = getNextPowerOfTwo(events.size());
    	   
    	   rawTraces.add(new RawTrace(0, events.size(), events.toArray(new Event[len])));
       }
       
       return rawTraces;
    }

}
