package rvpredict.analysis;

import java.util.Iterator;
import java.util.Map;

import soot.Body;
import soot.Pack;
import soot.PackManager;
import soot.Transform;
import soot.BodyTransformer;

import soot.jimple.IfStmt;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;

import soot.options.Options;

import rvpredict.util.Util;

import rvpredict.PredictorException;

public class Main extends BodyTransformer {
  final static int CDA = 1; // control dependence analysis
  final static int TA = 2; // termination analysis
  static int mode = CDA;
  @Override
    protected void internalTransform(Body body, String phase, Map options) {
    try {
      switch(mode){
      case CDA: doCDA(body); break;
      case TA: doTA(body); break;
      }
    } catch (Exception e){
      e.printStackTrace();
    }
  }
	
  void doCDA(Body body) throws PredictorException {
    //		if (body.getMethod().getName().compareTo("calc_bound") != 0)
    //			return;
    System.out.println("analyzing method : " + body.getMethod().getSignature() + " for control dependencies");
    ControlDependenceAnalyzer cda = new ControlDependenceAnalyzer(body, MethodAnalyzer.v());
    Iterator unitIt = body.getUnits().iterator();
    while (unitIt.hasNext()){
      Stmt stmt = (Stmt)unitIt.next();
      if ((stmt instanceof IfStmt) || (stmt instanceof LookupSwitchStmt) || (stmt instanceof TableSwitchStmt)){
        System.out.print(Util.extractLineNumber(stmt) + ": ");
        Iterator it = body.getUnits().iterator();
        int currln = -1;
        boolean wasDependent = false;;
        while (it.hasNext()){
          Stmt st = (Stmt)it.next();
          boolean isDependent = cda.dependOn(st, stmt);
          int newln = Util.extractLineNumber(st);
          // for all the statements from the same line of the source file, they should have the same dependence
          // this is our assumption
          if ((currln > -1) && (newln == currln) && (isDependent != wasDependent)){
            System.err.println("!! Error at line " + currln);
          } else {
            if (isDependent && (newln != currln)){
              currln = newln;
              System.out.print(currln + ", ");
              wasDependent = isDependent;
            }
          }
        }
        System.out.println();
      }				
    }
  }
	
  void doTA(Body body) throws PredictorException {
    System.out.println("analyzing method : " + body.getMethod().getSignature() + " for infinite loops");
    //LoopAnalyzer la = new LoopAnalyzer(body, MethodAnalyzer.getMethodAnalyzer());
    /*		Iterator unitIt = body.getUnits().iterator();
		while (unitIt.hasNext()){
                Stmt stmt = (Stmt)unitIt.next();
                Stmt target = null;
                if (stmt instanceof IfStmt)
                target = ((IfStmt)stmt).getTarget();
                else if (stmt instanceof GotoStmt)
                target = (Stmt)((GotoStmt)stmt).getTarget();
                if ((target != null) && (Util.extractLineNumber(stmt) > Util.extractLineNumber(target)) && !(target instanceof ExitMonitorStmt)){
                // need to check if target is an exitmonitor statement because, o.w., the following pattern will be considered as a loop:
                // synchronized(a) {
                // if (...) {}
                // }
                if (LoopAnalyzer.isInfinite(body, stmt)){
                System.out.println("Infinite loop found! " + Util.extractLineNumber(target) + " ~ " + Util.extractLineNumber(stmt));
                }
                }
		}*/
  }
	
  public static void main(String[] args) {
    /* check the arguments */
    if (args.length == 0) {
      System.err.println("Usage: java rvpredict.analysis.Main [options] classname");
      System.exit(0);
    }
    Options.v().set_keep_line_number(true);
    /* add a phase to transformer pack by call Pack.add */
    Pack jtp = PackManager.v().getPack("jtp");
    jtp.add(new Transform("jtp.analysis", new Main()));
    /* Give control to Soot to process all options,
     * InvokeStaticInstrumenter.internalTransform will get called.
     */
    soot.Main.main(args);
		
  }

}
// vim: tw=100:sw=2
