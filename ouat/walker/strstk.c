#include <errno.h>
#include <stdlib.h>
#include <string.h>

#include "nbcompat.h"
#include "strstk.h"

static int strstack_expand(strstack_t *);

int
strstack_init(strstack_t *ss)
{
	ss->ss_string = NULL;
	ss->ss_nfull = 0;
	ss->ss_capacity = 1;

	ss->ss_string = malloc(ss->ss_capacity * sizeof(*ss->ss_string));
	if (ss->ss_string == NULL)
		return -1;

	return 0;
}

static int
strstack_expand(strstack_t *ss)
{
	int ncapacity = ss->ss_capacity * 2;

	if (ncapacity < ss->ss_capacity) {
		errno = ERANGE;
		return -1;
	}

	char **nstring = realloc(ss->ss_string,
	    ncapacity * sizeof(*ss->ss_string));

	if (nstring == NULL)
		return -1;

	ss->ss_string = nstring;
	ss->ss_capacity = ncapacity;

	return 0;
}

int __printflike(2, 3)
strstack_pushf(strstack_t *ss, const char *fmt, ...)
{
	char *copy;
	va_list ap;

	va_start(ap, fmt);

	if (vasprintf(&copy, fmt, ap) == -1)
		return -1;

	va_end(ap);

	if (ss->ss_nfull == ss->ss_capacity && strstack_expand(ss) == -1) {
		const int serrno = errno;
		free(copy);
		errno = serrno;
		return -1;
	}

	ss->ss_string[ss->ss_nfull++] = copy;

	return ss->ss_nfull - 1;
}

int
strstack_push(strstack_t *ss, const char *s)
{
	return strstack_pushf(ss, "%s", s);
}

void
strstack_popto(strstack_t *ss, int nfull)
{
	while (ss->ss_nfull > nfull) {
		char *top = ss->ss_string[--ss->ss_nfull];
		free(top);
	}
}

void
strstack_pop(strstack_t *ss)
{
	strstack_popto(ss, ss->ss_nfull - 1);
}

int
strstack_fprintf(FILE *stream, strstack_t *ss) 
{
	int i, totwritten = 0;

	for (i = 0; i < ss->ss_nfull; i++) {
		int nwritten = fprintf(stream, "%s", ss->ss_string[i]);

		if (nwritten < 0)
			return nwritten;

		totwritten += nwritten;
	}
	return totwritten;
}

#ifdef TEST
#include <stdlib.h>

strstack_t ss;

static void p(void), q(void), r(void), s(void), t(void), u(void);

static void
p(void)
{
	int depth = strstack_push(&ss, "I");
	q();
	strstack_popto(&ss, depth);
	strstack_push(&ss, "I");
	r();
}

static void
q(void)
{
	strstack_push(&ss, " love");
}

static void
r(void)
{
	strstack_push(&ss, " dig");
}

static void
s(void)
{
	int depth = strstack_push(&ss, " king");
	t();
	strstack_popto(&ss, depth);
	depth = strstack_push(&ss, " donkey");
	u();
}

static void
t(void)
{
	strstack_push(&ss, " kong");
}

static void
u(void)
{
	strstack_push(&ss, " kong");
}

int
main(int argc, char **argv)
{
	strstack_init(&ss);
	p();
	s();
	strstack_fprintf(stdout, &ss);
	fprintf(stdout, "\n");
	return EXIT_SUCCESS;
}
#endif
