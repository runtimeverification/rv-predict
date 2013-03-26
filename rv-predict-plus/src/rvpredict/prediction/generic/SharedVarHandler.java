package rvpredict.prediction.generic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

import rvpredict.PredictorException;
import rvpredict.prediction.PredictorOptions;

import rvpredict.util.Configure;
import rvpredict.util.Util;

public class SharedVarHandler extends Stage {

  public SharedVarHandler() throws PredictorException {
    super("Filtering shared variables");
  }

  @Override
    public void process() throws PredictorException {
    try {
      HashSet<String> toCheck = SharedVarHandler.readShared();
      if (PredictorOptions.v().filter_name != null){
        toCheck = Util.filterByNames(toCheck, PredictorOptions.v().filter_name);
      }
      if (PredictorOptions.v().filter_more)
        toCheck = Util.filter(toCheck);
      storeToCheck(toCheck);
    } catch (Exception e) {
      throw PredictorException.report("SharedVarHandler", e);
    }
  }

  public static void storeShared(HashSet shared) throws IOException{
    FileWriter var_writer = new FileWriter(PredictorOptions.v().work_dir + File.separator + Configure.getString("SharedVarFile") + ".rvpf");
    Iterator it = shared.iterator();
    while (it.hasNext()){
      var_writer.write((String)it.next());
      var_writer.write('\n');
    }
    var_writer.flush();
    var_writer.close();
  }

  public static void storeToCheck(HashSet<String> toCheck) throws IOException{
    FileWriter var_writer = new FileWriter(PredictorOptions.v().work_dir + File.separator + Configure.getString("CheckVarFile") + ".rvpf");
    Iterator it = toCheck.iterator();
    while (it.hasNext()){
      var_writer.write((String)it.next());
      var_writer.write('\n');
    }
    var_writer.flush();
    var_writer.close();
  }

  public static HashSet<String> readShared() throws IOException{
    HashSet<String> shared = new HashSet<String>();
    BufferedReader r = new BufferedReader(new FileReader(PredictorOptions.v().work_dir + File.separator + Configure.getString("SharedVarFile") + ".rvpf"));
    String line = r.readLine();
    while(line != null){
      if (line.length() > 0){
        shared.add(line.trim());
      }
      line = r.readLine();
    }
    return shared;
  }

  public static HashSet<String> readToCheck() throws IOException{
    HashSet<String> shared = new HashSet<String>();
    BufferedReader r = new BufferedReader(new FileReader(PredictorOptions.v().work_dir + File.separator + Configure.getString("CheckVarFile") + ".rvpf"));
    String line = r.readLine();
    while(line != null){
      if (line.length() > 0){
        shared.add(line.trim());
      }
      line = r.readLine();
    }
    return shared;
  }

}
// vim: tw=100:sw=2
