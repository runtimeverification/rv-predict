package rvpredict.prediction.generic;

import rvpredict.analysis.ControlDependenceAnalyzer;
import rvpredict.analysis.MethodAnalyzer;
import rvpredict.prediction.io.ReverseTraceReader;
import rvpredict.prediction.io.ReversedTraceWriter;
import rvpredict.util.Configure;
import rvpredict.util.IString;
import rvpredict.util.Util;
import rvpredict.PredictorException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import java.io.File;
import java.io.IOException;

import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.UnknownType;
import soot.Value;
import soot.ValueBox;

import soot.jimple.ArrayRef;
import soot.jimple.DefinitionStmt;
import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.FieldRef;
import soot.jimple.IfStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.JimpleBody;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;

import soot.jimple.internal.JimpleLocal;

import soot.util.Chain;

public class Slicer {
   MethodAnalyzer ma;
   ArrayList<EventPattern> patterns;
   ReverseTraceReader tr;
   ReversedTraceWriter tw;
   HashSet shared;

   public Slicer(MethodAnalyzer ma, ArrayList<EventPattern> patterns, String workDir, HashSet shared, String name) throws IOException{
      this.ma = ma;
      this.patterns = patterns;
      tr = new ReverseTraceReader(workDir + File.separator, Configure.getString("TmpTraceFile"));
      tw = new ReversedTraceWriter(Util.makeLogFileName(workDir, "SlicedTraceFile", name));
      this.shared = shared;
   }

