CLEANABLE_FILES=rv-predict.jar.copy
FILES=$(CLEANABLE_FILES)

FILESNAME_rv-predict.jar.copy=rv-predict.jar

FILESMODE_rv-predict.jar.copy=0644

FILESDIR_rv-predict.jar.copy=$(LIBDIR)

buildit: .PHONY
.if $(RELEASE:Uno) == "no"
	@cd $(.CURDIR)/../native-z3 && mvn -q -DskipTests -DskipDocs install
	@cd $(.CURDIR)/../rv-predict-source && mvn -q -DskipTests -DskipDocs compile
	@[ -d $(.CURDIR)/../target/release/rv-predict ] || mkdir -p $(.CURDIR)/../target/release/rv-predict
	@cd $(.CURDIR)/../rv-predict-source/target/classes && jar cf $(.CURDIR)/../target/release/rv-predict/rv-predict.jar .
.else
	# I used to pass -DskipTests -DskipDocs here.
	cd $(.CURDIR)/.. && mvn -Dobfuscate -U package
.endif

rv-predict.jar.copy: $(.CURDIR)/../target/release/rv-predict/rv-predict.jar
	@$(INSTALL) -c -m 0644 $(.ALLSRC:Ncopyit) $(.OBJDIR)/$(.TARGET)
.if $(RELEASE:Uno) == "no"
	@$(.CURDIR)/add-metainfo.sh $(.OBJDIR) $(.TARGET)
.endif

$(.CURDIR)/../target/release/rv-predict/rv-predict.jar: buildit

clean cleandir distclean:
	@cd $(.CURDIR)/.. && mvn -q clean

#CLEANDIRS+=$(.CURDIR)/../target $(.CURDIR)/../rv-predict-source/target
CLEANFILES+=$(CLEANABLE_FILES)

.include <mkc.files.mk>
