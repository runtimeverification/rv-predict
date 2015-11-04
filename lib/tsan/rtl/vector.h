

#ifndef VECTOR_H
#define VECTOR_H

#include "tsan_mman.h"
#include "tsan_mutex.h"
#include "tsan_defs.h"

namespace __tsan{

  namespace __RV {

    void* alloc(int sz) {
      static MBlockType _typ = MBlockRV;
      return internal_alloc(_typ, sz);
    }

    template<typename value>
      struct vector {

        int sz;
        int max_sz;
        value* arr;
        mutable StaticSpinMutex mutex;

        value* new_alloc() {
          return (value *)alloc(max_sz * sizeof(value)); 
        }

        void move_to_new_zone() {
          value* narr = new_alloc();
          for(int i = 0; i < sz; ++i)
            narr[i] = arr[i];
          internal_free(arr);

          arr = narr;
        }

        void resize(int new_sz) {
          max_sz = new_sz;
          move_to_new_zone();
        }

        void grow() {
          max_sz *= 2;
        }

        void shrink() {
          max_sz /= 2;
        }

        void resize() {
          if(sz == max_sz) {
            grow();
            move_to_new_zone();
          } else if(4 * sz <= max_sz && max_sz > 8) {
            shrink();
            move_to_new_zone();
          }
        }

        value& operator[](int poz) {
          return arr[poz];
        }

        bool empty() {
          SpinMutexLock lock(&mutex);
          return sz == 0;
        }

        int size() {
          SpinMutexLock lock(&mutex);
          return sz;
        }

        int max_size() {
          SpinMutexLock lock(&mutex);
          return max_sz;
        }


        value back() {
          SpinMutexLock lock(&mutex);
          return arr[sz - 1];
        }


        int find(value v) {
          SpinMutexLock lock(&mutex);
          for(int i = 0; i < sz; ++i) {
            if(arr[i] == v) {
              return i;
            }
          }
          return sz;
        }

        void erase_position(int poz) {
          SpinMutexLock lock(&mutex);
          for(int i = poz; i + 1 < sz; ++i) {
            arr[i] = arr[i + 1];
          }
          --sz;
          resize();
        }

        void erase(value v) {
          SpinMutexLock lock(&mutex);
          int poz = find(v);
          if(poz != sz)
            erase_position(poz);
        }

        void push_back(value v) {
          SpinMutexLock lock(&mutex);
          arr[sz] = v;
          sz++;
          resize();
        }

        void insert(int poz, value v) {
          SpinMutexLock lock(&mutex);
          ++sz;
          resize();
          for(int i = sz; i > poz; --i)
            arr[i] = arr[i - 1];
          arr[poz] = v;
        }

        void pop_back() {
          SpinMutexLock lock(&mutex);
          --sz;
          resize();
        }
        void clear() {
          SpinMutexLock lock(&mutex);
          sz = 0;
          max_sz = 16;
          resize();
        }

        vector() {
          SpinMutexLock lock(&mutex);
          sz = 0;
          max_sz = 8;
          arr = new_alloc();
        }

        ~vector() {
          for(int i = 0; i < sz; ++i) {
            arr[i].~value();
          }
          internal_free(arr);
        }
      };
  };
};

#endif
