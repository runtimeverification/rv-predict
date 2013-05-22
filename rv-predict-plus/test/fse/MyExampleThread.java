package fse;

public class MyExampleThread extends Thread
{
 int id;
 MyExampleThread (int id)
 {
   this.id = id;
   System.out.println ("thread " + id + " starting.");
 }
 public void run()
 {
   int j =0;
   
   try
   {
     for (int i = 0; i < 10000; i++) 
     {
       int old = MyExample.value;
       old = old+1;
       
       if (i >= 9998 && this.id == 2) {
         Thread.sleep(100);
       }
       j=i;
       if (MyExample.value != old - 1) {
         int x = 1 / (MyExample.value-5000);
       }
       MyExample.value = old;
     }
   }catch(Exception e)
     {
       System.out.println("Died at " + j);
       e.printStackTrace();
       System.exit(1);
     }
 }
}
