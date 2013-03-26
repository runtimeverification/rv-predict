package safefinalize;

class Simple {
   int i = 1;
   public static void main(String[] args) {
      new Simple();
      System.gc();
   }
   Simple() {
   }
   @Override protected void finalize() {
      i++;
   }
}
