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
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.Map.Entry;

import config.Configuration;
import trace.AbstractNode;
import trace.IMemNode;
import trace.LockPair;
import trace.ReadNode;
import trace.Trace;
import trace.TraceInfo;
import trace.WriteNode;
import trace.AbstractNode.TYPE;
import violation.AtomicityViolation;
import violation.Deadlock;
import violation.ExactRace;
import violation.IViolation;
import violation.Race;
import z3.Z3Engine;
import db.DBEngine;

/**
 * HBRaceDetect implements the happens-before methods for race detection.
 * 
 * @author jeffhuang
 *
 */
public class HBRaceDetect {

	private static Configuration config;
	private static HashSet<IViolation> races= new HashSet<IViolation>();
	private static PrintWriter out;
	private static void initPrinter(String appname)
	{
		try{
		String fname = "result."+(config.window_size/1000)+"k";
		out = new PrintWriter(new FileWriter(fname,true));
		out.println("\n------------------ HB: "+appname+" -------------------\n");
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	private static void closePrinter()
	{
		if(out!=null)
			out.close();
	}	

	private static Vector<String> trim(Vector<String> schedule)
	{
		if(schedule.size()>100)
		{
			Vector<String> s = new Vector<String>();
			s.add("...");
			for(int i=schedule.size()-100;i<schedule.size();i++)
				s.add(schedule.get(i));
			return s;
		}
		else
			return schedule;
	}
	/**
	 * traverse all conflicting pairs. For each pair, query the CPEngine 
	 * whether there are reachable or not. If yes, report a race.
	 * 
	 * @param engine
	 * @param trace
	 */
	private static void detectRace(HBEngine engine, Trace trace)
	{
		Iterator<String> 
		addrIt =trace.getIndexedThreadReadWriteNodes().keySet().iterator();
		while(addrIt.hasNext())
		{
			
			String addr = addrIt.next();
			if(config.novolatile)
			{
			//all field addr should contain ".", not true for array access
			int dotPos = addr.indexOf(".");
			//continue if volatile
			if(dotPos>-1&&trace.isAddressVolatile(addr.substring(dotPos+1))) continue;
			}
			//get all read nodes on the address
			Vector<ReadNode> readnodes = trace.getIndexedReadNodes().get(addr);
			
			//get all write nodes on the address
			Vector<WriteNode> writenodes = trace.getIndexedWriteNodes().get(addr);
			if(writenodes==null||writenodes.size()<1)
			continue;
						
			//System.out.println("***** Checking Data Race *****\n");
			//check race read-write
			if(readnodes!=null)
			for(int i=0;i<readnodes.size();i++)
			{
				ReadNode rnode = readnodes.get(i);
				
				for(int j=0;j<writenodes.size();j++)
				{
					WriteNode wnode = writenodes.get(j);
					if(rnode.getTid()!=wnode.getTid())
					{
						Race race = new Race(trace.getStmtSigIdMap().get(rnode.getID()),
								trace.getStmtSigIdMap().get(wnode.getID()),rnode.getID(),wnode.getID());
						
						if(config.allrace||!races.contains(race))
						{
											
//							if(race.toString().equals("<org.w3c.util.SyncLRUList: void toHead(org.w3c.util.LRUAble)>|$r3 = $r2.<org.w3c.util.LRUNode: org.w3c.util.LRUAble next>|14 - <org.w3c.util.LRUNode: void setNext(org.w3c.util.LRUAble)>|r0.<org.w3c.util.LRUNode: org.w3c.util.LRUAble next> = r1|36"))
//								System.out.print("");
							

							
							if(engine.isRace(rnode, wnode))
							{
								report("Race: "+race,MSGTYPE.REAL);
								//report(rnode.getGID()+"--"+wnode.getGID(),false);
								if(config.allrace)
								{
									ExactRace race2 = new ExactRace(race,(int)rnode.getGID(),(int)wnode.getGID());
									races.add(race2); 

								}
								else races.add(race);
							
							}
						
						}
					}
				}
			}
				//check race write-write
				for(int i=0;i<writenodes.size();i++)
				{
					WriteNode wnode1 = writenodes.get(i);
					
					for(int j=0;j<writenodes.size();j++)
					{
						WriteNode wnode2 = writenodes.get(j);
						if(wnode1.getTid()!=wnode2.getTid())
						{
							Race race = new Race(trace.getStmtSigIdMap().get(wnode1.getID()),
									trace.getStmtSigIdMap().get(wnode2.getID()),wnode1.getID(),wnode2.getID());
							
							if(config.allrace||!races.contains(race))
							{
								if(engine.isRace(wnode1, wnode2))
								{
									report("Race: "+race,MSGTYPE.REAL);
									if(config.allrace)
									{
										ExactRace race2 = new ExactRace(race,(int)wnode1.getGID(),(int)wnode2.getGID());
										races.add(race2); 

									}
									else
									races.add(race); 
								}
							}
						}
					}
				}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		config = new Configuration(args);
				
		try{
			
			//start predict analysis
			long start_time = System.currentTimeMillis();
			//initialize printer
			initPrinter(config.appname);
			
			//db engine is used for interacting with database
			DBEngine db = new DBEngine(config.appname);

			//load all the metadata in the application
			HashMap<Integer, String> sharedVarIdSigMap = db.getSharedVarSigIdMap();
			HashMap<Integer, String> volatileAddresses = db.getVolatileAddresses();
			HashMap<Integer, String> stmtIdSigMap = db.getStmtSigIdMap();
			HashMap<Long,String> threadIdNameMap = db.getThreadIdNameMap();

			TraceInfo info = new TraceInfo(sharedVarIdSigMap,volatileAddresses,stmtIdSigMap,threadIdNameMap);



			long TOTAL_TRACE_LENGTH = db.getTraceSize();



			
			
			ExecutionInfoTask task = new ExecutionInfoTask(start_time,info,TOTAL_TRACE_LENGTH);
			//register a shutdown hook to store runtime statistics
			Runtime.getRuntime().addShutdownHook(task);
			
			//set a timer to timeout in a configured period
			Timer timer = new Timer();
			timer.schedule(new TimerTask(){
					public void run()
					{
						report("\n******* Timeout "+config.timeout+" seconds ******",MSGTYPE.REAL);//report it
						System.exit(0);
					}},config.timeout*1000);
			
			
			
			
			
			
			
			for(int round =0;round*config.window_size<TOTAL_TRACE_LENGTH;round++)
			{
				long index_start = round*config.window_size+1;
				long index_end = (round+1)*config.window_size;
				//if(TOTAL_TRACE_LENGTH>MAX_LENGTH)System.out.println("***************** Round "+(round+1)+": "+index_start+"-"+index_end+"/"+TOTAL_TRACE_LENGTH+" ******************\n");
				
				Trace trace = db.getTrace(index_start,index_end,info);
					
				HBEngine engine = new HBEngine(trace);				
					
				detectRace(engine,trace);
				
			}	
			
				
		}
		catch(Exception e)
		  {
			  e.printStackTrace();
		  }
		finally
		{
			
			//terminate
			System.exit(0);
		}

	}
	
	
	static class ExecutionInfoTask extends Thread
	{
		TraceInfo info;
		long start_time;
		long TOTAL_TRACE_LENGTH;
		ExecutionInfoTask (long st, TraceInfo info, long size)
		{
			this.info = info;
			this.start_time =st;
			this.TOTAL_TRACE_LENGTH = size;
		}
		
		@Override
		public void run() {
			
			//Report statistics about the trace and race detection
			
            //TODO: query the following information from DB may be expensive
			
			//int TOTAL_THREAD_NUMBER = db.getTraceThreadNumber();
			int TOTAL_THREAD_NUMBER = info.getTraceThreadNumber();
			//int TOTAL_SHAREDVARIABLE_NUMBER = db.getTraceSharedVariableNumber();
			int TOTAL_SHAREDVARIABLE_NUMBER = info.getTraceSharedVariableNumber();
			//int TOTAL_BRANCH_NUMBER = db.getTraceBranchNumber();
			//int TOTAL_READWRITE_NUMBER = db.getTraceReadWriteNumber();
			int TOTAL_READWRITE_NUMBER = info.getTraceSharedReadWriteNumber();
			//int TOTAL_SYNC_NUMBER = db.getTraceSyncNumber();
			int TOTAL_SYNC_NUMBER = info.getTraceSyncNumber();
			//int TOTAL_PROPERTY_NUMBER = db.getTracePropertyNumber();
			
			report("Trace Size: "+TOTAL_TRACE_LENGTH,MSGTYPE.STATISTICS);
			report("Total #Threads: "+TOTAL_THREAD_NUMBER,MSGTYPE.STATISTICS);
			report("Total #SharedVariables: "+TOTAL_SHAREDVARIABLE_NUMBER,MSGTYPE.STATISTICS);
			report("Total #Read-Writes: "+TOTAL_READWRITE_NUMBER,MSGTYPE.STATISTICS);
			report("Total #Synchronizations: "+TOTAL_SYNC_NUMBER,MSGTYPE.STATISTICS);
			report("Total #races: "+races.size(),MSGTYPE.STATISTICS);
			report("Total Time: "+(System.currentTimeMillis()-start_time)+"ms",MSGTYPE.STATISTICS); 			
		
			closePrinter();
		}
		
	}
	
	public enum MSGTYPE
	{
		REAL,POTENTIAL,STATISTICS
	}
	private static void report(String msg, MSGTYPE type)
	{
		switch(type)
		{
		case REAL:
			System.err.println(msg);
			out.println(msg);
			break;
		case STATISTICS:
			System.out.println(msg);
			out.println(msg);
			break;
		case POTENTIAL:
			break;
		default: break;
		}
	}
	
}
