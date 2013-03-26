package rvpredict.prediction.atomicity;

import rvpredict.PredictorException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Trap;
import soot.Unit;

import soot.jimple.EnterMonitorStmt;
import soot.jimple.ExitMonitorStmt;
import soot.jimple.FieldRef;
import soot.jimple.GotoStmt;
import soot.jimple.IfStmt;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.JimpleBody;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.Stmt;
import soot.jimple.TableSwitchStmt;
import soot.jimple.ThrowStmt;

import soot.util.Chain;

import rvpredict.prediction.generic.Event;
import rvpredict.prediction.generic.Preprocessor;
import rvpredict.prediction.generic.SharedVarHandler;
import rvpredict.prediction.generic.Stage;

import rvpredict.prediction.io.TraceReader;
import rvpredict.util.Configure;
import rvpredict.util.Util;

public class AtomicBlockFinder extends Stage {
  TraceReader tr;
  HashSet toCheck;
  ArrayList<AtomicBlockDef> blockDefinitions;
  HashMap<HashSet<String>, ArrayList<AtomicBlock>> atomicBlocks = new HashMap<HashSet<String>, ArrayList<AtomicBlock>>();

  public AtomicBlockFinder() throws PredictorException{
    super("searching for atomic blocks");
    try {
      tr = new TraceReader(workDir + File.separator + Configure.getString("TmpTraceFile") + ".rvpf");
      this.toCheck = SharedVarHandler.readToCheck();
      blockDefinitions = new ArrayList<AtomicBlockDef>();
      BufferedReader reader = new BufferedReader(new FileReader(workDir + File.separator + Configure.getString("AtomicDefFile") + ".rvpf"));
      String aline = reader.readLine();
      while (aline != null){
        blockDefinitions.add(new AtomicBlockDef(aline));
        aline = reader.readLine();
      }
      if (blockDefinitions.isEmpty())
        throw new PredictorException("No given atomic block definition");
    } catch (Exception e){
      throw PredictorException.report("AtomicBlockFinder", e);
    }
  }

