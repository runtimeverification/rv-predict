package rvpredict.instrumentation;

import static rvpredict.logging.ReplayWrapper.*;

import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.toolkits.graph.*;
import soot.options.Options;

import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;
import soot.toolkits.graph.CompleteBlockGraph;

import soot.util.Chain;
import soot.util.HashChain;

import soot.jimple.toolkits.annotation.logic.LoopFinder;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.thread.ThreadLocalObjectsAnalysis;
import soot.jimple.toolkits.thread.mhp.SynchObliviousMhpAnalysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import db.DBEngine;

public class ReplayInstrumentor extends SceneTransformer {
  
  private Set<String> sharedvariables;
  
  public ReplayInstrumentor(Set<String> s)
  {
	  this.sharedvariables = s;
  }
 
  public static final String constructorName = "<init>";
  public static final String staticInitializerName = "<clinit>";
  
  private static final SootClass objectClass = Scene.v().loadClassAndSupport("java.lang.Object");

  private static final SootMethod notify = objectClass.getMethod("void notify()");
  private static final SootMethod notifyAll = objectClass.getMethod("void notifyAll()");
  private static final SootMethod wait1 = objectClass.getMethod("void wait()");
  private static final SootMethod wait2 = objectClass.getMethod("void wait(long)");
  private static final SootMethod wait3 = objectClass.getMethod("void wait(long,int)");

  private static final SootMethod start = Scene.v().loadClassAndSupport("java.lang.Thread").getMethod("void start()");
  private static final SootMethod join = Scene.v().loadClassAndSupport("java.lang.Thread").getMethod("void join()");

  protected void internalTransform(String phase, Map options){

    for(SootClass sc : Scene.v().getClasses()){
      String packageName = sc.getPackageName();
      if(packageName.startsWith("java") 
          || packageName.startsWith("com.ning.compress.lzf") 
          || packageName.startsWith("jdbm")
          || packageName.startsWith("rvpredict")){
            //System.out.println("Skipping " + sc.getName());
            continue;
          }
      
      
      //System.out.println("class: " + sc.getName());
      for(SootMethod m : sc.getMethods()){
        if(!m.hasActiveBody() || m.hasTag("NoInstrumentTag")) {
          continue;
        }
        Body body = m.getActiveBody();
        Iterator<Unit> it = body.getUnits().snapshotIterator();
        while(it.hasNext()){
          Stmt stmt = (Stmt)it.next();
         
          //for each statement we apply the following methods until one of them succeeds
          //order is partially important in that markInvocations needs to come after
          //markStarts markConstructors and markLocks. The important thing is these
          //are all disjoint operations 

          //ridiculously, Java will not let you do this without assigning the boolean value to
          //something
          
          boolean unused = markJoin(body,stmt) || markStart(body,stmt) /*markConstructor(stmt) ||*/ 
        		  	|| markLock(body,stmt) || markWaitAndNotify(body,stmt)
        	       || markFieldAccess(body,stmt) || markArrayAccess(body, stmt)
        	       || markSynchronizedMethodCall(body,stmt)
        	        /*|| markReflectAccess(stmt) || markReflectConstructor(stmt) || markReflForName(stmt)*/
        	       /*|| markImpureCall(stmt)*/  /*|| markInvocation(stmt)*/;

          
          //Need to mark basic-block transition
          
          //do static slicing following the basic-block paths
        }
          
      }
    }
  }
  
