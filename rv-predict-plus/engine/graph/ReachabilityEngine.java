package graph;

import java.util.*;
import java.util.Map.Entry;

/*
 * property: never call addEdge after canReach
 * TODO: must be optimized to handle big graph
 */
public class ReachabilityEngine {
	
	boolean isDone = false;
	private int counter = 0;	
	private boolean[][] reachmx;
	
	HashMap<Integer,Integer> idMap = new HashMap<Integer,Integer>();
	
	private int M = 100000;//five Os
	HashSet<Integer> cachedNoReachSet = new HashSet<Integer>();

	HashMap<Integer,HashSet<Integer>> edgeSetMap = new HashMap<Integer,HashSet<Integer>>();
	
	public void addEdge(int i1, int i2)
	{
		i1 = getId(i1);
		i2 = getId(i2);
		
		HashSet<Integer> s = edgeSetMap.get(i1);
		if(s==null)
		{
			s = new HashSet<Integer>();
			edgeSetMap.put(i1, s);
		}
		
		s.add(i2);
	}
	
	private int getId(int id)
	{
		Integer ID = idMap.get(id);
		if(ID==null)
		{
			ID = counter++;
			idMap.put(id, ID);//oh, forgot to do this
		}		
		return ID;
	}
	
	/*
	 * Time to compute reachability matrix
	 */
	public void allEdgeAdded()
	{
		if(!isDone)
		{
			//compute();
			isDone = true;
		}
	}
	
	private void compute()
	{
		//initialize matrix
		reachmx = new boolean[counter][counter];
		
		Iterator<Entry<Integer,HashSet<Integer>>> entryIter = edgeSetMap.entrySet().iterator();//bloat edgeSetMap -- no longer used
		while(entryIter.hasNext())
		{
			Entry<Integer,HashSet<Integer>> entry = entryIter.next();
			int i = entry.getKey();
			Iterator<Integer> sIter = entry.getValue().iterator();
			while(sIter.hasNext())
			{
				int j = sIter.next();
				reachmx[i][j] = true;
			}
		}
		
		//gc it!
		//edgeSetMap = null;
		
		//compute reach-ability
		boolean[][] reachmx0 = reachmx.clone();
		boolean[][] reachmx_tmp = reachmx.clone();
		for(int k=0;k<counter-2;k++)
		{
			reachmx_tmp = multiplyMatrix(reachmx_tmp,reachmx0);//lots of bloat here, may call gc
			
			addMatrix(reachmx_tmp);
			
		}
	}
	private boolean[][] multiplyMatrix(boolean[][] m1,boolean[][] m2)
	{
		boolean[][] reachmx_tmp = new boolean[counter][counter];
		for(int i=0;i<counter;i++)
			for(int j=0;j<counter;j++)
			{				
				for(int k=0;k<counter;k++)
				{
					if(m1[i][k]&m2[k][j])
					{
						reachmx_tmp[i][j] = true;
						break;
					}
				}
			}
		return reachmx_tmp;
	}
	
	private void addMatrix(boolean[][] m)
	{
		for(int i=0;i<counter;i++)
			for(int j=0;j<counter;j++)
			{
				reachmx[i][j] =  reachmx[i][j] | m[i][j];
			}
	}
	private boolean hasEdge(int i1,int i2)
	{
		HashSet<Integer> s = edgeSetMap.get(i1);
		if(s==null)
		{
			s = new HashSet<Integer>();
			edgeSetMap.put(i1, s);
		}
		return s.contains(i2);
	}
	public boolean canReach(int i1, int i2)
	{
		//must have corresponding real id
		i1 = idMap.get(i1);
		i2 = idMap.get(i2);
		//return reachmx[i1][i2];
		int SIG = i1*M+i2;
		if(cachedNoReachSet.contains(SIG))
			return false;
		else if(hasEdge(i1,i2))
			return true;
		else
		{
		//DFS - without cache
		java.util.ArrayDeque<Integer> stack = new java.util.ArrayDeque<Integer>();
		HashSet<Integer> visitedNodes = new HashSet<Integer>();
		stack.push(i1);
		
		while(!stack.isEmpty())
		{
			int i1_ = stack.pop();
			
			visitedNodes.add(i1_);
			
			if(!hasEdge(i1,i1_))
				addEdge(i1,i1_);
			
			if(i1_ == i2) 
			{
				return true;
			}
			else
			{
				if(hasEdge(i1_,i2))
				{
					addEdge(i1,i2);
					return true;
				}
				else
				{
					Iterator<Integer> sIter = edgeSetMap.get(i1_).iterator();
					while(sIter.hasNext())
					{
						int i1__ = sIter.next();									
						//System.out.print("DEBUG: "+i1+" "+i1_+" "+ i1__+"\n");
						int sig = i1__*M+i2;
						if(!visitedNodes.contains(i1__)&&!cachedNoReachSet.contains(sig)) 
							stack.push(i1__);
					}
				}
			}
		}	
		
		cachedNoReachSet.add(SIG);
		return false;
		}
	}
}
