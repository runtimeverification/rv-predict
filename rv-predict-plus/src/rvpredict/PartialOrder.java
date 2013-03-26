package rvpredict;

public interface PartialOrder<T> {
  static enum Ordering { LessThan, GreaterThan, Equal, Incomparable }
  public Ordering compare(T t);
}
// vim: tw=100:sw=2