  private boolean markSynchronizedMethodCall(Body body, Stmt stmt)
  {
	  if(stmt instanceof JInvokeStmt && ((JInvokeStmt)stmt).getInvokeExpr().getMethod().isSynchronized())
	  {
		  		  
		  if(((JInvokeStmt)stmt).getInvokeExpr() instanceof JSpecialInvokeExpr)
		  {
			  body.getUnits().insertBefore(logStaticLock(),stmt);
			  body.getUnits().insertAfter(logStaticUnlock(),stmt);
		  }
		  else
		  {			  
			  
			  body.getUnits().insertBefore(logStaticLock(), stmt);
			  body.getUnits().insertAfter(logStaticUnlock(), stmt);
		  }
		  return true;
	  }
	  
	  return false;
  }
  private boolean markFieldAccess(Body body, Stmt stmt) {
	    if (stmt.containsFieldRef()) {
	      assert (stmt instanceof AssignStmt) : "Unknown FieldReffing Stmt";
	      
	      if(body.getMethod().getName().equals(constructorName)
	    		  ||body.getMethod().getName().equals(staticInitializerName)
	    		  ||sharedvariables.contains(stmt.getFieldRef().getField().getSignature()+"|"+getLineNumber(stmt)))//may add line number later
	      { 

	      InvokeStmt is 
	        = logFieldAcc();
	 	 body.getUnits().insertBefore(is,stmt);
	      return true;
	      }
	    }
	    return false;
	  }
  
  private boolean markArrayAccess(Body body, Stmt stmt) {
	    if (stmt.containsArrayRef()) {
	      assert (stmt instanceof AssignStmt) : "Unknown ArrayReffing Stmt";
	      
	      if(body.getMethod().getName().equals(constructorName)
	    		  ||body.getMethod().getName().equals(staticInitializerName)
	    		  ||sharedvariables.contains( body.getMethod().getSignature()+"|"+stmt.getArrayRef().getBase().toString()+"|"+getLineNumber(stmt)))
	      { 
	    	  
		      InvokeStmt is 
		        = logArrayAcc();
		      body.getUnits().insertBefore(is,stmt);
		      return true;
	      }
	      
	    }
	    return false;
	  }
  //mark waits and notifies.  Unfortunately, part of this is redundant 
  //with marking class inits and marking invocations.  If we had HoFs 
  //I would factor the commonalities out.  With the limited ability of 
  //java 6 I will just repeat code.  This will be factored once java 7
  //goes live and we can use closures
  private boolean markWaitAndNotify(Body body, Stmt stmt){
    if (stmt.containsInvokeExpr()) {
 
      SootMethod method = stmt.getInvokeExpr().getMethod();
      if (method == wait1) {

        InvokeStmt is = logWait();
        body.getUnits().insertBefore(is,stmt);
        return true;
      }
      if (method == wait2 || method == wait3) {
        //body.getUnits().insertAfter(logTimeoutWait(id),stmt);
        return true;
      }
      if(method == notify || method == notifyAll) {

        InvokeStmt is = 
          logNotify();
        body.getUnits().insertBefore(is,stmt);
        return true;
      }
    }
    return false;
  }
  
  // When a lock is aquired an EnterMonitorStmt is encountered. When a lock is released an
  // ExitMonitorStmt is encountered.   
  private boolean markLock(Body body, Stmt stmt) {
	  

    if (stmt instanceof EnterMonitorStmt){

      body.getUnits().insertBefore(logLock(), stmt);
      return true;
    }
    if (stmt instanceof ExitMonitorStmt){
       

      body.getUnits().insertBefore(logUnlock(), stmt);
      return true;
    }
    return false;
  }
  
  private boolean markStart(Body body, Stmt stmt) {
	    if (stmt.containsInvokeExpr()) {
	      if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
	        if (stmt.getInvokeExpr().getMethod() == start) {
	        	
	          body.getUnits().insertBefore(logStart(stmt),stmt);
	          return true;
	        }
	      }
	    }
	    return false;
	  }
  private boolean markJoin(Body body, Stmt stmt) {
	    if (stmt.containsInvokeExpr()) {
	      if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
	        if (stmt.getInvokeExpr().getMethod() == join) {
	        	
	          body.getUnits().insertBefore(logJoin(),stmt);
	          return true;
	        }
	      }
	    }
	    return false;
	  }
  
  private static int getLineNumber(Stmt stmt)
  {
	  Tag tag =  stmt.getTag("LineNumberTag");
	  if(tag==null)
		  return 0;
	  else
		  return ((LineNumberTag)tag).getLineNumber();
  }
}
