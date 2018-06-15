#include <assert.h>
#include <inttypes.h>
#include <signal.h>
#include <stdarg.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>

#include "intr.h"
#include "private_intr_tiaic.h"
#include "register.h"
#include "rvpredict_intr_tiaic.h"
#include "rvpsignal.h"

static bool tiaic_debug = false;

uint32_t tiaic_reg[] = {
	  [TIAIC_REVID] = TIAIC_REVID_REV_DFLT
	, [TIAIC_GNLR] = TIAIC_GNLR_NESTLVL_DFLT
	, [TIAIC_GPIR] = TIAIC_GPIR_NONE_DFLT
	, [TIAIC_HIPIR1] = TIAIC_HIPIR_NONE_DFLT
	, [TIAIC_HIPIR2] = TIAIC_HIPIR_NONE_DFLT
	, [TIAIC_HINLR1] = TIAIC_HINLR_NESTLVL_DFLT
	, [TIAIC_HINLR2] = TIAIC_HINLR_NESTLVL_DFLT
	, [TIAIC_HIPVR2] = 0			// reserve space for the last
						// register
};

static void
check_bounds(const int regidx)
{
	if (TIAIC_REVID <= regidx || regidx <= TIAIC_HIPVR2)
		return;

	raise(SIGBUS);
}

static void
check_unimpl(const int regidx)
{
	switch (regidx) {
	case TIAIC_VBR:
	case TIAIC_VSR:
	case TIAIC_VNR:
	case TIAIC_GPVR:
		raise(SIGBUS);
		return;
	default:
		break;
	}
}

static void
check_readonly(const int regidx, uint32_t v)
{
	uint32_t mask = __BITS(31, 0);

	switch (regidx) {
	case TIAIC_REVID:
	case TIAIC_GPIR:
	case TIAIC_HIPIR1:
	case TIAIC_HIPIR2:
	case TIAIC_HIPVR1:
	case TIAIC_HIPVR2:
		break;
	case TIAIC_CR:
		mask = TIAIC_CR_RSVD0 | TIAIC_CR_RSVD1;
		break;
	case TIAIC_GER:
		mask = TIAIC_GER_RSVD0;
		break;
	case TIAIC_GNLR:
		mask = TIAIC_GNLR_RSVD0;
		break;
	case TIAIC_SISR:
		mask = TIAIC_SISR_RSVD0;
		break;
	case TIAIC_SICR:
		mask = TIAIC_SICR_RSVD0;
		break;
	case TIAIC_EISR:
		mask = TIAIC_EISR_RSVD0;
		break;
	case TIAIC_EICR:
		mask = TIAIC_EICR_RSVD0;
		break;
	case TIAIC_HIEISR:
		mask = TIAIC_HIEISR_RSVD0;
		break;
	case TIAIC_HIEICR:
		mask = TIAIC_HIEICR_RSVD0;
		break;
	case TIAIC_SRSR4:
		mask = TIAIC_SRSR4_RSVD0;
		break;
	case TIAIC_SECR4:
		mask = TIAIC_SECR4_RSVD0;
		break;
	case TIAIC_ESR4:
		mask = TIAIC_ESR4_RSVD0;
		break;
	case TIAIC_ECR4:
		mask = TIAIC_ECR4_RSVD0;
		break;
	default:
		if (TIAIC_CMR(0) <= regidx && regidx <= TIAIC_CMR(25)) {
			mask = ~(uint32_t)(TIAIC_CMR_CHNL_MASK(0) |
			         TIAIC_CMR_CHNL_MASK(1) |
			         TIAIC_CMR_CHNL_MASK(2) |
				 TIAIC_CMR_CHNL_MASK(3));
			break;
		}
		return;
	}
	if ((v & mask) != 0)
		raise(SIGBUS);
}

uint32_t
tiaic_read(const int regidx)
{
	check_bounds(regidx);
	check_unimpl(regidx);

	return tiaic_reg[regidx];
}

