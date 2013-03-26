package unsafemapiterator;
import java.util.*;

 public class UnsafeMapIterator {
   public static void main(String[] args){
    try{
        Map<String, String> testMap = new HashMap<String,String>();
        testMap.put("Foo", "Bar");
        testMap.put("Bar", "Foo");
        Set<String> keys = testMap.keySet();
        Iterator i = keys.iterator();
        i.next();
        testMap.put("breaker", "borked");
        keys.iterator();
     }
     catch(Exception e){
        System.out.println("java found the problem too");
     }
   }
 }

