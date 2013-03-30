package rvpredict.logging;

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

public final class NewWrapper {
  private static final SootClass logClass = Scene.v().loadClassAndSupport("rvpredict.logging.NewRT");
  private static final SootMethodRef logLockMethod = logClass.getMethod("void logLock(int,java.lang.Object)").makeRef();
  private static final SootMethodRef logUnlockMethod = logClass.getMethod("void logUnlock(int,java.lang.Object)").makeRef();
  private static final SootMethodRef logWaitMethod = logClass.getMethod("void logWait(int,java.lang.Object)").makeRef();
  private static final SootMethodRef logNotifyMethod = logClass.getMethod("void logNotify(int,java.lang.Object)").makeRef();
  private static final SootMethodRef logFieldAccMethod = logClass.getMethod("void logFieldAcc(int,java.lang.Object,int,java.lang.Object,boolean)").makeRef();
  //private static final SootMethodRef logArrayAccMethod = logClass.getMethod("void logArrayAcc(java.lang.String,java.lang.Object,int,java.lang.String,java.lang.String,int,boolean)").makeRef();
  private static final SootMethodRef logStartMethod = logClass.getMethod("void logStart(int,java.lang.Object)").makeRef();
  private static final SootMethodRef logJoinMethod = logClass.getMethod("void logJoin(int,java.lang.Object)").makeRef();
  private static final SootMethodRef logBranchMethod = logClass.getMethod("void logBranch(int)").makeRef();
  private static final SootMethodRef logBBMethod = logClass.getMethod("void logBasicBlock(int)").makeRef();
  //private static final SootMethodRef logStaticSyncLockMethod = logClass.getMethod("void logStaticSyncLock(java.lang.String)").makeRef();
  //private static final SootMethodRef logStaticSyncUnlockMethod = logClass.getMethod("void logStaticSyncUnlock(java.lang.String)").makeRef();

  private static final SootMethodRef valueOfBooleanMethod = Scene.v().getMethod("<java.lang.Boolean: java.lang.Boolean valueOf(boolean)>").makeRef();
  private static final SootMethodRef valueOfByteMethod = Scene.v().getMethod("<java.lang.Byte: java.lang.Byte valueOf(byte)>").makeRef();
  private static final SootMethodRef valueOfCharMethod = Scene.v().getMethod("<java.lang.Character: java.lang.Character valueOf(char)>").makeRef();
  private static final SootMethodRef valueOfDoubleMethod = Scene.v().getMethod("<java.lang.Double: java.lang.Double valueOf(double)>").makeRef();
  private static final SootMethodRef valueOfFloatMethod = Scene.v().getMethod("<java.lang.Float: java.lang.Float valueOf(float)>").makeRef();
  private static final SootMethodRef valueOfIntMethod = Scene.v().getMethod("<java.lang.Integer: java.lang.Integer valueOf(int)>").makeRef();
  private static final SootMethodRef valueOfLongMethod = Scene.v().getMethod("<java.lang.Long: java.lang.Long valueOf(long)>").makeRef();
  private static final SootMethodRef valueOfShortMethod = Scene.v().getMethod("<java.lang.Short: java.lang.Short valueOf(short)>").makeRef();


  public static InvokeStmt logBranch(int id, final Stmt s) {
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logBranchMethod,
        IntConstant.v(id));
    return Jimple.v().newInvokeStmt(logExpr);
  }
  public static InvokeStmt logBasicBlock(int id, final Stmt s) {
	    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logBBMethod,
	        IntConstant.v(id));
	    return Jimple.v().newInvokeStmt(logExpr);
	  }

//  public static InvokeStmt logLock(SootClass c) {
//    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logStaticSyncLockMethod,
//        StringConstant.v(c.getName()));
//    return Jimple.v().newInvokeStmt(logExpr);
//  }

  public static InvokeStmt logLock(int id, Stmt s) {
        return logLock(id, ((EnterMonitorStmt)s).getOp());
  }

  public static InvokeStmt logLock(int id, Value v) {
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logLockMethod,
    		IntConstant.v(id),
        v);
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logWait(int id, Stmt s){
	    InstanceInvokeExpr e = (InstanceInvokeExpr)s.getInvokeExpr();

	    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logWaitMethod,
	    	IntConstant.v(id),	
	        e.getBase());
	    return Jimple.v().newInvokeStmt(logExpr);
	  }

  public static InvokeStmt logNotify(int id, Stmt s){
    InstanceInvokeExpr e = (InstanceInvokeExpr)s.getInvokeExpr();

    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logNotifyMethod,
    		IntConstant.v(id),	
        e.getBase());
    return Jimple.v().newInvokeStmt(logExpr);
  }

