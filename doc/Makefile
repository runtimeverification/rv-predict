FILES=manual.md 3RDPARTY.md
FILESNAME_manual.md=USERS-MANUAL.md
FILESDIR=$(DATADIR)/doc/rv-predict-c/
FILESMODE=0644

manual.html: manual.md
	@pandoc -f markdown -t html --output=$(.OBJDIR)/$(.TARGET) $(.ALLSRC)

manual.rst: manual.md
	@pandoc -f markdown -t rst --output=$(.OBJDIR)/$(.TARGET) $(.ALLSRC)

.include <mkc.files.mk>
