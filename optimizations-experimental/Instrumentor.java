package rvpredict.instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import rvpredict.prediction.PredictorOptions;
import rvpredict.util.Util;
import static rvpredict.logging.Wrapper.*;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Trap;
import soot.Unit;
import soot.Value;
import soot.Transform;
import soot.BodyTransformer;
import soot.Local;
import soot.RefType;
import soot.Type;
import soot.BooleanType;

import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.BreakpointStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.FieldRef;
import soot.jimple.GotoStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.NopStmt;
import soot.jimple.RetStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.Stmt;
import soot.jimple.StmtSwitch;
import soot.jimple.StringConstant;
import soot.jimple.TableSwitchStmt;
import soot.jimple.ThrowStmt;

import soot.javaToJimple.LocalGenerator;
import soot.jimple.internal.JAssignStmt;

import soot.jimple.toolkits.invoke.SynchronizerManager;

import soot.util.Chain;


public class Instrumentor extends BodyTransformer {
  private JimpleBody body;
  private List<Stmt> returnStmts;
  private Stmt lastIdentityStmt;
  private Map<InvokeStmt, Stmt> lineNumberPatchMap = new HashMap<InvokeStmt,Stmt>();
  private static final SootClass objectClass = Scene.v().loadClassAndSupport("java.lang.Object");
  private static final SootMethod notify = objectClass.getMethod("void notify()");
  private static final SootMethod notifyAll = objectClass.getMethod("void notifyAll()");
  private static final SootMethod wait1 = objectClass.getMethod("void wait()");
  private static final SootMethod wait2 = objectClass.getMethod("void wait(long)");
  private static final SootMethod wait3 = objectClass.getMethod("void wait(long,int)");
  private static final SootMethod start = Scene.v().loadClassAndSupport("java.lang.Thread").getMethod("void start()");
  private static final SootMethod join = Scene.v().loadClassAndSupport("java.lang.Thread").getMethod("void join()");
  private static final SootMethod fieldGetObj = Scene.v().loadClassAndSupport("java.lang.reflect.Field").getMethod("java.lang.Object get(java.lang.Object)");
  private static final SootMethod fieldGetBool = Scene.v().loadClassAndSupport("java.lang.reflect.Field").getMethod("boolean getBoolean(java.lang.Object)");
  private static final SootMethod fieldGetByte = Scene.v().loadClassAndSupport("java.lang.reflect.Field").getMethod("byte getByte(java.lang.Object)");
  private static final SootMethod fieldGetChar = Scene.v().loadClassAndSupport("java.lang.reflect.Field").getMethod("char getChar(java.lang.Object)");
  private static final SootMethod fieldGetDouble = Scene.v().loadClassAndSupport("java.lang.reflect.Field").getMethod("double getDouble(java.lang.Object)");
  private static final SootMethod fieldGetFloat = Scene.v().loadClassAndSupport("java.lang.reflect.Field").getMethod("float getFloat(java.lang.Object)");
  private static final SootMethod fieldGetInt = Scene.v().loadClassAndSupport("java.lang.reflect.Field").getMethod("int getInt(java.lang.Object)");
  private static final SootMethod fieldGetLong = Scene.v().loadClassAndSupport("java.lang.reflect.Field").getMethod("long getLong(java.lang.Object)");
  private static final SootMethod fieldGetShort = Scene.v().loadClassAndSupport("java.lang.reflect.Field").getMethod("short getShort(java.lang.Object)");
  private static final SootMethod fieldSetObj = Scene.v().loadClassAndSupport("java.lang.reflect.Field").getMethod("void set(java.lang.Object,java.lang.Object)");
  private static final SootMethod fieldSetBool = Scene.v().loadClassAndSupport("java.lang.reflect.Field").getMethod("void setBoolean(java.lang.Object,boolean)");
  private static final SootMethod fieldSetByte = Scene.v().loadClassAndSupport("java.lang.reflect.Field").getMethod("void setByte(java.lang.Object,byte)");
  private static final SootMethod fieldSetChar = Scene.v().loadClassAndSupport("java.lang.reflect.Field").getMethod("void setChar(java.lang.Object,char)");
  private static final SootMethod fieldSetDouble = Scene.v().loadClassAndSupport("java.lang.reflect.Field").getMethod("void setDouble(java.lang.Object,double)");
  private static final SootMethod fieldSetFloat = Scene.v().loadClassAndSupport("java.lang.reflect.Field").getMethod("void setFloat(java.lang.Object,float)");
  private static final SootMethod fieldSetInt = Scene.v().loadClassAndSupport("java.lang.reflect.Field").getMethod("void setInt(java.lang.Object,int)");
  private static final SootMethod fieldSetLong = Scene.v().loadClassAndSupport("java.lang.reflect.Field").getMethod("void setLong(java.lang.Object,long)");
  private static final SootMethod fieldSetShort = Scene.v().loadClassAndSupport("java.lang.reflect.Field").getMethod("void setShort(java.lang.Object,short)");
  private static final SootMethod reflNewInstance = Scene.v().loadClassAndSupport("java.lang.Class").getMethodByName("newInstance");
  private static final SootMethod reflConstructor = Scene.v().loadClassAndSupport("java.lang.reflect.Constructor").getMethodByName("newInstance");
  private static final SootMethod reflGetClass1 = Scene.v().loadClassAndSupport("java.lang.Class").getMethod("forName", new ArrayList<Type>() { private static final long serialVersionUID = 0; { add(RefType.v("java.lang.String")); }});
  private static final SootMethod reflGetClass2 = Scene.v().loadClassAndSupport("java.lang.Class").getMethod("forName", new ArrayList<Type>() { private static final long serialVersionUID = 0; {
    add(RefType.v("java.lang.String"));
    add(BooleanType.v());
    add(RefType.v("java.lang.ClassLoader"));
  }});

