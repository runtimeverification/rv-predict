package array;

public class Test {
	static Object lock = new Object();
	static int[] a= new int[2];
	static int x=0;

	public static void main(String[] args)
	{
		
		MyThread t = new MyThread();
		t.start();
		
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		synchronized(lock)
		{
			x=1;
		}
		a[0] =2;
		
	}
	
	static class MyThread extends Thread
	{
		public void run()
		{
			synchronized(lock)
			{
                            a[x]=1;
			}

		}
	}
}
