OCB := ocamlbuild -use-ocamlfind -plugin-tag "package(ocamlbuild_atdgen)"
NAME := rv_error
NATIVE  = $(NAME)_native.native
ARCHIVE  = $(NAME).cma
LIBRARY  = $(NAME).a
XARCHIVE = $(NAME).cmxa
SARCHIVE = $(NAME).cmxs


all: resources
	$(OCB) $(LIBRARY)
	$(OCB) $(ARCHIVE)
	$(OCB) $(XARCHIVE)


clean: resources_clean
	$(OCB) -clean


install: all uninstall
	ocamlfind install $(NAME) _build/error_t.mli _build/error_j.mli _build/error_t.cmi _build/error_t.cmx _build/error_j.cmi _build/error_j.cmx _build/rv_error.mli _build/rv_error.cmi _build/rv_error.cmx _build/$(ARCHIVE) _build/$(LIBRARY) META \
             -optional _build/$(XARCHIVE) _build/$(SARCHIVE) error.atd

uninstall:
	ocamlfind remove $(NAME)


native: install
	ocamlfind opt -linkpkg -package $(NAME) -o ../../../bin/rv-error $(NAME)_native.ml
	rm -f rv_error_native.cmi rv_error_native.cmx rv_error_native.o


resources: $(shell find ../resources -type f)
	ocp-ocamlres ../resources -o rv_error_res.ml


resources_clean:
	rm -f rv_error_res.ml


.PHONY: all install uninstall resources clean resources_clean native
