#include <iostream>

void bar() {
    int y;
    auto x = [&y](){y = 5;};
}
