// original
int ThreadSanitizer::getMemoryAccessFuncIndex(Value *Addr,
                                              const DataLayout &DL) {
  Type *OrigPtrTy = Addr->getType();
  Type *OrigTy = cast<PointerType>(OrigPtrTy)->getElementType();
  assert(OrigTy->isSized());
  uint32_t TypeSize = DL.getTypeStoreSizeInBits(OrigTy);
  if (TypeSize != 8  && TypeSize != 16 &&
      TypeSize != 32 && TypeSize != 64 && TypeSize != 128) {
    NumAccessesWithBadSize++;
    // Ignore all unusual sizes.
    return -1;
  }
  size_t Idx = countTrailingZeros(TypeSize / 8);
  assert(Idx < kNumberOfAccessSizes);
  return Idx;
}

// version 1
int ThreadSanitizer::getMemoryAccessFuncIndex(Value *addr,
                                              const DataLayout &dl) {
	Type *ptr_type = addr->getType();
	Type *type = cast<PointerType>(ptr_type)->getElementType();
	assert(type->isSized());
	int idx = ffs(dl.getTypeStoreSizeInBits(type)) - 1;
	// Ignore all unusual sizes.
	if (idx < 0 || kNumberOfAccessSizes <= idx) {
		NumAccessesWithBadSize++;
		return -1;
	}
	return idx;
}
