package rvpredict.instrumentation;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import rvpredict.config.Config;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class SnoopInstructionTransformer implements ClassFileTransformer {
	public static void premain(String agentArgs, Instrumentation inst) {
        //System.out.println("calling premain");
		inst.addTransformer(new SnoopInstructionTransformer());
		
	}

    public byte[] transform(ClassLoader loader,String cname, Class<?> c, ProtectionDomain d, byte[] cbuf)
            throws IllegalClassFormatException {

        boolean toInstrument = true;
    	String[] tmp = Config.instance.excludeList;

        for (int i = 0; i < tmp.length; i++) {
            String s = tmp[i];
            if (cname.startsWith(s)) {
                toInstrument = false;
                break;
            }
        }
        tmp = Config.instance.includeList;
        if(tmp!=null)
        for (int i = 0; i < tmp.length; i++) {
            String s = tmp[i];
            if (cname.startsWith(s)) {
                toInstrument = true;
                break;
            }
        }
        
//		try {
//			ClassLoader.getSystemClassLoader().getParent().loadClass("java.io.File");
//			Class cz= Class.forName("java.io.File");
//			 System.out.println("((((((((((((((( "+cz.toString());
//		} catch (ClassNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//      System.out.println("((((((((((((((( transform "+cname);
        //special handle java.io.File
        if(cname.equals("java/io/File"))
        	toInstrument = true;
        
        if (toInstrument) {
        	
            //System.out.println("((((((((((((((( transform "+cname);
            ClassReader cr = new ClassReader(cbuf);
            
            ClassWriter cw = new ClassWriter(cr, 0);
            ClassVisitor cv = new SnoopInstructionClassAdapter(cw);
//            ClassVisitor cv = new SnoopInstructionClassAdapter(new TraceClassVisitor(cw,new PrintWriter( System.out )));
            cr.accept(cv, 0);

            byte[] ret = cw.toByteArray();
//            if(cname.equals("org/dacapo/parser/Config$Size"))
//            try {
//                FileOutputStream out = new FileOutputStream("tmp.class");
//                out.write(ret);
//                out.close();
//            } catch(Exception e) {
//                e.printStackTrace();
//            }
            //System.err.println(")))))))))))))) end transform "+cname);
            return ret;
        } else {
            //System.out.println("--------------- skipping "+cname);
        }
        return cbuf;
    }
}
