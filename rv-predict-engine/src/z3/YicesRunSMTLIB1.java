package z3;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import config.Configuration;
import config.Util;

/**
 * Constraint solving with Yices.
 * The generated constraint is in the SMTLIB v1.2 format.
 * It should work with other standard solvers.
 * 
 * @author jeffhuang
 *
 */
public class YicesRunSMTLIB1 extends Z3Run
{
	protected static String SMT = ".smt";
	protected static String OUT = ".yicesout";
	
	File yicesOutFile,yicesErrFile;
	
	public YicesRunSMTLIB1(Configuration config, int id)
	{				
			super(config,id);

	}
	
	public void init(Configuration config, int id) throws IOException
	{		
		smtFile = Util.newOutFile(config.constraint_outdir,config.tableName +"_"+id+SMT);
        
		yicesOutFile = Util.newOutFile(config.constraint_outdir,config.tableName +"_"+id+OUT);
				
		CMD = "yices-smt -m -t "+config.solver_timeout+" ";
	}
	public void sendMessage(String msg)
	{
		PrintWriter smtWriter = null;
		try{
			smtWriter = Util.newWriter(smtFile, true);
			smtWriter.println(msg);
		    smtWriter.close();
		    
	        exec(yicesOutFile, yicesErrFile, smtFile.getAbsolutePath());

	        model = YicesModelReaderSMTLIB1.read(yicesOutFile);
	        
	        if(model!=null)
	        {
	        	sat = true;
	        	schedule = computeSchedule(model);
	        }
	        //String z3OutFileName = z3OutFile.getAbsolutePath();
	        //retrieveResult(z3OutFileName);
		    
		}catch(IOException e)
		{
			System.err.println(e.getMessage());

		}
	}
	
}
