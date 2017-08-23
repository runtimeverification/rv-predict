FILESMODE=0755
FILES=rvpsymbolize-json rvptrimframe
CLEANFILES_CMD=rm -f *.cmx *.cmi *.o rvpsymbolize-json rvptrimframe
.PATH: $(RV_ERROR_DIR)
rvpsymbolize-json: $(.CURDIR)/rvpsymbolize_json.ml
	ocamlfind opt -g $(.ALLSRC) -o $(.OBJDIR)/rvpsymbolize-json -package atdgen -package rv_error -linkpkg -package unix
rvptrimframe: $(.CURDIR)/rvptrimframe.ml
	ocamlfind opt -g $(.ALLSRC) -o $(.OBJDIR)/rvptrimframe -package atdgen -package rv_error -linkpkg -package unix

.include <mkc.prog.mk>
