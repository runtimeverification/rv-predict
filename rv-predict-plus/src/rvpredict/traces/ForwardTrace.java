package rvpredict.traces;

import java.util.Iterator;
import java.lang.Iterable;

import rvpredict.logging.Protos;

public class ForwardTrace implements Iterator<Protos.Event>, Iterable<Protos.Event> {
   @Override public Protos.Event next() { return null; }
   @Override public boolean hasNext() { return false; }
   @Override public void remove() { (new Exception("remove unsupported")).printStackTrace(); System.exit(1); }
   @Override public Iterator<Protos.Event> iterator() { return this; }
}

// vim: tw=100:sw=2
