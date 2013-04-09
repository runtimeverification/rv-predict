package trace;

import trace.AbstractNode.TYPE;

public interface IMemNode {

	public String getAddr();
	public long getGID();
	public long getTid();
	public TYPE getType();
	public int getID();

}
