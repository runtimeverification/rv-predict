package trace;

public class JoinNode extends AbstractNode  implements ISyncNode  {
	
	private String tid_join;
	
	public String getAddr()
	{
		return tid_join;
	}
	
	public JoinNode(long GID, long tid, int ID, String addr, TYPE type)
	{
		super(GID, tid, ID,type);
		tid_join = addr;
	}
	
}
