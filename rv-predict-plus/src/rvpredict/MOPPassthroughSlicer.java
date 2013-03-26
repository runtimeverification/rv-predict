package rvpredict;

import soot.Scene;

import soot.jimple.Stmt;

import java.util.Set;
import java.util.HashSet;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

import rvpredict.traces.BackwardTrace;
import rvpredict.logging.Protos;

class MOPPassthroughSlicer extends PassthroughSlicer {

  final List<Integer> instance;

  MOPPassthroughSlicer(final BackwardTrace t, final List<Integer> i) {
    super(t);
    instance = i;
  }

  //Mark an event as important.  This is currently set up for race detection
  @Override protected Protos.Event markImportant(final Protos.Event e){
    if (e.getType() == Protos.Event.Type.MOPEvent) {
      //events are immutable, so mergeFrom is necessary to get the original value of e
      return Protos.Event.newBuilder().mergeFrom(e).setIsImportant(true).build();
    } else {
      return e;
    }
  }
}

// vim: tw=100:sw=2
