FILES=copyright.copy manual.md 3RDPARTY.md
FILESNAME_copyright.copy=copyright
FILESNAME_manual.md=USERS-MANUAL.md
FILESDIR=$(DATADIR)/doc/rv-predict-c
FILESMODE=0644
CLEANFILES+=manual.html manual.rst

copyright.copy:
	@cp $(.CURDIR)/../debian/copyright $(.OBJDIR)/$(.TARGET)

manual.html: manual.md
	@pandoc -f markdown -t html --output=$(.OBJDIR)/$(.TARGET) $(.ALLSRC)

manual.rst: manual.md
	@pandoc -f markdown -t rst --output=$(.OBJDIR)/$(.TARGET) $(.ALLSRC)

.include <mkc.files.mk>
