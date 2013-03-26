package rvpredict;

import soot.Scene;

import soot.jimple.Stmt;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import java.util.Map;
import java.util.HashMap;

import java.util.Collection;
import java.util.ArrayList;

import rvpredict.traces.BackwardTrace;
import rvpredict.logging.Protos;

abstract class Slicer extends BackwardTrace {
  @Override public void remove() {};
  //this is a stored copy of the current event that WILL be added to the trace slice
  private Protos.Event nextValue; 

  private final Iterator<Protos.Event> trace;

  Slicer(final Iterator<Protos.Event> t) {
    trace = t;
  }

  protected Protos.Event e;
  @Override final public Protos.Event next() {
    Protos.Event returnValue = nextValue;
    nextValue = null;
    while ((returnValue == null) && (trace.hasNext())) {
      //set the value of e, e will be used by process
      //we need e to be a field rather than a parameter
      //because of side-effecty stuff
      e = trace.next();
      //System.out.println(e);
      returnValue = process();
    }
    return returnValue;
  }

  @Override final public boolean hasNext() {
    if (nextValue != null)
      return true;
    else {
      nextValue = next();
      return nextValue != null;
    }
  }

  abstract protected Protos.Event process();
}

// vim: tw=100:sw=2
