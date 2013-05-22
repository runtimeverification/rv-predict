package loop;

import java.util.Random;

public class TestLoop {
	Random r;
	int x=0;
	boolean b=false; 
	int[] a;
	
	static int TIMES = 100;
	
	MyThread t1,t2;
	
	public static void main(String[] args) throws InterruptedException
	{
		TestLoop t = new TestLoop();
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
		TestLoop t;
		MyThread(TestLoop t, int id)
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
		private void m()
		{
			
			inc();
			dec();
		}
		public static void m2()
		{
			System.out.println("Hello");
		}
		public void run()
		{
			for(int i=0;i<TIMES;i++)
			{

			int temp =t.r.nextInt();
			if(temp>10)
			t.b = t.r.nextBoolean();
			else
				t.b = true;
						
			if(id<2)
			{
								
				m();
				m2();
			}
			else
			{
				{
					t.b = true;
					
					mul();
					div();
				}
			}
			}
		}
	}
}
