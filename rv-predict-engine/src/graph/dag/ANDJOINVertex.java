package graph.dag;

public class ANDJOINVertex implements IVertex {
	private final String ANDJOIN = "ANDJOIN_";
	public Vertex v;
	public String label;
	ANDJOINVertex(int id, Vertex v)
	{
		this.label = ANDJOIN+id;
		this.v = v;
	}
	ANDJOINVertex(Vertex v)
	{
		this.v = v;
		this.label = ANDJOIN+v.getLabel();

	}
	public String getLabel()
	{
		return label;
	}
}
