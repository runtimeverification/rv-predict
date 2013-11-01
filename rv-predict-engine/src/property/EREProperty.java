package property;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.Vector;

import trace.PropertyNode;
import trace.Trace;


public class EREProperty {

	ArrayList<Integer> ereList= new ArrayList<Integer>();
	HashMap<Integer,Integer> idx_beginends = new HashMap<Integer,Integer>();
	HashSet<Integer> id_begins = new HashSet<Integer>();
	HashSet<Integer> id_ends = new HashSet<Integer>();

	HashMap<PropertyNode,PropertyNode> pairs = new HashMap<PropertyNode,PropertyNode>();
	boolean isParallel;
	int size =-1;//number of distinct events
	HashMap<Integer,String> threadBindingMap = new HashMap<Integer,String>();

	public boolean isParallel()
	{
		return isParallel;
	}
	public PropertyNode getPairedNode(PropertyNode node)
	{
		return pairs.get(node);
	}
	public boolean hasThreadBinding(Integer id)
	{
		return threadBindingMap.containsKey(id);
	}

	public int getSize()
	{
		if(size<0)
		{
			size = (new HashSet<Integer>(ereList)).size();
		}
		
		return size;
	}
	public int getPropertySize()
	{
		return ereList.size();
	}
	public ArrayList<Integer> getPropertyList()
	{
		return ereList;
	}
	public boolean hasPair()
	{
		return !idx_beginends.isEmpty();
	}
	//idx
	public boolean isPaired(Integer key)
	{
		return idx_beginends.containsKey(key);
	}
	public Integer getPairedID(Integer key)
	{
		return idx_beginends.get(key);
	}
	public EREProperty(String name, HashMap<String, Integer> map, Trace trace) {
	
		
		HashMap<String,Integer> bemap = new HashMap<String,Integer>();
		
		String[] strs = name.split(" ");
		for(int k=0;k<strs.length;k++)
		{
			String s = strs[k];
			if(s.charAt(s.length()-1)=='+')
			{
				s = s.substring(0,s.length()-1);
			}
			else if(s.charAt(s.length()-1)=='?'||s.charAt(s.length()-1)=='*')
				continue;//"?" or "*"
			else if(s.equals("||"))
			{
				isParallel = true;
				continue;
			}
			
			if(s.contains("(")&&s.charAt(s.length()-1)==')')
			{
				int pos = s.indexOf("(");
				String para = s.substring(pos+1, s.length()-1);
				s = s.substring(0, pos);

				String[] paras = para.split(",");
				if(paras.length==1)
				{
					if(paras[0].startsWith("<"))
					{
						bemap.put(paras[0].substring(1), ereList.size());
						id_begins.add(map.get(s));
					}
					else if(paras[0].startsWith(">"))
					{
						Integer id1 = bemap.get(paras[0].substring(1));
						idx_beginends.put(ereList.size(), id1);
						id_ends.add(map.get(s));
					}
					else
					{
						//bind to meta thread
						threadBindingMap.put(ereList.size(), paras[0]);
					}
				}
				else
				{
					//bind to meta thread
					threadBindingMap.put(ereList.size(), paras[0]);
					
					if(paras[1].startsWith("<"))
					{
						bemap.put(paras[1].substring(1), ereList.size());
						id_begins.add(map.get(s));
					}
					else if(paras[1].startsWith(">"))
					{
						Integer id1 = bemap.get(paras[1].substring(1));
						idx_beginends.put(ereList.size(), id1);
						id_ends.add(map.get(s));

					}
				}
				
			}
			
			Integer id = map.get(s);
			ereList.add(id);
		}
		
		
		if(!id_begins.isEmpty())
		{
			//get a map for paired nodes
			HashMap<Long,Vector<PropertyNode>> threadPropertyNodes = trace.getThreadPropertyNodes();
			Iterator<Long> tidIt = threadPropertyNodes.keySet().iterator();
			while(tidIt.hasNext())
			{
				Long tid = tidIt.next();
				Vector<PropertyNode> nodes = threadPropertyNodes.get(tid);
				Stack<PropertyNode> stack = new Stack<PropertyNode>();
				for(int k=0;k<nodes.size();k++)
				{
					PropertyNode node = nodes.get(k);
					int ID = node.getID();
					if(id_begins.contains(ID))
					{
						stack.push(node);
					}
					else if(id_ends.contains(ID))
					{
						if(!stack.isEmpty())
							pairs.put(stack.pop(),node);
					}
				}
			}
		}
	}
	public boolean hasCorrectThreadBinding(int k, long tid,
			ArrayList<PropertyNode> list) {
		
		String mid = threadBindingMap.get(k);
		Iterator<Entry<Integer,String>> entryIt = threadBindingMap.entrySet().iterator();
		while(entryIt.hasNext())
		{
			Entry<Integer,String> entry = entryIt.next();
			if(entry.getValue().equals(mid))
			{
				if(entry.getKey()<k)
					return true;
				else
				{
					for(int i=0;i<list.size();i++)
						if(list.get(i).getTid()==tid)
							return false;
					
					return true;
				}
			}

				
		}
		
		return true;
	}
	
}
