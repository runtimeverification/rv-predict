package rv;

public class TestExample {
	
	static Object lock = new Object();

	static int x;
	static int y;

	public static void main(String[] args)
	{
		
		MyThread t = new MyThread();
		t.start();
		synchronized(lock)
		{		
			y=1;
		}
		synchronized(lock)
		{
			x=1;
		}
		synchronized(lock)
		{		
			y=-1;
		}
		synchronized(lock)
		{
			x=-1;
		}
		synchronized(lock)
		{
			y=1;
		}
		
	}
	public static class MyThread extends Thread
	{
		public void run()
		{
			synchronized(lock)
			{	
				
				if(x>=0)
				{

					if(y>0)
						System.out.println("1");
					else
						System.out.println("2");

				}
				else
				{
					if(y>0)
						System.out.println("3");
					else
						System.out.println("4");

				}
					
			}
			
		}
	}
}


