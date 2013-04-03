package trace;

public class NotifyNode extends AbstractNode  implements ISyncNode{
	
	private String sig_addr;
	
	public String getAddr()
	{
		return sig_addr;
	}
	
	public NotifyNode(long GID, long tid, int ID, String addr , TYPE type)
	{
		super(GID, tid, ID,type);
		this.sig_addr = addr;
}
	
}
