package emp;

public class ArrayTest {

	int[] a;
	MyThread t1,t2;
	public static void main(String[] args) throws InterruptedException
	{
		
		ArrayTest t = new ArrayTest();
		t.a = new int[2];
		int[] at = t.a;
		
		t.t1 = new MyThread(t,1);
		t.t2 = new MyThread(t,2);
		
		t.t1.start();
		t.t2.start();
	
		at[0]=10;
		at[1]=20;
		
		t.t1.join();
		t.t2.join();

		String s = at[0]+" "+at[1];
		System.out.println(s);
	}
	
	static class MyThread extends Thread
	{
		int id;
		ArrayTest t;
		MyThread(ArrayTest t, int id)
		{
			this.t = t;
			this.id = id;
		}
		
		
		public void run()
		{
			if(id==1)
				t.a[0] = id;
			else
				t.a[1] =id;
		}
	}
}
