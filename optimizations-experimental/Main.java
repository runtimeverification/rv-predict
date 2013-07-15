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
import java.util.Set;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.PrintStream;

import soot.jimple.IdentityStmt;
import soot.jimple.spark.SparkTransformer;

import rvpredict.analysis.MethodAnalyzer;

import rvpredict.prediction.PredictorOptions;

public class Main{
	private static String DIR_RECORD = "record";
	private static String DIR_REPLAY = "replay";

    static LinkedList<String> excludeList = new LinkedList<String> ();
    static
    {
    excludeList.add("rvpredict.");
    excludeList.add("jdbm.");
    excludeList.add("com.");
    excludeList.add("java.");
    excludeList.add("javax.");
    excludeList.add("sun.");
    }
  private static String mkStars(float t){
    String s = String.valueOf(t);
    String stars = "";
    for(int i = 0; i < s.length(); ++i) stars += "*";
    return stars;
  }
  
  private static boolean optimizeLoops;

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
      
      Options.v().parse(args);
      
      //Jeff: make sure the parameter is only the main class
      String mainclass = args[0];
      
      if(args.length >= 2 && args[1].equals("true")){
    	optimizeLoops = true;
      }
      else {
    	optimizeLoops = false;
      }
      
      //Record version
      Set<String> sharedVariables = transformRecordVersion(mainclass);
      
      soot.G.reset();

      //Replay version
      transformReplayVersion(mainclass,sharedVariables);


    } catch (Exception e) { e.printStackTrace(); System.exit(1); }
  }

  private static Set<String> transformRecordVersion(String mainclass)
  {
      //output jimple
    // Options.v().set_output_format(1);
      //print tag
      //Options.v().set_print_tags_in_output(true);

	  long time = System.currentTimeMillis();

	  setOptions(DIR_RECORD);
      
      setClassPath();
      enableSpark();

      SetupPass setupPass = new SetupPass(optimizeLoops);
      MethodCloner methodClone = new MethodCloner();
      RecordInstrumentor recordInst = new RecordInstrumentor();
      PackManager.v().getPack("wjtp").add(new Transform("wjtp.SetupPass", setupPass));
      if(optimizeLoops) {
    	  PackManager.v().getPack("wjtp").add(new Transform("wjtp.MethodCloner", methodClone));
      }
      PackManager.v().getPack("wjtp").add(new Transform("wjtp.RecordInstrument", recordInst));

      SootClass appclass = Scene.v().loadClassAndSupport(mainclass);
      Scene.v().setMainClass(appclass);
      
      Scene.v().addBasicClass("rvpredict.logging.RecordRT",SootClass.SIGNATURES);
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
      recordInst.reportStatistics();
      recordInst.saveMetaToDB(mainclass);
      System.out.println("*****************************" + stars + "\n"); 

      return recordInst.getSharedVariableSignatures();
  }
  private static void transformReplayVersion(String mainclass, Set<String> sharedVariables)
  {
      //output jimple
    //  Options.v().set_output_format(1);
      //print tag
      //Options.v().set_print_tags_in_output(true);

      setOptions(DIR_REPLAY);

      setClassPath();
      
      enableSpark();

      ReplayInstrumentor replayInst = new ReplayInstrumentor(sharedVariables);
      PackManager.v().getPack("wjtp").add(new Transform("wjtp.ReplayInstrument", replayInst));

      System.out.println(
      "* Generating replay version...                     *");
      System.out.println(
      "* this may take a while as necessary libraries are pulled in *");
      System.out.println(
      "**************************************************************");
      SootClass appclass = Scene.v().loadClassAndSupport(mainclass);
      Scene.v().setMainClass(appclass);
      
      Scene.v().addBasicClass("rvpredict.logging.ReplayRT",SootClass.SIGNATURES);
      Scene.v().loadNecessaryClasses();
      
      long time = System.currentTimeMillis();
      PackManager.v().runPacks();
      PackManager.v().writeOutput();
      float t =  ((System.currentTimeMillis() - time)/ 1000f);
      String stars = mkStars(t);
      System.out.println("*****************************" + stars); 
      System.out.println("* Finished " + GUIMain.YELLOW + "[" + t + "s] *");
      System.out.println("*****************************" + stars + "\n"); 
  }
  private static void setClassPath()
  {
      Scene.v().setSootClassPath(System.getProperty("sun.boot.class.path")
              + File.pathSeparator + System.getProperty("java.class.path"));

  }
  private static void setOptions(String dir)
  {
	  Options.v().set_exclude(excludeList);
      Options.v().set_output_dir(getTempSubDirectory(dir));
      //-------------------
      
      Options.v().set_keep_line_number(true);
      Options.v().set_whole_program(true);
      Options.v().set_no_bodies_for_excluded(true);
      Options.v().set_allow_phantom_refs(true);
      Options.v().set_app(true);
      
      try {
		G.v().out = new PrintStream(new FileOutputStream(Options.v().output_dir() + File.separator + "soot-output.instr"));
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

  }
  private static void enableSpark()
  {
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
  }
  private static String getTempSubDirectory(String name)
  {
	  String tempdir = System.getProperty("user.dir");
		if (!(tempdir.endsWith("/") || tempdir.endsWith("\\"))) {
			tempdir = tempdir + System.getProperty("file.separator");
		}
		
		tempdir = tempdir +"tmp";
		
		File tempFile = new File(tempdir);
		if(!(tempFile.exists()))
			tempFile.mkdir();
			
		String dir = tempdir+System.getProperty("file.separator")+name;
		File dirFile = new File(dir);
		if(!(dirFile.exists()))
			dirFile.mkdir();
		
		return dir;	  
	  
  }
}
// vim: tw=100:sw=2
