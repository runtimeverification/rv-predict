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
package rvpredict.logging;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Engine for interacting with database.
 * 
 * @author jeffhuang
 *
 */
public class DBEngine {

	protected static long globalEventID=0;
	protected static long DBORDER=0;//handle strange classloader

	//currently we use the h2 database
	protected final String dbname = "RVDatabase";
	protected final String driver = "org.h2.Driver";	
	public String appname = "test";
	
	//database schema
	protected final String[] stmtsigtablecolname={"SIG","ID"};
	protected final String[] stmtsigtablecoltype={"VARCHAR","INT"};
	
	protected final String[] scheduletablecolname ={"ID","SIG","SCHEDULE"};
	protected final String[] scheduletablecoltype ={"INT","VARCHAR","ARRAY"};
	
	protected final String[] sharedvarsigtablecolname={"SIG","ID"};
	protected final String[] sharedvarsigcoltype={"VARCHAR","INT"};

	protected final String[] volatilesigtablecolname={"SIG","ID"};
	protected final String[] volatilesigcoltype={"VARCHAR","INT"};
	
	protected final String[] tracetablecolname={"GID","TID","ID","ADDR","VALUE","TYPE"};
	protected final String[] tracetablecoltype={"BIGINT","BIGINT","INT","VARCHAR","VARCHAR","TINYINT"};
	
	protected final String[] tidtablecolname={"TID","NAME"};
	protected final String[] tidtablecoltype={"BIGINT","VARCHAR"};
	
	protected final String[] propertytablecolname={"PROPERTY","ID"};
	protected final String[] propertytablecoltype={"VARCHAR","INT"};
	
	//READ,WRITE,LOCK,UNLOCK,WAIT,NOTIFY,START,JOIN,BRANCH,BB
	public final byte[] tracetypetable ={'0','1','2','3','4','5','6','7','8','9','a','b'};
	protected Connection conn;
	protected PreparedStatement prepStmt;
	
	protected PreparedStatement prepStmt2;//just for thread id-name

	public String tracetablename;
	public String tidtablename;
	public String stmtsigtablename;
	public String sharedvarsigtablename;
	public String volatilesigtablename;

	public String scheduletablename;
	public String propertytablename;

	
	class EventItem
	{
		long GID;
		long TID;
		int ID;
		String ADDR;
		String VALUE;
		byte TYPE;
		
