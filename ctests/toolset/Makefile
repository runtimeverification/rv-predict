FILES=xyz

$(TARGETS:Nclean:Ncleandir:Ntags):
.for __t in all install
	$(MAKE) ONLY_TEST_DEPENDENCIES=yes DESTDIR=$(.OBJDIR) PREFIX= \
	    LIBDIR=/lib \
	    -C $(.CURDIR)/../.. ${__t}
.endfor

CLEANDIRS+=bin include lib libexec man share

.include <mkc.files.mk>
