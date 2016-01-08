// Copyright (c) 2016 Runtime Verification Inc. (RV-Predict Team). All Rights Reserved.

#include <thread>
#include <exception>
#include <iostream>

using namespace std;

bool condition = false;
int sharedVar;

void thread1() {
    sharedVar = 1;
    condition = true;
}

void thread2() {
    while(!condition) {
        this_thread::yield();
    }
    if(sharedVar != 1) {
        throw new runtime_error("How is this possible!?");
    }
}

int main() {
    thread t1(thread1);
    thread t2(thread2);


    t1.join();
    t2.join();
    return 0;
}


