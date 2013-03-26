package reflection.finalize.two;

class Simple {
   static int i;
   public static void main(String[] args) {
      try {
         Class.forName("reflection.finalize.two.Simple").getConstructor().newInstance();
      } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      System.gc();
      for (int x = 0; x < 10000; x++) {
      }
   }
   public Simple() {
      finalize();
   }
   @Override protected void finalize() {
      i++;
   }
}
