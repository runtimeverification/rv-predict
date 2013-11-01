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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import db.DBEngine;
import db.DBEngineParallel;

public final class RecordRT {

//	static HashMap<Long,HashMap<Object,Stack<Object>>>
//		threadReEntrantLockMap = new HashMap<Long,HashMap<Object,Stack<Object>>>();
	
	//The following three options are for testing purposes
	final static boolean doTraceFilter = false;//true if we reduce redundant accesses
	final static boolean doInterleave = false;//true if we use random schedule
	final static boolean doLogInParallel = false;//true if we log in parallel
	
	static HashMap<Long,HashSet<Integer>> threadlocalWriteIDSet;
	static HashMap<Long,HashSet<Integer>> threadlocalReadIDSet;

	//can be computed during offline analysis
	static HashMap<Long,String> threadTidNameMap;
	static HashMap<Long,Integer> threadTidIndexMap;
	final static String MAIN_NAME = "0";
//	private static boolean isReEntrant(long tid, String addr, boolean isLock)
//	{
//		HashMap<Object,Stack<Object>> lockStackMap = threadReEntrantLockMap.get(tid);
//		if(lockStackMap==null)
//		{
//			lockStackMap = new HashMap<Object,Stack<Object>>();
//			threadReEntrantLockMap.put(tid, lockStackMap);
//		}
//		Stack<Object> stack = lockStackMap.get(addr);
//		if(stack==null)
//		{
//			stack = new Stack<Object>();
//			lockStackMap.put(addr, stack);
//		}
//		
//		boolean reentrant = false;
//		if(!stack.isEmpty())
//			reentrant = true;
//		
//		if(isLock)
//			stack.push(new Object());
//		else
//			stack.pop();
//
//		return reentrant;
//	}
//	private static void attachShutDownHook()
//	{
//		Runtime.getRuntime().addShutdownHook(new Thread() {
//			   @Override
//			   public void run() {
//				   db.finishLogging();
//			   }
//			  });
//
//	}
	
	//engine for storing events into database
	static DBEngine db;
	
	/**
	 * initialize the database engine
	 * @param appname
	 * @throws Exception
	 */
	public static void init(String appname) throws Exception
	{
		long tid = Thread.currentThread().getId();

		if(doLogInParallel)
		{
			db= new DBEngineParallel(appname);//use parallel db engine
			((DBEngineParallel)db).createTraceTable(tid);

		}
		else
		{
			db= new DBEngine(appname);
			db.createTraceTable();
		}	
		
		//create table for storing thread id to unique identifier map
		db.createThreadIdTable();
		
		//db.startAsynchronousLogging();//perform asynchronous logging, hopefully we can improve performance
		
		//attachShutDownHook();
		

		threadTidNameMap = new HashMap<Long,String>();
		db.saveThreadTidNameToDB(tid, MAIN_NAME);
		
		threadTidNameMap.put(tid, MAIN_NAME);

		threadTidIndexMap = new HashMap<Long,Integer>();
		threadTidIndexMap.put(tid, 1);
		
		if(doTraceFilter)
		{
			threadlocalReadIDSet = new HashMap<Long,HashSet<Integer>>();
			threadlocalWriteIDSet = new HashMap<Long,HashSet<Integer>>();
		}
	}
	private static void clearThreadLocalRWIDSet(long tid)
	{
		HashSet<Integer> set = threadlocalWriteIDSet.get(tid);
		if(set==null)
		{
			set = new HashSet<Integer>();
			threadlocalWriteIDSet.put(tid, set);
		}
		threadlocalWriteIDSet.clear();
		
		set = threadlocalReadIDSet.get(tid);
		if(set==null)
		{
			set = new HashSet<Integer>();
			threadlocalReadIDSet.put(tid, set);
		}
		threadlocalReadIDSet.clear();
	}
	private static boolean isRedundant(long tid, int ID, boolean write)
	{
		if(write)
		{
			HashSet<Integer> set = threadlocalWriteIDSet.get(tid);
			if(set==null)
			{
				set = new HashSet<Integer>();
				threadlocalWriteIDSet.put(tid, set);
			}
			
			if(set.contains(ID))
				return true;
			else
				set.add(ID);
		}
		else
		{
			HashSet<Integer> set = threadlocalReadIDSet.get(tid);
			if(set==null)
			{
				set = new HashSet<Integer>();
				threadlocalReadIDSet.put(tid, set);
			}
			
			if(set.contains(ID))
				return true;
			else
				set.add(ID);
		}
		
		return false;
	}
  public static  void logBranch(int ID) {
	  
	  db.saveEventToDB(Thread.currentThread().getId(), ID, "", "", db.tracetypetable[9]);
  }
  public static void logBasicBlock(int ID)
  {
	  db.saveEventToDB(Thread.currentThread().getId(), ID, "", "", db.tracetypetable[10]);
  }

