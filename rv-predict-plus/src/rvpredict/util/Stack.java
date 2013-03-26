package rvpredict.util;

public class Stack<T> {
  static final int CAPACITY = 100;
  int curr_index = 0;
  T[] elements;
  int capacity;

  @SuppressWarnings("unchecked")
  public Stack() {
    elements = (T[]) new Object[CAPACITY];
    capacity = CAPACITY;
  }

  @SuppressWarnings("unchecked")
  public Stack(int initial_capacity) {
    elements = (T[]) new Object[initial_capacity];
    capacity = initial_capacity;
  }

  public T peek() {
    return elements[curr_index - 1];
  }

  //returns the last element popped
  public T pop(int num) {
    curr_index -= num;
    return elements[curr_index];
  }

  @SuppressWarnings("unchecked")
  public void push(T elt) {
    if (curr_index < elements.length) {
      elements[curr_index++] = elt;
    } else {
      int len = elements.length;
      T[] old = elements;
      elements = (T[]) new Object[2*len];
      for (int i = 0; i < len; i++) {
        elements[i] = old[i];
      }
      elements[curr_index++] = elt;
    }
  }

  public int size(){
    return curr_index;
  } 

  public void clear() {
    curr_index = 0;
  }

  @Override public String toString(){
    if(curr_index == 0) return "[]";
    StringBuilder ret = new StringBuilder("[" + elements[0].toString());
    for(int i = 1; i < curr_index; ++i){
      ret.append(", ");
      ret.append(elements[i].toString());
    }
    ret.append("]");
    return ret.toString();
  }
}
