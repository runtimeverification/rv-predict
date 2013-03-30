package emp;

import java.util.Random;

public class Example {
	Random r;
	int x;
	boolean b; 
	int[] a;
	MyThread t1,t2;
	public static void main(String[] args) throws InterruptedException
	{
		
		Example t = new Example();
		t.a = new int[2];
		int[] at = t.a;
		at[0]=0;
		at[1]=1;
		
		//t.wait(1000);//that does not have lock release/acquire semantics?
		byte p = 'a';
		Object o = p;
		System.out.print(o);
		long pp = 1000;
		o = pp;
		System.out.print(o);
		o =t;
		System.out.print(o);
		boolean bb = false;
		o = bb;
		System.out.print(o);

		
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
		public void run()
		{
			LocalClass o = new LocalClass();
			o.setX(t.r.nextInt());
			int temp = o.getX();
			if(temp>10)
			t.b = t.r.nextBoolean();
			else
				t.b = true;
			
			if(t.b)
			{
				synchronized(t)
				{
					inc();
					try {
						t.wait();//This program has a bug here: may block forever
						dec();
						t.notify();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			else
			{
				mul();
				div();
			}
		}
	}
}
