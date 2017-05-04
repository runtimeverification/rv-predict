SUBDIR=trace

MKINSTALL=no

all: bin/rvpc lib/rvpinstrument.so lib/rvprt.a

bin/rvpc:
	$(MAKE) DESTDIR=$(.OBJDIR) PREFIX= -C $(.CURDIR)/../rvpdump install

lib/rvpinstrument.so:
	$(MAKE) DESTDIR=$(.OBJDIR) PREFIX= -C $(.CURDIR)/../cmade install

lib/rvprt.a:
	$(MAKE) DESTDIR=$(.OBJDIR) PREFIX= -C $(.CURDIR)/../ngrt install

CLEANDIRS+=bin include lib

.include <mkc.subdir.mk>
