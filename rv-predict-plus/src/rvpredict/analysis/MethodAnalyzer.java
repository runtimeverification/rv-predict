package rvpredict.analysis;

import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;

import rvpredict.PredictorException;

import rvpredict.prediction.PredictorOptions;

import rvpredict.util.Configure;
import rvpredict.util.IString;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import java.io.FileReader;
import java.io.BufferedReader;

/*@
 * the class analyzing the purity of methods
 */
public class MethodAnalyzer {
  public static enum Purity {
    SAFE,
      AGGRESIVE,
      INTERACTIVE;
  }
	
  static MethodAnalyzer ma = null;

  static {
    ma = new MethodAnalyzer();
    try {
      ma.addList(Configure.getString("SystemPureMethods"));
    } catch (PredictorException e){
      e.printStackTrace();
      System.exit(1);
    }
  }
 	
  public static MethodAnalyzer v() {
    return ma;
  }
	
  HashMap<String,HashSet<String>> class2Methods;
	
  public MethodAnalyzer(){
    class2Methods = new HashMap<String,HashSet<String>>();
    isTrackedCache = new HashMap<IString,HashSet<String>>();
  }

  // the format of the file should be: 
  //^full_classname
  //+unpure_method_signature
  //-pure_method_signature
  //+* means all other methods are non-pure (this is also the default assumpation
  //-* means all other methods are pure
  public void addList(String path) throws PredictorException{
    try {
      BufferedReader reader = new BufferedReader(new FileReader(path));
      String aLine = reader.readLine();
      String cls = null;
      HashSet<String> methods = null;
      while (aLine != null){
        if (aLine.startsWith("^")){
          if ((cls != null) && (methods != null)){
            class2Methods.put(cls, methods);
          }
          cls = aLine.substring(1).trim();
          methods = class2Methods.get(cls);
          if (methods == null)
            methods = new HashSet<String>();
        } else if (aLine.startsWith("+") || aLine.startsWith("-"))
          methods.add(aLine.trim());
        aLine = reader.readLine();
      }
      if ((cls != null) && (methods != null)){
        class2Methods.put(cls, methods);
      }
    } catch (Exception e){
      //			throw PredictorException.report("reading pure method list file! ", e);
      if (PredictorOptions.v().verbose_level >= 2) {
        System.err.println("Warning: pure method main file missing");
      }
    }
  }
	
  public void add(String cls, String method){
    HashSet<String> methods = class2Methods.get(cls);
    if (methods == null)
      methods = new HashSet<String>();
    methods.add(method);
    class2Methods.put(cls, methods);
  }
	
  public boolean isPure(SootMethodRef method){
    HashSet methods = class2Methods.get(method.declaringClass().getName());
    if (methods != null){
      String sig = method.getSubSignature().getString();
      String pure = "-" + sig;
      String unpure = "+" + sig;
      if ((methods.contains(pure)) || (methods.contains("-*") && ! methods.contains(unpure)))
        return true;
      else
        return false;
    }
    return false;
  }
	
  public boolean contains(SootMethod method){
    String cls = method.getDeclaringClass().getName();
    HashSet methods = class2Methods.get(cls);
    if (methods != null){
      if (methods.contains("+*") || methods.contains("-*"))
        return true;
      else if (methods.contains("+" + method.getSubSignature()) || methods.contains("-" + method.getSubSignature()))
        return true;
    }
    return false;
  }

  private HashMap<IString,HashSet<String>> isTrackedCache;
  public boolean isTracked(IString cls, String signature){
    HashSet<String> t1;
    t1 = isTrackedCache.get(cls);
    if ((t1 != null) && (t1.contains(signature))) {
      return true;
    }

    boolean ret = isTrackedGo(cls.toString(),signature);
    if (ret) {
      if (t1 == null) {
        t1 = new HashSet<String>();
        isTrackedCache.put(cls,t1);
      }
      t1.add(signature);
    }

    return ret;
  }
  private boolean isTrackedGo(String cls, String signature) {
    SootClass mainClass = null;
    try {
      mainClass = Scene.v().getSootClass(cls);
    } catch (RuntimeException e) {
      return false;
    }
    SootMethod m = null;
    while (mainClass.hasSuperclass()) {
      try {
        m = mainClass.getMethod(signature);
        break;
      } catch (RuntimeException e) {
        mainClass = mainClass.getSuperclass();
      }
    }
    return ((m != null) && m.hasActiveBody());
  }

  public String toString(){
    StringBuffer buf = new StringBuffer();
    Iterator it = class2Methods.keySet().iterator();
    while (it.hasNext()){
      String cls = (String)it.next();
      buf.append("^");
      buf.append(cls);
      buf.append("\n");
      HashSet methods = class2Methods.get(cls);
      Iterator mit = methods.iterator();
      while (mit.hasNext()){
        String m = (String)mit.next();
        buf.append(m);
        buf.append("\n");
      }
    }
    return buf.toString();
  }
	
  public boolean isCallBack(String sig){
    if (sig.indexOf("()") > -1)
      return true;
    else
      return ((sig.compareTo("org.apache.commons.logging.Log getInstance(java.lang.Class)") == 0) || (sig.compareTo("boolean contains(java.lang.Object)") == 0) || (sig.compareTo("int hashCode()") == 0) || (sig.indexOf("<init>")>-1) || (sig.compareTo("int size()") == 0) || (sig.compareTo("java.util.Iterator iterator()") == 0));
  }
}
// vim: tw=100:sw=2
