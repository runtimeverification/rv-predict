package rvpredict.logging;

import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.lang.Exception;

public class Printer {
   public static void main(String[] args) {
      try{
         CompressedReversedBlockInputStream crbis = new CompressedReversedBlockInputStream(args[0], args[1], args[2]);
         do {
           //System.out.println(new InputStreamReader(crbis));
          /* StringBuilder b = new StringBuilder();
           while(true){
             int c = crbis.read();
             if(c == -1) break;
             b.append((char)c);
           }
           System.out.println(b);*/
           System.out.println(Protos.Events.parseFrom(crbis));
           System.out.println("!!!!!!!NEW BLOCK!!!!!!!!!!!");
         } while(crbis.next());
      } catch (Exception e) {
         e.printStackTrace(); System.exit(1);
      }
   }
}
