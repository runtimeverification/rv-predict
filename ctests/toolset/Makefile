FILES=xyz

.export-env DESTDIR=$(.OBJDIR)
.export-env LIBDIR=/lib
.export-env PREFIX=

#__SRCTOP=$(.CURDIR)/../..
#CROSS_SRCTOP=$(__SRCTOP:tA)
#CROSS_OBJTOP=$(.OBJDIR)

CLEANDIRS+=bin include lib libexec man share usr

.include <mkc.files.mk>

#MAKEFLAGS+=	LIBDIR=/lib
#MAKEFLAGS+=	DESTDIR=$(.OBJDIR)
#MAKEFLAGS+=	PREFIX=

#.error MAKEFLAGS=$(MAKEFLAGS)
cleandir clean obj:
#	TOOLS_SRCTOP=$(TOOLS_SRCTOP) TOOLS_OBJTOP=$(TOOLS_OBJTOP) MAKEOBJDIR='$$(.CURDIR:C,$(TOOLS_SRCTOP),$(TOOLS_OBJTOP),)'
	DESTDIR=$(.OBJDIR) LIBDIR=/lib PREFIX= $(MAKE) ONLY_TEST_DEPENDENCIES=yes \
	    -C $(.CURDIR)/../.. $(.TARGETS)

tags:

$(TARGETS:Nclean:Ncleandir:Nobj:Ntags):
.for __t in all install
#	TOOLS_SRCTOP=$(TOOLS_SRCTOP) TOOLS_OBJTOP=$(TOOLS_OBJTOP) MAKEOBJDIR='$$(.CURDIR:C,$(TOOLS_SRCTOP),$(TOOLS_OBJTOP),)'
	DESTDIR=$(.OBJDIR) LIBDIR=/lib PREFIX= $(MAKE) ONLY_TEST_DEPENDENCIES=yes \
	    -C $(.CURDIR)/../.. ${__t}
.endfor