  public void process() throws PredictorException{
    try {
      Event event = tr.next();
      HashMap<String, Stack<Layer>> tMap = new HashMap<String, Stack<Layer>>();
      /*
       * We assume for every thread, there is at most one atomic block at a time.
       * If an atomic block is embedded in another one, we only check the outmost one
       */
      HashMap<String, AtomicBlock> bMap = new HashMap<String, AtomicBlock>();// map threads into the current atomic block
      String currThread = "";
      Layer currLayer = null;
      AtomicBlock currAtomicBlock = null;
      while (event != null) {
        if (event.thread.toString().compareTo(currThread)!=0){
          Stack<Layer> stack = null;
          if (currLayer != null) {
            stack = tMap.get(currThread);
            stack.push(currLayer);
          }
          stack = tMap.get(event.thread.toString());
          if (stack == null){
            stack = new Stack<Layer>();
            stack.push(new Layer());
            tMap.put(event.thread.toString(), stack);
          }
          currLayer = stack.pop();
          if (bMap.get(event.thread.toString()) == null) {
            currAtomicBlock = new AtomicBlock();
            currAtomicBlock.setThread(event.thread.toString());
            bMap.put(event.thread.toString(), currAtomicBlock);
          } else
            currAtomicBlock = bMap.get(event.thread.toString());
          currThread = event.thread.toString();
        }
        boolean moveEvent = false;
        boolean moveStmt = false;

        if (isBlockBegin(event)){
          currAtomicBlock.setStart(tr.pos);
        }

        if (isBlockEnd(event)){
          currAtomicBlock.setEnd(tr.pos);
          if (currAtomicBlock.isDone()){
            ArrayList<AtomicBlock> blockList = atomicBlocks.get(currAtomicBlock.locations);
            if (blockList == null) {
              blockList = new ArrayList<AtomicBlock>();
              atomicBlocks.put(currAtomicBlock.locations, blockList);
            }
            blockList.add(currAtomicBlock);
            currAtomicBlock = new AtomicBlock();
            currAtomicBlock.setThread(event.thread.toString());
            bMap.put(currThread, currAtomicBlock);
          }
        }

        // no statement will be matched to the beginning of a method, so it needs to be handled first
        /*
         * the begin event is allowed in three cases:
         * 1) we are on the top level of the thread;
         * 2) the method is <clinit>;
         * 3) an untracked method invocation was encountered right before this event (also see below for the invoke statement
         */

        if (event.eventType == Event.BEGIN){
          currLayer = addNewLayer(currThread, tMap, currLayer, event);
          moveEvent = true;
        } else if (event.eventType == Event.END) {
          currLayer = tMap.get(currThread).pop();
          moveEvent = true;
        } else if (event.eventType == Event.EXENTER) {
          // when this event is hit, we should have arrived the layer that handles this exception;
          // because necessary end events have been inserted before this event
          Chain traps = currLayer.method.getActiveBody().getTraps();
          Iterator trapIt = traps.iterator();
          while (trapIt.hasNext()){
            Trap trap = (Trap)trapIt.next();
            Unit target = trap.getHandlerUnit();
            if (event.ln == Util.extractLineNumber(target)){
              currLayer.stmt = (Stmt)target;
              break;
            }
          }
          if (event.ln != Util.extractLineNumber(currLayer.stmt))
            throw new PredictorException("Cannot find the exception handler for " + event);
          moveEvent = true;
        } else if (((event.eventType == Event.ENTER) || (event.eventType == Event.IFENTER))&& (event.ln == Util.getJimpleLine(currLayer.stmt))){
          moveEvent = true;
        } else {
          Stmt next = null;
          if (currLayer.stmt.containsInvokeExpr()){
            InvokeExpr expr = currLayer.stmt.getInvokeExpr();
            SootMethod method = expr.getMethod();

            if (event.eventType != Event.FIELD) // note if the target is null, no call method will be logged!
              throw new PredictorException("A field access (via untracked method call)/method call/exception event on " + event.cls + " is expected!");
            if (! ma.isTracked(event.cls, method.getSubSignature())){
              String loc = null;
              if (expr instanceof InstanceInvokeExpr){
                // treated as a read/write on the object
                loc = event.cls + "@" + event.id;
              } else {
                // this method is treated as a read/write on the class
                loc = event.cls + "!";
              }
              if (toCheck.contains(loc)){
                currAtomicBlock.addLocation(loc);
              }
            }
            moveStmt = true;
            moveEvent = true;
          } else if (currLayer.stmt instanceof IfStmt) {
            if (event.eventType != Event.ENTER)
              throw new PredictorException("An ENTER event is expected!");
            next = Preprocessor.getTarget(((IfStmt)currLayer.stmt).getTarget());
            if (event.ln != Util.getJimpleLine(next))
              next = Preprocessor.getTarget((Stmt)currLayer.units.getSuccOf(currLayer.stmt));
            if (event.ln != Util.getJimpleLine(next))
              throw new PredictorException("The line number of the enter event cannot match any target!");
            currLayer.stmt = next;
            moveEvent = true;
          } else if ((currLayer.stmt instanceof LookupSwitchStmt) || (currLayer.stmt instanceof TableSwitchStmt)){
            if (event.eventType != Event.ENTER)
              throw new PredictorException("An ENTER event is expected!");
            if (currLayer.stmt instanceof LookupSwitchStmt){
              LookupSwitchStmt lswitch = (LookupSwitchStmt)currLayer.stmt;
              int count = lswitch.getTargetCount();
              for (int i = 0; i < count; i ++){
                next = Preprocessor.getTarget((Stmt)lswitch.getTarget(i));
                if (event.ln == Util.getJimpleLine(next))
                  break;
              }
              if (event.ln != Util.getJimpleLine(next))
                next = Preprocessor.getTarget((Stmt)lswitch.getDefaultTarget());
            } else if (currLayer.stmt instanceof TableSwitchStmt){
              TableSwitchStmt lswitch = (TableSwitchStmt)currLayer.stmt;
              int count = lswitch.getTargets().size();
              for (int i = 0; i < count; i ++){
                next = Preprocessor.getTarget((Stmt)lswitch.getTarget(i));
                if (event.ln == Util.getJimpleLine(next))
                  break;
              }
              if (event.ln != Util.getJimpleLine(next))
                next = Preprocessor.getTarget((Stmt)lswitch.getDefaultTarget());
            }
            if (event.ln != Util.getJimpleLine(next))
              throw new PredictorException("The line number of the enter event cannot match any target!");
            currLayer.stmt = next;
            moveEvent = true;
          } else if (currLayer.stmt instanceof GotoStmt){
            next = (Stmt)((GotoStmt)currLayer.stmt).getTarget();
            currLayer.stmt = next;
          } else if ((currLayer.stmt instanceof EnterMonitorStmt) || (currLayer.stmt instanceof ExitMonitorStmt)){
            if ((event.eventType != Event.LOCK) && (event.eventType != Event.UNLOCK))
              throw new PredictorException("A lock event is needed for enter/exit monitor statement!");
            moveEvent = true;
            moveStmt = true;
          } else if (currLayer.stmt instanceof ThrowStmt) {
            if (event.eventType != Event.EXENTER)
              throw new PredictorException("An exception event on is expected!");
            moveEvent = true;
          }  else if (currLayer.stmt.containsFieldRef()){
            if (event.eventType != Event.FIELD)
              throw new PredictorException("A field access event is expected!");
            FieldRef ref = currLayer.stmt.getFieldRef();
            String loc = event.getLoc() + "." + ref.getField().getName();
            if (toCheck.contains(loc))
              currAtomicBlock.addLocation(loc);
            moveEvent = true;
            moveStmt = true;
          }  else if (currLayer.stmt.containsArrayRef()){
            if (event.eventType != Event.ELEMENT)
              throw new PredictorException("An element access event is expected!");
            String loc = event.cls + "@" + event.id + "." + event.index;
            if (toCheck.contains(loc))
              currAtomicBlock.addLocation(loc);
            moveEvent = true;
            moveStmt = true;
          } else
            moveStmt = true;
        }

        // post-analysis handling
        if (moveEvent){
          do {
            event = tr.next();
          } while ((event != null) && (event.eventType == Event.CLINIT || event.eventType == Event.IFENTER));
        }
        if (moveStmt){
          currLayer.stmt = (Stmt)currLayer.units.getSuccOf(currLayer.stmt);
        }
      }
      post_process(tr.pos + 1);
    } catch (Exception e){
      throw PredictorException.report("AtomicFinder, trace #" + tr.pos, e);
    }
  }

