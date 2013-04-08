package emp;

public class Simple {
	static Object lock = new Object();
	static int x=0;
	public static void main(String[] args)
	{
		try{
		MyThread t1 = new MyThread();
		MyThread t2 = new MyThread();

		t1.start();
		t2.start();
		
		synchronized(lock)
		{
			x++;
		}
		x=0;
		
		t1.join();
		
		t2.join();
				
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
	static class MyThread extends Thread
	{
		public void run()
		{
			synchronized(lock)
			{x++;
			}
			System.out.println(1/x);

		}
	}
}
