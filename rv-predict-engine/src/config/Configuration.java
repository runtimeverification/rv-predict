package config;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Configuration {

	final static String opt_rmm_pso = "pso";
	final static String opt_max_len = "maxlen";
	final static String opt_no_schedule = "noschedule";
	final static String opt_no_branch = "nobranch";
	final static String opt_all_consistent = "allconsistent";

	final static String default_max_len= "1000";
	final static String default_empty= "";
	
	public String appname;
	public long window_size;
	public boolean nobranch;
	public boolean noschedule;
	public boolean allconsistent;
	public boolean rmm_pso;
	
	public Configuration (String[] args) {
	
		try{
		
		if(args.length==0)
		{
			System.err.println("Usage: java NewRVPredict [options] classname");
			System.exit(1);
		}
		

		
		
		//emp.Example stringbuffer.StringBufferTest
		
		//String[] args2 = {"abc","-maxlen","10000","-noschedule","-nobranch"};
		
		// create Options object
		Options options = new Options();

		// add t option
		options.addOption(opt_max_len, true, "window size");
		options.addOption(opt_no_schedule, false, "not report schedule");
		options.addOption(opt_no_branch, false, "use no branch model");
		options.addOption(opt_all_consistent, false, "require all read-write consistent");
		options.addOption(opt_rmm_pso, false, "PSO memory model");

		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse( options, args);
		
		String maxlen = cmd.getOptionValue(opt_max_len,default_max_len);
		window_size = Long.valueOf(maxlen);
		
		noschedule = cmd.hasOption(opt_no_schedule);
		//ok, let's make noschedule by default
		//noschedule = true;
				
		rmm_pso = cmd.hasOption(opt_rmm_pso);
		//rmm_pso = true;
		
		 nobranch = cmd.hasOption(opt_no_branch);
		 allconsistent = cmd.hasOption(opt_all_consistent);
		
		appname = (String) cmd.getArgList().get(0);
		}catch(Exception e)
		{
			e.printStackTrace();
			System.exit(0);
		}

	}
	
	
}
