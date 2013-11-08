package rvpredict.instrumentation;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import rvpredict.config.Config;

public class SnoopInstructionClassAdapter extends ClassVisitor {
	
	private String classname;
	
    public SnoopInstructionClassAdapter(ClassVisitor cv) {
        super(Opcodes.ASM5,cv);
    }
    
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        if (cv != null) {
            cv.visit(version, access, name, signature, superName, interfaces);
        }
        
        classname = name;
        if(Config.instance.verbose)
        System.out.println("classname: "+classname);
    } 
    
    public FieldVisitor visitField(int access, String name, String desc,
            String signature, Object value) {
		String sig_var = (classname+"."+name).replace("/", ".");
		GlobalStateForInstrumentation.instance.getVariableId(sig_var);
    	//Opcodes.ACC_FINAL
    	if((access & Opcodes.ACC_VOLATILE)!=0)
    	{//volatile
    		GlobalStateForInstrumentation.instance.addVolatileVariable(sig_var);
    	}
    	
        if (cv != null) {
            return cv.visitField(access, name, desc, signature, value);
        }
        return null;
    }

    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
       boolean isSynchronized = false;
       boolean isStatic = false;

       if((access & Opcodes.ACC_SYNCHRONIZED)!=0)
    	   isSynchronized = true;
       if((access & Opcodes.ACC_STATIC)!=0)
    	   isStatic = true;
       
        MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
        
        if (mv != null) {
        	
        	
//        	if(signature==null)
//        		signature = name+desc;
        	Type[] args = Type.getArgumentTypes(desc);
        	int length = args.length;
        	for(int i=0;i<args.length;i++)
        	{
        		if(args[i]==Type.DOUBLE_TYPE||args[i]==Type.LONG_TYPE)
        			length++;
        	}
//            System.out.println("******************* "+((access & Opcodes.ACC_STATIC)>0));
            mv = new SnoopInstructionMethodAdapter(mv,classname,name,name+desc,name.equals("<init>")||name.equals("<clinit>"), isSynchronized,isStatic,length);
           
        }

        return mv;
    }
}
