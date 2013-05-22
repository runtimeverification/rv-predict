package rvpredict;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import soot.G;
import soot.Pack;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.Transform;

import soot.options.Options;

import rvpredict.logging.Protos;
import rvpredict.util.Util;

import rvpredict.traces.BackwardTrace;
import rvpredict.traces.ForwardTrace;
import rvpredict.traces.InitialTrace;
import rvpredict.traces.TraceReverser;
import rvpredict.Debug;
import rvpredict.util.ThreadBufferIterator;

public class Main {
  private static String mkStars(float t){
    String s = String.valueOf(t);
    String stars = "";
    for(int i = 0; i < s.length(); ++i) stars += "*";
    return stars;
  }


  /* Our command line options:
       AllShared:
         if used: Prediction will be ran on all shared variables.
         default: Predetermine some race candidates using a variation of the Racer algorithm by Bodden et al,
                  then run Prediction on those. When on, determining what variables to analyze should take slightly
                  longer, but there should overall be significant performance gains.
       Mop <classname>:
         if used: Prediciton of arbitrary constraints, as specified in the given classname.
         default: Off, just use race detection
       Passthrough:
         if used: the pass-through slicer is used
         default: uses the actual slicer
       OnlyCandidates:
         if used: Only output the candidates, then quit (Don't do any predicting, just find candidates)
       ShowEvents:
         Show the events encountered during slicing
       ShowEventTypes:
         Show the types of events encountered during slicing
       DebugSlicing:
         Turns on debugging info for slicing
       DebugVectorClocks:
         Turn on debuffing info for vector clocks
       TerminalLength:
         Specify a max terminal length (for printing out shared variables)
         default: 160
  */

  // Since we need information from soot we phrase our main as a scene transformer so that soot will read the classes
  // in etc. soot.Main.main errors if we try to parse options beforehand, so we do what we need from
  // soot.Main.main by hand
  public static void main(String[] args) {
    try{
      System.out.println(
          "***********************************************");
      System.out.println(
          "* Loading and configuring SOOT for prediction *");
      System.out.println(
          "***********************************************");
      long time = System.currentTimeMillis();

      // Set up the phase, and it's command line arguments
      Transform phase = new Transform("wjtp.predict", new MainPhase());
      String ourOptions = "AllShared Mop Passthrough OnlyCandidates ShowEvents ShowEventTypes "
                        + "DebugSlicing DebugVectorClocks TerminalLength";
      phase.setDeclaredOptions(ourOptions + " " + phase.getDeclaredOptions());

      
      //Added by Jeff
      //------------------
      LinkedList<String> list = new LinkedList<String> ();
      list.add("rvpredict.");
      list.add("jdbm.");
      list.add("com.");
      Options.v().set_exclude(list); 
      
      Options.v().set_output_format(1);//output jimple
      //Options.v().set_print_tags_in_output(true);//print tag
      String output_path = System.getProperty("user.dir");
      output_path = output_path.replace("javamop", "run");
      Options.v().set_output_dir(output_path);
      //-------------------

      
      PackManager.v().getPack("wjtp").add(phase);
      Options.v().parse(args);
      Options.v().set_keep_line_number(true);
      Options.v().set_whole_program(true);
      Options.v().setPhaseOption("cg","enabled:false");
      Options.v().set_output_format(Options.output_format_jimp);
      G.v().out = new PrintStream(new FileOutputStream(Options.v().output_dir() + File.separator + "soot-output.predict"));

      Scene.v().loadNecessaryClasses();
      float t = ((System.currentTimeMillis() - time)/ 1000f);
      String stars = mkStars(t);
      System.out.println("*****************************" + stars);
      System.out.println("* Finished loading Soot " + GUIMain.YELLOW 
           + "[" + t + "s] * ");
      System.out.println("*****************************" + stars);

      PackManager.v().runPacks();
    } catch (Exception e) { e.printStackTrace(); System.exit(1); }
  }

  static public boolean showEvents;
  static public boolean showEventTypes;
  static public boolean debugSlicing;
  static public boolean debugVectorClocks;
  static public Integer termLen;
    

  static public int initialStackDepth = 0;

  static public void debug(Debug d, String output) {
    switch (d) {
    case Slicing:
      if (debugSlicing) System.out.println(output);
      break;
    case VectorClocks:
      if (debugVectorClocks) System.out.println(output);
      break;
    }
  }

  private static class MainPhase extends SceneTransformer {
    private boolean AllSharedOption;
    private boolean mopSlicing;
    private boolean passthrough;
    private boolean onlyCandidates;
    private String mopClass;

    private static String mkStars(float t){
      String s = String.valueOf(t);
      String stars = "";
      for(int i = 0; i < s.length(); ++i) stars += "*";
      return stars;
    }

