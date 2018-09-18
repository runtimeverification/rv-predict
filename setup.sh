#!/bin/sh

set -e
set -u
set -x

objdir=$(mkcmake -V '$(.OBJDIR)')
export RVP_SETUP=yes	# tells `qclang` not to expect `missingposix.h` to
			# be available 
export OPAMROOT="${objdir}/.opam"

home_slash=${HOME}/
nbstage=${objdir}/.nbstage
tools=${objdir}/.tools
bindir=${tools}/bin
etcdir=${tools}/etc
mandir=${tools}/man
home_relative_bindir=${bindir##${HOME}/}
home_relative_mandir=${mandir##${HOME}/}

if [ ${1:-none} = clean ]; then
	mkcmake PREFIX=${nbstage} LINKSPREFIX=${tools} -C nbtools cleandir
	if [ -d $HOME/qnx700 ]; then
		PATH=${tools}/bin:${PATH} \
		  mkcmake PREFIX=${tools} -C cross/qnx/lib cleandir
	fi
	rm -rf ${nbstage}
	rm -rf ${tools}
	rm -rf ${OPAMROOT}
	exit 0
fi

yes n | opam init
opam update 
opam switch 4.03.0
eval $(opam config env)
OPAMYES=true opam install \
    ocp-ocamlres ocamlbuild-atdgen csv uri "atdgen>=2" atdj

mkcmake PREFIX=${nbstage} LINKSPREFIX=${tools} -C nbtools \
    cleandir \
    all-mtree all-pax all-xinstall \
    install-mtree install-pax install-xinstall \
    cleandir-mtree cleandir-pax cleandir-xinstall

# Be tidy: get rid of disused build products.  Everything we need is
# now installed at ${nbstage} and ${tools}.
mkcmake PREFIX=${nbstage} LINKSPREFIX=${tools} -C nbtools cleandir

install -o $(id -u) -g $(id -g) -m 0755 -d ${tools}/etc
install -o $(id -u) -g $(id -g) -m 0644 etc/mk-c.conf \
    ${tools}/etc/mk-c.conf

install -o $(id -u) -g $(id -g) -m 0555 scripts/rvpmake.in \
    ${tools}/bin/rvpmake

if [ -d $HOME/qnx700 ]; then
	rm -f ${tools}/bin/qclang*
	install -o $(id -u) -g $(id -g) -m 0555 scripts/qclang-4.0 \
	    ${tools}/bin/qclang-4.0
	install -o $(id -u) -g $(id -g) -m 0555 scripts/qclang-4.0 \
	    ${tools}/bin/qclang

	PATH=${tools}/bin/:${PATH} \
	  mkcmake PREFIX=${tools} -C cross/qnx/lib cleandir all install
	# Re-clean so that there's no garbage to interfere with a
	# subsequent build.
	PATH=${tools}/bin:${PATH} \
	  mkcmake PREFIX=${tools} -C cross/qnx/lib cleandir
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
