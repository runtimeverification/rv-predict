package rvpredict.prediction.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.IOException;

import rvpredict.prediction.generic.Event;
import rvpredict.prediction.generic.FormalizedEvent;

import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

public class TraceWriter {
  Writer writer;
  private static int compressionLevel = 1;

  public TraceWriter(String path) throws IOException{
    try {
      writer = new OutputStreamWriter(new DeflaterOutputStream(new FileOutputStream(path), new Deflater(compressionLevel)));
    } catch(Exception e) { e.printStackTrace(); System.exit(1); }
    Runtime.getRuntime().addShutdownHook(new Thread() { @Override public void run() { try {close();} catch(Exception e) { e.printStackTrace(); System.exit(1); } } });
  }

  public void writeEvent(Event event) throws IOException{
    writer.write(event.toString());
    writer.write("\n");
  }

  public void writeEvent(FormalizedEvent event) throws IOException{
    writer.write(event.toString());
    writer.write("\n");
  }

  public void close() throws IOException{
    writer.flush();
    writer.close();
  }
}
// vim: tw=100:sw=2
