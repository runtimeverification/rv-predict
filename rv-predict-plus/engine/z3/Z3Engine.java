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

import java.io.BufferedReader;

import java.io.IOException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;
import java.util.Map.Entry;

public class Z3Engine
{
	private static int id =0;
	public Z3Run task;
	
	private String CONS_DECLARE = "";
	private String CONS_ASSERT = "";
	private String CONS_GETMODEL = "(check-sat)\n(get-model)\n(exit)";

	private static String makeVariable(long GID)
	{
		return "x"+GID;
	}
	public static int getId()
	{
		return id;
	}
	public void setId(int id)
	{
		this.id = id;
	}
	
	public void declareVariables(Vector<AbstractNode> trace)
	{
		CONS_ASSERT = "(assert (distinct ";
		String CONS_ASSERT_RANGE ="";
		int size = trace.size();
		for(int i=0;i<size;i++)
		{
			AbstractNode node = trace.get(i);
			long GID = node.getGID();
			String var = makeVariable(GID);
			
			CONS_DECLARE += "(declare-const "+var+" Int)\n";
			CONS_ASSERT+=var+" ";
			
			CONS_ASSERT_RANGE += "(assert (and (> "+var+" 0) (< "+var+" "+(size+1)+")))\n";

		}
		
		CONS_ASSERT+="))\n";
		
		CONS_ASSERT= CONS_ASSERT_RANGE;//don't use distinct +
				
	}
	public void addIntraThreadConstraints(HashMap<Long,Vector<AbstractNode>> map)
	{
		Iterator<Vector<AbstractNode>> mapIt = map.values().iterator();
		while(mapIt.hasNext())
		{
			Vector<AbstractNode> nodes = mapIt.next();
			String lastVar = makeVariable(nodes.get(0).getGID());
			for(int i=1;i<nodes.size();i++)
			{			
				String var = makeVariable(nodes.get(i).getGID());
				CONS_ASSERT+="(assert (< "+lastVar+" "+var+"))\n";
				lastVar = var;
			}
		}
	}
	public void addSynchronizationConstraints(HashMap<String,Vector<ISyncNode>> syncNodesMap, 
												HashMap<Long,AbstractNode> firstNodes,
														HashMap<Long,AbstractNode> lastNodes)
	{
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
				String var = makeVariable(node.getGID());
				if(node instanceof StartNode)
				{
					long tid = Long.valueOf(node.getAddr());
					AbstractNode fnode = firstNodes.get(tid);
					String fvar = makeVariable(fnode.getGID());
					
					CONS_ASSERT+="(assert (< "+var+" "+fvar+"))\n";
				}
				else if (node instanceof JoinNode)
				{
					long tid = Long.valueOf(node.getAddr());
					AbstractNode lnode = lastNodes.get(tid);
					String lvar = makeVariable(lnode.getGID());
					
					CONS_ASSERT+="(assert (< "+lvar+" "+var+"))\n";
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
					
					assert(stack.size()>0);
					
					//TODO: make sure nested locks?
					lockPairs.add(new LockPair(stack.pop(),node));
					
				}
				else if (node instanceof WaitNode)
				{
					assert(matchNotifyNode!=null);
					
					String notifyVar = makeVariable(matchNotifyNode.getGID());
					CONS_ASSERT+="(assert (< "+notifyVar+" "+var+"))\n";
					
					long tid = node.getTid();
					Stack<ISyncNode> stack = threadSyncStack.get(tid);
					assert(stack.size()>0);
					
					lockPairs.add(new LockPair(stack.pop(),node));
					stack.push(node);
					
					//clear notifyNode
					matchNotifyNode=null;
				}
				else if (node instanceof NotifyNode)
				{
					matchNotifyNode = (NotifyNode)node;
				}
			}
			
