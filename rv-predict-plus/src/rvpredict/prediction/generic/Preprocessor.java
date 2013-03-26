package rvpredict.prediction.generic;

import soot.Body;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Trap;
import soot.Unit;
import soot.Value;
import soot.Scene;

import soot.util.Chain;

import soot.jimple.Stmt;
import soot.jimple.ReturnStmt;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.ThrowStmt;
import soot.jimple.AssignStmt;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.JimpleBody;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.ArrayRef;
import soot.jimple.FieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;

import rvpredict.prediction.io.TraceReader;
import rvpredict.prediction.io.ReversedTraceWriter;
import rvpredict.util.Configure;
import rvpredict.util.IString;
import rvpredict.util.Util;
import rvpredict.PredictorException;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.HashSet;
import java.util.HashMap;

import java.io.IOException;
import java.io.File;


/*@
 * build call graphs, add events for ends of methods, and generate shared variables
 */

public class Preprocessor extends Stage {

  public HashSet<String> shared; // the set of shared locations
  HashSet<String> sharedR; // the set of read-only shared locations
  /*@ map from locations to owner threads,
   *  if the thread starts with "-", the thread only reads the location
   *  if the thread starts with "+", the thread writes the location
   */
  HashMap<String, String> locations;
  HashMap<IString, List<CallTree>> methodcalls; // call-method trees of threads

  TraceReader ta;
  ReversedTraceWriter tw;
  public Event startEvent;

  // the map recording when a thread uses a class for the first time;
  // all the accesses to a class/object happen after <clinit> finishes!
  HashMap<IString, HashSet<String>> clinitMap;

  public Preprocessor() throws Exception {
    super("Preprocessing");
    ta = new TraceReader(workDir + File.separator + Configure.getString("TraceFile") + ".rvpf");
    tw = new ReversedTraceWriter(workDir + File.separator + Configure.getString("TmpTraceFile"));
    startEvent = ta.next();
    if (startEvent.eventType != Event.BEGIN)
      throw new PredictorException("An BEGIN event is expected at the beginning of the trace file!");
    shared = new HashSet<String>();
    sharedR = new HashSet<String>();
    locations = new HashMap<String,String>();
    methodcalls = new HashMap<IString, List<CallTree>>();
    clinitMap  = new HashMap<IString, HashSet<String>>();
  }

  /*
   * Note: the field access event for a method call appears before the beginning of
   * the called method (except for the <init> method) in the trace.
   * But the following analysis (slicing) assumes that the event appears after the
   * end of the called method.
   * So we need to change the order in preprocessing.
   */

