package rvpredict.instrumentation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import rvpredict.prediction.PredictorOptions;
import rvpredict.util.Util;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Trap;
import soot.Unit;
import soot.Value;
import soot.Transform;
import soot.BodyTransformer;

import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.FieldRef;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.TableSwitchStmt;
import soot.jimple.GotoStmt;
import soot.jimple.IdentityStmt;


import soot.util.Chain;


public class Instr extends BodyTransformer {
  static SootClass logClass;
  static SootMethod logLnMethod;
  static SootMethod logPostMethod;
  static SootMethod logExMethod;
  static SootMethod logEntryMethod;
  static SootMethod logObjMethod;
  static SootMethod logClsMethod;
  static SootMethod logArrayMethod;
  static HashSet<SootMethod> instrumentedMethods;
  static HashSet<SootMethod> calledMethods;
  static {
    logClass    = Scene.v().loadClassAndSupport("rvpredict.util.MyLogger");
    logLnMethod = logClass.getMethod("void logBlockEntry(int)");
    logPostMethod = logClass.getMethod("void logInvocationEnd(int)");
    logExMethod = logClass.getMethod("void logExEntry(int,java.lang.String,java.lang.String)");
    logEntryMethod = logClass.getMethod("void logMethodEntry(java.lang.String,java.lang.String)");
    logObjMethod = logClass.getMethod("void logFieldAccess(java.lang.Object,int)");
    logClsMethod = logClass.getMethod("void logClassAccess(java.lang.String,int)");
    logArrayMethod = logClass.getMethod("void logArrayAccess(java.lang.Object,int,int)");
    instrumentedMethods = new HashSet<SootMethod>();
    calledMethods = new HashSet<SootMethod>();
  }

