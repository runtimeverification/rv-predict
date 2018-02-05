SUBDIR=rvpa rvsyms stats trace

MKINSTALL=no

BINDEPS=share/rv-predict-c/rv-predict.jar share/rv-predict-c/ldscript \
BINDEPS+=bin/rvpc bin/rv-error bin/rvpdump bin/rvpsymbolize-json bin/rvsyms \
BINDEPS+=lib/rvpinstrument.so lib/rvprt.a

depend all: $(BINDEPS)

#
# The following rules set LIBDIR to lib for compatibility
# with dpkg-buildpackage, which overrides LIBDIR
# (e.g., LIBDIR=/usr/lib/x86_64-linux-gnu) in a way that
# $(.OBJDIR)/bin/rvpc will not find rvpinstrument.so.
#
$(BINDEPS):
	$(MAKE) ONLY_TEST_DEPENDENCIES=yes DESTDIR=$(.OBJDIR) PREFIX= \
	    LIBDIR=/lib -C $(.CURDIR)/.. install

CLEANDIRS+=bin include lib libexec man share

.include <mkc.subdir.mk>
