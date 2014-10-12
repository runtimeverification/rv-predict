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
import rvpredict.util.Metadata;

import java.sql.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import trace.*;
import violation.IViolation;

/**
 * Engine for interacting with database.
 * 
 * @author jeffhuang
 *
 */
public class DBEngine {

    public static boolean shutDown = false;
	protected long globalEventID=0;
	
	//currently we use the h2 database
	protected final String dbname = "RVDatabase";
    private final int TABLE_NOT_FOUND_ERROR_CODE = 42102;
	public String appname = "main";
	
	//database schema
	protected final String[] stmtsigtablecolname={"SIG","ID"};
	protected final String[] stmtsigtablecoltype={"VARCHAR","INT"};
	
	protected final String[] scheduletablecolname ={"ID","SIG","SCHEDULE"};
	protected final String[] scheduletablecoltype ={"INT","VARCHAR","ARRAY"};
	
    protected final String[] sharedvarsigtablecolname = {"SIG"};
    protected final String[] sharedvarsigcoltype = {"VARCHAR"};

    protected final String[] sharedarrayloctablecolname = {"SIG"};
    protected final String[] sharedarrayloccoltype = {"VARCHAR"};

    protected final String[] varsigtablecolname = {"SIG", "ID"};
    protected final String[] varsigcoltype = {"VARCHAR", "INT"};

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

    Metadata state;

	public String tracetablename;
	public String tidtablename;
	public String stmtsigtablename;
	public String sharedvarsigtablename;
	public String volatilesigtablename;

	public String scheduletablename;
	public String propertytablename;
	private String varsigtablename;
    private String sharedarrayloctablename;

	//TODO: What if the program does not terminate??
	
	protected BlockingQueue<Stack<EventItem>> queue;
	//we can also use our own Stack implementation here 
	protected Stack<EventItem> buffer;
	protected Object dblock = new Object();
	
	protected int BUFFER_THRESHOLD;
	protected boolean asynchronousLogging;
	
    public void saveMetaData(boolean isVerbose)
    {
        if (state == null) return;
        ConcurrentHashMap<String, Integer> variableIdMap = state.getUnsavedVariableIdMap();
        ConcurrentHashMap<String, Boolean> volatileVariables = state.getUnsavedVolatileVariables();
        ConcurrentHashMap<String, Integer> stmtSigIdMap = state.getUnsavedStmtSigIdMap();
        saveMetaData(isVerbose, variableIdMap, volatileVariables, stmtSigIdMap);
    }

