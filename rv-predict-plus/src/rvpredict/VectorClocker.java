package rvpredict;

import rvpredict.traces.ForwardTrace;
import rvpredict.logging.Protos;
import rvpredict.util.ActuallyCloneable;
import rvpredict.util.CloneableDefaultMap;
import rvpredict.util.DefaultMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import soot.Scene;

abstract class VectorClocker<RetType> implements Iterator<RetType>, Iterable<RetType> {
  @Override public void remove() {};
  @Override public Iterator<RetType> iterator() { return this; }
  private RetType nextValue;

  private final ForwardTrace trace;
  VectorClocker(final ForwardTrace t) {
    trace = t;
  }

  @Override final public RetType next() {
    RetType returnValue = nextValue;
    nextValue = null;
    while ((returnValue == null) && (trace.hasNext())) {
      returnValue = process(trace.next());
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

  // The start of the non boilerplate begins here

  // Class initialization must happen before certain other events, so we need to ensure that.
  // See the Java language Specification Third Edition (Section 12.4)
  private class ClassInitInfo implements ActuallyCloneable<ClassInitInfo> {
    final int ownerThread;
    private VectorClock vc;
    VectorClock getVectorClock() { return vc; }
    void setVectorClock(final VectorClock vc) { assert this.vc == null; this.vc = vc; }
    @Override public ClassInitInfo clone() { return new ClassInitInfo(this); }
    private ClassInitInfo(final ClassInitInfo input){
      this.ownerThread = input.ownerThread;
      this.vc = input.vc.clone();
    }
    ClassInitInfo(final Integer thread) { ownerThread = thread; }
  }

  protected final DefaultMap<Integer,VectorClock> threadClocks = new CloneableDefaultMap<Integer,VectorClock>(new VectorClock()); // Keep track of time for each thread
  protected final Map<String,ClassInitInfo> classInits = new HashMap<String,ClassInitInfo>(); // Keep track of when class initialization finishes
  protected final DefaultMap<Protos.Variable,VectorClock> readClocks = new CloneableDefaultMap<Protos.Variable,VectorClock>(new VectorClock()); // Keep track of the most recent read for each variable
  protected final DefaultMap<Protos.Variable,VectorClock> writeClocks = new CloneableDefaultMap<Protos.Variable,VectorClock>(new VectorClock()); // Keep track of the most recent write for each variable
  protected final DefaultMap<Integer,VectorClock> waitClocks = new CloneableDefaultMap<Integer,VectorClock>(new VectorClock()); // Keep track of the last notify time
  protected final Map<Integer,VectorClock> constructorClocks = new HashMap<Integer,VectorClock>(); // Keep track of when the constructor for an object finished

  protected final void increment(final Integer thread) {
    final VectorClock vc = threadClocks.get(thread).clone();
    threadClocks.put(thread,vc.increment(thread));
  }

  protected final void increment(final Protos.Event e) {
    increment(e.getThreadId());
  }

  protected RetType unimportantEvent(final Protos.Event e) {
    return null;
  }

  protected RetType unimportantAccess(final Protos.Event e) {
    if (e.getIsWrite()) {
      final VectorClock nwvc = writeClocks.get(e.getVariable()).join(threadClocks.get(e.getThreadId())).join(readClocks.get(e.getVariable()));
      final VectorClock nrvc = readClocks.get(e.getVariable()).join(nwvc);
      final VectorClock ntvc = threadClocks.get(e.getThreadId()).join(nwvc);
      writeClocks.put(e.getVariable(),nwvc);
      readClocks.put(e.getVariable(),nrvc);
      threadClocks.put(e.getThreadId(),ntvc);
      return null;
    } else {
      final VectorClock ntvc = threadClocks.get(e.getThreadId()).join(writeClocks.get(e.getVariable()));
      final VectorClock nrvc = readClocks.get(e.getVariable()).join(ntvc).join(writeClocks.get(e.getVariable()));
      threadClocks.put(e.getThreadId(),ntvc);
      readClocks.put(e.getVariable(),nrvc);
      return null;
    }
  }

  abstract protected RetType handleAccess(final Protos.Event e);
  abstract protected RetType handleMOPEvent(final Protos.Event e);
  abstract protected RetType handleLockUnlock(final Protos.Event e);

  private RetType process(final Protos.Event e) {
    increment(e);
    switch (e.getType()) {
      case MethodEnd:
      case Branch:
        return null; // If these really need no action perhaps we should never instrument them, though the pass through slicers will leave them

      case MethodBegin:
      case InstanceInvoke:
      case ConstructorBegin:
      case Invoke:
        // Instance methods cannot be called before <clinit> has finished, so we use the
        // classInitClocks map to enforce this.

        if (Scene.v().getSootClass(e.getClassName()).declaresMethod("void <clinit>()")
            && Scene.v().getSootClass(e.getClassName()).getMethod("void <clinit>()").hasActiveBody()
            && classInits.get(e.getClassName()).ownerThread != e.getThreadId())
          threadClocks.put(e.getThreadId(),
              threadClocks.get(e.getThreadId()).join(classInits.get(e.getClassName()).getVectorClock()));
        return null;

      case ClassInitBegin:
        classInits.put(e.getClassName(),new ClassInitInfo(e.getThreadId()));
        return null;

      case ClassInitEnd:
        classInits.get(e.getClassName()).setVectorClock(threadClocks.get(e.getThreadId()).clone());
        return null;

      case Start:
        // calling Thread.start implies that any event in the new thread must occur after the call

        threadClocks.put(e.getObjectId(),threadClocks.get(e.getThreadId()).clone());
        return null;

      case Join:
        // calling Thread.start implies that any event in the calling thread must occur after the last event of the other thread
        // Since we preserve order the death is probably the most recent event in the target thread.
        // There may be some danger if the thread is repeatedly started and joined.

        threadClocks.put(e.getThreadId(),
            threadClocks.get(e.getThreadId()).join(threadClocks.get(e.getObjectId()).clone()));
        return null;

      case Wait:
        // Wait events actually occur after the wait call finishes (i.e. once the thread has resumed
        // execution). The waitClocks tracks when the last notify(All) occurred for each object.
        // Thus we can assume that this thread was resumed by that notify(All), which is to say that
        // this event must occur after it.

        threadClocks.put(e.getThreadId(),
            threadClocks.get(e.getThreadId()).join(waitClocks.get(e.getObjectId()).clone()));
        return null;
      case Notify:
        waitClocks.put(e.getObjectId(),threadClocks.get(e.getThreadId()).clone());
        return null;

      case Access:
        return handleAccess(e);

      case ConstructorEnd:
        // Finalization must happen after the constructor for its object has completed. To save
        // space we should check that the class constructed actually overrides finalize
        constructorClocks.put(e.getObjectId(),threadClocks.get(e.getThreadId()));
        return null;

      case Finalize:
        // Finalization must happen after the constructor for its object has completed
        //threadClocks.get(e.getThreadId(),
          //  threadClocks.get(e.getThreadId()).join(constructorClocks.get(e.getObjectId())));
        return null;

      case Lock:
      case Unlock:
        return handleLockUnlock(e);
      case MOPEvent:
        return handleMOPEvent(e);

      default:
        System.out.println("Error on event:\n"+e);
        assert false : "Unimplemented";
        return null;
    }
  }
}
// vim: tw=100:sw=2
