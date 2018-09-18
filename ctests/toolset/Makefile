FILES=xyz

.info LIBDIR=$(LIBDIR)

.export DESTDIR=$(.OBJDIR)
.export LIBDIR=/lib
.export PREFIX=

CLEANDIRS+=bin include lib libexec man share usr

.include <mkc.files.mk>

MAKEFLAGS+=	LIBDIR=/lib
MAKEFLAGS+=	DESTDIR=$(.OBJDIR)
MAKEFLAGS+=	PREFIX=

$(TARGETS:Nclean:Ncleandir:Ntags):
.for __t in all install
	$(MAKE) MAKEFLAGS='$(MAKEFLAGS)' ONLY_TEST_DEPENDENCIES=yes \
	    -C $(.CURDIR)/../.. ${__t}
.endfor
