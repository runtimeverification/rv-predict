.if !make(tags)
SUBDIR=linux

HAVE_QNX700!=test -d $(HOME)/qnx700 && echo yes || echo no

.if $(HAVE_QNX700) == "yes"
SUBDIR+=qnx
.endif # $(HAVE_QNX700) == "yes"
.endif # !make(tags)

.include <mkc.subdir.mk>
