/* Copyright (c) 2016,2017,2018 Runtime Verification, Inc.
 * All rights reserved.
 */

#include <thread>
#include <string>
#include <mutex>

using namespace std;

class Helper {
public:
    string *data;

    Helper() {
        data = new string;
    }
};


Helper *helper;
mutex helper_mutex;

Helper* getHelper() {
    if(helper == 0) {
       helper_mutex.lock();
       if(helper == 0) {
           helper = new Helper();
       }
       helper_mutex.unlock();
    }
    return helper;
}

void thread1() {
    getHelper();
}

void thread2() {
    getHelper();
}

int main() {
    thread t1(thread1);
    thread t2(thread2);

    t1.join();
    t2.join();

    return 0;
}
