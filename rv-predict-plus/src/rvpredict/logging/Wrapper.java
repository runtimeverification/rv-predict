package rvpredict.logging;

import java.util.Properties;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethodRef;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.Local;

import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.FieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;

import static rvpredict.util.Util.*;

public final class Wrapper {
  private static final Properties purityprops = new Properties();
  static { try { purityprops.load(ClassLoader.getSystemResourceAsStream("rvpredict/Purity.properties")); } catch (Exception e) { e.printStackTrace(); System.exit(1); }}
  private static final SootClass logClass = Scene.v().loadClassAndSupport("rvpredict.logging.RT");
  private static final SootMethodRef logMethodBeginMethod = logClass.getMethod("void logMethodBegin(java.lang.String,java.lang.String)").makeRef();
  private static final SootMethodRef logMethodEndMethod = logClass.getMethod("void logMethodEnd(java.lang.String,java.lang.String)").makeRef();
  private static final SootMethodRef logStaticSyncLockMethod = logClass.getMethod("void logStaticSyncLock(java.lang.String)").makeRef();
  private static final SootMethodRef logLockMethod = logClass.getMethod("void logLock(java.lang.Object)").makeRef();
  private static final SootMethodRef logUnlockMethod = logClass.getMethod("void logUnlock(java.lang.Object)").makeRef();
  private static final SootMethodRef logWaitMethod = logClass.getMethod("void logWait(java.lang.Object,java.lang.String,java.lang.String,int)").makeRef();
  private static final SootMethodRef logNotifyMethod = logClass.getMethod("void logNotify(java.lang.Object,java.lang.String,java.lang.String,int)").makeRef();
  private static final SootMethodRef logStaticSyncUnlockMethod = logClass.getMethod("void logStaticSyncUnlock(java.lang.String)").makeRef();
  private static final SootMethodRef logInvokeBeginMethod = logClass.getMethod("void logInvokeBegin(java.lang.String,java.lang.String,java.lang.String,java.lang.String,int)").makeRef();
  private static final SootMethodRef logInvokeEndMethod = logClass.getMethod("void logInvokeEnd(java.lang.String,java.lang.String,java.lang.String,java.lang.String,int)").makeRef();
  private static final SootMethodRef logExceptionMethod = 
logClass.getMethod("void logException(java.lang.Throwable)").makeRef();
  private static final SootMethodRef logInstanceInvokeBeginMethod = logClass.getMethod("void logInstanceInvokeBegin(java.lang.Object,java.lang.String,java.lang.String,java.lang.String,java.lang.String,int)").makeRef();
  private static final SootMethodRef logInstanceInvokeEndMethod 
 = logClass.getMethod("void logInstanceInvokeEnd(java.lang.Object,java.lang.String,java.lang.String,java.lang.String,java.lang.String,int)").makeRef();
  private static final SootMethodRef logFieldAccMethod = logClass.getMethod("void logFieldAcc(java.lang.String,java.lang.String,java.lang.Object,java.lang.String,java.lang.String,int,boolean)").makeRef();
  private static final SootMethodRef logArrayAccMethod = logClass.getMethod("void logArrayAcc(java.lang.String,java.lang.Object,int,java.lang.String,java.lang.String,int,boolean)").makeRef();
  private static final SootMethodRef logImpureCallMethod = logClass.getMethod("void logImpureCall(java.lang.String,boolean,java.lang.Object,java.lang.String,java.lang.String,int)").makeRef();
  private static final SootMethodRef logConstructorBeginMethod 
= logClass.getMethod("void logConstructorBegin(java.lang.String,java.lang.String,java.lang.String,java.lang.String,int)").makeRef();
  private static final SootMethodRef logConstructorEndMethod
= logClass.getMethod("void logConstructorEnd(java.lang.Object,java.lang.String,java.lang.String,java.lang.String,java.lang.String,int)").makeRef();
  private static final SootMethodRef logClassInitBeginMethod = logClass.getMethod("void logClassInitBegin(java.lang.String)").makeRef();
  private static final SootMethodRef logClassInitEndMethod = logClass.getMethod("void logClassInitEnd(java.lang.String)").makeRef();
  private static final SootMethodRef logStartMethod = logClass.getMethod("void logStart(java.lang.Object)").makeRef();
  private static final SootMethodRef logJoinMethod = logClass.getMethod("void logJoin(java.lang.Object)").makeRef();
  private static final SootMethodRef logFinalizeMethod = logClass.getMethod("void logFinalize(java.lang.Object,java.lang.String,java.lang.String)").makeRef();
  private static final SootMethodRef logBranchMethod = logClass.getMethod("void logBranch(java.lang.String,java.lang.String,int)").makeRef();
  private static final SootMethodRef logBreakLocksMethod = logClass.getMethod("void breakLocks()").makeRef();
  private static final SootMethodRef logReflFieldAccessMethod = logClass.getMethod("void logReflFieldAccess(java.lang.reflect.Field,java.lang.Object,boolean,java.lang.String,java.lang.String,int)").makeRef();
  private static final SootMethodRef logReflNewInstanceBeginMethod = logClass.getMethod("void logReflNewInstanceBegin(java.lang.Class,java.lang.String,java.lang.String,int)").makeRef();
  private static final SootMethodRef logReflNewInstanceEndMethod = logClass.getMethod("void logReflNewInstanceEnd(java.lang.Class,java.lang.Object,java.lang.String,java.lang.String,int)").makeRef();
  private static final SootMethodRef logReflConstructorBeginMethod = logClass.getMethod("void logReflConstructorBegin(java.lang.reflect.Constructor,java.lang.String,java.lang.String,int)").makeRef();
  private static final SootMethodRef logReflConstructorEndMethod = logClass.getMethod("void logReflConstructorEnd(java.lang.reflect.Constructor,java.lang.Object,java.lang.String,java.lang.String,int)").makeRef();
  private static final SootMethodRef logReqClassInitMethod = logClass.getMethod("void logReqClassInitMethod(java.lang.String,java.lang.String,java.lang.String,int)").makeRef();

