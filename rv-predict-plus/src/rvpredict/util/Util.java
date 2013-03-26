package rvpredict.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import rvpredict.analysis.ControlDependenceAnalyzer;
import rvpredict.analysis.MethodAnalyzer;

import rvpredict.prediction.generic.FormalizedEvent;
import rvpredict.logging.Protos;
import rvpredict.Variable;
import rvpredict.Location;

import soot.Body;
import soot.SootMethod;
import soot.Unit;
import soot.Scene;

import soot.tagkit.LineNumberTag;
import soot.tagkit.Tag;
import soot.util.Chain;

import soot.jimple.GotoStmt;
import soot.jimple.Stmt;

import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.SmartLocalDefs;
import soot.toolkits.scalar.LiveLocals;
import soot.toolkits.scalar.SimpleLiveLocals;

import com.whirlycott.cache.Cache;
import com.whirlycott.cache.CacheConfiguration;
import com.whirlycott.cache.CacheManager;
import com.whirlycott.cache.CacheException;


public class Util {

  private static Cache cache;

  static {
    try {
    	
      cache = CacheManager.getInstance().getCache();
    } catch (CacheException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  public static String prettyPrint(final Protos.Variable v) {
    switch (v.getType()) {
      case FieldAcc:
        return v.getClassName()+"."+v.getFieldName()+" (instance #"+v.getObjectID()+")";
      case StaticFieldAcc:
        return v.getClassName()+"."+v.getFieldName();
      case ArrayAcc:
        return v.getClassName()+"["+v.getIndex()+"] (instance #"+v.getObjectID()+")";
      case ImpureCall:
        return v.getClassName()+" (instance #"+v.getObjectID()+")";
      default:
        assert false: "Tried to pretty print unsupported variable type, "+v.getType();
        System.exit(1);
        return null;
    }
  }

  //get the Stmt corresponding do a given event
  //If the Stmt cannot be found we return null
  public static Stmt getStmt(Protos.Event e){
    if(!e.hasLocation()) return null;
    return getStmt(e.getLocation());
  }

  //This should always work because we have a valid Location
  //If it doesn't we aren't logging Locations correctly
  public static Stmt getStmt(Protos.Location l) {
    //System.out.println("getting location" + l);
    Stmt ret;     
    String mangledName = l.getClassName() + " " + l.getMethodName();
    Object body = cache.retrieve(mangledName);
    if(body == null || !(body instanceof ArrayList)) {
       SootMethod m = getSootMethod(l);
       body = new ArrayList<Unit>(getSootMethod(l).getActiveBody().getUnits());
       cache.store(mangledName, body);
    }
    //System.out.println(body);
    try {
      Object stmtObj = ((ArrayList)body).get(l.getJimpleLine());//Jeff: BUG here, may throw IndexOutOfBoundExeption
    if(stmtObj instanceof Stmt) return (Stmt) stmtObj;
    else return null;
    } catch (ArrayIndexOutOfBoundsException e){
      System.out.println(body);
      throw e;
    }
  }

  public static Stmt getStmt(final Location l) { 
    Stmt ret;     
    String mangledName = l.getClassName() + " " + l.getMethodName();
    Object body = cache.retrieve(mangledName);
    if(body == null || !(body instanceof ArrayList)) {
       SootMethod m = getSootMethod(l);
       body = new ArrayList<Unit>(getSootMethod(l).getActiveBody().getUnits());
       cache.store(mangledName, body);
    }
    Object stmtObj = ((ArrayList)body).get(l.getJimpleLine());
    if(stmtObj instanceof Stmt) return (Stmt) stmtObj;
    else return null;
  }

  //If the SootMethod cannot be found we return null 
  public static SootMethod getSootMethod(Protos.Event e) {
    if(!e.hasLocation()) return null;
    return getSootMethod(e.getLocation());
  }

  //This should always work because we have a valid Location
  //If it doesn't we aren't logging Locations correctly
  public static SootMethod getSootMethod(Protos.Location l) {
    return Scene.v().getSootClass(l.getClassName()).getMethod(l.getMethodName());
  }
  public static SootMethod getSootMethod(final Location l) {
    return Scene.v().getSootClass(l.getClassName()).getMethod(l.getMethodName());
  }

  //get the local defs object for the enclosing method of a given event
  public static SmartLocalDefs getDefs(Protos.Event e){
    Protos.Location l = e.getLocation();
    String mangledName = l.getClassName() + " " + l.getMethodName() + "<defs>";
    SmartLocalDefs sld = (SmartLocalDefs) cache.retrieve(mangledName);  
    if(sld == null){
      ExceptionalUnitGraph g 
        = new ExceptionalUnitGraph(Scene.v().getSootClass(l.getClassName()).getMethod(l.getMethodName()).getActiveBody());
      LiveLocals ll = new SimpleLiveLocals(g);
      sld = new SmartLocalDefs(g,ll);
      cache.store(mangledName, sld);
    }
    return sld;
  } 
  
  //get the control depdendence for a given event
  public static ControlDependenceAnalyzer getCDA(Protos.Event e){
    MethodAnalyzer ma;
    ma = MethodAnalyzer.v();
    Protos.Location l = e.getLocation();
    SootMethod m = Scene.v().getSootClass(l.getClassName()).getMethod(l.getMethodName());
    ControlDependenceAnalyzer cda = (ControlDependenceAnalyzer)cache.retrieve(m);
    if(cda == null){
      cda = new ControlDependenceAnalyzer(m.getActiveBody(), ma);
      cache.store(m, cda);
    }
    return cda;
  }

  public static <T,A extends T, B extends T> Set<T> intersect(final Set<A> a, final Set<B> b) {
    Set<T> ret = new HashSet<T>(a);
    ret.retainAll(b);
    return ret;
  }

  public static <T,A extends T, B extends T> boolean disjoint(final Set<A> a, final Set<B> b) {
    return intersect(a,b).isEmpty();
  }

  // GotoStmts can point to other GotoStmts, we want to find the last GotoStmt in a chain of them
  public static GotoStmt getFinalGoto(GotoStmt s) {
    if (s.getTarget() instanceof GotoStmt)
      return getFinalGoto((GotoStmt)s.getTarget());
    else
      return s;
  }

  public static String seperator = "~";

  public static int extractLineNumber(final Protos.Location l) {
    return extractLineNumber(getStmt(l));
  }

  public static int extractLineNumber(final Location l) {
    return extractLineNumber(getStmt(l));
  }

  public static int extractLineNumber(Unit stmt) {
    LineNumberTag tag = (LineNumberTag)stmt.getTag("LineNumberTag");
    return (tag == null)?-1:tag.getLineNumber();
  }

  public static void addJimpleLineTags(Body body){
    if (body.hasTag("JimpleLineTag"))
      return;
    Chain<Unit> units = body.getUnits();
    Iterator<Unit> stmtIt = units.iterator();
    int jimpleLine = 0;
    while (stmtIt.hasNext()){
      Stmt stmt = (Stmt)stmtIt.next();
      if (! stmt.hasTag("JimpleLineTag")){
        JimpleLineTag tag = new JimpleLineTag(jimpleLine);
        stmt.addTag(tag);
      }
      jimpleLine ++;
    }
    body.addTag(new JimpleLineTag(-1));
  }

  public static int getJimpleLine(Stmt stmt){
    JimpleLineTag tag = ((JimpleLineTag)stmt.getTag("JimpleLineTag"));
    return tag == null? -1 : tag.jimpleLine;
  }
 
  public static ControlDependenceAnalyzer getCDA(SootMethod m, MethodAnalyzer ma){
    CDATag tag = (CDATag)m.getTag("CDATag");
    if (tag == null){
      ControlDependenceAnalyzer cda = new ControlDependenceAnalyzer(m.getActiveBody(), ma);
      tag = new CDATag(cda);
      m.addTag(tag);
    }
    return tag.cda;
  }

  public static boolean isInConstructor(FormalizedEvent e){
    int i = e.loc.indexOf("@");
    String cls = null;
    if (i < 0)
      i = e.loc.indexOf("!");
    cls = e.loc.substring(0, i);
    if (e.pos.indexOf(cls + ".void <init>") > -1)
      return true;
    return false;
  }

  static class CDATag implements Tag{
    ControlDependenceAnalyzer cda;
    public CDATag(ControlDependenceAnalyzer cda){
      this.cda = cda;
    }
    public String getName(){
      return "CDATag";
    }
    public byte[] getValue(){
      return null;
    }
  }

  static class JimpleLineTag implements Tag{
    public int jimpleLine = 0;
    public JimpleLineTag(int i){
      jimpleLine = i;
    }
    public String getName(){
      return "JimpleLineTag";
    }
    public byte[] getValue(){
      return null;
    }
  }

  static public String makeLogFileName(String workDir, String tag, String name){
    return workDir + File.separator + Configure.getString(tag) + "_" + name.replace('.','_');
  }
  static public String makeLogFileName(String tag, String name){
    return Configure.getString(tag) + "_" + name.replace('.','_');
  }

  /*
   * the function to filter out possibly redudant shared variables
   */

  static public HashSet<String> filter(HashSet<String> vars, String str){
    return filterByNames(filter(vars), str);
  }

  static public HashSet<String> filter(HashSet<String> vars){
    HashSet<String> results = new HashSet<String>();
    HashMap<String, ArrayList<String>> majors = new HashMap<String, ArrayList<String>>();
    for (String str : vars){
      ParsedVar var = new ParsedVar(str);
      ArrayList<String> l = majors.get(var.getMajor());
      if (l == null){
        l = new ArrayList<String>();
        majors.put(var.getMajor(), l);
      }
      l.add(str);
    }
    Random r = new Random();
    for (String m : majors.keySet()){
      ArrayList l = majors.get(m);
      int i = r.nextInt(l.size());
      results.add((String)l.get(i));
    }
    return results;
  }

  static public HashSet<String> filterByNames(HashSet<String> vars, String str){
    if ((str == null) || (str.length() ==0))
      return vars;
    String[] names = str.split(";");
    HashSet<String> results = new HashSet<String>();
    for (String var : vars){
      boolean flag = false;
      for (String s : names){
        if (var.contains(s)){
          flag = true;
          break;
        }
      }
      if (flag)
        results.add(var);
    }
    return results;
  }

  static public void main(String args[]){
    HashSet<String> shared = new HashSet<String>();
    try {
      BufferedReader r = new BufferedReader(new FileReader(args[0]));
      String line = r.readLine();
      while(line != null){
        if (line.length() > 0){
          shared.add(line.trim());
        }
        line = r.readLine();
      }
      HashSet<String> filtered = filter(shared);
      System.out.println(filtered.size() + " left:");
      Iterator<String> it = filtered.iterator();
      while (it.hasNext()){
        System.out.println(it.next());
      }
    } catch (Exception e){
      e.printStackTrace();
      System.exit(1);
    }
  }

  static class ParsedVar {
    final static int FIELD = 10;
    final static int OBJECT = 20;
    final static int ARRAY = 30;
    final static int CLASS = 40;

    public int type;
    public String cls;
    public String id;
    public String index;

    public ParsedVar(String str){
      if (str.indexOf("!")>-1) {
        type = CLASS;
        cls = str;
      } else {
        int i = str.indexOf("@");
        cls = str.substring(0, i);
        str = str.substring(i+1);
        i = str.lastIndexOf(".");
        if (i < 0){
          type = OBJECT;
          id = str;
        } else {
          if (cls.startsWith("["))
            type = ARRAY;
          else
            type = FIELD;
          id = str.substring(0, i);
          index = str.substring(i+1);
        }
      }
    }

    String getMajor(){
      switch (type){
      case FIELD:
        return cls + "." + index;
      case OBJECT:
        return cls;
      case ARRAY:
        return cls + "@" + id;
      case CLASS:
        return cls;
      }
      return "";
    }
    String getFull(){
      if (type == CLASS)
        return cls;
      String str = cls + "@" + id;
      if (index != null)
        str += "." + index;
      return str;
    }
  }

}
// vim: tw=100:sw=2