    @Override protected void internalTransform(String phaseName, Map options) {
      // Set up our command-line arguments
      setUpArgs(options);

      System.out.println("**************"); 
      System.out.println("* Predicting *");
      System.out.println("**************"); 

      long time = System.currentTimeMillis();
      initialStackDepth = new InitialTrace().initialStackDepth;
      System.out.println(GUIMain.GREEN + "  " + initialStackDepth);

      if (!mopSlicing) {
        // Set up the race candidates, depending on what method we want to use
        Iterable<Protos.Variable> candidates;
        String candidateDesc;
        if (AllSharedOption) {
          System.out.println("Finding all shared variables");
          //candidates = new SharedVarFinder(new InitialTrace());
          candidates = null;
          candidateDesc = "shared variable";
        } else {
          System.out.println("Determining race candidates");
          candidates = new LogTimeSharedVars();
          candidateDesc = "race candidate";
        }

        // (Pretty) Print out our candidates
        // and find out longest description size
        int longestSize = prettyPrintCandidates(candidates, candidateDesc);

        // Stop if the user only wants to see the candidates
        if (onlyCandidates) return;

        int i = 1;
        ExecutorService pool 
          = Executors.newFixedThreadPool(1);
        List<RaceDetectorRunner> tasks = new ArrayList<RaceDetectorRunner>();
        for (final Protos.Variable a : candidates) {
          tasks.add(new RaceDetectorRunner(a, i++, longestSize, candidateDesc));
        //    System.out.println("(Passthrough slicing)");
        //    r |= new RaceDetector(new RaceVectorClocker(
        //    TraceReverser.reverse(new RacePassthroughSlicer(
        //    new InitialTrace(),a)))).call();
        }
        try {
          List<Future<Boolean>> results = pool.invokeAll(tasks);
          boolean r = false;
          for(Future<Boolean> result : results){
            r |= result.get();
          }
          if(!r){
            System.out.println(GUIMain.GREEN + "/------------------\\");
            System.out.println(GUIMain.GREEN + "|  No Races Found  |");
            System.out.println(GUIMain.GREEN + "\\------------------/");
          }
        } catch (Exception e){ //some sort of concurrent exception
          throw new RuntimeException(e);
        }
      }
      else { //do mop detection
        System.out.println("Predicting for properties specified in " + mopClass);
        boolean r = false;
        for (final MOPInstance i : new MOPInstanceFinder(new InitialTrace())) {
          if (i.isFull()) {
            System.out.println("Predicting for instance: " + i);
            boolean rTemp 
  = new RevMOPDetector(new RevMOPVectorClocker(new MOPSlicer(
                        new ThreadBufferIterator<Protos.Event>(new InitialTrace()),i)), mopClass ).call();
            r |= rTemp;
          }
        }
        if (!r)
          System.out.println(GUIMain.GREEN + "/----------------------\\");
          System.out.println(GUIMain.GREEN + "|  No Violations Found  |");
          System.out.println(GUIMain.GREEN + "\\----------------------/");
      }
      float t =  ((System.currentTimeMillis() - time)/ 1000f);
      String stars = mkStars(t);
      System.out.println("***************************" + stars); 
      System.out.println("* Finished Predicting " + GUIMain.YELLOW + "[" + t + "s] *");
      System.out.println("***************************" + stars + "\n"); 
      System.exit(0);
    }

    private static class RaceDetectorRunner implements Callable<Boolean> {
      private final Protos.Variable a;
      private final int i;
      private final int longestSize;
      private final String candidateDesc; 
      RaceDetectorRunner(final Protos.Variable a, final int i, 
                         final int longestSize, String candidateDesc){
        this.a = a;
        this.i = i;
        this.longestSize = longestSize;
        this.candidateDesc = candidateDesc;
      }
      @Override
      public Boolean call(){
          String prettyPrinted = Util.prettyPrint(a);
          synchronized(System.out){
            System.out.println(String.valueOf(i) 
                + ". Predicting for " + candidateDesc + ": " 
                + prettyPrinted);
            System.out.flush();
          }
          boolean rTemp = new RevRaceDetector(
            new RevRaceVectorClocker(
              new RaceSlicer(
                new ThreadBufferIterator<Protos.Event>(
                 new InitialTrace()),a))).call();
            if(!rTemp) {
              synchronized(System.out){
                System.out.println(GUIMain.GREEN + "[No race found] on " 
                                 + candidateDesc + ": " + prettyPrinted); 
              }
            }
            return rTemp;
      }
    }

    private boolean checkOption(final Object o) {
      return (o != null);
    }

    private void setUpArgs(Map options) {
      mopClass = (String) options.get("Mop");
      if (checkOption(options.get("TerminalLength")))
        termLen = Integer.parseInt((String) options.get("TerminalLength"));
      else
        termLen = 160;
      mopSlicing = checkOption(mopClass);
      AllSharedOption = checkOption(options.get("AllShared"));
      passthrough = checkOption(options.get("Passthrough"));
      onlyCandidates = checkOption(options.get("OnlyCandidates"));
      showEvents = checkOption(options.get("ShowEvents"));
      showEventTypes = checkOption(options.get("ShowEventTypes"));
      debugSlicing = checkOption(options.get("DebugSlicing"));
      debugVectorClocks = checkOption(options.get("DebugVectorClocks"));
    }

    private static String dots(int i){
      StringBuilder ret = new StringBuilder("...");
      for(; i > 0; --i){
        ret.append(".");
      }
      return ret.toString();
    }
 
    private int prettyPrintCandidates( java.lang.Iterable<rvpredict.logging.Protos.Variable> candidates
                                      , String candidateDesc ){
        System.out.print("There are ");        
        String nl = System.getProperty("line.separator"); // cross-platform newlines
        StringBuilder str = new StringBuilder();
        int i = 0;
        int longest = 0;
        for (final Protos.Variable a : candidates) {
          ++i;
          String prettyPrinted = Util.prettyPrint(a);
          longest = (longest < prettyPrinted.length())? prettyPrinted.length() : longest;
          if (prettyPrinted.length() + 3 + str.length() - str.lastIndexOf(nl) > termLen)
            str.append(nl);
          str.append(" | " + prettyPrinted);
        }
        System.out.println(GUIMain.GREEN + String.valueOf(i) + GUIMain.BLACK + " " + candidateDesc + "s: ");
        System.out.println(str + "\n");
        return longest;
    }

  }



}

// vim: tw=100:sw=2