  public void process() throws PredictorException{
    try {
      Event event = startEvent;

      // map threads into statement stacks
      HashMap<String, Stack<Layer>> tMap = new HashMap<String, Stack<Layer>>();
      Layer currLayer = null;
      IString currThread = IString.get("");

      while (event != null){

        // switch the thread if needed
        if (!event.thread.equals(currThread)){
          currLayer = switchThread(event.thread, currThread, tMap, currLayer);
          currThread = event.thread;
        }

        if ((currLayer.units != null) && (currLayer.stmt == null)){
          throw new PredictorException("Cannot find the corresponding statement!");
        }

        boolean moveEvent = false;
        boolean writeEvent = true;
        Event newEvent = null;
        boolean moveStmt = false;
        // no statement will be matched to the beginning of a method, so it needs to
        // be handled first
        /*
         * the begin event is allowed in three cases:
         * 1) we are on the top level of the thread;
         * 2) the method is <clinit>;
         * 3) an untracked method invocation was encountered right before this event
         *    (also see below for the invoke statement
         */
        if ((event.eventType == Event.BEGIN) && ((currLayer.units == null) || (event.id.toString().indexOf("<clinit>") > -1) || (currLayer.callEvent != null))){

          /*
           * Sanity check: the corresponding method invocaiton should have been
           * encourntered except for <init> and <clinit>
           */
          if ((currLayer.stmt != null) && currLayer.stmt.containsInvokeExpr()) {
            String methodName = currLayer.stmt.getInvokeExpr().getMethod().getSubSignature();
            if ((event.id.toString().indexOf("<clinit>") < 0) && (currLayer.callEvent != null) && (methodName.compareTo(event.id.toString()) != 0)){
              if (!ma.isCallBack(event.id.toString()))
                System.err.println("Warning! The method name of the method invocation (" + methodName + ") is different from the called method (" + event.id + "), maybe a unknown callback method !");
            }
          }
          currLayer = addNewLayer(currThread, tMap, currLayer, event);
          moveEvent = true;
        } else if ((event.eventType == Event.EXENTER)) {
          currLayer = handleExEnter(currThread, currLayer, tMap, event);
          moveEvent = true;
        } else if ((event.eventType == Event.ENTER) && (event.ln == Util.getJimpleLine(currLayer.stmt))){
          moveEvent = true;
        } else {
          Stmt stmt = currLayer.stmt;
          Stmt next = null;
          if (stmt.containsInvokeExpr()){
            String methodName = currLayer.stmt.getInvokeExpr().getMethod().getSubSignature();

            if (currLayer.callEvent != null) {
              // this statement has been processed

              //thread start event has been printed already!
              if (! isThreadStart(currLayer.stmt.getInvokeExpr().getMethod()))
                tw.writeEvent(currLayer.callEvent);
              currLayer.callEvent = null;
              if (event.eventType == Event.EXENTER){
                currLayer = handleExEnter(currThread, currLayer, tMap, event);
                moveEvent = true;
              } else if (event.eventType == Event.POST){
                moveStmt = true;
                moveEvent = true;
                writeEvent = false; // the post event is not needed to output
              } else if (methodName.indexOf("<init>") > -1){
                // this happens when this init is an untracked function but it calls
                // some tracked function
                currLayer.callEvent = null;
              } else
                throw new PredictorException("A post-method-call event is expected!");
            } else {
              InvokeExpr expr = stmt.getInvokeExpr();
              SootMethod method = expr.getMethod();
              if (expr instanceof InstanceInvokeExpr){
                // the event for the <init> is generated after the method call;
                // so it can be after the end of the method call

                // note if the target is null, no call method will be logged!
                if ((method.getSubSignature().indexOf("<init>") < 0) && (event.eventType != Event.FIELD) && (event.eventType != Event.EXENTER))
                  throw new PredictorException("A field access (via untracked method call)/method call/exception event on " + event.cls + " is expected!");
                if (event.eventType == Event.EXENTER){
                  currLayer = handleExEnter(currThread, currLayer, tMap, event);
                  moveEvent = true;
                } else {
                  if (event.eventType == Event.FIELD){
                    if (! ma.isTracked(event.cls, method.getSubSignature())){
                      // treated as a read/write on the object
                      String loc = event.cls + "@" + event.id;
                      if (ma.isPure(method.makeRef())){
                        setReadLocation(loc, currThread);
                      } else {
                        setWriteLocation(loc, currThread);
                        event.setWrite();
                      }
                    }
                    //the corresponding post event can happen after the target thread
                    // start;
                    // In that case, the beginning of the thread seems to happen
                    // before the thread start if we print out the start event at the
                    // post event like other methods!
                    if (isThreadStart(method))
                      tw.writeEvent(event);

                    // even it is a untracked method,
                    // it can invoke some tracked methods!
                    currLayer.callEvent = event;
                    moveEvent = true;
                  } else {
                    // make up an event for <init>; this will be discarded when return
                    currLayer.callEvent = new Event(currThread, Util.getJimpleLine(stmt), Event.FIELD, IString.get(method.getDeclaringClass().getName()), "<init>", null, true);
                  }
                  checkClinit(currThread, event.cls.toString());
                  if ((method.getSubSignature().indexOf("<init>") > -1) && (event.eventType == Event.FIELD)){
                    currLayer.callEvent = null;
                    moveStmt = true;
                  } else
                    writeEvent = false;
                }
              } else {
                if (event.eventType != Event.FIELD)
                  throw new PredictorException("A field access (via untracked method call)/method call/exception event on " + event.cls + " is expected!");
                String clsName = expr.getMethod().getDeclaringClass().getName();
                checkClinit(currThread, clsName);
                if (! ma.isTracked(event.cls, method.getSubSignature())){
                  // this method is treated as a read/write on the class
                  String loc = event.cls + "!";
                  if (ma.isPure(method.makeRef())){
                    setReadLocation(loc, currThread);
                  } else {
                    setWriteLocation(loc, currThread);
                    event.setWrite();
                  }
                }
                currLayer.callEvent = event;
                moveEvent = true;
                writeEvent = false;
              }
            }
          } else if (stmt instanceof IfStmt) {
            if (event.eventType != Event.ENTER)
              throw new PredictorException("An ENTER event is expected!");
            next = getTarget(((IfStmt)stmt).getTarget());
            if (event.ln != Util.getJimpleLine(next))
              next = getTarget((Stmt)currLayer.units.getSuccOf(stmt));
            if (event.ln != Util.getJimpleLine(next))
              throw new PredictorException("The line number of the enter event cannot match any target!");
            newEvent = new Event(currThread, Util.getJimpleLine(stmt), Event.IFENTER, null, null, null, true);
            currLayer.stmt = next;
            moveEvent = true;
          } else if ((stmt instanceof LookupSwitchStmt) || (stmt instanceof TableSwitchStmt)){
            if (event.eventType != Event.ENTER)
              throw new PredictorException("An ENTER event is expected!");
            if (stmt instanceof LookupSwitchStmt){
              LookupSwitchStmt lswitch = (LookupSwitchStmt)stmt;
              int count = lswitch.getTargetCount();
              if (count == 0) {
                System.err.println("Empty LookupSwitchStmt");
                System.err.println(stmt);
                System.exit(1);
              }
              for (int i = 0; i < count; i ++){
                next = getTarget((Stmt)lswitch.getTarget(i));
                if (event.ln == Util.getJimpleLine(next))
                  break;
              }
              if (event.ln != Util.getJimpleLine(next))
                next = getTarget((Stmt)lswitch.getDefaultTarget());
            } else if (stmt instanceof TableSwitchStmt){
              TableSwitchStmt lswitch = (TableSwitchStmt)stmt;
              int count = lswitch.getTargets().size();
              for (int i = 0; i < count; i ++){
                next = getTarget((Stmt)lswitch.getTarget(i));
                if (event.ln == Util.getJimpleLine(next))
                  break;
              }
              if (event.ln != Util.getJimpleLine(next))
                next = getTarget((Stmt)lswitch.getDefaultTarget());
            }
            if (event.ln != Util.getJimpleLine(next))
              throw new PredictorException("The line number of the enter event cannot match any target!");
            newEvent = new Event(currThread, Util.getJimpleLine(stmt), Event.IFENTER, null, null, null, true);
            currLayer.stmt = next;
            moveEvent = true;
          } else if (stmt instanceof GotoStmt){
            newEvent = new Event(currThread, Util.getJimpleLine(stmt), Event.IFENTER, null, null, null, true);
            next = (Stmt)((GotoStmt)stmt).getTarget();
            currLayer.stmt = next;
          } else if ((stmt instanceof EnterMonitorStmt) || (stmt instanceof ExitMonitorStmt)){
            if (event.eventType != Event.FIELD)
              throw new PredictorException("A field access event is needed for enter/exit monitor statement!");
            if (stmt instanceof EnterMonitorStmt)
              event.eventType = Event.LOCK;
            else
              event.eventType = Event.UNLOCK;
            moveEvent = true;
            moveStmt = true;
          } else if (stmt instanceof ThrowStmt) {
            if (event.eventType != Event.EXENTER)
              throw new PredictorException("An exception event on is expected!");
            currLayer = handleExEnter(currThread, currLayer, tMap, event);
            moveEvent = true;
          }  else if (stmt instanceof AssignStmt){
            if ((stmt.containsArrayRef() || stmt.containsFieldRef()) && (event.eventType == Event.EXENTER)){
              // instance field access or array access can trigger exceptions,
              // e.g., nullpointer
              FieldRef ref = null;
              if (stmt.containsFieldRef()){
                ref = stmt.getFieldRef();
              }
              if ((ref == null) || (ref instanceof InstanceFieldRef)){
                currLayer = handleExEnter(currThread, currLayer, tMap, event);
                moveEvent = true;
              }
            }
            if (! moveEvent) {
              moveEvent = handleAssign((AssignStmt)stmt, event, currThread);
              moveStmt = true;
            }
          }  else if ((stmt instanceof ReturnStmt) || (stmt instanceof ReturnVoidStmt)){
            currLayer = handleReturn(currThread, currLayer, tMap);
          } else
            moveStmt = true;
        }

        // post-analysis handling
        if (newEvent != null)
          tw.writeEvent(newEvent);
        if (moveEvent){
          if (writeEvent)
            tw.writeEvent(event);
          event = ta.next();
        }
        if (moveStmt){
          currLayer.stmt = (Stmt)currLayer.units.getSuccOf(currLayer.stmt);
        }
      }
      tMap.get(currThread.toString()).push(currLayer);
      post_process(tMap);
    } catch (Exception e){
      throw PredictorException.report("Preprocess, trace #" + ta.pos, e);
    }
  }

