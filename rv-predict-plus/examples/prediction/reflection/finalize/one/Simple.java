package reflection.finalize.one;

class Simple {
   static int i;
   public static void main(String[] args) {
      try {
      Class.forName("reflection.finalize.one.Simple").newInstance();
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
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
