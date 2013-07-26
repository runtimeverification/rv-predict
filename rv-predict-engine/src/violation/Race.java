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
package violation;

import java.util.ArrayList;
import java.util.Vector;

public class Race implements IViolation{

	//not mutable
	//why hashset has strange behavior??
	final private String node1;
	final private String node2;
	private int hashcode;
	
	ArrayList<Vector<String>> schedules =  new ArrayList<Vector<String>>();
	
	public void addSchedule(Vector<String> schedule)
	{
		schedules.add(schedule);
	}

	public ArrayList<Vector<String>> getSchedules()
	{
		return schedules;
	}
//	public Race (String node1,String node2)
//	{
//		this.node1 = node1;
//		this.node2 = node2;
//	}
	public Race (String node1,String node2, int id1, int id2)
	{
		this.node1 = node1;
		this.node2 = node2;
		hashcode = id1*id1+id2*id2;
	}
	
	@Override
	public int hashCode()
	{
		//int code = node1.hashCode()+node2.hashCode();
		//return code;
		return hashcode;
	}
	@Override
	public boolean equals(Object o)
	{
		if(o instanceof Race)
		{
			if((((Race) o).node1 == node1
					&&((Race) o).node2 == node2)
					||(((Race) o).node1 == node2
							&&((Race) o).node2 == node1))
				return true;
			
		}
		
		return false;
	}
	
	@Override
	public String toString()
	{
		return node1+" - "+node2;
	}
	

}
