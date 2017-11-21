# Interrupt data-race categories

## Category 1

A low-priority thread is interrupted during a load; the interrupt
handler performs a store; the thread potentially reads a value that is
different both from the previous value and from the value the interrupt
handler wrote.

## Category 2

A low-priority thread is interrupted during a store; the interrupt handler
performs a load; the interrupt handler potentially reads a value that is
different both from the original value and from the value the low-priority
thread was writing.

## Category 3

A low-priority thread is interrupted during a store; an interrupt handler
performs a store; the shared variable potentially takes a value that is
different than either the thread or the interrupt handler stored.
