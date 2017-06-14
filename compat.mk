OS!=uname -s

.if $(OS) == "Linux"
#DWARF_LDDIR?=${HOME}/pkg/lib
#ELF_LDDIR?=${HOME}/pkg/lib
#CPPFLAGS+=-isystem ${HOME}/pkg/include
#DWARF_LDDIR?=${HOME}/elftoolchain-install/usr/lib
#ELF_LDDIR?=${HOME}/elftoolchain-install/usr/lib
#CPPFLAGS+=-isystem ${HOME}/elftoolchain-install/usr/include
_DWARF_LDDIR!=cd ${.CURDIR}/../external/bsd/elftoolchain/libdwarf && MAKEFLAGS= $(MAKE) -V .OBJDIR
_ELF_LDDIR!=cd ${.CURDIR}/../external/bsd/elftoolchain/libelf && MAKEFLAGS= $(MAKE) -V .OBJDIR
DWARF_LDDIR?=${_DWARF_LDDIR}
ELF_LDDIR?=${_ELF_LDDIR}
CPPFLAGS+=-isystem ${.CURDIR}/../external/bsd/elftoolchain/common
CPPFLAGS+=-isystem ${.CURDIR}/../external/bsd/elftoolchain/libelf
CPPFLAGS+=-isystem ${.CURDIR}/../external/bsd/elftoolchain/libdwarf
.endif
