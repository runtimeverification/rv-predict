/*******************************************************************************
 * Copyright (c) 2013 University of Illinois
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package rvpredict.instrumentation;

import soot.Pack;
import soot.PackManager;
import soot.Scene;
import soot.SootMethod;
import soot.Transform;
import soot.options.Options;
import soot.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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

/**
 * The Main class is the entry class for RVPredict instrumentation.
 * Two transformed versions of the application code are generated: 
 * a record version for runtime trace collection and a replay version
 * for bug reproduction. The generated classes are under tmp/record
 * and tmp/replay respectively.
 */
public class Main{

	private static Configuration config;

    /**
     * Skip instrumenting classes under the packages in excludeList
     * @param packageName
     * @return
     */
    public static boolean skipPackage(String packageName)
    {
    	for(String name : config.excludeList)
    	if(packageName.startsWith(name))
    		return true;

    		return false;
    }
  private static String mkStars(float t){
    String s = String.valueOf(t);
    String stars = "";
    for(int i = 0; i < s.length(); ++i) stars += "*";
    return stars;
  }

  /**
   * The input is the name of the application entry class
   * @param args
   */
  public static void main(String[] args) {

    config = new Configuration(args);
    
    try {
      System.out.println(
          "****************************************************");
      System.out.println(
          "* Loading and configuring SOOT for instrumentation *");
      System.out.println(
          "****************************************************");
      
      //Options.v().parse(args);
      
      //Make sure the first parameter is the main class
      String mainclass = args[0];
      
      //the second argument is 
      //TODO: parse the arguments
      
      //Record version
      Set<String> sharedVariables = transformRecordVersion(mainclass);
      
      if(config.noReplay)return;
      
      soot.G.reset();

      //Replay version
      transformReplayVersion(mainclass,sharedVariables);

    } catch (Exception e) { e.printStackTrace(); System.exit(1); }
  }
/**
 * Record version transformation 
 * @param mainclass
 * @return meta data for replay version transformation
 */
  private static Set<String> transformRecordVersion(String mainclass)
  {
      //output jimple
	  if(config.outputJimple)Options.v().set_output_format(1);
      //print tag
      //Options.v().set_print_tags_in_output(true);

	  long time = System.currentTimeMillis();

	  setOptions(config.DIR_RECORD);
      
      setClassPath();
      
      enableSpark();
      
      ThreadSharingAnalyzer sharingAnalyzer = new ThreadSharingAnalyzer(config.nosa);
      //ThreadSharingAnalyzer in the first phase to find shared variables
      PackManager.v().getPack("wjtp").add(new Transform("wjtp.ThreadSharing", sharingAnalyzer));

      RecordInstrumentor recordInst = new RecordInstrumentor(sharingAnalyzer);
      //Transformation instrumentation for record version
      PackManager.v().getPack("wjtp").add(new Transform("wjtp.RecordInstrument", recordInst));

      SootClass appclass = Scene.v().loadClassAndSupport(mainclass);
      Scene.v().setMainClass(appclass);
      
      Scene.v().addBasicClass("rvpredict.logging.RecordRT",SootClass.SIGNATURES);
      Scene.v().loadNecessaryClasses();
      float t = ((System.currentTimeMillis() - time)/ 1000f);
      String stars = mkStars(t);
      System.out.println("*****************************" + stars);
      System.out.println("* Finished loading Soot " 
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
      System.out.println("* Finished Instrumenting " + "[" + t + "s] *");
      System.out.println("*****************************" + stars + "\n"); 

      System.out.println("*****************************" + stars);
      //recordInst.reportStatistics();
      recordInst.saveMetaToDB(mainclass);
      System.out.println("*****************************" + stars + "\n"); 

      return recordInst.getSharedVariableSignatures();
  }
  /**
   * Transformation for replay version
   * @param mainclass
   * @param sharedVariables
   */
  private static void transformReplayVersion(String mainclass, Set<String> sharedVariables)
  {
      //output jimple
	  if(config.outputJimple)Options.v().set_output_format(1);
      //print tag
      //Options.v().set_print_tags_in_output(true);

      setOptions(config.DIR_REPLAY);

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
      System.out.println("* Finished " + "[" + t + "s] *");
      System.out.println("*****************************" + stars + "\n"); 
  }
  private static void setClassPath()
  {
	  String bootpath = System.getProperty("sun.boot.class.path");
      String javapath = System.getProperty("java.class.path");
	  String path = bootpath + File.pathSeparator +javapath;
      Scene.v().setSootClassPath(path);
      
      //System.out.println(path);
      //directories containing all the application classes 
//      if(config.nosa)
//      {
//    	  List<String> items = Arrays.asList(javapath.split(File.pathSeparator));
//    	 
//    	  Options.v().set_process_dir(items);
//      }

  }
  private static void setOptions(String dir)
  {
	  Options.v().set_include(config.includeList);
	  Options.v().set_exclude(config.excludeList);
	  
      Options.v().set_output_dir(getTempSubDirectory(dir));
      //-------------------
      
      Options.v().set_keep_line_number(true);
      Options.v().set_whole_program(true);
      
    //this option must be disabled for a sound call graph
      Options.v().set_no_bodies_for_excluded(true);
      
      Options.v().set_allow_phantom_refs(true);
      Options.v().set_app(true);
      
      try {
		G.v().out = new PrintStream(new FileOutputStream(Options.v().output_dir() + File.separator + "soot-output.txt"));
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

  }
  /** Enable Spark for whole program points-to analysis */
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
  /**
   * Create a folder under the "tmp" directory with the given parameter name
   * @param name
   * @return
   */
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