  @Override
  protected void internalTransform(final Body inputBody, final String phase, final Map options) {
    body = (JimpleBody)inputBody;

    // when the -app is used, our logger will also be visited we add its methods to the instrumented
    // methods to avoid purity check but we also need to skip instrumenting it
    SootMethod m = body.getMethod();
    if(m.hasTag("NoInstrumentTag")) {
      //System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!M NO INSTUMENT " + m);
      Iterator<Unit> it = body.getUnits().snapshotIterator();
      while(it.hasNext()){
        Stmt stmt = (Stmt)it.next();
        alwaysMark(stmt);
      }
      return;
    }
    String declClass = m.getDeclaringClass().getName();
    if (!(declClass.contains("rvpredict") || declClass.contains("com.ning.compress.lzf") ||
declClass.contains("jdbm"))) {
      assert !body.getMethod().getDeclaringClass().getName().contains("com.google.protobuf") : "Tried to instrument our own logging code";
      if (PredictorOptions.v().verbose_level >= 2)
        System.out.println("instrumenting method : " + body.getMethod().getSignature());
 
      returnStmts = getReturns(body.getUnits().snapshotIterator()); 
      lastIdentityStmt = getLastIdentityStmt();

      //this is also performed once per method, if the method happens to be a class initializer
      if(!markClassInit()){
        //this is performed once per method, and only if this is not  classinit
        markMethodEntryAndExit();
      }

      //mark the beginning and end of a synchronized methods with code to output
      //lock and unlock events respectively
      markSynchronizedMethod();

      markExceptionHandlers();

      Iterator<Unit> it = body.getUnits().snapshotIterator();
      while(it.hasNext()){
        Stmt stmt = (Stmt)it.next();
        if(stmt.hasTag("NoInstrumentTag")){
       //   System.out.println("!!!!!! S NO INSTRUMENT " + stmt);
          alwaysMark(stmt);
          continue;
        }
        //for each statement we apply the following methods until one of them succeeds
        //order is partially important in that markInvocations needs to come after
        //markStarts markConstructors and markLocks. The important thing is these
        //are all disjoint operations 

        //ridiculously, Java will not let you do this without assigning the boolean value to
        //something
      boolean unused = markJoin(stmt) || markStart(stmt) || markConstructor(stmt) || markLock(stmt) || markWaitAndNotify(stmt)
       || markFieldAccess(stmt) || markArrayAccess(stmt) || markReflectAccess(stmt) || markReflectConstructor(stmt) || markReflForName(stmt)
       || markImpureCall(stmt) || markBranch(stmt) || markInvocation(stmt);

      }
      
      //add jimple line numbers as Soot annotation Tags
      Util.addJimpleLineTags(body);

      patchLineNumbers();
      //printMethod(m);
    }
  }

  private void alwaysMark(Stmt stmt){
    boolean unused = markJoin(stmt) || markStart(stmt) || markLock(stmt) || markWaitAndNotify(stmt); 
  }

