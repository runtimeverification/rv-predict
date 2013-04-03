package emp;

public class Simple {
	static int x=0;
	public static void main(String[] args)
	{
		try{
		MyThread t1 = new MyThread();
		MyThread t2 = new MyThread();

		t1.start();
		t2.start();
		t1.join();
		t2.join();
		
		System.out.println(x);
		
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
	static class MyThread extends Thread
	{
		public void run()
		{
		x++;
		}
	}
}
