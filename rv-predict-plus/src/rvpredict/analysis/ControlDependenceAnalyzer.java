package rvpredict.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import rvpredict.util.Util;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Trap;
import soot.Unit;

import soot.jimple.ExitMonitorStmt;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.ThrowStmt;

import soot.toolkits.exceptions.ThrowableSet;

import soot.util.Chain;
/*
 * Please give me more info on how this piece of code really works
 */
public class ControlDependenceAnalyzer {
  HashMap<Stmt, HashSet<Unit>> FWRTable;
  HashMap<Stmt, ControlScope> scopes;

  public ControlDependenceAnalyzer(Body body, MethodAnalyzer ma){
    Chain<Unit> units = body.getUnits();
    LoopAnalyzer la = new LoopAnalyzer(body, ma); // get the loop information
    HashMap<Stmt, Unit> exceptionGoto = new HashMap<Stmt, Unit>(); // a map convert a throw statement into a goto
    // the intersacted prescopes already handled in computeExceptionEdges
    HashMap<Stmt, List<Unit>> exceptionEdges = computeExceptionEdges(body, exceptionGoto);
    HashMap<Stmt, ControlScope> prescopes = computePrescopes(units, exceptionGoto, la);
    FWRTable = computeForwardReachability(exceptionEdges, units, la);
    //		extendPreScope(prescopes, FWRTable, units);
    scopes = computeEquivalentClass(prescopes, la, body);
    /*		Iterator<Stmt> scopeIt = scopes.keySet().iterator();
		while (scopeIt.hasNext()){
                Stmt stmt = scopeIt.next();
                System.out.println(" : " + scopes.get(stmt));
		}*/
  }