		EventItem(long gid, long tid, int sid, String addr, String value, byte type)
		{
			this.GID = gid;
			this.TID = tid;
			this.ID = sid;
			this.ADDR = addr;
			this.VALUE = value;
			this.TYPE = type;
		}
	}
	public DBEngine(String name)
	{
		name = name.replace('.', '_');
		appname = name;
		tracetablename = "trace_"+name;
		tidtablename = "tid_"+name;
		volatilesigtablename = "volatile_"+name;
		stmtsigtablename="stmtsig_"+name;
		sharedvarsigtablename="sharedvarsig_"+name;
		scheduletablename = "schedule_"+name;
		propertytablename = "property_"+name;
		try
		{
			connectDB();
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	public void closeDB()
	{
		try {
			conn.createStatement().execute("SHUTDOWN");

			//conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public void saveProperty(String name, int ID, boolean dropTable)
	{
		try{
			if(dropTable)createPropertyTable();
	    	String sql_insertdata = "INSERT INTO "+propertytablename+" VALUES (?,?)";
	    	PreparedStatement prepStmt = conn.prepareStatement(sql_insertdata);
	    	prepStmt.setString(1, name);
	    	prepStmt.setInt(2, ID);
	    	prepStmt.execute();
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	public void createPropertyTable() throws Exception
	{
		String sql_dropTable = "DROP TABLE IF EXISTS "+propertytablename;

    	Statement stmt = conn.createStatement();
        stmt.execute(sql_dropTable);
        
        String sql_createTable = "CREATE TABLE IF NOT EXISTS "+propertytablename+" ("+
        		propertytablecolname[0]+" "+propertytablecoltype[0]+", "+
        		propertytablecolname[1]+" "+propertytablecoltype[1]+")";
        stmt.execute(sql_createTable);
        
	}
	public void createScheduleTable() throws Exception
	{
		String sql_dropTable = "DROP TABLE IF EXISTS "+scheduletablename;
    	String sql_insertdata = "INSERT INTO "+scheduletablename+" VALUES (?,?,?)";

    	Statement stmt = conn.createStatement();
        stmt.execute(sql_dropTable);
        
        String sql_createTable = "CREATE TABLE "+scheduletablename+" ("+
        		scheduletablecolname[0]+" "+scheduletablecoltype[0]+" PRIMARY KEY, "+
        		scheduletablecolname[1]+" "+scheduletablecoltype[1]+", "+
        		scheduletablecolname[2]+" "+scheduletablecoltype[2]+")";
        stmt.execute(sql_createTable);
        
        prepStmt = conn.prepareStatement(sql_insertdata);
	}
	public void createStmtSignatureTable() throws Exception
	{
		String sql_dropTable = "DROP TABLE IF EXISTS "+stmtsigtablename;
    	String sql_insertdata = "INSERT INTO "+stmtsigtablename+" VALUES (?,?)";

    	Statement stmt = conn.createStatement();
        stmt.execute(sql_dropTable);
        
        String sql_createTable = "CREATE TABLE "+stmtsigtablename+" ("+
        		stmtsigtablecolname[0]+" "+stmtsigtablecoltype[0]+" PRIMARY KEY, "+
        		stmtsigtablecolname[1]+" "+stmtsigtablecoltype[1]+")";
        stmt.execute(sql_createTable);
        
        prepStmt = conn.prepareStatement(sql_insertdata);
	}
	public void createSharedVarSignatureTable() throws Exception
	{
		String sql_dropTable = "DROP TABLE IF EXISTS "+sharedvarsigtablename;
    	String sql_insertdata = "INSERT INTO "+sharedvarsigtablename+" VALUES (?,?)";

    	Statement stmt = conn.createStatement();
        stmt.execute(sql_dropTable);
        
        String sql_createTable = "CREATE TABLE "+sharedvarsigtablename+" ("+
        		sharedvarsigtablecolname[0]+" "+sharedvarsigcoltype[0]+" PRIMARY KEY, "+
        		sharedvarsigtablecolname[1]+" "+sharedvarsigcoltype[1]+")";
        stmt.execute(sql_createTable);
        
        prepStmt = conn.prepareStatement(sql_insertdata);
	}
	public void createVolatileSignatureTable() throws Exception
	{
		String sql_dropTable = "DROP TABLE IF EXISTS "+volatilesigtablename;
    	String sql_insertdata = "INSERT INTO "+volatilesigtablename+" VALUES (?,?)";

    	Statement stmt = conn.createStatement();
        stmt.execute(sql_dropTable);
        
        String sql_createTable = "CREATE TABLE "+volatilesigtablename+" ("+
        		volatilesigtablecolname[0]+" "+volatilesigcoltype[0]+" PRIMARY KEY, "+
        		volatilesigtablecolname[1]+" "+volatilesigcoltype[1]+")";
        stmt.execute(sql_createTable);
        
        prepStmt = conn.prepareStatement(sql_insertdata);
	}
	public void createTraceTable(boolean newTable) throws Exception
	{
    	Statement stmt = conn.createStatement();
    	if(newTable)
    	{
    		String sql_dropTable = "DROP TABLE IF EXISTS "+tracetablename;
    		stmt.execute(sql_dropTable);
    	}
                
		String sql_createTable = "CREATE TABLE IF NOT EXISTS "+tracetablename+" ("+
		        tracetablecolname[0]+" "+tracetablecoltype[0]+" AUTO_INCREMENT, "+//PRIMARY KEY
		        tracetablecolname[1]+" "+tracetablecoltype[1]+", "+
		        tracetablecolname[2]+" "+tracetablecoltype[2]+", "+
		        tracetablecolname[3]+" "+tracetablecoltype[3]+", "+
		        tracetablecolname[4]+" "+tracetablecoltype[4]+", "+
		        tracetablecolname[5]+" "+tracetablecoltype[5]+")";
		        stmt.execute(sql_createTable);
		        
    	String sql_insertdata = "INSERT INTO "+tracetablename+" ( "+tracetablecolname[1]+", "+
    			tracetablecolname[2]+", "+
    			tracetablecolname[3]+", "+
    			tracetablecolname[4]+", "+
    			tracetablecolname[5]+" "+" ) VALUES (?,?,?,?,?)";
        prepStmt = conn.prepareStatement(sql_insertdata);
		
	}
	
	public void createThreadIdTable(boolean newTable) throws Exception
	{
    	Statement stmt = conn.createStatement();

		if(newTable)
    	{
			String sql_dropTable = "DROP TABLE IF EXISTS "+tidtablename;
	
	        stmt.execute(sql_dropTable);
    	}
		
        String sql_createTable = "CREATE TABLE IF NOT EXISTS "+tidtablename+" ("+
        tidtablecolname[0]+" "+tidtablecoltype[0]+", "+//PRIMARY KEY
        tidtablecolname[1]+" "+tidtablecoltype[1]+")";
        stmt.execute(sql_createTable);
        
    	String sql_insertdata = "INSERT INTO "+tidtablename+" VALUES (?,?)";
        prepStmt2 = conn.prepareStatement(sql_insertdata);
	}
	public void saveThreadTidNameToDB(long id, String name)
	{
		try
		{
			prepStmt2.setLong(1, id);
			prepStmt2.setString(2,name);
		
			prepStmt2.execute();
		
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	public void saveStmtSignatureToDB(String sig, int id)
	{
		try
		{
		prepStmt.setString(1, sig);
		prepStmt.setInt(2,id);

		prepStmt.execute();
		
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	public void saveSharedVarSignatureToDB(String sig, int id)
	{
		try
		{
		prepStmt.setString(1, sig);
		prepStmt.setInt(2,id);

		prepStmt.execute();

		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	public void saveVolatileSignatureToDB(String sig, int id)
	{
		try
		{
		prepStmt.setString(1, sig);
		prepStmt.setInt(2,id);

		prepStmt.execute();

		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	/**
	 * save an event to database. must be synchronized. 
	 * otherwise, easy to throw Unique index or primary key violation.
	 */
	public synchronized void saveEventToDB(long TID, int ID, String ADDR, String VALUE, byte TYPE)
	{
		//for testing only
		//if(TID==1)return;
		
		//make true->1. false->0
		if(VALUE.equals("true"))
			VALUE="1";
		if(VALUE.equals("false"))
			VALUE="0";
		
		//globalEventID=globalEventID+1;
		 //System.out.println(globalEventID+" "+TID+" "+ADDR+" "+VALUE+" "+TYPE);

		try
		{
/*			//prepStmt.setLong(1, globalEventID);
			prepStmt.setLong(2, TID);
			prepStmt.setInt(3, ID);
			prepStmt.setString(4, ADDR);
			prepStmt.setString(5, VALUE);
			prepStmt.setByte(6, TYPE);
*/
			prepStmt.setLong(1, TID);
			prepStmt.setInt(2, ID);
			prepStmt.setString(3, ADDR);
			prepStmt.setString(4, VALUE);
			prepStmt.setByte(5, TYPE);
			
			prepStmt.execute();
		

			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	protected void connectDB() throws Exception
	{
		try{
		Class.forName(driver);
        conn  = DriverManager.getConnection("jdbc:h2:"+Util.getUserHomeDirectory()+dbname+";AUTO_SERVER=true");//
        //conn.setAutoCommit(true);
        //check if Database may be already in use
        //kill?
		}catch(org.h2.jdbc.JdbcSQLException e)
		{
			e.printStackTrace();
			//DBORDER++;
	        //conn  = DriverManager.getConnection("jdbc:h2:"+Util.getUserHomeDirectory()+dbname+DBORDER);//+";AUTO_SERVER=true"
		}
	}

}
