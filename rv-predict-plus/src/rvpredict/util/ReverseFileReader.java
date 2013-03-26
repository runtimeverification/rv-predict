package rvpredict.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class ReverseFileReader {
  private BufferedReader file;
  private ArrayList<String> lines;
  private int position;

  public ReverseFileReader (String filename) throws IOException {
    lines = new ArrayList<String>();
    file = new BufferedReader(new FileReader(filename));
    String line = file.readLine();

    while (line != null) {
      lines.add(line);
      line = file.readLine();
    }

    position = lines.size();
    lines.trimToSize();
  }

  // Read one line from the current position towards the beginning
  public String readLine() throws IOException {
    if (position == 0)
      return null;
    else
      return lines.get(--position);
  }

  public void close() throws IOException {
    file.close();
  }
}
// vim: tw=100:sw=2

