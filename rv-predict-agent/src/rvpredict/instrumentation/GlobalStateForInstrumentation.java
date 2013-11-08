package rvpredict.instrumentation;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import org.objectweb.asm.ClassReader;

import rvpredict.config.Config;
import rvpredict.logging.RecordRT;

public class GlobalStateForInstrumentation {
    public static GlobalStateForInstrumentation instance = new GlobalStateForInstrumentation();
    public HashMap<String,Integer> variableIdMap = new HashMap<String,Integer>();
    public HashSet<String> volatilevariables = new HashSet<String>();
    public HashMap<String,Integer> stmtSigIdMap = new HashMap<String,Integer>();
    
    public 	GlobalStateForInstrumentation()
    {
    	//save instrumentation and runtime information?
    	Runtime.getRuntime().addShutdownHook(new Thread("Thread-logMetaData") {
    		public void run() {
    			RecordRT.saveMetaData(variableIdMap, volatilevariables, stmtSigIdMap,Config.instance.verbose);
    			}
    		});
    }
    public int getVariableId(String sig)
    {
  	  if(variableIdMap.get(sig)==null)
  	  {
  		variableIdMap.put(sig, variableIdMap.size()+1);
  	  }
  	  int sid = variableIdMap.get(sig);
  	  
  	  return sid;
    }
    public void addVolatileVariable(String sig)
    {
    	volatilevariables.add(sig);
    }
    
    public int getLocationId(String sig)
    {
    	if(stmtSigIdMap.get(sig)==null)
  	  {
  		  stmtSigIdMap.put(sig, stmtSigIdMap.size()+1);
  	  }
  	  
  	  return stmtSigIdMap.get(sig);
    }
    public boolean isThreadClass(String cname)
    {
    	while(!cname.equals("java/lang/Object"))
    	{
    		if(cname.equals("java/lang/Thread"))
    			return true;
    		
    		try {
				ClassReader cr= new ClassReader(cname);
				cname = cr.getSuperName();
			} catch (IOException e) {
				// TODO Auto-generated catch block
//				e.printStackTrace();
//				//if class can not find
//				System.out.println("Class "+cname+" can not find!");
				return false;
			}
    	}
    	return false;
    }
}
