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
import soot.toolkits.scalar.ArraySparseSet;
import soot.util.Chain;
import soot.util.HashChain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import db.DBEngine;

/**
 * The RecordInstrumentor extends SceneTransformer to instrument the
 * application classes to generate the record version. Shared data accesses
 * and thread synchronizations are instrumented.
 * 
 * @author jeffhuang
 *
 */
public class RecordInstrumentor extends SceneTransformer {
	
/**
 * meta data during transformation:
 * shared variable accesses to id, statement signature to id, etc.
 */
  private int totalAccess;
  private int sharedAccess;
  private HashMap<String,Integer> sharedvariableIdMap = new HashMap<String,Integer>();
  private HashMap<String,Integer> volatilevariableIdMap = new HashMap<String,Integer>();
  private HashSet<String> sharedVariableAccessSignatures = new HashSet<String>();
  
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

  ThreadSharingAnalyzer sharingAnalyzer;
  
  RecordInstrumentor(ThreadSharingAnalyzer sharingAnalyzer)
  {
	  this.sharingAnalyzer = sharingAnalyzer;
  }
  
  protected void internalTransform(String phase, Map options){

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
        
        UnitGraph g = new ExceptionalUnitGraph(body);
        BranchConditionDataFlowAnalysis an = new BranchConditionDataFlowAnalysis(g);
        
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
        	       || markBranch(an,body, stmt)
        	        /*|| markReflectAccess(stmt) || markReflectConstructor(stmt) || markReflForName(stmt)*/
        	       /*|| markImpureCall(stmt)*/  /*|| markInvocation(stmt)*/;

          
          //Need to mark basic-block transition
          
          //do static slicing following the basic-block paths
        }
        
