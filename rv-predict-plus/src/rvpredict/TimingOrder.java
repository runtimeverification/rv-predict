package rvpredict;

// Merge Equal or Incomparable into Concurrent while < implies Earlier than and > implies Later
public interface TimingOrder<T> extends PartialOrder<T> {
  static enum Ordering { EarlierThan, LaterThan, Concurrent }
  public Ordering relativeTime(T t);
}
// vim: tw=100:sw=2
