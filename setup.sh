#!/bin/sh

set -e
set -u

export OPAMROOT="$(mkcmake -V '$(.OBJDIR)')/.opam"

yes n | opam init
opam update 
opam switch 4.03.0
eval $(opam config env)
OPAMYES=true opam install ocp-ocamlres ocamlbuild-atdgen csv uri atdgen atdj

install -o $(id -u) -g $(id -g) -m 0555 bin/rvpmake.in bin/rvpmake

home_slash=${HOME}/
bindir=$(pwd)/bin
home_relative_bindir=${bindir##${HOME}/}

if [ ${home_relative_bindir} != ${bindir} ]; then
	pathdir=\$HOME/${home_relative_bindir}
else
	pathdir=${bindir}
fi

echo "Setup is complete.  Add ${pathdir} to your PATH."

exit 0