  void post_process(long total) throws Exception{
    BufferedWriter writer = new BufferedWriter(new FileWriter(workDir + File.separator + Configure.getString("AtomicBlockFile")  + ".rvpf" ));
    Iterator<HashSet<String>> it = atomicBlocks.keySet().iterator();
    while (it.hasNext()){
      HashSet<String> locations = it.next();
      if (locations.size() > 0){
        writer.write(locations.toString() + "\n");
        ArrayList<AtomicBlock> blockList = atomicBlocks.get(locations);
        for (AtomicBlock b:blockList){
          // this is needed, because slicer works in a backward manner
          b.adjust(total);
          writer.write(b.toString() + "\n");
        }
        writer.write("--------------------------\n");
      }
    }
    writer.close();
  }

  Layer addNewLayer(String currThread, HashMap<String, Stack<Layer>> tMap, Layer currLayer, Event event) throws PredictorException {
    // get the correponding stack
    Stack<Layer> stack = tMap.get(currThread);
    // push the current layer
    stack.push(currLayer);
    // find the method to begin
    SootClass mainClass = Scene.v().getSootClass(event.cls.toString());
    SootMethod m = mainClass.getMethod(event.id.toString());
    // create a new current layer with the new method and the first statement of the method
    Body body = m.getActiveBody();
    if (!(body instanceof JimpleBody))
      throw new PredictorException("Wrong body!");
    // attach count to each statement of the method
    Util.addJimpleLineTags(body);
    Layer layer = new Layer();
    layer.method = m;
    layer.units = body.getUnits();
    layer.stmt = (Stmt)layer.units.getFirst();
    return layer;
  }

  boolean isBlockBegin(Event e){
    for (AtomicBlockDef def:blockDefinitions){
      if (def.matchBegin(e))
        return true;
    }
    return false;
  }

  boolean isBlockEnd(Event e){
    for (AtomicBlockDef def:blockDefinitions){
      if (def.matchEnd(e))
        return true;
    }
    return false;
  }

  class Layer {
    Stmt stmt;
    Chain<Unit> units;
    SootMethod method;
  }

  class AtomicBlockDef {
    static final int METHOD = 10;

    String id;
    int type;

    AtomicBlockDef (String str) throws PredictorException {
      if (str.startsWith("method:")){
        type = METHOD;
        id = str.substring(str.indexOf(":") + 1).trim();
      } else
        throw new PredictorException("Unsupported Atomic Block Definition!");
    }

    boolean matchBegin(Event e){
      if ((type == METHOD) && (e.eventType == Event.BEGIN)){
        if (id.compareTo(e.cls + "~" + e.id) == 0)
          return true;
      }
      return false;
    }
    boolean matchEnd(Event e){
      if ((type == METHOD) && (e.eventType == Event.END)){
        if (id.compareTo(e.cls + "~" + e.id) == 0)
          return true;
      }
      return false;
    }
  }

}
// vim: tw=100:sw=2
