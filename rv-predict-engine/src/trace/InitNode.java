package trace;

public class InitNode extends AbstractNode
{
	private String value;
	private String addr;
	
	public InitNode(long GID, long tid, int ID, String addr, String value, TYPE type)
	{
		super(GID, tid, ID, type);
		this.addr = addr;
		this.value = value;
	}

	public String getValue()
	{
		return value;
	}
	
	public String getAddr()
	
	{
		return addr;
	}

	public String toString()
	{
		
			return GID+": thread "+tid+ " "+ID+" "+addr+" "+value+" "+type;
	}
	
}
