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

import java.util.ArrayList;
import java.util.List;

public class CFG {
		
	private DAG dag;
	private final IVertex begin = new BeginVertex();
	private final IVertex end = new EndVertex();

	public CFG(DAG dag)
	{
		this.dag = dag;
	}
	public void start (Vertex v0)
	{
		CFGThread thread = new CFGThread(begin,v0);
		thread.start();
	}
	
	class CFGThread extends Thread
	{
		List<IVertex> traversedList;
		Vertex curnode;
		CFGThread(IVertex v0, Vertex v)
		{
			traversedList = new ArrayList<IVertex>();
			traversedList.add(v0);
			
			curnode = v;
		}
		
		public void run()
		{
			//process nodes
			process();
			
			//TODO: put traversedList into a global data structure before exit

			output();
		}

		private void output() {
			synchronized(System.out)
			{int i=0;
			for(;i<traversedList.size()-1;i++)
			{
				System.out.print(traversedList.get(i).getLabel()+" -> ");
			}
			
			System.out.println(traversedList.get(i).getLabel());
			}
		}

		private void process() {
			while(curnode.mark())
			{
				traversedList.add(curnode);
				
				List<Vertex> list = curnode.getChildren();
				if(list.size()==0)
				{
					//add end to list, then stop
					traversedList.add(end);
					
					
					return;
					
				}
				else if(list.size()==1)
				{
					curnode = list.get(0);
				}
				else
				{
					ANDSPLITVertex asvertex = curnode.createANDSPLITVertex();
					
					curnode = list.get(0);
					for(int i=1;i<list.size();i++)
					{
						CFGThread thread = new CFGThread(asvertex,list.get(i));
						thread.start();
					}
				}
			}
			
			//if we come here, it means we encounter an AND-JOIN node
			traversedList.add(curnode.getANDJOINVertex());
		}
	}
}
