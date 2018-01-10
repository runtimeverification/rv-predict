SUBDIR=rvpa rvsyms trace

MKINSTALL=no

depend all: share/rv-predict-c/rv-predict.jar share/rv-predict-c/ldscript bin/rvpc lib/rvpinstrument.so lib/rvprt.a

#
# The following rules set LIBDIR to lib for compatibility
# with dpkg-buildpackage, which overrides LIBDIR
# (e.g., LIBDIR=/usr/lib/x86_64-linux-gnu) in a way that
# $(.OBJDIR)/bin/rvpc will not find rvpinstrument.so.
#
share/rv-predict-c/rv-predict.jar:
	$(MAKE) DESTDIR=$(.OBJDIR) PREFIX= LIBDIR=/lib -C $(.CURDIR)/../maven install

share/rv-predict-c/ldscript:
	$(MAKE) DESTDIR=$(.OBJDIR) PREFIX= LIBDIR=/lib -C $(.CURDIR)/../ldscript install

bin/rvpc:
	$(MAKE) DESTDIR=$(.OBJDIR) PREFIX= LIBDIR=/lib -C $(.CURDIR)/../scripts install

lib/rvpinstrument.so:
	$(MAKE) DESTDIR=$(.OBJDIR) PREFIX= LIBDIR=/lib -C $(.CURDIR)/../llvm/cmade install

lib/rvprt.a:
	$(MAKE) DESTDIR=$(.OBJDIR) PREFIX= LIBDIR=/lib -C $(.CURDIR)/../llvm/ngrt install

CLEANDIRS+=bin include lib man share

.include <mkc.subdir.mk>
