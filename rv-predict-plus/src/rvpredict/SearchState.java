package rvpredict;

import java.util.Collection;

interface SearchState<T> {
  public Collection<T> advance();
  public boolean advanceable();
  public boolean done();
  public boolean foundSomething();
  public void report();
  public void vcdone();
}
// vim: tw=100:sw=2
