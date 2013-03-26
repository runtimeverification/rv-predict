package finerimpure;

import java.util.*;

class Main {
   private static final HashSet<Object> s = new HashSet<Object>();
   public static void main(String[] args) {
      new Thread(){@Override public void run() {
         s.size();
      }}.start();
      new Thread(){@Override public void run() {
         s.size();
      }}.start();
   }
}
