import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;

import trace.IMemNode;
import trace.ReadNode;
import trace.Trace;
import trace.WriteNode;
import trace.AbstractNode.TYPE;
import z3.Z3Engine;
import db.DBEngine;


public class NewRVPredict {

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
			
			Trace trace = db.getTrace();
			
			HashMap<Long,String> threadIdNameMap = db.getThreadIdNameMap();
			trace.setThreadIdNameMap(threadIdNameMap);			
			
			
			Z3Engine engine = new Z3Engine();
			//1. declare all variables 
			engine.declareVariables(trace.getFullTrace());
			//2. intra-thread order for all nodes, excluding branches and basic block transitions
			engine.addIntraThreadConstraints(trace.getThreadNodesMap());
			//3. order for locks, signals, fork/joins
			engine.addSynchronizationConstraints(trace.getSyncNodesMap(),trace.getThreadFirstNodeMap(),trace.getThreadLastNodeMap());
			//4. match read-write
			engine.addReadWriteConstraints(trace.getIndexedReadNodes(),trace.getIndexedWriteNodes());

			HashMap<String,Vector<String>> schedules = new HashMap<String,Vector<String>>();
			
			boolean detectRace = true;
			boolean detectAtomicityViolation = true;
			
			HashMap<String,HashMap<Long,Vector<IMemNode>>> indexedThreadReadWriteNodes 
									= trace.getIndexedThreadReadWriteNodes();
			
			Iterator<Entry<String, Vector<ReadNode>>> 
							entryIt =trace.getIndexedReadNodes().entrySet().iterator();
			while(entryIt.hasNext())
			{
				Entry<String, Vector<ReadNode>> entry = entryIt.next();
				String addr = entry.getKey();
				
				int pos = addr.indexOf('.');
				int sid = Integer.valueOf(addr.substring(pos+1));
				//System.out.println(sharedVarIdSigMap.get(sid));
				
				//get all read nodes on the address
				Vector<ReadNode> readnodes = entry.getValue();
						
				//get all write nodes on the address
				Vector<WriteNode> writenodes = trace.getIndexedWriteNodes().get(addr);
				if(writenodes==null)
					continue;
				
				WriteNode initWrite = writenodes.get(0);
				
				//System.out.println("***** Checking Data Race *****\n");
				//check race read-write
				if(detectRace)
				for(int i=0;i<readnodes.size();i++)
				{
					ReadNode rnode = readnodes.get(i);
					
					for(int j=1;j<writenodes.size();j++)//skip the initial write node?
					{
						WriteNode wnode = writenodes.get(j);
						if(rnode.getTid()!=wnode.getTid())
						{
							if(engine.isRace(rnode, wnode))
							{
								String sig = stmtIdSigMap.get(rnode.getID())+
										" - "+stmtIdSigMap.get(wnode.getID());
								Vector<String> schedule_a = engine.getSchedule(trace.getNodeGIDTIdMap(),threadIdNameMap);

								engine.isRace(wnode,rnode);
								Vector<String> schedule_b = engine.getSchedule(trace.getNodeGIDTIdMap(),threadIdNameMap);

								
								
							
								String sig_a = sig+" * a";

							if(!schedules.containsKey(sig_a))
							{
								System.out.println("Race: "+sig);

								String sig_b = sig+" * b";

								schedules.put(sig_a, schedule_a);
								schedules.put(sig_b, schedule_b);

								System.out.println("Schedule_a: "+schedule_a);
								System.out.println("Schedule_b: "+schedule_b+"\n");

							}
							
							}
						}
					}
				}
				//check race write-write
				if(detectRace)
				for(int i=1;i<writenodes.size();i++)//skip the initial write node?
				{
					WriteNode wnode1 = writenodes.get(i);
					
					for(int j=1;j<writenodes.size();j++)
					{
						WriteNode wnode2 = writenodes.get(j);
						if(wnode1.getTid()!=wnode2.getTid())
						{
							if(engine.isRace(wnode1, wnode2))
							{
								String sig = stmtIdSigMap.get(wnode1.getID())+
								" - "+stmtIdSigMap.get(wnode2.getID());
								Vector<String> schedule_a = engine.getSchedule(trace.getNodeGIDTIdMap(),threadIdNameMap);

								engine.isRace(wnode2,wnode1);
								Vector<String> schedule_b = engine.getSchedule(trace.getNodeGIDTIdMap(),threadIdNameMap);

								
								
							
								String sig_a = sig+" * a";

							if(!schedules.containsKey(sig_a))
							{
								System.out.println("Race: "+sig);

								String sig_b = sig+" * b";

								schedules.put(sig_a, schedule_a);
								schedules.put(sig_b, schedule_b);

								System.out.println("Schedule_a: "+schedule_a);
								System.out.println("Schedule_b: "+schedule_b+"\n");

							}
							}
						}
					}
				}
				
				//System.out.println("\n***** Checking Atomicity Violations *****\n");

				//check atomicity-violation all nodes
				HashMap<Long,Vector<IMemNode>> threadReadWriteNodes = indexedThreadReadWriteNodes.get(addr);
				
				Object[] threads = threadReadWriteNodes.keySet().toArray();
				if(detectAtomicityViolation)
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
										if(engine.isAtomicityViolation(node1, node2, node3))
										{
											String sig = stmtIdSigMap.get(node1.getID())+
													" - "+stmtIdSigMap.get(node3.getID())+
													" - "+stmtIdSigMap.get(node2.getID());
											
											
											Vector<String> schedule = engine.getSchedule(trace.getNodeGIDTIdMap(),threadIdNameMap);
																						
											if(!schedules.containsKey(sig))
											{
												schedules.put(sig, schedule);

											System.out.println("Atomicity Violation: "+sig);

											System.out.println("Schedule: "+schedule+"\n");
											}
										}
									}
								}
							}
						}
					}
			}
			
			//save schedules to db
			int size = schedules.size();
			if(size>0)
			{
				System.out.println("Total #Schedules: "+size);

				db.createScheduleTable();
				db.saveSchedulesToDB(schedules);
			}
			
		}
		catch(Exception e)
		  {
			  e.printStackTrace();
		  }
	}

}
