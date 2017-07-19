FILESMODE=0755
FILES=rvperror rvpsymbolize-json rvptrimframe
CLEANFILES_CMD=rm -f error_t.ml error_t.mli error_j.ml error_j.mli *.cmx *.cmi *.o rvpsymbolize-json rvptrimframe rv-error.atd
.PATH: $(RV_ERROR_DIR)
rvpsymbolize-json: error_t.mli error_t.ml error_j.mli error_j.ml $(.CURDIR)/rvpsymbolize_json.ml
	ocamlfind opt -g $(.ALLSRC) -o $(.OBJDIR)/rvpsymbolize-json -package atdgen -linkpkg -package unix
rvptrimframe: error_t.mli error_t.ml error_j.mli error_j.ml $(.CURDIR)/rvptrimframe.ml
	ocamlfind opt -g $(.ALLSRC) -o $(.OBJDIR)/rvptrimframe -package atdgen -linkpkg -package unix

error_t.ml error_t.mli: rv-error.atd
	atdgen -t $(.ALLSRC) -o $(.OBJDIR)/error

error_j.ml error_j.mli:rv-error.atd
	atdgen -j -j-std $(.ALLSRC) -o $(.OBJDIR)/error

rv-error.atd:
	cd .. && mvn -q org.apache.maven.plugins:maven-dependency-plugin:3.0.1:get -Dartifact=com.runtimeverification.rv_match:rv-error:1.0-SNAPSHOT:atd:atd-spec
	cd .. && mvn -q org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -Dartifact=com.runtimeverification.rv_match:rv-error:1.0-SNAPSHOT:atd:atd-spec -DoutputDirectory=$(.OBJDIR) -Dmdep.stripVersion -Dmdep.stripClassifier=true

.include <mkc.prog.mk>
