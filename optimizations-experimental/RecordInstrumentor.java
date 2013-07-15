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
import soot.toolkits.graph.MHGPostDominatorsFinder;

import soot.util.Chain;
import soot.util.HashChain;

import soot.jimple.toolkits.annotation.logic.LoopFinder;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.jimple.toolkits.thread.ThreadLocalObjectsAnalysis;
import soot.jimple.toolkits.thread.mhp.SynchObliviousMhpAnalysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import db.DBEngine;

public class RecordInstrumentor extends SceneTransformer {
  private static LoopFinder lf;
  private static MHGPostDominatorsFinder pdf;
  private static HashMap<SootMethod, MHGPostDominatorsFinder> pdfMap 
    = new HashMap<SootMethod, MHGPostDominatorsFinder>();
  //TODO:  Currently, if there are multiple read/write accesses
  //to a given variable used uncondintionally within a loop (transitivelty)
  //we will put all the access at the lop accesses even though we don't really
  //need to know more than just one read/write.  I will fix this if it becomes
  //an issue.  The problem is I keep a set of Stmts when I should probaly
  //design some sort of "access object" that just records the ID and w/r info.
  //It should be noted that this is still correct, it just migght be *slightly*
  //less efficient in some cases (but still way more than not doing the optimization)
  private static HashMap<SootMethod, HashSet<Stmt>> accessesMap
    = new HashMap<SootMethod, HashSet<Stmt>>();
  
  
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
        if(!m.hasActiveBody() /* actually, now we want to instrument cloned methods || m.hasTag("NoInstrumentTag") */) {
          continue;
        }
        Body body = m.getActiveBody();
        lf = new LoopFinder();
        lf.transform(body);
        pdf = new MHGPostDominatorsFinder(new BriefUnitGraph(body));
        
      //    if(m.getName().equals("main")){
       //   System.out.println(body);
        //  }
        