  private void printMethod(SootMethod m){
      System.out.println("******" + m);
      Iterator<Unit> it = m.getActiveBody().getUnits().iterator();
      while(it.hasNext()){
        Stmt stmt = (Stmt)it.next();
        System.out.print("*!" + Util.getJimpleLine(stmt) + " ");
        System.out.println(stmt);
      }
  }

  //we want the line numbers to reflect instrumentation now
  //before we added Loop Peeling we just used the original line numbers
  private void patchLineNumbers(){
    for(InvokeStmt is : lineNumberPatchMap.keySet()){
      //System.out.println("*" + is);
      Stmt target = lineNumberPatchMap.get(is);
      //System.out.println("**" + target); 
      int lineNumber = Util.getJimpleLine(target);
      //System.out.println("***" + lineNumber); 
      InvokeExpr ie = is.getInvokeExpr();
      //System.out.println("****" + body);
      int index = ie.getArgCount() - 1;
      //unfortunately Array and FieldAccesses do not pass
      //their line numbers as the alst argument, but rather
      //second to last
      if(((IntConstant)ie.getArg(index)).value != -1){
        --index;
      }
      ie.setArg(index, IntConstant.v(lineNumber));
    }
  }

  private static boolean isUntracked(final SootMethod m) {
    //return m.getDeclaringClass().isLibraryClass();
    String name = m.getDeclaringClass().getName();
    return name.startsWith("java.") || name.startsWith("javax.")
         || name.startsWith("sun.");
  }

  private static boolean isImpure(final SootMethod m) {
    return true;
  }

  // soot requires that the parameter unpacking IdentityStmts come before any non identity
  // statements (i.e., our logging call) thus we iterate past them before inserting the logging
  // call. I don't know if we can get IdentityStmts any other way, so this might not be as safe as
  // hoped. Not all methods have parameters (or references to this) so we need to just insert our
  // logging call as the first statement in that case. Particularly, we need to make sure we don't
  // insert a logging call after any labels, otherwise we may log spuriously. The hasNext might be
  // unneeded, I don't know if it is possible to generate empty jimple methods except as part of the
  // compiler.
  private void insertAsFirstStmt(Stmt s) {
    if(lastIdentityStmt != null)
      body.getUnits().insertAfter(s,lastIdentityStmt);
    else
      body.getUnits().insertBefore(s,body.getFirstNonIdentityStmt());
  }

  //returns the last identity statement or null if there are none
  private Stmt getLastIdentityStmt(){
    Iterator<Unit> it = body.getUnits().iterator();
    Unit u = null;
    if(it.hasNext()) u = it.next();
    if(!(u instanceof IdentityStmt)) return null;
    Unit prev = u;
    while(it.hasNext()){
      u = it.next();
      if(!(u instanceof IdentityStmt)){
        return (Stmt) prev;
      }
      prev = u;
    }
    return null;
  }

  private void insertBeforeReturn(Stmt s) {    
    if (returnStmts.size() == 0)
      assert (body.getUnits().getLast() instanceof ThrowStmt) : 
          "no return Statements and somehow last Stmt isn't a Throw?";
    else
      for(Stmt ret : returnStmts){
        Stmt copy = (Stmt) s.clone();
        body.getUnits().insertBefore(copy,ret);
      }
  }

  private List<Stmt> getReturns(Iterator<Unit> it){
    List<Stmt> ret = new ArrayList<Stmt>();
    while(it.hasNext()){
      Unit u = it.next();
      if(u instanceof RetStmt){
        assert false : "Please report this, we haven't actually ever found a RetStmt";
      }
      if((u instanceof ReturnStmt) || (u instanceof ReturnVoidStmt)) {
        ret.add((Stmt)u);
      }
    }
    return ret;
  }

  private void markMethodEntryAndExit() {
    if (body.getMethod().getSubSignature().equals("void finalize()")) {
      insertAsFirstStmt(logFinalize(body.getThisLocal(),body.getMethod()));
      insertAsFirstStmt(logMethodBegin(body.getMethod()));
      insertBeforeReturn(logMethodEnd(body.getMethod()));
    }
    else {
      insertAsFirstStmt(logMethodBegin(body.getMethod()));
      insertBeforeReturn(logMethodEnd(body.getMethod()));
    }
  }