  @Override
    protected void internalTransform(Body body, String phase, Map options) {
    instrumentedMethods.add(body.getMethod());
    // when the -app is used, MyLogger will also be visited
    // we add its methods to the instrumented methods to avoid purity check
    // but we also need to skip instrumenting it
    if (body.getMethod().getDeclaringClass().getName().contains("rvpredict"))
      return;
    // add counts
    Util.addJimpleLineTags(body);
    // a set containing added logging statements
    HashSet<Stmt> newStmts = new HashSet<Stmt>();
    // body's method
    SootMethod method = body.getMethod();
    // debugging
    if (PredictorOptions.v().verbose_level >= 2)
      System.out.println("instrumenting method : " + method.getSignature());
    // get body's unit as a chain
    Chain<Unit> units = body.getUnits();
    // get a snapshot iterator of the unit since we are going to
    // mutate the chain when iterating over it.
    //
    Iterator stmtIt = units.snapshotIterator();
    Iterator stmtIt2 = units.snapshotIterator();
    boolean begin = true;
    body.validate();
    // typical while loop for iterating over each statement
    while (stmtIt.hasNext()) {
      // cast back to a statement.
      Stmt stmt = (Stmt)stmtIt.next();

      // We need to log the start of a method. Thus we handle the first statement from the iterator
      // specially. soot requirest that the parameter unpacking IdentityStmts come before any non
      // identity statements (i.e., our logging call) thus we iterate past them before inserting the
      // logging call. I don't know if we can get IdentityStmts any other way, so this might
      // not be as safe as hoped. Not all methods have parameters (or references to this) so we need
      // to just insert our logging call as the first statement in that case.
      if (begin){
        Stmt oldstmt = null;
        while(stmtIt.hasNext() && (stmt instanceof IdentityStmt)) {
          oldstmt = stmt;
          stmt = (Stmt)stmtIt.next();
        }
        begin = false;
        String cls = method.getDeclaringClass().getName();
        String m = method.getSubSignature();
        // prepare an invocation statement
        InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logEntryMethod.makeRef(),StringConstant.v(cls), StringConstant.v(m));
        Stmt logStmt = Jimple.v().newInvokeStmt(logExpr);
        // insert new statement
        if (oldstmt != null)
          units.insertAfter(logStmt, oldstmt);
        else
          units.insertBefore(logStmt, stmt);
      }
      if (stmt instanceof IfStmt) {
        handleIfStmt((IfStmt)stmt, newStmts, units);
      } else if ((stmt instanceof LookupSwitchStmt) || (stmt instanceof TableSwitchStmt)){
        handleSwitchStmt(stmt, newStmts, units);
      } else if (stmt instanceof EnterMonitorStmt) {
        Value owner = ((EnterMonitorStmt)stmt).getOp();
        // prepare an invocation statement
        InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logObjMethod.makeRef(),owner,IntConstant.v(Util.getJimpleLine(stmt)));
        Stmt logStmt = Jimple.v().newInvokeStmt(logExpr);
        // insert the logging statement
        units.insertAfter(logStmt, stmt);
      } else if (stmt instanceof ExitMonitorStmt) {
        Value owner = ((ExitMonitorStmt)stmt).getOp();
        // prepare an invocation statement
        InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logObjMethod.makeRef(),owner,IntConstant.v(Util.getJimpleLine(stmt)));
        Stmt logStmt = Jimple.v().newInvokeStmt(logExpr);
        // insert the logging statement
        units.insertAfter(logStmt, stmt);
      }
    }
    // get traps (exception handlers)
    // the source line number is used here
    Chain traps = body.getTraps();
    Iterator trapIt = traps.iterator();
    while (trapIt.hasNext()){
      Trap trap = (Trap)trapIt.next();
      Unit target = trap.getHandlerUnit();
      if (! newStmts.contains(target)){
        int ln = Util.extractLineNumber(target);
        // prepare arguments
        String cls = method.getDeclaringClass().getName();
        String m = method.getSubSignature();
        ArrayList<Value> args = new ArrayList<Value>();
        args.add(IntConstant.v(ln));
        args.add(StringConstant.v(cls));
        args.add(StringConstant.v(m));
        // prepare an invocation statement
        InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logExMethod.makeRef(),args);
        Stmt logStmt = Jimple.v().newInvokeStmt(logExpr);
        // insert new statement
        units.insertBefore(logStmt, target);
        // change the trap's handler
        trap.setHandlerUnit(logStmt);
        // record the new statement
        newStmts.add(logStmt);
      }
    }
    while (stmtIt2.hasNext()) {
      // cast back to a statement.
      Stmt stmt = (Stmt)stmtIt2.next();
      if (stmt.containsInvokeExpr()) {
        handleInvokeStmt(stmt, units);
        calledMethods.add(stmt.getInvokeExpr().getMethod());
      } else if (stmt.containsFieldRef()){
        handleFieldRef(stmt, units);
      } else if (stmt.containsArrayRef()){
        handleArrayRef(stmt, units);
      }
    }
  }

  /*
   * Note: the log statement should be inserted before the invocation.
   * Otherwise, the target object can be changed during the statement, e.g., n = n.next()
   * However, an object cannot be used before its <init> method!
   * Also, if we do not know the end of the invocation,
   * we may not know if a beginning of a <init> is caused by a call back or not.
   */
  public void handleInvokeStmt(Stmt stmt, Chain<Unit> units){
    InvokeExpr expr = stmt.getInvokeExpr();
    Value owner = null;
    if (expr instanceof InstanceInvokeExpr){
      owner = ((InstanceInvokeExpr)expr).getBase();
      // prepare an invocation statement
      InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logObjMethod.makeRef(),owner,IntConstant.v(Util.getJimpleLine(stmt)));
      Stmt logStmt = Jimple.v().newInvokeStmt(logExpr);
      // insert the logging statement
      if (expr.getMethod().getName().compareTo("<init>") == 0)
        units.insertAfter(logStmt, stmt);
      else
        units.insertBefore(logStmt, stmt);
    } else {
      owner = StringConstant.v(expr.getMethod().getDeclaringClass().getName());
      // prepare an invocation statement
      InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logClsMethod.makeRef(),owner,IntConstant.v(Util.getJimpleLine(stmt)));
      Stmt logStmt = Jimple.v().newInvokeStmt(logExpr);
      // insert the logging statement
      units.insertBefore(logStmt, stmt);
    }
    if (expr.getMethod().getName().compareTo("<init>") != 0) {
      // prepare an invocation statement
      InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logPostMethod.makeRef(),IntConstant.v(Util.getJimpleLine(stmt)));
      Stmt logStmt = Jimple.v().newInvokeStmt(logExpr);
      // insert the logging statement
      units.insertAfter(logStmt, stmt);
    }
  }

  public void handleFieldRef(Stmt stmt, Chain<Unit> units){
    FieldRef ref = stmt.getFieldRef();
    if (ref instanceof InstanceFieldRef){
      Value owner = ((InstanceFieldRef)ref).getBase();
      // prepare an invocation statement
      InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logObjMethod.makeRef(),owner,IntConstant.v(Util.getJimpleLine(stmt)));
      Stmt logStmt = Jimple.v().newInvokeStmt(logExpr);
      // insert the logging statement
      units.insertBefore(logStmt, stmt);
    }
  }

  public void handleArrayRef(Stmt stmt, Chain<Unit> units){
    ArrayRef ref = stmt.getArrayRef();
    Value owner = ref.getBase();
    Value index = ref.getIndex();
    // prepare arguments
    ArrayList<Value> args = new ArrayList<Value>();
    args.add(owner);
    args.add(index);
    args.add(IntConstant.v(Util.getJimpleLine(stmt)));
    // prepare an invocation statement
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logArrayMethod.makeRef(),args);
    Stmt logStmt = Jimple.v().newInvokeStmt(logExpr);
    // insert the logging statement
    units.insertBefore(logStmt, stmt);
  }

  /*
   * To be discarded
   */
  public void handleAssignStmt(AssignStmt stmt, Chain<Unit> units){
    Value left = stmt.getLeftOp();
    Value right = stmt.getRightOp();
    Value owner = null;
    Value index = null;
    // get the target line number
    // int ln = Util.extractLineNumber(stmt);
    // in Jimple, it is impossible to have field accesses on both sides
    if (left instanceof InstanceFieldRef){
      owner = ((InstanceFieldRef)left).getBase();
    } else if (right instanceof InstanceFieldRef){
      owner = ((InstanceFieldRef)right).getBase();
    } else if (left instanceof ArrayRef){
      owner = ((ArrayRef)left).getBase();
      index = ((ArrayRef)left).getIndex();
    } else if (right instanceof ArrayRef){
      owner = ((ArrayRef)right).getBase();
      index = ((ArrayRef)right).getIndex();
    } else if (right instanceof InstanceInvokeExpr){
      // Refer to note before handleInvokeExpr

      owner = ((InstanceInvokeExpr)right).getBase();
      // prepare an invocation statement
      InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logObjMethod.makeRef(),owner,IntConstant.v(Util.getJimpleLine(stmt)));
      Stmt logStmt = Jimple.v().newInvokeStmt(logExpr);
      // insert the logging statement
      units.insertBefore(logStmt, stmt);
      return;
    }
    if (owner != null){
      if (index == null){
        // prepare an invocation statement
        InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logObjMethod.makeRef(),owner,IntConstant.v(Util.getJimpleLine(stmt)));
        Stmt logStmt = Jimple.v().newInvokeStmt(logExpr);
        // insert the logging statement
        units.insertBefore(logStmt, stmt);
      } else {
        // prepare arguments
        ArrayList<Value> args = new ArrayList<Value>();
        args.add(owner);
        args.add(index);
        args.add(IntConstant.v(Util.getJimpleLine(stmt)));
        // prepare an invocation statement
        InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logArrayMethod.makeRef(),args);
        Stmt logStmt = Jimple.v().newInvokeStmt(logExpr);
        // insert the logging statement
        units.insertBefore(logStmt, stmt);
      }
    }
  }

  public void handleSwitchStmt(Stmt stmt, HashSet<Stmt> newStmts, Chain<Unit> units){
    if (stmt instanceof LookupSwitchStmt){
      LookupSwitchStmt lswitch = (LookupSwitchStmt)stmt;
      int count = lswitch.getTargetCount();
      for (int i = 0; i < count; i ++){
        Unit target = lswitch.getTarget(i);
        Unit from = null;
        while (target instanceof GotoStmt){
          // need to find out the real target of the goto,
          from = target;
          target = ((GotoStmt)from).getTarget();
        }
        if (! newStmts.contains(target)){
          // get the target line number
          // int ln = Util.extractLineNumber(target);
          // prepare an invocation statement
          InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logLnMethod.makeRef(),IntConstant.v(Util.getJimpleLine((Stmt)target)));
          Stmt logStmt = Jimple.v().newInvokeStmt(logExpr);
          // insert new statement
          units.insertBefore(logStmt, target);
          // change jump statment's target if needed
          if (from != null){
            ((GotoStmt)from).setTarget(logStmt);
          } else {
            lswitch.setTarget(i, logStmt);
          }
          // record the new statement
          newStmts.add(logStmt);
        }
      }
      Unit target = lswitch.getDefaultTarget();
      Unit from = null;
      while (target instanceof GotoStmt){
        // need to find out the real target of the goto,
        from = target;
        target = ((GotoStmt)from).getTarget();
      }
      if (! newStmts.contains(target)){
        // get the target line number
        // int ln = Util.extractLineNumber(target);
        // prepare an invocation statement
        InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logLnMethod.makeRef(),IntConstant.v(Util.getJimpleLine((Stmt)target)));
        Stmt logStmt = Jimple.v().newInvokeStmt(logExpr);
        // insert new statement
        units.insertBefore(logStmt, target);
        // change jump statment's target if needed
        if (from != null){
          ((GotoStmt)from).setTarget(logStmt);
        } else {
          lswitch.setDefaultTarget(logStmt);
        }
        // record the new statement
        newStmts.add(logStmt);
      }

    } else if (stmt instanceof TableSwitchStmt){
      TableSwitchStmt lswitch = (TableSwitchStmt)stmt;
      int count = lswitch.getTargets().size();
      for (int i = 0; i < count; i ++){
        Unit target = lswitch.getTarget(i);
        Unit from = null;
        while (target instanceof GotoStmt){
          // need to find out the real target of the goto,
          from = target;
          target = ((GotoStmt)from).getTarget();
        }
        if (! newStmts.contains(target)){
          // get the target line number
          // int ln = Util.extractLineNumber(target);
          // prepare an invocation statement
          InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logLnMethod.makeRef(),IntConstant.v(Util.getJimpleLine((Stmt)target)));
          Stmt logStmt = Jimple.v().newInvokeStmt(logExpr);
          // insert new statement
          units.insertBefore(logStmt, target);
          // change jump statment's target if needed
          if (from != null){
            ((GotoStmt)from).setTarget(logStmt);
          } else {
            lswitch.setTarget(i, logStmt);
          }
          // record the new statement
          newStmts.add(logStmt);
        }
      }
      Unit target = lswitch.getDefaultTarget();
      Unit from = null;
      while (target instanceof GotoStmt){
        // need to find out the real target of the goto,
        from = target;
        target = ((GotoStmt)from).getTarget();
      }
      if (! newStmts.contains(target)){
        // get the target line number
        // int ln = Util.extractLineNumber(target);
        // prepare an invocation statement
        InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logLnMethod.makeRef(),IntConstant.v(Util.getJimpleLine((Stmt)target)));
        Stmt logStmt = Jimple.v().newInvokeStmt(logExpr);
        // insert new statement
        units.insertBefore(logStmt, target);
        // change jump statment's target if needed
        if (from != null){
          ((GotoStmt)from).setTarget(logStmt);
        } else {
          lswitch.setDefaultTarget(logStmt);
        }
        // record the new statement
        newStmts.add(logStmt);
      }
    }
  }

  public void handleIfStmt(IfStmt stmt, HashSet<Stmt> newStmts, Chain<Unit> units){
    // get the target
    Stmt target = stmt.getTarget();
    Stmt from = null;
    while (target instanceof GotoStmt){
      // need to find out the real target of the goto,
      from = target;
      target = (Stmt)((GotoStmt)from).getTarget();
    }
    if (from == null)
      insertEnterLogStmt(target, stmt, newStmts, units);
    else
      insertEnterLogStmt(target, from, newStmts, units);

    // get the successor of stmt
    target = (Stmt)units.getSuccOf(stmt);
    from = null;
    while (target instanceof GotoStmt){
      // need to find out the real target of the goto,
      from = target;
      target = (Stmt)((GotoStmt)from).getTarget();
    }
    insertEnterLogStmt(target, from, newStmts, units);
  }

  public void insertEnterLogStmt(Stmt target, Stmt from, HashSet<Stmt> newStmts, Chain<Unit> units){
    if (newStmts.contains(target))
      return;
    // get the target line number
    // int ln = Util.extractLineNumber(target);
    // prepare another invocation statement
    InvokeExpr logExpr = Jimple.v().newStaticInvokeExpr(logLnMethod.makeRef(),IntConstant.v(Util.getJimpleLine(target)));
    Stmt logStmt = Jimple.v().newInvokeStmt(logExpr);
    // insert new statement
    units.insertBefore(logStmt, target);
    // change jump statment's target if needed
    if (from != null){
      if (from instanceof IfStmt)
        ((IfStmt)from).setTarget(logStmt);
      else if (from instanceof GotoStmt)
        ((GotoStmt)from).setTarget(logStmt);
    }
    // record the new statement
    newStmts.add(logStmt);

  }

}
// vim: tw=100:sw=2
