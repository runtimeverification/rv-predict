package z3;

import trace.AbstractNode;
import trace.IMemNode;
import trace.ISyncNode;
import trace.JoinNode;
import trace.LockNode;
import trace.LockPair;
import trace.NotifyNode;
import trace.ReadNode;
import trace.StartNode;
import trace.Trace;
import trace.UnlockNode;
import trace.WaitNode;
import trace.WriteNode;

import graph.LockSetEngine;
import graph.ReachabilityEngine;

import java.io.BufferedReader;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;
import java.util.Map.Entry;

import config.Configuration;

public class YicesEngineSMTLIB1 extends Z3Engine
{
	String CONS_BENCHNAME;
	final String BRACKET_LEFT = "(";
	final String BRACKET_RIGHT = ")";
	
	public YicesEngineSMTLIB1(Configuration config)
	{
		super(config);
		CONS_BENCHNAME="(benchmark "+config.appname+".smt\n";
	}
	
	public void declareVariables(Vector<AbstractNode> trace)
	{
		CONS_SETLOGIC = ":logic QF_IDL\n";
		
		CONS_DECLARE = new StringBuilder(":extrafuns (\n");
		
		CONS_ASSERT = new StringBuilder(":formula (and \n");
		
		//CONS_ASSERT = "(assert (distinct ";
		int size = trace.size();
		for(int i=0;i<size;i++)
		{
			AbstractNode node = trace.get(i);
			long GID = node.getGID();
			String var = makeVariable(GID);
			
			CONS_DECLARE.append(BRACKET_LEFT).append(var).append(" Int)\n");
		
			//CONS_ASSERT.append(var).append(" ");
			
//			CONS_ASSERT.append("(assert (and (> ").append(var).append(" 0) (< ").append(var)
//			    .append(" ").append(size+1).append(")))\n");
		}
		
		CONS_DECLARE.append(BRACKET_RIGHT).append("\n");
						
	}
	public void addIntraThreadConstraints(HashMap<Long,Vector<AbstractNode>> map)
	{
		Iterator<Vector<AbstractNode>> mapIt = map.values().iterator();
		while(mapIt.hasNext())
		{
			Vector<AbstractNode> nodes = mapIt.next();
			long lastGID = nodes.get(0).getGID();
			String lastVar = makeVariable(lastGID);
			for(int i=1;i<nodes.size();i++)
			{			
				long thisGID  = nodes.get(i).getGID();
				String var = makeVariable(thisGID);
				CONS_ASSERT.append("(< ").append(lastVar).append(" ").append(var).append(")\n");
				
				reachEngine.addEdge(lastGID, thisGID);

				lastGID = thisGID;
				lastVar = var;
				
			}
		}
	}
	public void addPSOIntraThreadConstraints(HashMap<String,HashMap<Long,Vector<IMemNode>>> indexedMap)
	{
		
		Iterator<HashMap<Long,Vector<IMemNode>>> mapIt1 = indexedMap.values().iterator();
		while(mapIt1.hasNext())
		{
			HashMap<Long,Vector<IMemNode>> map = mapIt1.next();
			
			Iterator<Vector<IMemNode>> mapIt2 = map.values().iterator();
			while(mapIt2.hasNext())
			{
				Vector<IMemNode> nodes = mapIt2.next();
				long lastGID = nodes.get(0).getGID();
				String lastVar = makeVariable(lastGID);
				for(int i=1;i<nodes.size();i++)
				{			
					long thisGID  = nodes.get(i).getGID();
					String var = makeVariable(thisGID);
					CONS_ASSERT.append("(< ").append(lastVar).append(" ").append(var).append(")\n");
					
					reachEngine.addEdge(lastGID, thisGID);

					lastGID = thisGID;
					lastVar = var;
					
				}
			}
		}
		
	}
	//the order constraints between wait/notify/fork/join/lock/unlock
	public void addSynchronizationConstraints(Trace trace, HashMap<String,Vector<ISyncNode>> syncNodesMap, 
												HashMap<Long,AbstractNode> firstNodes,
														HashMap<Long,AbstractNode> lastNodes)
	{
		lockEngine = new LockSetEngine();//construct a new lockset for this segment

		//thread first node - last node
		Iterator<Vector<ISyncNode>> mapIt = syncNodesMap.values().iterator();
		while(mapIt.hasNext())
		{
			Vector<ISyncNode> nodes = mapIt.next();
			
			Vector<LockPair> lockPairs = new Vector<LockPair>();
			
			HashMap<Long,Stack<ISyncNode>> threadSyncStack = new HashMap<Long,Stack<ISyncNode>>();
			NotifyNode matchNotifyNode = null;
			
			//during recording
			//should after wait, before notify
			//after lock, before unlock
			
			for(int i=0;i<nodes.size();i++)
			{			
				ISyncNode node = nodes.get(i);
				long thisGID = node.getGID();
				String var = makeVariable(thisGID);
				if(node instanceof StartNode)
				{
					long tid = Long.valueOf(node.getAddr());
					AbstractNode fnode = firstNodes.get(tid);
					if(fnode!=null)
					{
						long fGID = fnode.getGID();
					String fvar = makeVariable(fGID);
					
					CONS_ASSERT.append("(< ").append(var).append(" ").append(fvar).append(")\n");
					
					reachEngine.addEdge(thisGID, fGID);
					
					}
				}
				else if (node instanceof JoinNode)
				{
					long tid = Long.valueOf(node.getAddr());
					AbstractNode lnode = lastNodes.get(tid);
					if(lnode!=null)
					{
						long lGID = lnode.getGID();
						String lvar = makeVariable(lGID);
						CONS_ASSERT.append("(< ").append(lvar).append(" ").append(var).append(")\n");
						reachEngine.addEdge(lGID,thisGID);

					}
					
				}
				else if (node instanceof LockNode)
				{
					long tid = node.getTid();
					
					Stack<ISyncNode> stack = threadSyncStack.get(tid);
					if(stack==null)
					{
						stack = new Stack<ISyncNode>();
						threadSyncStack.put(tid, stack);
					}
					
					stack.push(node);					
				}
				else if (node instanceof UnlockNode)
				{
					long tid = node.getTid();
					Stack<ISyncNode> stack = threadSyncStack.get(tid);
					
					//assert(stack.size()>0);//this is possible when segmented
					if(stack==null)
					{
						stack = new Stack<ISyncNode>();
						threadSyncStack.put(tid, stack);
					}
					
					//TODO: make sure no nested locks?

					if(stack.isEmpty())
					{
						LockPair lp = new LockPair(null,node);
						lockPairs.add(lp);
						lockEngine.add(node.getAddr(),tid,lp);
					}
					else if(stack.size()==1)
					{
						LockPair lp = new LockPair(stack.pop(),node);
						lockPairs.add(lp);
						
						lockEngine.add(node.getAddr(),tid,lp);
					}
					else
						stack.pop();//handle reentrant lock here
					
				}
				else if (node instanceof WaitNode)
				{
					long tid = node.getTid();

					//assert(matchNotifyNode!=null);this is also possible when segmented
					if(matchNotifyNode!=null)
					{
						long notifyGID = matchNotifyNode.getGID();
						String notifyVar = makeVariable(notifyGID);
						
	
						int nodeIndex = trace.getFullTrace().indexOf(node)+1;
						
						try{					
							//TODO: handle OutofBounds
							try{
						while(trace.getFullTrace().get(nodeIndex).getTid()!=tid)
							nodeIndex++;
							}catch(Exception e)
							{
								//if we arrive here, it means the wait node is the last node of the corresponding thread
								//so add an order from notify to wait instead
								nodeIndex = trace.getFullTrace().indexOf(node);
							}
						long waitNextGID = trace.getFullTrace().get(nodeIndex).getGID();
						var = makeVariable(waitNextGID);
						
						CONS_ASSERT.append("(< ").append(notifyVar).append(" ").append(var).append(")\n");
						
						reachEngine.addEdge(notifyGID,waitNextGID);

						}catch(Exception e)
						{
							e.printStackTrace();
						}
						
						//clear notifyNode
						matchNotifyNode=null;
					}
					
					Stack<ISyncNode> stack = threadSyncStack.get(tid);
					//assert(stack.size()>0);
					if(stack==null)
					{
						stack = new Stack<ISyncNode>();
						threadSyncStack.put(tid, stack);
					}
					if(stack.isEmpty())
						lockPairs.add(new LockPair(null,node));
					else if(stack.size()==1)
						lockPairs.add(new LockPair(stack.pop(),node));
					else
						stack.pop();//handle reentrant lock here
					
					stack.push(node);
					

				}
				else if (node instanceof NotifyNode)
				{
					matchNotifyNode = (NotifyNode)node;
				}
			}
			
			
			//check threadSyncStack 
			Iterator<Stack<ISyncNode>> stackIt = threadSyncStack.values().iterator();
			while(stackIt.hasNext())
			{
				Stack<ISyncNode> stack = stackIt.next();
				if(stack.size()>0)//handle reentrant lock here, only pop the first locking node
				{
					ISyncNode node= stack.firstElement();
					LockPair lp = new LockPair(node,null);
					lockPairs.add(lp);
					lockEngine.add(node.getAddr(),node.getTid(),lp);
				}
			}
						
			CONS_ASSERT.append(constructLockConstraintsOptimized(lockPairs));
		}
		
	}
	private String constructLockConstraintsOptimized(Vector<LockPair> lockPairs)
	{
		String CONS_LOCK = "";
		
		//obtain each thread's last lockpair
		HashMap<Long,LockPair> lastLockPairMap = new HashMap<Long,LockPair>();
		
		for(int i=0;i<lockPairs.size();i++)
		{
			LockPair lp1 = lockPairs.get(i);
			String var_lp1_a="";
			String var_lp1_b="";
			
			if(lp1.lock==null)//
				continue;
			else
				var_lp1_a = makeVariable(lp1.lock.getGID());
			
			if(lp1.unlock!=null)
				var_lp1_b = makeVariable(lp1.unlock.getGID());

			
			long lp1_tid = lp1.lock.getTid();
			LockPair lp1_pre = lastLockPairMap.get(lp1_tid);
			
			/*String var_lp1_pre_b = "";
			if(lp1_pre!=null)
				var_lp1_pre_b = makeVariable(lp1_pre.unlock.getGID());;
			*/
			
			ArrayList<LockPair> flexLockPairs = new ArrayList<LockPair>();
			
			//find all lps that are from a different thread, and have no happens-after relation with lp1
			//could further optimize by consider lock regions per thread
			for(int j=0;j<lockPairs.size();j++)
			{
				LockPair lp = lockPairs.get(j);
				if(lp.lock!=null)
				{
					if(lp.lock.getTid()!=lp1_tid&&
							!canReach((AbstractNode)lp1.lock,(AbstractNode)lp.lock))
					{
						flexLockPairs.add(lp);
					}
				}
				else if(lp.unlock!=null)
				{
					if(lp.unlock.getTid()!=lp1_tid&&
							!canReach((AbstractNode)lp1.lock,(AbstractNode)lp.unlock))
					{
						flexLockPairs.add(lp);
					}
				}						
			}
			if(flexLockPairs.size()>0)
			{
				String cons_b = "";
				String cons_b_end = "";

			for(int j=0;j<flexLockPairs.size();j++)
			{
				LockPair lp2 = flexLockPairs.get(j);
					
				if(lp2.unlock==null||lp2.lock==null&&lp1_pre!=null)//impossible to match lp2
						continue;
					
				String var_lp2_b="";
				String var_lp2_a="";
				
				var_lp2_b = makeVariable(lp2.unlock.getGID());
				
				if(lp2.lock!=null)
					var_lp2_a = makeVariable(lp2.lock.getGID());
				
				
				//lp1_b==null, lp2_a=null
				if(lp1.unlock==null||lp2.lock==null)
				{
					cons_b= "(> "+var_lp1_a+" "+var_lp2_b+")\n"+cons_b;

				}
				else
				{
					cons_b= "(or (> "+var_lp1_a+" "+var_lp2_b+") (> "+var_lp2_a+" "+var_lp1_b+"))\n"+cons_b;
				}
				
				/*String cons_b_ = "";				
				

				if(lp1_pre!=null)
				{
					
					cons_b_ = "(and (and (> "+var_lp1_a+" "+var_lp2_b+") (> "+var_lp2_a+" "+var_lp1_pre_b+"))\n";
				}
				else
				{
					cons_b_ = "(and (> "+var_lp1_a+" "+var_lp2_b+")\n";	//must hold here
				}*/
				
				if(j<flexLockPairs.size())
				{	
					cons_b= "(and "+cons_b;
					cons_b_end +=")";
				}
				/*
				String cons_c = "";	
				String cons_c_end = "true";
				
				//this can be commented out??
				for(int k=0;k<flexLockPairs.size();k++)
				{
					if(k!=j)
					{
						LockPair lp3 = flexLockPairs.get(k);
						Long lp3_tid = null;
						if(lp3.lock==null)
						{
							lp3_tid = lp3.unlock.getTid();
						}
						else
						{
							lp3_tid = lp3.lock.getTid();
						}
						
						if(lp3_tid!=lp1_tid)
						{
						
						String var_lp3_a="";
						String var_lp3_b="";
						
						if(lp3.lock!=null)
						{
							if(canReach((AbstractNode)lp3.lock,(AbstractNode)lp2.unlock))
								continue;
							
							var_lp3_a = makeVariable(lp3.lock.getGID());
							
						}
						if(lp3.unlock!=null)
						{
							
							if(canReach((AbstractNode)lp3.unlock,(AbstractNode)lp2.unlock))
								continue;
							
							var_lp3_b = makeVariable(lp3.unlock.getGID());
							
						}
						
						
						String cons_d = "(and ";
						//it's possible multiple lockpairs with unlock nodes null
						if(lp3.unlock==null)//then the lock node must be the last one
						{
								if(lp1.unlock!=null)
									cons_d +="(> "+var_lp3_a+" "+var_lp1_b+")\n";
								else
									cons_d +="(> "+var_lp3_a+" "+var_lp1_a+")\n";

						}
						else if(lp3.lock==null||lp1.unlock==null)//then the unlock node must be the first one
						{
							if(lp2.lock!=null)
								cons_d +="(> " +var_lp2_a+" "+var_lp3_b+")\n";
							else 
								cons_d +="true\n";//looks impossible to be here
						}
						else
						{
							if(lp2.lock!=null)
								cons_d += "(or (> "+var_lp3_a+" "+var_lp1_b+")" +
										" (> " +var_lp2_a+" "+var_lp3_b+"))\n";
							else
								cons_d +="(> "+var_lp3_a+" "+var_lp1_b+")\n";
								
						}
						
						cons_c= cons_d + cons_c;
						cons_c_end +=")";
						}
					}
					
				}
				
				cons_c+=cons_c_end;
				
				cons_b_ = cons_b_ + cons_c + ")\n";

				cons_b += "(or "+cons_b_;*/
				
			}
			
			cons_b +=cons_b_end;
				
			/*
			String cons_a = "";//first lock
			String cons_a_end = "true";
			
				//consider the first lock node and the pre-node cases 
				if(lp1.unlock!=null)
				if(lp1_pre==null||lp1_pre.lock==null)
				{//lp1 is the first node -> all other nodes are after lp1
					
					for(int k=0;k<flexLockPairs.size();k++)
					{
						LockPair lp = flexLockPairs.get(k);
						
						if(lp.lock!=null)
						{
							String var_lp_a = makeVariable(lp.lock.getGID());
							//it's possible that both lp and lp1 unlock are null, due to the trace incompleteness
							if(lp1.unlock!=null)
								cons_a= "(and (> "+var_lp_a+" "+var_lp1_b+")\n" + cons_a;
							//else
								//cons_a= "(and (> "+var_lp_a+" "+var_lp1_a+")\n" + cons_a;

							cons_a_end +=")";
						}
					}
				}
				else if(lp1.unlock==null)
				{
					//all other nodes are before pre_node
					String var_lp1_pre_a = makeVariable(lp1_pre.lock.getGID());
					
					for(int k=0;k<flexLockPairs.size();k++)
					{
						LockPair lp = flexLockPairs.get(k);
						
						if(lp.unlock!=null)
						{
								String var_lp_b = makeVariable(lp.unlock.getGID());
								cons_a= "(and (> "+var_lp1_pre_a+" "+var_lp_b+")\n" + cons_a;
								cons_a_end +=")";
						}

					}
				}
				else
				{
					String var_lp1_pre_a = makeVariable(lp1_pre.lock.getGID());
					
					for(int k=0;k<flexLockPairs.size();k++)
					{
						LockPair lp = flexLockPairs.get(k);
						
						if(lp.lock==null)
						{
							String var_lp_b = makeVariable(lp.unlock.getGID());
							cons_a= "(and (> "+var_lp1_pre_a+" "+var_lp_b+")\n" + cons_a;
							cons_a_end +=")";

						}
						else if(lp.unlock==null)
						{
							String var_lp_a = makeVariable(lp.lock.getGID());
							cons_a= "(and (> "+var_lp_a+" "+var_lp1_b+")\n" + cons_a;
							cons_a_end +=")";
						}
						else
						{
							String var_lp_a = makeVariable(lp.lock.getGID());
							String var_lp_b = makeVariable(lp.unlock.getGID());

							cons_a= "(and (or (> "+var_lp_a+" "+var_lp1_b+") (> "+var_lp1_pre_a+" "+var_lp_b+"))\n" + cons_a;
							cons_a_end +=")";
						}

					}
				}
				
				if(cons_a.length()>0)//should hold here, otherwise trace is incomplete
				{
					cons_a+=cons_a_end+"\n";
					CONS_LOCK+="(assert \n(or \n"+cons_a+" "+cons_b+"))\n\n";
				}*/
			
			CONS_LOCK+=cons_b+"\n";

			}
				lastLockPairMap.put(lp1.lock.getTid(), lp1);

		}
		
		return CONS_LOCK;
		
	}
	
