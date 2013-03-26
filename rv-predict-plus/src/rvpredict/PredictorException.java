package rvpredict;

import java.io.PrintWriter;
import java.io.StringWriter;

public class PredictorException extends Exception {
  private final static long serialVersionUID = 0;
  public PredictorException(String str){
    super(str);
  }

  public static PredictorException report(String location, Exception e){
    StringWriter wr = new StringWriter();
    PrintWriter printer = new PrintWriter(wr);
    e.printStackTrace(printer);
    return new PredictorException("Exception encountered at " + location + ":\b" + wr.toString());
  }
}
// vim: tw=100:sw=2
