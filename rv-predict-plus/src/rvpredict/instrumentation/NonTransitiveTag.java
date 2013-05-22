package rvpredict.instrumentation;

import soot.tagkit.Tag;

public class NonTransitiveTag implements Tag {
  private static NoInstrumentTag v = new NoInstrumentTag();
  public static NoInstrumentTag v() { 
    return v; 
  }
  public NonTransitiveTag(){}
  public String getName(){
    return "NonTransitiveTag";
  } 
  public byte[] getValue() {
    return null;
  }
}
