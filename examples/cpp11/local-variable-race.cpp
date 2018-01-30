/* Copyright (c) 2016,2017,2018 Runtime Verification, Inc.
 * All rights reserved.
 */
#include <thread>

void f(int &x) {
    std::thread::thread t1([&x] {x++;});
    x++;
    t1.join();
}

int main() {
    // x lives between main's CFA and f's CFA.
    int x = 0;
    f(x);
    return 0;
}
