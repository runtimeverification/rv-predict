package com.runtimeverification.rvpredict.instrumentation.transformer;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

import static com.runtimeverification.rvpredict.instrumentation.InstrumentationUtils.*;
import static com.runtimeverification.rvpredict.instrumentation.RVPredictRuntimeMethods.*;

public class ClassInitializerTransformer extends AdviceAdapter implements Opcodes {

    protected ClassInitializerTransformer(MethodVisitor mv, int access, String name, String desc) {
        super(ASM5, mv, access, name, desc);
    }

    @Override
    protected void onMethodEnter() {
        invokeStatic(RVPREDICT_RUNTIME_TYPE, LOG_CLINIT_ENTER.method);
    }

    @Override
    protected void onMethodExit(int opcode) {
        invokeStatic(RVPREDICT_RUNTIME_TYPE, LOG_CLINIT_EXIT.method);
    }
}