  boolean isThreadStart(SootMethod m){
    return ((m.getDeclaringClass().getName().compareTo("java.lang.Thread") == 0) && (m.getSubSignature().compareTo("void start()") == 0));
  }

  void post_process(HashMap<String, Stack<Layer>> tMap) throws Exception{
    // need to mark the end of the main/run method of every thread to facilitate
    // the reverse process of slicer
    Iterator mapIt = tMap.keySet().iterator();
    while (mapIt.hasNext()){
      String currThread = (String)mapIt.next();
      Stack<Layer> stack = tMap.get(currThread);
      Layer currLayer = stack.pop();
      while (! stack.isEmpty()){
        if (currLayer.stmt.containsInvokeExpr()){
          // this must be a static method
          Event newEvent = new Event(IString.get(currThread), Util.getJimpleLine(currLayer.stmt), Event.FIELD, IString.get(currLayer.stmt.getInvokeExpr().getMethod().getDeclaringClass().getName()), "!", null, true);
          tw.writeEvent(newEvent);
        }
        Event newEvent = new Event(IString.get(currThread), Util.getJimpleLine(currLayer.stmt), Event.END, IString.get(currLayer.method.caller.getDeclaringClass().getName()), currLayer.method.caller.getSubSignature(), null, true);
        tw.writeEvent(newEvent);
        currLayer = stack.pop();
      }
    }
    ta.close();
    tw.close();
    SharedVarHandler.storeShared(shared);
  }

