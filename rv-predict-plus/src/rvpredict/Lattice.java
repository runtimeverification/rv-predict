package rvpredict;

public interface Lattice<T> extends PartialOrder<T> {
  public T meet(T t);
  public T join(T t);
}
// vim: tw=100:sw=2