	public void addReadWriteConstraints(HashMap<String, Vector<ReadNode>> indexedReadNodes,
			HashMap<String, Vector<WriteNode>> indexedWriteNodes)
	{
		CONS_ASSERT.append(constructReadWriteConstraints(indexedReadNodes,indexedWriteNodes));
	}
	
	//TODO: NEED to handle the feasibility of new added write nodes
	public StringBuilder constructCausalReadWriteConstraintsOptimized(
			Vector<ReadNode> readNodes,
			HashMap<String, Vector<WriteNode>> indexedWriteNodes, HashMap<String,String> initValueMap )
	{
		StringBuilder CONS_CAUSAL_RW = new StringBuilder("");
		
		for(int i=0;i<readNodes.size();i++)
		{
				
			ReadNode rnode = readNodes.get(i);
			
			//get all write nodes on the address
			Vector<WriteNode> writenodes = indexedWriteNodes.get(rnode.getAddr());
			//no write to array field?
			//Yes, it could be: java.io.PrintStream out
			if(writenodes==null||writenodes.size()<1)//
				continue;			

			WriteNode preNode = null;//
			
			//get all write nodes on the address & write the same value
			Vector<WriteNode> writenodes_value_match = new Vector<WriteNode>();
			for(int j=0;j<writenodes.size();j++)
			{
				WriteNode wnode = writenodes.get(j);
				if(wnode.getValue()==rnode.getValue()&&!canReach(rnode,wnode))
				{
					if(wnode.getTid()!=rnode.getTid())
						writenodes_value_match.add(wnode);
					else
					{
						if(preNode ==null
								||(preNode.getGID()<wnode.getGID()&&wnode.getGID()<rnode.getGID()))
							preNode = wnode;
							
					}
				}
			}
			if(writenodes_value_match.size()>0)
			{
			if(preNode!=null)
				writenodes_value_match.add(preNode);
			
			//TODO: consider the case when preNode is not null

						
				String var_r = makeVariable(rnode.getGID());						
	
				
				String cons_a="";
				String cons_a_end="";

				String cons_b = "";
				String cons_b_end = "";
				

				
				//make sure all the nodes that x depends on read the same value

				for(int j=0;j<writenodes_value_match.size();j++)
				{
					WriteNode wnode1 = writenodes_value_match.get(j);
					String var_w1 = makeVariable(wnode1.getGID());

					String cons_b_ = "(> "+var_r+" "+var_w1+")\n";						

					String cons_c = "";	
					String cons_c_end = "";
					String last_cons_d = null;
					for(int k=0;k<writenodes.size();k++)
					{
						WriteNode wnode2 = writenodes.get(k);
						if(wnode2.getGID()!=wnode1.getGID())
						{
							String var_w2 = makeVariable(wnode2.getGID());
							
							if(last_cons_d!=null)
							{
								cons_c+="(and " +last_cons_d;
								cons_c_end +=")";
							
							}
							last_cons_d = 
									"(or (> "+var_w2+" "+var_r+")" +
											" (> " +var_w1+" "+var_w2+"))\n";
							

						}
					}
					if(last_cons_d!=null)
					{
						cons_c+=last_cons_d;
					}
					cons_c=cons_c+cons_c_end;
					
					if(cons_c.length()>0)
						cons_b_ ="(and "+cons_b_+" "+cons_c + ")\n";
					
						
					if(j+1<writenodes_value_match.size())
					{
						cons_b+= "(or "+cons_b_;
						cons_b_end +=")";
						
						cons_a+= "(and (> "+var_w1+" "+var_r+")\n";
						cons_a_end +=")";
					}
					else
					{
						cons_b+= cons_b_;
						cons_a+= "(> "+var_w1+" "+var_r+")\n";
					}
				}
				
				cons_b +=cons_b_end;
				

					String rValue = rnode.getValue();
					String initValue = initValueMap.get(rnode.getAddr());
						
					//it's possible that we don't have initial value for static variable
					//so we allow init value to be zero or null? -- null is turned into 0 by System.identityHashCode
					boolean allowMatchInit = true;
					if(initValue==null)
					{
						for(int j=0;j<writenodes_value_match.size();j++)
						{
							if(writenodes_value_match.get(j).getGID()<rnode.getGID())
							{	
								allowMatchInit = false;
								break;
							}
						}
					}
					
				if(initValue==null&&allowMatchInit||initValue!=null&&rValue.equals(initValue))
				{
					if(cons_a.length()>0)
					{
						cons_a+=cons_a_end+"\n";
						CONS_CAUSAL_RW.append("(or \n"+cons_a+" "+cons_b+")\n\n");
					}
				}
				else
				{
					CONS_CAUSAL_RW.append(cons_b+"\n");
				}
			}
				
		}
		
		return CONS_CAUSAL_RW;
	}
	
