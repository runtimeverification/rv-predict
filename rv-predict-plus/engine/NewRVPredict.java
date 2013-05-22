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

public class NewRVPredict {

	private static int MAX_LENGTH = 1000;
	private static HashSet<IViolation> violations= new HashSet<IViolation>();

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
					
										System.out.println("Deadlock : "+deadlock);
					
										System.out.println("Schedule: "+schedule+"\n");
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
								trace.getStmtSigIdMap().get(wnode.getID()));
						
						if(!violations.contains(race))
						{
							
							//MAYBE we can provide a simple reachability analysis to reduce the solver invocations
							
							//get dependent nodes of rnode and wnode
							Vector<ReadNode> readNodes_r = trace.getDependentReadNodes(rnode);
							Vector<ReadNode> readNodes_w = trace.getDependentReadNodes(wnode);

							StringBuilder sb1 = engine.constructCausalReadWriteConstraintsOptimized(readNodes_r, trace.getIndexedWriteNodes());
							StringBuilder sb2 = engine.constructCausalReadWriteConstraintsOptimized(readNodes_w, trace.getIndexedWriteNodes());

							//TODO: NEED to ensure that the other non-dependent nodes by other threads are not included
							if(engine.isRace(rnode, wnode,sb1,sb2))
							{
								
								System.out.println("Race: "+race);

								Vector<String> schedule_a = engine.getSchedule(wnode.getGID(),trace.getNodeGIDTIdMap(),trace.getThreadIdNameMap());

								Vector<String> schedule_b = new Vector<String>(schedule_a);
								
								
								String str1 = schedule_b.remove(schedule_b.size()-1);
								String str2 = schedule_b.remove(schedule_b.size()-1);
								
								assert(str1!=str2);
								
								schedule_b.add(str1);
								schedule_b.add(str2);

								
								schedule_a.addAll(0, schedule_prefix);
								schedule_b.addAll(0, schedule_prefix);

								//TODO: need to remove all the after nodes in the schedule 
								race.addSchedule(schedule_a);
								race.addSchedule(schedule_b);

								violations.add(race);
							
								System.out.println("Schedule_a: "+schedule_a);
								System.out.println("Schedule_b: "+schedule_b+"\n");
							
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
									trace.getStmtSigIdMap().get(wnode2.getID()));
							
							if(!violations.contains(race))
							{
								//get dependent nodes of wnode1 and wnode2
								Vector<ReadNode> readNodes_w1 = trace.getDependentReadNodes(wnode1);
								Vector<ReadNode> readNodes_w2 = trace.getDependentReadNodes(wnode2);

								StringBuilder sb1 = engine.constructCausalReadWriteConstraintsOptimized(readNodes_w1, trace.getIndexedWriteNodes());
								StringBuilder sb2 = engine.constructCausalReadWriteConstraintsOptimized(readNodes_w2, trace.getIndexedWriteNodes());

								
								if(engine.isRace(wnode1, wnode2,sb1,sb2))
								{
									System.out.println("Race: "+race);

									Vector<String> schedule_a = engine.getSchedule(wnode2.getGID(),trace.getNodeGIDTIdMap(),trace.getThreadIdNameMap());
								

								
									Vector<String> schedule_b = new Vector<String>(schedule_a);
									
									
									String str1 = schedule_b.remove(schedule_b.size()-1);
									String str2 = schedule_b.remove(schedule_b.size()-1);
									
									assert(str1!=str2);
									
									schedule_b.add(str1);
									schedule_b.add(str2);
									
								
									schedule_a.addAll(0, schedule_prefix);
									schedule_b.addAll(0, schedule_prefix);

									race.addSchedule(schedule_a);
									race.addSchedule(schedule_b);
									
									violations.add(race); 
								
									System.out.println("Schedule_a: "+schedule_a);
									System.out.println("Schedule_b: "+schedule_b+"\n");
								
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
			if(writenodes==null||writenodes.size()<2)
			continue;
			
			WriteNode initWrite = writenodes.get(0);
			
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
								
								if((node1.getType()==TYPE.WRITE
										||node2.getType()==TYPE.WRITE
										||node3.getType()==TYPE.WRITE)&&
										node1.getGID()!=initWrite.getGID()
												&&node1.getGID()!=initWrite.getGID())
								{
									AtomicityViolation av = new AtomicityViolation(trace.getStmtSigIdMap().get(node1.getID()),
																				trace.getStmtSigIdMap().get(node3.getID()),
																				trace.getStmtSigIdMap().get(node2.getID()));
									
									
									if(!violations.contains(av))
									{
										
										//get dependent read nodes
										Vector<ReadNode> readNodes_1 = trace.getDependentReadNodes(node1);
										Vector<ReadNode> readNodes_2 = trace.getDependentReadNodes(node2);
										Vector<ReadNode> readNodes_3 = trace.getDependentReadNodes(node3);

										StringBuilder sb1 = engine.constructCausalReadWriteConstraintsOptimized(readNodes_1, trace.getIndexedWriteNodes());
										StringBuilder sb2 = engine.constructCausalReadWriteConstraintsOptimized(readNodes_2, trace.getIndexedWriteNodes());
										StringBuilder sb3 = engine.constructCausalReadWriteConstraintsOptimized(readNodes_3, trace.getIndexedWriteNodes());

										
										if(engine.isAtomicityViolation(node1, node2, node3,sb1,sb2,sb3))
										{
											Vector<String> schedule = engine.getSchedule(node3.getGID(),trace.getNodeGIDTIdMap(),trace.getThreadIdNameMap());
																					
											schedule.addAll(0, schedule_prefix);

											av.addSchedule(schedule);
											
											violations.add(av);
	
											System.out.println("Atomicity Violation: "+av);
		
											System.out.println("Schedule: "+schedule+"\n");
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
		// TODO Auto-generated method stub

		String appname = "emp.Simple";//emp.Example stringbuffer.StringBufferTest
		if(args.length>0)
			appname = args[0];
		
		try{
			DBEngine db = new DBEngine(appname);

			HashMap<Integer, String> sharedVarIdSigMap = db.getSharedVarSigIdMap();
			HashMap<Integer, String> stmtIdSigMap = db.getStmtSigIdMap();
			HashMap<Long,String> threadIdNameMap = db.getThreadIdNameMap();
			
			

			long TOTAL_TRACE_LENGTH = db.getTraceSize();
			
			Vector<String> schedule_prefix = new Vector<String>();

			Z3Engine engine = new Z3Engine(appname);

			for(int round =0;round*MAX_LENGTH<TOTAL_TRACE_LENGTH;round++)
			{
				if(TOTAL_TRACE_LENGTH>MAX_LENGTH)System.out.println("***************** Round "+(round+1)+" ******************\n");
				
				Trace trace = db.getTrace(round*MAX_LENGTH+1,(round+1)*MAX_LENGTH);
				
				trace.setSharedVarIdSigMap(sharedVarIdSigMap);
				trace.setStmtIdSigMap(stmtIdSigMap);
				trace.setThreadIdNameMap(threadIdNameMap);	
					
				//1. declare all variables 
				engine.declareVariables(trace.getFullTrace());
				//2. intra-thread order for all nodes, excluding branches and basic block transitions
				engine.addIntraThreadConstraints(trace.getThreadNodesMap());
				//3. order for locks, signals, fork/joins
				engine.addSynchronizationConstraints(trace,trace.getSyncNodesMap(),trace.getThreadFirstNodeMap(),trace.getThreadLastNodeMap());
				
				//4. match read-write
				//TODO: what if we don't have read-write constraints?
				//engine.addReadWriteConstraints(trace.getIndexedReadNodes(),trace.getIndexedWriteNodes());
				//engine.addReadWriteConstraints(trace.getIndexedReadNodes(),trace.getIndexedWriteNodes());

				boolean detectRace = false;
			
				boolean detectAtomicityViolation = false;
				boolean detectDeadlock = false;
				
				//let's only detect round 2
//				if(round==1)
					detectAtomicityViolation = true;
					detectRace = true;
					//detectDeadlock = true;
					
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
				
			}	
			
			//save schedules to db
			int size = violations.size();
			if(size>0)
			{

				db.createScheduleTable();
				size = db.saveSchedulesToDB(violations);
				System.out.println("Total #Schedules: "+size);

			}
			
		}
		catch(Exception e)
		  {
			  e.printStackTrace();
		  }
	}
	
	private static Vector<String> getTraceSchedule(Trace trace) {
		
		Vector<String> fullschedule = new Vector<String>();
		
		for(int k =0;k<trace.getFullTrace().size();k++)
			fullschedule.add(trace.getThreadIdNameMap().get(trace.getFullTrace().get(k).getTid()));
		
		return fullschedule;
	}
	

	

}