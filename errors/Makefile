.if $(RVPMAKE_DIR:Unone) == none
.error "RVPMAKE_DIR must be set.  Run `rvpmake`."
.endif

SUBPRJ=lib:bin	# bin depends on lib
SUBPRJ+=share

.include <mkc.subprj.mk>
