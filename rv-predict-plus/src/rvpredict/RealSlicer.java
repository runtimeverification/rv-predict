package rvpredict;

import soot.Scene;
import soot.ValueBox;
import soot.Value;
import soot.Local;
import soot.Unit;
import soot.SootMethod;
import soot.util.IdentityHashSet;
import soot.jimple.Stmt;
import soot.jimple.IdentityStmt;
import soot.jimple.Constant;
import soot.jimple.BinopExpr;
import soot.jimple.UnopExpr;
import soot.jimple.IdentityRef;
import soot.jimple.Ref;
import soot.jimple.ParameterRef;
import soot.jimple.ThisRef;
import soot.jimple.CaughtExceptionRef;
import soot.jimple.ArrayRef;
import soot.jimple.StaticFieldRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.InvokeExpr;
import soot.jimple.AnyNewExpr;
import soot.jimple.CastExpr;

import soot.toolkits.scalar.SmartLocalDefs;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

import java.util.Map;
import java.util.HashMap;

import java.util.List;
import java.util.ArrayList;

import rvpredict.traces.BackwardTrace;
import rvpredict.logging.Protos;
import rvpredict.util.Util;
import rvpredict.util.Stack;
import rvpredict.analysis.ControlDependenceAnalyzer;

// HANDLE EXCEPTIONS
// Use size of stack frame to push onto the control dependence stack
// the proper number of frames

// Currently this will keep events that are data dep in the same thread
// that aren't strictly necessary (since data dependencies across threads
// are all the matter in the final slice... but we must transitively look
// for stuff, so this is easier).  The check would need to consider thread
// ID in addition to just Variables.  This is only a (probably slight)
// efficiency concern.  This will not affect correctness

abstract class RealSlicer extends Slicer {
  //we will rarely see stack depths over 25
  private static final int STACK_SIZE = 25;
  private boolean first = true;

  //Set of Variable READS that we are trying to find WRITES for (and any thread).  When we first start out this
  //will contain any Variable READS used by an Event that we are interested in.
  private Set<Protos.Variable> dependentWrites = new HashSet<Protos.Variable>();

  //The Integer key is for ThreadIDs.  Frame Stacks for each Thread.
  //As explained near the Frame definition at the bottom of the file.
  //A Frame aggregates the method dependant information we care about,
  //namely, a set of Stmts s for which we are looking for Branches that
  //control s; a set of Stmts that correspond to Variable WRITES
  //for which we need to find the READ (in the same thread) so that we
  //can glean the necessary ObjectIDs to create a Variable to add
  //to dependantWrites; and a set of Parameters for which we are waiting
  //on a method invocation Event to patch up
  //
  // Currently we never push or pop to these stacks because we can't
  // get our ends and begins straight at the moment.  This is a bug
  // and *potentially* cause problems
  private Map<Integer, Stack<Frame>> frameStacks = new HashMap<Integer, Stack<Frame>>();

  //Because we pop the stack frame BEFORE we will see the invocation call we keep 
  //around the set of parameters that was in the last frame popped.  There might
  //be a more elegant way to do this
  //
  //Obviously we need one per thread! 
  //
  //perhaps is enough to check methodBegin method == invokeMethod in the presense of call-backs?
  private Map<Integer, ParamMethodPair> lastDependentParameters = new HashMap<Integer, ParamMethodPair>();
  
  RealSlicer(final Iterator<Protos.Event> t) {
    super(t);
  }

  abstract protected void markImportant();

