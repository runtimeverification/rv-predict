FILESMODE=0755
FILES=rvperror rvpsymbolize-json
CLEANFILES_CMD=rm -f error_t.ml error_t.mli error_j.ml error_j.mli *.cmx *.cmi *.o rvpsymbolize-json
.PATH: $(RV_ERROR_DIR)

rvpsymbolize-json: error_t.mli error_t.ml error_j.mli error_j.ml $(.CURDIR)/rvpsymbolize_json.ml
	ocamlfind opt -I $(RV_ERROR_DIR) $(.ALLSRC) -o $(.OBJDIR)/rvpsymbolize-json -package atdgen -linkpkg -package unix

error_t.ml error_t.mli: $(RV_ERROR_DIR)/error.atd
	atdgen -t $(.ALLSRC) -o $(.OBJDIR)/error

error_j.ml error_j.mli: $(RV_ERROR_DIR)/error.atd
	atdgen -j -j-std $(.ALLSRC) -o $(.OBJDIR)/error

.include <mkc.prog.mk>
