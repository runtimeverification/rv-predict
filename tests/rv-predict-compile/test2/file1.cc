#include <iostream>
#include <unordered_set>


void bar();

void foo() {
    std::unordered_set<int> H;
    H.insert(5);
    bar();
}
