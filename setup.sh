#!/bin/sh

set -e
set -u

objdir=$(mkcmake -V '$(.OBJDIR)')
export OPAMROOT="${objdir}/.opam"

yes n | opam init
opam update 
opam switch 4.03.0
eval $(opam config env)
OPAMYES=true opam install ocp-ocamlres ocamlbuild-atdgen csv uri atdgen atdj

home_slash=${HOME}/
bindir=${objdir}/.tools/bin
mandir=${objdir}/.tools/man
home_relative_bindir=${bindir##${HOME}/}
home_relative_mandir=${mandir##${HOME}/}

mkcmake PREFIX=${objdir}/.nbstage LINKSPREFIX=${objdir}/.tools -C nbtools \
    cleandir \
    all-mtree all-pax all-xinstall \
    install-mtree install-pax install-xinstall \
    cleandir-mtree cleandir-pax cleandir-xinstall

install -o $(id -u) -g $(id -g) -m 0555 bin/rvpmake.in \
    ${objdir}/.tools/bin/rvpmake

if [ ${home_relative_bindir} != ${bindir} ]; then
	pathdir=\$HOME/${home_relative_bindir}
else
	pathdir=${bindir}
fi

if [ ${home_relative_mandir} != ${mandir} ]; then
	manpathdir=\$HOME/${home_relative_mandir}
else
	manpathdir=${mandir}
fi

echo "Setup is complete.  Add ${pathdir} to your PATH."
echo "Add ${manpathdir} to your MANPATH."

exit 0
