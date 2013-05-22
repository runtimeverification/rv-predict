package rvpredict.instrumentation;

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

public class MethodCloner extends SceneTransformer {

	private static SootClass cloneClass;
	private static LoopFinder lf;
	
    public static HashMap<SootMethod, SootMethod> cloneMethodMap = new HashMap<SootMethod, SootMethod>();
    
    public static HashMap<InvokeExpr, SootMethod> cloneInvokeExprMethodMap = new HashMap<InvokeExpr, SootMethod>();

    private static HashMap<SootMethod, Boolean> unsynchMethodMap = new HashMap<SootMethod, Boolean>();
    
    private static final SootClass objectClass = Scene.v().loadClassAndSupport("java.lang.Object");
    private static final SootMethod notify = objectClass.getMethod("void notify()");
    private static final SootMethod notifyAll = objectClass.getMethod("void notifyAll()");
    private static final SootMethod wait1 = objectClass.getMethod("void wait()");

    private static final SootMethod start = Scene.v().loadClassAndSupport("java.lang.Thread").getMethod("void start()");
    private static final SootMethod join = Scene.v().loadClassAndSupport("java.lang.Thread").getMethod("void join()");
	
	private static void addCloneClass(){
	    long i = 0;
	    String cloneClassName = "RVPredictCloneClass";
	    //the odds of a class named RVPredictCloneClass0
	    //existing are extremely low, but this makes this
	    //robust
	    while(Scene.v().containsClass(cloneClassName + i)){++i;}
	    cloneClass = new SootClass(cloneClassName + i, Modifier.PUBLIC);
	    cloneClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
	    Scene.v().addClass(cloneClass); 
	    try{
	      String fileName 
	      = SourceLocator.v().getFileNameFor(cloneClass, Options.output_format_class);
	      java.io.OutputStream streamOut 
	      = new soot.util.JasminOutputStream(new java.io.FileOutputStream(fileName));
	      java.io.PrintWriter writerOut 
	      = new java.io.PrintWriter(new java.io.OutputStreamWriter(streamOut));
	      JasminClass jasminClass = new soot.jimple.JasminClass(cloneClass);
	      jasminClass.print(writerOut);
	      writerOut.flush();
	      writerOut.close();
	    }catch(Exception e){
	      throw new RuntimeException(e);  //checked exceptions are dumb
	    }
     }
	
	 private void checkLoopsAndCloneMethods(Body body){
		 for(Loop l : lf.loops()){
		    checkLoopAndCloneMethods(l, body);
		 }
	  }
	  
      private static boolean isSynchStmt(Stmt stmt){
     	if(stmt instanceof EnterMonitorStmt) {
	    	return true;
	    }
	    if(stmt instanceof ExitMonitorStmt){
	    	return true;
	    }
	    if (stmt.containsInvokeExpr()) {
 
            SootMethod method = stmt.getInvokeExpr().getMethod();
            if (       method.isSynchronized() || method == notify 
    		        || method == notifyAll     || method == wait1  
    		        || method == start         || method == join) {
    	        return true;
            }
	   }
	   return false;
     }
	 
	 public static boolean checkForSynchStmts(Loop l){
		 for(Stmt s : l.getLoopStatements()){
			if(isSynchStmt(s)) return true; 
			else if(s instanceof JInvokeStmt){
			  InvokeExpr ie = ((JInvokeStmt)s).getInvokeExpr();
			  SootMethod m = ie.getMethod();
			  if(checkForSynchStmts(m)) {
				  return true;
			  }
			}
		 }
	    // System.out.println("%%%%%No Synch in Loop%%%%%%%");
	    // printLoop(l);
		 return false;
	  }
	  
	  public static boolean checkForSynchStmts(SootMethod m){
		 Boolean b = unsynchMethodMap.get(m);
		 if(b != null){
		   return b;
		 }
		 //if we don't know the code, be conservative? whatever
		 if(!m.hasActiveBody()) {
			 unsynchMethodMap.put(m, true);
			 return true; 
		 }
		 Body body = m.getActiveBody();
	     Iterator<Unit> it = body.getUnits().snapshotIterator();
	     while(it.hasNext()){
	       Unit u = it.next();
	       if(u instanceof Stmt){
	    	 Stmt s = (Stmt) u;
	    	 if(isSynchStmt(s)) {
	    		 unsynchMethodMap.put(m, true);
	    		 return true;
	    	 }
	       }
	     }
	  //   System.out.println("%%%%%No Synch in Body%%%%%%\n" + body);
	     unsynchMethodMap.put(m, false);
		 return false;
	  }	 
	 
	  private void checkLoopAndCloneMethods(Loop l, Body body){
		if(!checkForSynchStmts(l)){ 
		  for(Stmt stmt: l.getLoopStatements()){
			  if(stmt instanceof JInvokeStmt){
				  handleInvokeStmt((JInvokeStmt) stmt);
			  }
		  }
		}
	  }
	  
	protected void internalTransform(String phase, Map options){
       addCloneClass();
       
      for(SootClass sc : Scene.v().getClasses()){
        String packageName = sc.getPackageName();
        if(   packageName.startsWith("java") 
           || packageName.startsWith("com.ning.compress.lzf") 
           || packageName.startsWith("jdbm")
           || packageName.startsWith("rvpredict")){
            //System.out.println("Skipping " + sc.getName());
            continue;
         }
         for(SootMethod m : sc.getMethods()){
           if(!m.hasActiveBody()) {
             continue;
           }
           Body body = m.getActiveBody();
           lf = new LoopFinder();
           lf.transform(body);
           
           checkLoopsAndCloneMethods(body);
         }
       }
	}
	