  boolean handleAssign(AssignStmt stmt, Event event, IString currThread) throws PredictorException, IOException{
    Value left = stmt.getLeftOp();
    Value right = stmt.getRightOp();
    // in Jimple, it is impossible to have field accesses on both sides
    if (left instanceof InstanceFieldRef){
      SootField field = ((InstanceFieldRef)left).getField();
      if (event.eventType != Event.FIELD)
        throw new PredictorException("A field access event on " + field.getSubSignature() + " is expected!");
      // event.setFieldName(field.getName());
      if (event.eventType == Event.FIELD) {
        checkClinit(currThread, event.cls.toString());
        String loc = event.cls + "@" + event.id + "." + field.getName();
        event.setWrite();
        setWriteLocation(loc, currThread);
      }
      return true;
    } else if (right instanceof InstanceFieldRef){
      SootField field = ((InstanceFieldRef)right).getField();
      if (event.eventType != Event.FIELD)
        throw new PredictorException("A field access event on " + field.getSubSignature() + " is expected!");
      // event.setFieldName(field.getName());
      if (event.eventType == Event.FIELD) {
        checkClinit(currThread, event.cls.toString());
        String loc = event.cls + "@" + event.id + "." + field.getName();
        setReadLocation(loc, currThread);
      }
      return true;
    } else if (left instanceof StaticFieldRef) {
      SootField field = ((StaticFieldRef)left).getField();
      String loc = field.getDeclaringClass().getName() + "!." + field.getName();
      setWriteLocation(loc, currThread);
      checkClinit(currThread, field.getDeclaringClass().getName());
      // create a new event for static field write
      Event newEvent = new Event(currThread, Util.getJimpleLine(stmt), Event.FIELD, IString.get(field.getDeclaringClass().getName()), "!", null, false);
      // newEvent.setFieldName(field.getName());
      tw.writeEvent(newEvent);
    } else if (right instanceof StaticFieldRef){
      SootField field = ((StaticFieldRef)right).getField();
      String loc = field.getDeclaringClass().getName() + "!." + field.getName();
      setReadLocation(loc, currThread);
      checkClinit(currThread, field.getDeclaringClass().getName());
      //create a new event for static field read
      Event newEvent = new Event(currThread, Util.getJimpleLine(stmt), Event.FIELD, IString.get(field.getDeclaringClass().getName()), "!", null, true);
      // newEvent.setFieldName(field.getName());
      tw.writeEvent(newEvent);
    } else if (left instanceof ArrayRef){
      if (event.eventType != Event.ELEMENT)
        throw new PredictorException("An array access event  is expected!");
      String loc = event.cls + "@" + event.id + "." + event.index;
      event.setWrite();
      setWriteLocation(loc, currThread);
      return true;
    } else if (right instanceof ArrayRef){
      if (event.eventType != Event.ELEMENT)
        throw new PredictorException("An array access event  is expected!");
      String loc = event.cls + "@" + event.id + "." + event.index;
      setReadLocation(loc, currThread);
      return true;
    }
    return false;
  }

