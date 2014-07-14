package z3;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

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
public class SMTLIB1Run extends Z3Run
{
	protected static String SMT = ".smt";
	protected static String OUT = ".smtout";
	
	File outFile, errFile;
	
	public SMTLIB1Run(Configuration config, int id)
	{				
			super(config,id);

	}
	
	public void init(Configuration config, int id) throws IOException
	{		
		smtFile = Util.newOutFile(config.constraint_outdir,config.tableName +"_"+id+SMT);
        
		outFile = Util.newOutFile(config.constraint_outdir,config.tableName +"_"+id+OUT);
				
		CMD = Arrays.asList(config.smt_solver.split(" "));
        timeout = config.solver_timeout;
	}
	public void sendMessage(String msg)
	{
		PrintWriter smtWriter = null;
		try{
			smtWriter = Util.newWriter(smtFile, true);
			smtWriter.println(msg);
		    smtWriter.close();
		    
	        exec(outFile, errFile, smtFile.getAbsolutePath());

	        model = SMTLIB1ModelReader.read(outFile);
	        
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