    public void saveSharedMetaData(boolean isVerbose, HashSet<String> sharedVariables,
                                   HashSet<String> sharedArrayLocations) {

        try {
            if(isVerbose)
                System.out.println("====================SHARED VARIABLES===================");

            createSharedVarSignatureTable(false);
for(String sig: sharedVariables)
            {
                saveSharedVarSignatureToDB(sig);
                if(isVerbose)
                System.out.println(sig);
            }

            if(isVerbose)
                System.out.println("====================SHARED ARRAY LOCATIONS===================");

            createSharedArrayLocTable(false);
            for(String sig: sharedArrayLocations)
            {
                  saveSharedArrayLocToDB(sig);
                if(isVerbose)
                    System.out.println(sig);
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void saveMetaData(boolean isVerbose, ConcurrentHashMap<String, Integer> variableIdMap, ConcurrentHashMap<String, Boolean> volatileVariables, ConcurrentHashMap<String, Integer> stmtSigIdMap) {
        if (state == null) return;
        try{
            //just reuse the connection

            //TODO: if db is null or closed, there must be something wrong
            //save variable - id to database
            createVarSignatureTable(false);
            Iterator<Map.Entry<String, Integer>> variableIdMapIter = variableIdMap.entrySet().iterator();
            while (variableIdMapIter.hasNext()) {
                Map.Entry<String,Integer> entry = variableIdMapIter.next();
                String sig = entry.getKey();
                Integer id = entry.getValue();
                variableIdMapIter.remove();
                saveVarSignatureToDB(sig, id);
                if(isVerbose)
                    System.out.println("* ["+id+"] "+sig+" *");

            }

//save volatilevariable - id to database
            createVolatileSignatureTable(false);
            Iterator<Map.Entry<String,Boolean>> volatileIt = volatileVariables.entrySet().iterator();
            while(volatileIt.hasNext())
            {
                String sig = volatileIt.next().getKey();
                volatileIt.remove();
                Integer id = state.getVariableId(sig);

                saveVolatileSignatureToDB(sig, id);
                if(isVerbose)System.out.println("* volatile: ["+id+"] "+sig+" *");

            }
//save stmt - id to database
            createStmtSignatureTable(false);

            Iterator<Map.Entry<String, Integer>> stmtSigIdMapIter = stmtSigIdMap.entrySet().iterator();
            while(stmtSigIdMapIter.hasNext())
            {
                Map.Entry<String, Integer> entry = stmtSigIdMapIter.next();
                stmtSigIdMapIter.remove();
                String sig = entry.getKey();
                Integer id = entry.getValue();

                saveStmtSignatureToDB(sig, id);
                //System.out.println("* ["+id+"] "+sig+" *");
            }

        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

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
	public DBEngine(String directory, String name, Metadata state)
	{
        this.state = state;
		appname = name;
		tracetablename = "trace_"+name;
		tidtablename = "tid_"+name;
		volatilesigtablename = "volatile_"+name;
		stmtsigtablename="stmtsig_"+name;
		varsigtablename="varsig_"+name;
		
        sharedarrayloctablename = "sharedarrayloc_" + name;
        sharedvarsigtablename = "sharedvarsig_" + name;
		scheduletablename = "schedule_"+name;
		propertytablename = "property_"+name;
		try
		{
			connectDB(directory);
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
	public void saveProperty(String name, int ID, boolean dropTable)
	{
		try{
            if (dropTable) createPropertyTable(true);
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

    /**
     * Drops all relevant tables of the database.  Used for a clean start.
     * @throws Exception if errors are reported by the sql command
     */
    public void dropAll() throws Exception {
        Statement stmt = conn.createStatement();

        String sql_dropTable;
        sql_dropTable = "DROP TABLE IF EXISTS "+propertytablename;
        stmt.execute(sql_dropTable);
        sql_dropTable = "DROP TABLE IF EXISTS "+scheduletablename;
        stmt.execute(sql_dropTable);
        sql_dropTable = "DROP TABLE IF EXISTS "+stmtsigtablename;
        stmt.execute(sql_dropTable);
        sql_dropTable = "DROP TABLE IF EXISTS "+sharedvarsigtablename;
        stmt.execute(sql_dropTable);
        sql_dropTable = "DROP TABLE IF EXISTS "+volatilesigtablename;
        stmt.execute(sql_dropTable);
        sql_dropTable = "DROP TABLE IF EXISTS "+tracetablename;
        stmt.execute(sql_dropTable);
        sql_dropTable = "DROP TABLE IF EXISTS "+tidtablename;
        stmt.execute(sql_dropTable);
        sql_dropTable = "DROP TABLE IF EXISTS " + varsigtablename;
        stmt.execute(sql_dropTable);
    }

    /**
     * Checks that all relevant tables exist.
     * @throws Exception
     */
    public boolean checkTables() throws SQLException {
        Statement stmt = conn.createStatement();

        String sql_checkTable;
        try {
            sql_checkTable = "SELECT COUNT(*) FROM "+stmtsigtablename;
            stmt.execute(sql_checkTable);
            sql_checkTable = "SELECT COUNT(*) FROM "+varsigtablename;
            stmt.execute(sql_checkTable);
            sql_checkTable = "SELECT COUNT(*) FROM "+volatilesigtablename;
            stmt.execute(sql_checkTable);
            sql_checkTable = "SELECT COUNT(*) FROM "+tracetablename;
            stmt.execute(sql_checkTable);
            sql_checkTable = "SELECT COUNT(*) FROM "+tidtablename;
            stmt.execute(sql_checkTable);
        } catch (SQLException e) {
            if (e.getErrorCode() == TABLE_NOT_FOUND_ERROR_CODE)
                return false;
            throw e;
        }
        return true;
    }

    public void createPropertyTable(boolean newTable) throws Exception {
        Statement stmt = conn.createStatement();
        if (newTable) {
		String sql_dropTable = "DROP TABLE IF EXISTS "+propertytablename;
        stmt.execute(sql_dropTable);
        }

        String sql_createTable = "CREATE TABLE IF NOT EXISTS "+propertytablename+" ("+
        		propertytablecolname[0]+" "+propertytablecoltype[0]+", "+
        		propertytablecolname[1]+" "+propertytablecoltype[1]+")";
        stmt.execute(sql_createTable);
        
	}

    public void createScheduleTable(boolean newTable) throws Exception {
    	Statement stmt = conn.createStatement();
        if (newTable) {
            String sql_dropTable = "DROP TABLE IF EXISTS " + scheduletablename;
        stmt.execute(sql_dropTable);
        }

        String sql_createTable = "CREATE TABLE IF NOT EXISTS " + scheduletablename + " (" +
        		scheduletablecolname[0]+" "+scheduletablecoltype[0]+" PRIMARY KEY, "+
        		scheduletablecolname[1]+" "+scheduletablecoltype[1]+", "+
        		scheduletablecolname[2]+" "+scheduletablecoltype[2]+")";
        stmt.execute(sql_createTable);
        
        String sql_insertdata = "INSERT INTO " + scheduletablename + " VALUES (?,?,?)";
        prepStmt = conn.prepareStatement(sql_insertdata);
	}

    public void createStmtSignatureTable(boolean newTable) throws Exception {
    	Statement stmt = conn.createStatement();
        if (newTable) {
            String sql_dropTable = "DROP TABLE IF EXISTS " + stmtsigtablename;
        stmt.execute(sql_dropTable);
        }

        String sql_createTable = "CREATE TABLE IF NOT EXISTS " + stmtsigtablename + " (" +
        		stmtsigtablecolname[0]+" "+stmtsigtablecoltype[0]+" PRIMARY KEY, "+
        		stmtsigtablecolname[1]+" "+stmtsigtablecoltype[1]+")";
        stmt.execute(sql_createTable);
        
        String sql_insertdata = "INSERT INTO " + stmtsigtablename + " VALUES (?,?)";
        prepStmt = conn.prepareStatement(sql_insertdata);
	}

    public void createVarSignatureTable(boolean newTable) throws Exception {
        Statement stmt = conn.createStatement();
        if (newTable) {
            String sql_dropTable = "DROP TABLE IF EXISTS " + varsigtablename;
            stmt.execute(sql_dropTable);
        }

        String sql_createTable = "CREATE TABLE IF NOT EXISTS " + varsigtablename + " (" +
                varsigtablecolname[0] + " " + varsigcoltype[0] + " PRIMARY KEY, " +
                varsigtablecolname[1] + " " + varsigcoltype[1] + ")";
        stmt.execute(sql_createTable);

        String sql_insertdata = "INSERT INTO " + varsigtablename + " VALUES (?,?)";
        prepStmt = conn.prepareStatement(sql_insertdata);
    }

    public void createSharedArrayLocTable(boolean newTable) throws Exception {
        Statement stmt = conn.createStatement();
        if (newTable) {
            String sql_dropTable = "DROP TABLE IF EXISTS " + sharedarrayloctablename;
            stmt.execute(sql_dropTable);
        }

        String sql_createTable = "CREATE TABLE IF NOT EXISTS " + sharedarrayloctablename + " (" +
                sharedarrayloctablecolname[0] + " " + sharedarrayloccoltype[0] + ")";
        stmt.execute(sql_createTable);

        String sql_insertdata = "INSERT INTO " + sharedarrayloctablename + " VALUES (?)";
        prepStmt = conn.prepareStatement(sql_insertdata);
    }

    public void createSharedVarSignatureTable(boolean newTable) throws Exception {
        Statement stmt = conn.createStatement();
        if (newTable) {
            String sql_dropTable = "DROP TABLE IF EXISTS "+sharedvarsigtablename;
            stmt.execute(sql_dropTable);
        }

        String sql_createTable = "CREATE TABLE IF NOT EXISTS " + sharedvarsigtablename + " (" +
                sharedvarsigtablecolname[0] + " " + sharedvarsigcoltype[0] + ")";
        stmt.execute(sql_createTable);

        String sql_insertdata = "INSERT INTO " + sharedvarsigtablename + " VALUES (?)";
        prepStmt = conn.prepareStatement(sql_insertdata);
	}

    public void createVolatileSignatureTable(boolean newTable) throws Exception {
    	Statement stmt = conn.createStatement();
        if (newTable) {
            String sql_dropTable = "DROP TABLE IF EXISTS " + volatilesigtablename;
            stmt.execute(sql_dropTable);
        }

        String sql_createTable = "CREATE TABLE IF NOT EXISTS " + volatilesigtablename + " (" +
        		volatilesigtablecolname[0]+" "+volatilesigcoltype[0]+" PRIMARY KEY, "+
        		volatilesigtablecolname[1]+" "+volatilesigcoltype[1]+")";
        stmt.execute(sql_createTable);
        
        String sql_insertdata = "INSERT INTO " + volatilesigtablename + " VALUES (?,?)";
        prepStmt = conn.prepareStatement(sql_insertdata);
	}

    public void createTraceTable(boolean newTable) throws Exception {
    	Statement stmt = conn.createStatement();
        if (newTable) {
            String sql_dropTable = "DROP TABLE IF EXISTS " + tracetablename;
            stmt.execute(sql_dropTable);
        }

        String sql_createTable = "CREATE TABLE IF NOT EXISTS " + tracetablename + " (" +
                tracetablecolname[0] + " " + tracetablecoltype[0] + " AUTO_INCREMENT, " +//PRIMARY KEY
        tracetablecolname[1]+" "+tracetablecoltype[1]+", "+
        tracetablecolname[2]+" "+tracetablecoltype[2]+", "+
        tracetablecolname[3]+" "+tracetablecoltype[3]+", "+
        tracetablecolname[4]+" "+tracetablecoltype[4]+", "+
                tracetablecolname[5] + " " + tracetablecoltype[5] + ", " +
                "PRIMARY KEY (" + tracetablecolname[0] + ")" +
                ")";
        stmt.execute(sql_createTable);
        
        String sql_insertdata = "INSERT INTO " + tracetablename + " ( " + tracetablecolname[1] + ", " +
                tracetablecolname[2] + ", " +
                tracetablecolname[3] + ", " +
                tracetablecolname[4] + ", " +
                tracetablecolname[5] + " " + " ) VALUES (?,?,?,?,?)";
        prepStmt = conn.prepareStatement(sql_insertdata);
		
	}
	
    public void createThreadIdTable(boolean newTable) throws Exception {
        Statement stmt = conn.createStatement();

        if (newTable) {
		String sql_dropTable = "DROP TABLE IF EXISTS "+tidtablename;

        stmt.execute(sql_dropTable);
        }

        String sql_createTable = "CREATE TABLE IF NOT EXISTS " + tidtablename + " (" +
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
		
        } catch (Exception e) {
            System.err.println("\nGot exception while trying to save " + "* stmt: ["+id+"] "+sig+" *");
            System.err.println(e.getMessage());
		}
	}

    public void saveSharedArrayLocToDB(String sig) {
        try {
            prepStmt.setString(1, sig);

            prepStmt.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public HashSet<String> loadSharedArrayLocs() {
        HashSet<String> sharedArrayLocs = new HashSet<String>();
        try {
            String sql_select = "SELECT * FROM " + sharedarrayloctablename;

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql_select);
            while (rs.next()) {
                // Get the data from the row using the column index
                String SIG = rs.getString(1);
                sharedArrayLocs.add(SIG);
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
        if (sharedArrayLocs.isEmpty())
            return null;
        else
            return sharedArrayLocs;
    }

    public HashSet<String> loadSharedVariables() {
        HashSet<String> sharedVariables = new HashSet<String>();
        try {
            String sql_select = "SELECT * FROM " + sharedvarsigtablename;

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql_select);
            while (rs.next()) {
                // Get the data from the row using the column index
                String SIG = rs.getString(1);
                sharedVariables.add(SIG);
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
        if (sharedVariables.isEmpty())
            return null;
        else
            return sharedVariables;
    }

    public void saveSharedVarSignatureToDB(String sig) {
        try {
		prepStmt.setString(1, sig);

		prepStmt.execute();

		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}

    public void saveVarSignatureToDB(String sig, int id) {
        try {
            prepStmt.setString(1, sig);
            prepStmt.setInt(2, id);

            prepStmt.execute();

        } catch (Exception e) {
            System.err.println("Got exception while trying to save " + "* [" + id + "] " + sig + " *");
            System.err.println(e.getMessage());
        }
    }

	public void saveVolatileSignatureToDB(String sig, int id)
	{
		try
		{
		prepStmt.setString(1, sig);
		prepStmt.setInt(2,id);

		prepStmt.execute();

        } catch (Exception e) {
            System.err.println("Got exception while trying to save " + "* volatile: ["+id+"] "+sig+" *");
            System.err.println(e.getMessage());
		}
	}


    public void saveEventToDB(long TID, int ID, String ADDR, String VALUE, byte TYPE) {
        if (shutDown) return;
        synchronizedSaveEventToDB(TID, ID, ADDR, VALUE, TYPE);
    }


	/**
	 * save an event to database. must be synchronized. 
	 * otherwise, easy to throw Unique index or primary key violation.
	 */
	public synchronized void synchronizedSaveEventToDB(long TID, int ID, String ADDR, String VALUE, byte TYPE)
	{

        PreparedStatement prepStmt = this.prepStmt;
        this.prepStmt = prepStmt;
		//make true->1. false->0
		if(VALUE.equals("true"))
			VALUE="1";
		if(VALUE.equals("false"))
			VALUE="0";
		
		if(asynchronousLogging)
		{
            globalEventID=globalEventID+1;
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
			prepStmt.setLong(1, TID);
			prepStmt.setInt(2, ID);
			prepStmt.setString(3, ADDR);
			prepStmt.setString(4, VALUE);
			prepStmt.setByte(5, TYPE);

				prepStmt.execute();
				

				
			}catch(Exception e)
			{

                if(!"Finalizer".equals(Thread.currentThread().getName()))
                    e.printStackTrace();// avoid finalizer thread

			}
		}
	}

    public void saveMetaData() {
        PreparedStatement prepStmt = this.prepStmt;
        saveMetaData(false);
        this.prepStmt = prepStmt;
    }

	protected void connectDB(String directory) throws Exception
	{
		try{
			Driver driver = new rvpredict.h2.Driver();
			String db_url = "jdbc:h2:"+directory+"/"+dbname+";DB_CLOSE_ON_EXIT=FALSE";
			conn = driver.connect(db_url, null);

	        //check if Database may be already in use
            //kill?
        }catch(Exception e)
        {
            e.printStackTrace();
        }
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
	public HashMap<Integer, String> getVolatileAddresses()
	{
		HashMap<Integer, String> map = new HashMap<Integer, String>();
		try{
		String sql_select = "SELECT * FROM "+volatilesigtablename;
		
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
	public HashMap<Integer, String> getVarSigIdMap()
	{
		HashMap<Integer, String> map = new HashMap<Integer, String>();
		try{
		String sql_select = "SELECT * FROM "+varsigtablename;
		
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
	public int getTracePropertyNumber() throws Exception
	{
		int number = 0;
		String sql_select = "SELECT COUNT(*) FROM "+tracetablename +" WHERE "+
				tracetablecolname[5]+" = "+tracetypetable[11];
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
	public Trace getTrace(TraceInfo info) throws Exception
	{
		return getTrace(1,0,info);
	}
	/**
	 * load trace from event min to event max
	 * @param min
	 * @param max
	 * @return
	 * @throws Exception
	 */
	public Trace getTrace(long min, long max, TraceInfo info) throws Exception
	{
		String sql_select = "SELECT * FROM "+tracetablename;
		if(max>min)
			sql_select+=" WHERE GID BETWEEN '"+min+"' AND '"+max+"'";
		sql_select+=" ORDER BY GID";
		
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql_select);
		
		Trace trace = new Trace(info);
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
	        	case 'b': node = new PropertyNode(GID,TID,ID,ADDR,AbstractNode.TYPE.PROPERTY); break;

	        	default:break;
	        }
	        
	        trace.addRawNode(node);
	        
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
		DBEngine db = new DBEngine("a.b.c.test", "name", null);
//		db.createSharedVarSignatureTable(true);
//		{
//			db.saveStmtSignatureToDB("a", 1);
//			db.saveStmtSignatureToDB("a.b", 2);
//		}
		
		db.createStmtSignatureTable(true);
		{
			db.saveStmtSignatureToDB("abc", 1);
			db.saveStmtSignatureToDB("a.b", 2);
		}
		
		db.createTraceTable(true);
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
	public HashMap<String,Integer> getProperty()
	{
		try{
		String sql_select = "SELECT * FROM "+propertytablename;
		
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql_select);
		HashMap<String,Integer> map = new HashMap<String,Integer>();

		while (rs.next()) 
	    {

	        String name = rs.getString(1);
	        Integer id = rs.getInt(2);

	         map.put(name, id);
	         
	    }
        return map;	        

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
