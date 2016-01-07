#ifndef HASHER
#define HASHER

#include "vector.h"
#include "tsan_defs.h"
#include "tsan_mman.h"
#include "tsan_mutex.h"

namespace __tsan{
namespace __RV {

const unsigned int MOD = (1 << 30);

template<typename T>
    unsigned int hasher(T x) {
        unsigned long long y = x;
        return (y * 13337 + 189) % MOD;
    }

template<typename Key, typename Value, unsigned int (*thasher)(const Key) = hasher<Key> >
    struct RVHash {
        struct entry {
            Key key;
            Value value;

            entry(const entry &e):
              key(e.key),
              value(e.value){}

            entry(const Key &k, const Value &v):
                key(k),
                value(v){}

            ~entry() {
              key.~Key();
              value.~Value();
            }
        };

        struct bucket {
          __tsan::__RV::vector<entry> ent;

            int find_position(const Key &key) {
                for(int i = 0; i < (int)ent.size(); ++i) {
                    if(ent[i].key == key)
                        return i;
                }
                return ent.size();
            }

            void init() {
              ent.arr = 0;
              ent.clear();
            }

            void clear() {
              ent.clear();
            }

            ~bucket() {
              ent.~vector();
            }

            int size() {
                return ent.size();
            }

            entry& operator[](int i) {
                return ent[i];
            }

            const entry& operator[](int i) const {
              return ent[i];
            }

            int count(const Key &key) {
                int pos = find_position(key);
                if(pos == ent.size())
                    return 0;
                else
                    return 1;
            }

            Value& get(const Key &key) {
                int pos = find_position(key);
                return ent[pos].value;
            }

            const Value& get(const Key &key) const {
                int pos = find_position(key);
                return ent[pos].value;
            }

            void insert(const Key &key, const Value &value) {
                entry e(key, value);
                if(count(key))
                    return;
                ent.push_back(e);
            }

            bool erase(const Key &key) {
                int pos = find_position(key);
                if(pos != ent.size()) {
                  ent.erase_position(pos);
                  return true;
                } else {
                  return false;
                }
            }
        };

        int size;
        int entries;

        bucket* b;

        void *new_alloc() {
            return alloc(sizeof(bucket) * size);
        }

        void realloc() {
            void* mem = new_alloc();
            b = (bucket *)mem;
            for(int i = 0; i < size; ++i) {
              b[i].init();
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

        bucket& get_bucket(const Key &key) {
            int val = thasher(key) % size;
            return b[val];
        }
        const bucket& get_bucket(const Key &key) const {
            int val = thasher(key) % size;
            return b[val];
        }

        int count(const Key &key) {
            bucket& now = get_bucket(key);
            return now.count(key);
        }

        Value& get(const Key &key) {
            bucket& now = get_bucket(key);
            return now.get(key);
        }

        const Value& get(const Key &key) const {
          bucket& now = get_bucket(key);
          return now.get(key);
        }

        Value& operator[](const Key &key) {
          return get(key);
        }

        const Value& operator[](const Key key) const {
          return get(key);
        }

        void internal_insert(const Key key, const Value &value) {
            bucket& now = get_bucket(key);
            now.insert(key, value);
        }

        void insert(const Key& key, const Value &value) {
            ++entries;
            internal_insert(key, value);
            resize();
        }

        void erase(const Key& key) {
            bucket& now = get_bucket(key);
            now.erase(key);
            entries -= now.erase(key);
            resize();
        }
    };

}
}
#endif
