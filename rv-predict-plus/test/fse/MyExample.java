package fse;

public class MyExample 
{
	 public static volatile Integer value = 1;
	 
	 public static void main (String [] args) 
	 {
	   for (int i = 0; i < 1; i++)
	   {
	     Thread t1 = new MyExampleThread(1);
	     Thread t2 = new MyExampleThread(2);
	     t2.start();
	     t1.start();
	     try {
	       t1.join();
	       t2.join();
	     } catch (InterruptedException e) {
	       e.printStackTrace();
	       System.exit(1);
	     }
	     System.out.println("value = " + value.toString());
	   }
	 }
	}
