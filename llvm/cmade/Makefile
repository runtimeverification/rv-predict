LLVMDIR?=/usr/lib/llvm-4.0/cmake

CLEANABLE_FILES=rvpinstrument.so
.if $(RELEASE:Uno) != "yes" && $(BUILD_TSAN:Uno) == "yes"
CLEANABLE_FILES+=libclang_rt.tsan_cxx-x86_64.a
CLEANABLE_FILES+=libclang_rt.tsan_cxx-x86_64.a.syms
CLEANABLE_FILES+=libclang_rt.tsan-x86_64.a
.endif
FILES=$(CLEANABLE_FILES)
FILESMODE=0755
FILESDIR=$(LIBEXECDIR)

installit:	.USE
	@$(INSTALL) -c -m 0755 $(.ALLSRC:Ninstallit) $(.OBJDIR)

libclang_rt.tsan_cxx-x86_64.a: build/runtime/lib/linux/libclang_rt.tsan_cxx-x86_64.a installit

libclang_rt.tsan_cxx-x86_64.a.syms: build/runtime/lib/linux/libclang_rt.tsan_cxx-x86_64.a.syms installit

libclang_rt.tsan-x86_64.a: build/runtime/lib/linux/libclang_rt.tsan-x86_64.a installit

rvpinstrument.so: build/pass/rvpinstrument.so installit

build: $(.CURDIR)/../CMakeLists.txt $(.CURDIR)/../pass/CMakeLists.txt $(.CURDIR)/../runtime/CMakeLists.txt
	@rm -rf $(.OBJDIR)/build
	@$(INSTALL) -d -m 0700 $(.OBJDIR)/build
	cd $(.OBJDIR)/build && cmake -DCMAKE_C_COMPILER=$(CC) -DCMAKE_CXX_COMPILER=$(CXX) -DLLVM_DIR=$(LLVMDIR) $(.CURDIR)/..

build/runtime/lib/linux/libclang_rt.tsan_cxx-x86_64.a \
build/runtime/lib/linux/libclang_rt.tsan_cxx-x86_64.a.syms \
build/runtime/lib/linux/libclang_rt.tsan-x86_64.a! build
	@cd $(.OBJDIR)/build && cmake --build . --target compiler-rt -- -s

build/pass/rvpinstrument.so! build
	@cd $(.OBJDIR)/build && cmake --build . --target rvpinstrument -- -s

#	@$(MAKE) -C $(.OBJDIR)/build

CLEANDIRS+=$(.OBJDIR)/build
CLEANFILES+=$(CLEANABLE_FILES)

.include <mkc.files.mk>
