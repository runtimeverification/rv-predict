/* This code that used to be at the end of RVPredictInstrument::runOnFunction
 * contains some hard-won knowledge of how to use the LLVM API.
 *
 * It matches a function `main` and inserts a call to
 * __rvpredict_main_entry() at the top, printing a diagnostic messages
 * if `main`'s signature is `int main(int, char **) { ... }` or `int
 * main(void) { ... }`.
 */

	PointerType *ptr_to_ptr_to_char_type =
	    Type::getInt8Ty(F.getContext())->getPointerTo()->getPointerTo();

	if (F.getName() == "main") {
		std::string type_message;
		llvm::raw_string_ostream sstr(type_message);
		IRBuilder<> builder(F.getEntryBlock().getFirstNonPHI());
		auto void_type = builder.getVoidTy();
		auto fn_type = F.getFunctionType();

		if ((fn_type->getReturnType()->isIntegerTy() &&
		     fn_type->getNumParams() == 0) ||
		    (fn_type->getReturnType()->isIntegerTy() &&
		     fn_type->getNumParams() >= 2 &&
		     fn_type->getReturnType() == fn_type->getParamType(0) &&
		     fn_type->getParamType(1) == ptr_to_ptr_to_char_type)) {
			sstr << F.getName().str() << " has type ";
			fn_type->print(sstr);
			builder.getContext().diagnose(
			    DiagnosticInfoRemark(F,
				F.getEntryBlock().getFirstNonPHI()->getDebugLoc(),
				sstr.str()));
		}

		FunctionType *main_entry_type = FunctionType::get(void_type,
		    {fn_type->getReturnType(), ptr_to_ptr_to_char_type}, false);
		Function *main_entry = checkSanitizerInterfaceFunction(
		    m.getOrInsertFunction("__rvpredict_main_entry",
			main_entry_type));
		Value *argc, *argv;
		if (fn_type->getNumParams() == 0) {
			argc = ConstantInt::get(fn_type->getReturnType(), 0);
			argv =
			    ConstantPointerNull::get(ptr_to_ptr_to_char_type);
		} else {
			llvm::Function::arg_iterator argiter = F.arg_begin();
			argc = &(*argiter++);
			argv = &(*argiter++);
		}
		builder.CreateCall(main_entry, {argc, argv});
	}