  HashMap<Stmt, List<Unit>> computeExceptionEdges(Body body, HashMap<Stmt, Unit> exceptionGoto){
    HashMap<Stmt, List<Unit>> exceptionEdges = new HashMap<Stmt, List<Unit>>();
    LinkedList<Trap> traps = new LinkedList<Trap>(); // the traps that have not been used
    traps.addAll(body.getTraps());
    LinkedList<Trap> activeTraps = new LinkedList<Trap>(); // the traps handling the current unit
    Iterator<Unit> unitIt = body.getUnits().iterator();
    while (unitIt.hasNext()){
      Stmt stmt = (Stmt)unitIt.next();
      // update the active trap list
      Object[] trapArray = traps.toArray();
      for (int i = 0; i<trapArray.length; i++){
        Trap trap = (Trap)trapArray[i];
        if (trap.getBeginUnit() == stmt){
          activeTraps.add(trap);
          traps.remove(trap);
        }
      }
      // remove used traps
      trapArray = activeTraps.toArray();
      for (int i = 0; i<trapArray.length; i++){
        Trap trap = (Trap)trapArray[i];
        if (trap.getEndUnit() == stmt) // trap.getEndUnit() returns the unit following the traped units
          activeTraps.remove(trap);
      }
      ThrowableSet ts = null;
      // for throw statements and invocation statements, find all possible exceptions
      // for control flow statements, record their targets for later use
      if (stmt instanceof ThrowStmt) {
        ts = Scene.v().getDefaultThrowAnalysis().mightThrowExplicitly((ThrowStmt)stmt);
      } else if (stmt instanceof InvokeStmt) {
        SootMethod toInvoke = ((InvokeStmt)stmt).getInvokeExpr().getMethod();
        if (toInvoke.getExceptions().size() > 0){
          Iterator it = toInvoke.getExceptions().iterator();
          ts = ThrowableSet.Manager.v().EMPTY;
          while (it.hasNext()){
            SootClass exClass =(SootClass)it.next();
            ts = ts.add(exClass.getType());
          }
        }
      }
      if ((ts != null) && (ts != ThrowableSet.Manager.v().EMPTY)){
        // exceptions found, locate corresponding handlers
        ArrayList<Unit> targets = new ArrayList<Unit>(); // the beginnings of handlers
        Iterator it = activeTraps.iterator();
        Unit last = null; // the beginning of the last handler for the given list of exceptions
        while (it.hasNext()){
          Trap trap = (Trap)it.next();
          if (ts.catchableAs(trap.getException().getType())){
            // one trap may catch multiple exceptions
            Unit target = trap.getHandlerUnit();
            targets.add(target);
            if ((last == null) || (Util.getJimpleLine((Stmt)target) > Util.getJimpleLine((Stmt)last)))
              last = target;
          }
          ts = ts.whichCatchableAs(trap.getException().getType()).getUncaught();
        }
        // add new exception edge
        exceptionEdges.put(stmt, targets);
        // add new exception goto
        if (ts != ThrowableSet.Manager.v().EMPTY) // some exceptions are not handled in this method
          exceptionGoto.put(stmt, new MyStmt());
        else
          exceptionGoto.put(stmt, last); // last should not be null
      }
    }
    return exceptionEdges;
  }
  /*@
   * compute the pre-scope of every if statment (it would be much easier if we parse the source code)
   */
  @SuppressWarnings("unchecked")
  HashMap<Stmt, ControlScope> computePrescopes(Chain<Unit> units, HashMap<Stmt, Unit> exceptionGoto, LoopAnalyzer la){
    HashMap<Stmt, ControlScope> prescopes = new HashMap<Stmt, ControlScope>();
    HashSet<ScopeInfo> scopes = new HashSet<ScopeInfo>();
    Iterator unitIt = units.iterator();
    while(unitIt.hasNext()){
      Stmt stmt = (Stmt)unitIt.next();
      if (! la.isLoop(stmt)){
        // for a branch statement, create a new item to check
        ScopeInfo curr = new ScopeInfo(stmt);
        if (stmt instanceof IfStmt){
          //for a non-loop if statement, create a new item to check
          curr.targets.add((Stmt)units.getSuccOf(stmt));
          curr.targets.add(((IfStmt)stmt).getTarget());
        } else if (stmt instanceof LookupSwitchStmt){
          LookupSwitchStmt lswitch = (LookupSwitchStmt)stmt;
          curr.targets.addAll(lswitch.getTargets());
        } else if (stmt instanceof TableSwitchStmt){
          TableSwitchStmt lswitch = (TableSwitchStmt)stmt;
          curr.targets.addAll(lswitch.getTargets());
        } else
          curr = null;
        if (curr != null){
          // also updates all surrounding conditional statements' scopes
          Iterator<ScopeInfo> scopeIt = scopes.iterator();
          while (scopeIt.hasNext()){
            ScopeInfo sc = scopeIt.next();
            if (sc.targets.contains(stmt)){
              sc.targets.remove(stmt);
              sc.targets.addAll(curr.targets);
              Iterator tIt = curr.targets.iterator();
              while (tIt.hasNext()){
                Stmt st = (Stmt)tIt.next();
                curr.farest = curr.farest < Util.getJimpleLine(st)? Util.getJimpleLine(st) : curr.farest;
              }
            }
          }
          // a new item created
          scopes.add(curr);
          continue;
        }
      }

      // handling other statements and loops
      Unit next = null; // the target statement to execute after stmt
      if (stmt instanceof GotoStmt){
        // only use the target of a goto that is not part of the beginning/end of a loop
        // note that the goto may be at the beginning of the loop: a for loop may have a goto
        // at the beginning to jump to the conditional at the end
        if  (la.getLoopByLn(Util.getJimpleLine(stmt))==null)
          next = ((GotoStmt)stmt).getTarget();
        // the prescope of a goto scales from its source to the dest
        // not needed any more: prescopes.put(stmt, new ControlScope(Util.getJimpleLine((Stmt)stmt), Util.getJimpleLine((Stmt)next)));
      } else if ((stmt instanceof ThrowStmt) || (stmt instanceof InvokeStmt)){
        // treat throw as a goto
        next = exceptionGoto.get(stmt);
      }  else if ((stmt instanceof ReturnStmt) || (stmt instanceof ReturnVoidStmt))
        next = new MyStmt();
      if (next == null)
        next = units.getSuccOf(stmt);
      if (next instanceof MyStmt){
        // some thrown exceptions are not handled in this method or return encountered
        // prescopes.put(stmt, new ControlScope(Util.getJimpleLine((Stmt)stmt), Integer.MAX_VALUE));
        HashSet<ScopeInfo> newScopes = new HashSet<ScopeInfo>();
        Iterator<ScopeInfo> scopeIt = scopes.iterator();
        while (scopeIt.hasNext()){
          // update the information for all related conditionals
          ScopeInfo curr = scopeIt.next();
          if (curr.targets.contains(stmt)){
            prescopes.put(curr.start, new ControlScope(Util.getJimpleLine(curr.start), Integer.MAX_VALUE));
          } else
            newScopes.add(curr);
        }
        scopes = newScopes;
      } else {
        HashSet<ScopeInfo> newScopes = new HashSet<ScopeInfo>();
        Iterator<ScopeInfo> scopeIt = scopes.iterator();
        while (scopeIt.hasNext()){
          // update the targets for all related conditionals
          // because targets is a set, it can be reduced to one if the branches converge
          ScopeInfo curr = scopeIt.next();
          if (curr.targets.contains(stmt)){
            curr.targets.remove(stmt);
            curr.targets.add(next);
            curr.farest = curr.farest < Util.getJimpleLine((Stmt)next)? Util.getJimpleLine((Stmt)next) : curr.farest;
          }
          if (curr.targets.size() == 1){
            // the branches merge
            Stmt target = (Stmt)curr.targets.iterator().next();
            int sln = Util.getJimpleLine(curr.start);
            int eln = Util.getJimpleLine(target);
            if (eln >= sln)
              prescopes.put(curr.start, new ControlScope(sln, eln));
            else {
              // there are two possibilities:
              // 1) the end of loop
              // 2) the end of a synchronized block
              ControlScope lScope = la.getLoopByLn(eln);
              if (lScope != null)
                prescopes.put(curr.start, new ControlScope(sln, lScope.end, 1));
              else if (target instanceof ExitMonitorStmt){
                target = (Stmt)units.getSuccOf(target);
                while (target instanceof GotoStmt){
                  target = (Stmt)((GotoStmt)target).getTarget();
                }
                prescopes.put(curr.start, new ControlScope(sln, Util.getJimpleLine(target)));
              } else
                // one more possibility is from conditional expressions, for example:
                //       float score =
                // f < SCORE_CACHE_SIZE
                //  ? scoreCache[f]
                // : similarity.tf(f)*weightValue;
                // it will merge at the first line (the assignment), making the target before the if
                // so we need to use the farest field to decide the scope
                prescopes.put(curr.start, new ControlScope(sln, curr.farest));
              //System.err.println("Cannot find the control scope of line " + sln);
            }
          } else
            newScopes.add(curr);
        }
        scopes = newScopes;
      }
    }
    return prescopes;
  }
  HashMap<Stmt, HashSet<Unit>> computeForwardReachability(HashMap<Stmt, List<Unit>> exceptionEdges, Chain<Unit> units, LoopAnalyzer la){
    HashMap<Stmt, HashSet<Unit>> FWRTable = new HashMap<Stmt, HashSet<Unit>>();
    computeFWR(FWRTable, exceptionEdges, units, units.getFirst(), la);
    HashMap<Stmt, HashSet<Unit>> FWRTable2 = new HashMap<Stmt, HashSet<Unit>>();
    Iterator<Stmt> tableIt = FWRTable.keySet().iterator();
    while (tableIt.hasNext()){
      Stmt stmt = tableIt.next();
      if (la.isLoop(stmt)){
        Unit target = null;
        if (stmt instanceof IfStmt){
          target = ((IfStmt)stmt).getTarget();
        } else
          target = ((GotoStmt)stmt).getTarget();
        HashSet<Unit> t1 = FWRTable.get(target);
        HashSet<Unit> t2 = FWRTable.get(stmt);
        t2.addAll(t1);
        FWRTable2.put(stmt, t2);
      } else {
        if ((stmt instanceof IfStmt) || (stmt instanceof LookupSwitchStmt) || (stmt instanceof TableSwitchStmt)){
          FWRTable2.put(stmt, FWRTable.get(stmt));
        }
      }
    }
    return FWRTable2;
  }

