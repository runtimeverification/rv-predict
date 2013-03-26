package rvpredict.prediction;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import rvpredict.PredictorException;
import rvpredict.analysis.MethodAnalyzer;
import rvpredict.prediction.atomicity.AtomicBlockFinder;

import rvpredict.prediction.generic.Event;
import rvpredict.prediction.generic.EventPattern;
import rvpredict.prediction.generic.Preprocessor;
import rvpredict.prediction.generic.SharedVarHandler;
import rvpredict.prediction.generic.Slicer;
import rvpredict.prediction.generic.VCCalculator;

import rvpredict.prediction.io.TraceReader;
import rvpredict.prediction.race.RaceDetector;

import rvpredict.util.Configure;
import rvpredict.util.Util;

import soot.Body;
import soot.BodyTransformer;

public class Predictor  extends BodyTransformer {
  MethodAnalyzer ma;
  String workDir;
  Preprocessor preprocessor;
  boolean doFilter;
  boolean doPreprocessor;
  boolean doSlicing;
  boolean doVC;
  boolean doPredict;
  boolean doAtomicFinder;
  boolean moreFilter;
  String mainClass;
  String mainMethod;
  String filterNames; // the names used to filter out shared variables to check

  public Predictor(String dir, boolean doPre, boolean doSlicing, boolean doVC, boolean doPrediction, boolean doFilter, boolean moreFilter, boolean doAtomicFinder, String filter) throws Exception{
    ma = MethodAnalyzer.v();
    ma.addList(dir + File.separator + Configure.getString("PureListFile")  + ".rvpf");
    this.workDir = dir;
    doPreprocessor = doPre;
    this.doSlicing = doSlicing;
    this.doVC = doVC;
    this.doPredict = doPrediction;
    this.doFilter = doFilter;
    this.moreFilter = moreFilter;
    this.doAtomicFinder = doAtomicFinder;
    if (doPreprocessor) {
      this.preprocessor = new Preprocessor();
      mainClass = preprocessor.startEvent.cls.toString();
      mainMethod = preprocessor.startEvent.id.toString();
    } else {
      TraceReader ta = new TraceReader(workDir + File.separator + Configure.getString("TraceFile")  + ".rvpf");
      Event e = ta.next();
      if (e.eventType != Event.BEGIN)
        throw new PredictorException("An BEGIN event is expected at the beginning of the trace file!");
      mainClass = e.cls.toString();
      mainMethod = e.id.toString();
    }
    filterNames = filter;
  }
  public Predictor(String dir) throws Exception{
    this(dir, true, true, true, true, false, false, false, null);
  }

  @Override
    protected void internalTransform(Body body, String phase, Map options) {
    if ((body.getMethod().getDeclaringClass().getName().compareTo(mainClass)!=0) || (body.getMethod().getSubSignature().compareTo(mainMethod)!=0))
      return;
    try {
      HashSet<String> shared;
      HashSet<String> toCheck;
      long startTime = System.currentTimeMillis();
      long duarution = 0;
      if (doPreprocessor) {
        System.out.println("Preprocessing the trace...");
        preprocessor.go();
        SharedVarHandler.storeShared(preprocessor.shared);
        shared = preprocessor.shared;
        if (doFilter && moreFilter)
          toCheck = Util.filter(shared, filterNames);
        else if (doFilter) {
          toCheck = Util.filterByNames(shared, filterNames);
        } else {
          toCheck = new HashSet<String>();
          toCheck.addAll(shared);
        }
        SharedVarHandler.storeToCheck(toCheck);
        duarution = System.currentTimeMillis() - startTime;
        System.out.println("Preprocessing done in " + duarution + " ms.");
      } else {
        shared = SharedVarHandler.readShared();
        toCheck = SharedVarHandler.readToCheck();

      }
      System.out.println("Found " + shared.size() + " shared variables, checking for " + toCheck.size() + " variables");

      if (doAtomicFinder){
        startTime = System.currentTimeMillis();
        System.out.println("Searching atomic blocks...");
        new AtomicBlockFinder().go();
        duarution = System.currentTimeMillis() - startTime;
        System.out.println("Searching atomic blocks done in " + duarution + " ms.");
      }

      if (doSlicing) {
        Iterator it = toCheck.iterator();
        int c = 0;
        while (it.hasNext()){
          String loc = (String)it.next();
          c++;
          startTime = System.currentTimeMillis();
          System.out.println("Slicing the trace for " + loc + " (" + c + "/" + toCheck.size() + ")");
          ArrayList<EventPattern> l = new ArrayList<EventPattern>();
          l.add(new EventPattern(loc, false));
          l.add(new EventPattern(loc, true));
          Slicer slicer = new Slicer(ma, l, workDir, shared, loc);
          slicer.process();
          duarution = System.currentTimeMillis() - startTime;
          System.out.println("Slicing the trace for " + loc + " done in " + duarution + " ms.");
        }
      }
      if (doVC){
        Iterator it = toCheck.iterator();
        int c = 0;
        while (it.hasNext()){
          String loc = (String)it.next();
          c++;
          startTime = System.currentTimeMillis();
          System.out.println("Computing VC for " + loc + " (" + c + "/" + toCheck.size() + ")");
          VCCalculator vcc = new VCCalculator(workDir, loc);
          vcc.process();
          duarution = System.currentTimeMillis() - startTime;
          System.out.println("Computing VC for #" + loc + " done in " + duarution + " ms.");
        }
      }
      if (doPredict){
        Iterator it = toCheck.iterator();
        int c = 0;
        while (it.hasNext()){
          String loc = (String)it.next();
          c++;
          startTime = System.currentTimeMillis();
          System.out.println("Analyzing for for " + loc + " (" + c + "/" + toCheck.size() + ")");
          RaceDetector rd = new RaceDetector(workDir, loc);
          rd.process();
          duarution = System.currentTimeMillis() - startTime;
          System.out.println("Analyzing for " + loc + " done in " + duarution + " ms.");
        }
      }
    } catch (Exception e){
      e.printStackTrace();
      System.exit(1);
    }
  }

}
// vim: tw=100:sw=2
