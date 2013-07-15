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