   public void process() throws PredictorException{
      try {
         Event event = tr.prev();
         HashMap<IString, Stack<Layer>> tMap = new HashMap<IString, Stack<Layer>>(); // map threads into statement stacks
         Layer currLayer = null;
         IString currThread = IString.get("");
         Front front = new Front();
         // MemoryMonitor mm = new MemoryMonitor(front);
         // mm.start();

         while (event != null){
            // if the new event's thread is not the one we handled, the thread has been switched
            if (!event.thread.equals(currThread)){
               // switch threads
               if (!currThread.equals(IString.get(""))){
                  // keep info about the current thread
                  if (currLayer == null)
                     throw new PredictorException("The layer is null when switching threads!");
                  Stack<Layer> stack = tMap.get(currThread);
                  if (stack == null){
                     throw new PredictorException("An unknown current thread!");
                  }
                  stack.push(currLayer);
               }
               // get info about the new thread
               currThread = event.thread;
               Stack<Layer> stack = tMap.get(currThread);
               if (stack == null) {
                  // new thread
                  stack = new Stack<Layer>();
                  tMap.put(currThread, stack);
                  currLayer = new Layer(null, null);
                  front.addThread(currThread);
               } else {
                  currLayer = stack.pop();
               }
            }
            // the event is moved forward only when it is consumed, i.e., found the matching statement
            boolean moveEvent = false;
            // the statement is moved forward when 1) it does not match the event; or 2) it matches an Enter event
            boolean moveStmt = false;

            if ((event.eventType != Event.END) && (event.eventType != Event.CLINIT))
               currLayer.caller_id = null;

            switch (event.eventType){
               case Event.END:
                  // end of a method
                  // get the correponding stack and push the current layer
                  tMap.get(currThread).push(currLayer);
                  // find the method to begin
                  SootClass mainClass = Scene.v().getSootClass(event.cls.toString());
                  SootMethod m = mainClass.getMethod(event.id.toString());

                  Body body = m.getActiveBody();
                  if (!(body instanceof JimpleBody))
                     throw new PredictorException("Wrong body, please use the -f J option in the command!");
                  // attach count to each statement of the method
                  Util.addJimpleLineTags(m.getActiveBody());

                  // generate a end clinit event
                  if (m.getSubSignature().indexOf("<clinit>")>-1){
                     FormalizedEvent e = new FormalizedEvent(currThread, "endcl" + tr.pos, event.cls.toString(), m.getSubSignature(), event.cls.toString(), true);
                     tw.writeEvent(e);
                  }

                  //generate lock events for synchronized method
                  if (m.isSynchronized()){
                     FormalizedEvent e = new FormalizedEvent(currThread, "unlock" + tr.pos, mainClass.getName(), m.getSubSignature(), mainClass.getName() + "@" + currLayer.caller_id, true);
                     tw.writeEvent(e);
                  }
                  // create a new current layer with the new method and the end statement
                  // Assumption: the previous event in the same thread should contain a invoke expression!
                  currLayer = new Layer(getStmtAt(body.getUnits(), event.ln), m);
                  moveEvent = true;
                  //moveStmt = false;
                  break;
               case Event.BEGIN:
                  // begin of a method
                  SootMethod oldm = currLayer.method;
                  boolean isSync = currLayer.method.isSynchronized();
                  // get the upper layer
                  currLayer = tMap.get(currThread).pop();
                  if ((currLayer.method != null) && (oldm.getName().compareTo("<clinit>") != 0)){
                     //generate unlock events for synchronized method
                     if (isSync){
                        FormalizedEvent e = new FormalizedEvent(currThread, "lock" + tr.pos, event.cls.toString(), event.id.toString(), event.cls + "@" + currLayer.caller_id, true);
                        tw.writeEvent(e);
                     }

                     // the statement in the caller's context should be the invoke statement for this method
                     // however, it could be another (untracked) method call, because this method can be call-back method invoked in the method
                     // for example, toString() and hashCode()
                     if (currLayer.stmt.containsInvokeExpr()){
                        // replace the statements in the method with the call to the method
                        front.removeLocalStatements(oldm, currLayer.stmt, currThread);
                        InvokeExpr invoke = currLayer.stmt.getInvokeExpr();
                        // check if the method was invoked indirectly via call-back
                        if (invoke.getMethod().getSubSignature().compareTo(event.id.toString()) == 0){
                           // correct the dependencies on parameters
                           front.replaceParameters(invoke, currThread);
                        }
                        //System.err.println("The invocation statement and the invoked method do not match (maybe an unknown callback method?).");
                        // currLayer.invoke = null;
                        currLayer.caller_id = null;
                     } else
                        throw new PredictorException("Cannot find the corresponding method invocation for the tracked method call");
                  } else if (currLayer.method == null) {
                     // the top of the thread
                     // mark the begin of a thread to facilitate the VC calculation
                     if (oldm.getSubSignature().compareTo("void run()") == 0){
                        FormalizedEvent e = new FormalizedEvent(currThread, "run" + tr.pos, "Thread", "run", currThread.toString(), true);
                        tw.writeEvent(e);
                     }
                  }
                  if ((oldm.getName().compareTo("<clinit>") != 0) && (currLayer.init_cls!=null)){
                     // this is the first time to call the class
                     FormalizedEvent e = new FormalizedEvent(currThread, "clinit" + tr.pos, currLayer.init_cls, "clinit", currLayer.init_cls, true);
                     tw.writeEvent(e);
                     currLayer.init_cls = null;
                  }
                  moveEvent = true;
                  //moveStmt = false;
                  break;
               case Event.CLINIT:
                  {
                     if (currLayer.caller_id == null){
                        // this is caused by a field access
                        FormalizedEvent e = new FormalizedEvent(currThread, "clinit" + tr.pos, event.cls.toString(), "clinit", event.cls.toString(), true);
                        tw.writeEvent(e);
                     } else // this is caused by a method call; need to print the clinit event before the method call begins
                        currLayer.init_cls = event.cls.toString();
                     moveEvent = true;
                  }
                  break;
               case Event.FIELD:
                  if (Util.getJimpleLine(currLayer.stmt) == event.ln){
                     if (currLayer.stmt.containsFieldRef()) {
                        // check if this event is dependant event and also related to shared variable,
                        // if so, add an event in the sliced trace
                        front.checkDependant(currLayer.stmt, event, currThread, currLayer);
                        // check and update the data dependence info
                        front.checkData(currLayer.stmt, currThread, event, currLayer);
                        // record the statement because we do not move it now
                        currLayer.handledStmt = currLayer.stmt;

                        FieldRef ref = currLayer.stmt.getFieldRef();
                        if (isRelevant(event, ref.getField().getName())) {
                           front.addRelevant(currLayer.stmt, event, currLayer, tMap.get(currThread), ref.getField().getName());
                        }
                     } else if (currLayer.stmt.containsInvokeExpr()){
                        InvokeExpr invoke = currLayer.stmt.getInvokeExpr();
                        if (! ma.isTracked(event.cls, invoke.getMethod().getSubSignature())) {
                           SootMethod im = invoke.getMethod();
                           boolean flag = false;
                           // need to handle Thread.start() and Thread.join() seperately
                           if (im.getDeclaringClass().getName().compareTo("java.lang.Thread") == 0){
                              if (im.getSubSignature().compareTo("void start()") == 0){
                                 FormalizedEvent e = new FormalizedEvent(currThread, "start" + tr.pos, event.cls.toString(), "void start()", event.getLoc(), true);
                                 tw.writeEvent(e);
                                 flag = true;
                              } else if (im.getSubSignature().compareTo("void join()") == 0){
                                 FormalizedEvent e = new FormalizedEvent(currThread, "join" + tr.pos, event.cls.toString(), "void join()", event.getLoc(), true);
                                 tw.writeEvent(e);
                                 flag = true;
                              }
                           } else if (im.getDeclaringClass().getName().compareTo("java.util.Timer") == 0){
                              if (im.getSubSignature().indexOf("schedule") > -1){
                                 FormalizedEvent e = new FormalizedEvent(currThread, "sch" + tr.pos, event.cls.toString(), "void schedule()", event.getLoc(), true);
                                 tw.writeEvent(e);
                                 flag = true;
                              }
                           }
                           if (! flag){
                              // check if this event is dependant event and also related to shared variable,
                              // if so, add an event in the sliced trace
                              front.checkDependant(currLayer.stmt, event, currThread, currLayer);
                              // check and update the data dependence info
                              front.checkData(currLayer.stmt, currThread, event, currLayer);
                              // record the statement because we do not move it now
                              currLayer.handledStmt = currLayer.stmt;
                              Event tmpEvent = new Event(event.thread, event.ln, Event.FIELD, event.cls, event.id.toString(), null, ma.isPure(invoke.getMethodRef()));
                              if (isRelevant(tmpEvent, "")) {
                                 front.addRelevant(currLayer.stmt, tmpEvent, currLayer, tMap.get(currThread), "");
                              }
                           }
                        } else {
                           // currLayer.invoke = currLayer.stmt;
                           currLayer.caller_id = event.id.toString();
                        }
                     }
                     moveEvent = true;
                  } else
                     moveStmt = true;
                  break;
               case Event.ELEMENT:
                  if ((Util.getJimpleLine(currLayer.stmt) == event.ln) && (currLayer.stmt.containsArrayRef())) {
                     // check if this event is dependant event and also related to shared variable,
                     // if so, add an event in the sliced trace
                     front.checkDependant(currLayer.stmt, event, currThread, currLayer);
                     // check and update the data dependence info
                     front.checkData(currLayer.stmt, currThread, event, currLayer);
                     // record the statement because we do not move it now
                     currLayer.handledStmt = currLayer.stmt;
                     if (isRelevant(event, event.index))
                        front.addRelevant(currLayer.stmt, event, currLayer, tMap.get(currThread), event.index);
                     moveEvent = true;
                  } else
                     moveStmt = true;
                  break;
               case Event.IFENTER:
                  currLayer.stmt = getStmtAt(currLayer.method.getActiveBody().getUnits(), event.ln);
                  moveEvent = true;
                  //moveStmt = false;
                  break;
               case Event.EXENTER:
                  if (Util.extractLineNumber(currLayer.stmt) == event.ln)
                     moveEvent = true;
                  moveStmt = true;
                  break;
               case Event.LOCK:
                  if (Util.getJimpleLine(currLayer.stmt) == event.ln){
                     if (currLayer.stmt instanceof EnterMonitorStmt){
                        moveEvent = true;
                        FormalizedEvent e = new FormalizedEvent(event.thread, "lock" + String.valueOf(tr.pos), currLayer.method.getDeclaringClass().getName(), currLayer.method.getSubSignature(), event.getLoc(), true);
                        tw.writeEvent(e);
                     } else
                        throw new PredictorException("Unmatched lock event and entermonitor statement!");
                  } else
                     moveStmt = true;
                  break;
               case Event.UNLOCK:
                  if (Util.getJimpleLine(currLayer.stmt) == event.ln){
                     if (currLayer.stmt instanceof ExitMonitorStmt){
                        moveEvent = true;
                        FormalizedEvent e = new FormalizedEvent(event.thread, "unlock" + String.valueOf(tr.pos), currLayer.method.getDeclaringClass().getName(), currLayer.method.getSubSignature(), event.getLoc(), true);
                        tw.writeEvent(e);
                     } else
                        throw new PredictorException("Unmatched unlock event and exitrmonitor statement!");
                  }  else
                     moveStmt = true;
                  break;
               case Event.ENTER:
                  if ((currLayer.stmt == null) || (Util.getJimpleLine(currLayer.stmt) == event.ln))
                     moveEvent = true;
                  moveStmt = true;
                  break;
               default:
                  System.err.println("Unhandled Case");
                  System.exit(1);
            }
            if ((currLayer.stmt != null) && moveStmt){
               if (currLayer.stmt == currLayer.handledStmt){
                  // this statement has been handled
                  currLayer.handledStmt = null;
               } else {
                  // check for data dependence
                  front.checkData(currLayer.stmt, currThread, event, currLayer);
                  // check for control dependence
                  front.checkControl(currLayer.cda, currLayer.stmt, currThread);
               }
            }
            if (moveEvent){
               event = tr.prev();
               // the ENTER event can be overlapped by antoher event
               // if ((event != null) && ((event.eventType == Event.ENTER) && (Util.getJimpleLine(currLayer.stmt) == event.ln) || (event.eventType == Event.EXENTER)  && (Util.extractLineNumber(currLayer.stmt) == event.ln)))
               // event = tr.prev();
            }
            if ((currLayer.stmt != null) && moveStmt){
               currLayer.stmt = (Stmt)currLayer.method.getActiveBody().getUnits().getPredOf(currLayer.stmt);
            }
         }
         /*tr.close();
         tw.close();*/
         //mm.stop = true;
      } catch (Exception e){
         throw PredictorException.report("Slicer, trace# " + tr.pos, e);
      }
   }

