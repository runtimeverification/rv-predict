package causal;

public class Main {
	static Object lock = new Object();
	static int x=0;
	static int y=0;

	public static void main(String[] args)
	{
//		MyThread t1 = new MyThread();
//		MyThread t2 = new MyThread();//how about we enable more threads?
//
//		t1.start();
//		t2.start();
		
		MyThread t = new MyThread();
		t.start();
		
		y = 1;
		
		x =1;
		
//		if(y==0)
//		{
//			System.out.println(1/x);//can this throw / by zero?? -- NO!
//		}
	}
	
	static class MyThread extends Thread
	{
		public void run()
		{
			synchronized(lock)
			{
				if(x>0)
				{
//					x = 0;
					System.out.println(1/y);//can this throw / by zero?? -- NO!
//					x = 1;
//					y = 0;
				}
			}

		}
	}
}
