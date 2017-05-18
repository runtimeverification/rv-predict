CLEANABLE_FILES=checkJava.copy rv-predict.copy rv-predict.jar.copy
FILES=$(CLEANABLE_FILES)

FILESNAME_checkJava.copy=checkJava
FILESNAME_rv-predict.copy=rv-predict
FILESNAME_rv-predict.jar.copy=rv-predict.jar

FILESMODE_checkJava.copy=0755
FILESMODE_rv-predict.copy=0755
FILESMODE_rv-predict.jar.copy=0644

FILESDIR_checkJava.copy=$(BINDIR)
FILESDIR_rv-predict.copy=$(BINDIR)
FILESDIR_rv-predict.jar.copy=$(LIBDIR)

buildit: .PHONY
	cd $(.CURDIR)/.. && mvn -q -DskipTests -DskipDocs package

copyit: .USE
	@$(INSTALL) -c -m 0644 $(.ALLSRC:Ncopyit) $(.OBJDIR)/$(.TARGET)

checkJava.copy: $(.CURDIR)/../target/release/rv-predict/bin/checkJava copyit
rv-predict.copy: $(.CURDIR)/../target/release/rv-predict/bin/rv-predict copyit
rv-predict.jar.copy: $(.CURDIR)/../target/release/rv-predict/rv-predict.jar copyit

$(.CURDIR)/../target/release/rv-predict/bin/checkJava: buildit

$(.CURDIR)/../target/release/rv-predict/bin/rv-predict: buildit

$(.CURDIR)/../target/release/rv-predict/rv-predict.jar: buildit

#clean:
#	@rm -rf $(.CURDIR)/../target

CLEANDIRS+=$(.CURDIR)/../target
CLEANFILES+=$(CLEANABLE_FILES)

.include <mkc.files.mk>
