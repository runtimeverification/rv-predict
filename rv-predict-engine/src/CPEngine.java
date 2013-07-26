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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.Map.Entry;

public class CPEngine
{
	
	private ReachabilityEngine reachEngine = new ReachabilityEngine();//TODO: do segmentation on this
	private LockSetEngine lockEngine;

	CPEngine(Trace trace)
	{
		addIntraThreadEdge(trace.getThreadNodesMap());
		
		addCPEdges(trace,trace.getThreadFirstNodeMap(),trace.getThreadLastNodeMap());
	}

	private void addIntraThreadEdge(HashMap<Long,Vector<AbstractNode>> map)
	{
		Iterator<Vector<AbstractNode>> mapIt = map.values().iterator();
		while(mapIt.hasNext())
		{
			Vector<AbstractNode> nodes = mapIt.next();
			long lastGID = nodes.get(0).getGID();
			for(int i=1;i<nodes.size();i++)
			{			
				long thisGID  = nodes.get(i).getGID();
				
				reachEngine.addEdge(lastGID, thisGID);

				lastGID = thisGID;
				
			}
		}
	}
	private void addCPEdges(Trace trace,
			HashMap<Long, AbstractNode> firstNodes,HashMap<Long, AbstractNode> lastNodes)
	{
			HashMap<String,WriteNode> addressLastWriteMap = new HashMap<String,WriteNode>();
			HashMap<String,ReadNode> addressLastReadMap = new HashMap<String,ReadNode>();

			HashMap<Long,HashSet<String>> threadCurrentLockRegionAddresses = new HashMap<Long,HashSet<String>>();
			HashMap<Long,Stack<HashSet<String>>> threadAccessAddrStack = new HashMap<Long,Stack<HashSet<String>>>();

			HashMap<String,ArrayList<HashSet<String>>> lockAccessedAddresses = new HashMap<String,ArrayList<HashSet<String>>>();
			HashMap<String,ArrayList<LockPair>> lockAddrNodes = new HashMap<String,ArrayList<LockPair>>();
			HashMap<Long,Stack<ISyncNode>> threadSyncStack = new HashMap<Long,Stack<ISyncNode>>();

			NotifyNode matchNotifyNode = null;
			
			//during recording
			//should after wait, before notify
			//after lock, before unlock
			Vector<AbstractNode> nodes = trace.getFullTrace();
			for(int i=0;i<nodes.size();i++)
			{			
				AbstractNode node = nodes.get(i);
				long thisGID = node.getGID();
				
				//add first node
				
				if(node instanceof StartNode)
				{
					long tid = Long.valueOf(((StartNode)node).getAddr());
					
					AbstractNode fnode = firstNodes.get(tid);
					if(fnode!=null)
					{
						long fGID = fnode.getGID();
					reachEngine.addEdge(thisGID, fGID);
					
					}
				}
				else if (node instanceof JoinNode)
				{
					long tid = Long.valueOf(((JoinNode)node).getAddr());
					AbstractNode lnode = lastNodes.get(tid);
					if(lnode!=null)
					{
						long lGID = lnode.getGID();
						reachEngine.addEdge(lGID,thisGID);

					}
					
				}
				else if (node instanceof ReadNode)
				{
					
					String addr = ((ReadNode) node).getAddr();
					WriteNode wnode = addressLastWriteMap.get(addr);
					if(wnode!=null)
					{
						reachEngine.addEdge(wnode.getGID(), node.getGID());
					}
					addressLastReadMap.put(addr, (ReadNode)node);
					
					HashSet<String> addresses = threadCurrentLockRegionAddresses.get(node.getTid());
					if(addresses==null)
					{
						addresses = new HashSet<String>();
						threadCurrentLockRegionAddresses.put(node.getTid(), addresses);
					}
					addresses.add(addr);
				}
				else if(node instanceof WriteNode)
				{
					String addr = ((WriteNode) node).getAddr();
					WriteNode wnode = addressLastWriteMap.get(addr);
					if(wnode!=null)
					{
						reachEngine.addEdge(wnode.getGID(), node.getGID());
					}
					ReadNode rnode = addressLastReadMap.get(addr);
					if(rnode!=null)
					{
						reachEngine.addEdge(rnode.getGID(), node.getGID());
					}
					addressLastWriteMap.put(addr, (WriteNode)node);

					HashSet<String> addresses = threadCurrentLockRegionAddresses.get(node.getTid());
					if(addresses==null)
					{
						addresses = new HashSet<String>();
						threadCurrentLockRegionAddresses.put(node.getTid(), addresses);
					}
					addresses.add(addr);
				}
				else if (node instanceof LockNode)
				{
					long tid = node.getTid();
					
					Stack<HashSet<String>> stack = threadAccessAddrStack.get(tid);
					if(stack==null)
					{
						stack = new Stack<HashSet<String>>();
						threadAccessAddrStack.put(tid, stack);
					}

					HashSet<String> addresses = threadCurrentLockRegionAddresses.get(tid);
	
					if(addresses!=null)
					{
						stack.push(new HashSet(addresses));	
						addresses.clear();
					}
					
					Stack<ISyncNode> syncstack = threadSyncStack.get(tid);
					if(syncstack==null)
					{
						syncstack = new Stack<ISyncNode>();
						threadSyncStack.put(tid, syncstack);
					}
					syncstack.push(((LockNode) node));	
					
				}
				else if (node instanceof UnlockNode)
				{
					long tid = node.getTid();
					
					
					Stack<ISyncNode> syncstack = threadSyncStack.get(tid);
					
					//assert(stack.size()>0);//this is possible when segmented
					if(syncstack==null)
					{
						syncstack = new Stack<ISyncNode>();
						threadSyncStack.put(tid, syncstack);
					}
					LockPair lp = null;
					if(syncstack.isEmpty())
					{
						lp = new LockPair(null,(ISyncNode)node);

					}
					else
					{
						lp = new LockPair(syncstack.pop(),(ISyncNode)node);
					}					
					
					
					
					HashSet<String> addresses = threadCurrentLockRegionAddresses.get(tid);
					if(addresses==null)
					{
						addresses = new HashSet<String>();
						threadCurrentLockRegionAddresses.put(tid, addresses);
					}
					
					String addr = ((UnlockNode) node).getAddr();
					
					ArrayList<HashSet<String>> addrList = lockAccessedAddresses.get(addr);
					ArrayList<LockPair> syncNodeList = lockAddrNodes.get(addr);
					if(addrList==null)
					{
						addrList = new ArrayList<HashSet<String>>();
						syncNodeList = new ArrayList<LockPair>();
						lockAccessedAddresses.put(addr, addrList);
						lockAddrNodes.put(addr, syncNodeList);
					
					}
					addrList.add(new HashSet(addresses));syncNodeList.add(lp);
					
					Stack<HashSet<String>> stack = threadAccessAddrStack.get(tid);
					if(stack!=null&&!stack.isEmpty())
					{
						addresses.addAll(stack.pop());
					}
					
					
					
				}
				else if (node instanceof WaitNode)
				{
					long tid = node.getTid();

					//assert(matchNotifyNode!=null);this is also possible when segmented
					if(matchNotifyNode!=null)
					{
						long notifyGID = matchNotifyNode.getGID();
						
	
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
												
						reachEngine.addEdge(notifyGID,waitNextGID);

						}catch(Exception e)
						{
							e.printStackTrace();
						}
						
						//clear notifyNode
						matchNotifyNode=null;
					}
					
					
					
					Stack<ISyncNode> syncstack = threadSyncStack.get(tid);
					//assert(stack.size()>0);
					if(syncstack==null)
					{
						syncstack = new Stack<ISyncNode>();
						threadSyncStack.put(tid, syncstack);
					}
					LockPair lp = null;
					if(syncstack.isEmpty())
						lp = new LockPair(null,((WaitNode) node));
					else 
						lp = new LockPair(syncstack.pop(),((WaitNode) node));
					
						syncstack.push(((WaitNode) node));
					
					
					HashSet<String> addresses = threadCurrentLockRegionAddresses.get(tid);
					if(addresses==null)
					{
						addresses = new HashSet<String>();
						threadCurrentLockRegionAddresses.put(tid, addresses);
					}
					
					String addr = ((WaitNode) node).getAddr();
					
					ArrayList<HashSet<String>> addrList = lockAccessedAddresses.get(addr);
					ArrayList<LockPair> syncNodeList = lockAddrNodes.get(addr);
					if(addrList==null)
					{
						addrList = new ArrayList<HashSet<String>>();
						syncNodeList = new ArrayList<LockPair>();
						lockAccessedAddresses.put(addr, addrList);
						lockAddrNodes.put(addr, syncNodeList);
					
					}
					addrList.add(new HashSet(addresses));syncNodeList.add(lp);
					
					Stack<HashSet<String>> stack = threadAccessAddrStack.get(tid);
					if(stack!=null&&!stack.isEmpty())
					{
						addresses.addAll(stack.pop());
					}
					
							

					

				}
				else if (node instanceof NotifyNode)
				{
					matchNotifyNode = (NotifyNode)node;
				}
			}
			
			
			//last lock node
			Iterator<Long> tidIt = threadSyncStack.keySet().iterator();
			while(tidIt.hasNext())
			{
				Long tid = tidIt.next();
				
				
				HashSet<String> addresses = threadCurrentLockRegionAddresses.get(tid);
				if(addresses!=null)
				{

					Stack<ISyncNode> stack = threadSyncStack.get(tid);
					
					while(stack.size()>0)
					{
						ISyncNode node = stack.pop();
						LockPair lp = new LockPair(node,null);
						
						String addr = node.getAddr();
						
						ArrayList<HashSet<String>> addrList = lockAccessedAddresses.get(addr);
						ArrayList<LockPair> syncNodeList = lockAddrNodes.get(addr);
						if(addrList!=null)
						{
							addrList.add(new HashSet(addresses));syncNodeList.add(lp);
						}
						
					}
				}
			}
			
			
			
