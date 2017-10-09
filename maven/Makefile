CLEANABLE_FILES=rv-predict.jar.copy
FILES=$(CLEANABLE_FILES)

FILESNAME_rv-predict.jar.copy=rv-predict.jar

FILESMODE_rv-predict.jar.copy=0644

FILESDIR_rv-predict.jar.copy=$(DATADIR)/rv-predict-c

.if ${NOISY:Uno} == "no"
MAVEN_OPTS=-q
.else
MAVEN_OPTS=
.endif

buildit: .PHONY
.if $(PREBUILT:Uno) == "yes"
	@echo rv-predict.jar is prebuilt.
.else
	cd $(.CURDIR)/.. && mvn -q -DonlyJar -DskipDocs -DskipTests -Dobfuscate -U $(MAVEN_OPTS) package
.endif

rv-predict.jar.copy: $(.CURDIR)/../target/release/rv-predict/rv-predict.jar
	@$(INSTALL) -c -m 0644 $(.ALLSRC:Ncopyit) $(.OBJDIR)/$(.TARGET)
.if $(PREBUILT:Uno) == "yes"
	@echo Installed prebuilt rv-predict.jar.
.endif

$(.CURDIR)/../target/release/rv-predict/rv-predict.jar: buildit

clean cleandir distclean:
	@cd $(.CURDIR)/.. && mvn -q clean

#CLEANDIRS+=$(.CURDIR)/../target $(.CURDIR)/../java/target
CLEANFILES+=$(CLEANABLE_FILES)

.include <mkc.files.mk>
