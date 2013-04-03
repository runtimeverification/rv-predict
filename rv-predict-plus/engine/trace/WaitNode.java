package trace;

public class WaitNode extends AbstractNode  implements ISyncNode {
	
	private String sig_addr;

	public String getAddr()
	{
		return sig_addr;
	}
	
	public WaitNode(long GID, long tid, int ID, String addr , TYPE type)
	{
		super(GID, tid, ID,type);
		this.sig_addr = addr;
	}
	
}
