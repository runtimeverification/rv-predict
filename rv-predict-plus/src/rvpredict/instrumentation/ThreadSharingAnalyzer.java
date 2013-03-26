package rvpredict.instrumentation;

import static rvpredict.logging.NewWrapper.*;

import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;

import soot.options.Options;

import soot.tagkit.Tag;

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
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

public class ThreadSharingAnalyzer extends SceneTransformer {
  private static ThreadLocalObjectsAnalysis tlo;

  private static HashMap<String,Integer> sigIdMap = new HashMap<String,Integer>();
  
  private static final SootMethod join = Scene.v().loadClassAndSupport("java.lang.Thread").getMethod("void join()");

  protected void internalTransform(String phase, Map options){
    //if(true) return;
    //System.out.println("running");

    for(SootClass sc : Scene.v().getClasses()){
      String packageName = sc.getPackageName();
      if(packageName.startsWith("java") 
          || packageName.startsWith("com.ning.compress.lzf") 
          || packageName.startsWith("jdbm")
          || packageName.startsWith("rvpredict")){
            //System.out.println("Skipping " + sc.getName());
            continue;
          }
      
      tlo = new ThreadLocalObjectsAnalysis(new SynchObliviousMhpAnalysis());
      
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
          
//          boolean unused = markJoin(body,stmt) || markStart(stmt) || /*markConstructor(stmt) ||*/ markLock(stmt) || markWaitAndNotify(stmt)
//        	       || markFieldAccess(stmt) || markArrayAccess(stmt) || /*markReflectAccess(stmt) || markReflectConstructor(stmt) || markReflForName(stmt)*/
//        	       /*|| markImpureCall(stmt)*/ || markBranch(stmt) || markInvocation(stmt);

        }
      }
    }
  }

  private boolean markJoin(Body body, Stmt stmt) {
	    if (stmt.containsInvokeExpr()) {
	      if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
	        if (stmt.getInvokeExpr().getMethod() == join) {
	        	
	        int id = getStaticId(stmt);
	        	
//	          body.getUnits().insertAfter(logJoin(id,stmt),stmt);
	          return true;
	        }
	      }
	    }
	    return false;
	  }
  
  private static int getStaticId(Stmt stmt)
  {
	  String sig = getStmtSignature(stmt);
	  if(sigIdMap.get(sig)==null)
	  {
		  sigIdMap.put(sig, sigIdMap.size()+1);
	  }
	  
	  return sigIdMap.get(sig);
	  
  }

  private static String getStmtSignature(Stmt stmt) {
	
	  
	return null;
  }
}
