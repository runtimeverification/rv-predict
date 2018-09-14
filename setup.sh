#!/bin/sh

set -e
set -u

objdir=$(mkcmake -V '$(.OBJDIR)')
export RVP_SETUP=yes	# tells `qclang` not to expect `missingposix.h` to
			# be available 
export OPAMROOT="${objdir}/.opam"

yes n | opam init
opam update 
opam switch 4.03.0
eval $(opam config env)
OPAMYES=true opam install ocp-ocamlres ocamlbuild-atdgen csv uri "atdgen>=2" atdj

home_slash=${HOME}/
bindir=${objdir}/.tools/bin
etcdir=${objdir}/.tools/etc
mandir=${objdir}/.tools/man
home_relative_bindir=${bindir##${HOME}/}
home_relative_mandir=${mandir##${HOME}/}

mkcmake PREFIX=${objdir}/.nbstage LINKSPREFIX=${objdir}/.tools -C nbtools \
    cleandir \
    all-mtree all-pax all-xinstall \
    install-mtree install-pax install-xinstall \
    cleandir-mtree cleandir-pax cleandir-xinstall

install -o $(id -u) -g $(id -g) -m 0755 -d ${objdir}/.tools/etc
install -o $(id -u) -g $(id -g) -m 0644 etc/mk-c.conf \
    ${objdir}/.tools/etc/mk-c.conf

install -o $(id -u) -g $(id -g) -m 0555 scripts/rvpmake.in \
    ${objdir}/.tools/bin/rvpmake

if [ -d $HOME/qnx700 ]; then
	rm -f ${objdir}/.tools/bin/qclang*
	install -o $(id -u) -g $(id -g) -m 0555 scripts/qclang-4.0 \
	    ${objdir}/.tools/bin/qclang-4.0
	install -o $(id -u) -g $(id -g) -m 0555 scripts/qclang-4.0 \
	    ${objdir}/.tools/bin/qclang

	PATH=${objdir}/.tools/bin/:${PATH} \
	  mkcmake PREFIX=${objdir}/.tools -C cross/qnx/lib cleandir all install
fi


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
