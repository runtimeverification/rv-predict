#include <errno.h>
#include <pthread.h>
#include <string.h>

#include "init.h"
#include "interpose.h"
#include "specific.h"
#include "thread.h"

static pthread_mutex_t specific_mtx;

typedef struct _kd kd_t;

struct _kd {
	pthread_key_t	kd_key;
	void		(*kd_destructor)(void *);
};

int kdcount = 0;
kd_t *kd = NULL;

void
rvp_specific_prefork_init(void)
{
	ESTABLISH_PTR_TO_REAL(int (*)(pthread_key_t *, void (*)(void *)),
	    pthread_key_create);
	ESTABLISH_PTR_TO_REAL(int (*)(pthread_key_t), pthread_key_delete);
	ESTABLISH_PTR_TO_REAL(int (*)(pthread_key_t, const void *),
	    pthread_setspecific);

	const int rc = real_pthread_mutex_init(&specific_mtx, NULL);

	if (rc != 0)
		errx(EXIT_FAILURE, "%s: pthread_mutex_init: %s", __func__,
		    strerror(rc));
}

static void
kd_lock(void)
{
	const int rc = real_pthread_mutex_lock(&specific_mtx);

	if (rc != 0) {
		errx(EXIT_FAILURE, "%s: pthread_mutex_lock: %s", __func__,
		    strerror(rc));
	}
}

static void
kd_unlock(void)
{
	const int rc = real_pthread_mutex_unlock(&specific_mtx);

	if (rc != 0) {
		errx(EXIT_FAILURE, "%s: pthread_mutex_unlock: %s", __func__,
		    strerror(rc));
	}
}

void
rvp_thread_destructor(void *arg)
{
	int i, j;
	rvp_thread_t *t = arg;

	real_pthread_setspecific(rvp_thread_key, t);
	kd_lock();
	for (i = 0; i < kdcount; i++)
		real_pthread_setspecific(kd[i].kd_key, NULL);
	for (i = 0; i < kdcount; i++) {
		pthread_key_t key = kd[i].kd_key;
		void (*destructor)(void *) = kd[i].kd_destructor;

		for (j = 0; j < t->t_nthrspecs; j++) {
			const void *value = t->t_thrspec[j].ts_value;
			if (t->t_thrspec[j].ts_key != key)
				continue;
			assert(value != NULL);
			(*destructor)((void *)value);
		}
	}
	if (t->t_nthrspecs != 0) {
		t->t_nthrspecs = 0;
		free(t->t_thrspec);
	}
	kd_unlock();
	real_pthread_setspecific(rvp_thread_key, NULL);
}

static void
rvp_destructor(void *arg __unused)
{
	int i;
	kd_lock();
	for (i = 0; i < kdcount; i++)
		real_pthread_setspecific(kd[i].kd_key, NULL);
	kd_unlock();
}

int
__rvpredict_pthread_key_create(pthread_key_t *keyp, void (*destructor)(void *))
{
	int rc;
	kd_t *nkd;

	kd_lock();
	rc = real_pthread_key_create(keyp, rvp_destructor);
	if (rc != 0)
		goto out;

	if ((nkd = realloc(kd, (kdcount + 1) * sizeof(kd[0]))) == NULL) {
		rc = ENOMEM;
		goto out;
	}
	nkd[kdcount] = (kd_t){.kd_key = *keyp, .kd_destructor = destructor};
	kd = nkd;
	kdcount++;
out:
	kd_unlock();
	return rc;
}

int
__rvpredict_pthread_key_delete(pthread_key_t key)
{
	int i, rc;
	kd_lock();
	rc = real_pthread_key_delete(key);
	if (rc != 0)
		goto out;
	/* Search for matching key. */
	for (i = 0; i < kdcount; i++) {
		if (kd[i].kd_key == key)
			break;
	}
	/* If found, then swap with last.  Free last. */
	if (i != kdcount) {
		kd[i] = kd[--kdcount];
		kd = realloc(kd, kdcount * sizeof(kd[0]));
	}
out:
	kd_unlock();
	return rc;
}

int
__rvpredict_pthread_setspecific(pthread_key_t key, const void *value)
{
	int i, j, rc;
	rvp_thread_t *t = rvp_thread_for_curthr();

	kd_lock();

	if ((rc = real_pthread_setspecific(key, value)) != 0)
		goto out;

	/* Search for matching key. */
	for (i = 0; i < kdcount; i++) {
		if (kd[i].kd_key == key)
			break;
	}

	/* If not found, then bail. */
	if (i == kdcount)
		goto out;

	/* Modify this thread's copy.  First, look for an existing one. */
	for (j = 0; j < t->t_nthrspecs; j++) {
		if (t->t_thrspec[j].ts_key == key)
			break;
	}

	/* setting NULL, not found -> nothing to do
	 * setting non-NULL, found -> replace
	 * setting NULL, found -> free a slot
	 * setting non-NULL, not found -> allocate a slot
	 */
	if (j == t->t_nthrspecs && value == NULL) {
		;
	} else if (j < t->t_nthrspecs && value != NULL) {
		t->t_thrspec[j].ts_value = value;
	} else if (j < t->t_nthrspecs && value == NULL) {
		t->t_thrspec[j] = t->t_thrspec[--t->t_nthrspecs];
		t->t_thrspec = realloc(t->t_thrspec,
		    t->t_nthrspecs * sizeof(t->t_thrspec[0]));
	} else if (j == t->t_nthrspecs && value != NULL) {
		rvp_thrspec_t *nthrspec;

		nthrspec = realloc(t->t_thrspec,
		            (t->t_nthrspecs + 1) * sizeof(nthrspec[0]));
		if (nthrspec == NULL) {
			rc = ENOMEM;
			goto out;
		}
		t->t_thrspec = nthrspec;
		t->t_nthrspecs++;
		t->t_thrspec[j].ts_key = key;
		t->t_thrspec[j].ts_value = value;
	}
out:
	kd_unlock();
	return rc;
}

INTERPOSE(int, pthread_key_create, pthread_key_t *, void (*)(void *));
INTERPOSE(int, pthread_key_delete, pthread_key_t);
INTERPOSE(int, pthread_setspecific, pthread_key_t, const void *);

REAL_DEFN(int, pthread_key_create, pthread_key_t *, void (*)(void *));
REAL_DEFN(int, pthread_key_delete, pthread_key_t);
REAL_DEFN(int, pthread_setspecific, pthread_key_t, const void *);
