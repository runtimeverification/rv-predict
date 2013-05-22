package emp;

public class Simple {
	static Object lock = new Object();
	
	static int x=0;
	static int y=0;
	static int z=0;
	static int q=0;
	static int[] foo = new int[10];
	
	static  void foo(boolean b){
	    z++;
		//synchronized(lock)
		if(b) System.out.println(q++);
	}
	
	public static void main(String[] args)
	{
	  try{
		MyThread t1 = new MyThread();
		MyThread t2 = new MyThread();

		t1.start();
		t2.start();

		for(int i = 0; i < 2000 ; ++i){
			y++;
			foo[i] = i;
			foo(true);
		}
		
	/*	synchronized(lock)
		{
			x++;
		}
		x=0;*/
		
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
			{x++; y++; q++; z++;
			foo[0] = 1;
			}
			System.out.println(1/x);

		}
	}
}
