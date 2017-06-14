# SCCS: @(#)unix.sed	1.1 (98/08/28) TETware release 3.8

/-START-WIN32-ONLY-/,/-END-WIN32-ONLY-/d

/^#[ 	]*-START-UNIX-ONLY-.*/d
/^#[ 	]*-END-UNIX-ONLY-.*/d

