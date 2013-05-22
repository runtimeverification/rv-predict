package causal;

public class Tricky3 {
	
	static Object lock = new Object();
	static int x=0;
	static int y=0;
	static int z=0;

	public static void main(String[] args)
	{
		MyThread t = new MyThread();
		t.start();
		
		y=1;
		synchronized(lock)
		{
			x=1;
		}
		
	}
	static class MyThread extends Thread
	{
		public void run()
		{
			synchronized(lock)
			{	
				if(x>=0)
					y=2;//there is a real race here, but we may miss it
				//not necessarily, we may still detect this race if x read 0
			}
			
		}
	}
}