static bool
irq_is_enabled(int irq)
{
	int bitno, regno;

	if (0 <= irq && irq <= 31) {
		regno = TIAIC_ESR1;
		bitno = irq;
	} else if (32 <= irq && irq <= 63) {
		regno = TIAIC_ESR2;
		bitno = irq - 32;
	} else if (64 <= irq && irq <= 95) {
		regno = TIAIC_ESR3;
		bitno = irq - 64;
	} else if (96 <= irq && irq <= 100) {
		regno = TIAIC_ESR4;
		bitno = irq - 96;
	} else
		abort();

	return tiaic_reg[regno] & __BIT(bitno);
}

int
irq_to_channel(int irq)
{
	const uint32_t cmr = tiaic_reg[TIAIC_CMR(irq / 4)],
	    mask = TIAIC_CMR_CHNL_MASK(irq % 4);

	return __SHIFTOUT(cmr, mask);
}

/* XXX You cannot really printf(3) in a signal handler.  This should use
 * some reentrant (signal-safe) snprintf(3) and write(2).
 */
static void __printflike(1, 2)
dbg_printf(const char *fmt, ...)
{
	va_list ap;

	if (!tiaic_debug)
		return;

	va_start(ap, fmt);
	(void)vprintf(fmt, ap);
	va_end(ap);
}

static void
__rvpredict_update_state(uint64_t prior_mask, bool fire_all)
{
	sigset_t omask, tmpmask;
	uint64_t next_mask = 0;
	uint32_t channels_enabled = 0;
	int i, j;

	/* 1 compute global mask
	 * 2 fire all making enabled -> disabled transition
	 * 3 update handlers' masks if any interrupts are enabled
	 * 4 set global mask
	 * 5 fire all making disabled -> enabled transition
	 */

	if ((tiaic_reg[TIAIC_GER] & TIAIC_GER_ENABLE) == 0)
		next_mask = tiaic_state.total_mask;
	else if ((tiaic_reg[TIAIC_HIER] &
	          (TIAIC_HIER_IRQ | TIAIC_HIER_FIQ)) == 0)
		next_mask = tiaic_state.total_mask;
	else if (tiaic_reg[TIAIC_ESR1] == 0 && tiaic_reg[TIAIC_ESR2] == 0 &&
	         tiaic_reg[TIAIC_ESR3] == 0 && tiaic_reg[TIAIC_ESR4] == 0)
		next_mask = tiaic_state.total_mask;

	assert((tiaic_reg[TIAIC_CR] & TIAIC_CR_NESTMODE_MASK) ==
	       TIAIC_CR_NESTMODE_INDIVIDUAL);

	assert((tiaic_reg[TIAIC_CR] & TIAIC_CR_PRHOLDMODE) != 0);

	if ((tiaic_reg[TIAIC_HIER] & TIAIC_HIER_FIQ) != 0) {
		uint32_t nest_level = __SHIFTOUT(tiaic_reg[TIAIC_HINLR1],
		    TIAIC_HINLR_NESTLVL);
		channels_enabled |= __BITS(1, 0) & ~__BITS(31, nest_level);
	}

	if ((tiaic_reg[TIAIC_HIER] & TIAIC_HIER_IRQ) != 0) {
		uint32_t nest_level = __SHIFTOUT(tiaic_reg[TIAIC_HINLR2],
		    TIAIC_HINLR_NESTLVL);
		channels_enabled |= __BITS(31, 2) & ~__BITS(31, nest_level);
	}

	for (i = 0; i < rvp_static_nassigned; i++) {
		const rvp_static_intr_t *osi = &rvp_static_intr[i];
		const int oirq = osi->si_prio;
		struct sigaction sa;

		if (osi->si_signum == -1)
			continue;

		const int ochannel = irq_to_channel(oirq);

		if ((channels_enabled & __BIT(ochannel)) == 0)
			next_mask |= __BIT(signo_to_bitno(osi->si_signum));

		if (!irq_is_enabled(oirq))
			next_mask |= __BIT(signo_to_bitno(osi->si_signum));

		uint64_t smask = 0;

		/* Each interrupt handler runs with all interrupts
		 * belonging to equal or greater channel # blocked.
		 */
		for (j = 0; j < rvp_static_nassigned; j++) {
			const rvp_static_intr_t *isi = &rvp_static_intr[j];
			const int iirq = isi->si_prio;

			if (isi->si_signum == -1)
				continue;

			const int ichannel = irq_to_channel(iirq);

			if (ichannel < ochannel)
				continue;

			smask |= __BIT(signo_to_bitno(isi->si_signum));
		}

		dbg_printf("sig '%d (interrupt %d) on channel %d "
		    "masks '%" PRIx64 "\n",
		    signo_to_bitno(osi->si_signum), oirq, ochannel, smask);

		memset(&sa, 0, sizeof(sa));
		mask_to_sigset(smask, &sa.sa_mask);
		sa.sa_handler = __rvpredict_tiaic_handler;

		if (sigaction(osi->si_signum, &sa, NULL) == -1)
			abort();
	}

	const uint64_t changed = next_mask ^ prior_mask,
	               enabled = changed & ~next_mask,
		       disabled = changed & next_mask;

	if (fire_all) {
		for (i = 0; i < 64; i++) {
			if ((disabled & __BIT(i) & tiaic_state.total_mask) != 0)
				raise(bitno_to_signo(i));
		}
	}

	if (pthread_sigmask(SIG_SETMASK, mask_to_sigset(prior_mask, &tmpmask),
	                    NULL) == -1)
		abort();
	if (pthread_sigmask(SIG_SETMASK, mask_to_sigset(next_mask, &tmpmask),
	                    NULL) == -1)
		abort();

	if (fire_all) {
		for (i = 0; i < 64; i++) {
			if ((enabled & __BIT(i) & tiaic_state.total_mask) != 0)
				raise(bitno_to_signo(i));
		}
	}
}

