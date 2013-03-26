package rvpredict.prediction;

import java.util.ArrayList;

import javax.swing.text.html.FormSubmitEvent.MethodType;

import rvpredict.analysis.MethodAnalyzer;
import rvpredict.util.Configure;

public class PredictorOptions {
  public final static int CHECK_RACE = 10;
  public final static int CHECK_ATOMIC = 20;
  public final static int CHECK_PROP = 30;
  public final static int CHECK_HB = 40;

  static PredictorOptions options = new PredictorOptions();
  public MethodAnalyzer.Purity purity_level = MethodAnalyzer.Purity.SAFE;

  public boolean do_pre = false;
  public boolean do_slice = false;
  public boolean do_find_atomic = false;
  public boolean do_vc = false;
  public boolean do_vc2 = false;
  public boolean do_merge = false;
  public boolean do_detect = false;
  public boolean pipeline_mode = false;
  public boolean filter_more = false;
  public boolean get_all_errors = false;
  public boolean compress_results = true;
  public boolean manual_list = false;
  public int prop = CHECK_RACE;
  public String filter_name;
  public String work_dir;
  public int verbose_level = 1;

  public static PredictorOptions v(){
    return options;
  }

  public String[] parse(String[] args) {
    ArrayList<String> remaining = new ArrayList<String>();
    if (args.length == 0){
      printUsage();
      System.exit(1);
    }
    for (int i = 0; i < args.length; i++){
      if (args[i].compareTo("-wd") == 0){
        if (i + 1 >= args.length) {
          printUsage();
          System.exit(1);
        }
        i++;
        work_dir = args[i];
      } else if (args[i].compareTo("-atomic") == 0){
        prop = CHECK_ATOMIC;
      } else if (args[i].compareTo("-prop") == 0){
        prop = CHECK_PROP;
      } else if (args[i].compareTo("-pre") == 0){
        do_pre = true;
      } else if (args[i].compareTo("-slice") == 0){
        do_slice = true;
      } else if (args[i].compareTo("-vc") == 0){
        do_vc = true;
      } else if (args[i].compareTo("-vc2") == 0){
        do_vc2 = true;
      } else if (args[i].compareTo("-detect") == 0){
        do_detect = true;
      } else if (args[i].startsWith("-find")){
        do_find_atomic = true;
      } else if (args[i].startsWith("-merge")){
        do_merge = true;
      } else if (args[i].startsWith("-pipeline")){
        pipeline_mode = true;
      } else if (args[i].startsWith("-get-all-errors")){
        get_all_errors = true;
      } else if (args[i].startsWith("-uncompressed-results")){
        compress_results = false;
      } else if (args[i].startsWith("-manual-list")){
        manual_list = true;
      } else if (args[i].startsWith("-filter")){
        if (args[i].startsWith("-filter-more"))
          filter_more = true;
        int j = args[i].indexOf(":");
        if (j > -1){
          // filter names are found
          filter_name = args[i].substring(j + 1);
        }
      } else {
        remaining.add(args[i]);
        if (args[i].compareTo("-cp") == 0){
          args[i+1] += ";" + Configure.getString("JavaRT");
        }
      }
    }
    if (! (do_pre || do_slice || do_find_atomic || do_merge || do_vc || do_detect || do_vc2)){
      if (prop == CHECK_RACE) {
        do_pre = do_slice = do_vc = do_detect = true;
      } else if (prop == CHECK_ATOMIC) {
        do_pre = do_slice = do_find_atomic = do_merge = do_vc = do_detect = true;
      } else if (prop == CHECK_HB) {
        do_pre = do_slice = do_vc = true;
      }
    }

    String[] ss = new String[remaining.size()];
    remaining.toArray(ss);
    return ss;
  }

  void printUsage(){
    System.out.println("Usage: java -cp ClassPath rvpredict.predictor.Main -wd <WorkDir> [options] [ClassList]");
    System.out.println("ClassPath: the paths containing the path to the original program under analysis and the jPredictor folder");
    System.out.println("WorkDir: the directorying under which jPredictor will work on; should contain the trace log and other needed information");
    System.out.println("Options: ");
    System.out.println("   -app MainClass: the main entry class of the application");
    System.out.println("   -atomic: detect atomicity violations (the atomic.def should be provided in the WorkDir); by default, races are checked");
    System.out.println("   -get-all-error: get all potential violations from the trace; by default, jPredictor continues to the next property when a violation is found for the current property");
    System.out.println("   -pre: carry out only the preprocessing stage");
    System.out.println("   -find: carry out only the atomic blocks finding stage, only used with -atomic");
    System.out.println("   -slice: carry out only the slicing stage");
    System.out.println("   -merge: carry out only the sliced trace merging stage, only used with -atomic");
    System.out.println("   -vc: carry out only the VC calculation stage");
    System.out.println("   -detect: carry out only the violation detection stage");
    System.out.println("      (Note: when no specific stage is chosen, the tool will carry out all stages from preprocessing to violcation dection)");
    System.out.println("   -uncompressed-results: do not compress results by removing violations caused by the same sequence of actions");
    System.out.println("   -pipeline: turn on the pipeline mode, in which the tool will check one property through all stages at one time; by default, jPredictor works in the batch mode, i.e., moving to the next stage only when all the properties have passed the current stage");
    System.out.println("   -filter[:ClassNames]: filter the shared variables for analysis according to the given list of class names (seperated by ;), used with race and atomicity checking");
    System.out.println("      -filter-more[:ClassNames]: do more aggressive filtering by randomly pickup one element in every array and one object for every field access");
  }
}
// vim: tw=100:sw=2
