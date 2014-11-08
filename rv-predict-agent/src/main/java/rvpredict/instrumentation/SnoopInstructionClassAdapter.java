package rvpredict.instrumentation;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import rvpredict.config.Config;

public class SnoopInstructionClassAdapter extends ClassVisitor {

    private final Config config;
    
    private final GlobalStateForInstrumentation globalState;
    
    private String classname;
    private String source;

    public SnoopInstructionClassAdapter(ClassVisitor cv, Config config,
            GlobalStateForInstrumentation globalState) {
        super(Opcodes.ASM5, cv);
        this.config = config;
        this.globalState = globalState;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName,
            String[] interfaces) {
        if (cv != null) {
            cv.visit(version, access, name, signature, superName, interfaces);
        }

        classname = name;
        if (config.verbose)
            System.out.println("classname: " + classname);
    }

    @Override
    public void visitSource(String source, String debug) {
        this.source = source;
        if (cv != null) {
            cv.visitSource(source, debug);
        }
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature,
            Object value) {
        String sig_var = (classname + "." + name).replace("/", ".");
        GlobalStateForInstrumentation.instance.getVariableId(sig_var);
        // Opcodes.ACC_FINAL
        if ((access & Opcodes.ACC_VOLATILE) != 0) {// volatile
            GlobalStateForInstrumentation.instance.addVolatileVariable(sig_var);
        }

        if (cv != null) {
            return cv.visitField(access, name, desc, signature, value);
        }
        return null;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature,
            String[] exceptions) {
        boolean isSynchronized = false;
        boolean isStatic = false;

        if ((access & Opcodes.ACC_SYNCHRONIZED) != 0)
            isSynchronized = true;
        if ((access & Opcodes.ACC_STATIC) != 0)
            isStatic = true;

        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);

        if (mv != null) {

            // if(signature==null)
            // signature = name+desc;
            Type[] args = Type.getArgumentTypes(desc);
            int length = args.length;
            for (int i = 0; i < args.length; i++) {
                if (args[i] == Type.DOUBLE_TYPE || args[i] == Type.LONG_TYPE)
                    length++;
            }
            // System.out.println("******************* "+((access &
            // Opcodes.ACC_STATIC)>0));
            mv = new SnoopInstructionMethodAdapter(mv, source, classname, name, name + desc,
                    name.equals("<init>") || name.equals("<clinit>"), isSynchronized, isStatic,
                    length, config, globalState);

        }

        return mv;
    }
}
