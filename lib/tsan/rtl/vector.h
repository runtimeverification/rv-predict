

#ifndef VECTOR_H
#define VECTOR_H

#include "tsan_mman.h"
#include "tsan_defs.h"

namespace __tsan{

  namespace __RV {

    void* alloc(int sz) {
      static MBlockType _typ = MBlockMetadata;
      return internal_alloc(_typ, sz);
    }

    template<typename value>
      struct vector {

        int sz;
        int max_sz;
        value* arr;

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
          max_sz = max_sz * 5 / 4;
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
          return sz == 0;
        }

        int size() {
          return sz;
        }

        int max_size() {
          return max_sz;
        }


        value back() {
          return arr[sz - 1];
        }


        int find(value v) {
          for(int i = 0; i < sz; ++i) {
            if(arr[i] == v) {
              return i;
            }
          }
          return sz;
        }

        void erase_position(int poz) {
          for(int i = poz; i + 1 < sz; ++i) {
            arr[i] = arr[i + 1];
          }
          --sz;
          resize();
        }

        void erase(value v) {
          int poz = find(v);
          if(poz != sz)
            erase_position(poz);
        }

        void push_back(value v) {
          resize();
          arr[sz] = v;
          sz++;
        }

        void insert(int poz, value v) {
          resize();
          ++sz;
          for(int i = sz; i > poz; --i)
            arr[i] = arr[i - 1];
          arr[poz] = v;
        }

        void pop_back() {
          --sz;
          resize();
        }
        void clear() {
          sz = 0;
          max_sz = 8;
          if(arr != 0){
            internal_free(arr);
            arr = 0;
          }
          arr = new_alloc();
        }

        vector() {
          arr = 0;
          clear();
        }

        ~vector() {
          if(arr != 0) {
            internal_free(arr);
            arr = 0;
          }
        }
      };
  };
};

#endif
