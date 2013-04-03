package emp;

import java.util.Random;

public class Example {
	Random r;
	int x=0;
	boolean b=false; 
	int[] a;
	
	static int xxx = 100;
	
	MyThread t1,t2;
	public static void main(String[] args) throws InterruptedException
	{
		Example t = new Example();
		t.a = new int[2];
		int[] at = t.a;
		at[0]=10;
		at[1]=20;
		
		t.r = new Random();
		t.x=0;		
		t.t1 = new MyThread(t,1);
		t.t2 = new MyThread(t,2);
		
		t.t1.start();
		t.t2.start();
	
		t.t1.join();
		t.t2.join();

		String s = t.x==0?"true":"false";
		System.out.println(s);
	}
	
	static class MyThread extends Thread
	{
		int id;
		Example t;
		MyThread(Example t, int id)
		{
			this.t = t;
			this.id = id;
		}
		private void inc()
		{
			t.x = t.x+id;
		}
		private void dec()
		{
			t.x=t.x-id;
		}
		private void mul()
		{
			t.x=t.x*id;
		}
		private void div()
		{
			t.x=t.x/id;
		}
		private synchronized void m()
		{
			LocalClass o = new LocalClass();
			o.setX(0);
			int t = o.getX();
			
			inc();
			dec();
		}
		public static synchronized void m2()
		{
			System.out.println("Hello");
		}
		public void run()
		{
			LocalClass o = new LocalClass();
			o.setX(t.r.nextInt());
			int temp = o.getX();
			if(temp>10)
			t.b = t.r.nextBoolean();
			else
				t.b = true;
						
			if(id<2)
			{
				synchronized(t)
				{
					inc();
					try {
						t.wait();//This program has a bug here: may block forever
						dec();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				m();
				m2();
			}
			else
			{
				synchronized(t)
				{
					t.notify();

					mul();
					div();
				}
			}
		}
	}
}
