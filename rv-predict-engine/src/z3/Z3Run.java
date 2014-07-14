/*******************************************************************************
 * Copyright (c) 2013 University of Illinois
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package z3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

import config.Configuration;
import config.Util;

/**
 * Constraint solving with Z3 solver
 * 
 * @author jeffhuang
 *
 */
public class Z3Run
{
	protected static String Z3_SMT2 = ".z3smt2";
	protected static String Z3_OUT = ".z3out";
	protected static String Z3_ERR = ".z3err.";
	
	
	File smtFile,z3OutFile,z3ErrFile;
	protected List<String> CMD;
	
	public Z3Model model;
	public Vector<String> schedule;
		
	boolean sat;

    long timeout;
	
	public Z3Run(Configuration config, int id)
	{				
		try{
			init(config,id);
		
		}catch(IOException e)
		{
			System.err.println(e.getMessage());
		}
	}
	/**
	 * initialize solver configuration
	 * @param config
	 * @param id
	 * @throws IOException
	 */
	protected void init(Configuration config, int id) throws IOException
	{
		
		//constraint file
		smtFile = Util.newOutFile(config.constraint_outdir,config.tableName +"_"+id+Z3_SMT2);
        
		//solution file
		z3OutFile = Util.newOutFile(config.constraint_outdir,config.tableName +"_"+id+Z3_OUT);
		
		//z3ErrFile = Util.newOutFile(Z3_ERR+id);//looks useless
		
		//command line to Z3 solver
		CMD = Arrays.asList("z3", "-memory:"+config.solver_memory, "-smt2");
        timeout = config.solver_timeout;
	}
	
	/**
	 * solve constraint "msg"
	 * @param msg
	 */
	public void sendMessage(String msg)
	{
		PrintWriter smtWriter = null;
		try{
			smtWriter = Util.newWriter(smtFile, true);
			smtWriter.println(msg);
		    smtWriter.close();
		    
		    //invoke the solver
	        exec(z3OutFile, z3ErrFile, smtFile.getAbsolutePath());

	        model = Z3ModelReader.read(z3OutFile);
	        
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
	
	/**
	 * Given the model of solution, return the corresponding schedule
	 * 
	 * @param model
	 * @return
	 */
	public Vector<String> computeSchedule(Z3Model model) {
		
		Vector<String> schedule = new Vector<String>();
		
		Iterator<Entry<String,Object>> setIt = model.getMap().entrySet().iterator();
		while(setIt.hasNext())
		{
			Entry<String,Object> entryModel = setIt.next();
			String op = entryModel.getKey();
			int order = (Integer)entryModel.getValue();
						
			if(schedule.isEmpty())
				schedule.add(op);
			else
			for(int i=0;i<schedule.size();i++)
			{
				if(order<(Integer)model.getMap().get(schedule.get(i)))
				{
					schedule.insertElementAt(op, i);
					break;
				}
				else if(i==schedule.size()-1)
				{
					schedule.add(op);
					break;
				}
				
			}
		}
		
		return schedule;
	}
	
	public void exec(final File outFile, File errFile, String file) throws IOException
	{

		final List<String> cmds = new ArrayList<String>();
        cmds.addAll(CMD);
        cmds.add(file);

        FutureTask<Integer> task = new FutureTask<Integer>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                ProcessBuilder processBuilder = new ProcessBuilder(cmds);
                processBuilder.redirectOutput(outFile);
                Process process = processBuilder.start();
                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    process.destroy();
                }
                return process.exitValue();
            }
        });
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(task);
        try {
            task.get(timeout, TimeUnit.SECONDS);
        } catch (Exception e) {
            task.cancel(true);
            executorService.shutdown();
        }
    }
	
}
