package rvpredict.instrumentation;

import org.objectweb.asm.*;

import rvpredict.config.Config;

public class SnoopInstructionMethodAdapter extends MethodVisitor implements Opcodes{
	
	final static String CLASS_INTEGER = "java/lang/Integer";
	final static String CLASS_BOOLEAN = "java/lang/Boolean";
	final static String CLASS_CHAR = "java/lang/Character";
	final static String CLASS_SHORT = "java/lang/Short";
	final static String CLASS_BYTE = "java/lang/Byte";
	final static String CLASS_LONG = "java/lang/Long";
	final static String CLASS_FLOAT = "java/lang/Float";
	final static String CLASS_DOUBLE = "java/lang/Double";

	final static String METHOD_VALUEOF = "valueOf";
	final static String DESC_INTEGER_VALUEOF = "(I)Ljava/lang/Integer;";
	final static String DESC_BOOLEAN_VALUEOF = "(Z)Ljava/lang/Boolean;";
	final static String DESC_BYTE_VALUEOF = "(B)Ljava/lang/Byte;";
	final static String DESC_SHORT_VALUEOF = "(S)Ljava/lang/Short;";
	final static String DESC_CHAR_VALUEOF = "(C)Ljava/lang/Character;";
	final static String DESC_LONG_VALUEOF = "(J)Ljava/lang/Long;";
	final static String DESC_FLOAT_VALUEOF = "(F)Ljava/lang/Float;";
	final static String DESC_DOUBLE_VALUEOF = "(D)Ljava/lang/Double;";

    boolean isInit,isSynchronized,isStatic;
    String classname;
    String methodname;
    String methodsignature;
    private int maxindex_cur;//current max index of local variables 
    private int line_cur;
    
    public SnoopInstructionMethodAdapter(MethodVisitor mv, String cname,String mname, String msignature,boolean isInit, boolean isSynchronized, boolean isStatic, int argSize) {
        super(Opcodes.ASM5,mv);
        this.classname = cname;
        this.methodname = mname;
        this.methodsignature = msignature;
        this.isInit = isInit;
        this.isSynchronized = isSynchronized;
        this.isStatic = isStatic;
        
        maxindex_cur=argSize+1;
        if(Config.instance.verbose)
        System.out.println("method: "+methodname);
    }
    private void addBipushInsn(MethodVisitor mv, int val) {
        switch (val) {
            case 0:
                mv.visitInsn(ICONST_0);
                break;
            case 1:
                mv.visitInsn(ICONST_1);
                break;
            case 2:
                mv.visitInsn(ICONST_2);
                break;
            case 3:
                mv.visitInsn(ICONST_3);
                break;
            case 4:
                mv.visitInsn(ICONST_4);
                break;
            case 5:
                mv.visitInsn(ICONST_5);
                break;
            default:
                mv.visitLdcInsn(new Integer(val));
                break;
        }

    }
    public void visitMaxs(int maxStack, int maxLocals) {
        mv.visitMaxs(maxStack + 5, maxindex_cur+2);//may change to ...

    }
    public void visitVarInsn(int opcode, int var) {

    	if(var>maxindex_cur)
    		maxindex_cur=var;
    	
        switch (opcode) {
            case LSTORE:
            case DSTORE:
            case LLOAD:
            case DLOAD:
            	if(var==maxindex_cur)
            		maxindex_cur=var+1;
        	case ISTORE:
        	case FSTORE:
        	case ASTORE:
            case ILOAD:
            case FLOAD:
            case ALOAD:
            case RET:  	
                mv.visitVarInsn(opcode, var);
                break;
            default:
                System.err.println("Unknown var instruction opcode "+opcode);
                System.exit(1);
        }
    }
    