  Layer handleExEnter(IString currThread, Layer currLayer, HashMap<String, Stack<Layer>> tMap, Event event) throws Exception{
    // look for the correct exception handler
    boolean flag = false;
    while (currLayer != null){
      if ((currLayer.method.caller.getSubSignature().compareTo(event.id.toString()) == 0) && (currLayer.method.caller.getDeclaringClass().getName().compareTo(event.cls.toString()) == 0)){
        Chain traps = currLayer.method.caller.getActiveBody().getTraps();
        Iterator trapIt = traps.iterator();
        while (trapIt.hasNext()){
          Trap trap = (Trap)trapIt.next();
          Unit target = trap.getHandlerUnit();
          if (event.ln == Util.extractLineNumber(target)){
            if (currLayer.callEvent != null){
              // this exception is thrown in a called method
              tw.writeEvent(currLayer.callEvent);
              currLayer.callEvent = null;
            }
            // mark the source of the jump
            Event newEvent = new Event(currThread, Util.getJimpleLine(currLayer.stmt), Event.IFENTER, null, null, null, true);
            tw.writeEvent(newEvent);
            currLayer.stmt = (Stmt)target;
            flag = true;
            break;
          }
        }
        break;
      } else {
        if (currLayer.callEvent != null){
          // this exception is thrown in a called method
          tw.writeEvent(currLayer.callEvent);
        }
        // mark the end of a method
        Event newEvent = new Event(currThread, Util.getJimpleLine(currLayer.stmt), Event.END, IString.get(currLayer.method.caller.getDeclaringClass().getName()), currLayer.method.caller.getSubSignature(), null, true);
        tw.writeEvent(newEvent);
        currLayer = tMap.get(currThread.toString()).pop();
      }
    }
    if (! flag)
      throw new PredictorException("Cannot find the expected exception handler!");
    return currLayer;

  }

  Layer handleReturn(IString currThread, Layer currLayer, HashMap<String, Stack<Layer>> tMap) throws Exception{
    Stmt stmt = currLayer.stmt;
    // mark the end of a method
    Event newEvent = new Event(currThread, Util.getJimpleLine(stmt), Event.END, IString.get(currLayer.method.caller.getDeclaringClass().getName()), currLayer.method.caller.getSubSignature(), null, true);
    tw.writeEvent(newEvent);
    String methodName = currLayer.method.caller.getSubSignature();
    Stack<Layer> stack = tMap.get(currThread.toString());
    currLayer = stack.pop();
    if ((methodName.indexOf("<init>") > -1) && (currLayer.callEvent.id.toString().indexOf("<init>") > -1))
      currLayer.callEvent = null;
    return currLayer;
  }

  Layer addNewLayer(IString currThread, HashMap<String, Stack<Layer>> tMap, Layer currLayer, Event event) throws PredictorException {
    // get the correponding stack
    Stack<Layer> stack = tMap.get(currThread.toString());
    // find the method to begin
    SootClass mainClass = Scene.v().getSootClass(event.cls.toString());
    SootMethod m = mainClass.getMethod(event.id.toString());
    // construct a new call tree for the method (every method can correspond to
    // multiple trees)
    CallTree newM = new CallTree(m);
    // add the new method to the call list of the current(old) method
    currLayer.method.add(newM);
    // push the current layer
    stack.push(currLayer);
    // create a new current layer with the new method and the first statement of
    // the method
    Body body = m.getActiveBody();
    if (!(body instanceof JimpleBody))
      throw new PredictorException("Wrong body!");
    // attach count to each statement of the method
    Util.addJimpleLineTags(body);
    return new Layer((Stmt)body.getUnits().getFirst(), newM, body.getUnits());
  }

