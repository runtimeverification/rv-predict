#ifndef _STRSTK_H_
#define _STRSTK_H_

#include <stdio.h>	/* for FILE */

typedef struct _strstack {
	char **ss_string;
	int ss_capacity, ss_nfull;
} strstack_t;

int strstack_init(strstack_t *);
int strstack_push(strstack_t *, const char *);
int __printflike(2, 3) strstack_pushf(strstack_t *, const char *, ...);
void strstack_popto(strstack_t *, int);
void strstack_pop(strstack_t *);
int strstack_fprintf(FILE *, strstack_t *);

#endif /* _STRSTK_H_ */
