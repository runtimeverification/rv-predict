SUBDIR=rvpa rvsyms trace

MKINSTALL=no

depend all: share/rv-predict-c/rv-predict.jar share/rv-predict-c/ldscript bin/rvpc lib/rvpinstrument.so lib/rvprt.a

share/rv-predict-c/rv-predict.jar:
	$(MAKE) DESTDIR=$(.OBJDIR) PREFIX= -C $(.CURDIR)/../maven install

share/rv-predict-c/ldscript:
	$(MAKE) DESTDIR=$(.OBJDIR) PREFIX= -C $(.CURDIR)/../ldscript install

bin/rvpc:
	$(MAKE) DESTDIR=$(.OBJDIR) PREFIX= -C $(.CURDIR)/../scripts install

lib/rvpinstrument.so:
	$(MAKE) DESTDIR=$(.OBJDIR) PREFIX= -C $(.CURDIR)/../llvm/cmade install

lib/rvprt.a:
	$(MAKE) DESTDIR=$(.OBJDIR) PREFIX= -C $(.CURDIR)/../llvm/ngrt install

CLEANDIRS+=bin include lib man share

.include <mkc.subdir.mk>
