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
package graph.dag;

import java.util.List;

public class DAGTest {
	
	public static void main(String[] args)
	{
		//testdag1();
		//testdag2();
		testdag3();
		
	}
	private static void testdag1()
	{
		DAG dag = new DAG();
		Vertex v1 = new Vertex("1");
		Vertex v2 = new Vertex("2");
		Vertex v3 = new Vertex("3");
		Vertex v4 = new Vertex("4");
		Vertex v5 = new Vertex("5");


		try{
			
		dag.addEdge(v1, v2);
		dag.addEdge(v2, v3);
		dag.addEdge(v2, v4);
		dag.addEdge(v1, v5);
		dag.addEdge(v5, v4);
		CFG cfg = new CFG(dag);
		cfg.start(v1);
		
		}catch(Exception e)
		{
			e.printStackTrace();
		}

	}
	private static void testdag2()
	{
		DAG dag = new DAG();
		Vertex v1 = new Vertex("1");
		Vertex v2 = new Vertex("2");
		Vertex v3 = new Vertex("3");
		Vertex v4 = new Vertex("4");
		Vertex v5 = new Vertex("5");
		Vertex v6 = new Vertex("6");
		
		try{
			
		dag.addEdge(v1, v2);
		dag.addEdge(v1, v3);
		dag.addEdge(v2, v4);
		dag.addEdge(v2, v5);
		dag.addEdge(v3, v6);
		
		CFG cfg = new CFG(dag);
		
		cfg.start(v1);
		
		}catch(Exception e)
		{
			e.printStackTrace();
		}

	}
	private static void testdag3()
	{
		DAG dag = new DAG();
		Vertex v1 = new Vertex("1");
		Vertex v2 = new Vertex("2");
		Vertex v3 = new Vertex("3");
		Vertex v4 = new Vertex("4");
		Vertex v5 = new Vertex("5");		
		try{
			
		dag.addEdge(v2, v3);
		dag.addEdge(v1, v3);
		dag.addEdge(v2, v4);
		dag.addEdge(v4, v5);
		dag.addEdge(v3, v5);
		//need to create a dummy node v0
		Vertex v0 = new Vertex("0");
		//add edge from v0 to beginning nodes v1, v2
		dag.addEdge(v0, v1);
		dag.addEdge(v0, v2);
		
		CFG cfg = new CFG(dag);
		
		//start from v0 instead
		cfg.start(v0);
		
		}catch(Exception e)
		{
			e.printStackTrace();
		}

	}
}
