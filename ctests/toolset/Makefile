FILES=xyz

CLEANDIRS+=bin include lib libexec man share usr

.include <mkc.files.mk>

DESTDIR=$(.OBJDIR)
.undef PREFIX
PREFIX=

MAKEFLAGS+=	USE_PACKAGE_LIBDIR=no
MAKEFLAGS+=	DESTDIR=$(.OBJDIR)
MAKEFLAGS+=	PREFIX=

cleandir clean obj:
	$(MAKE) $(MAKEFLAGS) ONLY_TEST_DEPENDENCIES=yes \
	    -C $(.CURDIR)/../.. $(.TARGETS)

tags:

$(TARGETS:Nclean:Ncleandir:Nobj:Ntags):
.for __t in all install
	$(MAKE) $(MAKEFLAGS) ONLY_TEST_DEPENDENCIES=yes \
	    -C $(.CURDIR)/../.. ${__t}
.endfor