			CONS_ASSERT+=constructLockConstraints(lockPairs);
			
		}
	}
	private static String constructLockConstraints(Vector<LockPair> lockPairs)
	{
		String CONS_LOCK = "";
		//handle lock pairs
		for(int i=0;i<lockPairs.size()-1;i++)
		{
			LockPair lp1 = lockPairs.get(i);
			String var_lp1_a = makeVariable(lp1.lock.getGID());
			String var_lp1_b = makeVariable(lp1.unlock.getGID());

			String cons_a = "";//first lock
			String cons_a_end = "true";
			
			String cons_b = "";
			String cons_b_end = "false";
			
			for(int j=i+1;j<lockPairs.size();j++)
			{
				LockPair lp2 = lockPairs.get(j);
				String var_lp2_a = makeVariable(lp2.lock.getGID());
				String var_lp2_b = makeVariable(lp2.unlock.getGID());
				
				cons_a= "(and (> "+var_lp2_a+" "+var_lp1_b+")\n" + cons_a;
				cons_a_end +=")";
								
				
				String cons_b_ = "(and (> "+var_lp1_a+" "+var_lp2_b+")\n";						

				String cons_c = "";	
				String cons_c_end = "true";
				
				for(int k=0;k<lockPairs.size();k++)
				{
					if(k!=i&&k!=j)
					{
						LockPair lp3 = lockPairs.get(k);
						String var_lp3_a = makeVariable(lp3.lock.getGID());
						String var_lp3_b = makeVariable(lp3.unlock.getGID());
						
						//(and
						//(or (> c1 a2) (> d1 c2))
						String cons_d = "(and " +
								"(or (> "+var_lp3_a+" "+var_lp1_b+")" +
										" (> " +var_lp2_a+" "+var_lp3_b+"))\n";
						
						cons_c= cons_d + cons_c;
						cons_c_end +=")";
					}
					
				}
				
				cons_c+=cons_c_end;
				
				cons_b_ = cons_b_ + cons_c + ")\n";

				cons_b += "(or "+cons_b_;
				
				cons_b_end +=")";
			}
			cons_b +=cons_b_end;
			cons_a+=cons_a_end+"\n";
			
			CONS_LOCK+="(assert \n(or \n"+cons_a+" "+cons_b+"))\n\n";
		}
		
		return CONS_LOCK;
	}
	public void addReadWriteConstraints(HashMap<String, Vector<ReadNode>> indexedReadNodes,
			HashMap<String, Vector<WriteNode>> indexedWriteNodes)
	{
		CONS_ASSERT+=constructReadWriteConstraints(indexedReadNodes,indexedWriteNodes);
	}
	
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
			if(writenodes==null
					||writenodes.size()<2)
				continue;
			
			WriteNode initNode = writenodes.get(0);
			
			String initVar = makeVariable(initNode.getGID());
			
			for(int i=0;i<readnodes.size();i++)
			{
				ReadNode rnode = readnodes.get(i);
				String var_r = makeVariable(rnode.getGID());
				
				CONS_RW+="(assert (> "+var_r+" "+initVar+"))\n";//initial write
						
				String cons_a = "";
				
				//TODO: require this only for control-dependent nodes
				//String cons_a_end = "(= "+rnode.getValue()+" "+initNode.getValue()+")\n";;
				String cons_a_end = "true";
				
				String cons_b = "";
				String cons_b_end = "false";
				
				for(int j=1;j<writenodes.size();j++)
				{
					WriteNode wnode1 = writenodes.get(j);	
					//TODO: require this only for control-dependent nodes
					//if(wnode1.getValue().equals(rnode.getValue()))
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
	
	public boolean isRace(AbstractNode node1, AbstractNode node2)
	{
		String var1 = makeVariable(node1.getGID());
		String var2 = makeVariable(node2.getGID());
		
		String QUERY = //"(assert (= "+var1+" "+var2+"))\n";//not global order
				"(assert (or (= (- "+var1+" "+var2+") 1)\n" +
									"(= (- "+var1+" "+var2+") -1)" +
											"))\n";
		
		id++;
		task = new Z3Run(id);
		String msg = CONS_DECLARE+CONS_ASSERT+QUERY+CONS_GETMODEL;
		task.sendMessage(msg);
		
		return task.sat;
	}
	
	public boolean isAtomicityViolation(IMemNode node1, IMemNode node2,
			IMemNode node3) {
		
		String var1 = makeVariable(node1.getGID());
		String var2 = makeVariable(node2.getGID());
		String var3 = makeVariable(node3.getGID());

		//not global order
		String QUERY = "(assert (and (<= "+var1+" "+var3+")\n" +
									"(<= "+var3+" "+var2+")" +
											"))\n";
		
		id++;
		task = new Z3Run(id);
		String msg = CONS_DECLARE+CONS_ASSERT+QUERY+CONS_GETMODEL;
		task.sendMessage(msg);
		
		return task.sat;
	}
	
	public void detectBugs()
	{
		
	}
	
	public static void testConstructLockConstraints()
	{
		Vector<LockPair> lockPairs = new Vector<LockPair>();
		
		LockPair pair1 = new LockPair(new LockNode(1,1,1,"l",AbstractNode.TYPE.LOCK), 
										new UnlockNode(2,1,2,"l",AbstractNode.TYPE.UNLOCK));
		LockPair pair2 = new LockPair(new LockNode(3,2,3,"l",AbstractNode.TYPE.LOCK), 
				new UnlockNode(4,2,4,"l",AbstractNode.TYPE.UNLOCK));
		LockPair pair3 = new LockPair(new LockNode(5,3,5,"l",AbstractNode.TYPE.LOCK), 
				new UnlockNode(6,3,6,"l",AbstractNode.TYPE.UNLOCK));
		LockPair pair4 = new LockPair(new LockNode(7,4,7,"l",AbstractNode.TYPE.LOCK), 
				new UnlockNode(8,4,8,"l",AbstractNode.TYPE.UNLOCK));

		lockPairs.add(pair1);
		lockPairs.add(pair2);
		lockPairs.add(pair3);
		lockPairs.add(pair4);

		System.out.println(constructLockConstraints(lockPairs));
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
	public Vector<String> getSchedule(HashMap<Long,Long> nodeGIDTidMap
			,HashMap<Long,String> threadIdNameMap) {
		
		Vector<String> schedule = new Vector<String>();
		for (int i=0;i<task.schedule.size();i++)
		{
			String xi = task.schedule.get(i);
			long gid = Long.valueOf(xi.substring(1));
			long tid = nodeGIDTidMap.get(gid);
			String name = threadIdNameMap.get(tid);
			schedule.add(name);
		}
		
		return schedule;
	}


}