    private void storeValue(String desc, int index)
    {
    	if(desc.startsWith("L")||desc.startsWith("["))
    	{
        	mv.visitInsn(DUP);
    		mv.visitVarInsn(ASTORE, index);
    	}
    	else if (desc.startsWith("I")||desc.startsWith("B")||desc.startsWith("S")||desc.startsWith("Z")||desc.startsWith("C"))
    	{
        	mv.visitInsn(DUP);
    		mv.visitVarInsn(ISTORE, index);
    	}
    	else if (desc.startsWith("J"))
    	{
        	mv.visitInsn(DUP2);
    		mv.visitVarInsn(LSTORE, index);
    		maxindex_cur++;
    	}
    	else if (desc.startsWith("F"))
    	{
    		mv.visitInsn(DUP);
    		mv.visitVarInsn(FSTORE, index);
    	}
    	else if (desc.startsWith("D"))
    	{
        	mv.visitInsn(DUP2);
    		mv.visitVarInsn(DSTORE, index);
    		maxindex_cur++;
    	}

//    	if(classname.equals("org/eclipse/core/runtime/internal/adaptor/PluginConverterImpl"))
//    		System.out.println("Signature: "+desc);
    }
    private void loadValue(String desc, int index)
    {
    	if(desc.startsWith("L")||desc.startsWith("["))
        	mv.visitVarInsn(ALOAD, index);
        else if (desc.startsWith("I"))
        {
        		//convert int to object?
        		mv.visitVarInsn(ILOAD, index);
        		mv.visitMethodInsn(INVOKESTATIC, CLASS_INTEGER, METHOD_VALUEOF,
                        DESC_INTEGER_VALUEOF);
        }
        else if (desc.startsWith("B"))
        {
        		//convert int to object?
        		mv.visitVarInsn(ILOAD, index);
        		mv.visitMethodInsn(INVOKESTATIC, CLASS_BYTE, METHOD_VALUEOF,
                        DESC_BYTE_VALUEOF);
        }
        else if (desc.startsWith("S"))
        {
        		//convert int to object?
        		mv.visitVarInsn(ILOAD, index);
        		mv.visitMethodInsn(INVOKESTATIC, CLASS_SHORT, METHOD_VALUEOF,
                        DESC_SHORT_VALUEOF);
        }
        else if (desc.startsWith("Z"))
        {
        		//convert int to object?
        		mv.visitVarInsn(ILOAD, index);
        		mv.visitMethodInsn(INVOKESTATIC, CLASS_BOOLEAN, METHOD_VALUEOF,
                        DESC_BOOLEAN_VALUEOF);
        }
        else if (desc.startsWith("C"))
        {
        		//convert int to object?
        		mv.visitVarInsn(ILOAD, index);
        		mv.visitMethodInsn(INVOKESTATIC, CLASS_CHAR, METHOD_VALUEOF,
                        DESC_CHAR_VALUEOF);
        }
        else if (desc.startsWith("J"))
        {
    		//convert int to object?
    		mv.visitVarInsn(LLOAD, index);
    		mv.visitMethodInsn(INVOKESTATIC, CLASS_LONG, METHOD_VALUEOF,
                    DESC_LONG_VALUEOF);
        }
    	else if (desc.startsWith("F"))
        {
    		//convert int to object?
    		mv.visitVarInsn(FLOAD, index);
    		mv.visitMethodInsn(INVOKESTATIC, CLASS_FLOAT, METHOD_VALUEOF,
                    DESC_FLOAT_VALUEOF);
        }
    	else if (desc.startsWith("D"))
        {
    		//convert int to object?
    		mv.visitVarInsn(DLOAD, index);
    		mv.visitMethodInsn(INVOKESTATIC, CLASS_DOUBLE, METHOD_VALUEOF,
                    DESC_DOUBLE_VALUEOF);
        }
    	
    }
    public void visitLineNumber(int line, Label start) {
    	line_cur = line;
        mv.visitLineNumber(line, start);
    }
    public void visitMethodInsn(int opcode, String owner, String name,
            String desc) {
    	String sig_loc = (classname+"|"+methodsignature+"|"+line_cur).replace("/", ".");
    	int ID  = GlobalStateForInstrumentation.instance.getLocationId(sig_loc);
    	switch (opcode) {
        case INVOKEVIRTUAL:
        	if(GlobalStateForInstrumentation.instance.isThreadClass(owner)
        			&&name.equals("start") &&desc.equals("()V")) {
        		maxindex_cur++;
            	int index = maxindex_cur;
            	mv.visitInsn(DUP);
            	mv.visitVarInsn(ASTORE, index);
            	addBipushInsn(mv,ID);
             	mv.visitVarInsn(ALOAD, index);
        		mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_THREAD_START, 
        				Config.instance.DESC_LOG_THREAD_START);
                
        		mv.visitMethodInsn(opcode, owner, name, desc);
			}
        	else if(GlobalStateForInstrumentation.instance.isThreadClass(owner)
        			&&name.equals("join") &&desc.equals("()V")) {
        		maxindex_cur++;
            	int index = maxindex_cur;
            	mv.visitInsn(DUP);
            	mv.visitVarInsn(ASTORE, index);
            	
        		mv.visitMethodInsn(opcode, owner, name, desc);

            	addBipushInsn(mv,ID);
             	mv.visitVarInsn(ALOAD, index);
        		mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_THREAD_JOIN, 
        				Config.instance.DESC_LOG_THREAD_JOIN);
                
			}
        	else if(GlobalStateForInstrumentation.instance.isThreadClass(owner)
        			&&name.equals("wait") &&desc.equals("()V")) {
        		maxindex_cur++;
            	int index = maxindex_cur;
            	mv.visitInsn(DUP);
            	mv.visitVarInsn(ASTORE, index);
            	
            	addBipushInsn(mv,ID);
             	mv.visitVarInsn(ALOAD, index);
        		mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_WAIT, 
        				Config.instance.DESC_LOG_WAIT);
        		
