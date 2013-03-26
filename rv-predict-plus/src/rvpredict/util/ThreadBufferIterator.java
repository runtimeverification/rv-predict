package rvpredict.util;

import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.AbstractIterator;

public class ThreadBufferIterator<E> extends AbstractIterator<E> {
  private static final int defaultBuffersize = 1000;
  private boolean done = false;
  private final BlockingQueue<E> q;

  public ThreadBufferIterator(final Iterator<E> it) {
    this(it,defaultBuffersize);
  }
  public ThreadBufferIterator(final Iterator<E> it, final int size) {
    q = new ArrayBlockingQueue<E>(size);
    new Thread() { private static final long serialVersionUID = 0; { setDaemon(true); }
      @Override public void run() {
        try {
          while (it.hasNext())
            q.put(it.next());
          done = true;
        } catch (Exception e) { e.printStackTrace(); System.exit(1); }
      }}.start();
  }
  @Override protected E computeNext() {
    try {
      while (!q.isEmpty() || !done) {
        E e = q.poll(1,TimeUnit.SECONDS);
        if ( e != null)
          return e;
      }
    } catch (Exception e) { e.printStackTrace(); System.exit(1); }
    return endOfData();
  }
}
// vim: tw=100:sw=2
