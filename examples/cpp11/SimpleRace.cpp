// Copyright (c) 2016 Runtime Verification Inc. (RV-Predict Team). All Rights Reserved.

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

    return 0;
}
