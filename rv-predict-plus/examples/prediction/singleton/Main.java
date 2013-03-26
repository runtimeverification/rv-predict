package singleton;

class Main {
   static class Singleton {
      final static Singleton instance = new Singleton();
      static Singleton v() { return instance; }
      int i;
      Singleton() {
         i = 2;
      }
   }
   public static void main(String[] args) {
      (new Thread(){ @Override public void run() {
         Singleton.v();
      }}).start();
      System.out.println(Singleton.v().i);
   }
}