  public static  void logWait(int ID,final Object o) {
	  if(doInterleave)Thread.yield();
	  long tid = Thread.currentThread().getId();
	  if(doTraceFilter)
		  clearThreadLocalRWIDSet(tid);
	  db.saveEventToDB(tid, ID, ""+System.identityHashCode(o), "", db.tracetypetable[5]);
	 
  }

  public static  void logNotify(int ID,final Object o) {
	  if(doInterleave)Thread.yield();
	  long tid = Thread.currentThread().getId();
	  if(doTraceFilter)
		  clearThreadLocalRWIDSet(tid);
	  db.saveEventToDB(tid, ID, ""+System.identityHashCode(o), "", db.tracetypetable[6]);

  }

  public static  void logStaticSyncLock(int ID, int SID) {
	  if(doInterleave)Thread.yield();
	  long tid = Thread.currentThread().getId();
	  String addr = ""+SID;
	  if(doTraceFilter)
		  clearThreadLocalRWIDSet(tid);
	  
	  db.saveEventToDB(tid, ID, addr, "", db.tracetypetable[3]);

  }

  public static  void logStaticSyncUnlock(int ID, int SID) {
	  if(doInterleave)Thread.yield();
	  long tid = Thread.currentThread().getId();
	  if(doTraceFilter)
		  clearThreadLocalRWIDSet(tid);
	  db.saveEventToDB(tid, ID, ""+SID, "", db.tracetypetable[4]);
  }

  public static  void logLock(int ID, final Object lock) {
	  
	  if(doInterleave)Thread.yield();
	  long tid = Thread.currentThread().getId();
	  if(doTraceFilter)
		  clearThreadLocalRWIDSet(tid);
	  db.saveEventToDB(tid, ID, ""+System.identityHashCode(lock), "", db.tracetypetable[3]);
  }

  public static  void logUnlock(int ID,final Object lock) {
	  if(doInterleave)Thread.yield();
	  long tid = Thread.currentThread().getId();
	  if(doTraceFilter)
		  clearThreadLocalRWIDSet(tid);
	  db.saveEventToDB(tid, ID, ""+System.identityHashCode(lock), "", db.tracetypetable[4]);

 }

  
  public static  void logFieldAcc(int ID, final Object o, int SID, final Object v, final boolean write) {
	  if(doInterleave)Thread.yield();
	  long tid = Thread.currentThread().getId();
	  if(!doTraceFilter||!isRedundant(tid,ID,write))
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
  
  public static  void logFieldAcc(int ID, final Object o, int SID, final boolean write) {
	  if(doInterleave)Thread.yield();
	  long tid = Thread.currentThread().getId();
	  if(!doTraceFilter||!isRedundant(tid,ID,write))
	  {
		  db.saveEventToDB(tid, ID, o==null?SID+"":System.identityHashCode(o)+"."+SID, "LOOP", write?db.tracetypetable[2]: db.tracetypetable[1]);
	  }
  }
  public static void logInitialWrite(int ID, final Object o, int index, final Object v){
	  
	  if(doInterleave)Thread.yield();
	  long tid = Thread.currentThread().getId();
	  if(!doTraceFilter||!isRedundant(tid,ID,true))
	  db.saveEventToDB(tid, ID, o==null?"."+index:System.identityHashCode(o)+"."+index, isPrim(v)?v+"":System.identityHashCode(v)+"", db.tracetypetable[0]);
	  
	  
  }
  public static  void logArrayAcc(int ID, final Object o, int index, final Object v, final boolean write) {
	  if(doInterleave)Thread.yield();
	  long tid = Thread.currentThread().getId();
	  if(!doTraceFilter||!isRedundant(tid,ID,write))
	  db.saveEventToDB(tid, ID, System.identityHashCode(o)+"_"+index, isPrim(v)?v+"":System.identityHashCode(v)+"_", write?db.tracetypetable[2]: db.tracetypetable[1]);
  }
  public static  void logArrayAcc(int ID, final Object o, final boolean write) {
	  if(doInterleave)Thread.yield();
	  long tid = Thread.currentThread().getId();
	  if(!doTraceFilter||!isRedundant(tid,ID,write))
	  db.saveEventToDB(tid, ID, System.identityHashCode(o)+".0", "LOOP", write?db.tracetypetable[2]: db.tracetypetable[1]);
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
  
  public static void logPropertyEvent(String monitor,int ID){
	  
	  //add event to database
	  db.saveEventToDB(Thread.currentThread().getId(), ID, monitor, "", db.tracetypetable[11]);
	  //System.err.println("logPropertyEvent: "+Thread.currentThread().getId()+" "+monitor+" "+ID);

  }
  public static void setProperty(String name, int ID){
	  
	  //save property to database
	  if(ID==-1)
		  db.saveProperty(name,ID,true);
	  else db.saveProperty(name,ID,false);
	  
	  //System.err.println("setProperty: "+name +" "+ID);

  }

}
