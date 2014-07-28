package rvpredict.instrumentation;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import rvpredict.config.Config;
import rvpredict.logging.RecordRT;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.Arrays;

public class SnoopInstructionTransformer implements ClassFileTransformer {

    public static void premain(String agentArgs, Instrumentation inst) {
        if (agentArgs.startsWith("\"")) {
            assert agentArgs.endsWith("\"") : "Argument must be quoted";
            agentArgs = agentArgs.substring(1, agentArgs.length() - 1);
        }
        String[] args = agentArgs.split(" (?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        Config config = Config.instance;
        JCommander jc = new JCommander(config);
        jc.setProgramName(Config.PROGRAM_NAME);
        try {
            jc.parse(args);
        } catch (ParameterException e) {
            System.err.println("Error: Cannot parse command line arguments.");
            System.err.println(e.getMessage());
            System.exit(1);
        }
        if (Config.additionalExcludes != null) {
            String[] excludes = Config.additionalExcludes.replace('.','/').split(",");
            String[] array = new String[config.excludeList.length + excludes.length];
            System.arraycopy(config.excludeList, 0, array, 0, config.excludeList.length);
            System.arraycopy(excludes, 0, array, config.excludeList.length, excludes.length);
            System.out.println("Excluding: " + Arrays.toString(array));
            config.excludeList = array;
        }
        if (Config.additionalIncludes != null) {
            String[] includes = Config.additionalIncludes.replace('.','/').split(",");
            int length = config.includeList.length;
            String[] array = new String[length + includes.length];
            System.arraycopy(config.includeList, 0, array, 0, length);
            System.arraycopy(includes, 0, array, length, includes.length);
            System.out.println("Including: " + Arrays.toString(array));
            config.includeList = array;
        }
        if (config.includeList.length == 1 && config.includeList[0].isEmpty()) {
            config.includeList = null;
        }

		//initialize RecordRT first
        RecordRT.init();
        
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
