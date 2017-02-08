#include <signal.h>
#include <stdint.h>	/* for uint32_t */

#include "init.h"
#include "interpose.h"
#include "rvpsignal.h"
#include "sigutil.h"
#include "trace.h"

REAL_DEFN(int, sigaction, int, const struct sigaction *, struct sigaction *);

static rvp_signal_t * _Atomic signal_tbl = NULL;
static int nsignals = 0;

static void
rvp_signal_table_init(void)
{
	sigset_t ss;
	rvp_signal_t *tbl;
	int lastsig, ninvalid = 0;

	sigfillset(&ss);
	for (lastsig = 0; ninvalid < 2; lastsig++) { 
		if (sigismember(&ss, lastsig) == -1)
			ninvalid++;
	}
	if ((tbl = calloc(sizeof(*signal_tbl), lastsig)) == NULL)
		err(EXIT_FAILURE, "%s: calloc", __func__);
	atomic_store(&signal_tbl, tbl);
	nsignals = lastsig;
}

void
rvp_signal_init(void)
{
	ESTABLISH_PTR_TO_REAL(sigaction);
	rvp_signal_table_init();
}

rvp_signal_t *
rvp_signal_lookup(int signum)
{
	return &atomic_load(&signal_tbl)[signum];
}

rvp_ring_t *
rvp_signal_ring_get(rvp_thread_t *t)
{
	return &t->t_ring;
}

static void
handler_wrapper(int signum, siginfo_t *info, void *ctx)
{
	/* XXX rvp_thread_for_curthr() calls pthread_getspecific(), which
	 * is not guaranteed to be async signal-safe.  However, it is
	 * known to be safe on Linux, and it is probably safe on many other
	 * operating systems.
	 */
	rvp_thread_t *t = rvp_thread_for_curthr();
	rvp_signal_t *s = rvp_signal_lookup(signum);
	rvp_ring_t *r = rvp_signal_ring_get(t);
	rvp_buf_t b = RVP_BUF_INITIALIZER;

	/* `r` may have been used by some other thread, so
	 * record a switch to the current thread.
	 *
	 * Then, record a switch to the signal/interrupt context.
	 */
	rvp_buf_put_addr(&b, rvp_vec_and_op_to_deltop(0, RVP_OP_SWITCH));
	rvp_buf_put(&b, t->t_id);
	rvp_buf_put_addr(&b, rvp_vec_and_op_to_deltop(0, RVP_OP_ENTERSIG));
	rvp_buf_put(&b, signum);	
	rvp_ring_put_buf(r, b);

	if (s->s_handler != NULL)
		(*s->s_handler)(signum);
	else
		(*s->s_sigaction)(signum, info, ctx);

	b = RVP_BUF_INITIALIZER;

	rvp_buf_put_addr(&b, rvp_vec_and_op_to_deltop(0, RVP_OP_EXITSIG));
	rvp_ring_put_buf(r, b);
}

int
__rvpredict_sigaction(int signum, const struct sigaction *act,
    struct sigaction *oact)
{
	rvp_signal_t *s;
	sigset_t mask;
	struct sigaction act_copy = *act;

	if (act == NULL)
		return real_sigaction(signum, act, oact);

	if ((s = rvp_signal_lookup(signum)) == NULL)
		err(EXIT_FAILURE, "%s: malloc", __func__);

	if ((act->sa_flags & SA_SIGINFO) != 0) {
		s->s_sigaction = act->sa_sigaction;
	} else {
		void (*handler)(int);

		handler = s->s_handler = act->sa_handler;
		if (handler == SIG_IGN || handler == SIG_DFL) {
			/* TBD trace signal-handler disestablishment */
			return real_sigaction(signum, act, oact);
		}
	}

	mask = act->sa_mask;

	// add signum to the set unless this signal can preempt itself 
	if ((act->sa_flags & SA_NODEFER) == 0)
		sigaddset(&mask, signum);
	
	s->s_blockset = intern_sigset(&mask);

	// TBD let transmitter know to emit new blockset: should check
	// latest nsigblocksets and if it increased, emit latest before
	// new traces

	act_copy = *act;
	act_copy.sa_flags |= SA_SIGINFO;
	act_copy.sa_sigaction = handler_wrapper;

	return real_sigaction(signum, &act_copy, oact);
}

INTERPOSE(int, sigaction, int signum, const struct sigaction *,
    struct sigaction *);