  /// MUST PUSH STACK FIRST,  FIRST EVENT MUST BE
  /// END SOMETHING
  @Override protected final Protos.Event process() {
    //System.out.println("processing: " + e.getType().toString());
//    if(first && (e.getType() != Protos.Event.Type.MethodEnd) 
//             && (e.getType() != Protos.Event.Type.ClassInitEnd)){ 
//            throw new RuntimeException(
//              "first event was a " + e.getType().toString() 
//           +  " when it should be the END of a method or class init.  This means the log was"
//           +  " incomplete. Most likely your program under test ended with an uncaught Exception,"
//           +  " System.exit(...), or you are using finializers and your JVM is buggy and stops"
//           +  " finalizers midstride.");
//         }
//    first = false;
    markImportant();

    //if any of the following methods return true we know we should
    //keep the event in the trace slice
    if (Main.showEventTypes) System.out.println("ID:" + e.getThreadId() + " " + e.getType());
    else if (Main.showEvents) System.out.println(e);

    boolean keepEvent = simplePassThrough() 
                     || handleAccess()
                     || handleCallerEnd()
                     || handleCallerBegin()
                     || handleCalleeEnd()
                     || handleCalleeBegin()
                     || handleBranch()
                     || handleException();
     //System.out.println("ID : " + e.getThreadId() + " stack : " + getStack(e.getThreadId())); 
     if(keepEvent){
       Stmt toAdd = Util.getStmt(e);
       if(toAdd != null) addDependentControl(e.getThreadId(), toAdd); 
       return e;
     }

     return null;
  }


  //Any events that pass through to each
  //slice with no computation go in this 
  //method
  // 
  //Technically Start and Join only need
  //to be passed through if the Thread
  //to which they are assigned actually
  //contains a relevent event
  private boolean simplePassThrough(){
    switch(e.getType()){
      case ReqClassInit:
      case Start:
      case Join:
      case Wait:
      case Notify:
      case Finalize:
      case Lock:
      case Unlock:
        //for now all of these are just passed through
        return true;
      case MOPEvent:
        if (e.getIsImportant())
          return true;
        else
          return false;
      default:
        return false;
    }
  }

  private boolean handleAccess() {
    if(e.getType() != Protos.Event.Type.Access) return false;
    //First check to see if this Event is a WRITE.  If it is a
    //WRITE we will create a new Variable to see if this is a WRITE 
    //to a Variable a previous Event READ.  If is a WRITE
    //we need to add all the Stmts that define its rhs using
    //canonicalizeUses (see canonicalizeUses for more info)
    if(e.getIsWrite()){
      //Recall that Set.remove returns boolean if
      //anything actually is removed, this saves us 
      //a lookup over checking Set.contains first.
      //We call remove first because it is important to
      //remove a Variable when a WRITE is first found (so that we
      //only end up with one WRITE)
      if(dependentWrites.remove(e.getVariable())){
        Frame frame = getStack(e.getThreadId()).peek();
       // System.out.println("ID: " + e.getThreadId() + " found Write: " + e.getVariable() 
         //                  + "\n\tdependentReads: " + frame.dependentReads 
           //                + "\n\tdependentWrites: " + dependentWrites);
        canonicalizeUses(); 
        return true; //add to slice
      }
    }
    //Else if e is a Read, check to see if any previous events
    //generated a Stmt corresponding to this Event that needed more 
    //info in order to create Variables (e.g., objectID, index) 
    //corresponding to this Event
    else { 
      Frame frame = getStack(e.getThreadId()).peek();
      Stmt stmt = Util.getStmt(e);
      if(frame.dependentReads.remove(stmt)){
        dependentWrites.add(e.getVariable());
     //   System.out.println("ID: " + e.getThreadId() + " found Read: " + stmt 
       //                    + "\n\tdependentReads: " + frame.dependentReads 
         //                  + "\n\tdependentWrites: " + dependentWrites);
        return true; //add to slice
      }
    }
    if(e.getIsImportant()){
     // System.out.println("ID: " + e.getThreadId() + " important: " + new Variable(e) + " stmt: " + Util.getStmt(e)); 
      if(e.getIsWrite()) canonicalizeUses(); 
      return true; //add to slice
    }
    return false; //this event is not necessary for the slice
  }

  //On an End Event we need to push a new frame (because this is backwards)
  //Yes, this method currently does nothing
  private boolean handleCallerEnd() {
    switch(e.getType()){
      case ConstructorEnd:
        return true;
      default : return false;
    }
  }

