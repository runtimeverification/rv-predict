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

class RacePassthroughSlicer extends PassthroughSlicer {

  private final Protos.Variable var;

  RacePassthroughSlicer(final BackwardTrace t, final Protos.Variable a) {
    super(t);
    var = a;
  }

  //Mark an event as important.  This is currently set up for race detection
  @Override protected Protos.Event markImportant(final Protos.Event e){
    if ((e.getType() == Protos.Event.Type.Access) && (var.equals(e.getVariable()))) {
      //events are immutable, so mergeFrom is necessary to get the original value of e
      return Protos.Event.newBuilder().mergeFrom(e).setIsImportant(true).build();
    } else {
      return e;
    }
  }
}

// vim: tw=100:sw=2
