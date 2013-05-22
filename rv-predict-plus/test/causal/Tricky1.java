package causal;

public class Tricky1 {
	
	static Object lock = new Object();
	static int x=0;
	static int y=0;
	static int z=0;

	public static void main(String[] args)
	{
		MyThread t = new MyThread();
		t.start();
		
		synchronized(lock)
		{
			y=1;
			x=1;
			if(x==2)
				z=1;
		}
	}
	static class MyThread extends Thread
	{
		public void run()
		{
			synchronized(lock)
			{			
				x=2;
			}
			y=2;//we can detect this race
		}
	}
}
