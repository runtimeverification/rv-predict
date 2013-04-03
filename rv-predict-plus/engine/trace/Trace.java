package trace;

import java.util.HashMap;
import java.util.Vector;

public class Trace {

	Vector<AbstractNode> fulltrace = new Vector<AbstractNode>();
	HashMap<Long,Vector<AbstractNode>> threadNodesMap = new HashMap<Long,Vector<AbstractNode>>();
	HashMap<Long, AbstractNode> threadFirstNodeMap = new HashMap<Long, AbstractNode>();
	HashMap<Long, AbstractNode> threadLastNodeMap = new HashMap<Long, AbstractNode>();
			
	HashMap<Long,Vector<BranchNode>> threadBranchNodes = new HashMap<Long,Vector<BranchNode>>();
	HashMap<Long,Vector<BBNode>> threadBBNodes = new HashMap<Long,Vector<BBNode>>();
	
	HashMap<String,Vector<ISyncNode>> syncNodesMap = new HashMap<String,Vector<ISyncNode>>();	

	HashMap<String,Vector<ReadNode>> indexedReadNodes = new HashMap<String,Vector<ReadNode>>();
	
	HashMap<String,Vector<WriteNode>> indexedWriteNodes = new
			HashMap<String,Vector<WriteNode>>();
	
	public Vector<AbstractNode> getFullTrace()
	{
		return fulltrace;
	}
	public HashMap<Long, AbstractNode> getThreadFirstNodeMap()
	{
		return threadFirstNodeMap;
	}
	public HashMap<Long, AbstractNode> getThreadLastNodeMap()
	{
		return threadLastNodeMap;
	}
	public HashMap<Long,Vector<AbstractNode>> getThreadNodesMap()
	{
		return threadNodesMap;
	}
	public HashMap<String,Vector<ISyncNode>> getSyncNodesMap()
	{
		return syncNodesMap;
	}
	public HashMap<String,Vector<ReadNode>> getIndexedReadNodes()
	{
		return indexedReadNodes;
	}
	public HashMap<String,Vector<WriteNode>> getIndexedWriteNodes()
	{
		return indexedWriteNodes;
	}
	
	public void addNode(AbstractNode node)
	{
		Long tid = node.getTid();
		
		if(node instanceof BBNode)
		{
			Vector<BBNode> bbnodes = threadBBNodes.get(tid);
			if(bbnodes == null)
			{
				bbnodes = new Vector<BBNode>();
				threadBBNodes.put(tid, bbnodes);
			}
			bbnodes.add((BBNode)node);
		}
		else if(node instanceof BranchNode)
		{
			Vector<BranchNode> branchnodes = threadBranchNodes.get(tid);
			if(branchnodes == null)
			{
				branchnodes = new Vector<BranchNode>();
				threadBranchNodes.put(tid, branchnodes);
			}
			branchnodes.add((BranchNode)node);
		}
		else
		{
			fulltrace.add(node);
			
			Vector<AbstractNode> threadNodes = threadNodesMap.get(tid);
			if(threadNodes ==null)
			{
				threadNodes = new Vector<AbstractNode>();
				threadNodesMap.put(tid, threadNodes);
				threadFirstNodeMap.put(tid, node);
			}
			
			threadNodes.add(node);
			
			//TODO: Optimize it
			threadLastNodeMap.put(tid, node); 
			
			if(node instanceof ReadNode)
			{
				String addr = ((ReadNode) node).getAddr();
				
				Vector<ReadNode> readNodes = indexedReadNodes.get(addr);
				if(readNodes == null)
				{
					readNodes =  new Vector<ReadNode>();
					indexedReadNodes.put(addr, readNodes);
				}
				readNodes.add((ReadNode)node);
				
			}
			else if(node instanceof WriteNode)
			{
				String addr = ((WriteNode) node).getAddr();
				Vector<WriteNode> writeNodes = indexedWriteNodes.get(addr);
				if(writeNodes ==null)
				{
					writeNodes = new Vector<WriteNode>();
					indexedWriteNodes.put(addr, writeNodes);
				}
				writeNodes.add((WriteNode)node);
			}
			else
			{
				String addr = ((ISyncNode)node).getAddr();
				Vector<ISyncNode> syncNodes = syncNodesMap.get(addr);
				if(syncNodes==null)
				{
					syncNodes = new Vector<ISyncNode>();
					syncNodesMap.put(addr, syncNodes);
				}
				
				syncNodes.add((ISyncNode)node);
			}
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		
	}

}