  @SuppressWarnings("unchecked")
  HashSet<Unit> computeFWR(HashMap<Stmt, HashSet<Unit>> FWRTable, HashMap<Stmt, List<Unit>> exceptionEdges, Chain<Unit> units, Unit curr, LoopAnalyzer la){
    HashSet<Unit> targets = FWRTable.get((Stmt)curr);
    if (targets == null){
      targets = new HashSet<Unit>();
      targets.add(curr); // forward reachability is reflexive
      FWRTable.put((Stmt)curr, targets);
      ArrayList<Unit> nextList = new ArrayList<Unit>();
      if (!(curr instanceof ReturnStmt) && !(curr instanceof ReturnVoidStmt) && (units.getLast() != curr)){
        if (curr instanceof IfStmt){
          nextList.add(units.getSuccOf(curr));
          Unit target = ((IfStmt)curr).getTarget();
          if (! la.isLoop((curr))){
            nextList.add(target);
          } else {
            if (FWRTable.get((Stmt)target)==null)
              nextList.add(target);
          }
        } else if (curr instanceof GotoStmt){
          Unit target = ((GotoStmt)curr).getTarget();
          if (! la.isLoop((curr))){
            nextList.add(target);
          } else if (FWRTable.get((Stmt)target) != null)
            nextList.add(units.getSuccOf(curr));
          else
            System.err.println("Unexpected situation for Forward reachability!");
        } else if (curr instanceof LookupSwitchStmt){
          nextList.addAll(((LookupSwitchStmt)curr).getTargets());
        } else if (curr instanceof TableSwitchStmt){
          nextList.addAll(((TableSwitchStmt)curr).getTargets());
        } else if (curr instanceof ThrowStmt){
          List<Unit> ex = exceptionEdges.get(curr);
          if (ex != null)
            nextList.addAll(ex);
        } else if (curr instanceof InvokeStmt){
          nextList.add(units.getSuccOf(curr));
          List<Unit> ex = exceptionEdges.get(curr);
          if (ex != null)
            nextList.addAll(ex);
        } else {
          nextList.add(units.getSuccOf(curr));
        }
        Iterator it = nextList.iterator();
        while (it.hasNext()){
          Unit next = (Unit)it.next();
          if (! la.isLoop((Stmt)next) || (FWRTable.get((Stmt)next) == null))
            targets.addAll(computeFWR(FWRTable, exceptionEdges, units, next, la));
        }
      }
    }
    return targets;
  }

