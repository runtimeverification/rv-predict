package rvpredict.util;

// Immutable Strings using pointer comparision for equality

import java.util.HashMap;

public class IString {
  private static HashMap<String, IString> stringRef;
  private static HashMap<IString, String> refString;

  static {
    stringRef = new HashMap<String, IString>();
    refString = new HashMap<IString, String>();
  }

  public static IString get(String s){
    if(stringRef.containsKey(s)){
      return stringRef.get(s);
    }
    else {
      IString ret = new IString();
      stringRef.put(s, ret);
      refString.put(ret, s);
      return ret;
    }
  }

  public String toString(){
    return refString.get(this);
  }
}
// vim: tw=100:sw=2
