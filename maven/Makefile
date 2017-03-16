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

SRC_PATHADD+=$(.CURDIR)/../target/release/rv-predict
SRC_PATHADD+=$(.CURDIR)/../target/release/rv-predict/bin

installit: .USE
	@$(INSTALL) -c -m 0644 $(.ALLSRC:Ninstallit) $(.OBJDIR)/$(.TARGET)

checkJava.copy: checkJava installit
rv-predict.copy: rv-predict installit
rv-predict.jar.copy: rv-predict.jar installit

target:
	@rm -rf $(.CURDIR)/../target
	@cd $(.CURDIR)/.. && mvn -q -DskipTests -DskipDocs package

checkJava rv-predict rv-predict.jar! target

CLEANFILES+=$(CLEANABLE_FILES)

.include <mkc.files.mk>
