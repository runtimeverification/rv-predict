package rvpredict.util;

import java.util.HashMap;

public class CounterMap<Type> extends HashMap<Type, Integer> {
  private static final long serialVersionUID = 0;

  public CounterMap() {
    super();
  }

  public CounterMap(CounterMap<Type> m) {
    super(m);
  }

  public void put(Type key){
    Integer c = super.get(key);
    if(c != null)
      put(key, c + 1);
    else 
      put(key, 1);
  }

  public Integer get(Type key){
    Integer c = super.get(key);
    if(c == null) return 0;
    else return c;
  }

  public void decrement(Type key){
    Integer c = super.get(key);
    if(c == null) return;
    if(c == 0) remove(key);
    else put(key, c - 1);  
  }
  //Test code
  /*
     public static void main(String[] args){
     CounterMap<String> foo = new CounterMap<String>(){{ put("foo"); 
     put("foo");
     put("foo");  
     put("bar");
     }} ;
     System.out.println(foo);
     System.out.println(foo.get("foo"));
     System.out.println(foo.get("bar"));
     System.out.println(foo.get("car"));
     foo.decrement("foo");
     foo.decrement("bar");
     System.out.println(foo);
     }
     */
}
// vim: tw=100:sw=2
