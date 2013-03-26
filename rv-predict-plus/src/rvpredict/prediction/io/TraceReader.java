package rvpredict.prediction.io;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;


import rvpredict.prediction.generic.Event;
import rvpredict.prediction.generic.FormalizedEvent;

import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class TraceReader {

  BufferedReader reader;
  public long pos;

  public TraceReader(String path) throws IOException {
    reader = new BufferedReader(new InputStreamReader(new InflaterInputStream(new FileInputStream(path), new Inflater())));
    pos = 0;
  }

  public Event next() throws Exception {
    String aLine = reader.readLine();
    if (aLine == null)
      return null;
    else {
      pos ++;
      return new Event(aLine);
    }
  }

  public FormalizedEvent nextF() throws Exception {
    String aLine = reader.readLine();
    if (aLine == null)
      return null;
    else {
      pos ++;
      return new FormalizedEvent(aLine, pos);
    }
  }

  public void reset() throws IOException {
    pos = 0;
    reader.reset();
  }

  public void close() throws IOException {
    pos = -1;
    reader.close();
  }
}
// vim: tw=100:sw=2