  //On a Begin Event we need to pop a frame (because this is backwards)
  private boolean handleCallerBegin() {
    switch(e.getType()){
      case Invoke:
      case InstanceInvoke:
        //System.out.println("instance patch");
        patchParameters();
        return true;
      case ConstructorBegin:
        //System.out.println("constructor patch");
        patchParameters();
        return true;
      default : return false;
    }
  }

  //classinit must be a callee because it is never explicitly called

  private boolean handleCalleeBegin() {
    switch(e.getType()) {
      case MethodBegin: 
        //System.out.println("method " + e);
        //popFrame(e.getThreadId());
        //return false;
      case ClassInitBegin:
        popFrame(e.getThreadId());
        return true;
    }
    return false;
  }

  private boolean handleCalleeEnd() {
    switch(e.getType()) {
      case MethodEnd: 
        pushFrame(e.getThreadId());
        return false;
      case ClassInitEnd:
        pushFrame(e.getThreadId());
        return true;  //only ClassInit and ConstructorEnd are added to the slice
    }
    return false;
  }

  private boolean handleBranch() {
    if(e.getType() != Protos.Event.Type.Branch) return false;
    Stmt branchStmt = Util.getStmt(e);
    ControlDependenceAnalyzer cda = Util.getCDA(e);
    Frame frame = getStack(e.getThreadId()).peek();
    for(Stmt stmt : frame.dependentControl){  //right now this set can grow to include every Stmt in
                                              //the program... probably won't overflow memory
      if(cda.dependOn(stmt, branchStmt)){
        //System.out.println("\t" + stmt + " depends on " + branchStmt);
        //System.out.println(frame.dependentReads);
        canonicalizeUses();  //add all the branches uses to the set of data dependences
        //System.out.println(frame.dependentReads);
        break;
      }
    }
    return false;
  }

  private boolean handleException(){
    if(e.getType() != Protos.Event.Type.Exception) return false;
    int trackedClasses = 0;
    for(String className : e.getClasstackList()){
      if(Scene.v().getSootClass(className).isApplicationClass()){
        ++trackedClasses;
      }
    }
  //  System.out.println(trackedClasses);
   // System.out.println(getStack(e.getThreadId()).size());
   // System.out.println("pre exception stack " + getStack(e.getThreadId()));
    pushFrames(e.getThreadId(), trackedClasses - getStack(e.getThreadId()).size()); 
    return false;
  }

  ////////////////////////////////////////
  //// Utilities                      ////
  ////                                ////
  ////////////////////////////////////////
 
  ////////////////////////////////////////
  //stack frame utilities               //
  ////////////////////////////////////////
  private Stack<Frame> getStack(final Integer threadID){
    Stack<Frame> frameStack = frameStacks.get(threadID);
    if(frameStack == null){ 
      frameStack = new Stack<Frame>(STACK_SIZE);  
      frameStacks.put(threadID, frameStack);
      //because main never has an end inserted we start each new Thread with a one element stack
  //    pushFrame(threadID); 
      pushFrames(threadID, Main.initialStackDepth);
    }
    return frameStack;
  }

  private void pushFrames(final Integer threadID, final int numToPush){
    for(int i = 0; i < numToPush; ++i){
      pushFrame(threadID);  
    }
  }

  private void pushFrame(final Integer threadID){
    Stack<Frame> frameStack = getStack(threadID);
    frameStack.push(new Frame());
   // System.out.println("ID: " + threadID + " --- " + e.getMethodName() +" push " + frameStack);
  }

  private void popFrame(final Integer threadID){
    popFrames(threadID,1);
    //System.out.println("ID: " + threadID + " " + frameStacks.get(threadID));
  }

  //I don't think this will ever be used
  private void popFrames(final Integer threadID, final int numToPop){
    Stack<Frame> frameStack = getStack(threadID);
    lastDependentParameters.put(threadID, frameStack.pop(numToPop).dependentParameters);
    if(frameStack.peek().dependentParameters.m != null){
      //System.out.println("popping m" + frameStack.peek().dependentParameters.m);
      //System.out.println("popping params" + frameStack.peek().dependentParameters.params);
    }
    //System.out.println("ID: " + threadID + "----" + e.getMethodName() + " params " + lastDependentParameters);
    //System.out.println("ID: " + threadID + "----" + e.getMethodName() + " pop " + frameStack);
  }

