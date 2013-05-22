package rvpredict.instrumentation;

import soot.*;

import soot.jimple.toolkits.thread.ThreadLocalObjectsAnalysis;
import soot.jimple.toolkits.thread.mhp.SynchObliviousMhpAnalysis;

import java.util.Map;

public class SetupPass extends SceneTransformer {
    public static ThreadLocalObjectsAnalysis tlo;

    static public LoopOptimizationMode loopOptimizationMode;
    
    public SetupPass(LoopOptimizationMode loopOptMode){
      loopOptimizationMode = loopOptMode;
    }
    
	protected void internalTransform(String phase, Map options){
	  tlo = new ThreadLocalObjectsAnalysis(new SynchObliviousMhpAnalysis());
	}
}