void
tiaic_write(const int regidx, uint32_t v)
{
	sigset_t omask, tmpmask;
	int idx, sidx;
	bool side_effects = false;
	uint32_t *reg = &tiaic_reg[regidx];

	if (pthread_sigmask(SIG_BLOCK, mask_to_sigset(tiaic_state.total_mask,
	                    &tmpmask), &omask) == -1)
		abort();

	check_bounds(regidx);
	check_readonly(regidx, v);
	check_unimpl(regidx);

	switch (regidx) {
	case TIAIC_SISR:
		idx = __SHIFTOUT(v, TIAIC_SISR_INDEX);
		if (0 <= idx && idx <= 31) {
			reg = &tiaic_reg[TIAIC_SRSR1];
			sidx = idx;
		} else if (32 <= idx && idx <= 63) {
			reg = &tiaic_reg[TIAIC_SRSR2];
			sidx = idx - 32;
		} else if (64 <= idx && idx <= 95) {
			reg = &tiaic_reg[TIAIC_SRSR3];
			sidx = idx - 64;
		} else if (96 <= idx && idx <= 100) {
			reg = &tiaic_reg[TIAIC_SRSR4];
			sidx = idx - 96;
		} else {
			raise(SIGBUS);
			goto out;
		}
		v = *reg | __BIT(sidx);
		break;
	case TIAIC_SICR:
		idx = __SHIFTOUT(v, TIAIC_SICR_INDEX);
		if (0 <= idx && idx <= 31) {
			reg = &tiaic_reg[TIAIC_SRSR1];
			sidx = idx;
		} else if (32 <= idx && idx <= 63) {
			reg = &tiaic_reg[TIAIC_SRSR2];
			sidx = idx - 32;
		} else if (64 <= idx && idx <= 95) {
			reg = &tiaic_reg[TIAIC_SRSR3];
			sidx = idx - 64;
		} else if (96 <= idx && idx <= 100) {
			reg = &tiaic_reg[TIAIC_SRSR4];
			sidx = idx - 96;
		} else {
			raise(SIGBUS);
			goto out;
		}
		v = *reg & ~__BIT(sidx);
		break;
	case TIAIC_EISR:
		idx = __SHIFTOUT(v, TIAIC_EISR_INDEX);
		if (0 <= idx && idx <= 31) {
			reg = &tiaic_reg[TIAIC_ESR1];
			sidx = idx;
		} else if (32 <= idx && idx <= 63) {
			reg = &tiaic_reg[TIAIC_ESR2];
			sidx = idx - 32;
		} else if (64 <= idx && idx <= 95) {
			reg = &tiaic_reg[TIAIC_ESR3];
			sidx = idx - 64;
		} else if (96 <= idx && idx <= 100) {
			reg = &tiaic_reg[TIAIC_ESR4];
			sidx = idx - 96;
		} else {
			raise(SIGBUS);
			goto out;
		}
		v = *reg | __BIT(sidx);
		break;
	case TIAIC_EICR:
		idx = __SHIFTOUT(v, TIAIC_EICR_INDEX);
		if (0 <= idx && idx <= 31) {
			reg = &tiaic_reg[TIAIC_ESR1];
			sidx = idx;
		} else if (32 <= idx && idx <= 63) {
			reg = &tiaic_reg[TIAIC_ESR2];
			sidx = idx - 32;
		} else if (64 <= idx && idx <= 95) {
			reg = &tiaic_reg[TIAIC_ESR3];
			sidx = idx - 64;
		} else if (96 <= idx && idx <= 100) {
			reg = &tiaic_reg[TIAIC_ESR4];
			sidx = idx - 96;
		} else {
			raise(SIGBUS);
			goto out;
		}
		v = *reg & ~__BIT(sidx);
		break;
	case TIAIC_HIEISR:
		idx = __SHIFTOUT(v, TIAIC_HIEISR_INDEX);
		reg = &tiaic_reg[TIAIC_HIER];
		v = *reg | ((idx == 0) ? TIAIC_HIER_FIQ : TIAIC_HIER_IRQ);
		break;
	case TIAIC_HIEICR:
		idx = __SHIFTOUT(v, TIAIC_HIEICR_INDEX);
		reg = &tiaic_reg[TIAIC_HIER];
		v = *reg & ~((idx == 0) ? TIAIC_HIER_FIQ : TIAIC_HIER_IRQ);
		break;
	case TIAIC_SRSR1:
	case TIAIC_SRSR2:
	case TIAIC_SRSR3:
	case TIAIC_SRSR4:
	case TIAIC_ESR1:
	case TIAIC_ESR2:
	case TIAIC_ESR3:
	case TIAIC_ESR4:
		v = *reg | v;
		break;
	case TIAIC_SECR1:
	case TIAIC_SECR2:
	case TIAIC_SECR3:
	case TIAIC_SECR4:
		reg = &tiaic_reg[regidx - TIAIC_SECR1 + TIAIC_SRSR1];
		v = *reg & ~v;
		break;
	case TIAIC_ECR1:
	case TIAIC_ECR2:
	case TIAIC_ECR3:
	case TIAIC_ECR4:
		reg = &tiaic_reg[regidx - TIAIC_ECR1 + TIAIC_ESR1];
		v = *reg & ~v;
		break;
	case TIAIC_CR:
	case TIAIC_GER:
	case TIAIC_GNLR:
	case TIAIC_HIER:
		break;
	default:
		if (regidx < TIAIC_CMR(0) || TIAIC_CMR(25) < regidx) {
			raise(SIGBUS);
			goto out;
		}
		break;
	}
	if (*reg != v) {
		*reg = v;
		__rvpredict_update_state(sigset_to_mask(&omask), true);
		return;
	}
out:
	pthread_sigmask(SIG_SETMASK, &omask, NULL);
}