        		mv.visitMethodInsn(opcode, owner, name, desc); 
			}
        	else if(GlobalStateForInstrumentation.instance.isThreadClass(owner)
        			&&name.equals("wait") &&desc.equals("()V")) {
        		maxindex_cur++;
            	int index = maxindex_cur;
            	mv.visitInsn(DUP);
            	mv.visitVarInsn(ASTORE, index);
            	
            	addBipushInsn(mv,ID);
             	mv.visitVarInsn(ALOAD, index);
        		mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_WAIT, 
        				Config.instance.DESC_LOG_WAIT);
        		
        		mv.visitMethodInsn(opcode, owner, name, desc); 
			}
        	else if(GlobalStateForInstrumentation.instance.isThreadClass(owner)
        			&&(name.equals("notify")||name.equals("notifyAll"))&&desc.equals("()V")) {
        		maxindex_cur++;
            	int index = maxindex_cur;
            	mv.visitInsn(DUP);
            	mv.visitVarInsn(ASTORE, index);
            	
            	addBipushInsn(mv,ID);
             	mv.visitVarInsn(ALOAD, index);
        		mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_NOTIFY, 
        				Config.instance.DESC_LOG_NOTIFY);
        		
        		mv.visitMethodInsn(opcode, owner, name, desc); 
			}
        	else
        		mv.visitMethodInsn(opcode, owner, name, desc); 

        	break;
        case INVOKESPECIAL:
        case INVOKESTATIC:
        case INVOKEINTERFACE:
        	mv.visitMethodInsn(opcode, owner, name, desc); 
        	break;
        default:
            System.err.println("Unknown method invocation opcode "+opcode);
            System.exit(1);
    	}

    }
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {

    	//signature + line number
    	String sig_var = (owner+"."+name).replace("/", ".");
    	String sig_loc = (owner+"|"+methodsignature+"|"+sig_var+"|"+line_cur).replace("/", ".");
    	int SID = GlobalStateForInstrumentation.instance.getVariableId(sig_var);
    	int ID  = GlobalStateForInstrumentation.instance.getLocationId(sig_loc);
    	
        switch (opcode) {
            case GETSTATIC:
            	mv.visitFieldInsn(opcode, owner, name, desc);
            	if(!isInit)
            	{
            	maxindex_cur++;

        		int index = maxindex_cur;
        		storeValue(desc,index);
            	
            	addBipushInsn(mv,ID);
            	mv.visitInsn(ACONST_NULL);
            	addBipushInsn(mv,SID);
            	loadValue(desc,index);
            	addBipushInsn(mv,0);

            	
            	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_FIELD_ACCESS,
            			Config.instance.DESC_LOG_FIELD_ACCESS);
            	
            	}
                break;
            case PUTSTATIC:
            	maxindex_cur++;
            	int index = maxindex_cur;
        		storeValue(desc,index);

            	 mv.visitFieldInsn(opcode, owner, name, desc);
            	 addBipushInsn(mv,ID);
             	mv.visitInsn(ACONST_NULL);
             	addBipushInsn(mv,SID);
             	loadValue(desc,index);
             	
             	if(isInit)
                 	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_INIT_WRITE_ACCESS,
                 			Config.instance.DESC_LOG_INIT_WRITE_ACCESS);//
             	else
             	{
                 	addBipushInsn(mv,1);
             		mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_FIELD_ACCESS,
             			Config.instance.DESC_LOG_FIELD_ACCESS);//
             	}
            	 
                break;
            case GETFIELD:
            	if(!isInit)
            	{
	            	maxindex_cur++;
	            	int index1 = maxindex_cur;
	            	mv.visitInsn(DUP);
	            	mv.visitVarInsn(ASTORE, index1);
	            	
	            	 mv.visitFieldInsn(opcode, owner, name, desc);
	            	 
	            	 maxindex_cur++;
	            	 int index2 = maxindex_cur;
	        		storeValue(desc,index2);
	
	             	addBipushInsn(mv,ID);
	             	mv.visitVarInsn(ALOAD, index1);
	             	addBipushInsn(mv,SID);
	             	loadValue(desc,index2);
	             	
	             	addBipushInsn(mv,0);
	
	             	
	             	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_FIELD_ACCESS,
	             			Config.instance.DESC_LOG_FIELD_ACCESS);
            	}
            	else
	            	 mv.visitFieldInsn(opcode, owner, name, desc);	
             		
                break;
                
            case PUTFIELD:
            	if(name.startsWith("this$"))//inner class
            	{
            		mv.visitFieldInsn(opcode, owner, name, desc);break;
            	}
            	
            	if(classname.contains("$")&&name.startsWith("val$"))//strange class
            	{   		
            		mv.visitFieldInsn(opcode, owner, name, desc);break;
            	}
            		
