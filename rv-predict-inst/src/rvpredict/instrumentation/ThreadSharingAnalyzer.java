package rvpredict.instrumentation;

import rvpredict.thread.mhp.MHPAnalysis;
import rvpredict.thread.mhp.ThreadSharedAccessAnalysis;
import soot.*;
import soot.jimple.*;

import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.toolkits.thread.ThreadLocalObjectsAnalysis;
import soot.jimple.toolkits.thread.mhp.SynchObliviousMhpAnalysis;
import soot.util.queue.QueueReader;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

public class ThreadSharingAnalyzer extends SceneTransformer {
  public ThreadLocalObjectsAnalysis tlo;
  //public ThreadSharedAccessAnalysis tlo;
  
  private HashSet<String> sharedVariableSignatures = new HashSet<String>();
  
 
  public static final String constructorName = "<init>";
  public static final String staticInitializerName = "<clinit>";
  
  protected void internalTransform(String phase, Map options){

      tlo = new ThreadLocalObjectsAnalysis(new SynchObliviousMhpAnalysis());
      
      //tlo = new ThreadSharedAccessAnalysis( new MHPAnalysis());
      
      //For DEBUG
//      ReachableMethods rms = Scene.v().getReachableMethods();
//      QueueReader<MethodOrMethodContext> reader = rms.listener();
//      while(reader.hasNext())
//      {
//    	  MethodOrMethodContext m = reader.next();
//    	  
//    	  System.out.println(m.method().getSignature());
//      }
      
    for(SootClass sc : Scene.v().getClasses()){
      String packageName = sc.getPackageName();
      if(Main.skipPackage(packageName)){
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
         
          if (stmt.containsFieldRef()) {
    	      assert (stmt instanceof AssignStmt) : "Unknown FieldReffing Stmt";
    	      
    	      //WE NEED TO INSTRUMENT CONSTRUCTORS to COLLET THE INITIAL WRITE
//    	      if(!body.getMethod().getName().equals(constructorName)&&
//    	    		  !body.getMethod().getName().equals(staticInitializerName))
    	    {
    	    	  		//check if it is a write
    	    	  boolean write = (((DefinitionStmt)stmt).getLeftOp() instanceof FieldRef);
    	    	  if(write&&!tlo.isObjectThreadLocal(stmt.getFieldRef(), body.getMethod()))
    	    	  {
    	    		  
    	    		  sharedVariableSignatures.add(stmt.getFieldRef().getField().getSignature());
    	    	  }
    	    }
        }
        }
      }
    }
   
  }
  
  public Set<String> getSharedVariableSignatures()
  {
	  return sharedVariableSignatures;
  }

public boolean isShared(String sig) {
	return sharedVariableSignatures.contains(sig);
}
}
