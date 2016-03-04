#include <iostream>
#include <thread>
#include <list>
#include <algorithm>

using namespace std;

// a global variable
std::list<int>myList;

void addToList(int max, int interval)
{
  for (int i = 0; i < max; i++) {
    if( (i % interval) == 0) myList.push_back(i);
  }
}

void printList()
{
  for (auto itr = myList.begin(), end_itr = myList.end(); itr != end_itr; ++itr ) {
    cout << *itr << ",";
  }
}

int main()
{
  int max = 100;

  std::thread t1(addToList, max, 1);
  std::thread t2(printList);
  std::thread t3(addToList, max, 10);
  std::thread t4(printList);

  t1.join();
  t2.join();
  t3.join();
  t4.join();

  return 0;
}
