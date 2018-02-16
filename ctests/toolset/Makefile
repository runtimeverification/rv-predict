.include <mkc.init.mk>

FILES=xyz

#
# The following rules set LIBDIR to lib for compatibility
# with dpkg-buildpackage, which overrides LIBDIR
# (e.g., LIBDIR=/usr/lib/x86_64-linux-gnu) in a way that
# $(.OBJDIR)/bin/rvpc will not find rvpinstrument.so.
#
$(TARGETS:Nclean:Ncleandir):
	$(MAKE) ONLY_TEST_DEPENDENCIES=yes DESTDIR=$(.OBJDIR) PREFIX= \
	    LIBDIR=/lib -C $(.CURDIR)/../.. all install

clean cleandir:
	$(MAKE) ONLY_TEST_DEPENDENCIES=yes DESTDIR=$(.OBJDIR) PREFIX= \
	    LIBDIR=/lib -C $(.CURDIR)/../.. uninstall


CLEANDIRS+=bin include lib libexec man share

.include <mkc.files.mk>
