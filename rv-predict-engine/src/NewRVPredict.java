import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;

import config.Configuration;

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

public class NewRVPredict {

	private static HashSet<IViolation> violations= new HashSet<IViolation>();
	private static HashSet<IViolation> potentialviolations= new HashSet<IViolation>();
	private static PrintWriter out;
	private static Configuration config;
	private static boolean detectRace = true;
	private static boolean detectAtomicityViolation = false;
	private static boolean detectDeadlock = false;
	
	private static void initPrinter(String appname)
	{
		try{
		String fname = "result."+(config.window_size/1000)+"k";
		out = new PrintWriter(new FileWriter(fname,true));
		
		String type = "";
		if(config.rmm_pso)
			type+="pso: ";
		
		if(config.nobranch)
			type += "maximal: ";
		else if(config.allconsistent)
			type += "Said et al.: ";
		else
			type += "maximal-branch: ";
		out.println("\n------------------ "+type+appname+" -------------------\n");
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
	
	private static void detectDeadlock(Z3Engine engine,Trace trace,Vector<String> schedule_prefix)
	{
		HashMap<Long,HashMap<String,Vector<LockPair>>> threadIndexedLockPairs 
			= trace.getThreadIndexedLockPairs();
		Object[] threads = threadIndexedLockPairs.keySet().toArray();
		for(int i=0;i<threads.length-1;i++)
		{
			Set<String> lockset1 = threadIndexedLockPairs.get(threads[i]).keySet();
			if(lockset1.size()>1)
			for(int j=1;j<threads.length;j++)
			{
				Set<String> lockset2 = new HashSet(threadIndexedLockPairs.get(threads[j]).keySet());
				lockset2.retainAll(lockset1);
				if(lockset2.size()>1)
				{
				HashMap<String,Vector<LockPair>> indexedLockpairs1 = threadIndexedLockPairs.get(threads[i]);
				HashMap<String,Vector<LockPair>> indexedLockpairs2 = threadIndexedLockPairs.get(threads[j]);
				Object[] addrs = lockset2.toArray();
				for(int k1=0;k1<addrs.length-1;k1++)
				{
				Vector<LockPair> vlp1a = indexedLockpairs1.get(addrs[k1]);
				Vector<LockPair> vlp2a = indexedLockpairs2.get(addrs[k1]);
				
				for(int k2=1;k2<addrs.length;k2++)
				{
				Vector<LockPair> vlp1b = indexedLockpairs1.get(addrs[k2]);
				Vector<LockPair> vlp2b = indexedLockpairs2.get(addrs[k2]);
				
				for(int i1=0;i1<vlp1a.size();i1++)
				{
				 LockPair lp1a = vlp1a.get(i1);
				 for(int i2=0;i2<vlp2a.size();i2++)
				 {
					 LockPair lp2a = vlp2a.get(i2);
					 for(int i3=0;i3<vlp1b.size();i3++)
					 {
						 LockPair lp1b = vlp1b.get(i3);
						 for(int i4=0;i4<vlp2b.size();i4++)
						 {
							 LockPair lp2b = vlp2b.get(i4);
							 
							 if(lp1a.lock==null||
									 (lp1b.lock!=null&&lp1a.lock.getGID()>lp1b.lock.getGID()))
							 {
								 LockPair templp = lp1b;
								 lp1b = lp1a;
								 lp1a = templp;
							 }
							 
							 if(lp2b.lock==null||
									 (lp2a.lock!=null&&lp2b.lock.getGID()>lp2a.lock.getGID()))
							 {
								 LockPair templp = lp2a;
								 lp2a = lp2b;
								 lp2b = templp;
							 }

							 Deadlock deadlock = new Deadlock(trace.getStmtSigIdMap().get(lp1a.lock.getID()),
																trace.getStmtSigIdMap().get(lp2b.lock.getID()),
																trace.getStmtSigIdMap().get(lp1b.lock.getID()),
																trace.getStmtSigIdMap().get(lp2a.lock.getID()));

							 if(!violations.contains(deadlock))
							 {
								 if(engine.isDeadlock(lp1a,lp2b,lp1b,lp2a))
								 {
										Vector<String> schedule = engine.getSchedule(lp1b.lock.getGID(),trace.getNodeGIDTIdMap(),trace.getThreadIdNameMap());
														
										schedule.addAll(0, schedule_prefix);

										deadlock.addSchedule(schedule);
										
										violations.add(deadlock);
					
										report("Deadlock : "+deadlock,true);
					
										report("Schedule: "+trim(schedule)+"\n",true);
								}
							}
				
						 }
					 }
				 }
				}
				
				}
				
				}
				
				}
			}
		}
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
	private static void detectRace(Z3Engine engine, Trace trace, Vector<String> schedule_prefix)
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
						
						if(!violations.contains(race))//&&!potentialviolations.contains(race) may miss real violation with the same signature
						{
							
							if(engine.hasCommonLock(rnode,wnode))
								continue;
							
							//a simple reachability analysis to reduce the solver invocations
							if(rnode.getGID()<wnode.getGID())
							{
								if(engine.canReach(rnode, wnode))
									continue;
							}
							else
							{
								if(engine.canReach(wnode,rnode))
									continue;
							}
							
							
							StringBuilder sb;
							if(config.allconsistent)
							{
								Vector<ReadNode> readNodes_rw = trace.getAllReadNodes();
								sb = engine.constructCausalReadWriteConstraintsOptimized(readNodes_rw, 
										trace.getIndexedWriteNodes(),trace.getInitialWriteValueMap());
							}
							else
							{
							//get dependent nodes of rnode and wnode
							Vector<ReadNode> readNodes_r = trace.getDependentReadNodes(rnode,config.nobranch);
							Vector<ReadNode> readNodes_w = trace.getDependentReadNodes(wnode,config.nobranch);
							
							
							
							StringBuilder sb1 = engine.constructCausalReadWriteConstraintsOptimized(readNodes_r, 
												trace.getIndexedWriteNodes(),trace.getInitialWriteValueMap());
							StringBuilder sb2 = engine.constructCausalReadWriteConstraintsOptimized(readNodes_w, 
												trace.getIndexedWriteNodes(),trace.getInitialWriteValueMap());
							sb = sb1.append(sb2);
							}
							//TODO: NEED to ensure that the other non-dependent nodes by other threads are not included
							if(engine.isRace(rnode, wnode,sb))
							{
								
								report("Race: "+race,true);
								violations.add(race);
								
								
								if(config.noschedule)
									continue;
								
								Vector<String> schedule_a = engine.getSchedule(wnode.getGID(),trace.getNodeGIDTIdMap(),trace.getThreadIdNameMap());

								Vector<String> schedule_b = new Vector<String>(schedule_a);
								
								
								String str1 = schedule_b.remove(schedule_b.size()-1);
								
								int pos = schedule_b.size()-1;
								String str2 = schedule_b.remove(pos);
								while(str1==str2)
								{
									pos--;
									schedule_b.add(str2);
									str2 = schedule_b.remove(pos);
								}
								
								schedule_b.add(str1);
								schedule_b.add(str2);

								
								schedule_a.addAll(0, schedule_prefix);
								schedule_b.addAll(0, schedule_prefix);

								//TODO: need to remove all the after nodes in the schedule 
								//add the reverse order first
								if(rnode.getGID()<wnode.getGID())
								{
									race.addSchedule(schedule_a);
									race.addSchedule(schedule_b);
								}
								else
								{
									race.addSchedule(schedule_b);
									race.addSchedule(schedule_a);
								}
								
							
								report("Schedule_a: "+trim(schedule_a),true);
								report("Schedule_b: "+trim(schedule_b)+"\n",true);
							
							}
							else
							{
								//if we arrive here, it means we find a case where lockset+happens-before could produce false positive
								if(potentialviolations.add(race))
									report("Potential Race: "+race,false);

							}
						
						}
					}
				}
			}
				//check race write-write
				for(int i=0;i<writenodes.size();i++)//skip the initial write node
				{
					WriteNode wnode1 = writenodes.get(i);
					
					for(int j=0;j<writenodes.size();j++)
					{
						WriteNode wnode2 = writenodes.get(j);
						if(wnode1.getTid()!=wnode2.getTid())
						{
							Race race = new Race(trace.getStmtSigIdMap().get(wnode1.getID()),
									trace.getStmtSigIdMap().get(wnode2.getID()),wnode1.getID(),wnode2.getID());
							
							if(!violations.contains(race))//&&!potentialviolations.contains(race)
							{
								if(engine.hasCommonLock(wnode1,wnode2))
									continue;
								
								if(wnode1.getGID()<wnode2.getGID())
								{
									if(engine.canReach(wnode1, wnode2))
										continue;
								}
								else
								{
									if(engine.canReach(wnode2,wnode1))
										continue;
								}
								
								StringBuilder sb;
								if(config.allconsistent)
								{
									Vector<ReadNode> readNodes_ww = trace.getAllReadNodes();
									sb = engine.constructCausalReadWriteConstraintsOptimized(readNodes_ww, 
											trace.getIndexedWriteNodes(),trace.getInitialWriteValueMap());
								}
								else
								{
								//get dependent nodes of rnode and wnode
								Vector<ReadNode> readNodes_w1 = trace.getDependentReadNodes(wnode1,config.nobranch);
								Vector<ReadNode> readNodes_w2 = trace.getDependentReadNodes(wnode2,config.nobranch);
								
								
								
								StringBuilder sb1 = engine.constructCausalReadWriteConstraintsOptimized(readNodes_w1, 
													trace.getIndexedWriteNodes(),trace.getInitialWriteValueMap());
								StringBuilder sb2 = engine.constructCausalReadWriteConstraintsOptimized(readNodes_w2, 
													trace.getIndexedWriteNodes(),trace.getInitialWriteValueMap());
								sb = sb1.append(sb2);
								}
								//TODO: NEED to ensure that the other non-dependent nodes by other threads are not included
								if(engine.isRace(wnode1, wnode2,sb))
								{
									report("Race: "+race,true);

									violations.add(race); 
									if(config.noschedule)
										continue;
									

									Vector<String> schedule_a = engine.getSchedule(wnode2.getGID(),trace.getNodeGIDTIdMap(),trace.getThreadIdNameMap());
								

								
									Vector<String> schedule_b = new Vector<String>(schedule_a);
									
									
									String str1 = schedule_b.remove(schedule_b.size()-1);
									int pos = schedule_b.size()-1;
									String str2 = schedule_b.remove(pos);
									while(str1==str2)
									{
										pos--;
										schedule_b.add(str2);
										str2 = schedule_b.remove(pos);
									}
									
									schedule_b.add(str1);
									schedule_b.add(str2);
									
								
									schedule_a.addAll(0, schedule_prefix);
									schedule_b.addAll(0, schedule_prefix);

									if(wnode1.getGID()<wnode2.getGID())
									{
										race.addSchedule(schedule_a);
										race.addSchedule(schedule_b);
									}
									else
									{
										race.addSchedule(schedule_b);
										race.addSchedule(schedule_a);
									}
									
								
									report("Schedule_a: "+trim(schedule_a),true);
									report("Schedule_b: "+trim(schedule_b)+"\n",true);
								
								}
								else
								{
									//if we arrive here, it means we find a case where lockset+happens-before could produce false positive
									if(potentialviolations.add(race))
										report("Potential Race: "+race,false);

								}
							}
						}
					}
				}
		}
	}
	private static void detectAtomicityViolation(Z3Engine engine, Trace trace,Vector<String> schedule_prefix)
	{
	
		HashMap<String,HashMap<Long,Vector<IMemNode>>> indexedThreadReadWriteNodes 
		= trace.getIndexedThreadReadWriteNodes();
		Iterator<Entry<String, Vector<ReadNode>>> 
		entryIt =trace.getIndexedReadNodes().entrySet().iterator();
		while(entryIt.hasNext())
		{
			Entry<String, Vector<ReadNode>> entry = entryIt.next();
			String addr = entry.getKey();
			
			//get all write nodes on the address
			Vector<WriteNode> writenodes = trace.getIndexedWriteNodes().get(addr);
			if(writenodes==null||writenodes.size()<1)
			continue;
						
			//check atomicity-violation all nodes
			HashMap<Long,Vector<IMemNode>> threadReadWriteNodes = indexedThreadReadWriteNodes.get(addr);
			
			Object[] threads = threadReadWriteNodes.keySet().toArray();
	
			for(int i=0;i<threads.length-1;i++)
				for(int j=i+1;j<threads.length;j++)
				{
					Vector<IMemNode> rwnodes1 = threadReadWriteNodes.get(threads[i]);
					
					Vector<IMemNode> rwnodes2 = threadReadWriteNodes.get(threads[j]);
	
					if(rwnodes1!=null&rwnodes2!=null&&rwnodes1.size()>1)
					{
						for(int k=0;k<rwnodes1.size()-1;k++)
						{
							//require atomic region specification
							IMemNode node1 = rwnodes1.get(k);
							IMemNode node2 = rwnodes1.get(k+1);
							
							for(int m =0;m<rwnodes2.size();m++)
							{
								IMemNode node3 = rwnodes2.get(m);
								
								if(node1.getType()==TYPE.WRITE
										||node2.getType()==TYPE.WRITE
										||node3.getType()==TYPE.WRITE)
								{
									AtomicityViolation av = new AtomicityViolation(trace.getStmtSigIdMap().get(node1.getID()),
																				trace.getStmtSigIdMap().get(node3.getID()),
																				trace.getStmtSigIdMap().get(node2.getID()),node1.getID(),node2.getID(),node3.getID());
									
									
									if(!violations.contains(av))//&&!potentialviolations.contains(av)
									{
										
										//it's possible that av is true in the original execution
										//no need to model its constraints
										//TODO: optimize it!
										
										//TODO: Need to check lock-history
										if(engine.isAtomic(node1,node2,node3))
											continue;
										
										if(node3.getGID()<node1.getGID())
										{
											if(engine.canReach((AbstractNode)node3, (AbstractNode)node1))
												continue;
										}
										else if(node3.getGID()>node2.getGID())
										{
											if(engine.canReach((AbstractNode)node2, (AbstractNode)node3))
												continue;
										}
										
										//get dependent read nodes
										Vector<ReadNode> readNodes_1 = trace.getDependentReadNodes(node1,config.nobranch);
										Vector<ReadNode> readNodes_2 = trace.getDependentReadNodes(node2,config.nobranch);
										Vector<ReadNode> readNodes_3 = trace.getDependentReadNodes(node3,config.nobranch);

										StringBuilder sb1 = engine.constructCausalReadWriteConstraintsOptimized(readNodes_1, 
												trace.getIndexedWriteNodes(),trace.getInitialWriteValueMap());
										StringBuilder sb2 = engine.constructCausalReadWriteConstraintsOptimized(readNodes_2, 
												trace.getIndexedWriteNodes(),trace.getInitialWriteValueMap());
										StringBuilder sb3 = engine.constructCausalReadWriteConstraintsOptimized(readNodes_3, 
												trace.getIndexedWriteNodes(),trace.getInitialWriteValueMap());

										
										if(engine.isAtomicityViolation(node1, node2, node3,sb1,sb2,sb3))
										{
											
											report("Atomicity Violation: "+av,true);

											violations.add(av);
	
											if(config.noschedule)
												continue;
											
											Vector<String> schedule = engine.getSchedule(node3.getGID(),trace.getNodeGIDTIdMap(),trace.getThreadIdNameMap());
											
											schedule.addAll(0, schedule_prefix);

											av.addSchedule(schedule);
											report("Schedule: "+trim(schedule)+"\n",true);
										}
										else
										{
											//if we arrive here, it means we find a case where lockset+happens-before could produce false positive
											report("Potential Atomicity Violation: "+av+"\n",false);
											potentialviolations.add(av);

										}
									}
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

		config = new Configuration(args);
		
		
		initPrinter(config.appname);
		
		try{
			
			DBEngine db = new DBEngine(config.appname);

			HashMap<Integer, String> sharedVarIdSigMap = db.getSharedVarSigIdMap();
			HashMap<Integer, String> stmtIdSigMap = db.getStmtSigIdMap();
			HashMap<Long,String> threadIdNameMap = db.getThreadIdNameMap();
			

			long TOTAL_TRACE_LENGTH = db.getTraceSize();
			
			int TOTAL_THREAD_NUMBER = db.getTraceThreadNumber();
			int TOTAL_SHAREDVARIABLE_NUMBER = db.getTraceSharedVariableNumber();
			int TOTAL_BRANCH_NUMBER = db.getTraceBranchNumber();
			int TOTAL_READWRITE_NUMBER = db.getTraceReadWriteNumber();
			int TOTAL_SYNC_NUMBER = db.getTraceSyncNumber();

			Vector<String> schedule_prefix = new Vector<String>();

			Z3Engine engine = new Z3Engine(config.appname);
			HashMap<String,String> initialWriteValueMap = new HashMap<String,String>();
			
			//start predict analysis
			long start_time = System.currentTimeMillis();
			
			for(int round =0;round*config.window_size<TOTAL_TRACE_LENGTH;round++)
			{
				long index_start = round*config.window_size+1;
				long index_end = (round+1)*config.window_size;
				//if(TOTAL_TRACE_LENGTH>MAX_LENGTH)System.out.println("***************** Round "+(round+1)+": "+index_start+"-"+index_end+"/"+TOTAL_TRACE_LENGTH+" ******************\n");
				
				Trace trace = db.getTrace(index_start,index_end);
				
				trace.setSharedVarIdSigMap(sharedVarIdSigMap);
				trace.setStmtIdSigMap(stmtIdSigMap);
				trace.setThreadIdNameMap(threadIdNameMap);	
				
				if(round>0)trace.setInitialWriteValueMap(initialWriteValueMap);
				
				
				//1. declare all variables 
				engine.declareVariables(trace.getFullTrace());
				//2. intra-thread order for all nodes, excluding branches and basic block transitions
				if(config.rmm_pso)//TODO: add intra order between sync
					engine.addPSOIntraThreadConstraints(trace.getIndexedThreadReadWriteNodes());
				else
					engine.addIntraThreadConstraints(trace.getThreadNodesMap());

				//3. order for locks, signals, fork/joins
				engine.addSynchronizationConstraints(trace,trace.getSyncNodesMap(),trace.getThreadFirstNodeMap(),trace.getThreadLastNodeMap());
				
				//4. match read-write
				//TODO: what if we don't have read-write constraints?
				//engine.addReadWriteConstraints(trace.getIndexedReadNodes(),trace.getIndexedWriteNodes());
				//engine.addReadWriteConstraints(trace.getIndexedReadNodes(),trace.getIndexedWriteNodes());

					
				if(detectDeadlock)//when detecting deadlocks, make sure the lock constraints of the rest unlock nodes are disabled
				{
					 detectDeadlock(engine,trace,schedule_prefix);
				}
				if(detectRace)
				{
					detectRace(engine,trace,schedule_prefix);
				}
				if(detectAtomicityViolation)
				{
					detectAtomicityViolation(engine,trace,schedule_prefix);
				}
				
				schedule_prefix.addAll(getTraceSchedule(trace));
				
				//get last write value from the current trace 
				//as the initial value for the next round
				initialWriteValueMap = trace.getInitialWriteValueMap();
				trace.saveLastWriteValues(initialWriteValueMap); 
			}	
			
			report("Trace Size: "+TOTAL_TRACE_LENGTH,false);
			report("Total #Threads: "+TOTAL_THREAD_NUMBER,false);
			report("Total #SharedVariables: "+TOTAL_SHAREDVARIABLE_NUMBER,false);
			report("Total #Read-Writes: "+TOTAL_READWRITE_NUMBER,false);
			report("Total #Synchronizations: "+TOTAL_SYNC_NUMBER,false);
			report("Total #Branches: "+TOTAL_BRANCH_NUMBER,false);
			report("Total #Potential Violations: "+(potentialviolations.size()+violations.size()),false);
			report("Total #Real Violations: "+violations.size(),false);
			report("Total Time: "+(System.currentTimeMillis()-start_time)+"ms",false); 
			//System.out.println("Total #Schedules: "+size_schedule);
			
			
			if(!config.noschedule)
			{
				//save schedules to db
				int size = violations.size();
				int size_schedule =0;
				if(size>0)
				{
					db.createScheduleTable();
					size_schedule = db.saveSchedulesToDB(violations);			
				}
			}
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
	
	private static Vector<String> getTraceSchedule(Trace trace) {
		
		Vector<String> fullschedule = new Vector<String>();
		
		for(int k =0;k<trace.getFullTrace().size();k++)
			fullschedule.add(trace.getThreadIdNameMap().get(trace.getFullTrace().get(k).getTid()));
		
		return fullschedule;
	}
	

	

}