package rvpredict.instrumentation.transformer;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.InstructionAdapter;
import org.objectweb.asm.commons.Method;

import rvpredict.config.Configuration;
import rvpredict.instrumentation.MetaData;
import rvpredict.instrumentation.RVPredictRuntimeMethods;

import java.util.HashSet;
import java.util.Set;

import static rvpredict.instrumentation.InstrumentationUtils.*;

public class ClassTransformer extends ClassVisitor implements Opcodes {

    private final ClassWriter cw;
    private final Configuration config;
    private final ClassLoader loader;
    private final boolean isClassLoad;

    private int access;
    private boolean hasFinalizeMethod;

    private String className;
    private String superName;
    private String source;

    private int version;

    private final Set<String> finalFields = new HashSet<>();

    public static byte[] transform(ClassLoader loader, boolean isClassLoad, byte[] cbuf,
            Configuration config) {
        ClassReader cr = new ClassReader(cbuf);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        ClassTransformer transformer = new ClassTransformer(cw, loader, isClassLoad, config);
        cr.accept(transformer, 0);
        return cw.toByteArray();
    }

    private ClassTransformer(ClassWriter cw, ClassLoader loader, boolean isClassLoad,
            Configuration config) {
        super(ASM5, cw);
        assert cw != null;

        this.cw = (ClassWriter) this.cv;
        this.loader = loader;
        this.isClassLoad = isClassLoad;
        this.config = config;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName,
            String[] interfaces) {
        className = name;
        this.superName = superName;
        this.version = version;
        this.access = access;
        cv.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public void visitSource(String source, String debug) {
        this.source = source;
        cv.visitSource(source, debug);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature,
            Object value) {
        /* TODO(YilongL): add comments about what is special about `final`,
         * `volatile`, and `static` w.r.t. instrumentation */

        MetaData.addField(className, name);
        if ((access & ACC_FINAL) != 0) {
            finalFields.add(name);
        }
        if ((access & ACC_VOLATILE) != 0) {
            MetaData.addVolatileVariable(className, name);
        }

        return cv.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
            String[] exceptions) {
        hasFinalizeMethod = hasFinalizeMethod || (name + desc).equals("finalize()V");

        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);

        /* do not instrument synthesized bridge method; otherwise, it may cause
         * infinite recursion at runtime */
        if (mv != null && (access & ACC_BRIDGE) == 0) {
            int crntMaxLocals = 0;
            for (Type type : Type.getArgumentTypes(desc)) {
                crntMaxLocals += type.getSize();
            }

            mv = new MethodTransformer(mv, source, className, version, name, desc, access,
                    crntMaxLocals, finalFields, loader, config);
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        addFinalizeMethod();
        super.visitEnd();
    }

    private void addFinalizeMethod() {
        /* class redefinition does not support adding method */
        if (!isClassLoad || hasFinalizeMethod || (access & (ACC_INTERFACE | ACC_ENUM)) != 0) {
            return;
        }
        if (className.contains("$$Lambda$")) {
            // YilongL: related to issue https://github.com/runtimeverification/rv-predict/issues/302
            return;
        }

        Pair<String, Boolean> result = getSuperFinalizeClass(superName, loader);
        String superFinalizeClass = result.getLeft();
        if (result.getRight()) {
            System.err.println("[Warning] unable to add finalize() method to " + className
                    + " because it's declared as final in " + superFinalizeClass);
            return;
        }

        InstructionAdapter mv = new InstructionAdapter(cw.visitMethod(ACC_PROTECTED,
                "finalize", "()V", null, new String[] { "java/lang/Throwable" }));
        /* call super.finalize() first */
        mv.load(0, OBJECT_TYPE);
        mv.invokespecial(superFinalizeClass, "finalize", "()V", false);
        /* log FINALIZE event */
        Method method = RVPredictRuntimeMethods.LOG_OBJECT_FINALIZE.method;
        mv.load(0, OBJECT_TYPE);
        mv.invokestatic(RVPREDICT_RUNTIME_TYPE.getInternalName(), method.getName(),
                method.getDescriptor(), false);
        /* return */
        mv.areturn(Type.VOID_TYPE);
        mv.visitMaxs(0, 0);
    }

}
