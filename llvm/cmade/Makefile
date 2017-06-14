CLEANABLE_FILES=rvpinstrument.so
CLEANABLE_FILES+=libclang_rt.tsan_cxx-x86_64.a
CLEANABLE_FILES+=libclang_rt.tsan_cxx-x86_64.a.syms
CLEANABLE_FILES+=libclang_rt.tsan-x86_64.a
FILES=$(CLEANABLE_FILES)
FILESMODE=0755
FILESDIR=$(LIBDIR)

installit:	.USE
	@$(INSTALL) -c -m 0755 $(.ALLSRC:Ninstallit) $(.OBJDIR)

libclang_rt.tsan_cxx-x86_64.a: build/runtime/lib/linux/libclang_rt.tsan_cxx-x86_64.a installit

libclang_rt.tsan_cxx-x86_64.a.syms: build/runtime/lib/linux/libclang_rt.tsan_cxx-x86_64.a.syms installit

libclang_rt.tsan-x86_64.a: build/runtime/lib/linux/libclang_rt.tsan-x86_64.a installit

rvpinstrument.so: build/pass/rvpinstrument.so installit

build: $(.CURDIR)/../CMakeLists.txt $(.CURDIR)/../pass/CMakeLists.txt $(.CURDIR)/../runtime/CMakeLists.txt
	@rm -rf $(.OBJDIR)/build
	@$(INSTALL) -d -m 0700 $(.OBJDIR)/build
	cd $(.OBJDIR)/build && cmake -DLLVM_DIR=$(HOME)/clang+llvm-3.8.1-x86_64-linux-gnu-ubuntu-16.04/share/llvm/cmake $(.CURDIR)/..

build/runtime/lib/linux/libclang_rt.tsan_cxx-x86_64.a \
build/runtime/lib/linux/libclang_rt.tsan_cxx-x86_64.a.syms \
build/runtime/lib/linux/libclang_rt.tsan-x86_64.a! build
	@cd $(.OBJDIR)/build && cmake --build $(.OBJDIR)/build/runtime -- -s

build/pass/rvpinstrument.so! build
	@cd $(.OBJDIR)/build && cmake --build $(.OBJDIR)/build/pass -- -s

#	@$(MAKE) -C $(.OBJDIR)/build

CLEANDIRS+=$(.OBJDIR)/build
CLEANFILES+=$(CLEANABLE_FILES)

.include <mkc.files.mk>
