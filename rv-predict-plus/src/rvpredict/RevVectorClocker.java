package rvpredict;

import rvpredict.traces.BackwardTrace;
import rvpredict.logging.Protos;
import rvpredict.util.ActuallyCloneable;
import rvpredict.util.CloneableDefaultMap;
import rvpredict.util.DefaultMap;
import rvpredict.util.DiskHash;
import rvpredict.util.DefaultDiskHash;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;

import soot.Scene;

abstract class RevVectorClocker<R> implements Iterator<R>, Iterable<R> {
  @Override public void remove() {};
  @Override public Iterator<R> iterator() { return this; }
  private R nextValue;

  private final Iterator<Protos.Event> trace;
  RevVectorClocker(final Iterator<Protos.Event> t) {
    trace = t;
  }

  @Override final public R next() {
    R returnValue = nextValue;
    nextValue = null;
    while ((returnValue == null) && (trace.hasNext())) {
      final Protos.Event e = trace.next();
      returnValue = process(e);
      //System.out.println(threadClocks.get(e.getThreadId()));
      //System.out.println(e);
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
  protected final DefaultMap<String,VectorClock> classInits = new CloneableDefaultMap<String,VectorClock>(new VectorClock()); // Keep track of when class initialization finishes
  protected final DefaultDiskHash<SerializableVariable,VectorClock> readClocks = new DefaultDiskHash<SerializableVariable,VectorClock>(new VectorClock()); // Keep track of the most recent read for each variable
  protected final DefaultDiskHash<SerializableVariable,VectorClock> writeClocks = new DefaultDiskHash<SerializableVariable,VectorClock>(new VectorClock()); // Keep track of the most recent write for each variable
  protected final DefaultMap<Integer,VectorClock> waitClocks = new CloneableDefaultMap<Integer,VectorClock>(new VectorClock()); // Keep track of the last notify time
  protected final DefaultMap<Integer,VectorClock> constructorClocks = new CloneableDefaultMap<Integer,VectorClock>(new VectorClock()); // Keep track of when the constructor for an object finished

  protected final void decrement(final Integer thread) {
    final VectorClock vc = threadClocks.get(thread).clone();
    threadClocks.put(thread,vc.decrement(thread));
  }

  protected final void decrement(final Protos.Event e) {
    decrement(e.getThreadId());
  }

  protected R unimportantEvent(final Protos.Event e) {
    return null;
  }

  protected R unimportantAccess(final Protos.Event e) {
    if (e.getIsWrite()) {
      final VectorClock nwvc = writeClocks.get(new SerializableVariable(e.getVariable())).meet(threadClocks.get(e.getThreadId())).meet(readClocks.get(new SerializableVariable(e.getVariable())));
      final VectorClock nrvc = readClocks.get(new SerializableVariable(e.getVariable())).meet(nwvc);
      final VectorClock ntvc = threadClocks.get(e.getThreadId()).meet(nwvc);
      writeClocks.put(new SerializableVariable(e.getVariable()),nwvc);
      readClocks.put(new SerializableVariable(e.getVariable()),nrvc);
      threadClocks.put(e.getThreadId(),ntvc);
      return null;
    } else {
      final VectorClock ntvc = threadClocks.get(e.getThreadId()).meet(writeClocks.get(new SerializableVariable(e.getVariable())));
      final VectorClock nrvc = readClocks.get(new SerializableVariable(e.getVariable())).meet(ntvc).meet(writeClocks.get(new SerializableVariable(e.getVariable())));
      threadClocks.put(e.getThreadId(),ntvc);
      readClocks.put(new SerializableVariable(e.getVariable()),nrvc);
      return null;
    }
  }

  protected R handleAccess(final Protos.Event e) { return unimportantAccess(e); }
  protected R handleMOPEvent(final Protos.Event e) { return unimportantEvent(e); }
  protected R handleLockUnlock(final Protos.Event e) { return unimportantEvent(e); }

  private R process(final Protos.Event e) {
    decrement(e);
    switch (e.getType()) {
      case MethodEnd:
      case Branch:
      case ClassInitBegin:
        return null; // If these really need no action perhaps we should never instrument them, though the pass through slicers will leave them

      case MethodBegin:
      case InstanceInvoke:
      case ConstructorBegin:
      case Invoke:
      case ReqClassInit:
        // Instance methods cannot be called before <clinit> has finished, so we use the
        // classInitClocks map to enforce this.

        if (Scene.v().getSootClass(e.getClassName()).declaresMethod("void <clinit>()")
            && Scene.v().getSootClass(e.getClassName()).getMethod("void <clinit>()").hasActiveBody())
          classInits.put(e.getClassName(),
              classInits.get(e.getClassName()).meet(threadClocks.get(e.getThreadId())));
        return null;

      case ClassInitEnd:
        threadClocks.put(e.getThreadId(),
            threadClocks.get(e.getThreadId()).meet(classInits.get(e.getClassName())));
        return null;

      case Start:
        // calling Thread.start implies that any event in the new thread must occur after the call

        threadClocks.put(e.getThreadId(),
            threadClocks.get(e.getThreadId()).meet(threadClocks.get(e.getObjectId())));
        return null;

      case Join:
        // calling Thread.start implies that any event in the calling thread must occur after the last event of the other thread
        // Since we preserve order the death is probably the most recent event in the target thread.
        // There may be some danger if the thread is repeatedly started and joined.

        threadClocks.put(e.getObjectId(),threadClocks.get(e.getThreadId()));
        return null;

      case Wait:
        // Wait events actually occur after the wait call finishes (i.e. once the thread has resumed
        // execution). The waitClocks tracks when the last notify(All) occurred for each object.
        // Thus we can assume that this thread was resumed by that notify(All), which is to say that
        // this event must occur after it.

        waitClocks.put(e.getObjectId(),
            waitClocks.get(e.getObjectId()).meet(threadClocks.get(e.getThreadId())));
        return null;
      case Notify:
        threadClocks.put(e.getThreadId(),
            threadClocks.get(e.getThreadId()).meet(waitClocks.get(e.getObjectId())));
        return null;

      case Access:
        return handleAccess(e);

      case ConstructorEnd:
        // Finalization must happen after the constructor for its object has completed. To save
        // space we should check that the class constructed actually overrides finalize
        threadClocks.put(e.getThreadId(),
            threadClocks.get(e.getThreadId()).meet(constructorClocks.get(e.getObjectId())));
        return null;

      case Finalize:
        // Finalization must happen after the constructor for its object has completed
        constructorClocks.put(e.getObjectId(),
            constructorClocks.get(e.getObjectId()).meet(threadClocks.get(e.getThreadId())));
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