  private void addDependentRead(final Integer threadID, final Stmt read){
    Frame frame = getStack(threadID).peek();
    frame.dependentReads.add(read); 
    //System.out.println("ID: " + e.getThreadId() + " adding: " + read + " to dependentReads: " + frame.dependentReads);
  } 

  private void addDependentControl(final Integer threadID, final Stmt stmt){
    Frame frame = getStack(threadID).peek();
    frame.dependentControl.add(stmt); 
  } 
  
  private void addDependentParameter(final Integer threadID, final ParameterRef parameter){
    Frame frame = getStack(threadID).peek();
    frame.dependentParameters.params.add(parameter.getIndex()); 
    frame.dependentParameters.m = Util.getSootMethod(e);
    //System.out.println("ID: " + e.getThreadId() + " adding parameter: " + parameter 
      //               + " to dependentParameters: " + frame.dependentParameters);
  } 

  /////////////////////////////////////////
  //canonicalization utilities           //
  /////////////////////////////////////////
  
  //Will patch up parameter uses found in a method body with the
  //actual local arguments used in Invocation instruction
  //
  //Note that EVERY invoke event must come immediately after its previous
  //methodBegin event, but there may not be an invoke event for every
  //methodBegin.  Because there must always be a begin immediately before
  //and invoke in the trace, we can never accidentally associate the wrong
  //parameters set with an invoke 
  private void patchParameters(){
    //System.out.println("patching");
    Frame frame = getStack(e.getThreadId()).peek();    
    //if we aren't dependent on any parameters, no use doing the rest of this
    //stuff
    ParamMethodPair pair = lastDependentParameters.get(e.getThreadId());
    if(pair == null) return;
    Set<Integer> params = pair.params;
    if(params.size() == 0) return;

    //System.out.println("patching invoke :" + Util.getStmt(e));

    Stmt invokeStmt = Util.getStmt(e);
    assert invokeStmt != null : "patchParameters even does not have an associated Stmt! " + e;
    assert invokeStmt.containsInvokeExpr() : "patchParameters called on a non invocation event! " + e;
    
    InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
    //System.out.println("invoked: " + invokeExpr.getMethod());
    //System.out.println("invokee: " + lastDependentParameters.get(e.getThreadId()).m);
    List<ValueBox> args = new ArrayList<ValueBox>();
    for(Integer paramNum : lastDependentParameters.get(e.getThreadId()).params){ 
      //System.out.println(invokeExpr);
      if(paramNum > invokeExpr.getArgCount()) return; //this is defense against a possible
                                                    //intrumentation bug, should assert this later
      args.add(invokeExpr.getArgBox(paramNum));
    }
    //System.out.println("Can ARGS: " + args);
    canonicalizeUses(args);
    //set the last parameters to this thead to null in case we see
    //an invoke event for an untracked method before we see our
    //next method begin
    lastDependentParameters.put(e.getThreadId(), null);
  }


  //This will find the first field, array value, or parameter 
  //in a chain of Locals.  The idea is to locate those Stmts
  //which we need to wait for Events for.  When the Event
  //arrive later we may correctly generate a Variable to 
  //look for interthread dependences
  private void canonicalizeUses(List<ValueBox> values){
    for(ValueBox vb : values){
      Value v = vb.getValue();
      if(v instanceof Local) {
        canonicalizeLocal((Local) v);
      }
      else if(v instanceof Ref){
        handleCurrentRef((Ref) v);
      }
      else {
        assert (v instanceof InvokeExpr) || (v instanceof BinopExpr)
            || (v instanceof Constant)   || (v instanceof AnyNewExpr) 
            || (v instanceof CastExpr)   || (v instanceof UnopExpr) : v + " unhandled expression type?";
      }
    }
  }