   boolean isRelevant(Event event, String field){
      for (int i = 0; i < patterns.size(); i++){
         if (patterns.get(i).match(event, field))
            return true;
      }
      return false;
   }

   Stmt getStmtAt(Chain units, long ln){
      Iterator it = units.iterator();
      while (it.hasNext()) {
         Stmt stmt = (Stmt)it.next();
         if (Util.getJimpleLine(stmt) == ln)
            return stmt;
      }
      return null;// this should not be reached
   }

   class Layer {
      Stmt stmt;
      SootMethod method;
      ControlDependenceAnalyzer cda;
      String caller_id;// the call obj
      String init_cls; // the first time in the thread to call the class
      Stmt handledStmt; // the field/element events process but do not move the statement, so we need to skip the handled statement
      //		Stmt invoke; // the statement makes the tracked method call
      Layer(Stmt stmt, SootMethod method){
         this.stmt = stmt;
         this.method = method;
         if (method != null)
            cda = Util.getCDA(method, ma);
         else
            cda = null;
         caller_id = null;
         init_cls = null;
         //			invoke = null;
      }
   }

   class MemoryMonitor extends Thread{
      Front front;
      boolean stop;
      MemoryMonitor(Front f){
         front = f;
         stop = false;
      }
      public void run(){
         while (true){
            try {
               Thread.sleep(10000);
            } catch(Exception e){}
            front.printSize();
            if (stop)
               break;
         }
      }
   }

