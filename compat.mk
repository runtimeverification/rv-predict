OS!=uname -s

_ELFTOP:=$(.PARSEDIR)/external/bsd/elftoolchain

#.info .PARSEDIR=$(.PARSEDIR)
#.info _ELFTOP=$(_ELFTOP)

.if $(OS) == "Linux" || $(OS) == "QNX"
#DWARF_LDDIR?=${HOME}/pkg/lib
#ELF_LDDIR?=${HOME}/pkg/lib
#CPPFLAGS+=-isystem ${HOME}/pkg/include
#DWARF_LDDIR?=${HOME}/elftoolchain-install/usr/lib
#ELF_LDDIR?=${HOME}/elftoolchain-install/usr/lib
#CPPFLAGS+=-isystem ${HOME}/elftoolchain-install/usr/include
_DWARF_LDDIR!=cd $(_ELFTOP)/libdwarf && MAKECONF= MAKEFLAGS= $(MAKE) -V .OBJDIR
_ELF_LDDIR!=cd $(_ELFTOP)/libelf && MAKECONF= MAKEFLAGS= $(MAKE) -V .OBJDIR
DWARF_LDDIR?=${_DWARF_LDDIR}
ELF_LDDIR?=${_ELF_LDDIR}
CPPFLAGS+=-isystem $(_ELFTOP)/common
CPPFLAGS+=-isystem $(_ELFTOP)/libelf
CPPFLAGS+=-isystem $(_ELFTOP)/libdwarf
.endif
