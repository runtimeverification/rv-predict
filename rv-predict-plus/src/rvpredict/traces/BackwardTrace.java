package rvpredict.traces;

import java.lang.Iterable;

import java.util.Iterator;

import rvpredict.logging.Protos;

public abstract class BackwardTrace implements Iterator<Protos.Event>, Iterable<Protos.Event> {
   @Override public abstract Protos.Event next();
   @Override public abstract boolean hasNext();
   @Override public abstract void remove();
   @Override public Iterator<Protos.Event> iterator() { return this; }
}

// vim: tw=100:sw=2
