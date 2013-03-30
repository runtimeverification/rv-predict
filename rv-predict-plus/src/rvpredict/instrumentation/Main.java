package rvpredict.instrumentation;
import rvpredict.GUIMain;

import soot.Pack;
import soot.PackManager;
import soot.Scene;
import soot.SootMethod;
import soot.Transform;
import soot.options.Options;

import soot.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.PrintStream;

import soot.jimple.IdentityStmt;
import soot.jimple.spark.SparkTransformer;

import rvpredict.analysis.MethodAnalyzer;

import rvpredict.prediction.PredictorOptions;

public class Main{
  private static String mkStars(float t){
    String s = String.valueOf(t);
    String stars = "";
    for(int i = 0; i < s.length(); ++i) stars += "*";
    return stars;
  }

  public static void main(String[] args) {
    /* check the arguments */
    if (args.length == 0) {
      System.err.println("Usage: java Main [options] classname");
      System.exit(1);
    }
    try {
      System.out.println(
          "****************************************************");
      System.out.println(
          "* Loading and configuring SOOT for instrumentation *");
      System.out.println(
          "****************************************************");
      long time = System.currentTimeMillis();
      Options.v().parse(args);

      //Added by Jeff
      //------------------
      LinkedList<String> list = new LinkedList<String> ();
      list.add("rvpredict.");
      list.add("jdbm.");
      list.add("com.");
      list.add("java.");
      list.add("javax.");
      list.add("sun.");
      Options.v().set_exclude(list); 
      
      //Options.v().set_output_format(1);//output jimple
      Options.v().set_print_tags_in_output(true);//print tag
      Options.v().set_output_dir(System.getProperty("user.dir")+ File.separator+"tmp");
      //-------------------
      
      G.v().out = new PrintStream(new FileOutputStream(Options.v().output_dir() + File.separator + "soot-output.instr"));
      Options.v().set_keep_line_number(true);
      Options.v().set_whole_program(true);
      Options.v().set_no_bodies_for_excluded(true);
      Options.v().set_allow_phantom_refs(true);
      Options.v().set_app(true); 
      

      //Added by Jeff
      Scene.v().setSootClassPath(System.getProperty("sun.boot.class.path")
              + File.pathSeparator + System.getProperty("java.class.path"));
      
      ThreadSharingAnalyzer analyzer = new ThreadSharingAnalyzer();
      
      PackManager.v().getPack("wjtp").add(new Transform("wjtp.ThreadSharingAnalyzer", analyzer));

    //Jeff: make sure the parameter is only the main class
      String mainclass = args[0];
      SootClass appclass = Scene.v().loadClassAndSupport(mainclass);
      Scene.v().setMainClass(appclass);

    //Enable Spark
      HashMap<String,String> opt = new HashMap<String,String>();
      //opt.put("verbose","true");
      opt.put("propagator","worklist");
      opt.put("simple-edges-bidirectional","false");
      opt.put("on-fly-cg","true");
      opt.put("set-impl","double");
      opt.put("double-set-old","hybrid");
      opt.put("double-set-new","hybrid");
      opt.put("pre_jimplify", "true");
      SparkTransformer.v().transform("",opt);
      PhaseOptions.v().setPhaseOption("cg.spark", "enabled:true");
      
      //Commented by Jeff
      //PackManager.v().getPack("wjtp").add(new Transform("wjtp.LoopPeeler", new LoopPeeler()));
      //PackManager.v().getPack("jtp").add(new Transform("jtp.instrumenter", new Instrumentor()));
      
      Scene.v().addBasicClass("rvpredict.logging.NewRT",SootClass.SIGNATURES);
      Scene.v().loadNecessaryClasses();
      float t = ((System.currentTimeMillis() - time)/ 1000f);
      String stars = mkStars(t);
      System.out.println("*****************************" + stars);
      System.out.println("* Finished loading Soot " + GUIMain.YELLOW 
          + "[" + t + "s] * ");
      System.out.println("*****************************" + stars);
      System.out.println(
      "**************************************************************");
      System.out.println(
      "* Instrumenting all necessary classes...                     *");
      System.out.println(
      "* this may take a while as necessary libraries are pulled in *");
      System.out.println(
      "**************************************************************");
      time = System.currentTimeMillis();
      PackManager.v().runPacks();
      PackManager.v().writeOutput();
      t =  ((System.currentTimeMillis() - time)/ 1000f);
      stars = mkStars(t);
      System.out.println("*****************************" + stars); 
      System.out.println("* Finished Instrumenting " + GUIMain.YELLOW + "[" + t + "s] *");
      System.out.println("*****************************" + stars + "\n"); 

      System.out.println("*****************************" + stars);
      analyzer.reportStatistics();
      System.out.println("*****************************" + stars + "\n"); 

    } catch (Exception e) { e.printStackTrace(); System.exit(1); }
  }

  private static String[] removeLast(String[] args, int cnt) {
    String[] new_args = new String[args.length - cnt];
    for (int i =0; i < new_args.length; i++) {
      new_args[i] = args[i];
    }
    return new_args;
  }
}
// vim: tw=100:sw=2
