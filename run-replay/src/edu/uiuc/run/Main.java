package edu.uiuc.run;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import db.DBEngine;

import rvpredict.logging.ReplayRT;

public class Main {
	
	public static void main(String[] args)
	{
		if(args.length==0)
		{
			System.err.println("please specify the application name");
		}
		else 
		{
			run(args);
		}
}
		
private static void run(String[] args)
{
	try 
	{		
		String appname = args[0];
		DBEngine db = new DBEngine(appname);
		int size = db.getScheduleSize();
		
		//the second argument is the schedule id
		if(args.length>1)
		{
			int id = Integer.valueOf(args[1]);
			Object[] schedule = null;
			if(id<=size) schedule = db.getSchedule(id);
			db.closeDB();
			if(schedule==null)
			{
				//no schedule to replay, just terminate
				System.exit(0);
			}
			
			ReplayRT.init(appname,schedule);
	
			Class<?> c = Class.forName(appname);
			
		    Class[] argTypes = new Class[] { String[].class };
		    Method main = c.getDeclaredMethod("main", argTypes);
		   
		    String[] mainArgs = {};
	
		    if(args.length>1)
		    {
		    	mainArgs = new String[args.length-1];
		    	for(int k=0;k<args.length-1;k++)
		    		mainArgs[k] = args[k+1];
		    }
		    
		    System.out.println("\n------------ Replaying "+appname+" with schedule "+id+" ---------------\n");
		    
		    main.invoke(null, (Object)mainArgs);
			// production code should handle these exceptions more gracefully
		}
		else
		{
			//TODO: test this part
			
			//if the schedule id is not specified, we will explore all schedules in the database
			db.closeDB();
			for(int id=1;id<=size;id++)
			{
				ProcessBuilder pb = new ProcessBuilder("java", "-cp", "tmp/replay:rv-replayer.jar", "edu.uiuc.run.Main",appname+" "+id);
				pb.redirectErrorStream();
				
				InputStream is = null;
		        try {

		            Process process = pb.start();
		            process.waitFor();
		            
		            is = process.getInputStream();

		            int value;
		            while ((value = is.read()) != -1) {

		                char inChar = (char)value;
		                System.out.print(inChar);

		            }

		        } catch (IOException ex) {
		            ex.printStackTrace();
		        }        

			}
		    
		}
		}catch (Exception x) {
			    x.printStackTrace();
			}
		}
}