   class Front {
      HashMap<IString, ArrayList<Stmt>> forControl; // map threads to statements to check control dependence
      HashSet<String> usedFields; // the set of used fields, shared by all threads
      HashMap<IString, HashSet<Value>> parameters; // used parameters of threads
      HashMap<IString, HashSet<Value>> usedLocals; // used locals of threads
      Value returnValue; // the faked return values

      Front(){
         forControl = new HashMap<IString, ArrayList<Stmt>>();
         usedFields = new HashSet<String>();
         parameters = new HashMap<IString, HashSet<Value>>();
         usedLocals = new HashMap<IString, HashSet<Value>>();
         returnValue = new JimpleLocal("$return$", UnknownType.v());
      }

      void printSize(){
         System.out.println("forControl : " + forControl.size());
         Iterator<ArrayList<Stmt>> it1 = forControl.values().iterator();
         while (it1.hasNext()){
            System.out.println("   : " + it1.next().size());
         }
         System.out.println("usedFields : " + usedFields.size());
         System.out.println("parameters : " + parameters.size());
         Iterator<HashSet<Value>> it2 = parameters.values().iterator();
         while (it2.hasNext()){
            System.out.println("   : " + it2.next().size());
         }
         System.out.println("usedLocals : " + parameters.size());
         it2 = usedLocals.values().iterator();
         while (it2.hasNext()){
            System.out.println("   : " + it2.next().size());
         }
      }

