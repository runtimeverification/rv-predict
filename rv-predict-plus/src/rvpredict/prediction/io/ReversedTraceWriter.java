package rvpredict.prediction.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import rvpredict.logging.OldCompressedReversedBlockOutput;

import rvpredict.prediction.generic.Event;
import rvpredict.prediction.generic.FormalizedEvent;

public class ReversedTraceWriter {
  OldCompressedReversedBlockOutput writer;

  public ReversedTraceWriter(String path) throws IOException{
    writer = new OldCompressedReversedBlockOutput(path,10000);
  }

  public void writeEvent(Event event) throws IOException{
    writer.addEvent(event.toString());
    writer.addEvent("\n");
  }

  public void writeEvent(FormalizedEvent event) throws IOException{
    writer.addEvent(event.toString());
    writer.addEvent("\n");
  }

  public void close() throws IOException{
    writer.close();
  }
}
// vim: tw=100:sw=2