	//does not consider value and causal dependence
	private static String constructReadWriteConstraints(
			HashMap<String, Vector<ReadNode>> indexedReadNodes,
			HashMap<String, Vector<WriteNode>> indexedWriteNodes) {
		
		String CONS_RW = "";
		
		Iterator<Entry<String, Vector<ReadNode>>> 
						entryIt =indexedReadNodes.entrySet().iterator();
		while(entryIt.hasNext())
		{
			Entry<String, Vector<ReadNode>> entry = entryIt.next();
			String addr = entry.getKey();
			
			//get all read nodes on the address
			Vector<ReadNode> readnodes = entry.getValue();
					
			//get all write nodes on the address
			Vector<WriteNode> writenodes = indexedWriteNodes.get(addr);
			
				
			//no write to array field?
			//Yes, it could be: java.io.PrintStream out
			if(writenodes==null||writenodes.size()<2)
				continue;
						
			
			for(int i=0;i<readnodes.size();i++)
			{
				ReadNode rnode = readnodes.get(i);
				String var_r = makeVariable(rnode.getGID());
										
				String cons_a = "";
				
				String cons_a_end = "true";
				
				String cons_b = "";
				String cons_b_end = "false";
				
				//we start from j=1 to exclude the initial write
				for(int j=1;j<writenodes.size();j++)
				{
					WriteNode wnode1 = writenodes.get(j);	
					{
						String var_w1 = makeVariable(wnode1.getGID());

						cons_a= "(and (> "+var_w1+" "+var_r+")\n" + cons_a;
						cons_a_end +=")";
						
						String cons_b_ = "(and (> "+var_r+" "+var_w1+")\n";						
	
						String cons_c = "";	
						String cons_c_end = "true";
						
						for(int k=1;k<writenodes.size();k++)
						{
							if(j!=k)
							{
								WriteNode wnode2 = writenodes.get(k);
								String var_w2 = makeVariable(wnode2.getGID());
	
								String cons_d = "(and " +
										"(or (> "+var_w2+" "+var_r+")" +
												" (> " +var_w1+" "+var_w2+"))\n";
								
								cons_c= cons_d + cons_c;
								cons_c_end +=")";
							}
						}
						
						cons_c+=cons_c_end;
						
						cons_b_ = cons_b_ + cons_c + ")\n";
	
						cons_b += "(or "+cons_b_;
						
						cons_b_end +=")";
					}
				}
				
				cons_b +=cons_b_end;
				
				cons_a+=cons_a_end+"\n";
				

				CONS_RW+="(assert \n(or \n"+cons_a+" "+cons_b+"))\n\n";
			}

		}
		
		return CONS_RW;
	}
	public boolean isAtomic(IMemNode node1, IMemNode node2, IMemNode node3)
	{
		long gid1 = node1.getGID();
		long gid2 = node2.getGID();
		long gid3 = node3.getGID();
		
		return lockEngine.isAtomic(node1.getTid(),gid1, gid2, node3.getTid(), gid3);	
		
	}
	public boolean hasCommonLock(IMemNode node1, IMemNode node2)
	{
		long gid1 = node1.getGID();
		long gid2 = node2.getGID();
		
		return lockEngine.hasCommonLock(node1.getTid(),gid1, node2.getTid(), gid2);	
		
	}
	public boolean canReach(AbstractNode node1, AbstractNode node2)
	{
		long gid1 = node1.getGID();
		long gid2 = node2.getGID();
		
		return reachEngine.canReach(gid1, gid2);	
		
	}
	public boolean isRace(AbstractNode node1, AbstractNode node2, StringBuilder casualConstraint)
	{
		long gid1 = node1.getGID();
		long gid2 = node2.getGID();
//		
//		if(gid1<gid2)
//		{	if(reachEngine.canReach(gid1, gid2))
//				return false;
//		}
//		else
//		{
//			if(reachEngine.canReach(gid2, gid1))
//				return false;
//		}
		
		String var1 = makeVariable(gid1);
		String var2 = makeVariable(gid2);
		
		//String QUERY = "\n(assert (= "+var1+" "+var2+"))\n\n";
		
		id++;
		task = new YicesRunSMTLIB1(config,id);
		
		String cons_assert = CONS_ASSERT.toString()+casualConstraint.toString()+")\n";
		cons_assert = cons_assert.replace(var2+" ", var1+" ");
		cons_assert = cons_assert.replace(var2+")", var1+")");
		StringBuilder msg = new StringBuilder(CONS_BENCHNAME).append(CONS_SETLOGIC).append(CONS_DECLARE).append(cons_assert).append(BRACKET_RIGHT);	
		task.sendMessage(msg.toString());
		
		return task.sat;
	}

	
	public boolean isAtomicityViolation(IMemNode node1, IMemNode node2, IMemNode node3, 
			StringBuilder casualConstraint1, StringBuilder casualConstraint2, StringBuilder casualConstraint3) {
		
		long gid1 = node1.getGID();
		long gid2 = node2.getGID();
		long gid3 = node3.getGID();
		
//		if(gid2<gid1)
//		{
//			if(reachEngine.canReach(gid2, gid1))
//				return false;
//		}
//		else if(gid2>gid3)
//		{
//			if(reachEngine.canReach(gid3, gid2))
//				return false;
//		}
		
		String var1 = makeVariable(gid1);
		String var2 = makeVariable(gid2);
		String var3 = makeVariable(gid3);

		//not global order
		String QUERY = "(and (<= "+var1+" "+var3+")\n" +
									"(<= "+var3+" "+var2+")" +
											")\n\n";
		
		id++;
		task = new Z3Run(config,id);
		StringBuilder msg = new StringBuilder(CONS_DECLARE).append(CONS_ASSERT).append(casualConstraint1).append(casualConstraint2).append(casualConstraint3).append(QUERY).append(CONS_GETMODEL);
		task.sendMessage(msg.toString());
		
		return task.sat;
	}
	public boolean isAtomicityViolation(IMemNode node1, IMemNode node2,
			IMemNode node3) {
		
		String var1 = makeVariable(node1.getGID());
		String var2 = makeVariable(node2.getGID());
		String var3 = makeVariable(node3.getGID());

		//not global order
		String QUERY = "(and (<= "+var1+" "+var3+")\n" +
									"(<= "+var3+" "+var2+")" +
											")\n\n";
		
		id++;
		task = new Z3Run(config,id);
		StringBuilder msg = new StringBuilder(CONS_DECLARE).append(CONS_ASSERT).append(QUERY).append(CONS_GETMODEL);
		task.sendMessage(msg.toString());
		
		return task.sat;
	}
	
