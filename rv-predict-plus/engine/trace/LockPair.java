package trace;

public class LockPair {
	public ISyncNode lock;
	public ISyncNode unlock;
	//make be wait node
	public LockPair(ISyncNode lock, ISyncNode unlock)
	{
		this.lock = lock;
		this.unlock = unlock;	
	}
}
