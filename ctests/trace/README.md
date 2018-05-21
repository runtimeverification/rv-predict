Under this directory are tests that compare expected RV-Predict/C traces
with actual traces.  Deviation from the expected traces may indicate
a regression.

rmw/: Tests of C11 atomic read-modify-write operations
	add/: Tests of C11 atomic add (32- and 64-bit versions so far)
	sub/: Tests of C11 atomic add (32- and 64-bit versions so far)
	xchg/: Tests of C11 atomic exchange (32- and 64-bit versions so far)

store/: Rudimentary tests of stores.

unaligned/: Rudimentary test of unaligned accesses.

memcpy/: Rudimentary test of memcpy.

mask/: Intended to test traces of getting/setting signal masks.  Unfinished.

atomic_store: 
atomic_load: Test of atomic load and stores

natest:	Test of nonatomic load and store

xchange_test:
xchange_t816:  Test read/modify/write

gnutest_sig: Tests signals using the gnu test program sigaction.c as base
