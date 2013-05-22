package reentrantlock;

public class Main {
	
	static int TIMES = 10;
	static int x;
	static MyThread t1,t2;
	public static void main(String[] args) throws InterruptedException
	{
		Main t = new Main();
		
		t1 = new MyThread();
		t2 = new MyThread();
		
		t1.start();
		t2.start();
	
		t1.join();
		t2.join();

		System.out.println(x);
	}
	
	static class MyThread extends Thread
	{
		public void run()
		{
			for(int i=0;i<TIMES;i++)
				m();
		}
		private synchronized void m()
		{
			synchronized(this)
			{
			x++;	
			}
		}

	}
}
