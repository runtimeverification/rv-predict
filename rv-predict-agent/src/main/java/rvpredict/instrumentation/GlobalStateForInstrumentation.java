package rvpredict.instrumentation;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.ClassReader;

import rvpredict.config.Config;
import rvpredict.logging.RecordRT;

public class GlobalStateForInstrumentation {
    public static GlobalStateForInstrumentation instance = new GlobalStateForInstrumentation();
    public ConcurrentHashMap<String,Integer> variableIdMap = new ConcurrentHashMap<String,Integer>();
    public HashMap<Integer,String> arrayIdMap = new HashMap<Integer,String>();

    public HashSet<String> volatilevariables = new HashSet<String>();
    public ConcurrentHashMap<String,Integer> stmtSigIdMap = new ConcurrentHashMap<String,Integer>();
    HashSet<String> sharedVariables;
    HashSet<String> sharedArrayLocations;
    
    public boolean isVariableShared(String sig)
    {
    	if(sharedVariables==null
    			||sharedVariables.contains(sig))
    		return true;
    	else
    		return false;
    }
    public boolean shouldInstrumentArray(String loc)
    {
    	if(sharedArrayLocations==null
    			||sharedArrayLocations.contains(loc))
    		return true;
    	else
    		return false;
    }
    public void setSharedArrayLocations(HashSet<String> locs)
    {
    	this.sharedArrayLocations = locs;
    }
    public void setSharedVariables(HashSet<String> locs)
    {
    	this.sharedVariables = locs;
    }
    public 	GlobalStateForInstrumentation()
    {
    	//save instrumentation and runtime information?
    	Runtime.getRuntime().addShutdownHook(new Thread("Thread-logMetaData") {
    		public void run() {
    	    	if(!Config.instance.commandLine.agentOnlySharing)
    	    		RecordRT.saveMetaData(variableIdMap, volatilevariables, stmtSigIdMap,Config.instance.verbose);
    	    	else
    	    	{
    	    		//show arrayId
	    	    	HashSet<Integer> sharedArrayIds = new HashSet<Integer>();
	    	    	for(Integer sid: RecordRT.sharedArrayIds)
	    	    	{
	    	    		HashSet<Integer> ids = RecordRT.arrayIdsMap.get(sid);
	    	    			sharedArrayIds.addAll(ids);
	    	    	}
	    	    	
	    	    	sharedVariables = new HashSet<String>();
		    	    	//show variableId
		    	    	for(Map.Entry<String,Integer> entry: variableIdMap.entrySet())
		    	    	{	    	    		
		    	    		Integer id = entry.getValue();
		    	    		String var = entry.getKey();
		    	    		if(RecordRT.sharedVariableIds.contains(id))
		    	    			sharedVariables.add(var);
		    	    		
		    	    	}
		    	    	
		    	    	sharedArrayLocations = new HashSet<String>();

		    	    	for(Integer id: arrayIdMap.keySet())
		    	    	{	    	    		
		    	    		String var = arrayIdMap.get(id);
		    	    		if(sharedArrayIds.contains(id))
			    	    		sharedArrayLocations.add(var);
		    	    	}

		    	    	
    	    		
	    	    	if(Config.instance.verbose)
	    	    	{
		    	    	int size_var = variableIdMap.entrySet().size(); 
		    	    	int size_array = arrayIdMap.entrySet().size(); 

		    	    	double svar_percent = size_var==0?0:((double)RecordRT.sharedVariableIds.size()/variableIdMap.entrySet().size());
		    	    	double sarray_percent = size_array==0?0:((double)sharedArrayIds.size()/arrayIdMap.entrySet().size());
	
	    	    		System.out.println("\nSHARED VARIABLE PERCENTAGE: "+svar_percent);
	    	    		System.out.println("SHARED ARRAY PERCENTAGE: "+sarray_percent);
	    	    	}
    	    		//save the sharedvariable to database??
    	    		RecordRT.saveSharedMetaData(sharedVariables,sharedArrayLocations);
    	    	}
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
    public int getArrayLocationId(String sig)
    {
    	int id = getLocationId(sig);
    	
    	arrayIdMap.put(id,sig);
    	
    	return id;
    }
    public String getArrayLocationSig(int id)
    {
    	return arrayIdMap.get(id);
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