        /* Disable tracking thread execution path for now
         * We will use it later for performing offline symbolic analysis
        
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
         */
      }
    }
  }
  
  public Set<String> getSharedVariableSignatures()
  {
	  return sharedVariableAccessSignatures;
  }
  /**
   * Report percentage of shared data accesses, etc.
   */
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
  /**
   * Save sharedvariableIdMap and stmtSigIdMap to database
   * @param appName
   */
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
    	  
    	  db.saveSharedVarSignatureToDB(sig, id);
    	  
    	  //TODO: include array and lock access signatures
    	  if(sharingAnalyzer.isShared(sig))
    	  {
        	  System.out.println("* ["+id+"] "+sig+" *");

    	  }
      }
      
	  //save volatilevariable - id to database
	  db.createVolatileSignatureTable();
      Iterator<Entry<String,Integer>> volatileIt = volatilevariableIdMap.entrySet().iterator();
      while(volatileIt.hasNext())
      {
    	  Entry<String,Integer> entry = volatileIt.next();
    	  String sig = entry.getKey();
    	  Integer id = entry.getValue();
    	  
    	  db.saveVolatileSignatureToDB(sig, id);
    	  
    	  //TODO: include array and lock access signatures
    	  if(sharingAnalyzer.isShared(sig))
    	  {
        	  System.out.println("* volatile: ["+id+"] "+sig+" *");

    	  }
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
  /**
   * insert instrumentation before and after the synchronized method call
   * @param body
   * @param stmt
   * @return
   */
  private boolean markSynchronizedMethodCall(Body body, Stmt stmt)
  {
	 
	  if((stmt instanceof JInvokeStmt && ((JInvokeStmt)stmt).getInvokeExpr().getMethod().isSynchronized())
			  || (stmt instanceof AssignStmt && ((AssignStmt) stmt).getRightOp() instanceof InvokeExpr 
					  && ((InvokeExpr)((AssignStmt) stmt).getRightOp()).getMethod().isSynchronized()))
	  {
		  InvokeExpr expr;
		  if(stmt instanceof JInvokeStmt)
			  expr = ((JInvokeStmt)stmt).getInvokeExpr();
		  else
			  expr = ((InvokeExpr)((AssignStmt) stmt).getRightOp());
			  
		  int id = getStaticId(body, stmt);
		  if(expr instanceof InstanceInvokeExpr)
		  {
		  body.getUnits().insertBefore(logLock(id,((InstanceInvokeExpr)expr).getBase()), stmt);
		  body.getUnits().insertAfter(logUnlock(id,((InstanceInvokeExpr)expr).getBase()), stmt);
		  }
		  else
		  {
			  
			  //System.out.println("Static Sync: "+stmt);
			  
			  int sid = getSharedVariableId(expr.getMethod().getDeclaringClass().toString(),stmt,false);
			  
			  body.getUnits().insertBefore(logLock(id,sid), stmt);
			  body.getUnits().insertAfter(logUnlock(id,sid), stmt);
		  }
		  return true;
	  }
	  
	  return false;
  }
  /**
   *  insert instrumentation before a branch statement JIfStmt
   * @param body
   * @param stmt
   * @return
   */
  private boolean markBranch(BranchConditionDataFlowAnalysis an, Body body, Stmt stmt) {
	    if (stmt.branches()) {
	    	
	    	if(stmt instanceof JIfStmt)
	    	{
	    		
	    		ArraySparseSet o = (ArraySparseSet) an.getFlowBefore(stmt);
	    		for(Iterator it = o.iterator();it.hasNext();)
	    		{
	    			FieldRef r = (FieldRef) it.next();
	    			String sig = r.getField().getSignature();
	    		      if(sharingAnalyzer.isShared(sig))
	    		      {
	    		    		int id = getStaticId(body, stmt);

	    				      InvokeStmt is = logBranch(id,stmt);
	    			
	    				      body.getUnits().insertBefore(is,stmt);
	    				      return true;
	    		      }
	    		}
	    		

	    	}
	    }
	    return false;
	  }
  /**
   * insert instrumentation before a shared field access.
   * shared writes in constructors are instrumented to track the initial value
   * @param body
   * @param stmt
   * @return
   */
  private boolean markFieldAccess(Body body, Stmt stmt) {
	    if (stmt.containsFieldRef()) {
	      assert (stmt instanceof AssignStmt) : "Unknown FieldReffing Stmt";
	      totalAccess++;
	      SootField sootfield = stmt.getFieldRef().getField();
	      String sig = sootfield.getSignature();
	      if(sharingAnalyzer.isShared(sig))
	      {
	    	  sharedAccess++;
	    	  int sid = getSharedVariableId(sig,stmt,sootfield.isVolatile());	    	  
	    	  int id = getStaticId(body, stmt);
	    	  
	    	  if(body.getMethod().getName().equals(constructorName)||
		    		  body.getMethod().getName().equals(staticInitializerName))
	    	  {
	    		  //log field write in constructors -- initial write
	    		  boolean write = (((DefinitionStmt)stmt).getLeftOp() instanceof FieldRef);
	    		  if(write)logInitialFieldWriteAcc(id,sid,body,stmt);
	    			  
	    	  }
	    	  else
	    	  logFieldAcc(id,sid,body,stmt);
		    	  
	    	  return true;
	    	  
	      }
	    }
		return false;

  }
  /**
   * add instrumentation before shared array accesses, similar to markFieldAccess
   * @param body
   * @param stmt
   * @return
   */
  private boolean markArrayAccess(Body body, Stmt stmt) {
	    if (stmt.containsArrayRef()) {
	      assert (stmt instanceof AssignStmt) : "Unknown ArrayReffing Stmt";

	      totalAccess++;
	    
	      if(sharingAnalyzer.isShared(stmt.getArrayRef().getBase(), body.getMethod()))
	      { 
	    	  sharedAccess++;
	    	  //we need the following, please don't comment!
	    	  int sid = getSharedVariableId(body.getMethod().getSignature()+"|"+stmt.getArrayRef().getBase().toString(),stmt,false);

	    	  int id = getStaticId(body, stmt);
	    	  
	    	  
	    	  if(body.getMethod().getName().equals(constructorName)||
		    		  body.getMethod().getName().equals(staticInitializerName))
	    	  {
	    		  boolean write = (((DefinitionStmt)stmt).getLeftOp() instanceof ArrayRef);
		    	  if(write)logInitialArrayWriteAcc(id,body,stmt);
		    	  
	    	  }
	    	  else
	    	  {
		      //handle array index here
		    	 //if the index variable is a non-constant
		    	if(!(stmt.getArrayRef().getIndex() instanceof Constant))
		    	{    
			      InvokeStmt is = logBranch(id,stmt);
			      body.getUnits().insertBefore(is,stmt);
		    	} 
		      
		    	logArrayAcc(id,body,stmt);
		    	//body.getUnits().insertAfter(is,stmt);
		      
	    	  }
		      return true;

	      }
	      
	    }
	    return false;
	  }
  
  /**
   * mark waits and notifies.
   * Instrumentations are inserted after wait and before notify
   * @param body
   * @param stmt
   * @return
   */
  //Unfortunately, part of this is redundant 
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
  
  /**
   *  Add instrumentation when a lock is aquired an EnterMonitorStmt is encountered.
   *  When a lock is released an ExitMonitorStmt is encountered
   * @param body
   * @param stmt
   * @return
   */ 
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
  
  /**
   * add instrumentation before start a Thread object
   * @param body
   * @param stmt
   * @return
   */
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
  
  /**
   * Add instrumentation after joining a Thread object
   * @param body
   * @param stmt
   * @return
   */
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
  /**
   * Retrieve the ID of the statement from stmtSigIdMap
   * @param body
   * @param stmt
   * @return
   */
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
  /**
   * Get line number of the given statement
   * @param stmt
   * @return
   */
  private static int getLineNumber(Stmt stmt)
  {
	  Tag tag =  stmt.getTag("LineNumberTag");
	  if(tag==null)
		  return 0;
	  else
		  return ((LineNumberTag)tag).getLineNumber();
  }
  
  /**
   * Retrieve the ID of the shared variable signature
   * sharedVariableAccessSignatures
   * @param sig
   * @param stmt
   * @return
   */
  private int getSharedVariableId(String sig, Stmt stmt, boolean isVolatile)
  {
	  sharedVariableAccessSignatures.add(sig+"|"+getLineNumber(stmt));
	  
	  
	  
	  if(sharedvariableIdMap.get(sig)==null)
	  {
		  sharedvariableIdMap.put(sig, sharedvariableIdMap.size()+1);
	  }
	  int sid = sharedvariableIdMap.get(sig);
	  
	  if(isVolatile)
	  {
		  volatilevariableIdMap.put(sig, sid);
	  }
	  
	  return sid;
	  
  }
}
