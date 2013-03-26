package rvpredict;

import rvpredict.logging.Protos;

import java.io.Serializable;

public final class Location implements Serializable {
  private static final long serialVersionUID = 0;
  private final String className;
  private final String methodName;
  private final int jimpleLine;
  public String getClassName() { return className; }
  public String getMethodName() { return methodName; }
  public int getJimpleLine() { return jimpleLine; }
  @Override public int hashCode() { return 0; }
  @Override public boolean equals(final Object o) {
    if (o == null) return false;
    if (!(o instanceof Location)) return false;
    final Location l = (Location)o;
    return className.equals(l.className) && methodName.equals(l.methodName) && jimpleLine == l.jimpleLine;
  }
  Location(final Protos.Location loc) {
    className = loc.getClassName();
    methodName = loc.getMethodName();
    jimpleLine = loc.getJimpleLine();
  }
  @Override public String toString() { return className + methodName + ":" + rvpredict.util.Util.extractLineNumber(this); }

}

// vim: tw=100:sw=2
//Test