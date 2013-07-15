package rvpredict.instrumentation;
import soot.Pack;
import soot.PackManager;
import soot.SootMethod;
import soot.Transform;
import soot.options.Options;

import java.util.Iterator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;

import soot.jimple.IdentityStmt;

import rvpredict.analysis.MethodAnalyzer;

import rvpredict.prediction.PredictorOptions;

public class InstrMain{

  public static void main(String[] args) {
    /* check the arguments */
    if (args.length == 0) {
      System.err.println("Usage: java InstrMain [options] classname");
      System.exit(0);
    }
    Options.v().set_keep_line_number(true);
    /* add a phase to transformer pack by call Pack.add */
    Pack jtp = PackManager.v().getPack("jtp");
    jtp.add(new Transform("jtp.instrumenter", new Instr()));
    /* Give control to Soot to process all options,
     * InvokeStaticInstrumenter.internalTransform will get called.
     */
    soot.Main.main(args);
    try {
      MethodAnalyzer sys_ma = MethodAnalyzer.v();
      MethodAnalyzer ma = new MethodAnalyzer();
      BufferedReader inreader = new BufferedReader(new InputStreamReader(System.in));
      Iterator it = Instr.calledMethods.iterator();
      while (it.hasNext()){
        SootMethod m = (SootMethod)it.next();
        if ((! m.isAbstract()) && (! Instr.instrumentedMethods.contains(m))){
          if (!sys_ma.contains(m))
            ma.add(m.getDeclaringClass().toString(), "+" + m.getSubSignature().toString());
        }
      }
      String path = Options.v().output_dir() + File.separator + "pure.lst";
      FileWriter writer = new FileWriter(path);
      writer.write(ma.toString());
      writer.close();
      System.out.println("Pure methods saved!");
    } catch (Exception e){
      System.out.println("Errors at main! " + e.getMessage());
    }
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
