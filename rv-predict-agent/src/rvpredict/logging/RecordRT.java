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
package rvpredict.logging;

import rvpredict.config.Config;
import rvpredict.instrumentation.GlobalStateForInstrumentation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public final class RecordRT {

	//can be computed during offline analysis
	static HashMap<Long,String> threadTidNameMap;
	static HashMap<Long,Integer> threadTidIndexMap;
	public static HashSet<Integer> sharedVariableIds;
	public static HashSet<Integer> sharedArrayIds;
	static HashMap<Integer,Long> writeThreadMap;
	static HashMap<Integer,long[]> readThreadMap;
	public static HashMap<Integer,HashSet<Integer>> arrayIdsMap;

	static HashMap<Integer,Long> writeThreadArrayMap;
	static HashMap<Integer,long[]> readThreadArrayMap;
	final static String MAIN_NAME = "0";
	
	static ThreadLocal<HashSet<Integer>> threadLocalIDSet;
	static ThreadLocal<HashSet<Integer>> threadLocalIDSet2;

	//engine for storing events into database
	static DBEngine db;

    public static void init()
	{
    	if(Config.detectSharingOnly){
    		sharedVariableIds = new HashSet<Integer>();
    		writeThreadMap = new HashMap<Integer,Long>();
    		readThreadMap = new HashMap<Integer,long[]> ();
    		
    		sharedArrayIds = new HashSet<Integer>();
    		arrayIdsMap = new HashMap<Integer,HashSet<Integer>>();
    		writeThreadArrayMap = new HashMap<Integer,Long>();
    		readThreadArrayMap = new HashMap<Integer,long[]>();
    		
    		threadLocalIDSet = new ThreadLocal<HashSet<Integer>>()
    				{
		    			protected HashSet<Integer> initialValue() {
		
		                    return new HashSet<Integer>();
		
		    			}
    				};
	    		threadLocalIDSet2 = new ThreadLocal<HashSet<Integer>>()
	    				{
			    			protected HashSet<Integer> initialValue() {
			
			                    return new HashSet<Integer>();
			
			    			}
	    				};
    	}
    	else
    	{
			//appname = "org.eclipse.equinox.launcher.Main";
			try{
				
//				StackTraceElement[] stack = Thread.currentThread ().getStackTrace ();
//				StackTraceElement main = stack[stack.length - 1];
	
			initNonSharing(false);
			}catch(Exception e)
			{
				//e.printStackTrace();
			}
    	}
	}
	/**
	 * initialize the database engine
	 * @throws Exception
	 */
	public static void initNonSharing(boolean newTable) throws Exception
	{
		long tid = Thread.currentThread().getId();
		db= new DBEngine(Config.logDir, Config.tableName);
		
		//load sharedvariables and sharedarraylocations
		GlobalStateForInstrumentation.instance.setSharedArrayLocations(db.loadSharedArrayLocs());
		GlobalStateForInstrumentation.instance.setSharedVariables(db.loadSharedVariables());
		
		db.createTraceTable(newTable);	
		
		//create table for storing thread id to unique identifier map
		db.createThreadIdTable(newTable);
		
		threadTidNameMap = new HashMap<Long,String>();
		if(newTable)
		db.saveThreadTidNameToDB(tid, MAIN_NAME);
		
		threadTidNameMap.put(tid, MAIN_NAME);

		threadTidIndexMap = new HashMap<Long,Integer>();
		threadTidIndexMap.put(tid, 1);
		
	}
	public static void saveSharedMetaData(HashSet<String> sharedVariables,
			HashSet<String> sharedArrayLocations) {
		
		DBEngine db= new DBEngine(Config.logDir, Config.tableName);
		try {
	    	if(Config.instance.verbose)
	    		System.out.println("====================SHARED VARIABLES===================");
	    	
			db.createSharedVarSignatureTable();
			for(String sig: sharedVariables)
			{
		    	db.saveSharedVarSignatureToDB(sig);
		    	if(Config.instance.verbose)
    			System.out.println(sig);
			}

	    	if(Config.instance.verbose)
	    		System.out.println("====================SHARED ARRAY LOCATIONS===================");
	    	
			db.createSharedArrayLocTable();
			for(String sig: sharedArrayLocations)
			{
		    	  db.saveSharedArrayLocToDB(sig);
				if(Config.instance.verbose)
	    			System.out.println(sig);
			}
			
			db.closeDB();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void saveMetaData(ConcurrentHashMap<String,Integer> variableIdMap,
			HashSet<String> volatilevariables,
			ConcurrentHashMap<String,Integer> stmtSigIdMap, boolean isVerbose)
	{
		try{
			//just reuse the connection 
			
			//TODO: if db is null or closed, there must be something wrong
			DBEngine db= new DBEngine(Config.logDir, Config.tableName);
			
		//save variable - id to database
		  db.createVarSignatureTable();
	      for(Map.Entry<String,Integer> entry: variableIdMap.entrySet())
	      {
	    	  String sig = entry.getKey();
	    	  Integer id = entry.getValue();
	    	  
	    	  db.saveVarSignatureToDB(sig, id);
	    	  if(isVerbose)
        	  System.out.println("* ["+id+"] "+sig+" *");

    	  }
	      
	    //save volatilevariable - id to database
		  db.createVolatileSignatureTable();
	      Iterator<String> volatileIt = volatilevariables.iterator();
	      while(volatileIt.hasNext())
	      {
	    	  String sig = volatileIt.next();
	    	  Integer id = variableIdMap.get(sig);
	    	  
	    	  db.saveVolatileSignatureToDB(sig, id);
	    	  if(isVerbose)System.out.println("* volatile: ["+id+"] "+sig+" *");

    	  }
	      //save stmt - id to database
		  db.createStmtSignatureTable();

	      for(Entry<String,Integer> entry: stmtSigIdMap.entrySet())
	      {
	    	  String sig = entry.getKey();
	    	  Integer id = entry.getValue();
	    	  
	    	  //System.out.println("* ["+id+"] "+sig+" *");
	    	  db.saveStmtSignatureToDB(sig, id);
	      }
	      
	      db.closeDB();
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}

  public static  void logBranch(int ID) {
	  
	  db.saveEventToDB(Thread.currentThread().getId(), ID, "", "", db.tracetypetable[9]);
  }
  public static void logBasicBlock(int ID)
  {
	  db.saveEventToDB(Thread.currentThread().getId(), ID, "", "", db.tracetypetable[10]);
  }

  public static  void logWait(int ID,final Object o) {
	  long tid = Thread.currentThread().getId();
	  db.saveEventToDB(tid, ID, ""+System.identityHashCode(o), "", db.tracetypetable[5]);
	 
  }

  public static  void logNotify(int ID,final Object o) {
	  long tid = Thread.currentThread().getId();
	  db.saveEventToDB(tid, ID, ""+System.identityHashCode(o), "", db.tracetypetable[6]);

  }

  public static  void logStaticSyncLock(int ID, int SID) {
	  long tid = Thread.currentThread().getId();
	  String addr = ""+SID;
	  db.saveEventToDB(tid, ID, addr, "", db.tracetypetable[3]);
  }

  public static  void logStaticSyncUnlock(int ID, int SID) {
	  long tid = Thread.currentThread().getId();
	  db.saveEventToDB(tid, ID, ""+SID, "", db.tracetypetable[4]);
  }

  public static  void logLock(int ID, final Object lock) {
	  
	  long tid = Thread.currentThread().getId();
	  db.saveEventToDB(tid, ID, ""+System.identityHashCode(lock), "", db.tracetypetable[3]);
  }

  public static  void logUnlock(int ID,final Object lock) {
	  long tid = Thread.currentThread().getId();
	  db.saveEventToDB(tid, ID, ""+System.identityHashCode(lock), "", db.tracetypetable[4]);
 }
  public static  void logFileAcc(String name, boolean write)
  {
	  long tid = Thread.currentThread().getId();
	  String str = "write";
	  if(!write) str = "read";
	  
	  System.out.println("Thread "+tid+" "+str+" to file "+name);
  }
  /**
   * detect shared variables -- two conditions
   * 1. the address is accessed by more than two threads 
   * 2. at least one of them is a write
   * @param ID -- shared variable id
   * @param o -- runtime object
   * @param SID -- field id
   * @param write or read
   */
  public static  void logFieldAcc(int ID, int SID, final boolean write) {
	  long tid = Thread.currentThread().getId();
	  
	  {
		  if(!threadLocalIDSet.get().contains(ID))
		  {
			  if(threadLocalIDSet2.get().contains(ID))
				  threadLocalIDSet.get().add(ID);
			  else
				  threadLocalIDSet2.get().add(ID);
			  
			  
			  //o is not used...
			  
			  //instance-based approach consumes too much memory
			  
			  //String sig = o==null?"."+SID:System.identityHashCode(o)+"."+SID;
	
			  if(Config.instance.verbose)
			  {
			  String readOrWrite = (write?" write":" read");
			  System.out.println("Thread "+tid+" "+readOrWrite+" variable "+SID);		  
			  }
			 if(!sharedVariableIds.contains(SID))
			 {
				 if(writeThreadMap.containsKey(SID))
				 {
					 if(writeThreadMap.get(SID)!=tid)
					 {
						 sharedVariableIds.add(SID);
						 return;
					 }
				 }
				 
				 if(write)//write
				 {
					 if(readThreadMap.containsKey(SID))
					 {
						 long[] readThreads = readThreadMap.get(SID);
						 if(readThreads!=null
								 &&(readThreads[0]!=tid||
								 (readThreads[1]>0&&readThreads[1]!=tid)))
						 {
							 sharedVariableIds.add(SID);
							 return;
						 }
					 }
					 
					 writeThreadMap.put(SID, tid);
				}
				 else//read
				 {
					 long[] readThreads = readThreadMap.get(SID);
					 
					 if(readThreads==null)
					 {
						 readThreads = new long[2];
						 readThreads[0]= tid;
						 readThreadMap.put(SID, readThreads); 
					 }	 
					 else
					 {
						 if(readThreads[0]!=tid)
							 readThreads[1]= tid;
	
					 }
				 }
			 }
		  }
	  }
  }
  public static  void logFieldAcc(int ID, final Object o, int SID, final Object v, final boolean write) {
	  long tid = Thread.currentThread().getId();
	  {
		  
		  //shared object reference variable deference
		  //make it as a branch event
		  
		  int hashcode_o = System.identityHashCode(o);
		  if(!isPrim(v))
		  {
			  db.saveEventToDB(tid, ID, o==null?"_."+SID:hashcode_o+"_."+SID, isPrim(v)?v+"":System.identityHashCode(v)+"_", write?db.tracetypetable[2]: db.tracetypetable[1]);

			  logBranch(-1);
		  }
		  else
			  db.saveEventToDB(tid, ID, o==null?"."+SID:hashcode_o+"."+SID, isPrim(v)?v+"":System.identityHashCode(v)+"_", write?db.tracetypetable[2]: db.tracetypetable[1]);
 
	  }
	  

  }
  
  public static void logInitialWrite(int ID, final Object o, int index, final Object v){
	  
	  try{
	  long tid = Thread.currentThread().getId();
	  db.saveEventToDB(tid, ID, o==null?"."+index:System.identityHashCode(o)+"."+index, isPrim(v)?v+"":System.identityHashCode(v)+"", db.tracetypetable[0]);
	  }catch(Exception e)
	  {
		  e.printStackTrace();
		  if(db==null)
			  System.out.println("DB is null in logInitialWrite - appname is ");
	  }
	  
  }
  public static  void logArrayAcc(int ID, final Object o, int index, final boolean write) {
	  long tid = Thread.currentThread().getId();
	  
//	  StringBuilder builder = new StringBuilder(20);
//	  builder.append(ID).append('.').append(sig);
//	  String identifier = builder.toString();
	  
	  //String identifier = ID+"."+sig;
	  //System.out.println(identifier);
	  if(!threadLocalIDSet.get().contains(ID))
	  {
		  if(threadLocalIDSet2.get().contains(ID))
			  threadLocalIDSet.get().add(ID);
		  else
			  threadLocalIDSet2.get().add(ID);
		  
		  Integer sig = System.identityHashCode(o);//+"_"+index;//array

		  HashSet<Integer> ids = arrayIdsMap.get(sig);
		  if(ids==null){
			  ids = new HashSet<Integer>();
			  arrayIdsMap.put(sig, ids);
		  }
		  ids.add(ID);
		  if(Config.instance.verbose)
		  {	  
			  String readOrWrite = (write?" write":" read");
		  	System.out.println("Thread "+tid+" "+readOrWrite+" array "+GlobalStateForInstrumentation.instance.getArrayLocationSig(ID));		  
		  }
		 if(!sharedArrayIds.contains(sig))
		 {
			 if(writeThreadArrayMap.containsKey(sig))
			 {
				 if(writeThreadArrayMap.get(sig)!=tid)
				 {
					 sharedArrayIds.add(sig);
					 return;
				 }
			 }
			 
			 if(write)//write
			 {
				 if(readThreadArrayMap.containsKey(sig))
				 {
					 long[] readThreads = readThreadArrayMap.get(sig);
					 if(readThreads!=null
							 &&(readThreads[0]!=tid||
							 (readThreads[1]>0&&readThreads[1]!=tid)))
					 {
						 sharedArrayIds.add(sig);
						 return;
					 }
				 }
				 
				 writeThreadArrayMap.put(sig, tid);
			}
			 else//read
			 {
				 long[] readThreads = readThreadArrayMap.get(sig);
				 
				 if(readThreads==null)
				 {
					 readThreads = new long[2];
					 readThreads[0]= tid;
					 readThreadArrayMap.put(sig, readThreads); 
				 }	 
				 else
				 {
					 if(readThreads[0]!=tid)
						 readThreads[1]= tid;
	
				 }
			 }
		 }
	 }
  }
  public static  void logArrayAcc(int ID, final Object o, int index, final Object v, final boolean write) {
	  long tid = Thread.currentThread().getId();
	  db.saveEventToDB(tid, ID, System.identityHashCode(o)+"_"+index, isPrim(v)?v+"":System.identityHashCode(v)+"_", write?db.tracetypetable[2]: db.tracetypetable[1]);
  }

  private static boolean isPrim(Object o)
  {
	  if(o instanceof Integer
			  || o instanceof Long
			  || o instanceof Byte
			  || o instanceof Boolean
			  || o instanceof Float
			  || o instanceof Double
			  || o instanceof Short
			  || o instanceof Character)
		  return true;
	  
	  return false;
  }

  /**
   * When starting a new thread, a consistent unique identifier of the thread
   * is created, and stored into a map with the thread id as the key.
   * The unique identifier, i.e, name, is a concatenation of the name of the 
   * parent thread with the order of children threads forked by the parent thread.
   * @param ID
   * @param o
   */
  public static  void logStart(int ID, final Object o) {
	  long tid = Thread.currentThread().getId();
	  Thread t = (Thread) o;
	  long tid_t = t.getId();
	  
	  String name =  threadTidNameMap.get(tid);
	  //it's possible that name is NULL, because this thread is started from library: e.g., AWT-EventQueue-0
	  if(name==null)
	  {
		  name = Thread.currentThread().getName();
		  threadTidIndexMap.put(tid, 1);
		  threadTidNameMap.put(tid,name);
	  }
	  
	  int index = threadTidIndexMap.get(tid);
	  
	  if(name.equals(MAIN_NAME))
		  name = ""+index;
	  else
		  name = name+"."+index;
	  
	  threadTidNameMap.put(tid_t, name);
	  threadTidIndexMap.put(tid_t, 1);

	  index++;
	  threadTidIndexMap.put(tid, index);

	  db.saveThreadTidNameToDB(tid_t, name);
	  
	  db.saveEventToDB(tid, ID, ""+tid_t, "", db.tracetypetable[7]);
	  

  }
  public static  void logJoin(int ID, final Object o) {
	  
	  db.saveEventToDB(Thread.currentThread().getId(), ID, ""+((Thread) o).getId(), "", db.tracetypetable[8]);

  }

}