  // Since class initializers can't throw exceptions we can just mark before return
  private boolean markClassInit() {
    if (body.getMethod().getSubSignature().equals("void <clinit>()")) {
      insertAsFirstStmt(logClassInitBegin(body.getMethod().getDeclaringClass()));
      insertBeforeReturn(logClassInitEnd(body.getMethod().getDeclaringClass()));
      return true;
    }
    return false;
  }

  //put a logLock at the beginning and a logUnlock at the end of the method since
  //it is synchronized.  Synchronized methods need to be handled specially since soot
  //seems not to convert them to a synchronized block
  private void markSynchronizedMethod(){
     if (body.getMethod().isSynchronized()) {
      if (!body.getMethod().isStatic()) {
        insertAsFirstStmt(logLock(body.getThisLocal()));
        insertBeforeReturn(logUnlock(body.getThisLocal()));
      } else {
        insertAsFirstStmt(logLock(body.getMethod().getDeclaringClass()));
        insertBeforeReturn(logUnlock(body.getMethod().getDeclaringClass()));
      }
    }
  }

  private void markExceptionHandlers(){
	Set<Unit> seenCaughts = new HashSet<Unit>();
    for(Trap trap : body.getTraps()){
      Unit handler = trap.getHandlerUnit();
	  if(seenCaughts.contains(handler)) continue;
      body.getUnits().insertAfter(logException((Local)handler.getDefBoxes().get(0).getValue()), handler);
	  seenCaughts.add(handler);
    }
  }

  private boolean markBranch(Stmt stmt) {
    if (stmt.branches()) {
      InvokeStmt is = logBranch(stmt, body.getMethod().getDeclaringClass(),body.getMethod());
      lineNumberPatchMap.put(is, stmt);
      body.getUnits().insertBefore(is,stmt);
      return true;
    }
    return false;
  }

  // NB a class could have a start method without extending thread so we must avoid marking those
  // start methods.
  private boolean markStart(Stmt stmt) {
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

  private boolean markJoin(Stmt stmt) {
    if (stmt.containsInvokeExpr()) {
      if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
        if (stmt.getInvokeExpr().getMethod() == join) {
          body.getUnits().insertAfter(logJoin(stmt),stmt);
          return true;
        }
      }
    }
    return false;
  }
 
  // When a lock is aquired an EnterMonitorStmt is encountered. When a lock is released an
  // ExitMonitorStmt is encountered.   
  private boolean markLock(Stmt stmt) {
    if (stmt instanceof EnterMonitorStmt){
      body.getUnits().insertAfter(logLock(stmt), stmt);
      return true;
    }
    if (stmt instanceof ExitMonitorStmt){
      body.getUnits().insertAfter(logUnlock(stmt), stmt);
      return true;
    }
    return false;
  }

