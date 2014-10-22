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
import rvpredict.config.Config;
import rvpredict.instrumentation.GlobalStateForInstrumentation;
import rvpredict.h2.jdbc.JdbcSQLException;

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

    protected static long globalEventID = 0;
    protected static long DBORDER = 0;//handle strange classloader

    //currently we use the h2 database
    protected final String dbname = "RVDatabase";
    private final int TABLE_NOT_FOUND_ERROR_CODE = 42102;
    private final int DATABASE_CLOSED = 90098;
	public String appname = "main";

    //database schema
    protected final String[] stmtsigtablecolname = {"SIG", "ID"};
    protected final String[] stmtsigtablecoltype = {"VARCHAR", "INT"};

    protected final String[] scheduletablecolname = {"ID", "SIG", "SCHEDULE"};
    protected final String[] scheduletablecoltype = {"INT", "VARCHAR", "ARRAY"};

    protected final String[] sharedvarsigtablecolname = {"SIG"};
    protected final String[] sharedvarsigcoltype = {"VARCHAR"};

    protected final String[] sharedarrayloctablecolname = {"SIG"};
    protected final String[] sharedarrayloccoltype = {"VARCHAR"};

    protected final String[] varsigtablecolname = {"SIG", "ID"};
    protected final String[] varsigcoltype = {"VARCHAR", "INT"};

    protected final String[] volatilesigtablecolname = {"SIG", "ID"};
    protected final String[] volatilesigcoltype = {"VARCHAR", "INT"};

    protected final String[] tracetablecolname = {"GID", "TID", "ID", "ADDR", "VALUE", "TYPE"};
    protected final String[] tracetablecoltype = {"BIGINT", "BIGINT", "INT", "VARCHAR", "VARCHAR", "TINYINT"};

    protected final String[] tidtablecolname = {"TID", "NAME"};
    protected final String[] tidtablecoltype = {"BIGINT", "VARCHAR"};

    protected final String[] propertytablecolname = {"PROPERTY", "ID"};
    protected final String[] propertytablecoltype = {"VARCHAR", "INT"};

    //READ,WRITE,LOCK,UNLOCK,WAIT,NOTIFY,START,JOIN,BRANCH,BB
    public final byte[] tracetypetable = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b'};
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
    private String varsigtablename;
    private String sharedarrayloctablename;

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

        EventItem(long gid, long tid, int sid, String addr, String value, byte type) {
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
	public DBEngine(String directory, String name)
	{
		appname = name;
		tracetablename = "trace_" + name;
		tidtablename = "tid_" + name;
		volatilesigtablename = "volatile_" + name;
		stmtsigtablename = "stmtsig_" + name;
		varsigtablename = "varsig_" + name;

		sharedarrayloctablename = "sharedarrayloc_" + name;
		sharedvarsigtablename = "sharedvarsig_" + name;
		scheduletablename = "schedule_" + name;
		propertytablename = "property_" + name;
		try {
			connectDB(directory);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    public void closeDB() {
        try {
            conn.createStatement().execute("SHUTDOWN");
            //conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveProperty(String name, int ID, boolean dropTable) {
        try {
            if (dropTable) createPropertyTable(true);
            String sql_insertdata = "INSERT INTO " + propertytablename + " VALUES (?,?)";
            PreparedStatement prepStmt = conn.prepareStatement(sql_insertdata);
            prepStmt.setString(1, name);
            prepStmt.setInt(2, ID);
            prepStmt.execute();
        } catch (Exception e) {
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
            String sql_dropTable = "DROP TABLE IF EXISTS " + propertytablename;
            stmt.execute(sql_dropTable);
        }

        String sql_createTable = "CREATE TABLE IF NOT EXISTS " + propertytablename + " (" +
                propertytablecolname[0] + " " + propertytablecoltype[0] + ", " +
                propertytablecolname[1] + " " + propertytablecoltype[1] + ")";
        stmt.execute(sql_createTable);

    }

    public void createScheduleTable(boolean newTable) throws Exception {
        Statement stmt = conn.createStatement();
        if (newTable) {
            String sql_dropTable = "DROP TABLE IF EXISTS " + scheduletablename;
            stmt.execute(sql_dropTable);
        }

        String sql_createTable = "CREATE TABLE IF NOT EXISTS " + scheduletablename + " (" +
                scheduletablecolname[0] + " " + scheduletablecoltype[0] + " PRIMARY KEY, " +
                scheduletablecolname[1] + " " + scheduletablecoltype[1] + ", " +
                scheduletablecolname[2] + " " + scheduletablecoltype[2] + ")";
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
                stmtsigtablecolname[0] + " " + stmtsigtablecoltype[0] + " PRIMARY KEY, " +
                stmtsigtablecolname[1] + " " + stmtsigtablecoltype[1] + ")";
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
            String sql_dropTable = "DROP TABLE IF EXISTS " + sharedvarsigtablename;
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
                volatilesigtablecolname[0] + " " + volatilesigcoltype[0] + " PRIMARY KEY, " +
                volatilesigtablecolname[1] + " " + volatilesigcoltype[1] + ")";
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
                tracetablecolname[1] + " " + tracetablecoltype[1] + ", " +
                tracetablecolname[2] + " " + tracetablecoltype[2] + ", " +
                tracetablecolname[3] + " " + tracetablecoltype[3] + ", " +
                tracetablecolname[4] + " " + tracetablecoltype[4] + ", " +
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
            String sql_dropTable = "DROP TABLE IF EXISTS " + tidtablename;

            stmt.execute(sql_dropTable);
        }

        String sql_createTable = "CREATE TABLE IF NOT EXISTS " + tidtablename + " (" +
                tidtablecolname[0] + " " + tidtablecoltype[0] + ", " +//PRIMARY KEY
                tidtablecolname[1] + " " + tidtablecoltype[1] + ")";
        stmt.execute(sql_createTable);

        String sql_insertdata = "INSERT INTO " + tidtablename + " VALUES (?,?)";
        prepStmt2 = conn.prepareStatement(sql_insertdata);
    }

    public void saveThreadTidNameToDB(long id, String name) {
        try {
            prepStmt2.setLong(1, id);
            prepStmt2.setString(2, name);

            prepStmt2.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveStmtSignatureToDB(String sig, int id) {
        try {
            prepStmt.setString(1, sig);
            prepStmt.setInt(2, id);

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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveVarSignatureToDB(String sig, int id) {
        try {
            prepStmt.setString(1, sig);
            prepStmt.setInt(2, id);

            prepStmt.execute();

        } catch (Exception e) {
            System.err.println("Got exception while trying to save " + "* ["+id+"] "+sig+" *");
            System.err.println(e.getMessage());
        }
    }

    public void saveVolatileSignatureToDB(String sig, int id) {
        try {
            prepStmt.setString(1, sig);
            prepStmt.setInt(2, id);

            prepStmt.execute();

        } catch (Exception e) {
            System.err.println("Got exception while trying to save " + "* volatile: ["+id+"] "+sig+" *");
            System.err.println(e.getMessage());
        }
    }


    public void saveEventToDB(long TID, int ID, String ADDR, String VALUE, byte TYPE) {
        if (Config.shutDown) return;
            synchronizedSaveEventToDB(TID, ID, ADDR, VALUE, TYPE);
    }


	/**
	 * save an event to database. must be synchronized. 
	 * otherwise, easy to throw Unique index or primary key violation.
	 */
	public synchronized void synchronizedSaveEventToDB(long TID, int ID, String ADDR, String VALUE, byte TYPE)
	{

        PreparedStatement prepStmt = this.prepStmt;
        RecordRT.saveMetaData(this, GlobalStateForInstrumentation.instance, false);
        this.prepStmt = prepStmt;
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
			checkException(e);
			if(!"Finalizer".equals(Thread.currentThread().getName()))
				e.printStackTrace();// avoid finalizer thread

		}
	}

	public void checkException(Exception e) {
		if (Config.shutDown) return;
		if (e instanceof SQLException) {
			SQLException esql = (SQLException) e;
			if (esql.getErrorCode() == DATABASE_CLOSED) {
				System.err.println("Not enough space left for logging in " + Config.instance.commandLine.outdir);
				System.err.println("Please free some space and restart RV-Predict.");
				Config.shutDown = true;
				System.exit(1);
			}
		}
		e.printStackTrace();
	}
	
	protected void connectDB(String directory) throws Exception
	{
		try{
			Driver driver = new rvpredict.h2.Driver();
			String db_url = "jdbc:h2:"+directory+"/"+dbname+";DB_CLOSE_ON_EXIT=FALSE";
			conn = driver.connect(db_url, null);
			
			//conn  = DriverManager.getConnection(db_url);
	        //conn.setAutoCommit(true);
	        //check if Database may be already in use
	        //kill?
		}catch(Exception e)
		{
			e.printStackTrace();
			//DBORDER++;
	        //conn  = DriverManager.getConnection("jdbc:h2:"+Util.getUserHomeDirectory()+dbname+DBORDER);//+";AUTO_SERVER=true"
		}
	}

}