      // this is called only when the event matches the stmt and the event is field or element
      public void checkDependant(Stmt stmt, Event event, IString thread, Layer layer) throws IOException {
         if (!(stmt instanceof DefinitionStmt) && !(stmt instanceof InvokeStmt) && (event.eventType != Event.FIELD) && (event.eventType != Event.ELEMENT))
            return;
         if (stmt instanceof DefinitionStmt){
            Value left = ((DefinitionStmt)stmt).getLeftOp();
            Value right = ((DefinitionStmt)stmt).getRightOp();

            if (left instanceof FieldRef){
               String loc = event.cls + "@" + event.id + "." + ((FieldRef)left).getField().getName();
               if (usedFields.contains(loc)) {
                  if (shared.contains(loc)){
                     FormalizedEvent e = new FormalizedEvent(thread, String.valueOf(tr.pos), layer.method.getDeclaringClass().getName(), layer.method.getSubSignature(), loc, event.isRead);
                     tw.writeEvent(e);
                  }
               }
            } else if (left instanceof ArrayRef) {
               String loc = event.cls + "@" + event.id + "." + event.index;
               if (usedFields.contains(loc)) {
                  if (shared.contains(loc)){
                     FormalizedEvent e = new FormalizedEvent(thread, String.valueOf(tr.pos), layer.method.getDeclaringClass().getName(), layer.method.getSubSignature(), loc, event.isRead);
                     tw.writeEvent(e);
                  }
               }
            } else {
               HashSet<Value> locals = usedLocals.get(thread);
               if ((locals != null) && (locals.contains(left))){
                  if (right instanceof FieldRef){
                     String loc = event.cls + "@" + event.id + "." + ((FieldRef)right).getField().getName();
                     if (shared.contains(loc)){
                        FormalizedEvent e = new FormalizedEvent(thread, String.valueOf(tr.pos), layer.method.getDeclaringClass().getName(), layer.method.getSubSignature(), loc, event.isRead);
                        tw.writeEvent(e);
                     }
                  } else if (right instanceof ArrayRef) {
                     String loc = event.cls + "@" + event.id + "." + event.index;
                     if (shared.contains(loc)){
                        FormalizedEvent e = new FormalizedEvent(thread, String.valueOf(tr.pos), layer.method.getDeclaringClass().getName(), layer.method.getSubSignature(), loc, event.isRead);
                        tw.writeEvent(e);
                     }
                  }
               }
            }
         } else {
            InvokeExpr expr = stmt.getInvokeExpr();
            if (!ma.isTracked(event.cls, expr.getMethod().getSubSignature()) && ! ma.isPure(expr.getMethodRef())) {
               if (usedFields.contains(event.cls.toString() + event.id.toString())) {
                  String loc = event.cls + "@" + event.id;
                  if (shared.contains(loc)){
                     FormalizedEvent e = new FormalizedEvent(thread, String.valueOf(tr.pos), layer.method.getDeclaringClass().getName(), layer.method.getSubSignature(), loc, false);
                     tw.writeEvent(e);
                  }

               }
            }
         }
      }

