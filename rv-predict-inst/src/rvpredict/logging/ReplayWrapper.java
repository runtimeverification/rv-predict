/*******************************************************************************
 * Copyright (c) 2013 University of Illinois
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
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

/**
 * The ReplayWrapper class includes utilities for instrumenting the classes
 * for the replay version. Most methods of this class only need to insert 
 * static method invocation with empty argument before statement, 
 * so it is much simpler than RecordWrapper.
 * @author jeffhuang
 *
 */
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
