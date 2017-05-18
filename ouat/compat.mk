OS!=uname -s

.if $(OS) == "Linux"
#DWARF_LDDIR?=-L${HOME}/pkg/lib
#ELF_LDDIR?=-L${HOME}/pkg/lib
#CPPFLAGS+=-isystem ${HOME}/pkg/include
DWARF_LDDIR?=-L${HOME}/elftoolchain-install/usr/lib
ELF_LDDIR?=-L${HOME}/elftoolchain-install/usr/lib
CPPFLAGS+=-isystem ${HOME}/elftoolchain-install/usr/include
.endif
