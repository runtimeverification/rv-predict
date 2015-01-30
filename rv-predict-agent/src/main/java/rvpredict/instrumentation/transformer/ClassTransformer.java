package rvpredict.instrumentation.transformer;

import org.objectweb.asm.*;
import rvpredict.config.Configuration;
import rvpredict.instrumentation.MetaData;

import java.util.HashSet;
import java.util.Set;

public class ClassTransformer extends ClassVisitor {

    private final Configuration config;

    private String className;
    private String source;

    private int version;

    private final Set<String> finalFields = new HashSet<>();

    public static byte[] transform(byte[] cbuf, Configuration config) {
        ClassReader cr = new ClassReader(cbuf);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        ClassTransformer transformer = new ClassTransformer(cw, config);
        cr.accept(transformer, 0);
        return cw.toByteArray();
    }

    private ClassTransformer(ClassVisitor cv, Configuration config) {
        super(Opcodes.ASM5, cv);
        assert cv != null;

        this.config = config;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName,
            String[] interfaces) {
        className = name;
        this.version = version;
        MetaData.setSuperclass(name, superName);
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
        if ((access & Opcodes.ACC_FINAL) != 0) {
            finalFields.add(name);
        }
        if ((access & Opcodes.ACC_VOLATILE) != 0) {
            MetaData.addVolatileVariable(className, name);
        }

        return cv.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
            String[] exceptions) {
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);

        /* do not instrument synthesized bridge method; otherwise, it may cause
         * infinite recursion at runtime */
        if (mv != null && (access & Opcodes.ACC_BRIDGE) == 0) {
            int crntMaxLocals = 0;
            for (Type type : Type.getArgumentTypes(desc)) {
                crntMaxLocals += type.getSize();
            }

            mv = new MethodTransformer(mv, source, className, version, name, desc, access,
                    crntMaxLocals, finalFields, config);
        }
        return mv;
    }
}
