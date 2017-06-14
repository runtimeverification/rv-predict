##
## Copyright (c) 1997 X/Open Company Ltd., A member of The Open Group.
##
## All rights reserved. No part of this source code may be reproduced,
## stored in a retrieval system, or transmitted, in any form or by any
## means, electronic, mechanical, photocopying, recording or otherwise,
## except as stated in the end-user licence agreement, without the prior
## permission of the copyright owners.
##
##
## A Semi portable makefile. 

CFLAGS= 

# SGS Definitions

# The C compiler name
# CC = cc

# GNU
# CC = gcc

# win32, using the MS C compiler, switch off the copyright notice
CC = cl -nologo

# object filenames 
# O = .o

# win32 systems use .obj
O = .obj

# exe filenames
# For UNIX systems this should have no value
#E = 

# win32 systems use .exe
E = .exe

RM = rm -f


#### no changes should be needed below this point

all:	rpt$E rptm$E

rpt$E:	rpt.c getopt$O
	$(CC) $(CFLAGS) -c rpt.c
	$(CC) $(CFLAGS) -o rpt rpt$O getopt$O

rptm$E:	rptm.c getopt$O
	$(CC) $(CFLAGS) -c rptm.c
	$(CC) $(CFLAGS) -o rptm rptm$O getopt$O

getopt$O:	getopt.c
	$(CC) $(CFLAGS) -c getopt.c

install: rpt$E rptm$E
	cp rptm$E $(TET_ROOT)/bin/
	cp rpt$E $(TET_ROOT)/bin/

clean:
	$(RM) *$O rpt$E rptm$E

clobber: clean
	$(RM) $(TET_ROOT)/bin/rptm$E $(TET_ROOT)/bin/rpt$E

