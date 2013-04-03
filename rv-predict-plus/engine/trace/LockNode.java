package trace;

public class LockNode extends AbstractNode  implements ISyncNode 
{
	private long did;//this is the ID of the event from the same thread the rwnode depends on
	private String lock_addr;
	
	public LockNode(long GID, long tid, int ID, String addr, TYPE type)
	{
		super(GID, tid, ID,type);
		this.lock_addr = addr;
	}

	public void setDid(long did)
	{
		this.did = did;
	}
	public long getDid()
	{
		return did;
	}
	public String getAddr()
	{
		return lock_addr;
	}
}