  public static InvokeStmt logBranch(final Stmt s, final SootClass lclass, final SootMethod lmeth) {
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logBranchMethod,
        StringConstant.v(lclass.getName()),
        StringConstant.v(lmeth.getSubSignature()),
        IntConstant.v(/*getJimpleLine(s)*/-1));
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logFinalize(Value v,SootMethod m) {
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logFinalizeMethod,
        v,
        StringConstant.v(m.getDeclaringClass().getName()),
        StringConstant.v(m.getSubSignature()));
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logMethodBegin(SootMethod m) {
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logMethodBeginMethod,
        StringConstant.v(m.getDeclaringClass().getName()),
        StringConstant.v(m.getSubSignature()));
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logMethodEnd(SootMethod m) {
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logMethodEndMethod,
        StringConstant.v(m.getDeclaringClass().getName()),
        StringConstant.v(m.getSubSignature()));
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logException(Local l){
   InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logExceptionMethod,l);
   return Jimple.v().newInvokeStmt(logExpr);
  }

  
  public static InvokeStmt logConstructorBegin(Stmt s, SootClass c, SootMethod m) {
    InvokeExpr e = s.getInvokeExpr();

    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logConstructorBeginMethod,
        StringConstant.v(e.getMethod().getDeclaringClass().getName()),
        StringConstant.v(e.getMethod().getSubSignature()),
        StringConstant.v(c.getName()),
        StringConstant.v(m.getSubSignature()),
        IntConstant.v(/*getJimpleLine(s)*/-1));
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logConstructorEnd(Stmt s, SootClass c, SootMethod m) {
    InstanceInvokeExpr e = (InstanceInvokeExpr) s.getInvokeExpr();

    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logConstructorEndMethod,
        e.getBase(),
        StringConstant.v(e.getMethod().getDeclaringClass().getName()),
        StringConstant.v(e.getMethod().getSubSignature()),
        StringConstant.v(c.getName()),
        StringConstant.v(m.getSubSignature()),
        IntConstant.v(/*getJimpleLine(s)*/-1));
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logLock(SootClass c) {
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logStaticSyncLockMethod,
        StringConstant.v(c.getName()));
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logLock(Stmt s) {
        return logLock(((EnterMonitorStmt)s).getOp());
  }

  public static InvokeStmt logLock(Value v) {
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logLockMethod,
        v);
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logWait(Stmt s, SootClass c, SootMethod m){
    InstanceInvokeExpr e = (InstanceInvokeExpr)s.getInvokeExpr();

    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logWaitMethod,
        e.getBase(),
        StringConstant.v(c.getName()),
        StringConstant.v(m.getSubSignature()),
        IntConstant.v(/*getJimpleLine(s)*/-1));
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logNotify(Stmt s, SootClass c, SootMethod m){
    InstanceInvokeExpr e = (InstanceInvokeExpr)s.getInvokeExpr();

    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logNotifyMethod,
        e.getBase(),
        StringConstant.v(c.getName()),
        StringConstant.v(m.getSubSignature()),
        IntConstant.v(/*getJimpleLine(s)*/-1));
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logUnlock(SootClass c) {
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logStaticSyncUnlockMethod,
        StringConstant.v(c.getName()));
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logUnlock(Stmt s) {
    return logUnlock(((ExitMonitorStmt)s).getOp());
  }

  public static InvokeStmt logUnlock(Value v) {
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logUnlockMethod,
        v);
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logInvokeBegin(Stmt s, SootClass c, SootMethod m) {
    InvokeExpr e = s.getInvokeExpr();

    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logInvokeBeginMethod,

        StringConstant.v(e.getMethod().getDeclaringClass().getName()),
        StringConstant.v(e.getMethod().getSubSignature()),
        StringConstant.v(c.getName()),
        StringConstant.v(m.getSubSignature()),
        IntConstant.v(/*getJimpleLine(s)*/-1));
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logInvokeEnd(Stmt s, SootClass c, SootMethod m) {
    InvokeExpr e = s.getInvokeExpr();
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logInvokeEndMethod,
        StringConstant.v(e.getMethod().getDeclaringClass().getName()),
        StringConstant.v(e.getMethod().getSubSignature()),
        StringConstant.v(c.getName()),
        StringConstant.v(m.getSubSignature()),
        IntConstant.v(/*getJimpleLine(s)*/-1));
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logInstanceInvokeBegin(Stmt s, SootClass c, SootMethod m) {
    InstanceInvokeExpr e = (InstanceInvokeExpr) s.getInvokeExpr();

    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logInstanceInvokeBeginMethod,
        e.getBase(),
        StringConstant.v(e.getMethod().getDeclaringClass().getName()),
        StringConstant.v(e.getMethod().getSubSignature()),
        StringConstant.v(c.getName()),
        StringConstant.v(m.getSubSignature()),
        IntConstant.v(/*getJimpleLine(s)*/-1));
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logInstanceInvokeEnd(Stmt s, SootClass c, SootMethod m) {
    InstanceInvokeExpr e = (InstanceInvokeExpr) s.getInvokeExpr();

    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logInstanceInvokeEndMethod,
        e.getBase(),
        StringConstant.v(e.getMethod().getDeclaringClass().getName()),
        StringConstant.v(e.getMethod().getSubSignature()),
        StringConstant.v(c.getName()),
        StringConstant.v(m.getSubSignature()),
        IntConstant.v(/*getJimpleLine(s)*/-1));
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logFieldAcc(Stmt s, SootClass c, SootMethod m) {
    SootField f = s.getFieldRef().getField();
    DefinitionStmt d = (DefinitionStmt)s;
    boolean write = (d.getLeftOp() instanceof FieldRef);
    Value instanceObject;
    if (s.getFieldRef() instanceof InstanceFieldRef) {
      instanceObject = ((InstanceFieldRef)s.getFieldRef()).getBase();
    } else {
      instanceObject = NullConstant.v();
    }

    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logFieldAccMethod,
        StringConstant.v(f.getDeclaringClass().getName()),
        StringConstant.v(f.getName()),
        instanceObject,
        StringConstant.v(c.getName()),
        StringConstant.v(m.getSubSignature()),
        IntConstant.v(/*getJimpleLine(s)*/-1),
        IntConstant.v(write?1:0));
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logArrayAcc(Stmt s, SootClass c, SootMethod m) {
    ArrayRef ar = s.getArrayRef();
    DefinitionStmt d = (DefinitionStmt)s;
    boolean write = (d.getLeftOp() instanceof FieldRef);

    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logArrayAccMethod,
        StringConstant.v(ar.getType().toString()),
        ar.getBase(),
        ar.getIndex(),
        StringConstant.v(c.getName()),
        StringConstant.v(m.getSubSignature()),
        IntConstant.v(/*getJimpleLine(s)*/-1),
        IntConstant.v(write?1:0));
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logImpureCall(Stmt s, SootClass c, SootMethod m) {
    InstanceInvokeExpr e = (InstanceInvokeExpr) s.getInvokeExpr();

    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logImpureCallMethod,
        StringConstant.v(e.getBase().getType().toString()),
        IntConstant.v(new Boolean(purityprops.getProperty(e.getMethod().getDeclaringClass().toString()+"."+e.getMethod().getName(),"true"))?1:0),
        e.getBase(),
        StringConstant.v(c.getName()),
        StringConstant.v(m.getSubSignature()),
        IntConstant.v(/*getJimpleLine(s)*/-1));
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logClassInitBegin(SootClass cls) {
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logClassInitBeginMethod,
        StringConstant.v(cls.getName()));
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logClassInitEnd(SootClass cls) {
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logClassInitEndMethod,
        StringConstant.v(cls.getName()));
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logStart(Stmt s) {
    InstanceInvokeExpr e = (InstanceInvokeExpr) s.getInvokeExpr();

    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logStartMethod,
        e.getBase());
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logJoin(Stmt s) {
    InstanceInvokeExpr e = (InstanceInvokeExpr) s.getInvokeExpr();

    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logJoinMethod,
        e.getBase());
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logTimeoutWait() {
    return Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(logBreakLocksMethod));
  }

  public static InvokeStmt logReflFieldAccess(final Stmt s, final boolean isWrite, final SootClass c, final SootMethod m) {
    InstanceInvokeExpr e = (InstanceInvokeExpr) s.getInvokeExpr();
    assert e.getArgCount() > 0;

    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logReflFieldAccessMethod,
        e.getBase(),
        e.getArg(0),
        IntConstant.v(isWrite?1:0),
        StringConstant.v(c.getName()),
        StringConstant.v(m.getSubSignature()),
        IntConstant.v(/*getJimpleLine(s)*/-1));
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logReflNewInstanceBegin(final Stmt s, final AssignStmt astmt, final SootClass c, final SootMethod m) {
    InstanceInvokeExpr e = (InstanceInvokeExpr) astmt.getInvokeExpr();

    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logReflNewInstanceBeginMethod,
        e.getBase(),
        StringConstant.v(c.getName()),
        StringConstant.v(m.getSubSignature()),
        IntConstant.v(/*getJimpleLine(s)*/-1));
    return Jimple.v().newInvokeStmt(logExpr);
  }
  public static InvokeStmt logReflNewInstanceEnd(final Stmt s, final AssignStmt astmt, final SootClass c, final SootMethod m) {
    InstanceInvokeExpr e = (InstanceInvokeExpr) astmt.getInvokeExpr();
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logReflNewInstanceEndMethod,
        e.getBase(),
        astmt.getLeftOp(),
        StringConstant.v(c.getName()),
        StringConstant.v(m.getSubSignature()),
        IntConstant.v(/*getJimpleLine(s)*/-1));
    return Jimple.v().newInvokeStmt(logExpr);
  }
  public static InvokeStmt logReflConstructorBegin(final Stmt s, final AssignStmt astmt, final SootClass c, final SootMethod m) {
    InstanceInvokeExpr e = (InstanceInvokeExpr) astmt.getInvokeExpr();

    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logReflConstructorBeginMethod,
        e.getBase(),
        StringConstant.v(c.getName()),
        StringConstant.v(m.getSubSignature()),
        IntConstant.v(/*getJimpleLine(s)*/-1));
    return Jimple.v().newInvokeStmt(logExpr);
  }
  public static InvokeStmt logReflConstructorEnd(final Stmt s, final AssignStmt astmt, final SootClass c, final SootMethod m) {
    InstanceInvokeExpr e = (InstanceInvokeExpr) astmt.getInvokeExpr();
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logReflConstructorEndMethod,
        e.getBase(),
        astmt.getLeftOp(),
        StringConstant.v(c.getName()),
        StringConstant.v(m.getSubSignature()),
        IntConstant.v(/*getJimpleLine(s)*/-1));
    return Jimple.v().newInvokeStmt(logExpr);
  }
  public static InvokeStmt logReqClassInit(final Stmt s, final SootClass c, final SootMethod m) {
    final InvokeExpr e =  s.getInvokeExpr();
    assert e.getArgCount() > 0;
    final InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logReqClassInitMethod,
        e.getArg(0),
        StringConstant.v(c.getName()),
        StringConstant.v(m.getSubSignature()),
        IntConstant.v(/*getJimpleLine(s)*/-1));
    return Jimple.v().newInvokeStmt(logExpr);
  }
}
// vim: tw=100:sw=2
