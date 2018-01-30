/* Copyright (c) 2016,2017,2018 Runtime Verification, Inc.
 * All rights reserved.
 */

#include <thread>
#include <mutex>
#include <atomic>

using namespace std;


mutex l;
bool ready = false;
enum State { STOP, INIT, START };
State state = STOP;

void init() {
  l.lock();
    ready = true;
  l.unlock();
  state = INIT;
  l.lock();
    ready = true;
  l.unlock();
}

void start() {
  this_thread::yield();
  l.lock();
    if (ready && state == INIT) {
      state = START;
   }
  l.unlock();
}

void stop() {
  l.lock();
    ready = false;
    state = STOP;
  l.unlock();
}


int main() {
    thread t1(init);
    thread t2(start);
    thread t3(stop);
    t1.join();
    t2.join();
    t3.join();
    return 0;
}