  //	void extendPreScope(HashMap<Stmt, ControlScope> prescopes, HashMap<Stmt, HashSet> FWRTable, Chain units){}

  HashMap<Stmt, ControlScope> computeEquivalentClass(HashMap<Stmt, ControlScope> prescopes, LoopAnalyzer la, Body body){
    HashMap<Stmt, ControlScope> scopes = new HashMap<Stmt, ControlScope>();
    HashMap<Stmt, HashSet<Stmt>> equivalentClasses = new HashMap<Stmt, HashSet<Stmt>>();
    Iterator<Unit> loopIt = la.loopIterator();
    while (loopIt.hasNext()){
      Unit loop = loopIt.next();
      ControlScope lscope = la.getLoop(loop);
      lscope.isInfinite = la.isInfinite(body, loop);
      Iterator<Stmt> scopeIt = prescopes.keySet().iterator();
      HashSet<Stmt> aClass = new HashSet<Stmt>();
      if (loop instanceof IfStmt)
        aClass.add((Stmt)loop);
      while (scopeIt.hasNext()){
        Stmt stmt = scopeIt.next();
        ControlScope iscope = prescopes.get(stmt);
        if (lscope.includes(iscope.start) && (iscope.includes(lscope.end))) {
          // extend the conditional statement
          iscope.start = lscope.start;
          iscope.isInfinite = lscope.isInfinite;
          HashSet<Stmt> anotherClass = equivalentClasses.get(stmt);
          if (anotherClass != null)
            aClass.addAll(anotherClass);
          else
            aClass.add(stmt);
        } else if ((lscope.start >= iscope.start) && (lscope.end <= iscope.end)) {
          if (isFWR(stmt, (Stmt)loop)){
            iscope.isInfinite = lscope.isInfinite;
          }
        }
      }
      Iterator<Stmt> classIt = aClass.iterator();
      while (classIt.hasNext())
        equivalentClasses.put(classIt.next(), aClass);
    }
    Iterator<HashSet<Stmt>> classIt = equivalentClasses.values().iterator();
    while (classIt.hasNext()) {
      HashSet<Stmt> aClass = classIt.next();
      ControlScope scope = null;
      Iterator<Stmt> stmtIt = aClass.iterator();
      while (stmtIt.hasNext()){
        Stmt stmt = stmtIt.next();
        ControlScope sc = prescopes.get(stmt);
        if (sc == null)
          sc = la.getLoop(stmt);
        if (scope == null)
          scope = new ControlScope(sc);
        else {
          if (scope.start >= sc.start)
            scope.start = sc.start;
          if (scope.end <= sc.end) {
            scope.end = sc.end;
            scope.offset = sc.offset;
          }
          scope.isInfinite = sc.isInfinite;
        }
      }
      stmtIt = aClass.iterator();
      while (stmtIt.hasNext())
        scopes.put(stmtIt.next(), scope);
    }
    Iterator<Stmt> scopeIt = prescopes.keySet().iterator();
    while (scopeIt.hasNext()){
      Stmt stmt = scopeIt.next();
      ControlScope sc = scopes.get(stmt);
      if (sc == null)
        scopes.put(stmt, prescopes.get(stmt));
    }
    return scopes;
  }