  private boolean markReflectAccess(Stmt stmt) {
    if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
      SootMethod m = stmt.getInvokeExpr().getMethod();
      if (m == fieldGetObj || m == fieldGetBool || m == fieldGetByte || m == fieldGetChar || m == fieldGetDouble
          || m == fieldGetFloat || m == fieldGetInt || m == fieldGetLong || m == fieldGetShort) {
      
      InvokeStmt is 
        = logReflFieldAccess(stmt,false,body.getMethod().getDeclaringClass(),body.getMethod());
      lineNumberPatchMap.put(is, stmt);
        body.getUnits().insertAfter(is,stmt);
        return true;
          }
      if (m == fieldSetObj || m == fieldSetBool || m == fieldSetByte || m == fieldSetChar || m == fieldSetDouble
          || m == fieldSetFloat || m == fieldSetInt || m == fieldSetLong || m == fieldSetShort) {

      InvokeStmt is 
        = logReflFieldAccess(stmt,true,body.getMethod().getDeclaringClass(),body.getMethod());
      lineNumberPatchMap.put(is, stmt);
        body.getUnits().insertAfter(is,stmt);
        return true;
          }
    }
    return false;
  }

  private boolean markReflectConstructor(final Stmt stmt) {
    if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
      SootMethod m = stmt.getInvokeExpr().getMethod();
      if (m == reflNewInstance) {
        // Since expressions while being Values cannot always be used in value contexts if we have
        // only an invokeStmt we will force it to the more useful AssignStmt
        if (stmt instanceof AssignStmt) {
          final AssignStmt astmt = (AssignStmt) stmt;
      InvokeStmt is 
    = logReflNewInstanceBegin(stmt,astmt,body.getMethod().getDeclaringClass(),body.getMethod());
      lineNumberPatchMap.put(is, stmt);
          body.getUnits().insertBefore(is,astmt);
      is 
    = logReflNewInstanceEnd(stmt,astmt,body.getMethod().getDeclaringClass(),body.getMethod());
      lineNumberPatchMap.put(is, stmt);
          body.getUnits().insertAfter(is,astmt);
          return true;
        } else {
          assert stmt instanceof InvokeStmt;
          final AssignStmt astmt = new JAssignStmt(new LocalGenerator(body).generateLocal(stmt.getInvokeExpr().getType()),stmt.getInvokeExpr());
          body.getUnits().swapWith(stmt,astmt);
      InvokeStmt is =
logReflNewInstanceBegin(stmt,astmt,body.getMethod().getDeclaringClass(),body.getMethod());
      lineNumberPatchMap.put(is, stmt);
          body.getUnits().insertBefore(is ,astmt);

      is = logReflNewInstanceEnd(stmt,astmt,body.getMethod().getDeclaringClass(),body.getMethod());
      lineNumberPatchMap.put(is, stmt);
          body.getUnits().insertAfter(is,astmt);
          return true;
        }
      }
      if (m == reflConstructor) {
        // Since expressions while being Values cannot always be used in value contexts if we have
        // only an invokeStmt we will force it to the more useful AssignStmt
        if (stmt instanceof AssignStmt) {
          final AssignStmt astmt = (AssignStmt) stmt;
      InvokeStmt is = logReflConstructorBegin(stmt,astmt,body.getMethod().getDeclaringClass(),body.getMethod());
      lineNumberPatchMap.put(is, stmt);
          body.getUnits().insertBefore(is,astmt);

      is =
logReflConstructorEnd(stmt,astmt,body.getMethod().getDeclaringClass(),body.getMethod());
      lineNumberPatchMap.put(is, stmt);

          body.getUnits().insertAfter(is,astmt);
          return true;
        } else {
          assert stmt instanceof InvokeStmt;
          final AssignStmt astmt = new JAssignStmt(new LocalGenerator(body).generateLocal(stmt.getInvokeExpr().getType()),stmt.getInvokeExpr());
          body.getUnits().swapWith(stmt,astmt);
          InvokeStmt is = 
      logReflConstructorBegin(stmt,astmt,body.getMethod().getDeclaringClass(),body.getMethod());
      lineNumberPatchMap.put(is, stmt);
        body.getUnits().insertBefore(is,astmt);
        is = 
logReflConstructorEnd(stmt,astmt,body.getMethod().getDeclaringClass(),body.getMethod());
      lineNumberPatchMap.put(is, stmt);
        body.getUnits().insertAfter(is,astmt);
          return true;
        }
      }
    }
    return false;
  }

  private boolean markFieldAccess(Stmt stmt) {
    if (stmt.containsFieldRef()) {
      assert (stmt instanceof AssignStmt) : "Unknown FieldReffing Stmt";
      InvokeStmt is 
        = logFieldAcc(stmt,body.getMethod().getDeclaringClass(),body.getMethod());
      lineNumberPatchMap.put(is, stmt);
      body.getUnits().insertAfter(is,stmt);
      return true;
    }
    return false;
  }

  private boolean markArrayAccess(Stmt stmt) {
    if (stmt.containsArrayRef()) {
      assert (stmt instanceof AssignStmt) : "Unknown ArrayReffing Stmt";

      InvokeStmt is 
        = logArrayAcc(stmt,body.getMethod().getDeclaringClass(),body.getMethod());
      lineNumberPatchMap.put(is, stmt);
      body.getUnits().insertAfter(is,stmt);
      return true;
    }
    return false;
  }

  // Mark impure calls to the java libraries. This probably will miss some stuff, but at least will
  // catch System.out.println(...)
  private boolean markImpureCall(Stmt stmt) {
    if ((stmt instanceof InvokeStmt)
        && (stmt.getInvokeExpr() instanceof InstanceInvokeExpr)
        && (isUntracked(stmt.getInvokeExpr().getMethod()))
        && (isImpure(stmt.getInvokeExpr().getMethod()))) {
      InvokeStmt is 
        = logImpureCall(stmt,body.getMethod().getDeclaringClass(),body.getMethod());
      lineNumberPatchMap.put(is, stmt);
      body.getUnits().insertAfter(is ,stmt);
      return true;
    } else {
      return false;
    }
  }

  // Since constructors don't have the object they produce available before they are called we
  // cannot include its object ID beforehand. I really think constructors should be static.
  //
  //This just keeps the impure logging from happening.  Not sure why impure logging would
  //be a problem
  private boolean markConstructor(Stmt stmt) {
    if (stmt.containsInvokeExpr() && (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) 
        && stmt.getInvokeExpr().getMethod().getName().contains("<init>")) {
        SootMethod method = body.getMethod();
        //only need to log end as constructor, treat beginning as a static invoke so that we don't piss off Java
        //(making it an instance invoke would piss off java because it would be placed BEFORE the constructor
        //which is not allowed in java)
        InvokeStmt is = logConstructorBegin(stmt,method.getDeclaringClass(),method);
        lineNumberPatchMap.put(is, stmt);
        body.getUnits().insertBefore(is,stmt);
        is = logConstructorEnd(stmt,method.getDeclaringClass(),method);
        lineNumberPatchMap.put(is, stmt);
        body.getUnits().insertAfter(is,stmt);
      return true;
    }
    return false;
  }

  //mark waits and notifies.  Unfortunately, part of this is redundant 
  //with marking class inits and marking invocations.  If we had HoFs 
  //I would factor the commonalities out.  With the limited ability of 
  //java 6 I will just repeat code.  This will be factored once java 7
  //goes live and we can use closures
  private boolean markWaitAndNotify(Stmt stmt){
    if (stmt.containsInvokeExpr()) {
      SootMethod method = stmt.getInvokeExpr().getMethod();
      if (method == wait1) {
        InvokeStmt is = logWait(stmt,body.getMethod().getDeclaringClass(),body.getMethod());
        lineNumberPatchMap.put(is, stmt);
        body.getUnits().insertAfter(is,stmt);
        return true;
      }
      if (method == wait2 || method == wait3) {
        body.getUnits().insertAfter(logTimeoutWait(),stmt);
        return true;
      }
      if(method == notify || method == notifyAll) {
        InvokeStmt is = 
          logNotify(stmt,body.getMethod().getDeclaringClass(),body.getMethod());
        lineNumberPatchMap.put(is, stmt);
        body.getUnits().insertBefore(is,stmt);
        return true;
      }
    }
    return false;
  }

  private boolean markReflForName(final Stmt stmt) {
    if (stmt.containsInvokeExpr()) {
      SootMethod method = stmt.getInvokeExpr().getMethod();
      if (method == reflGetClass1 || method == reflGetClass2) {
        InvokeStmt is = 
          logReqClassInit(stmt,body.getMethod().getDeclaringClass(),body.getMethod());
        lineNumberPatchMap.put(is, stmt);
        body.getUnits().insertAfter(is,stmt);
        return true;
      }
    }
    return false;
  }

  // Log all REMAINING method calls. For invocations that are called on a particular object we need to log the
  // object. Since constructors don't have their target around (they construct it) we will handle
  // them specially. For invocations without a target we can just record its line numbers.  I don't
  // know if there is a better way to pick out constructors.
  private boolean markInvocation(Stmt stmt) {
    if (stmt.containsInvokeExpr()) {
      SootMethod method = body.getMethod();
      InvokeExpr e = stmt.getInvokeExpr();
      //log instance methods
      if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
        InvokeStmt is = logInstanceInvokeBegin(stmt,method.getDeclaringClass(),method);
        lineNumberPatchMap.put(is, stmt);
        body.getUnits().insertBefore(is, stmt);
        // body.getUnits().insertAfter(logInstanceInvokeEnd(stmt,method.getDeclaringClass(),method),stmt);
        return true;
      }
      //log static methods
      // We don't want to log calls to other logging functions so skip them...
      // all our logging functions are static so they would show up here
      if (!e.getMethod().getDeclaringClass().getPackageName().equals("rvpredict.logging")) {
        InvokeStmt is = logInvokeBegin(stmt,method.getDeclaringClass(),method);
        lineNumberPatchMap.put(is, stmt);
        body.getUnits().insertBefore(is,stmt);
        // body.getUnits().insertAfter(logInvokeEnd(stmt,method.getDeclaringClass(),method),stmt);
        return true;
      }
    }
    return false;  //returning false and true in the method doesn't really matter since
                   //we will presumably always put it last in the disjunction chain
  }
}
// vim: tw=100:sw=2
