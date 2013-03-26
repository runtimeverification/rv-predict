package failfinalize;

class Simple {
   static int i;
   public static void main(String[] args) {
      new Simple();
      System.gc();
      for (int x = 0; x < 10000; x++) {
      }
   }
   Simple() {
      finalize();
   }
   @Override protected void finalize() {
      i++;
   }
}
