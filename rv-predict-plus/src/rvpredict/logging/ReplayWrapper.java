package rvpredict.logging;

import java.util.HashMap;
import java.util.Properties;

import soot.Body;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.LongType;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
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

public final class ReplayWrapper {
	
  private static final SootClass logClass = Scene.v().loadClassAndSupport("rvpredict.logging.ReplayRT");
  private static final SootMethodRef logLockMethod = logClass.getMethod("void logLock()").makeRef();
  private static final SootMethodRef logUnlockMethod = logClass.getMethod("void logUnlock()").makeRef();
  private static final SootMethodRef logWaitMethod = logClass.getMethod("void logWait()").makeRef();
  private static final SootMethodRef logNotifyMethod = logClass.getMethod("void logNotify()").makeRef();
  private static final SootMethodRef logFieldAccMethod = logClass.getMethod("void logFieldAcc()").makeRef();
  private static final SootMethodRef logArrayAccMethod = logClass.getMethod("void logArrayAcc()").makeRef();
  private static final SootMethodRef logStartMethod = logClass.getMethod("void logStart(java.lang.Object)").makeRef();
  private static final SootMethodRef logJoinMethod = logClass.getMethod("void logJoin()").makeRef();
  private static final SootMethodRef logStaticSyncLockMethod = logClass.getMethod("void logStaticSyncLock()").makeRef();
  private static final SootMethodRef logStaticSyncUnlockMethod = logClass.getMethod("void logStaticSyncUnlock()").makeRef();

  public static InvokeStmt logStaticLock() {
	    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logStaticSyncLockMethod
);
	    return Jimple.v().newInvokeStmt(logExpr);
  }	
  public static InvokeStmt logLock() {
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logLockMethod);
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logWait(){

	    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logWaitMethod);
	    return Jimple.v().newInvokeStmt(logExpr);
	  }

  public static InvokeStmt logNotify(){

    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logNotifyMethod);
    return Jimple.v().newInvokeStmt(logExpr);
  }

//  public static InvokeStmt logUnlock(SootClass c) {
//    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logStaticSyncUnlockMethod,
//        StringConstant.v(c.getName()));
//    return Jimple.v().newInvokeStmt(logExpr);
//  }


  public static InvokeStmt logStaticUnlock() {
	  InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logStaticSyncUnlockMethod
	    		);
	    return Jimple.v().newInvokeStmt(logExpr);
	  }
  public static InvokeStmt logUnlock() {
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logUnlockMethod);
    return Jimple.v().newInvokeStmt(logExpr);
  }

  
  public static InvokeStmt logArrayAcc()
  {
	 
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logArrayAccMethod);
    
    InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(logExpr);    
    return invokeStmt;
  }
  public static InvokeStmt logFieldAcc() {
    
    //may add sid later to parallelize replay
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logFieldAccMethod
    		//IntConstant.v(sid),
    		);
    
    InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(logExpr);
    
    return invokeStmt;
    
  }

  public static InvokeStmt logStart(Stmt s) {
	    InstanceInvokeExpr e = (InstanceInvokeExpr) s.getInvokeExpr();

    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logStartMethod,
    		e.getBase());
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logJoin() {

    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logJoinMethod);
    return Jimple.v().newInvokeStmt(logExpr);
  }

}
// vim: tw=100:sw=2
