#include <memory>
#include <mutex>
#include <thread>

struct some_resource
{
    void do_something()
    {}
    
};


std::shared_ptr<some_resource> resource_ptr;
std::mutex resource_mutex;
std::thread thread;
std::thread join;
void foo()
{
  if(!resource_ptr) {
    std::unique_lock<std::mutex> lk(resource_mutex);
    if(!resource_ptr)
    {
        resource_ptr.reset(new some_resource);
    }
    resource_ptr->do_something();
  }
}

int main()
{
    std::thread::thread t1(foo);
    std::thread::thread t2(foo);

    t1.join();
    t2.join();
}

