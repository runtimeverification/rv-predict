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
package db;
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

import trace.*;
import trace.AbstractNode.TYPE;
import violation.IViolation;
import z3.Z3Engine;

/**
 * Engine for interacting with database.
 * 
 * @author jeffhuang
 *
 */
public class DBEngine {

	protected long globalEventID=0;
	
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

	protected final String[] tracetablecolname={"GID","TID","ID","ADDR","VALUE","TYPE"};
	protected final String[] tracetablecoltype={"BIGINT","BIGINT","INT","VARCHAR","VARCHAR","TINYINT"};
	
	protected final String[] tidtablecolname={"TID","NAME"};
	protected final String[] tidtablecoltype={"BIGINT","VARCHAR"};
	
	//READ,WRITE,LOCK,UNLOCK,WAIT,NOTIFY,START,JOIN,BRANCH,BB
	public final byte[] tracetypetable ={'0','1','2','3','4','5','6','7','8','9','a'};
	protected Connection conn;
	protected PreparedStatement prepStmt;
	
	protected PreparedStatement prepStmt2;//just for thread id-name

	public String tracetablename;
	public String tidtablename;
	public String stmtsigtablename;
	public String sharedvarsigtablename;
	public String scheduletablename;
	
	//TODO: What if the program does not terminate??
	
	protected BlockingQueue<Stack<EventItem>> queue;
	//we can also use our own Stack implementation here 
	protected Stack<EventItem> buffer;
	protected Object dblock = new Object();
	
	protected int BUFFER_THRESHOLD;
	protected boolean asynchronousLogging;
	
	//private final String NO_AUTOCLOSE = ";DB_CLOSE_ON_EXIT=FALSE";//BUGGY in H2, DON'T USE IT
	
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
	 
