.include <mkc.init.mk>

.include "../crossdirs.mk"

.if make(obj)
SUBDIR=${CROSS_SUBDIR:C,^,../,}
.endif # make(obj)

obj:
	# do nothing

.include <mkc.subdir.mk>
