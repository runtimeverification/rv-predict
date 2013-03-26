package rvpredict.prediction.io;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import rvpredict.PredictorException;

import rvpredict.prediction.generic.Event;
import rvpredict.prediction.generic.FormalizedEvent;
import rvpredict.util.ReverseFileReader;
import rvpredict.logging.CompressedReversedBlockInputStream;

public class ReverseTraceReader {
  BufferedReader reader;
  public long pos;
  public ReverseTraceReader(String dir, String file) throws IOException{
    reader = new BufferedReader(new InputStreamReader(new CompressedReversedBlockInputStream(dir, file, "rvpf")));
    pos = 0;
  }

  public Event prev() throws IOException,PredictorException {
    String aLine = reader.readLine();
    if (aLine == null)
      return null;
    while (aLine.length() == 0)
      aLine = reader.readLine();
    pos ++;
    //System.out.println(pos);
    return new Event(aLine);
  }

  public FormalizedEvent prevF() throws IOException,PredictorException {
    String aLine = reader.readLine();
    if (aLine == null)
      return null;
    while (aLine.length() == 0)
      aLine = reader.readLine();
    pos ++;
    return new FormalizedEvent(aLine, pos);
  }

  public void close() throws IOException {
    pos = -1;
    reader.close();
  }

  public void translateToXML() throws IOException,PredictorException {
    FormalizedEvent event = prevF();
    while (event != null){

      event = prevF();
    }
  }
}
// vim: tw=100:sw=2