	public boolean isDeadlock(LockPair lp1, LockPair lp2, LockPair lp3,
			LockPair lp4) {
		
		String var1a ="";
		String var1b ="";
		String var2a ="";
		String var2b ="";
		String var3a ="";
		String var3b ="";
		String var4a ="";
		String var4b ="";
		
		if(lp1.lock!=null) var1a = makeVariable(lp1.lock.getGID());
		if(lp1.unlock!=null) var1b = makeVariable(lp1.unlock.getGID());
		
		if(lp2.lock!=null)  var2a = makeVariable(lp2.lock.getGID());
		else return false;//lp2.lock must be here
		
		if(lp2.unlock!=null)  var2b = makeVariable(lp2.unlock.getGID());
		
		if(lp3.lock!=null) var3a = makeVariable(lp3.lock.getGID());
		else return false;//lp3.lock must be here
		if(lp3.unlock!=null)  var3b = makeVariable(lp3.unlock.getGID());
		else return false;//lp3.unlock must be here
		
		if(lp4.lock!=null) var4a = makeVariable(lp4.lock.getGID());
		else return false;//lp4.lock must be here
		if(lp4.unlock!=null)  var4b = makeVariable(lp4.unlock.getGID());
		else return false;//lp4.unlock must be here
		
		String QUERY = "";
		 
		if(lp1.lock!=null)
		QUERY +="(< " +var1a+" "+var2a+")\n";
		
		QUERY +="(< " +var2a+" "+var3a+")\n";
				
		if(lp1.unlock!=null)
		QUERY +="(< " +var3b+" "+var1b+")\n";
		
		if(lp2.unlock!=null)
		QUERY +="(< " +var4b+" "+var2b+")\n";

		
		id++;
		task = new Z3Run(config,id);
		StringBuilder msg = new StringBuilder(CONS_DECLARE).append(CONS_ASSERT).append(QUERY).append(CONS_GETMODEL);
		task.sendMessage(msg.toString());
		
		return task.sat;
		
	}
	
	
	public static void testConstructReadWriteConstraints()
	{
		HashMap<String, Vector<ReadNode>> indexedReadNodes = new HashMap<String, Vector<ReadNode>>();
		
		HashMap<String, Vector<WriteNode>> indexedWriteNodes = new HashMap<String, Vector<WriteNode>>();
		
		Vector<WriteNode> writeNodes = new Vector<WriteNode>();
		writeNodes.add(new WriteNode(1,1,1,"s","0",AbstractNode.TYPE.WRITE));
		writeNodes.add(new WriteNode(2,2,3,"s","0",AbstractNode.TYPE.WRITE));
		writeNodes.add(new WriteNode(3,3,5,"s","1",AbstractNode.TYPE.WRITE));
		writeNodes.add(new WriteNode(4,4,7,"s","1",AbstractNode.TYPE.WRITE));

		Vector<ReadNode> readNodes = new Vector<ReadNode>();
		readNodes.add(new ReadNode(5,1,2,"s","0",AbstractNode.TYPE.READ));
		readNodes.add(new ReadNode(6,2,4,"s","0",AbstractNode.TYPE.READ));
		readNodes.add(new ReadNode(7,3,6,"s","1",AbstractNode.TYPE.READ));
		readNodes.add(new ReadNode(8,4,8,"s","1",AbstractNode.TYPE.READ));

		indexedWriteNodes.put("s", writeNodes);
		indexedReadNodes.put("s", readNodes);

		System.out.println(constructReadWriteConstraints(indexedReadNodes,indexedWriteNodes));
	}
	
	public static void main(String[] args) throws IOException
	{
		//testConstructLockConstraints();
		testConstructReadWriteConstraints();
	}

	public Vector<String> getSchedule(long endGID, HashMap<Long,Long> nodeGIDTidMap
			,HashMap<Long,String> threadIdNameMap) {
		
		Vector<String> schedule = new Vector<String>();
		for (int i=0;i<task.schedule.size();i++)
		{
			String xi = task.schedule.get(i);
			long gid = Long.valueOf(xi.substring(1));
			long tid = nodeGIDTidMap.get(gid);
			String name = threadIdNameMap.get(tid);
			schedule.add(name);
			if(gid==endGID)
				break;
		}
		
		return schedule;
	}
	


}
