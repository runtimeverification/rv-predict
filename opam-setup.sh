#!/bin/sh

set -e
set -u
set -x

export OPAMROOT="$(mkcmake -V '$(.OBJDIR)')/.opam"

yes n | opam init
opam update 
opam switch 4.03.0
eval $(opam config env)
OPAMYES=true opam install ocp-ocamlres ocamlbuild-atdgen csv uri atdgen atdj
