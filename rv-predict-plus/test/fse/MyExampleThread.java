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
   int i = -1;
   try
   {
     for (i = 0; i < 10000; i++) 
     {
       int old = MyExample.value;
       old++;
       if (i >= 9998 && this.id == 2) {
         Thread.sleep(100);
       }
       if (MyExample.value != old - 1) {
         int x = 1 / 0;
       }
       MyExample.value = old;
     }
   }catch(Exception e)
     {
       System.out.println("Died at " + i);
       e.printStackTrace();
       "Crashed_with".equals(e);
       System.exit(1);
     }
 }
}
