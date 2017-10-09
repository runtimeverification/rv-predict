SUBDIR=trace

MKINSTALL=no

depend all: share/rv-predict-c/ldscript bin/rvpc lib/rvpinstrument.so lib/rvprt.a

share/rv-predict-c/ldscript:
	$(MAKE) DESTDIR=$(.OBJDIR) PREFIX= -C $(.CURDIR)/../../ldscript install

bin/rvpc:
	$(MAKE) DESTDIR=$(.OBJDIR) PREFIX= -C $(.CURDIR)/../../scripts install

lib/rvpinstrument.so:
	$(MAKE) DESTDIR=$(.OBJDIR) PREFIX= -C $(.CURDIR)/../cmade install

lib/rvprt.a:
	$(MAKE) DESTDIR=$(.OBJDIR) PREFIX= -C $(.CURDIR)/../ngrt install

CLEANDIRS+=bin include lib share

.include <mkc.subdir.mk>
