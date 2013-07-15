package graph.dag;

public class ANDSPLITVertex implements IVertex {
	private final String ANDSPLIT = "ANDSPLIT_";

	public Vertex v;
	public String label;
	ANDSPLITVertex(int id, Vertex v)
	{
		this.label = ANDSPLIT+id;
		this.v = v;
	}
	ANDSPLITVertex(Vertex v)
	{
		this.v = v;
		this.label = ANDSPLIT+v.getLabel();

	}
	public String getLabel()
	{
		return label;
	}
}
