// Tons of events, and mixed locking standards

package mixedlocksbig;

class Main implements Runnable {
  static long shared;
  static long shared_protected;
  static long shared_protected2;
  static long shared_protected3;
  private long not_shared;

  public void run() {
    for (long i = 0; i < 1000; ++i) {
      // No races, becuase we have good locking habits
      synchronized (Main.class) {
        shared_protected++;
        shared_protected2 = shared_protected * (shared_protected2 + shared);
        shared_protected3 = shared_protected ^ shared_protected2;
      }
      // Race here
      shared++;
      not_shared++;
    }
    synchronized (Main.class) {
      System.out.print(shared + " ");
      System.out.print(shared_protected + " ");
      System.out.print(shared_protected2 + " ");
      System.out.print(shared_protected3 + " ");
      System.out.print(not_shared + " ");
      System.out.println();
    }
  }

  public static void main(String[] args) {
    Main m1 = new Main();
    Main m2 = new Main();
    Main m3 = new Main();
    Main m4 = new Main();
    Main m5 = new Main();

    new Thread(m1).start();
    new Thread(m2).start();
    new Thread(m3).start();
    new Thread(m4).start();
    new Thread(m5).start();
  }
}