	@SuppressWarnings(value = "unchecked")
	  private static SootMethod createCloneMethod(SootMethod m){
	    if(!m.hasActiveBody()) return m;
	    String name         = m.getName();
	    //if(name.equals("<init>")) return m;
	           name         = name;
	    List parameterTypes = new ArrayList();
	    parameterTypes.addAll(m.getParameterTypes());
	    parameterTypes.add(cloneClass.getType());
	    Type returnType     = m.getReturnType();
	    int modifiers       = m.getModifiers();
	    List<SootClass> thrownExceptions
	                        = m.getExceptions();

	    SootClass declClass = m.getDeclaringClass();
	    if(declClass.declaresMethod(name, parameterTypes)){
	      return declClass.getMethod(name, parameterTypes);
	    }  
	    String packageName = declClass.getPackageName();

	    if(packageName.startsWith("java")
	       || packageName.startsWith("sun") 
	       || packageName.startsWith("com.ning.compress.lzf") 
	       || packageName.startsWith("jdbm")
	       || packageName.startsWith("rvpredict")){
	          //System.out.println("No Clone " + m + " in package " + packageName);
	          return m;
	    }

	    SootMethod ret = new SootMethod(name, parameterTypes, 
	                        returnType, modifiers, thrownExceptions);     
	    m.getDeclaringClass().addMethod(ret);
	    Body body = (Body)m.getActiveBody().clone();
	    ret.setActiveBody(body);
	    ret.setPhantom(m.isPhantom());
	    ret.setSource(m.getSource());
	    ret.addTag(NoInstrumentTag.v());

	    //we must clone any methods called by the cloned method
	    //(derp to me)
	    Iterator<Unit> it = body.getUnits().iterator();
	    Stmt s = (Stmt) it.next();
	    for(;it.hasNext(); s = (Stmt) it.next()){
	      if(s instanceof JInvokeStmt){ 
	        handleInvokeStmt((JInvokeStmt) s);
	      } 
	    }

	    cloneMethodMap.put(m, ret);
	    return ret;
	  }
	  
	  private static void handleInvokeStmt(JInvokeStmt jis){
		    InvokeExpr origIE = jis.getInvokeExpr();
		    SootMethod origMethod  = origIE.getMethod();
		    SootMethod cloneMethod = createCloneMethod(origMethod);
		    if(cloneMethod.hasTag("NoInstrumentTag")){
		      InvokeExpr cloneIE 
		= createCloneInvoke(origIE, cloneMethod);
		      jis.setInvokeExpr(cloneIE);
		    }
		  }
	  
	  
  private static InvokeExpr createCloneInvoke(InvokeExpr ie){
	return createCloneInvoke(ie, cloneMethodMap.get(ie.getMethod()));
  }
	  
  @SuppressWarnings(value = "unchecked")
  private static InvokeExpr createCloneInvoke(InvokeExpr ie, SootMethod cloneMethod){
    InvokeExpr ret;
    if(ie instanceof JDynamicInvokeExpr){
      JDynamicInvokeExpr die = (JDynamicInvokeExpr) ie;
      SootMethod cloneBootStrapMethod 
        = createCloneMethod(die.getBootstrapMethod());
      List<Value> bootStrapArgs = die.getBootstrapArgs();
      List<Value> args = die.getArgs();
      if(cloneBootStrapMethod.hasTag("NoInstrumentTag")){
        bootStrapArgs.add(NullConstant.v());
      }
      args.add(NullConstant.v());
      ret = new JDynamicInvokeExpr(cloneBootStrapMethod.makeRef(),
                                   bootStrapArgs,
                                   cloneMethod.makeRef(),
                                   args);
    }
    else if(ie instanceof JInterfaceInvokeExpr){
      JInterfaceInvokeExpr die = (JInterfaceInvokeExpr) ie;
      List<Value> args = die.getArgs();
      args.add(NullConstant.v());
      ret = new JInterfaceInvokeExpr(die.getBase(),
                                     cloneMethod.makeRef(),
                                     args);
    }
    else if(ie instanceof JSpecialInvokeExpr){
      JSpecialInvokeExpr die = (JSpecialInvokeExpr) ie;
      List<Value> args = die.getArgs();
      args.add(NullConstant.v());
      ret = new JSpecialInvokeExpr((Local) die.getBase(),
                                   cloneMethod.makeRef(),
                                   args);
    }
    else if(ie instanceof JStaticInvokeExpr){
      JStaticInvokeExpr die = (JStaticInvokeExpr) ie;
      List<Value> args = die.getArgs();
      args.add(NullConstant.v());
      ret = new JStaticInvokeExpr(cloneMethod.makeRef(),
                                  args);
    }
    else if(ie instanceof JVirtualInvokeExpr){
      JVirtualInvokeExpr die = (JVirtualInvokeExpr) ie;
      List<Value> args = die.getArgs();
      args.add(NullConstant.v());
      ret = new JVirtualInvokeExpr(die.getBase(),
                                   cloneMethod.makeRef(),
                                   args);
    }
    else {
      throw new RuntimeException("Unknown InvokeExpr type!");
    }
    ret.setMethodRef(cloneMethod.makeRef());
    cloneInvokeExprMethodMap.put(ret, cloneMethod);
    return ret;
  }
}
