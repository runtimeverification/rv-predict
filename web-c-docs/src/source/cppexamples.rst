3. Unsafe Data Structure Manipulation
--------------------------------------

Many standard library data structures are not designed to be used in a
multithreaded environment, e.g. widely used vector class.

First, consider a simple example (examples.demo/unsafe-vector.c):

.. code-block:: c

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

In the example both threads are trying to add to ``std::vector`` without synchronization.
RV-Predict/C catches the data race as shown below. 
 
.. code-block:: none

  Data race on global 'v' of size 24 at 0x00000153ecc8 (a.out + 0x00000153ecd8): {{{
      Concurrent read in thread T2 (locks held: {})
   ---->  at thread1() unsafe-vector.cpp:12
      T2 is created by T1
          at main unsafe-vector.cpp:20

      Concurrent write in thread T3 (locks held: {})
   ---->  at thread2() unsafe-vector.cpp:16
      T3 is created by T1
          at main unsafe-vector.cpp:20
  }}}

  ...

This example is easily fixed by using some synchronization mechanisms (e.g., locks) when
performing the access to the shared variable ``v``. 

Consider now a more interesting example (see below), where we used ``vector`` data structure
to implement a stack. At first sight, it looks like all the operations are properly synchronized, 
however just because we are using a mutex or other synchronization mechanism to protect 
shared data, it does not mean we are protected from race conditions!

.. code-block:: c

  using namespace std;
  mutex myMutex;
  class stack
  {
  public:
    stack() {};
    ~stack() {};
    void pop();
    int top() { return data.back(); }
    void push(int);
    void print();
    int getSize() { return data.size(); }
  private:
      vector<int> data;
  };

  void stack::pop()
  {
    lock_guard<mutex> guard(myMutex);
    data.erase(data.end()-1);
  }

  void stack::push(int n) {
    lock_guard<mutex> guard(myMutex);
    data.push_back(n);
  }

  void stack::print()
  {
    cout << "initial stack : " ;
    for(int item : data)
        cout << item << " ";
    cout << endl;
  }

  void process(int val, string s) {
    lock_guard<mutex> guard(myMutex);
    cout << s << " : " << val << endl;
  }

  void thread_function(stack& st, string s) {
    int val = st.top();
    st.pop();
    process(val, s);
  }

  int main()
  {
      stack st;
      for (int i = 0; i < 10; i++)  st.push(i);

      st.print();

      while(true) {
        if(st.getSize() > 0) {
          thread t1(&thread_function, ref(st), string("thread1"));
          thread t2(&thread_function, ref(st), string("thread2"));
          t1.join();
          t2.join();
        } else break;
      }

      return 0;
  }

(For full source see
``/usr/share/examples/rv-predict-c/cpp11/stack.cpp``.)  In the example
below each shared access is guarded using

.. code-block:: c
    
  lock_guard<mutex> guard(myMutex);
  
Now, it would be tempting to conclude that the code is thread-safe. 
However, we actually cannot rely on the result of getSize(). 
Although it might be correct at the time of call, once it returns
other threads are free to access the stack and might push() new 
elements to the stack or pop() existing elements of the stack. 

This particular data race is consequence of the interface design, and
the use of mutex internally to protect the stack does not prevent it. 
As shown below, RV-Predict/C can be used to detect these kind of flaws. 

.. code-block:: none

  Data race on array element #11: {{{
      Concurrent read in thread T3 (locks held: {})
   ---->  at stack::top() Stack.cpp:18
      T3 is created by T1
          at main Stack.cpp:66

      Concurrent write in thread T2 (locks held: {WriteLock@27})
   ---->  at stack::pop() Stack.cpp:29
          - locked WriteLock@27 at stack::pop() Stack.cpp:29 
      T2 is created by T1
          at main Stack.cpp:65
  }}}




