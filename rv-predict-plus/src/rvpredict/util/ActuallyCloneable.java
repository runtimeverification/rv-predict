package rvpredict.util;

import java.lang.Cloneable;

public interface ActuallyCloneable<T> extends Cloneable {
  public T clone();
}

// vim: tw=100:sw=2
