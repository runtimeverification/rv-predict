FILESMODE=0755
FILES=rv-error rvpsymbolize-json rvptrimframe rvpshortenpaths
CLEANFILES_CMD=rm -f *.cmx *.cmi *.o $(FILES)

rv-error: $(.CURDIR)/../errors/rv-error/src/main/ocaml/rv_error_native.ml
	ocamlfind opt -g $(.ALLSRC) -o $(.OBJDIR)/rv-error -package atdgen -package rv_error -linkpkg -package unix

rvpsymbolize-json: rvpsymbolize_json.ml
	ocamlfind opt -g $(.ALLSRC) -o $(.OBJDIR)/rvpsymbolize-json -package atdgen -package rv_error -linkpkg -package unix

rvptrimframe: rvptrimframe.ml
	ocamlfind opt -g $(.ALLSRC) -o $(.OBJDIR)/rvptrimframe -package atdgen -package rv_error -linkpkg -package unix

rvpshortenpaths: rvpshortenpaths.ml
	ocamlfind opt -g $(.ALLSRC) -o $(.OBJDIR)/rvpshortenpaths -package atdgen -package rv_error -linkpkg -package unix

.include <mkc.prog.mk>
