package rvpredict.instrumentation;

import db.DBEngine;
import rvpredict.config.Configuration;
import rvpredict.config.Util;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import rvpredict.config.Config;
import rvpredict.engine.main.Main;
import rvpredict.logging.RecordRT;
import rvpredict.util.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SnoopInstructionTransformer implements ClassFileTransformer {

    public static void premain(String agentArgs, Instrumentation inst) {
        if (agentArgs == null) {
            agentArgs = "";
        }
        if (agentArgs.startsWith("\"")) {
            assert agentArgs.endsWith("\"") : "Argument must be quoted";
            agentArgs = agentArgs.substring(1, agentArgs.length() - 1);
        }
        String[] args = agentArgs.split(" (?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        Config config = Config.instance;
        config.commandLine.parseArguments(args, false);
        if (Config.instance.commandLine.additionalExcludes != null) {
            String[] excludes = Config.instance.commandLine.additionalExcludes.replace('.','/').split(",");
            if (config.excludeList == null) {
                config.excludeList = excludes;
            } else {
                String[] array = new String[config.excludeList.length + excludes.length];
                System.arraycopy(config.excludeList, 0, array, 0, config.excludeList.length);
                System.arraycopy(excludes, 0, array, config.excludeList.length, excludes.length);
                config.excludeList = array;
            }
            System.out.println("Excluding: " + Arrays.toString(config.excludeList));
        }
        if (Config.instance.commandLine.additionalIncludes != null) {
            String[] includes = Config.instance.commandLine.additionalIncludes.replace('.','/').split(",");
            if (config.includeList == null) {
                config.includeList = includes;
            } else {
                System.out.println("Includes: " + Arrays.toString(includes));
                int length = config.includeList.length;
                String[] array = new String[length + includes.length];
                System.arraycopy(includes, 0, array, length, includes.length);
                config.includeList = array;
            }
            System.out.println("Including: " + Arrays.toString(config.includeList));
        }

        DBEngine db = new DBEngine(Config.instance.commandLine.outdir, Config.instance.commandLine.tableName);
        try {
            db.dropAll();
        } catch (Exception e) {
            config.commandLine.logger.report("Unexpected error while cleaning up the database:\n" +
                    e.getMessage(), Logger.MSGTYPE.ERROR);
            System.exit(1);
        }
        db.closeDB();
		//initialize RecordRT first
        RecordRT.init();
        
		inst.addTransformer(new SnoopInstructionTransformer());
        if (Config.instance.commandLine.predict == true) {
            String java = org.apache.tools.ant.util.JavaEnvUtils.getJreExecutable("java");
            String basePath = Main.getBasePath();
            String separator = System.getProperty("file.separator");
            String libPath = basePath + separator + "lib" + separator;
            String rvEngine = libPath + "rv-predict"  + ".jar";
            List<String> appArgList = new ArrayList<>();
            appArgList.add(java);
            appArgList.add("-cp");
            appArgList.add(rvEngine);
            appArgList.add("rvpredict.engine.main.Main");
            appArgList.addAll(Arrays.asList(args));

            int index = appArgList.indexOf(Configuration.opt_outdir);
            if (index != -1) {
                appArgList.set(index, Configuration.opt_only_predict);
            } else {
                appArgList.add(Configuration.opt_only_predict);
                appArgList.add(config.commandLine.outdir);
            }

            Config.instance.commandLine.logger.report("Prediction command: " + appArgList, Logger.MSGTYPE.VERBOSE);

            final ProcessBuilder processBuilder =
                    new ProcessBuilder(appArgList.toArray(args));
            String logOutputString = Config.instance.commandLine.log_output;
            boolean logToScreen = false;
            String file = null;
            if (logOutputString.equalsIgnoreCase(Configuration.YES)) {
                logToScreen = true;
            } else if (!logOutputString.equals(Configuration.NO)) {
                file = logOutputString;
                String actualOutFile = file + ".out";
                String actualErrFile = file + ".err";
                processBuilder.redirectError(new File(actualErrFile));
                processBuilder.redirectOutput(new File(actualOutFile));
            }
            StringBuilder commandMsg = new StringBuilder();
            commandMsg.append("Executing command: \n");
            commandMsg.append("   ");
            for (String arg : args) {
                if (arg.contains(" ")) {
                    commandMsg.append(" \"" + arg + "\"");
                } else {
                    commandMsg.append(" " + arg);
                }
            }
            Config.instance.commandLine.logger.report(commandMsg.toString(), Logger.MSGTYPE.VERBOSE);

            final boolean finalLogToScreen = logToScreen;
            final String finalFile = file;
            Thread predict = new Thread() {
                @Override
                public void run() {
                    Config.shutDown = true;
                    Process process = null;
                    try {
                        process = processBuilder.start();
                        if (finalLogToScreen) {
                            Util.redirectOutput(process.getErrorStream(), System.err);
                            Util.redirectOutput(process.getInputStream(), System.out);
                        } else if (finalFile == null) {
                            Util.redirectOutput(process.getErrorStream(), null);
                            Util.redirectOutput(process.getInputStream(), null);
                        }
                        Util.redirectInput(process.getOutputStream(), System.in);

                        process.waitFor();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            Runtime.getRuntime().addShutdownHook(predict);
        }
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
