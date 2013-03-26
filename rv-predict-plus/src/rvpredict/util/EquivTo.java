package rvpredict.util;

//class to return EquivHashcode and use EquivTo for equals so that
//hash sets actually do what I want them to do.  This is a serious
//flaw in the Java standard library design.
public final class EquivTo<T extends soot.EquivTo> {
  private  T v;

  public EquivTo(T v){
    this.v = v;
  }

  public T get() {
    return v;
  }

  @Override public int hashCode(){
    return v.equivHashCode();
  }

  @Override public boolean equals(Object o){
    if(!(o instanceof EquivTo)) return false;
    return v.equivTo(((EquivTo) o).get());
  }

  @Override public String toString(){
    return "ET(" + v.toString() + ")";
  }
}