  /*@
   * check if a control-depends on b
   */
  public boolean dependOn(Stmt a, Stmt b){
    ControlScope scope = scopes.get(b);
    if (scope == null)
      return false;
    int lna = Util.getJimpleLine(a);
    int lnb = Util.getJimpleLine(b);
    // check if lnb falls in the control scope of a
    if (scope.includes(lna)){
      // if yes, check if b is before a (in a loop) or a can forward-reach b
      if ((lnb >= lna) || isFWR(b, a)) {
        //System.out.println(lna + ":" + Util.getJimpleLine(a) + ":" + a);
        //System.out.println("depends on");
        //System.out.println(lnb + ":" + Util.getJimpleLine(b) + ":" + b);
        return true;
      }
    }
    return false;
  }

  /*@
   * check if b can be forward-reached from a
   */
  public boolean isFWR(Stmt a, Stmt b){
    HashSet<Unit> targets = FWRTable.get(a);
    if (targets == null)
      return false;
    return targets.contains(b);
  }

  /*@
   * check if the statement jumps back (the end of a loop)
   */
  boolean isBackJump(Stmt stmt){
    Unit target = null;
    // get the jump target
    if (stmt instanceof IfStmt)
      target = ((IfStmt)stmt).getTarget();
    else if (stmt instanceof GotoStmt)
      target = ((GotoStmt)stmt).getTarget();
    if (target != null){
      int ln1 = Util.getJimpleLine(stmt);
      int ln2 = Util.getJimpleLine((Stmt)target);
      return ln1 > ln2;
    }
    return false;
  }
  /*@
   * Auxillary class for compute prescopes of conditional statements
   */
  class ScopeInfo {
    Stmt start; // the control statement
    HashSet<Unit> targets; // the ends of different branches
    int farest; // the end of the farest branches, useful in some special cases
    public ScopeInfo(Stmt s){
      start = s;
      targets = new HashSet<Unit>();
      farest = -1;
    }
  }
  /*@
   * A dump class to mark out the end of method
   */
  @SuppressWarnings("unchecked")
  class MyStmt extends soot.jimple.internal.JNopStmt{ // used to mark unhandled exceptions
    static final long serialVersionUID = 0;
  }
}
// vim: tw=100:sw=2
