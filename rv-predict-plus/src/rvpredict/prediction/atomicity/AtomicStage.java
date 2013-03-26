package rvpredict.prediction.atomicity;

import rvpredict.PredictorException;
import rvpredict.prediction.generic.Stage;

import java.util.ArrayList;
import java.util.HashSet;

public class AtomicStage extends Stage {

  HashSet<String> locations;
  ArrayList<AtomicBlock> blockList;

  public AtomicStage(String str) throws PredictorException {
    super(str);
  }

  public void setLists(HashSet<String> ll, ArrayList<AtomicBlock> bl){
    locations = ll;
    blockList = bl;
  }

  @Override
    public void process() throws PredictorException {
  }

}
// vim: tw=100:sw=2