//            	if(classname.equals("org/eclipse/osgi/framework/eventmgr/CopyOnWriteIdentityMap$Snapshot$EntrySet")
//            			&&methodname.equals("<init>"))
//            	{
//            		System.out.println(owner+" "+name+" "+desc);
//            		mv.visitFieldInsn(opcode, owner, name, desc);break;
//            	}
            	
            	maxindex_cur++;
            	int index1 = maxindex_cur;     
            	int index2;
            	if(desc.startsWith("D"))
            	{
            		mv.visitVarInsn(DSTORE, index1);
            		maxindex_cur++;//double
            		maxindex_cur++;
                	index2 = maxindex_cur;
                	mv.visitInsn(DUP);
                 	mv.visitVarInsn(ASTORE, index2);
                 	mv.visitVarInsn(DLOAD, index1);
            	}
            	else if(desc.startsWith("J"))
            	{
            		mv.visitVarInsn(LSTORE, index1);
            		maxindex_cur++;//long
            		maxindex_cur++;
                	index2 = maxindex_cur;
                	mv.visitInsn(DUP);
                 	mv.visitVarInsn(ASTORE, index2);
                 	mv.visitVarInsn(LLOAD, index1);
            	}
            	else if(desc.startsWith("F"))
            	{
            		mv.visitVarInsn(FSTORE, index1);
            		maxindex_cur++;//float
                	index2 = maxindex_cur;
                	mv.visitInsn(DUP);
                 	mv.visitVarInsn(ASTORE, index2);
                 	mv.visitVarInsn(FLOAD, index1);
            	}
            	else if(desc.startsWith("["))
            	{
            		mv.visitVarInsn(ASTORE, index1);
            		maxindex_cur++;//ref or array
                	index2 = maxindex_cur;
                	mv.visitInsn(DUP);
                 	mv.visitVarInsn(ASTORE, index2);
                 	mv.visitVarInsn(ALOAD, index1);
            	}
            	else if(desc.startsWith("L"))
            	{
            		mv.visitVarInsn(ASTORE, index1);
            		maxindex_cur++;//ref or array
                	index2 = maxindex_cur;
                	mv.visitInsn(DUP);
                 	mv.visitVarInsn(ASTORE, index2);
                 	mv.visitVarInsn(ALOAD, index1);
                 	
//                	if(classname.equals("org/dacapo/parser/Config$Size")
//                			&&methodname.equals("<init>"))
//                		System.out.println("index1: "+ index1+" index2: "+index2);
            	}
            	else
            	{
            		mv.visitVarInsn(ISTORE, index1);
            		maxindex_cur++;//integer,char,short,boolean
                	index2 = maxindex_cur;
                	mv.visitInsn(DUP);
                 	mv.visitVarInsn(ASTORE, index2);
                 	mv.visitVarInsn(ILOAD, index1);
            	}

            	 mv.visitFieldInsn(opcode, owner, name, desc);
            	 
            	addBipushInsn(mv,ID);
              	mv.visitVarInsn(ALOAD, index2);
              	addBipushInsn(mv,SID);
              	loadValue(desc,index1);
              	
             	if(isInit)
                 	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_INIT_WRITE_ACCESS,
                 			Config.instance.DESC_LOG_INIT_WRITE_ACCESS);//
             	else
             	{
	              	addBipushInsn(mv,1);
	
	              	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_FIELD_ACCESS,
	              			Config.instance.DESC_LOG_FIELD_ACCESS);
             	}
             	
                break;
            default:
                System.err.println("Unknown field access opcode "+opcode);
                System.exit(1);
        }
    }
    private void convertPrimitiveToObject(int opcode)
    {
    	switch (opcode) {
    	case IALOAD:
    	case IASTORE:
    		mv.visitMethodInsn(INVOKESTATIC, CLASS_INTEGER, METHOD_VALUEOF,
                    DESC_INTEGER_VALUEOF);
    		break;
    	case BALOAD:
    	case BASTORE:
    		mv.visitMethodInsn(INVOKESTATIC, CLASS_BOOLEAN, METHOD_VALUEOF,
                    DESC_BOOLEAN_VALUEOF);break;
    	case CALOAD:
    	case CASTORE:
    		mv.visitMethodInsn(INVOKESTATIC, CLASS_CHAR, METHOD_VALUEOF,
                    DESC_CHAR_VALUEOF);break;
    	case DALOAD:
    	case DASTORE:
    		mv.visitMethodInsn(INVOKESTATIC, CLASS_DOUBLE, METHOD_VALUEOF,
                    DESC_DOUBLE_VALUEOF);break;
    	case FALOAD:
    	case FASTORE:
    		mv.visitMethodInsn(INVOKESTATIC, CLASS_FLOAT, METHOD_VALUEOF,
                    DESC_FLOAT_VALUEOF);break;
    	case LALOAD:
    	case LASTORE:
    		mv.visitMethodInsn(INVOKESTATIC, CLASS_LONG, METHOD_VALUEOF,
                    DESC_LONG_VALUEOF);break;
    	case SALOAD:
    	case SASTORE:
    		mv.visitMethodInsn(INVOKESTATIC, CLASS_SHORT, METHOD_VALUEOF,
                    DESC_SHORT_VALUEOF);
    		break;
    	}
    }
    public void visitInsn(int opcode) {
    	
    	String sig_loc = (classname+"|"+methodsignature+"|"+line_cur).replace("/", ".");
    	int ID  = GlobalStateForInstrumentation.instance.getLocationId(sig_loc);

    	switch (opcode) {
    	case AALOAD:
    		if(!isInit)
    		{
    		mv.visitInsn(DUP2);
    		maxindex_cur++;
        	int index1 = maxindex_cur;
        	mv.visitVarInsn(ISTORE, index1);
        	maxindex_cur++;
        	int index2 = maxindex_cur;
        	mv.visitVarInsn(ASTORE, index2);
        	mv.visitInsn(opcode);
        	mv.visitInsn(DUP);
        	maxindex_cur++;
        	int index3 = maxindex_cur;
        	mv.visitVarInsn(ASTORE, index3);
        	
        	addBipushInsn(mv,ID);
    		mv.visitVarInsn(ALOAD, index2);
    		mv.visitVarInsn(ILOAD, index1);
    		mv.visitVarInsn(ALOAD, index3);
    		
        	addBipushInsn(mv,0);

             	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_ARRAY_ACCESS,
             			Config.instance.DESC_LOG_ARRAY_ACCESS);
    		}
    		else
    			mv.visitInsn(opcode);
    		
    		break;

    	case BALOAD:
    	case CALOAD:
    	case SALOAD:
    	case IALOAD:
    		if(!isInit)
    		{
    		mv.visitInsn(DUP2);
    		maxindex_cur++;
        	int index1 = maxindex_cur;
        	mv.visitVarInsn(ISTORE, index1);
        	maxindex_cur++;
        	int index2 = maxindex_cur;
        	mv.visitVarInsn(ASTORE, index2);
        	mv.visitInsn(opcode);
        	mv.visitInsn(DUP);
        	maxindex_cur++;
        	int index3 = maxindex_cur;
        	mv.visitVarInsn(ISTORE, index3);
        	
        	addBipushInsn(mv,ID);
    		mv.visitVarInsn(ALOAD, index2);
    		mv.visitVarInsn(ILOAD, index1);
    		mv.visitVarInsn(ILOAD, index3);
    		
    		
    		convertPrimitiveToObject(opcode);
    		
        	addBipushInsn(mv,0);

             	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_ARRAY_ACCESS,
             			Config.instance.DESC_LOG_ARRAY_ACCESS);
    		}
    		else
    			mv.visitInsn(opcode);
    		break;
    	case FALOAD:
    		if(!isInit)
    		{
    		mv.visitInsn(DUP2);
    		maxindex_cur++;
        	int index1 = maxindex_cur;
        	mv.visitVarInsn(ISTORE, index1);
        	maxindex_cur++;
        	int index2 = maxindex_cur;
        	mv.visitVarInsn(ASTORE, index2);
        	mv.visitInsn(opcode);
        	mv.visitInsn(DUP);
        	maxindex_cur++;
        	int index3 = maxindex_cur;
        	mv.visitVarInsn(FSTORE, index3);
        	
        	addBipushInsn(mv,ID);
    		mv.visitVarInsn(ALOAD, index2);
    		mv.visitVarInsn(ILOAD, index1);
    		mv.visitVarInsn(FLOAD, index3);
    		
    		
    		convertPrimitiveToObject(opcode);
    		
        	addBipushInsn(mv,0);

             	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_ARRAY_ACCESS,
             			Config.instance.DESC_LOG_ARRAY_ACCESS);
    		}
    		else
    			mv.visitInsn(opcode);
    		
    		break;
    	case DALOAD:
    		if(!isInit)
    		{
    		mv.visitInsn(DUP2);
    		maxindex_cur++;
        	int index1 = maxindex_cur;
        	mv.visitVarInsn(ISTORE, index1);
        	maxindex_cur++;
        	int index2 = maxindex_cur;
        	mv.visitVarInsn(ASTORE, index2);
        	mv.visitInsn(opcode);
        	mv.visitInsn(DUP2);//double
        	maxindex_cur++;
        	int index3 = maxindex_cur;
        	mv.visitVarInsn(DSTORE, index3);maxindex_cur++;
        	
        	addBipushInsn(mv,ID);
    		mv.visitVarInsn(ALOAD, index2);
    		mv.visitVarInsn(ILOAD, index1);
    		mv.visitVarInsn(DLOAD, index3);
    		
    		
    		convertPrimitiveToObject(opcode);
    		
        	addBipushInsn(mv,0);

             	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_ARRAY_ACCESS,
             			Config.instance.DESC_LOG_ARRAY_ACCESS);
    		}
    		else
    			mv.visitInsn(opcode);
    		break;
    	case LALOAD:
    		if(!isInit)
    		{
    		mv.visitInsn(DUP2);
    		maxindex_cur++;
        	int index1 = maxindex_cur;
        	mv.visitVarInsn(ISTORE, index1);
        	maxindex_cur++;
        	int index2 = maxindex_cur;
        	mv.visitVarInsn(ASTORE, index2);
        	mv.visitInsn(opcode);
        	mv.visitInsn(DUP2);//long
        	maxindex_cur++;
        	int index3 = maxindex_cur;
        	mv.visitVarInsn(LSTORE, index3);maxindex_cur++;
        	
        	addBipushInsn(mv,ID);
    		mv.visitVarInsn(ALOAD, index2);
    		mv.visitVarInsn(ILOAD, index1);
    		mv.visitVarInsn(LLOAD, index3);
    		
    		
    		convertPrimitiveToObject(opcode);
    		
        	addBipushInsn(mv,0);

             	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_ARRAY_ACCESS,
             			Config.instance.DESC_LOG_ARRAY_ACCESS);
    		}
    		else
    			mv.visitInsn(opcode);
    		break;
    	case AASTORE:
    		maxindex_cur++;
        	int index1 = maxindex_cur;
        	mv.visitVarInsn(ASTORE, index1);
        	maxindex_cur++;
        	int index2 = maxindex_cur;
        	mv.visitVarInsn(ISTORE, index2);
        	
        	mv.visitInsn(DUP);
        	maxindex_cur++;
        	int index3 = maxindex_cur;
        	mv.visitVarInsn(ASTORE, index3);//arrayref
        	mv.visitVarInsn(ILOAD, index2);//index
        	mv.visitVarInsn(ALOAD, index1);//value

        	mv.visitInsn(opcode);
        	
        	addBipushInsn(mv,ID);
    		mv.visitVarInsn(ALOAD, index3);
    		mv.visitVarInsn(ILOAD, index2);
    		mv.visitVarInsn(ALOAD, index1);

    		if(isInit)
    		{
             	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_INIT_WRITE_ACCESS,
             			Config.instance.DESC_LOG_INIT_WRITE_ACCESS);

    		}
    		else
    		{
            	addBipushInsn(mv,1);
             	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_ARRAY_ACCESS,
             			Config.instance.DESC_LOG_ARRAY_ACCESS);
    		}
    		break;
    	case BASTORE:
    	case CASTORE:
    	case SASTORE:
    	case IASTORE:	
    		maxindex_cur++;
        	index1 = maxindex_cur;
        	mv.visitVarInsn(ISTORE, index1);
        	maxindex_cur++;
        	index2 = maxindex_cur;
        	mv.visitVarInsn(ISTORE, index2);
        	
        	mv.visitInsn(DUP);
        	maxindex_cur++;
        	index3 = maxindex_cur;
        	mv.visitVarInsn(ASTORE, index3);//arrayref
        	mv.visitVarInsn(ILOAD, index2);//index
        	mv.visitVarInsn(ILOAD, index1);//value

        	mv.visitInsn(opcode);
        	
        	addBipushInsn(mv,ID);
    		mv.visitVarInsn(ALOAD, index3);
    		mv.visitVarInsn(ILOAD, index2);
    		mv.visitVarInsn(ILOAD, index1);
    		convertPrimitiveToObject(opcode);

    		if(isInit)
    		{
             	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_INIT_WRITE_ACCESS,
             			Config.instance.DESC_LOG_INIT_WRITE_ACCESS);

    		}
    		else
    		{
            	addBipushInsn(mv,1);
             	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_ARRAY_ACCESS,
             			Config.instance.DESC_LOG_ARRAY_ACCESS);
    		}
    		
    		break;
    	case FASTORE:
    		maxindex_cur++;
        	index1 = maxindex_cur;
        	mv.visitVarInsn(FSTORE, index1);
        	maxindex_cur++;
        	index2 = maxindex_cur;
        	mv.visitVarInsn(ISTORE, index2);
        	
        	mv.visitInsn(DUP);
        	maxindex_cur++;
        	index3 = maxindex_cur;
        	mv.visitVarInsn(ASTORE, index3);//arrayref
        	mv.visitVarInsn(ILOAD, index2);//index
        	mv.visitVarInsn(FLOAD, index1);//value

        	mv.visitInsn(opcode);
        	
        	addBipushInsn(mv,ID);
    		mv.visitVarInsn(ALOAD, index3);
    		mv.visitVarInsn(ILOAD, index2);
    		mv.visitVarInsn(FLOAD, index1);
    		convertPrimitiveToObject(opcode);

    		if(isInit)
    		{
             	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_INIT_WRITE_ACCESS,
             			Config.instance.DESC_LOG_INIT_WRITE_ACCESS);

    		}
    		else
    		{
            	addBipushInsn(mv,1);
             	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_ARRAY_ACCESS,
             			Config.instance.DESC_LOG_ARRAY_ACCESS);
    		}
    		break;
    	case DASTORE:
    		maxindex_cur++;
        	index1 = maxindex_cur;
        	mv.visitVarInsn(DSTORE, index1);maxindex_cur++;
        	mv.visitInsn(DUP2);//dup arrayref and index
        	maxindex_cur++;
        	index2 = maxindex_cur;
        	mv.visitVarInsn(ISTORE, index2);//index
        	maxindex_cur++;
        	index3 = maxindex_cur;
        	mv.visitVarInsn(ASTORE, index3);//arrayref
    
        	mv.visitVarInsn(DLOAD, index1);//double value

        	mv.visitInsn(opcode);
        	
        	addBipushInsn(mv,ID);
    		mv.visitVarInsn(ALOAD, index3);
    		mv.visitVarInsn(ILOAD, index2);
    		mv.visitVarInsn(DLOAD, index1);
    		convertPrimitiveToObject(opcode);

    		if(isInit)
    		{
             	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_INIT_WRITE_ACCESS,
             			Config.instance.DESC_LOG_INIT_WRITE_ACCESS);

    		}
    		else
    		{
            	addBipushInsn(mv,1);
             	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_ARRAY_ACCESS,
             			Config.instance.DESC_LOG_ARRAY_ACCESS);
    		}
    		break;
    	case LASTORE:
    		maxindex_cur++;
        	index1 = maxindex_cur;
        	mv.visitVarInsn(LSTORE, index1);maxindex_cur++;
        	mv.visitInsn(DUP2);//dup arrayref and index
        	maxindex_cur++;
        	index2 = maxindex_cur;
        	mv.visitVarInsn(ISTORE, index2);//index
        	maxindex_cur++;
        	index3 = maxindex_cur;
        	mv.visitVarInsn(ASTORE, index3);//arrayref
    
        	mv.visitVarInsn(LLOAD, index1);//double value

        	mv.visitInsn(opcode);
        	
        	addBipushInsn(mv,ID);
    		mv.visitVarInsn(ALOAD, index3);
    		mv.visitVarInsn(ILOAD, index2);
    		mv.visitVarInsn(LLOAD, index1);
    		convertPrimitiveToObject(opcode);

    		if(isInit)
    		{
             	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_INIT_WRITE_ACCESS,
             			Config.instance.DESC_LOG_INIT_WRITE_ACCESS);

    		}
    		else
    		{
            	addBipushInsn(mv,1);
             	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_ARRAY_ACCESS,
             			Config.instance.DESC_LOG_ARRAY_ACCESS);
    		}
    		break;
    	case MONITORENTER:
    		mv.visitInsn(DUP);
        	maxindex_cur++;
        	int index = maxindex_cur;
        	mv.visitVarInsn(ASTORE, index);//objectref
        	mv.visitInsn(opcode);
        	addBipushInsn(mv,ID);
    		mv.visitVarInsn(ALOAD, index);
         	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_LOCK_INSTANCE,
         			Config.instance.DESC_LOG_LOCK_INSTANCE);
    		break;
    	case MONITOREXIT:
    		mv.visitInsn(DUP);
        	maxindex_cur++;
        	index = maxindex_cur;
        	mv.visitVarInsn(ASTORE, index);//objectref
        	addBipushInsn(mv,ID);
    		mv.visitVarInsn(ALOAD, index);
         	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_UNLOCK_INSTANCE,
         			Config.instance.DESC_LOG_UNLOCK_INSTANCE);
        	mv.visitInsn(opcode);
    		break;
    	case IRETURN:
    	case LRETURN:
    	case FRETURN:
    	case DRETURN:
    	case ARETURN:
    	case RETURN:
    	case ATHROW:
    		if(isSynchronized)
    		{
    	    	addBipushInsn(mv,ID);

    			if(isStatic)
    			{
        	    	//signature + line number
        	    	String sig_var = (classname+".0").replace("/", ".");
        	    	int SID = GlobalStateForInstrumentation.instance.getVariableId(sig_var);
        	    	addBipushInsn(mv,SID);
                 	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_UNLOCK_STATIC,
                 			Config.instance.DESC_LOG_UNLOCK_STATIC);
    			}
    			else
    			{
    				mv.visitVarInsn(ALOAD, 0);//the this objectref
                 	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_UNLOCK_INSTANCE,
                 			Config.instance.DESC_LOG_UNLOCK_INSTANCE);
    			}
    		}
            mv.visitInsn(opcode);break;
    	default:
            mv.visitInsn(opcode);break;
        }
    }
    public void visitCode() {
        mv.visitCode();
        
    	String sig_loc = (classname+"|"+methodsignature+"|"+line_cur).replace("/", ".");
    	int ID  = GlobalStateForInstrumentation.instance.getLocationId(sig_loc);
    	if(isSynchronized)
		{
	    	addBipushInsn(mv,ID);

			if(isStatic)
			{
    	    	//signature + line number
    	    	String sig_var = (classname+".0").replace("/", ".");
    	    	int SID = GlobalStateForInstrumentation.instance.getVariableId(sig_var);
    	    	addBipushInsn(mv,SID);
             	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_LOCK_STATIC,
             			Config.instance.DESC_LOG_LOCK_STATIC);
			}
			else
			{
				mv.visitVarInsn(ALOAD, 0);//the this objectref
             	mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_LOCK_INSTANCE,
             			Config.instance.DESC_LOG_LOCK_INSTANCE);
			}
		}

    }
    public void visitJumpInsn(int opcode, Label label) {
    	String sig_loc = (classname+"|"+methodsignature+"|"+line_cur).replace("/", ".");
    	int ID  = GlobalStateForInstrumentation.instance.getLocationId(sig_loc);

    	switch (opcode) {
    	case IFEQ://branch
    	case IFNE:
    	case IFLT:
    	case IFGE:
    	case IFGT:
    	case IFLE:
    	case IF_ICMPEQ:
    	case IF_ICMPNE:
    	case IF_ICMPLT:
    	case IF_ICMPGE:
    	case IF_ICMPGT:
    	case IF_ICMPLE:
    	case IF_ACMPEQ:
    	case IF_ACMPNE:
    	case IFNULL:
    	case IFNONNULL:
    		addBipushInsn(mv,ID);
    		mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_BRANCH,
         			Config.instance.DESC_LOG_BRANCH);
    	default:
            mv.visitJumpInsn(opcode, label);break;
        }
    }
    
    public void visitTableSwitchInsn(int min, int max, Label dflt,
            Label... labels) {
    	String sig_loc = (classname+"|"+methodsignature+"|"+line_cur).replace("/", ".");
    	int ID  = GlobalStateForInstrumentation.instance.getLocationId(sig_loc);
		addBipushInsn(mv,ID);
		mv.visitMethodInsn(INVOKESTATIC, Config.instance.logClass, Config.instance.LOG_BRANCH,
     			Config.instance.DESC_LOG_BRANCH);
		
		mv.visitTableSwitchInsn(min, max, dflt, labels);
    }
}