//  public static InvokeStmt logUnlock(SootClass c) {
//    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logStaticSyncUnlockMethod,
//        StringConstant.v(c.getName()));
//    return Jimple.v().newInvokeStmt(logExpr);
//  }

  public static InvokeStmt logUnlock(int id, Stmt s) {
    return logUnlock(id, ((ExitMonitorStmt)s).getOp());
  }

  public static InvokeStmt logUnlock(int id, Value v) {
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logUnlockMethod,
    		IntConstant.v(id),
        v);
    return Jimple.v().newInvokeStmt(logExpr);
  }


  public static InvokeStmt logFieldAcc(int id, int sid, Body body, Stmt s) {
    SootField f = s.getFieldRef().getField();
    DefinitionStmt d = (DefinitionStmt)s;
    boolean write = (d.getLeftOp() instanceof FieldRef);
    Value instanceObject;
    if (s.getFieldRef() instanceof InstanceFieldRef) {
      instanceObject = ((InstanceFieldRef)s.getFieldRef()).getBase();
    } else {
      instanceObject = NullConstant.v();
    }
    
	Local rv_local;
	AssignStmt newAssignStmt;
	Value staticInvoke;
	
    Value v;
    if(write)
    	v = d.getRightOp();
    else
    	v = d.getLeftOp();
        
    if(v.getType() instanceof PrimType)
    {

    	
    	if(v.getType() instanceof BooleanType)
    	{
    		rv_local = Jimple.v().newLocal("rv_local", RefType.v("java.lang.Boolean"));           
            staticInvoke = Jimple.v().newStaticInvokeExpr(valueOfBooleanMethod,v);  
    	}
    	else if(v.getType() instanceof ByteType)
    	{
    		rv_local = Jimple.v().newLocal("rv_local", RefType.v("java.lang.Byte"));           
            staticInvoke = Jimple.v().newStaticInvokeExpr(valueOfByteMethod,v); 
    	}
    	else if(v.getType() instanceof CharType)
    	{
    		rv_local = Jimple.v().newLocal("rv_local", RefType.v("java.lang.Char"));           
            staticInvoke = Jimple.v().newStaticInvokeExpr(valueOfCharMethod,v); 
    	}
    	else if(v.getType() instanceof DoubleType)
    	{
    		rv_local = Jimple.v().newLocal("rv_local", RefType.v("java.lang.Double"));           
            staticInvoke = Jimple.v().newStaticInvokeExpr(valueOfDoubleMethod,v); 
    	}
    	else if(v.getType() instanceof FloatType)
    	{
    		rv_local = Jimple.v().newLocal("rv_local", RefType.v("java.lang.Float"));           
            staticInvoke = Jimple.v().newStaticInvokeExpr(valueOfFloatMethod,v); 
    	}
    	else if(v.getType() instanceof IntType)
    	{
    		rv_local = Jimple.v().newLocal("rv_local", RefType.v("java.lang.Int"));           
            staticInvoke = Jimple.v().newStaticInvokeExpr(valueOfIntMethod,v); 
    	}
    	else if(v.getType() instanceof LongType)
    	{
    		rv_local = Jimple.v().newLocal("rv_local", RefType.v("java.lang.Long"));           
            staticInvoke = Jimple.v().newStaticInvokeExpr(valueOfLongMethod,v); 
    	}
    	else//if (v.getType() instanceof ShortType)
    	{
    		rv_local = Jimple.v().newLocal("rv_local", RefType.v("java.lang.Short"));           
            staticInvoke = Jimple.v().newStaticInvokeExpr(valueOfShortMethod,v); 
    	}
		
        newAssignStmt = Jimple.v().newAssignStmt(rv_local, staticInvoke);
		 			 
    }
    else
    {
		rv_local = Jimple.v().newLocal("rv_local", v.getType());           
        newAssignStmt = Jimple.v().newAssignStmt(rv_local, v);

    }

    body.getLocals().add(rv_local);
    body.getUnits().insertAfter(newAssignStmt,s);
    
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logFieldAccMethod,
    		IntConstant.v(id),
    		instanceObject,
    		IntConstant.v(sid),
    		rv_local,
        IntConstant.v(write?1:0));
    
    InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(logExpr);
	 body.getUnits().insertAfter(invokeStmt,newAssignStmt);

    
    return invokeStmt;
    
  }

//  public static InvokeStmt logArrayAcc(Stmt s, SootClass c, SootMethod m) {
//    ArrayRef ar = s.getArrayRef();
//    DefinitionStmt d = (DefinitionStmt)s;
//    boolean write = (d.getLeftOp() instanceof FieldRef);
//
//    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logArrayAccMethod,
//        StringConstant.v(ar.getType().toString()),
//        ar.getBase(),
//        ar.getIndex(),
//        StringConstant.v(c.getName()),
//        StringConstant.v(m.getSubSignature()),
//        IntConstant.v(/*getJimpleLine(s)*/-1),
//        IntConstant.v(write?1:0));
//    return Jimple.v().newInvokeStmt(logExpr);
//  }

  public static InvokeStmt logStart(int id, Stmt s) {
    InstanceInvokeExpr e = (InstanceInvokeExpr) s.getInvokeExpr();

    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logStartMethod,
    		IntConstant.v(id),
        e.getBase());
    return Jimple.v().newInvokeStmt(logExpr);
  }

  public static InvokeStmt logJoin(int id, Stmt s) {
    InstanceInvokeExpr e = (InstanceInvokeExpr) s.getInvokeExpr();

    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logJoinMethod,
    		IntConstant.v(id),
        e.getBase());
    return Jimple.v().newInvokeStmt(logExpr);
  }

}
// vim: tw=100:sw=2
