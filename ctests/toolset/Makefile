FILES=xyz

.info LIBDIR=$(LIBDIR)
LIBDIR=/lib
.export LIBDIR

CLEANDIRS+=bin include lib libexec man share usr

.include <mkc.files.mk>

$(TARGETS:Nclean:Ncleandir:Ntags):
.for __t in all install
	$(MAKE) MAKEFLAGS='$(MAKEFLAGS)' ONLY_TEST_DEPENDENCIES=yes DESTDIR=$(.OBJDIR) PREFIX= \
	    -C $(.CURDIR)/../.. ${__t}
.endfor