			Iterator<String> addrIt = lockAccessedAddresses.keySet().iterator();
			while(addrIt.hasNext())
			{
				String addr = addrIt.next();
				
				ArrayList<HashSet<String>> addrList = lockAccessedAddresses.get(addr);
				ArrayList<LockPair> syncNodeList = lockAddrNodes.get(addr);
				if(addrList!=null)
				{
					for(int k=addrList.size()-1;k>=1;k--)
					{
						LockPair lp = syncNodeList.get(k);
						if(lp.lock!=null)
						{
							HashSet<String>  addresses = addrList.get(k);
							
							for(int k1 = k-1;k1>=0;k1--)
							{
								HashSet<String> lastAddresses = new HashSet(addrList.get(k1));
								
								lastAddresses.retainAll(addresses);
								
								if(!lastAddresses.isEmpty())
								{
									ISyncNode lastnode = syncNodeList.get(k1).unlock;
									if(lastnode!=null)
									{
										reachEngine.addEdge(lastnode.getGID(), lp.lock.getGID());
									}
								}
							}
						}
					
					}
				}
			}
						
		
	}
	
	public boolean isRace(AbstractNode node1, AbstractNode node2)
	{
		long gid1 = node1.getGID();
		long gid2 = node2.getGID();
		
		if(gid1>gid2)
		{
			long t = gid1;
			gid1 = gid2;
			gid2 = t;
		}
		//exclude this edge first
		boolean e12 = reachEngine.deleteEdge(gid1, gid2); 
		boolean e21 = reachEngine.deleteEdge(gid2, gid1);

		boolean race = !reachEngine.canReach(gid1, gid2)&&!reachEngine.canReach(gid2, gid1);	
		
		//add back 	
		if(e12)
			reachEngine.addEdge(gid1, gid2);
		if(e21)
			reachEngine.addEdge(gid2, gid1);
		
		return race;
	}
}
