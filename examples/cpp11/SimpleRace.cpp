/* Copyright (c) 2016,2017,2018 Runtime Verification, Inc.
 * All rights reserved.
 */

#include <iostream>
#include <thread>

using namespace std;

int sharedVar;

void thread1() {
   sharedVar++; 
}

void thread2() {
    sharedVar++;
}

int main() {
    sharedVar = 1;
    thread t1(thread1);
    thread t2(thread2);

    t1.join();
    t2.join();

    cout << sharedVar << endl;

    return 0;
}
