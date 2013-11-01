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

/**
 * ThreadSharingAnalyzer traverses all the field accesses in the application
 * and uses ThreadLocalObjectsAnalysis to determine if an access is to a shared
 * data or not. All shared data signatures are store in sharedVariableSignatures
 * @author jeffhuang
 *
 */
public class ThreadSharingAnalyzer extends SceneTransformer {
  private ThreadLocalObjectsAnalysis tlo;
  //public ThreadSharedAccessAnalysis tlo;
  boolean noSharingAnalysis;
  private HashSet<String> sharedVariableSignatures = new HashSet<String>();
  
 
  public static final String constructorName = "<init>";
  public static final String staticInitializerName = "<clinit>";
  
  public ThreadSharingAnalyzer(boolean nosa) {
	// TODO Auto-generated constructor stub
	  super();
	  noSharingAnalysis = nosa;
}

protected void internalTransform(String phase, Map options){

	if(!noSharingAnalysis)
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
    	    	  if(write&&(noSharingAnalysis
    	    			  || !tlo.isObjectThreadLocal(stmt.getFieldRef(), body.getMethod())))
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
  /**
   * return true if the signature is in sharedVariableAccessSignatures
   * @param sig
   * @param stmt
   * @return
   */
public boolean isShared(String sig) {
	return sharedVariableSignatures.contains(sig);
}
public boolean isShared(Value v, SootMethod sm)
{
	if(noSharingAnalysis) return true;
	else 
		return !tlo.isObjectThreadLocal(v,sm);
}
}