      private boolean checkAndDelete(Value v, IString thread, Event event, Layer layer) throws IOException{
         if (v instanceof FieldRef) {
            String loc = event.cls + "@" + event.id + "." + ((FieldRef)v).getField().getName();
            if (usedFields.contains(loc)) {
               usedFields.remove(loc);
               return true;
            } else
               return false;
         } else if (v instanceof ArrayRef) {
            String loc = event.cls + "@" + event.id + "." + event.index;
            if (usedFields.contains(loc)) {
               usedFields.remove(loc);
               return true;
            } else
               return false;
         }
         HashSet<Value> locals = usedLocals.get(thread);
         if ((locals != null) && (locals.contains(v))){
            locals.remove(v);
            return true;
         }
         return false;
      }

      private void addUsedValue(Value value, IString thread, Event event, Layer layer) throws IOException{
         if (value instanceof FieldRef) {
            String loc = event.cls + "@" + event.id + "." + ((FieldRef)value).getField().getName();
            usedFields.add(loc);
         } else if (value instanceof ArrayRef) {
            String loc = event.cls + "@" + event.id + "." + event.index;
            usedFields.add(loc);
         } else if (value instanceof InvokeExpr){
            SootMethod m = ((InvokeExpr)value).getMethod();
            if (!ma.isTracked(event.cls, m.getSubSignature())){
               String loc = event.cls + "@" + event.id;
               usedFields.add(loc);
            } else {
               // if a return of a method is used, we add a faked return value (unique for every thread)
               usedLocals.get(thread).add(returnValue);
               return;
            }
         }

         List boxes = value.getUseBoxes();
         Iterator boxIt = boxes.iterator();
         // since Jimple is a 3-address language, we only need to go down one level here
         while (boxIt.hasNext()){
            Value v = ((ValueBox)boxIt.next()).getValue();
            if (v instanceof Local)
               usedLocals.get(thread).add(v);
            else if (v instanceof ParameterRef){
               usedLocals.get(thread).add(v);
               parameters.get(thread).add((ParameterRef)v);
            }
         }
      }

      boolean replaceValue(Value def, Value used, IString thread, Event event, Layer layer) throws IOException{
         if (checkAndDelete(def, thread, event, layer)){
            addUsedValue(used, thread, event, layer);
            return true;
         }
         return false;
      }

      /*@
       * check if the control statement controls some statements in the front
       * if so, remove those statements from the forControl list
       * and add the new control statement as well as its condition to the front
       */
      void checkControl(ControlDependenceAnalyzer cda, Stmt stmt, IString thread) throws IOException{
         if (!(stmt instanceof IfStmt) && !(stmt instanceof TableSwitchStmt) && !(stmt instanceof LookupSwitchStmt))
            return;
         ArrayList<Stmt> l = forControl.get(thread);
         if (l != null){
            ArrayList<Stmt> newl = new ArrayList<Stmt>();
            boolean flag = false;
            for (int i = 0; i < l.size(); i ++){
               Stmt st = l.get(i);
               if (cda.dependOn(st, stmt) && ! flag){
                  flag = true;
               } else
                  newl.add(st);
            }
            if (flag) {
               newl.add(stmt);
               // for Jimple statements, the conditions/keys should be locals
               if (stmt instanceof IfStmt)
                  addUsedValue(((IfStmt)stmt).getCondition(), thread, null, null);
               else if (stmt instanceof TableSwitchStmt)
                  addUsedValue(((TableSwitchStmt)stmt).getKey(), thread, null, null);
               else if (stmt instanceof LookupSwitchStmt)
                  addUsedValue(((LookupSwitchStmt)stmt).getKey(), thread, null, null);
            }
            forControl.put(thread, newl);
         }
      }

