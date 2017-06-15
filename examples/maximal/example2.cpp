// Copyright (c) 2016 Runtime Verification Inc. (RV-Predict Team). All Rights Reserved.

#include <thread>
#include <mutex>

using namespace std;

mutex l;
unsigned char x,y,z;

void thread1() {
  l.lock();
    y = 1;
    x = 1;
    if (x == 2) {
      z = 1;
    }
  l.unlock();
}

void thread2() {
  this_thread::yield();
  l.lock();
    x = 2;
  l.unlock();
  y = 2;
}

int main() {
    thread t1(thread1);
    thread t2(thread2);
    t1.join();
    t2.join();
    return 0;
}
