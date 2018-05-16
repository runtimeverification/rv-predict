# Interrupt data-race demonstration

Run `mkcmake` to build the example programs.

Each example program, `1` - `6`, corresponds to one of the data-race
categories, below.

Example programs `p1` - `p3` use a shared atomic `bool` to protect
against races; otherwise they are identical to `1` - `3`.

# Interrupt data-race categories

## Category 1

A low-priority thread is interrupted while it loads a shared memory
location, L; the interrupt handler performs a store that overlaps L;
the thread potentially reads a value that is different both from the
previous value and from the value the interrupt handler wrote.

## Category 2

A low-priority thread is interrupted during a store on a shared memory
location, L; the interrupt handler performs a load that overlaps L; the
interrupt handler potentially observes a value that is different both from
the original value and from the value the low-priority thread was storing.

## Category 3

A low-priority thread is interrupted during a store on a shared memory
location, L; an interrupt handler performs a store that overlaps L;
the shared memory region potentially takes a value that is different
than either the thread or the interrupt handler stored.

## Category 4

A low-priority thread is interrupted while it loads a shared bit, L;
the interrupt handler performs a store to a nearby second bit, M; the
bit accesses compile to wider accesses (e.g., byte or word accesses)
that overlap.  The thread potentially reads a value that is different both
from the previous value and from the value the interrupt handler wrote.

Predict does not yet have a model for single-bit accesses.  When a program
performs unsynchronized accesses to bits belonging to the same byte,
Predict may report a data race.

## Category 5

A low-priority thread is interrupted while it loads a shared array
element, L; the interrupt handler performs a store to a nearby second
array element, M; the array accesses compile to wider accesses (e.g.,
byte or word accesses) that overlap.  The thread potentially reads a
value that is different both from the previous value and from the value
the interrupt handler wrote.

Predict does not yet have a model for accesses that are widened by the
compiler or by the target CPU, so it does not make a report on Category-5
data races.

## Category 6

A low-priority thread is interrupted while it loads a shared structure
member, L; the interrupt handler performs a store to a nearby second
structure member, M; the structure accesses compile to wider accesses
(e.g., byte or word accesses) that overlap.  The thread potentially
reads a value that is different both from the previous value and from
the value the interrupt handler wrote.

Predict does not yet have a model for accesses that are widened by the
compiler or by the target CPU, so it does not make a report on Category-5
data races.