      /*@
       * check the data dependence
       * the current implementation has a leak: if some field of some un-tracked object can be directly changed,
       * the field access is treated as a different location from the owner as a whole.
       * for example, obj.x = ... and obj.m() (m is a non-pure method) are regarded as working on two different memory locations.
       * to be more precise, the latter should contain the former.
       */
      void checkData(Stmt stmt, IString thread, Event event, Layer layer) throws IOException{
         if (!(stmt instanceof DefinitionStmt) && !(stmt instanceof InvokeStmt) && !(stmt instanceof ReturnStmt))
            return;
         if (stmt instanceof DefinitionStmt){
            Value left = ((DefinitionStmt)stmt).getLeftOp();
            Value right = ((DefinitionStmt)stmt).getRightOp();
            if (replaceValue(left, right, thread, event, layer))
               forControl.get(thread).add(stmt);
         } else if (stmt instanceof InvokeStmt) {
            InvokeExpr expr = stmt.getInvokeExpr();
            if (!ma.isTracked(event.cls, expr.getMethod().getSubSignature()) && ! ma.isPure(expr.getMethodRef())) {
               if (usedFields.contains(event.cls + "@" + event.id)) {
                  addUsedValue(expr, thread, event, layer);
                  forControl.get(thread).add(stmt);
               }
            }
         } else if (stmt instanceof ReturnStmt){
            if (replaceValue(returnValue, ((ReturnStmt)stmt).getOp(), thread, event, layer))
               forControl.get(thread).add(stmt);
         }
      }
      /*@
       * replace the parameter reference inside a method with actual arguments
       */
      void replaceParameters(InvokeExpr expr, IString thread){
         HashSet<Value> tPara = parameters.get(thread);
         if (tPara != null) {
            HashSet<Value> locals = usedLocals.get(thread);
            Iterator it = tPara.iterator();
            while (it.hasNext()){
               ParameterRef p = (ParameterRef)it.next();
               Value v = expr.getArg(p.getIndex());
               locals.remove(p);
               locals.add(v);
            }
            tPara.clear();
         }
      }

      void addRelevant(Stmt stmt, Event event, Layer layer, Stack<Layer> stack, String field) throws IOException{
         // the read/write on the location also depends on the base and index stored in some local variables!
         Value value = null;
         if (stmt.containsArrayRef()){
            value = stmt.getArrayRef();
         } else if (stmt.containsFieldRef()){
            value = stmt.getFieldRef();
         } else if (stmt.containsInvokeExpr()){
            value = stmt.getInvokeExpr();
         } else
            return;
         forControl.get(event.thread).add(stmt);
         List boxes = value.getUseBoxes();
         Iterator boxIt = boxes.iterator();
         // since Jimple is a 3-address language, we only need to go down one level here
         while (boxIt.hasNext()){
            Value v = ((ValueBox)boxIt.next()).getValue();
            if (v instanceof Local)
               usedLocals.get(event.thread).add(v);
            else if (v instanceof ParameterRef){
               usedLocals.get(event.thread).add(v);
               parameters.get(event.thread).add((ParameterRef)v);
            }
         }

         // build the trace to the event
         StringBuffer buf = new StringBuffer();
         Iterator<Layer> it = stack.iterator();
         while (it.hasNext()){
            Layer l = it.next();
            if (l.stmt != null) {
               buf.append(getPosition(l));
               buf.append("=>");
            }
         }
         buf.append(getPosition(layer));
         FormalizedEvent e = new FormalizedEvent(event.thread, "rel" + tr.pos, buf.toString(), event.getLoc()+"."+field, event.isRead);
         tw.writeEvent(e);
      }

      String getPosition(Layer layer){
         return layer.method.getDeclaringClass().getName() + "." + layer.method.getSubSignature() + "@" + Util.extractLineNumber(layer.stmt);
      }

      void addThread(IString thread){
         forControl.put(thread, new ArrayList<Stmt>());
         usedLocals.put(thread, new HashSet<Value>());
         parameters.put(thread, new HashSet<Value>());
      }

      /*@
       * replace the statements of a method with a call to the method
       */
      void removeLocalStatements(SootMethod method, Stmt stmt, IString thread){
         ArrayList<Stmt> l = forControl.get(thread);
         if (l != null){
            boolean flag = false;
            Chain<Unit> units = method.getActiveBody().getUnits();
            for (int i = l.size(); i > 0; i --){
               Stmt st = l.get(i-1);
               if (units.contains(st)){
                  l.remove(i-1);
                  flag = true;
               } else
                  break;
            }
            if (flag)
               l.add(stmt);
         }
      }
   }
}
// vim: tw=100:sw=2
