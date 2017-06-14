# SCCS: @(#)win32.sed	1.1 (98/08/28) TETware release 3.8

/-START-UNIX-ONLY-/,/-END-UNIX-ONLY-/d

/^#[ 	]*-START-WIN32-ONLY-.*/d
/^#[ 	]*-END-WIN32-ONLY-.*/d

