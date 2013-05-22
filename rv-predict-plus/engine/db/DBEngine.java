package db;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import trace.*;
import trace.AbstractNode.TYPE;
import violation.IViolation;
import z3.Z3Engine;

public class DBEngine {

	private long globalEventID=0;
	
	private final String dbname = "RVDatabase";
	private final String driver = "org.h2.Driver";	
	public String appname = "test";
	
	private final String[] stmtsigtablecolname={"SIG","ID"};
	private final String[] stmtsigtablecoltype={"VARCHAR","INT"};
	
	private final String[] scheduletablecolname ={"ID","SIG","SCHEDULE"};
	private final String[] scheduletablecoltype ={"INT","VARCHAR","ARRAY"};
	
	private final String[] sharedvarsigtablecolname={"SIG","ID"};
	private final String[] sharedvarsigcoltype={"VARCHAR","INT"};

	private final String[] tracetablecolname={"GID","TID","ID","ADDR","VALUE","TYPE"};
	private final String[] tracetablecoltype={"BIGINT","BIGINT","INT","VARCHAR","VARCHAR","TINYINT"};
	
	private final String[] tidtablecolname={"TID","NAME"};
	private final String[] tidtablecoltype={"BIGINT","VARCHAR"};
	
	//READ,WRITE,LOCK,UNLOCK,WAIT,NOTIFY,START,JOIN,BRANCH,BB
	public final byte[] tracetypetable ={'0','1','2','3','4','5','6','7','8','9','a'};
	private Connection conn;
	private PreparedStatement prepStmt;
	
	private PreparedStatement prepStmt2;//just for thread id-name

	public String tracetablename;
	public String tidtablename;
	public String stmtsigtablename;
	public String sharedvarsigtablename;
	public String scheduletablename;
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

	/*
	 * must be synchronized?? 
	 * otherwise, easy to throw Unique index or primary key violation
	 */
	public synchronized void saveEventToDB(long TID, int ID, String ADDR, String VALUE, byte TYPE)
	{
		try
		{
			globalEventID=globalEventID+1;
			
			prepStmt.setLong(1, globalEventID);
			prepStmt.setLong(2, TID);
			prepStmt.setInt(3, ID);
			prepStmt.setString(4, ADDR);
			prepStmt.setString(5, VALUE);
			prepStmt.setByte(6, TYPE);
			
			prepStmt.execute();
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void connectDB() throws Exception
	{
		Class.forName(driver);
        conn  = DriverManager.getConnection("jdbc:h2:"+Util.getUserHomeDirectory()+dbname);

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
	public Trace getTrace() throws Exception
	{
		return getTrace(1,0);
	}
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
	        
	        switch(TYPE)
	        {
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
	        
	        //System.out.println(node);
	    }
	    
	    trace.finishedLoading();
	    
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
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String appname = "emp.Simple";//emp.Example stringbuffer.StringBufferTest
		try{
			DBEngine db = new DBEngine(appname);
			
			HashMap<Integer, String> sharedVarIdSigMap = db.getSharedVarSigIdMap();
			
			HashMap<Integer, String> stmtIdSigMap = db.getStmtSigIdMap();
			
			Trace trace = db.getTrace();
			
			HashMap<Long,String> threadIdNameMap = db.getThreadIdNameMap();
			trace.setThreadIdNameMap(threadIdNameMap);			
			
			
			Z3Engine engine = new Z3Engine(appname);
			//1. declare all variables 
			engine.declareVariables(trace.getFullTrace());
			//2. intra-thread order for all nodes, excluding branches and basic block transitions
			engine.addIntraThreadConstraints(trace.getThreadNodesMap());
			//3. order for locks, signals, fork/joins
			engine.addSynchronizationConstraints(trace, trace.getSyncNodesMap(),trace.getThreadFirstNodeMap(),trace.getThreadLastNodeMap());
			//4. match read-write
			engine.addReadWriteConstraints(trace.getIndexedReadNodes(),trace.getIndexedWriteNodes());
			
		}
		catch(Exception e)
		  {
			  e.printStackTrace();
		  }
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
