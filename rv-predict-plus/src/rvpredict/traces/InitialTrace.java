package rvpredict.traces;

import java.io.File;
import java.io.FilenameFilter;

import java.util.HashSet;
import java.util.Iterator;

import soot.options.Options;

import rvpredict.logging.Protos;
import rvpredict.logging.CompressedReversedBlockInputStream;

public class InitialTrace extends BackwardTrace {
   private Iterator<String> threadTraces;
   private CompressedReversedBlockInputStream curStream;
   private Iterator<Protos.Event> eventIter;

   public int initialStackDepth = 0;

   public InitialTrace() {
      HashSet<String> trs = new HashSet<String>();
      File dir = new File(Options.v().output_dir());
      String[] fileList = dir.list(new FilenameFilter() {
         public boolean accept(File dir, String name) {
            return name.startsWith("trace_");
         }
      });
      for (String s : fileList)
         trs.add(s.split("[.]")[0]);
      threadTraces = trs.iterator();
      try {
         curStream = new CompressedReversedBlockInputStream(Options.v().output_dir(),threadTraces.next(),"rvpf");
         initialStackDepth = curStream.initialStackDepth;
         try{
           eventIter = Protos.Events.parseFrom(curStream).getEventList().iterator();
           
           //Jeff Test
//           while(eventIter.hasNext())
//           {
//        	   Protos.Event e = eventIter.next();
//        	   System.out.println(e);
//           }
           
           
         } catch (com.google.protobuf.InvalidProtocolBufferException e) {
           System.out.println(rvpredict.GUIMain.RED + " last trace file was not closed correctly, skipping");
           if (curStream.hasNext()){
             curStream.next();
             eventIter = Protos.Events.parseFrom(curStream).getEventList().iterator();
           }
         }
         assert eventIter.hasNext() : "Empty compressed block";
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
   }
   @Override public void remove() { (new Exception("remove() unsupported")).printStackTrace(); System.exit(1); }
   @Override public boolean hasNext() { return eventIter.hasNext() || curStream.hasNext() || threadTraces.hasNext(); }
   @Override public Protos.Event next() {
     try {
       if (eventIter.hasNext())
         return eventIter.next();
       else if (curStream.hasNext()) {
         curStream.next();
         eventIter = Protos.Events.parseFrom(curStream).getEventList().iterator();
         assert eventIter.hasNext() : "Empty compressed block";
         return eventIter.next();
       } else if (threadTraces.hasNext()){
         curStream = new CompressedReversedBlockInputStream(Options.v().output_dir(),threadTraces.next(),"rvpf");
         eventIter = Protos.Events.parseFrom(curStream).getEventList().iterator();
         assert eventIter.hasNext() : "Empty compressed block";
         return eventIter.next();
       }
     } catch (Exception e) { e.printStackTrace(); System.exit(1); }
     return null;
   }
}

// vim: tw=100:sw=2
