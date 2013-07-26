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

import db.DBEngine.EventItem;

import trace.*;
import trace.AbstractNode.TYPE;
import violation.IViolation;
import z3.Z3Engine;

public class DBEngineParallel extends DBEngine{
	
	private final String[] tracetablecolname={"ID","ADDR","VALUE","TYPE"};
	private final String[] tracetablecoltype={"INT","VARCHAR","VARCHAR","TINYINT"};
	private HashMap<Long,PreparedStatement> threadPrepStmtMap = new HashMap<Long,PreparedStatement>();
	private HashMap<Long,Connection> threadConnectionMap = new HashMap<Long,Connection>();

	public DBEngineParallel(String name)
	{
		super(name);
	}
	
	public void connectDB(long tid) throws Exception
	{
        Connection conn  = DriverManager.getConnection("jdbc:h2:"+Util.getUserHomeDirectory()+dbname+"_"+tracetablename+"_"+tid);//+"_"+tid   +";MVCC=TRUE" +";MULTI_THREADED=1"
        //conn.setAutoCommit(true);
        threadConnectionMap.put(tid, conn);
	}
	
	public void createTraceTable(long tid) throws Exception
	{
		Connection conn = threadConnectionMap.get(tid);
		if(conn==null)
		{
			connectDB(tid);
	        conn = threadConnectionMap.get(tid);
		}
			
		String tablename = tracetablename+"_"+tid;
		
		String sql_dropTable = "DROP TABLE IF EXISTS "+tablename;

    	Statement stmt = conn.createStatement();
        stmt.execute(sql_dropTable);
        
        String sql_createTable = "CREATE TABLE "+tablename+" ("+
        tracetablecolname[0]+" "+tracetablecoltype[0]+", "+
        tracetablecolname[1]+" "+tracetablecoltype[1]+", "+
        tracetablecolname[2]+" "+tracetablecoltype[2]+", "+
        tracetablecolname[3]+" "+tracetablecoltype[3]+")";
        stmt.execute(sql_createTable);
        
    	String sql_insertdata = "INSERT INTO "+tablename+" VALUES (?,?,?,?)";
    	PreparedStatement prepStmt = conn.prepareStatement(sql_insertdata);
        threadPrepStmtMap.put(tid, prepStmt);
	}
	
	public void saveEventToDB(long TID, int ID, String ADDR, String VALUE, byte TYPE)
	{
		try
		{
			PreparedStatement prepStmt = threadPrepStmtMap.get(TID);
			if(prepStmt==null)
			{
				//this thread is not captured, we need to create a table for it
				createTraceTable(TID);
				prepStmt = threadPrepStmtMap.get(TID);
			}
			
			prepStmt.setInt(1, ID);
			prepStmt.setString(2, ADDR);
			prepStmt.setString(3, VALUE);
			prepStmt.setByte(4, TYPE);
			
			prepStmt.execute();
			
			  //if(ADDR.length()>0&&ADDR.charAt(ADDR.length()-1)=='3')
			//	  System.out.println(globalEventID+" "+TID+" "+ADDR+" "+VALUE+" "+TYPE);

			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