        if(SetupPass.optimizeLoops){ 
          handleLoops(body);
        }
        Iterator<Unit> it = body.getUnits().snapshotIterator();
        while(it.hasNext()){
          Stmt stmt = (Stmt)it.next();
          if(stmt.hasTag("NoInstrumentTag")) continue;
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
        
//          if(m.getName().equals("main")){
  //        System.out.println(body);
    //      }
        //System.out.println(m.getActiveBody());
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
  
  //These methods do special instrumentation
  //for loops in the case where loop optimization
  //is turned on
  // Optimization: if there are no synchronization statements
  //in a loop (and transitively any methods it calls), we can
  //move instrumentation for unconditionally executed accesses
  //to the loop exits.
  private void handleLoops(Body body){
	 for(Loop l : lf.loops()){
		 handleLoop(l, body);
	 }
  }
  
  private void handleLoop(Loop l, Body body){

	 if(!MethodCloner.checkForSynchStmts(l)) { 
		Collection<Stmt> exits = l.getLoopExits();
		Collection<Stmt> targets = new HashSet<Stmt>();
		for(Stmt exit : exits){
			targets.addAll(closeTargets(l.targetsOfLoopExit(exit), body));
		}
		//printLoop(l);
		//System.out.println(targets);
		Iterator<Stmt> it = l.getLoopStatements().iterator();
		it.next(); //this is the loop header which is a branch, we want the target of the branch
		Stmt firstStmt = it.next(); 
		//printLoop(l);
		Stmt loopHead = l.getHead();
		HashSet<Stmt> accesses = new HashSet<Stmt>();
		for(Stmt stmt: l.getLoopStatements()){
			if(pdf.isDominatedBy(firstStmt, stmt)){
              boolean used = markFieldAccess(body, stmt, targets) 
        	       || markArrayAccess(body, stmt, targets)
        	       || markBranch(body, stmt);
              if(!used && stmt instanceof JInvokeStmt){
            	 accesses.addAll(findAccesses((JInvokeStmt) stmt, targets)); 
              }
              stmt.addTag(NoInstrumentTag.v());
			}
		}
		for(Stmt stmt : accesses) {
			boolean unused = markFieldAccess(body, stmt, targets)
					||       markArrayAccess(body, stmt, targets);
		}
	 }
  }

  //Unfortunately the LoopFinder returns targets of loop exist that are just branches
  //immediately to other targets.  This method transitively closes this so that
  //we don't end up with extra instrumentation
  private static Collection<Stmt> closeTargets(Collection<Stmt> targets, Body body){
	  //System.out.println("closeTargets before:" + targets);
	  Collection<Stmt> ret = new HashSet<Stmt>();
	  for(Stmt target : targets){
		 ret.addAll(closeTarget(target, body)); 
	  }
	  //System.out.println("closeTargets after:" + ret);
	  return ret;
  }
  
  private static Collection<Stmt> closeTarget(Stmt target, Body body){
	 Collection<Stmt> workList = new HashSet<Stmt>();
	 Collection<Stmt> wlTmp = new HashSet<Stmt>();
	 Collection<Stmt> ret = new HashSet<Stmt>();
	 
	 workList.add(target);
	 boolean notFinished = true;
	 while(notFinished){
	   notFinished = false;
	   for(Stmt stmt : workList){
         if(stmt.branches()){
           notFinished = true;
    	   if(stmt instanceof GotoStmt){
    	     wlTmp.add((Stmt)((GotoStmt)stmt).getTarget());
    	   }
    	   else if(stmt instanceof IfStmt){
    	     IfStmt is = (IfStmt) stmt; 
    	     wlTmp.add(is.getTarget());
    	     Iterator<Unit> findFallThrough = body.getUnits().iterator(stmt);
    	     findFallThrough.next();
    	     Stmt fallThrough = (Stmt) findFallThrough.next();
    	     wlTmp.add(fallThrough);
    	   }
         }
         else{
    	    ret.add(stmt);
         } 
	   }
	   workList = wlTmp;
	   wlTmp = new HashSet<Stmt>();
	 }
	 return ret;
  }
   
  private static void printLoop(Loop l){
	 for(Stmt s : l.getLoopStatements()){
	   System.out.println(s);	 
	 }
  }
 
  //This finds the accesses in recursively called methods.  Currently, we just
  //save this as Stmts, which can result in say multiple reads to a given
  //variable 'x' appearing at our target instrumentation point.  I will
  //change this if it becomes and issue
  private HashSet<Stmt> findAccesses(JInvokeStmt stmt, Collection<Stmt> targets){
	  InvokeExpr origIE = stmt.getInvokeExpr();
	  //System.out.println(origIE);
	  SootMethod m = MethodCloner.cloneInvokeExprMethodMap.get(origIE);
	  if(m == null){
		  m = origIE.getMethod();
		  if(!m.hasActiveBody()) 
			  return new HashSet();
	  }
	  return findAccesses(m, targets);
  }
  
  private HashSet<Stmt> findAccesses(SootMethod m, Collection<Stmt> targets){
	HashSet<Stmt> ret = accessesMap.get(m);
	if(ret != null){
	  return ret;
	}
	ret = new HashSet<Stmt>();
    Body body = m.getActiveBody();
    
    MHGPostDominatorsFinder lpdf = pdfMap.get(m); 
    if(lpdf == null){
      lpdf = new MHGPostDominatorsFinder(new BriefUnitGraph(body));
      pdfMap.put(m, lpdf);
    }
    Iterator<Unit> it = body.getUnits().snapshotIterator();
    Stmt firstStmt = (Stmt) body.getUnits().getFirst();
	while(it.hasNext()){
	  Stmt stmt = (Stmt) it.next();
		if(lpdf.isDominatedBy(firstStmt, stmt)){
          if(stmt.containsFieldRef() || stmt.containsArrayRef()){
        	 ret.add(stmt); 
          }
          else if(stmt instanceof JInvokeStmt){
            ret.addAll(findAccesses((JInvokeStmt) stmt, targets)); 
          }
          stmt.addTag(NoInstrumentTag.v());
		}
	}
	accessesMap.put(m, ret);
	return ret;
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
			  
			  int sid = getSharedVariableId(expr.getMethod().getDeclaringClass().toString(),stmt);
			  
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
 
  //if insertsAfter is null we know it is not a loop access that we
  //are instrumenting.  Loops accesses need instrumentation points
  //specified
  private boolean markFieldAccess(Body body, final Stmt stmt) {
	  return markFieldAccess(body, stmt, null);
  }
  
  private boolean markFieldAccess(Body body, Stmt stmt, Collection<Stmt> insertsAfter) {
	    if (stmt.containsFieldRef()) {
	      assert (stmt instanceof AssignStmt) : "Unknown FieldReffing Stmt";
	      totalAccess++;
	      
	      //WE NEED TO INSTRUMENT CONSTRUCTORS to COLLET THE INITIAL WRITE
	      if(body.getMethod().getName().equals(constructorName)||
	    		  body.getMethod().getName().equals(staticInitializerName))
	    {
	    	  		//check if it is a write
	    	  boolean write = (((DefinitionStmt)stmt).getLeftOp() instanceof FieldRef);
	    	  if(write&&! SetupPass.tlo.isObjectThreadLocal(stmt.getFieldRef(), body.getMethod()))
	    	  {
		    	  int sid = getSharedVariableId(stmt.getFieldRef().getField().getSignature(),stmt);

		    	  int id = getStaticId(body, stmt);
	    	      if(insertsAfter == null) logFieldAcc(id,sid,body,stmt);
	    	      else logLoopFieldAcc(id,sid,body,stmt, insertsAfter);
			      return true;
	    	  }
	    	  
	    }
	      else if(!SetupPass.tlo.isObjectThreadLocal(stmt.getFieldRef(), body.getMethod()))
	      { 
	    	  sharedAccess++;
	    		  
	    	int sid = getSharedVariableId(stmt.getFieldRef().getField().getSignature(),stmt);
	    	  
	    	int id = getStaticId(body, stmt);

	    	//objectinstance,fieldsignature-id
	    	if(insertsAfter == null) logFieldAcc(id,sid,body,stmt);
	    	else logLoopFieldAcc(id, sid, body, stmt, insertsAfter);
	      //body.getUnits().insertAfter(is,stmt);//for replay, must insert before
	      return true;
	      }
	    }
	    return false;
	  }
  
  //if insertsAfter is null we know it is not a loop access that we
  //are instrumenting.  Loops accesses need instrumentation points
  //specified
  private boolean markArrayAccess(Body body, final Stmt stmt) {
	  return markArrayAccess(body, stmt, null);
  }
  
  private boolean markArrayAccess(Body body, Stmt stmt, Collection<Stmt> insertsAfter) {
	    if (stmt.containsArrayRef()) {
	      assert (stmt instanceof AssignStmt) : "Unknown ArrayReffing Stmt";

	      totalAccess++;
	    
	      //WE NEED TO INSTRUMENT CONSTRUCTORS to COLLET THE INITIAL WRITE
	      if(body.getMethod().getName().equals(constructorName)||
	    		  body.getMethod().getName().equals(staticInitializerName))
	    {
	    	  		//check if it is a write
	    	  boolean write = (((DefinitionStmt)stmt).getLeftOp() instanceof ArrayRef);
	    	  if(write&& !SetupPass.tlo.isObjectThreadLocal(stmt.getFieldRef(), body.getMethod()))
	    	  {
		    	  //we need the following, please don't comment!
		    	  int sid = getSharedVariableId(body.getMethod().getSignature()+"|"+stmt.getArrayRef().getBase().toString(),stmt);

		    	  int id = getStaticId(body, stmt);
		    	  if(insertsAfter == null) logArrayAcc(id,body,stmt);
		    	  else logLoopArrayAcc(id, body, stmt, insertsAfter);
			      return true;
	    	  }
	    	  
	    }
	      else if(!SetupPass.tlo.isObjectThreadLocal(stmt.getArrayRef().getBase(), body.getMethod()))
	      { 
	    	  sharedAccess++;
	    	  //we need the following, please don't comment!
	    	  int sid = getSharedVariableId(body.getMethod().getSignature()+"|"+stmt.getArrayRef().getBase().toString(),stmt);

	    	  int id = getStaticId(body, stmt);
	    	  if(insertsAfter == null) logArrayAcc(id,body,stmt);
	    	  else logLoopArrayAcc(id, body, stmt, insertsAfter);
		      //body.getUnits().insertAfter(is,stmt);
		      return true;
	      }
	      
	    }
	    return false;
	  }
  //mark waits and notifies.  Unfortunately, part of this is redundant 
  //with marking class inits and marking invocations.  If we had HoFs 
  //I would factor the commonalities out.  With the limited ability of 
  //java 6 I will just repeat code.  This will be factored once java 8(!, bastards)
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