	public void finishLogging()
	{
		//should wait for logging thread to finish
		while(!queue.isEmpty());
		
		saveEventsToDB(buffer);
		closeDB();
	}
	public void saveEventsToDB(Stack<EventItem> stack)
	{
		synchronized(dblock)
		{
		while(!stack.isEmpty())
		{
			EventItem item = stack.pop();
			//System.out.println(item.GID);
		try
		{
			prepStmt.setLong(1, item.GID);
			prepStmt.setLong(2, item.TID);
			prepStmt.setInt(3, item.ID);
			prepStmt.setString(4, item.ADDR);
			prepStmt.setString(5, item.VALUE);
			prepStmt.setByte(6, item.TYPE);
			
			prepStmt.execute();
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		}
		}
	}
	public void startAsynchronousLogging()
	{
		asynchronousLogging = true;
		
		queue = new LinkedBlockingQueue<Stack<EventItem>>();
		buffer = new Stack<EventItem>();
		BUFFER_THRESHOLD = 100000;//TODO: make it configurable
				
		Thread t = new Thread(new Runnable()
		{

			@Override
			public void run() {
				
				
				while(true)
				{
					try {
						Stack<EventItem> stack = queue.take();
						saveEventsToDB(stack);
						
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
		});
		
		t.setDaemon(true);
		
		t.start();
		
	}
	public DBEngine(String name)
	{
		name = name.replace('.', '_');
		appname = name;
		tracetablename = "trace_"+name;
		tidtablename = "tid_"+name;
		stmtsigtablename="stmtsig_"+name;
		sharedvarsigtablename="sharedvarsig_"+name;
		scheduletablename = "schedule_"+name;
		try
		{
			connectDB();
		}catch(Exception e)
		{
			System.err.println(e.getMessage());
		}
	}
	public void closeDB()
	{
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
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
	public void createTraceTable() throws Exception
	{
		String sql_dropTable = "DROP TABLE IF EXISTS "+tracetablename;

    	Statement stmt = conn.createStatement();
        stmt.execute(sql_dropTable);
        
        String sql_createTable = "CREATE TABLE "+tracetablename+" ("+
        tracetablecolname[0]+" "+tracetablecoltype[0]+" PRIMARY KEY, "+
        tracetablecolname[1]+" "+tracetablecoltype[1]+", "+
        tracetablecolname[2]+" "+tracetablecoltype[2]+", "+
        tracetablecolname[3]+" "+tracetablecoltype[3]+", "+
        tracetablecolname[4]+" "+tracetablecoltype[4]+", "+
        tracetablecolname[5]+" "+tracetablecoltype[5]+")";
        stmt.execute(sql_createTable);
        
    	String sql_insertdata = "INSERT INTO "+tracetablename+" VALUES (?,?,?,?,?,?)";
        prepStmt = conn.prepareStatement(sql_insertdata);
		
	}
	
	public void createThreadIdTable() throws Exception
	{
		String sql_dropTable = "DROP TABLE IF EXISTS "+tidtablename;

    	Statement stmt = conn.createStatement();
        stmt.execute(sql_dropTable);
        
        String sql_createTable = "CREATE TABLE "+tidtablename+" ("+
        tidtablecolname[0]+" "+tidtablecoltype[0]+" PRIMARY KEY, "+
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

	/**
	 * save an event to database. must be synchronized. 
	 * otherwise, easy to throw Unique index or primary key violation.
	 */
	public synchronized void saveEventToDB(long TID, int ID, String ADDR, String VALUE, byte TYPE)
	{
		//make true->1. false->0
		if(VALUE.equals("true"))
			VALUE="1";
		if(VALUE.equals("false"))
			VALUE="0";
		
		globalEventID=globalEventID+1;
		
//		if(globalEventID%1000000==0)
//			System.out.println("logging the "+globalEventID+"th event...");
		
		if(asynchronousLogging)
		{
			EventItem item = new EventItem(globalEventID,TID,ID,ADDR,VALUE,TYPE);
			if(buffer.size()==BUFFER_THRESHOLD)
			{
				queue.add(buffer);
				buffer = new Stack<EventItem>();
			}
			else
			{
				buffer.add(item);
			}
		}
		else
		{
			try
			{
				
				
				prepStmt.setLong(1, globalEventID);
				prepStmt.setLong(2, TID);
				prepStmt.setInt(3, ID);
				prepStmt.setString(4, ADDR);
				prepStmt.setString(5, VALUE);
				prepStmt.setByte(6, TYPE);
				
				prepStmt.execute();
				
				  //if(ADDR.length()>0&&ADDR.charAt(ADDR.length()-1)=='3')
				//	  System.out.println(globalEventID+" "+TID+" "+ADDR+" "+VALUE+" "+TYPE);

				
			}catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	protected void connectDB() throws Exception
	{
		Class.forName(driver);
        conn  = DriverManager.getConnection("jdbc:h2:"+Util.getUserHomeDirectory()+dbname);
        //conn.setAutoCommit(true);
	}
	
	 public HashMap<Long, String> getThreadIdNameMap()
	{	
		 HashMap<Long, String> map = new HashMap<Long, String>();
		 
		 try{
				String sql_select = "SELECT * FROM "+tidtablename;
				
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery(sql_select);
				while (rs.next()) 
			    {
			        // Get the data from the row using the column index
			        Long tid = rs.getLong(1);
			        String name = rs.getString(2);

			        //System.out.println(ID+"-"+SIG);
			        map.put(tid, name);
			    }
				}catch(Exception e)
				{
					e.printStackTrace();
				}
				return map;
	}
	public HashMap<Integer, String> getSharedVarSigIdMap()
	{
		HashMap<Integer, String> map = new HashMap<Integer, String>();
		try{
		String sql_select = "SELECT * FROM "+sharedvarsigtablename;
		
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql_select);
		while (rs.next()) 
	    {
	        // Get the data from the row using the column index
	        String SIG = rs.getString(1);
	        Integer ID = rs.getInt(2);
	        //System.out.println(ID+"-"+SIG);
	        map.put(ID, SIG);
	    }
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		return map;
	}
	public HashMap<Integer, String> getStmtSigIdMap()
	{
		HashMap<Integer, String> map = new HashMap<Integer, String>();
		try{
		String sql_select = "SELECT * FROM "+stmtsigtablename;
		
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql_select);
		while (rs.next()) 
	    {
	        // Get the data from the row using the column index
	        String SIG = rs.getString(1);
	        Integer ID = rs.getInt(2);
	        //System.out.println(ID+"-"+SIG);
	        map.put(ID, SIG);
	    }
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		return map;
	}
	
	public int getTraceThreadNumber() throws Exception
	{
		int number = 0;
		String sql_select = "SELECT COUNT(DISTINCT "+tracetablecolname[1]+") FROM "+tracetablename;
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql_select);
		if(rs.next())
			number = rs.getInt(1);
		return number;
	}
	public int getTraceSharedVariableNumber() throws Exception
	{
		int number = 0;
		String sql_select = "SELECT COUNT(DISTINCT "+tracetablecolname[3]+") FROM "+tracetablename;
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql_select);
		if(rs.next())
			number = rs.getInt(1);
		return number;
	}
	
	public int getTraceBranchNumber() throws Exception
	{
		int number = 0;
		String sql_select = "SELECT COUNT(*) FROM "+tracetablename +" WHERE "+tracetablecolname[5]+" = "+tracetypetable[9];
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql_select);
		if(rs.next())
			number = rs.getInt(1);
		return number;
	}
	public int getTraceReadWriteNumber() throws Exception
	{
		int number = 0;
		String sql_select = "SELECT COUNT(*) FROM "+tracetablename +" WHERE "+
								tracetablecolname[5]+" in ("+tracetypetable[1]+
								", "+tracetypetable[2]+")";
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql_select);
		if(rs.next())
			number = rs.getInt(1);
		return number;
	}
	public int getTraceSyncNumber() throws Exception
	{
		int number = 0;
		String sql_select = "SELECT COUNT(*) FROM "+tracetablename +" WHERE "+
								tracetablecolname[5]+" > "+tracetypetable[1]+
								" AND "+tracetablecolname[5]+" < "+tracetypetable[8];
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql_select);
		if(rs.next())
			number = rs.getInt(1);
		return number;
	}
	
	public long getTraceSize() throws Exception
	{
		long size = 0;
		
		String sql_select = "SELECT COUNT(*) FROM "+tracetablename;
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql_select);
		if(rs.next())
			size = rs.getLong(1);
		return size;
	}
	
	/**
	 * load all trace
	 * @return
	 * @throws Exception
	 */
	public Trace getTrace() throws Exception
	{
		return getTrace(1,0);
	}
	/**
	 * load trace from event min to event max
	 * @param min
	 * @param max
	 * @return
	 * @throws Exception
	 */
	public Trace getTrace(long min, long max) throws Exception
	{
		String sql_select = "SELECT * FROM "+tracetablename;
		if(max>min)
			sql_select+=" WHERE GID BETWEEN '"+min+"' AND '"+max+"'";
		sql_select+=" ORDER BY GID";
		
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql_select);
		
		Trace trace = new Trace();
		AbstractNode node = null;
//		int NUM_CS = 0;
//		long lastTID = -1;
		 // Fetch each row from the result set
	    while (rs.next()) 
	    {
	        // Get the data from the row using the column index
	        long GID = rs.getLong(1);
	        long TID = rs.getLong(2);
	        int ID = rs.getInt(3);
	        String ADDR = rs.getString(4);
	        String VALUE = rs.getString(5);
	        byte TYPE = rs.getByte(6);
	        
			  //System.out.println(GID+" "+TID+" "+ADDR+" "+VALUE+" "+TYPE);

	        switch(TYPE)
	        {
	        	case '0': node = new InitNode(GID,TID,ID,ADDR,VALUE,AbstractNode.TYPE.INIT); break;
	        	case '1': node = new ReadNode(GID,TID,ID,ADDR,VALUE,AbstractNode.TYPE.READ); break;
	        	case '2': node = new WriteNode(GID,TID,ID,ADDR,VALUE,AbstractNode.TYPE.WRITE); break;
	        	case '3': node = new LockNode(GID,TID,ID,ADDR,AbstractNode.TYPE.LOCK); break;
	        	case '4': node = new UnlockNode(GID,TID,ID,ADDR,AbstractNode.TYPE.UNLOCK); break;
	        	case '5': node = new WaitNode(GID,TID,ID,ADDR,AbstractNode.TYPE.WAIT); break;
	        	case '6': node = new NotifyNode(GID,TID,ID,ADDR,AbstractNode.TYPE.NOTIFY); break;
	        	case '7': node = new StartNode(GID,TID,ID,ADDR,AbstractNode.TYPE.START); break;
	        	case '8': node = new JoinNode(GID,TID,ID,ADDR,AbstractNode.TYPE.JOIN); break;
	        	case '9': node = new BranchNode(GID,TID,ID,AbstractNode.TYPE.BRANCH); break;
	        	case 'a': node = new BBNode(GID,TID,ID,AbstractNode.TYPE.BB); break;

	        	default:break;
	        }
	        
	        trace.addNode(node);
	        
//	        if(TID!=lastTID)
//	        	NUM_CS++;
//	        lastTID=TID;
	    }
	    
	    trace.finishedLoading();
        //System.out.println("Context Switches: "+NUM_CS);

	    return trace;
	}
	
	private void test()
	{
		try{
		DBEngine db = new DBEngine("a.b.c.test");
		db.createSharedVarSignatureTable();
		{
			db.saveStmtSignatureToDB("a", 1);
			db.saveStmtSignatureToDB("a.b", 2);
		}
		
		db.createStmtSignatureTable();
		{
			db.saveStmtSignatureToDB("abc", 1);
			db.saveStmtSignatureToDB("a.b", 2);
		}
		
		db.createTraceTable();
		{
			db.saveEventToDB(1l, 1, "abc","1", db.tracetypetable[1]);
			db.saveEventToDB(1, 2, "ac", "21",db.tracetypetable[2]);
			db.saveEventToDB(1, 3, "ac", "24",db.tracetypetable[3]);
			db.saveEventToDB(1, 4, "ac", "324",db.tracetypetable[4]);
			db.saveEventToDB(1, 5, "ac", "1324",db.tracetypetable[5]);
			db.saveEventToDB(2, 6, "ac", "24",db.tracetypetable[5]);
			db.saveEventToDB(2, 7, "ac", "1324",db.tracetypetable[6]);
			db.saveEventToDB(3, 8, "ac", "34",db.tracetypetable[7]);
			db.saveEventToDB(4, 9, "ac", "14",db.tracetypetable[8]);
			db.saveEventToDB(4, 10, "ac", "14",db.tracetypetable[9]);

			db.closeDB();
		}
		
		 }catch(Exception e)
		  {
			  e.printStackTrace();
		  }
		
	}

	public int getScheduleSize()
	{
		try{
			String sql_select = "SELECT COUNT(*) FROM "+scheduletablename;
			
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(sql_select);
			if (rs.next()) 
		    {
				return rs.getInt(1);
        
		    }
			}catch(Exception e)
			{
				e.printStackTrace();
			}
			return 0;
	}
	public Object[] getSchedule(int id)
	{
		try{
		String sql_select = "SELECT * FROM "+scheduletablename +" WHERE ID="+id;
		
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql_select);
		if (rs.next()) 
	    {
	        Object o = rs.getObject(3);
	        Object[] schedule = (Object[])o;
	         
	        return schedule;	        
	    }
		}catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Save schedules for each violation to database.
	 * The schedule is identified by a unique order.
	 * 
	 * @param violations
	 * @return
	 */
	public int saveSchedulesToDB(HashSet<IViolation> violations) {
		
		Iterator<IViolation> violationIt = violations.iterator();
		
		int i=0;
		while(violationIt.hasNext())
		{
			
			IViolation violation = violationIt.next();
			ArrayList<Vector<String>> schedules = violation.getSchedules();
			
			Iterator<Vector<String>> scheduleIt = schedules.iterator();
			
			while(scheduleIt.hasNext())
			{
				i++;
				Vector<String> schedule = scheduleIt.next();
				try
				{				
					prepStmt.setInt(1, i);
					prepStmt.setString(2, violation.toString());
					//Array aArray = conn.createArrayOf("VARCHAR", schedule.toArray());
					prepStmt.setObject(3,schedule.toArray());
					
					prepStmt.execute();
					
				}catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		
		return i;
		
	}

}
