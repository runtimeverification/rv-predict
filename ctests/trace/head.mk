#
#   This is the include file for the 1st portion of the test Makefiles. 
#  (See tail.mk for last portion of the Makefile)
#
#  The makefile uses mk-configure macro files
# (https://github.com/cheusov/mk-configure) with BSD make.  On a
# POSIX-compliant platforms like Linux, Mac OS X, or *BSD, I recommend
# installing bmake and mk-configure from pkgsrc.org. bmake is in the
# pkgsrc bootstrap kit. The package for mk-configure is in devel/.
#

NOMAN=

PATH:=$(.OBJDIR)/../../toolset/bin:$(PATH)
RVP_TRACE_FILE:=$(.OBJDIR)/test.trace 
RVP_TRACE_ONLY:=yes

CTEST_dir=$(.CURDIR)/../..

.export PATH
.export RVP_TRACE_FILE
.export RVP_TRACE_ONLY

# Code coverage
.if $(MKCOVERAGE:U"no")=="yes"
#      Generates default.profraw when program runs
CFLAGS+=-fprofile-instr-generate -fcoverage-mapping 
#LDFLAGS+=-fprofile-instr-generate -fcoverage-mapping
LDFLAGS+=-fprofile-instr-generate
.endif

# Code coverage

.if $(MKCOVERAGE1:U"no")=="yes"
#       each .c gets a .gcno at compile time and .gcda at execution
#       --coverage includes -fprofile-arcs -ftest-coverage
#       -g includes debug info (as per https://llvm.org/docs/CommandGuide/llvm-cov.html )
#CFLAGS+=--coverage -g
CFLAGS+=--coverage -fprofile-instr-generate
#    I don't know what LDFLAGS should be - this is overkill, probably
#LDFLAGS+=--coverage -fprofile-instr-generate -fcoverage-mapping
#LDFLAGS+= -fprofile-instr-generate -fcoverage-mapping
#LDFLAGS+=--coverage -fprofile-instr-generate
LDFLAGS+=--coverage
.endif

#
