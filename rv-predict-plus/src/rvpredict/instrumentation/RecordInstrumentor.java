package rvpredict.instrumentation;

import static rvpredict.logging.RecordWrapper.*;

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

public class RecordInstrumentor extends SceneTransformer {
  private static ThreadLocalObjectsAnalysis tlo;
  
  private int totalAccess;
  private int sharedAccess;
  private HashMap<String,Integer> sharedvariableIdMap = new HashMap<String,Integer>();
  private HashSet<String> sharedVariableSignatures = new HashSet<String>();
  
  private static HashMap<String,Integer> stmtSigIdMap = new HashMap<String,Integer>();
 
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
    //if(true) return;
    //System.out.println("running");

      tlo = new ThreadLocalObjectsAnalysis(new SynchObliviousMhpAnalysis());

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
        	       || markFieldAccess(body,stmt) 
        	       || markArrayAccess(body, stmt)
        	       || markSynchronizedMethodCall(body,stmt)
        	       || markBranch(body, stmt)
        	        /*|| markReflectAccess(stmt) || markReflectConstructor(stmt) || markReflForName(stmt)*/
        	       /*|| markImpureCall(stmt)*/  /*|| markInvocation(stmt)*/;

          
          //Need to mark basic-block transition
          
          //do static slicing following the basic-block paths
        }
        
        CompleteBlockGraph cfg = new CompleteBlockGraph(body);
        Iterator<Block> cfgIt = cfg.iterator();
        while(cfgIt.hasNext())
        {
        	Block block = cfgIt.next();
        	Unit unit = block.getHead();
        	while(unit instanceof IdentityStmt)
        	{
        		unit = block.getSuccOf(unit);
        	}
        	if(unit!=null)
        	{
	        	Stmt stmt = (Stmt)unit;
	        	
	        	int id = getStaticId(body, stmt);
	
	  	      	InvokeStmt is = logBasicBlock(id,stmt);
	  	      	body.getUnits().insertBefore(is,stmt);
        	}
        }
      }
    }
  }
  
  public Set<String> getSharedVariableSignatures()
  {
	  return sharedVariableSignatures;
  }
  public void reportStatistics()
  {
	  float percentage = ((float)sharedAccess)/((float)totalAccess);
      System.out.println("* Shared data access percentage: " + sharedAccess + "/" + totalAccess +" ["+percentage+"] *");
//      Iterator<String> svIt = sharedvariableIdMap.keySet().iterator();
//      while(svIt.hasNext())
//      {
//    	  System.out.println("* "+svIt.next()+" *");
//      }
  }
  
  public void saveMetaToDB(String appName)
  {
	  try{
	  DBEngine db = new DBEngine(appName);
	  
	  //save sharedvariable - id to database
	  db.createSharedVarSignatureTable();
      Iterator<Entry<String,Integer>> svIt = sharedvariableIdMap.entrySet().iterator();
      while(svIt.hasNext())
      {
    	  Entry<String,Integer> entry = svIt.next();
    	  String sig = entry.getKey();
    	  Integer id = entry.getValue();
    	  
    	  System.out.println("* ["+id+"] "+sig+" *");
    	  db.saveSharedVarSignatureToDB(sig, id);
      }
	  
      //save stmt - id to database
	  db.createStmtSignatureTable();

      Iterator<Entry<String,Integer>> sigIdIt = stmtSigIdMap.entrySet().iterator();
      while(sigIdIt.hasNext())
      {
    	  Entry<String,Integer> entry = sigIdIt.next();
    	  String sig = entry.getKey();
    	  Integer id = entry.getValue();
    	  
    	  //System.out.println("* ["+id+"] "+sig+" *");
    	  db.saveStmtSignatureToDB(sig, id);
      }
      
      db.closeDB();
      
	  }catch(Exception e)
	  {
		  e.printStackTrace();
	  }
	  
  }
  
  private boolean markSynchronizedMethodCall(Body body, Stmt stmt)
  {
	  if(stmt instanceof JInvokeStmt && ((JInvokeStmt)stmt).getInvokeExpr().getMethod().isSynchronized())
	  {
		  		  
		  int id = getStaticId(body, stmt);
		  if(((JInvokeStmt)stmt).getInvokeExpr() instanceof JSpecialInvokeExpr)
		  {
		  body.getUnits().insertBefore(logLock(id,((JSpecialInvokeExpr)((JInvokeStmt)stmt).getInvokeExpr()).getBase()), stmt);
		  body.getUnits().insertAfter(logUnlock(id,((JSpecialInvokeExpr)((JInvokeStmt)stmt).getInvokeExpr()).getBase()), stmt);
		  }
		  else
		  {
			  
			  //System.out.println("Static Sync: "+stmt);
			  
			  int sid = getSharedVariableId(((AbstractInvokeExpr)stmt.getInvokeExpr()).getClass().toString(),stmt);
			  
			  body.getUnits().insertBefore(logLock(id,sid), stmt);
			  body.getUnits().insertAfter(logUnlock(id,sid), stmt);
		  }
		  return true;
	  }
	  
	  return false;
  }
  private boolean markBranch(Body body, Stmt stmt) {
	    if (stmt.branches()) {
	    	
	    	int id = getStaticId(body, stmt);

	      InvokeStmt is = logBranch(id,stmt);

	      body.getUnits().insertBefore(is,stmt);
	      return true;
	    }
	    return false;
	  }
  private boolean markFieldAccess(Body body, Stmt stmt) {
	    if (stmt.containsFieldRef()) {
	      assert (stmt instanceof AssignStmt) : "Unknown FieldReffing Stmt";
	      totalAccess++;
	      
	      if(body.getMethod().getName().equals(constructorName)
	    		  ||body.getMethod().getName().equals(staticInitializerName)
	    		  ||!tlo.isObjectThreadLocal(stmt.getFieldRef(), body.getMethod()))
	      { 
	    	  sharedAccess++;

	    		  
	    	  int sid = getSharedVariableId(stmt.getFieldRef().getField().getSignature(),stmt);
	    	  
	    	int id = getStaticId(body, stmt);

	    	//objectinstance,fieldsignature-id
	      InvokeStmt is 
	        = logFieldAcc(id,sid,body,stmt);
	      //body.getUnits().insertAfter(is,stmt);//for replay, must insert before
	      return true;
	      }
	    }
	    return false;
	  }
  
  private boolean markArrayAccess(Body body, Stmt stmt) {
	    if (stmt.containsArrayRef()) {
	      assert (stmt instanceof AssignStmt) : "Unknown ArrayReffing Stmt";

	      totalAccess++;
	      
	      if(body.getMethod().getName().equals(constructorName)
	    		  ||body.getMethod().getName().equals(staticInitializerName)
	    		  ||!tlo.isObjectThreadLocal(stmt.getArrayRef().getBase(), body.getMethod()))
	      { 
	    	  sharedAccess++;
	    	  int sid = getSharedVariableId(body.getMethod().getSignature()+"|"+stmt.getArrayRef().getBase().toString(),stmt);

	    	  int id = getStaticId(body, stmt);
		      InvokeStmt is 
		        = logArrayAcc(id,body,stmt);
		      //body.getUnits().insertAfter(is,stmt);
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
      	int id = getStaticId(body, stmt);

        InvokeStmt is = logWait(id,stmt);
        body.getUnits().insertAfter(is,stmt);
        return true;
      }
      if (method == wait2 || method == wait3) {
        //body.getUnits().insertAfter(logTimeoutWait(id),stmt);
        return true;
      }
      if(method == notify || method == notifyAll) {
      	int id = getStaticId(body, stmt);

        InvokeStmt is = 
          logNotify(id,stmt);
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
        int id = getStaticId(body, stmt);

      body.getUnits().insertAfter(logLock(id,stmt), stmt);
      return true;
    }
    if (stmt instanceof ExitMonitorStmt){
        int id = getStaticId(body, stmt);

      body.getUnits().insertBefore(logUnlock(id,stmt), stmt);
      return true;
    }
    return false;
  }
  
  private boolean markStart(Body body, Stmt stmt) {
	    if (stmt.containsInvokeExpr()) {
	      if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
	        if (stmt.getInvokeExpr().getMethod() == start) {
	        	
	        	int id = getStaticId(body, stmt);
	          body.getUnits().insertBefore(logStart(id,stmt),stmt);
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
	        	
	        int id = getStaticId(body, stmt);
	          body.getUnits().insertAfter(logJoin(id,stmt),stmt);
	          return true;
	        }
	      }
	    }
	    return false;
	  }
  
  private static int getStaticId(Body body, Stmt stmt)
  {
	  String methodname = body.getMethod().getSignature();
	  
	  String sig = methodname+"|"+stmt+"|"+getLineNumber(stmt);
	  if(stmtSigIdMap.get(sig)==null)
	  {
		  stmtSigIdMap.put(sig, stmtSigIdMap.size()+1);
	  }
	  
	  return stmtSigIdMap.get(sig);
	  
  }
  private static int getLineNumber(Stmt stmt)
  {
	  Tag tag =  stmt.getTag("LineNumberTag");
	  if(tag==null)
		  return 0;
	  else
		  return ((LineNumberTag)tag).getLineNumber();
  }
  
  private int getSharedVariableId(String sig, Stmt stmt)
  {
	  sharedVariableSignatures.add(sig+"|"+getLineNumber(stmt));
	  
	  if(sharedvariableIdMap.get(sig)==null)
	  {
		  sharedvariableIdMap.put(sig, sharedvariableIdMap.size()+1);
	  }
	  
	  return sharedvariableIdMap.get(sig);
	  
  }
}
