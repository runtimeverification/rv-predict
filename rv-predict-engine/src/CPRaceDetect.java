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
import java.util.Vector;
import java.util.Map.Entry;

import trace.AbstractNode;
import trace.IMemNode;
import trace.LockPair;
import trace.ReadNode;
import trace.Trace;
import trace.WriteNode;
import trace.AbstractNode.TYPE;
import violation.AtomicityViolation;
import violation.Deadlock;
import violation.IViolation;
import violation.Race;
import z3.Z3Engine;
import db.DBEngine;

public class CPRaceDetect {

	private static int MAX_LENGTH = 1000;
	private static HashSet<IViolation> races= new HashSet<IViolation>();
	private static PrintWriter out;
	private static void initPrinter(String appname)
	{
		try{
		String fname = "result."+(MAX_LENGTH/1000)+"k";
		out = new PrintWriter(new FileWriter(fname,true));
		out.println("\n------------------ CP: "+appname+" -------------------\n");
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
	private static void report(String msg, boolean isRealRace)
	{
		out.println(msg);
		
		if(isRealRace)
		{
			System.err.println(msg);
		}
		else
			System.out.println(msg);
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
	private static void detectRace(CPEngine engine, Trace trace)
	{
		Iterator<String> 
		addrIt =trace.getIndexedThreadReadWriteNodes().keySet().iterator();
		while(addrIt.hasNext())
		{
			
			String addr = addrIt.next();
			
			
			//get all read nodes on the address
			Vector<ReadNode> readnodes = trace.getIndexedReadNodes().get(addr);
			
			//get all write nodes on the address
			Vector<WriteNode> writenodes = trace.getIndexedWriteNodes().get(addr);
			if(writenodes==null||writenodes.size()<2)
			continue;
						
			//System.out.println("***** Checking Data Race *****\n");
			//check race read-write
			if(readnodes!=null)
			for(int i=0;i<readnodes.size();i++)
			{
				ReadNode rnode = readnodes.get(i);
				
				for(int j=1;j<writenodes.size();j++)//skip the initial write node
				{
					WriteNode wnode = writenodes.get(j);
					if(rnode.getTid()!=wnode.getTid())
					{
						Race race = new Race(trace.getStmtSigIdMap().get(rnode.getID()),
								trace.getStmtSigIdMap().get(wnode.getID()),rnode.getID(),wnode.getID());
						
						if(!races.contains(race))
						{
														
							if(engine.isRace(rnode, wnode))
							{
								report("Race: "+race,true);
								races.add(race);
							
							}
						
						}
					}
				}
			}
				//check race write-write
				for(int i=1;i<writenodes.size();i++)//skip the initial write node
				{
					WriteNode wnode1 = writenodes.get(i);
					
					for(int j=1;j<writenodes.size();j++)
					{
						WriteNode wnode2 = writenodes.get(j);
						if(wnode1.getTid()!=wnode2.getTid())
						{
							Race race = new Race(trace.getStmtSigIdMap().get(wnode1.getID()),
									trace.getStmtSigIdMap().get(wnode2.getID()),wnode1.getID(),wnode2.getID());
							
							if(!races.contains(race))
							{
								if(engine.isRace(wnode1, wnode2))
								{
									report("Race: "+race,true);
									
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

		//emp.Example stringbuffer.StringBufferTest
		if(args.length==0)
		{
			System.err.println("Usage: java CPRaceDetect classname [window-size]");
			return;
		}
		
		String appname = args[0];
		if(args.length>1)
			MAX_LENGTH = Integer.valueOf(args[1]);
		
		initPrinter(appname);
		
		try{
			

			
			DBEngine db = new DBEngine(appname);

			HashMap<Integer, String> sharedVarIdSigMap = db.getSharedVarSigIdMap();
			HashMap<Integer, String> stmtIdSigMap = db.getStmtSigIdMap();
			HashMap<Long,String> threadIdNameMap = db.getThreadIdNameMap();
			

			long TOTAL_TRACE_LENGTH = db.getTraceSize();
			
			int TOTAL_THREAD_NUMBER = db.getTraceThreadNumber();
			int TOTAL_SHAREDVARIABLE_NUMBER = db.getTraceSharedVariableNumber();
			int TOTAL_READWRITE_NUMBER = db.getTraceReadWriteNumber();
			int TOTAL_SYNC_NUMBER = db.getTraceSyncNumber();


			//start predict analysis
			long start_time = System.currentTimeMillis();
			
			for(int round =0;round*MAX_LENGTH<TOTAL_TRACE_LENGTH;round++)
			{
				long index_start = round*MAX_LENGTH+1;
				long index_end = (round+1)*MAX_LENGTH;
				//if(TOTAL_TRACE_LENGTH>MAX_LENGTH)System.out.println("***************** Round "+(round+1)+": "+index_start+"-"+index_end+"/"+TOTAL_TRACE_LENGTH+" ******************\n");
				
				Trace trace = db.getTrace(index_start,index_end);
				
				trace.setSharedVarIdSigMap(sharedVarIdSigMap);
				trace.setStmtIdSigMap(stmtIdSigMap);
				trace.setThreadIdNameMap(threadIdNameMap);	
					
				CPEngine engine = new CPEngine(trace);				
					
				detectRace(engine,trace);
				
			}	
			
			report("Trace Size: "+TOTAL_TRACE_LENGTH,false);
			report("Total #Threads: "+TOTAL_THREAD_NUMBER,false);
			report("Total #SharedVariables: "+TOTAL_SHAREDVARIABLE_NUMBER,false);
			report("Total #Read-Writes: "+TOTAL_READWRITE_NUMBER,false);
			report("Total #Synchronizations: "+TOTAL_SYNC_NUMBER,false);
			report("Total #races: "+races.size(),false);
			report("Total Time: "+(System.currentTimeMillis()-start_time)+"ms",false); 			
			
		}
		catch(Exception e)
		  {
			  e.printStackTrace();
		  }
		finally
		{
			closePrinter();
		}
	}
}
