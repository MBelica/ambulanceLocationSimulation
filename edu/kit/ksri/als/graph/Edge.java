package edu.kit.ksri.als.graph;

/**
 * Basic class describing an edge in a graph.
 *
 */
public class Edge {

	public Node fromNode;
	public Node toNode;
	double weight;
	
	/**
	 * Standard constructor.
	 * @param from Origin node of the edge.
	 * @param to Destination node of the edge.
	 * @param weight Edge weight, for example travel time between the nodes.
	 */
	public Edge (Node from, Node to, double weight) {
		fromNode = from;
		toNode = to;
		this.weight = weight;
	}
}
