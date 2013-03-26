package rvpredict;

import soot.Scene;

import soot.jimple.Stmt;

import java.util.Set;
import java.util.HashSet;

import java.util.Map;
import java.util.HashMap;

import java.util.Collection;
import java.util.ArrayList;

import rvpredict.traces.BackwardTrace;
import rvpredict.logging.Protos;

abstract class PassthroughSlicer extends BackwardTrace {
  @Override public void remove() {};
  //this is a stored copy of the current event that WILL be added to the trace slice
  private Protos.Event nextValue; 

  private final BackwardTrace trace;

  PassthroughSlicer(final BackwardTrace t) {
    trace = t;
  }

  @Override public Protos.Event next() {
    Protos.Event returnValue = nextValue;
    nextValue = null;
    while ((returnValue == null) && (trace.hasNext())) {
      returnValue = process(trace.next());
    }
    return returnValue;
  }

  @Override public boolean hasNext() {
    if (nextValue != null)
      return true;
    else {
      nextValue = next();
      return nextValue != null;
    }
  }

  private Protos.Event process(final Protos.Event e) {
    return markImportant(e);
  }

  //Mark an event as important.  This is currently set up for race detection
  protected abstract Protos.Event markImportant(final Protos.Event e);
}

// vim: tw=100:sw=2
