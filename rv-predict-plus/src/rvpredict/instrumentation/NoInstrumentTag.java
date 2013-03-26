package rvpredict.instrumentation;

import soot.tagkit.Tag;

public class NoInstrumentTag implements Tag {
  private static NoInstrumentTag v = new NoInstrumentTag();
  public static NoInstrumentTag v() { 
    return v; 
  }
  public NoInstrumentTag(){}
  public String getName(){
    return "NoInstrumentTag";
  } 
  public byte[] getValue() {
    return null;
  }
}
