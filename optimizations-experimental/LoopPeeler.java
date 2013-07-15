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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

public class LoopPeeler extends SceneTransformer {
  private static SootClass cloneClass;

  protected void internalTransform(String phase, Map options){
    //if(true) return;
    //System.out.println("running");
 //   if(true) return;
    //super.internalTransform(body, phase, options);
    addCloneClass();
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
        LoopFinder lf = new LoopFinder();
        lf.transform(body);
        //System.out.println("--method: " + m);
        for(Loop l : lf.loops()){
          handleLoop(l, body);
        }
      }
    }
  }

  private static void handleLoop(Loop l, Body body){
    if(isExceptionStmt(l.getHead())) return;
    Chain<Unit> loopClone = new HashChain<Unit>();
    //this will map all stmts to their clones so that
    //we can properly patch branches at the end.
    //That is, branches in the peeled iteration need
    //to go into the peeled iteration, not the original
    //loop
    Map<Unit, Unit> stmt2clone = new HashMap<Unit, Unit>();
    Stmt backJump = l.getBackJumpStmt();
    //System.out.println(l.getLoopStatements());
      //if there are any exceptions in the loop we
      //are punting.  The LoopFinder thinks that
      //exception labels are loops in some cases,
      //and it's too much work to figure out with
      //better granularity
    if(checkExceptions(l)) return;
    Set<Stmt> loopStmts = new HashSet<Stmt>();
    loopStmts.addAll(l.getLoopStatements());
    Iterator<Unit> it = body.getUnits().iterator(l.getHead());
    Stmt s = (Stmt) it.next();
    for(;it.hasNext(); s = (Stmt) it.next()){
      if(!loopStmts.contains(s)) break; //loop statements are not in program
                                        //order, so we have to hack this using
                                        //a set :(
      if(s != backJump) {  //we do not want to copy the back jump
                           //or we'll create a loop copy rather than
                           //one peeled iteration!
        Unit u = (Unit) s.clone();
        stmt2clone.put(s,u);
        loopClone.add(u);
      }
      // if this is an invokeStmt we need to (potentially) clone the method
      // so that we can create an uninstrumented version.  All of the
      // original invoke statements need to call the clone.  The peeled
      // statement still calls the original method that will be instrumented
      // because the cloning happens before we replace the invoked method
      if(s instanceof JInvokeStmt){ 
         handleInvokeStmt((JInvokeStmt) s);
      } 
      //this tag will be used by the instrumentor to ensure that
      //the original statements are not instrumented
      s.addTag(NoInstrumentTag.v());
    }
    //patch up map refs
    //this method will replace all jumps to original statement
    //'s' to its stmt2clone specified clone
    patchMapRefs(loopClone, stmt2clone);
    //insert the loopClone before the loop header
    Chain<Unit> bodyUnits = body.getUnits();
    bodyUnits.insertAfter(loopClone, 
    bodyUnits.getPredOf(l.getHead()));
    //System.out.println("PEELED " + loopStmts);
  }

  //This creates a class with no default constructor.
  //I didn't think java would even allow this!
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

  private static boolean isExceptionStmt(Stmt stmt){
    if(stmt instanceof JIdentityStmt){
      JIdentityStmt id = (JIdentityStmt) stmt;
      if(id.getRightOp() instanceof JCaughtExceptionRef){
        //System.out.println("caught exception");
        return true;
      }
    }
    return false;
  }

  private static boolean checkExceptions(Loop l){
    for(Stmt s : l.getLoopStatements()){
      if(isExceptionStmt(s)) { 
        //System.out.println("no peel " + l.getLoopStatements()); 
        return true; }
    }
    return false;
  }

  private static void patchMapRefs(Chain<Unit> loopClone, Map<Unit, Unit> stmt2clone){
    for(Unit u : loopClone){
      if(u instanceof JGotoStmt){
        JGotoStmt jgoto = (JGotoStmt) u;
        Unit target = jgoto.getTarget();
        Unit clone = stmt2clone.get(target);
        if(clone != null){
          //System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!! replacing " + jgoto + " with ");
          jgoto.setTarget(clone);
          //System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!! replacing goto " + jgoto);
        }
      }
      else if(u instanceof JIfStmt){
        JIfStmt jif = (JIfStmt) u;
        Unit target = jif.getTarget();
        Unit clone = stmt2clone.get(target);
        if(clone != null){
          //System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!! replacing " + jif + " with ");
          jif.setTarget(clone);
          //System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!! replacing if " + jif);
        }
      }
    }
  }

  @SuppressWarnings(value = "unchecked")
  private static InvokeExpr createUninstrumentableCloneInvoke(InvokeExpr ie, 
                                                              SootMethod cloneMethod){
    InvokeExpr ret;
    if(ie instanceof JDynamicInvokeExpr){
      JDynamicInvokeExpr die = (JDynamicInvokeExpr) ie;
      SootMethod cloneBootStrapMethod 
        = createUninstrumentableCloneMethod(die.getBootstrapMethod());
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
    return ret;
  }

  public static void handleInvokeStmt(JInvokeStmt jis){
    InvokeExpr origIE = jis.getInvokeExpr();
    SootMethod origMethod  = origIE.getMethod();
    SootMethod cloneMethod = createUninstrumentableCloneMethod(origMethod);
    if(cloneMethod.hasTag("NoInstrumentTag")){
      InvokeExpr cloneIE 
= createUninstrumentableCloneInvoke(origIE, cloneMethod);
      jis.setInvokeExpr(cloneIE);
    }
  }

  @SuppressWarnings(value = "unchecked")
  public static SootMethod createUninstrumentableCloneMethod(SootMethod m){
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

    return ret;
  }
}
