# Interrupt data-race categories

## Category 1

A low-priority thread is interrupted while it loads a shared memory
location, L; the interrupt handler performs a store that overlaps L;
the thread potentially reads a value that is different both
from the previous value and from the value the interrupt handler wrote.

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