  Layer switchThread(IString newThread, IString currThread, HashMap<String, Stack<Layer>> tMap, Layer currLayer) throws PredictorException {
    // switch threads
    if (currThread.toString().length() > 0){
      // keep info about the current thread
      if (currLayer == null)
        throw new PredictorException("The layer is null when switching threads!");
      Stack<Layer> stack = tMap.get(currThread.toString());
      if (stack == null){
        throw new PredictorException("An unknown current thread!");
      }
      stack.push(currLayer);
    }
    // get info about the new thread
    currThread = newThread;
    Stack<Layer> stack = tMap.get(currThread.toString());
    if (clinitMap.get(currThread)==null)
      clinitMap.put(currThread, new HashSet<String>());
    if ((stack == null) || stack.isEmpty()){
      // the current thread is on the top level
      stack = new Stack<Layer>();
      tMap.put(currThread.toString(), stack);
      CallTree newM = new CallTree(null);
      currLayer = new Layer(null, newM, null);
      List<CallTree> l = methodcalls.get(currThread);
      if (l == null) {
        l = new ArrayList<CallTree>();
        methodcalls.put(currThread, l);
      }
      l.add(newM);
    } else {
      currLayer = stack.pop();
    }
    return currLayer;
  }

  static public Stmt getTarget(Stmt stmt){
    Unit t = stmt ;
    while (t instanceof GotoStmt)
      t = ((GotoStmt)t).getTarget();
    return (Stmt)t;
  }

  void checkClinit(IString thread, String cls) throws IOException {
    // check if this is the first time to use this class
    if (! clinitMap.get(thread).contains(cls)){
      Event newEvent = new Event(thread, 0, Event.CLINIT, IString.get(cls), "!", null, true);
      tw.writeEvent(newEvent);
      clinitMap.get(thread).add(cls);
    }

  }

  void setReadLocation(String loc, IString currThread){
    if (! shared.contains(loc)) {// not a shared location yet
      if (! sharedR.contains(loc)){ // not a read_shared location yet
        String thread = locations.get(loc);
        if (thread == null){
          locations.put(loc, "-" + currThread);
        } else {
          if (thread.endsWith(currThread.toString()))
            locations.put(loc, thread);
          else {
            if (thread.startsWith("-")){
              sharedR.add(loc);
            } else
              shared.add(loc);
            locations.remove(loc);
          }
        }
      }
    }
  }

  void setWriteLocation(String loc, IString currThread){
    if (! shared.contains(loc)) {// not a shared location yet
      if (sharedR.contains(loc)){ // is a read_shared location
        shared.add(loc);
        sharedR.remove(loc);
      } else {
        String thread = locations.get(loc);
        if (thread == null){
          locations.put(loc, "+" + currThread);
        } else {
          if (thread.endsWith(currThread.toString()))
            locations.put(loc, "+" + currThread);
          else {
            shared.add(loc);
            locations.remove(loc);
          }
        }
      }
    }
  }

  public void dump(){
    System.out.println("Found " + shared.size() + " shared variables:");
    Iterator it = shared.iterator();
    while (it.hasNext()){
      System.out.print(it.next() + ", ");
    }
    System.out.println();
    System.out.println("------------------");
    System.out.println("Call trees are: ");
    it = methodcalls.keySet().iterator();
    while (it.hasNext()){
      IString thread = (IString)it.next();
      System.out.println("*" + thread);
      List roots = methodcalls.get(thread);
      Iterator rIt = roots.iterator();
      while (rIt.hasNext()){
        CallTree root = (CallTree)rIt.next();
        printCallTree(root, "   ");
      }
    }
  }

  public void printCallTree(CallTree tree, String pre){
    Iterator it = tree.methods.iterator();
    while (it.hasNext()){
      CallTree sub = (CallTree)it.next();
      System.out.println(pre + "-" + sub.caller.getDeclaringClass().getName() + "." + sub.caller.getSubSignature());
      printCallTree(sub, pre + pre);
    }
  }

  class Layer {
    Stmt stmt;
    Chain<Unit> units;
    CallTree method;
    Event callEvent; // the event to invoke a method

    public Layer(Stmt s, CallTree m, Chain<Unit> units){
      stmt = s;
      method = m;
      this.units = units;
      //    redundant = new HashSet();
    }
  }
  class CallTree {
    SootMethod caller;
    ArrayList<CallTree> methods;
    int pos;

    public CallTree(SootMethod m){
      caller = m;
      methods = new ArrayList<CallTree>();
      pos = -1;
    }

    public void add(CallTree sub){
      methods.add(sub);
    }

    public CallTree next(){
      if (pos < methods.size()-1){
        pos ++;
        return methods.get(pos);
      }
      return null;
    }
    public CallTree previous(){
      if (pos>-1){
        return methods.get(pos--);
      }
      return null;
    }
  }
}
// vim: tw=100:sw=2
