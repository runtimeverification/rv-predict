/* Copyright (c) 2016,2017,2018 Runtime Verification, Inc.
 * All rights reserved.
 */

#include <vector>
#include <thread>

using namespace std;

vector<int> v;

void thread1() {
    v.push_back(1);
}

void thread2() {
    v.push_back(2);
}

int main() {
    thread t1(thread1);
    thread t2(thread2);

    t1.join();
    t2.join();

    return 0;
}
