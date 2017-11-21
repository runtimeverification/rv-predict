#ifndef _FOO_H_
#define _FOO_H_

typedef struct _pqr {
	int p, q, r;
} pqr_t;

typedef struct _xyz {
	pqr_t x, y, z;
} xyz_t;

void foo(void);

#endif /* _FOO_H_ */
