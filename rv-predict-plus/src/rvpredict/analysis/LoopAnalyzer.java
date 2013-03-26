package rvpredict.analysis;

import rvpredict.util.Util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import soot.Body;
import soot.Unit;

import soot.jimple.AssignStmt;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;

import soot.util.Chain;

public class LoopAnalyzer {
	
  HashMap<Unit, ControlScope> loops;
  HashMap<Integer, Unit> loopsByLn;
	
  MethodAnalyzer ma;
	
  public LoopAnalyzer(Body body, MethodAnalyzer ma){
    this.ma = ma;
    loops = new HashMap<Unit, ControlScope>();
    loopsByLn = new HashMap<Integer, Unit>();
    int maxln = -1;
    HashSet<Unit> visited = new HashSet<Unit>();
    Chain<Unit> units = body.getUnits();
    Iterator unitIt = units.iterator();
    while (unitIt.hasNext()){
      Unit unit = (Unit)unitIt.next();
      visited.add(unit);
      if (Util.extractLineNumber(unit) > maxln)
        maxln = Util.extractLineNumber(unit);
      Unit target = null;
      if (unit instanceof IfStmt)
        target = ((IfStmt)unit).getTarget();
      else if (unit instanceof GotoStmt)
        target = ((GotoStmt)unit).getTarget();
      if (target!= null){
        int s = Util.extractLineNumber(unit);
        int e = Util.extractLineNumber(target);
        ControlScope sc = null;
        if (s > e) {
          sc = new ControlScope(e, s, 1);
        }
        if (visited.contains(target)){
          if (sc == null)
            sc = new ControlScope(s, maxln, 1);
          loops.put(unit, sc);
          loopsByLn.put(new Integer(sc.start), unit);
          /*					System.out.println("found loop: " + sc);
                                                if (isInfinite(body, unit))
						System.out.println("!!! this is an infinite loop!");*/
        }
      }
    }
  }
	
  public boolean isLoop(Unit unit){
    return loops.containsKey(unit);
  }
	
  public ControlScope getLoop(Unit unit){
    return loops.get(unit);
  }
	
  public ControlScope getLoopByLn(int ln){
    return loops.get(loopsByLn.get(new Integer(ln)));
  }
	
  public Unit getLoopEnd(int ln){
    return loopsByLn.get(new Integer(ln));
  }
	
  public Iterator<Unit> loopIterator(){
    return loops.keySet().iterator();
  }
	
  /*@
   * a very naive termination analyzer
   */
  public boolean isInfinite(Body body, Unit loop){
    ControlScope scope = loops.get(loop);
    boolean hasExit = false; // has some branch statement out of the loop
    boolean hasUpdate = false; // has update statement in the loop
    Unit start = null;
    if (loop instanceof IfStmt) {
      start = ((IfStmt)loop).getTarget();
      hasExit = true;
    }
    else if (loop instanceof GotoStmt)
      start = ((GotoStmt)loop).getTarget();
		
    if (start != null){
      Chain<Unit> units = body.getUnits();
      while (start != loop) {
        int toln;
        if (start instanceof IfStmt) {
          toln = Util.extractLineNumber(((IfStmt)start).getTarget());
          if ((toln > scope.end) || (toln < scope.start))
            hasExit = true;
        }
        else if (start instanceof GotoStmt) {
          toln = Util.extractLineNumber(((GotoStmt)start).getTarget());
          if ((toln > scope.end) || (toln < scope.start))
            hasExit = true;
        } else if (start instanceof AssignStmt)
          hasUpdate = true;
        else if (start instanceof InvokeStmt){
          InvokeExpr invoke = ((InvokeStmt)start).getInvokeExpr();
          if (!ma.isPure(invoke.getMethodRef()))
            hasUpdate = true;
        }
        start = units.getSuccOf(start);
      }
      if ((! hasExit) || (! hasUpdate))
        return true;			
    }
    return false;
  }
}
// vim: tw=100:sw=2
