package trace;

public class StartNode extends AbstractNode implements ISyncNode{
	
	private String tid_child;
	
	public String getAddr()
	{
		return tid_child;
	}
	public StartNode(long GID, long tid, int ID, String addr, TYPE type)
	{
		super(GID, tid, ID,type);
		tid_child = addr;
	}
	
}