  //convenience method
  private void canonicalizeUses(){
    Stmt stmt = Util.getStmt(e);
    List<ValueBox> values = stmt.getUseBoxes();
    canonicalizeUses(values);
  }

  //This is where we actually do the def/use chasing
  private void canonicalizeLocal(final Local l){
    //System.out.println("local " + l);
    //get a copy of the local defs object for this method
    //we use this to trace back through the def/use chains
    SmartLocalDefs sld = Util.getDefs(e);
    List<Unit> defs = sld.getDefsOfAt(l, Util.getStmt(e.getLocation()));
    Set<Unit> workList = new HashSet<Unit>(defs);
    //don't investigate Locals we've already seen, such can lead to infinite loops
    Set<Local> seen = new HashSet<Local>();
    int i = 0;
    while(!workList.isEmpty()){
      //grab a statement from the workList (unit), we need to convert ITS
      //uses to the units corresponding to their defs and add those
      //obviously, we need to remove the current statement itself
      Unit currentUnit = workList.iterator().next();
      workList.remove(currentUnit);
      for(ValueBox useBox : currentUnit.getUseBoxes()){
        Value v = useBox.getValue();
        if(v instanceof Local) {
          Local local = (Local) v;
          if(seen.contains(local)) continue;
          seen.add(local);
          workList.addAll(sld.getDefsOfAt(local, currentUnit));
        }
        else if(v instanceof Ref){
          //here we do not want to use handleCurrentRef corresponds to a 
          //Ref who's Event we are yet to see.  (note that future means
          //future Event, this is past as far as the original program is concerned
          //because this is backwards)
          handleFutureRef((Ref) v, currentUnit);
        }
        else {
          assert (v instanceof InvokeExpr) || (v instanceof BinopExpr)
              || (v instanceof Constant)   || (v instanceof AnyNewExpr) 
              || (v instanceof CastExpr)   || (v instanceof UnopExpr) : v + " unhandled expression type?";
        }
      }
    }
  }

  //handleCurrentRef handles a Ref in the CURRENT Event
  private void handleCurrentRef(final Ref r){
    //Only field and array refs should actually be possible
    //in an Event
    //
    //Parameters must be assigned to Locals and can thus not 
    //show up in a Current Event  (same with ThisRef)
    if(    (r instanceof StaticFieldRef) || (r instanceof InstanceFieldRef) 
        || (r instanceof ArrayRef) ){
      //this Event must have been a READ Event we were looking for
      //add a new Variable corresponding to it to the
      //set of Variables waiting on WRITES
      dependentWrites.add(e.getVariable());
    }
    else {
      assert false : "Current Ref type of " + r + " not handled";
    }
  }

  //handleFutureRef handles a Ref relating to a FUTURE Event
  //(note that this is really a past Event as far as the original
  //program is concerned because this is backwards).
  //Parameters are kept in their own Set in the Frame
  private void handleFutureRef(final Ref r, final Unit currentUnit){
    if(currentUnit instanceof Stmt) {
      //Parameters can show up here
      if(r instanceof ParameterRef){
        addDependentParameter(e.getThreadId(), (ParameterRef) r);
      }
      else if(    (r instanceof StaticFieldRef) || (r instanceof InstanceFieldRef) 
               || (r instanceof ArrayRef)  ){
        addDependentRead(e.getThreadId(), (Stmt) currentUnit);
      }
      else if(r instanceof ThisRef){} //we might want to get the actual 
                                      //ObjectID for This at some
                                      //point
      else {
        assert false : "Future Ref type of " + r + " not handled";
      }
    } 
    else {
      assert false : "How is the current Unit " + currentUnit + " not a Stmt?";
    } 
  }

}

//This class bundles all the information we need during a given method invocation
class Frame {
  final Set<Stmt> dependentReads = new HashSet<Stmt>();
  final Set<Stmt> dependentControl = new HashSet<Stmt>();
  final ParamMethodPair dependentParameters 
    = new ParamMethodPair();  
}

class ParamMethodPair {
  public SootMethod m;
  public Set<Integer> params = new HashSet<Integer>();
}

// vim: tw=100:sw=2
