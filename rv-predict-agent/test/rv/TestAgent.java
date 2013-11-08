package rv;

import java.io.PrintStream;
import java.util.Vector;

public class TestAgent {
	
	volatile int x=10,y=100;
	String str;
	String val$value;
	static Object[] o;
	static Object o2;
	static int[] a;
	static boolean[] b;
	static char[] c;
	static short[] s;
	static float[] f;
	static double[] d;
	static long[] l;
	static int mm=11;
	static long kkk =199999999;
//	static boolean q1;
//	static char q2;
//	static long q3;
//	static float q4;
//	static double q5;
//	static byte q6;
//	static short q7;
//	static int q8;

	static int z;
	
		public TestAgent(String name)
		{
//			b[0] = false;
//			b[1] = b[0];
//			c[0]='1';
//			c[1]='0'+1;
//			d[0]=1.00;
//			d[1]=d[0]+100.56;
//			l[0]=1l;
//			l[1]=l[0]+100;
			o[0] = o[1];
			o[1] = new Object();
//			s[1]=10;
//			s[0] = s[1];
//			f[0]=f[1]+10;
			
			str = name;
			
		}
	
		public static void main(String[] args)
		{
//			q1=true;
//			q2='a';
//			q3=1000000000;
//			q4=1f;
//			q5=1.0;
//			q6=100;
//			q7=10000;
//			q8=1;

//			a = new int[2];
//			a[0]=1;
//			a[1]=2;
//			mm=a[0]+a[1];
			b = new boolean[2];
			b[0]=true;
			b[1]=false;
			if(b[0]|b[1])
//			PrintStream out = System.out;
//			logFieldAcc(10,null,1,out,false);
//			out.println("hello world");
			System.out.println("hello world");
			
//			c = new char[2];
//			c[0]='0';
//			c[1]='1';
//			d = new double[2];
//			d[0]=0.01;
//			d[1]=0.02;
//			l = new long[2];
//			l[0]=2000;
//			l[1]=1000;
			o = new Object[2];
			Object[] o1 = o;
			o1[0] = new Object();
			o1[1] = new Object();
			o2 = new Object();
			Object o21 = o2;
			logFieldAcc(0, null, 1, o21, false);

//			s[0]=100;
//			s[1]=0;
//			f[0]=1f;
//			f[1]=10000;
			synchronized(o[1])
			{
			test(false);
			test(true);
			}
		}
		
		
		public synchronized int m()
		{
			
			TestAgent t = new TestAgent("m");
			Size sz = new Size("x",new Vector<String>());
			t.str = sz.name;
			this.x = t.y+1;
			int r = z;
			logFieldAcc(1,null,10,r,false);
			return this.x+r;
		}
		
		public synchronized static void test(boolean b)
		{
			TestAgent t = new TestAgent("test");
			t.x = 0;
			t.y=1;
			z = 1;
			t.y = t.m();
		}
		
		  public static  void logFieldAcc(int ID, final Object o, int SID, final Object v, final boolean write) {

		  }
		  public static  void logArrayAcc(int ID, final Object o, int index, final Object v, final boolean write) {

		  }
		  
		  class Size {
			    final String name;
			    final String[] args;
			    Size(String name, Vector<String> args) {
			        this.args = (String[]) args.toArray(new String[0]);
			        this.name = name;
			      }
		  }
}
