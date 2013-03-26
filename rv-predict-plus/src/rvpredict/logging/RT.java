package rvpredict.logging;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.Collections;
import java.util.Collection;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

import java.util.zip.GZIPOutputStream;

import java.lang.Exception;
import java.lang.ThreadLocal;
import java.lang.StringBuilder;
import java.lang.Thread;
import java.util.Arrays;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import rvpredict.MacerCore;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class RT extends Thread {
  //block size in events
  private static int blockSize = 100000;
  private static MacerCore macer = new MacerCore();

  private static final ThreadLocal<Set<Object>> lockset =
    new ThreadLocal<Set<Object>>() {
      @Override protected Set<Object> initialValue() {
        return Collections.newSetFromMap(new IdentityHashMap<Object,Boolean>());
      }
    };

  private static final CompressedReversedBlockOutput crbos = new CompressedReversedBlockOutput(new StringBuffer("trace_").append(System.identityHashCode(Thread.currentThread())).toString(), blockSize);
  static { try { new FileOutputStream("sharedvars").close(); } catch (Exception e) { e.printStackTrace(); System.exit(1); } }
  private static FileOutputStream sharedvarfile; 
  static { try { sharedvarfile = new FileOutputStream("sharedvars"); } catch (Exception e) { e.printStackTrace(); System.exit(1); } }
  private static synchronized void addVar(final Protos.Variable v) {
    try {
      v.writeDelimitedTo(sharedvarfile);
    } catch (Exception e) { e.printStackTrace(); System.exit(1); }
  }

  private static void checkLocks() {
    for (Object lock : lockset.get()) {
      if ((!Thread.currentThread().holdsLock(lock)) && (lockset.get().remove(lock))) {
        logRealUnlock(lock);
      }
    }
  }

  private static  Collection<Integer> getLocks() {
    Collection<Integer> ret = new ArrayList<Integer>(lockset.get().size());
    for (Object lock : lockset.get()) {
      if ((!Thread.currentThread().holdsLock(lock)) && (lockset.get().remove(lock))) {
        logRealUnlock(lock);
      } else {
        ret.add(System.identityHashCode(lock));
      }
    }
    return ret;
  }

  // logUnlock logs every exitmonitor so this logs only the needed unlocks
  private static  void logRealUnlock(Object lock) {
//    if(true) return;
    try{
      crbos.addEvent(Protos.Event.newBuilder()
          .setType(Protos.Event.Type.Unlock)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setObjectId(System.identityHashCode(lock))
          .build());
    } catch(IOException e){ e.printStackTrace(); System.exit(1); }
  }

  // logLock logs every entermonitor so this logs only the needed locks
  private static  void logRealLock(Object lock) {
//    if(true) return;
    try{
      crbos.addEvent(Protos.Event.newBuilder()
          .setType(Protos.Event.Type.Lock)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setObjectId(System.identityHashCode(lock))
          .build());
    } catch(IOException e){ e.printStackTrace(); System.exit(1); }
  }

  // First release all locks then require them. Mostly needed for implementing wait()
  private static  void breakLocks() {
//    if(true) return;
    checkLocks();
    for (Object lock : lockset.get())
      logRealUnlock(lock);
    for (Object lock : lockset.get())
      logRealLock(lock);
  }

  public static  void logBranch(final String lclass, final String lmeth, final int jline) {
//    if(true) return;
    checkLocks();
    try {
      crbos.addEvent(Protos.Event.newBuilder()
          .setType(Protos.Event.Type.Branch)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setLocation(Protos.Location.newBuilder().setClassName(lclass).setMethodName(lmeth).setJimpleLine(jline).build())
          .build());
    } catch(IOException e){ e.printStackTrace(); System.exit(1); }
  }

  public static  void logFinalize(final Object o, final String className, final String methodName) {
//    if(true) return;
    // Try to only log finalize methods whenever it actually is called by the GC.
    final StackTraceElement[] stack = Thread.currentThread().getStackTrace();
    if ((stack.length == 7)
        && (stack[stack.length-1].getClassName().startsWith("java.lang.ref.Finalizer"))
        && (stack[stack.length-1].getMethodName().equals("run"))
        && (stack[stack.length-2].getClassName().startsWith("java.lang.ref.Finalizer"))
        && (stack[stack.length-2].getMethodName().startsWith("access"))
        && (stack[stack.length-3].getClassName().startsWith("java.lang.ref.Finalizer"))
        && (stack[stack.length-3].getMethodName().startsWith("runFinalizer"))
        && (stack[stack.length-4].getClassName().startsWith("java.lang.ref.Finalizer"))
        && (stack[stack.length-4].getMethodName().equals("invokeFinalizeMethod"))
        && (stack[stack.length-4].isNativeMethod())
        && (stack[stack.length-5].getMethodName().equals("finalize"))) {
      try {
        checkLocks();
        crbos.addEvent(Protos.Event.newBuilder()
            .setType(Protos.Event.Type.Finalize)
            .setThreadId(System.identityHashCode(Thread.currentThread()))
            .setObjectId(System.identityHashCode(o))
            .build());
      } catch(IOException e){ e.printStackTrace(); System.exit(1); }
    }
  }

  // Since we log all method entries (excluding safe ones from the standard library) we only need to keep track of
  // which line this call was on to reconstruct the call stack with line numbers
  public static  void logInstanceInvokeBegin(final Object o, final String className, final String methodName, final String lclass, final String lmeth, final int jline) {
//    if(true) return;
    checkLocks();
    try{
      Class.forName(className);
      crbos.addEvent(Protos.Event.newBuilder()
          .setType(Protos.Event.Type.InstanceInvoke)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setObjectId(System.identityHashCode(o))
          .setClassName(className)
          .setMethodName(methodName)
          .setLocation(Protos.Location.newBuilder().setClassName(lclass).setMethodName(lmeth).setJimpleLine(jline).build())
          .build());
    } catch(Exception e){ e.printStackTrace(); System.exit(1); }
  }
  public static  void logInstanceInvokeEnd(final Object o, final String className, final String methodName, final String lclass, final String lmeth, final int jline) {
//    if(true) return;
    checkLocks();
    try{
      crbos.addEvent(Protos.Event.newBuilder()
          .setType(Protos.Event.Type.InstanceInvoke)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setObjectId(System.identityHashCode(o))
          .setClassName(className)
          .setMethodName(methodName)
          .setLocation(Protos.Location.newBuilder().setClassName(lclass).setMethodName(lmeth).setJimpleLine(jline).build())
          .build());
    } catch(IOException e){ e.printStackTrace(); System.exit(1); }
  }

  public static  void logInvokeBegin(final String className, final String methodName, final String lclass, final String lmeth, final int jline) {
//    if(true) return;
    checkLocks();
    try{
      Class.forName(className);
      crbos.addEvent(Protos.Event.newBuilder()
          .setType(Protos.Event.Type.Invoke)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setClassName(className)
          .setMethodName(methodName)
          .setLocation(Protos.Location.newBuilder().setClassName(lclass).setMethodName(lmeth).setJimpleLine(jline).build())
          .build());
    } catch(Exception e){ e.printStackTrace(); System.exit(1); }
  }

  public static  void logConstructorBegin(final String className, final String methodName, final String lclass, final String lmeth, final int jline) {
//    if(true) return;
    checkLocks();
    try{
      Class.forName(className);
      crbos.addEvent(Protos.Event.newBuilder()
          .setType(Protos.Event.Type.ConstructorBegin)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setClassName(className)
          .setMethodName(methodName)
          .setLocation(Protos.Location.newBuilder().setClassName(lclass).setMethodName(lmeth).setJimpleLine(jline).build())
          .build());
    } catch(Exception e){ e.printStackTrace(); System.exit(1); }
  }
  public static  void logConstructorEnd(final Object o, final String className, final String methodName, final String lclass, final String lmeth, final int jline) {
//    if(true) return;
    checkLocks();
    try{
      crbos.addEvent(Protos.Event.newBuilder()
          .setType(Protos.Event.Type.ConstructorEnd)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setObjectId(System.identityHashCode(o))
          .setClassName(className)
          .setMethodName(methodName)
          .setLocation(Protos.Location.newBuilder().setClassName(lclass).setMethodName(lmeth).setJimpleLine(jline).build())
          .build());
    } catch(IOException e){ e.printStackTrace(); System.exit(1); }
  }

  public static  void logWait(final Object o, final String lclass, final String lmeth, final int jline) {
//    if(true) return;
    breakLocks();
    try{
      crbos.addEvent(Protos.Event.newBuilder()
          .setType(Protos.Event.Type.Wait)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setObjectId(System.identityHashCode(o))
          .setLocation(Protos.Location.newBuilder().setClassName(lclass).setMethodName(lmeth).setJimpleLine(jline).build())
          .build());
    } catch(IOException e){ e.printStackTrace(); System.exit(1); }
  }

  public static  void logNotify(final Object o, final String lclass, final String lmeth, final int jline) {
//    if(true) return;
    checkLocks();
    try{
      crbos.addEvent(Protos.Event.newBuilder()
          .setType(Protos.Event.Type.Notify)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setObjectId(System.identityHashCode(o))
          .setLocation(Protos.Location.newBuilder().setClassName(lclass).setMethodName(lmeth).setJimpleLine(jline).build())
          .build());
    } catch(IOException e){ e.printStackTrace(); System.exit(1); }
  }

  public static  void logInvokeEnd(final String className, final String methodName, final String lclass, final String lmeth, final int jline) {
//    if(true) return;
    checkLocks();
    try{
      crbos.addEvent(Protos.Event.newBuilder()
          .setType(Protos.Event.Type.Invoke)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setClassName(className)
          .setMethodName(methodName)
          .setLocation(Protos.Location.newBuilder().setClassName(lclass).setMethodName(lmeth).setJimpleLine(jline).build())
          .build());
    } catch(IOException e){ e.printStackTrace(); System.exit(1); }
  }

  public synchronized static  void logMethodBegin(final String className, final String methodName) {
    //if(true) return;
    checkLocks();
    try{
      crbos.addEvent(Protos.Event.newBuilder()
          .setType(Protos.Event.Type.MethodBegin)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setClassName(className)
          .setMethodName(methodName)
          .build());
    } catch(IOException e){ e.printStackTrace(); System.exit(1); }
  }

  public synchronized static  void logMethodEnd(final String className, final String methodName) {
    //if(true) return;
    checkLocks();
    try{
      crbos.addEvent(Protos.Event.newBuilder()
          .setType(Protos.Event.Type.MethodEnd)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setClassName(className)
          .setMethodName(methodName)
          .build());
      if (methodName.contains("finalize"))
        crbos.flush();
      // crbos.flush();
    } catch(IOException e){ e.printStackTrace(); System.exit(1); }
  }

  public static  void logException(Throwable e){
//    if(true) return;
    checkLocks();
    List<String> classNames = new ArrayList<String>();
    for(StackTraceElement el : e.getStackTrace()){
      classNames.add(el.getClassName());
    }
    try{
      crbos.addEvent(Protos.Event.newBuilder()
                     .setType(Protos.Event.Type.Exception)
                     .setThreadId(System.identityHashCode(Thread.currentThread()))
                     .addAllClasstack(classNames)
                     .build());
    } catch(IOException ioe) { ioe.printStackTrace(); System.exit(1); }
  }

  public static  void logMOPEvent(final String eventName, final Object... os) {
//    if(true) return;
    checkLocks();
    Protos.Event.Builder e = Protos.Event.newBuilder()
      .setType(Protos.Event.Type.MOPEvent)
      .setThreadId(System.identityHashCode(Thread.currentThread()))
      .setMopname(eventName);
    for (Object o : os)
      e.addMopids(System.identityHashCode(o));
    try{
      crbos.addEvent(e.build());
    } catch(IOException ioe){ ioe.printStackTrace(); System.exit(1); }
  }

  public static  void logStaticSyncLock(final String s) {
//    if(true) return;
    checkLocks();
    try{
      logLock(Class.forName(s));
    } catch(Exception e){ e.printStackTrace(); System.exit(1); }
  }

  public static  void logStaticSyncUnlock(final String s) {
//    if(true) return;
    checkLocks();
    try{
      logUnlock(Class.forName(s));
    } catch(Exception e){ e.printStackTrace(); System.exit(1); }
  }

  public static  void logLock(final Object lock) {
//    if(true) return;
    checkLocks();
    if (lockset.get().add(lock)) {
      logRealLock(lock);
    }
  }

  public static  void logUnlock(final Object lock) {
//    if(true) return;
    checkLocks();
  }

  public static  void logFieldAcc(final String className, final String fieldName, final Object o, final String lclass, final String lmeth, final int jline, final boolean write) {
//    if(true) return;
    checkLocks();
    try{
      final Protos.Variable v;
      if (o == null)
        v = Protos.Variable.newBuilder()
          .setType(Protos.Variable.Type.StaticFieldAcc)
          .setClassName(className)
          .setFieldName(fieldName)
          .build();
      else
        v = Protos.Variable.newBuilder()
          .setType(Protos.Variable.Type.FieldAcc)
          .setClassName(className)
          .setFieldName(fieldName)
          .setObjectID(System.identityHashCode(o))
          .build();
      Protos.Event e = Protos.Event.newBuilder()
        .setType(Protos.Event.Type.Access)
        .setThreadId(System.identityHashCode(Thread.currentThread()))
        .setLocation(Protos.Location.newBuilder().setClassName(lclass).setMethodName(lmeth).setJimpleLine(jline).build())
        .setVariable(v)
        .addAllLockset(getLocks())
        .setIsWrite(write)
        .build();
      crbos.addEvent(e);
      if (macer.process(e))
        addVar(v);
    } catch(IOException e){ e.printStackTrace(); System.exit(1); }
  }

  public static  void logArrayAcc(final String type, final Object base, final int index, final String lclass, final String lmeth, final int jline, final boolean write) {
//    if(true) return;
    try{
      final Protos.Variable v = Protos.Variable.newBuilder()
        .setType(Protos.Variable.Type.ArrayAcc)
        .setClassName(type)
        .setObjectID(System.identityHashCode(base))
        .setIndex(index)
        .build();
      final Protos.Event e = Protos.Event.newBuilder()
          .setType(Protos.Event.Type.Access)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setLocation(Protos.Location.newBuilder().setClassName(lclass).setMethodName(lmeth).setJimpleLine(jline).build())
          .setVariable(v)
          .addAllLockset(getLocks())
          .setIsWrite(write)
          .build();
      crbos.addEvent(e);
      if (macer.process(e))
        addVar(v);
    } catch(IOException e){ e.printStackTrace(); System.exit(1); }
  }

  public static  void logImpureCall(final String cls, final boolean isWrite, final Object o, final String lclass, final String lmeth, final int jline) {
//    if(true) return;
    try{
      final Protos.Variable v = Protos.Variable.newBuilder()
        .setType(Protos.Variable.Type.ImpureCall)
        .setClassName(cls)
        .setObjectID(System.identityHashCode(o))
        .build();
      final Protos.Event e = Protos.Event.newBuilder()
          .setType(Protos.Event.Type.Access)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setLocation(Protos.Location.newBuilder().setClassName(lclass).setMethodName(lmeth).setJimpleLine(jline).build())
          .setVariable(v)
          .addAllLockset(getLocks())
          .setIsWrite(isWrite)
          .build();
      crbos.addEvent(e);
      if (macer.process(e))
        addVar(v);
    } catch(IOException e){ e.printStackTrace(); System.exit(1); }
  }

  public static  void logClassInitBegin(final String className) {
    checkLocks();
//    if(true) return;
    try{
      crbos.addEvent(Protos.Event.newBuilder()
          .setType(Protos.Event.Type.ClassInitBegin)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setClassName(className)
          .build());
    } catch(IOException e){ e.printStackTrace(); System.exit(1); }
  }

  public static  void logClassInitEnd(final String className) {
    checkLocks();
//    if(true) return;
    try{
      crbos.addEvent(Protos.Event.newBuilder()
          .setType(Protos.Event.Type.ClassInitEnd)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setClassName(className)
          .build());
    } catch(IOException e){ e.printStackTrace(); System.exit(1); }
  }

  public static  void logStart(final Object o) {
//    if(true) return;
    checkLocks();
    try{
      crbos.addEvent(Protos.Event.newBuilder()
          .setType(Protos.Event.Type.Start)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setObjectId(System.identityHashCode(o))
          .build());
    } catch(IOException e){ e.printStackTrace(); System.exit(1); }
  }
  public static  void logJoin(final Object o) {
//    if(true) return;
    checkLocks();
    try{
      crbos.addEvent(Protos.Event.newBuilder()
          .setType(Protos.Event.Type.Join)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setObjectId(System.identityHashCode(o))
          .build());
    } catch(IOException e){ e.printStackTrace(); System.exit(1); }
  }

  public static  void logReflFieldAccess(final Field f, final Object o, final boolean isWrite, final String lclass, final String lmeth, final int jline) {
//    if(true) return;
    checkLocks();
    try {
      final Protos.Variable v;
      if (Modifier.isStatic(f.getModifiers()))
        v = Protos.Variable.newBuilder()
          .setType(Protos.Variable.Type.StaticFieldAcc)
          .setClassName(f.getDeclaringClass().getName())
          .setFieldName(f.getName())
          .build();
      else
        v = Protos.Variable.newBuilder()
          .setType(Protos.Variable.Type.FieldAcc)
          .setClassName(f.getDeclaringClass().getName())
          .setFieldName(f.getName())
          .setObjectID(System.identityHashCode(o))
          .build();
      Protos.Event e = Protos.Event.newBuilder()
          .setType(Protos.Event.Type.Access)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setLocation(Protos.Location.newBuilder().setClassName(lclass).setMethodName(lmeth).setJimpleLine(jline).build())
          .setVariable(v)
          .addAllLockset(getLocks())
          .setIsWrite(isWrite)
          .build();
      crbos.addEvent(e);
      if (macer.process(e))
        addVar(v);
    } catch (Exception e) { e.printStackTrace(); System.exit(1); }
  }
  public static  void logReflNewInstanceBegin(final Class c, final String lclass, final String lmeth, final int jline) {
//    if(true) return;
    checkLocks();
    try{
      crbos.addEvent(Protos.Event.newBuilder()
          .setType(Protos.Event.Type.ConstructorBegin)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setClassName(c.getName())
          .setMethodName("void <init>()")
          .setLocation(Protos.Location.newBuilder().setClassName(lclass).setMethodName(lmeth).setJimpleLine(jline).build())
          .build());
    } catch(Exception e){ e.printStackTrace(); System.exit(1); }
  }

  public static  void logReflNewInstanceEnd(final Class c, final Object o, final String lclass, final String lmeth, final int jline) {
//    if(true) return;
    checkLocks();
    try{
      crbos.addEvent(Protos.Event.newBuilder()
          .setType(Protos.Event.Type.ConstructorEnd)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setObjectId(System.identityHashCode(o))
          .setClassName(c.getName())
          .setMethodName("void <init>()")
          .setLocation(Protos.Location.newBuilder().setClassName(lclass).setMethodName(lmeth).setJimpleLine(jline).build())
          .build());
    } catch(IOException e){ e.printStackTrace(); System.exit(1); }
  }

  public static  void logReflConstructorBegin(final Constructor c, final String lclass, final String lmeth, final int jline) {
//    if(true) return;
    checkLocks();
    try{
      crbos.addEvent(Protos.Event.newBuilder()
          .setType(Protos.Event.Type.ConstructorBegin)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setClassName(c.getDeclaringClass().getName())
          .setMethodName("void <init>()")
          .setLocation(Protos.Location.newBuilder().setClassName(lclass).setMethodName(lmeth).setJimpleLine(jline).build())
          .build());
    } catch(Exception e){ e.printStackTrace(); System.exit(1); }
  }
  public static  void logReflConstructorEnd(final Constructor c, final Object o, final String lclass, final String lmeth, final int jline) {
//    if(true) return;
    checkLocks();
    try{
      crbos.addEvent(Protos.Event.newBuilder()
          .setType(Protos.Event.Type.ConstructorEnd)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setObjectId(System.identityHashCode(o))
          .setClassName(c.getDeclaringClass().getName())
          .setMethodName("void <init>()")
          .setLocation(Protos.Location.newBuilder().setClassName(lclass).setMethodName(lmeth).setJimpleLine(jline).build())
          .build());
    } catch(IOException e){ e.printStackTrace(); System.exit(1); }
  }
  public static  void logReqClassInitMethod(final String className, final String lclass, final String lmeth, final int jline) {
//    if(true) return;
    checkLocks();
    try{
      crbos.addEvent(Protos.Event.newBuilder()
          .setType(Protos.Event.Type.ReqClassInit)
          .setThreadId(System.identityHashCode(Thread.currentThread()))
          .setClassName(className)
          .setLocation(Protos.Location.newBuilder().setClassName(lclass).setMethodName(lmeth).setJimpleLine(jline).build())
          .build());
    } catch(IOException e){ e.printStackTrace(); System.exit(1); }
  }
}
// vim: tw=100:sw=2
