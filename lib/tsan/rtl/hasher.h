#ifndef HASHER
#define HASHER

#include "vector.h"
#include "tsan_defs.h"
#include "tsan_mman.h"
#include "tsan_mutex.h"

namespace __tsan{
namespace __RV {

const unsigned int MOD = (1 << 31);

template<typename T>
    unsigned int hasher(T x) {
        unsigned long long y = x;
        return (y * 13337 + 189) % MOD;
    }

template<typename Key, typename Value, unsigned int (*thasher)(Key) = hasher<Key> >
    struct RVHash {
        struct entry {
            Key key;
            Value value;

            entry() {
            }
            entry(Key k, Value v) {
                key = k;
                value = v;
            }
        };

        struct bucket {
            vector<entry> ent;

            int find_position(Key key) {
                for(int i = 0; i < ent.size(); ++i) {
                    if(ent[i].key == key)
                        return i;
                }
                return ent.size();
            }

            void clear() {
              ent.clear();
            }

            int size() {
                return ent.size();
            }

            entry& operator[](int i) {
                return ent[i];
            }

            int count(Key key) {
                int pos = find_position(key);
                if(pos == ent.size())
                    return 0;
                else
                    return 1;
            }

            Value get(Key key) {
                int pos = find_position(key);
                return ent[pos].value;
            }

            void insert(Key key, Value value) {
                entry e(key, value);
                if(count(key))
                    return;
                ent.push_back(e);
            }

            void erase(Key key) {
                int poz = find_position(key);
                ent.erase_position(poz);
            }
        };

        int size;
        int entries;
        mutable StaticSpinMutex mutex;

        bucket* b;

        void *new_alloc() {
            return alloc(sizeof(bucket) * size);
        }

        void realloc() {
            void* mem = new_alloc();
            b = (bucket *)mem;
            for(int i = 0; i < size; ++i) {
              b[i].clear();
            }
        }

        RVHash() {
            size = 16;
            entries = 0;
            realloc();
        }

        ~RVHash() {
          internal_free(b);
        }

        void rebuild(int old_size) {
            bucket* old = b;
            realloc();


            for(int i = 0; i < old_size; ++i) {
                bucket &now = old[i];
                for(int j = 0; j < now.size(); ++j) {
                    entry &e = now[j];
                    internal_insert(e.key, e.value);
                }
            }

            internal_free(old);
        }

        void grow() {
            int old_size = size;
            size *= 2;
            rebuild(old_size);
        }

        void shrink() {
            int old_size = size;
            size /= 2;
            rebuild(old_size);
        }


        void resize() {
            int total = size * 6;
            if(entries == total) {
                grow();
            }else if(entries == total / 6 && size > 16) {
                shrink();
            }
        }

        bucket& get_bucket(Key key) {
            int val = thasher(key) % size;
            return b[val];
        }

        int count(Key key) {
            SpinMutexLock lock(&mutex);
            bucket& now = get_bucket(key);
            return now.count(key);
        }

        Value get(Key key) {
            SpinMutexLock lock(&mutex);
            bucket& now = get_bucket(key);
            return now.get(key);
        }

        void internal_insert(Key key, Value value) {
            bucket& now = get_bucket(key);
            now.insert(key, value);
        }

        void insert(Key key, Value value) {
            SpinMutexLock lock(&mutex);
            ++entries;
            internal_insert(key, value);
            resize();
        }

        void erase(Key key) {
            SpinMutexLock lock(&mutex);

            bucket& now = get_bucket(key);
            now.erase(key);
            --entries;
            resize();
        }
    };

};
};
#endif
