// Copyright (c) 2016 Runtime Verification Inc. (RV-Predict Team). All Rights Reserved.

#include <thread>
#include <mutex>

using namespace std;

mutex l;
unsigned char x,y;

void thread1() {
  l.lock();
    x = 1;
  l.unlock();
  y = 1;
  l.lock();
    x = 1;
  l.unlock();
}

void thread2() {
  this_thread::yield();
  l.lock();
    if (x > 0) {
      y = 2;
   }
  l.unlock();
}

int main() {
    thread t1(thread1);
    thread t2(thread2);
    t1.join();
    t2.join();
    return 0;
}
