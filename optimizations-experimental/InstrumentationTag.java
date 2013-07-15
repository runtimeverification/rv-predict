package rvpredict.instrumentation;

import soot.tagkit.Tag;

public class InstrumentationTag implements Tag {
  private static InstrumentationTag v = new InstrumentationTag();
  public static InstrumentationTag v() { 
    return v; 
  }
  public InstrumentationTag(){}
  public String getName(){
    return "InstrumentationTag";
  } 
  public byte[] getValue() {
    return null;
  }
}

