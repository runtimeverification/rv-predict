package rvpredict.traces;

import java.io.File;
import java.util.Iterator;

import soot.options.Options;

import rvpredict.logging.Protos;
import rvpredict.logging.CompressedReversedBlockInputStream;
import rvpredict.logging.CompressedReversedBlockOutput;

public class TraceReverser {
  private final static int blocksize = 10000; // In events?
  public static ForwardTrace reverse(final BackwardTrace t) {
    return new ForwardTrace() {
      private CompressedReversedBlockInputStream crbis;
      private Iterator<Protos.Event> eventIter;
      {
        CompressedReversedBlockOutput crbo = new CompressedReversedBlockOutput(Options.v().output_dir()+File.separator+"rev_tmp_"+System.identityHashCode(this), blocksize);
        while (t.hasNext()) {
          try {
            crbo.addEvent(t.next());
          } catch (Exception e) { e.printStackTrace(); System.exit(1); }
        }
        crbo.flush();
        try {
          crbis = new CompressedReversedBlockInputStream(Options.v().output_dir(),"rev_tmp_"+System.identityHashCode(this),"rvpf");
          eventIter = Protos.Events.parseFrom(crbis).getEventList().iterator();
          assert eventIter.hasNext() : "Empty compressed block";
        } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }
      @Override public void remove() {};
      @Override public Protos.Event next() {
        try {
          if (eventIter.hasNext())
            return eventIter.next();
          else if (crbis.hasNext()) {
            crbis.next();
            eventIter = Protos.Events.parseFrom(crbis).getEventList().iterator();
            assert eventIter.hasNext() : "Empty compressed block";
            return eventIter.next();
          } else
            return null;
        } catch (Exception e) { e.printStackTrace(); System.exit(1); }
        return null;
      }
      @Override public boolean hasNext() {
        return eventIter.hasNext() || crbis.hasNext();
      }
    };
  }
  public static BackwardTrace reverse(ForwardTrace trace) {
    return null;
  }
}
// vim: tw=100:sw=2
