package com.runtimeverification.rvpredict.instrument.transformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.util.CheckClassAdapter;

import com.runtimeverification.rvpredict.config.Configuration;
import com.runtimeverification.rvpredict.instrument.InstrumentUtils;

public class ClassTransformer extends ClassVisitor implements Opcodes {

    private final Configuration config;

    private final TransformStrategy strategy;

    private final ClassLoader loader;

    private String className;
    private String source;

    private int version;

    public static byte[] transform(ClassLoader loader, String cname, byte[] cbuf, Configuration config,
            TransformStrategy strategy) {
        ClassReader cr = new ClassReader(cbuf);
        ClassWriter cw = new ClassWriter(cr, loader, cname, config.logger());
        ClassTransformer transformer = new ClassTransformer(cw, loader, config, strategy);
        cr.accept(transformer, ClassReader.EXPAND_FRAMES);

        byte[] result = cw.toByteArray();
        if (Configuration.debug) {
            new ClassReader(result).accept(
                new CheckClassAdapter(new org.objectweb.asm.ClassWriter(0)),
                0);
        }
        return result;
    }

    private ClassTransformer(ClassWriter cw, ClassLoader loader, Configuration config,
            TransformStrategy strategy) {
        super(ASM5, cw);
        assert cw != null;

        this.loader = loader;
        this.config = config;
        this.strategy = strategy;
    }

    private String replaceStandardLibraryClass(String literal) {
        return strategy.replaceStandardLibraryClass()
                ? InstrumentUtils.replaceStandardLibraryClass(className, literal) : literal;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName,
            String[] interfaces) {
        className = name;
        this.version = version;
        String[] interfaces2 = new String[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            interfaces2[i] = replaceStandardLibraryClass(interfaces[i]);
        }
        cv.visit(version, access, name, replaceStandardLibraryClass(signature),
                replaceStandardLibraryClass(superName), interfaces2);
    }

    @Override
    public void visitSource(String source, String debug) {
        this.source = source;
        cv.visitSource(source, debug);
    }

    @Override
    public void visitOuterClass(String owner, String name, String desc) {
        cv.visitOuterClass(owner, name, replaceStandardLibraryClass(desc));
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc,
            String signature, Object value) {
        return cv.visitField(access, name, replaceStandardLibraryClass(desc),
                replaceStandardLibraryClass(signature), value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
            String[] exceptions) {
        desc = replaceStandardLibraryClass(desc);
        signature = replaceStandardLibraryClass(signature);

        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        assert mv != null;

        if (strategy.logCallStackEvent()) {
            mv = new ExceptionHandlerSorter(mv, access, name, desc, signature, exceptions);
        }

        /* do not instrument synthesized bridge method; otherwise, it may cause
         * infinite recursion at runtime */
        if ((access & ACC_BRIDGE) == 0) {
            mv = new MethodTransformer(mv, source, className, version, name, desc, access,
                    loader, config, strategy);
        }

        if ("<clinit>".equals(name)) {
            mv = new ClassInitializerTransformer(mv, access, name, desc);
        }

        if ((version & 0xFFFF) < Opcodes.V1_6) {
            mv = new JSRInlinerAdapter(mv, access, name, desc, signature, exceptions);
        }

        return mv;
    }

}
