package rvpredict.analysis;

public class ControlScope {
  public int start, end;
  // some scopes are precise, i.e., the end is closed, e.g., loops;
  // but some are enlarged, i.e., the end is open, e.g., most if statement
  // this could be avoided by parsing the source file directly
  // here, offset is used to adjust this inconsistency, making all ends open
  public int offset;
  public boolean isInfinite;
  public ControlScope(ControlScope sc){
    start = sc.start;
    end = sc.end;
    offset = sc.offset;
    isInfinite = sc.isInfinite;
  }
  public ControlScope(int s, int e){
    start = s; end = e;
    offset = 0;
    isInfinite = false;
  }

  public ControlScope(int s, int e, int o){
    start = s; end = e;
    offset = o;
    isInfinite = false;
  }
  public boolean includes(int ln){
    return (ln >= start) && (ln < end + offset);
  }

  public String toString(){
    return "(" + start + ", " + end + "+" + offset + ")";
  }
}
// vim: tw=100:sw=2
