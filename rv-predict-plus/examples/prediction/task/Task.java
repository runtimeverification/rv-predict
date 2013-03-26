/**
 * Based on example code from paper "RACER: Effective race detection for Java using AspectJ"
 * (authors: Eric Bodden and Klaus Havelund).
 *
 * The code runs two threads, accessing three different fields, two of which are
 * shared. The shared field shared_protected is protected by a lock. Therefore,
 * a race only occurs on field "shared". The field "not_shared" is not shared because
 * it exists for each Test separately. The thread-local objects analsyis is powerful
 * enough to find this out. Therefore, abc will remove the instrumentation for "not_shared"
 * if the command line parameter -optimizeMaybeSharedPointcut is given.
 *
 * The author releases this code into the public domain. USE AT OWN RISC!
 *
 * Based on work of: Eric Bodden
 */
package task;
class Task implements Runnable {
  static int shared ;
  static int shared_protected ;
  int not_shared ;
  
  public void run () {
    System.out.println (shared++);
    synchronized(Task. class ) {		
      System.out.println ( shared_protected ++);
    }
    System.out.println ( not_shared ++);
  }
	
  public static void main(String [] args) {
    Task task1 = new Task();
    Task task2 = new Task();
    Thread thread1 = new Thread(task1 );
    Thread thread2 = new Thread(task2 );
    thread1.start ();
    thread2.start ();
  }
}
