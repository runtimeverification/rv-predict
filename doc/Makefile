FILES=C-README.md manual.md
FILESNAME_C-README.md=README.md
FILESNAME_manual=USERS-MANUAL.md
FILESDIR=$(DATADIR)/doc/rv-predict-c/
FILESMODE=0644

manual.rst: manual.md
	@pandoc -f markdown -t rst --output=$(.OBJDIR)/$(.TARGET) $(.ALLSRC)

.include <mkc.files.mk>
