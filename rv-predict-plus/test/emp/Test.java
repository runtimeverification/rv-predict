package emp;

public class Test {
	
	static int x;
	static int[] a;
	static MyThread thrd;
	public static void main(String[] args) throws InterruptedException
	{
	
		Test t = new Test();
		t.x=10;
		a = new int[2];
		a[0]=1;
		a[1]=2;
		
		thrd = new MyThread(t);
		thrd.start();
		
		for(int i=0;i<100;i++)
		{
			t.m();
		}
		
		thrd.join();
		
		print();
	}
	void m()
	{
		synchronized(this)
		{
			x++;
			Thread.yield();
		}
	}
	
	static synchronized void print()
	{
		System.out.println(x+""+thrd.t.x);

	}
	static class MyThread extends Thread
	{
		Test t;
		MyThread(Test t)
		{
			this.t = t;
		}
		public void run()
		{
			m2();
		}
		private void m2()
		{
			synchronized(t)
			{
				try {
					t.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				t.x++;
				t.notify();
			}
		}
	}
}
