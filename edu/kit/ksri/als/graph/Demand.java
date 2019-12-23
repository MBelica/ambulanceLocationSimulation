package edu.kit.ksri.als.graph;

import java.util.HashSet;

/**
 * Describes a demand node with its demand probability distribution
 * and its connections to base nodes in the graph.
 *
 */
public class Demand {

	public double[] probabilities;	// discrete probability distribution of demand (index as value for the volume of demand, double value as is its probability)
	public HashSet<Base> basesCovering = new HashSet<Base>(); ;	// set of bases that can reach this demand node in time
	public Node node; // location in the graph
	
	/**
	 * Standard constructor.
	 * @param node Defines the location in the graph.
	 * @param probabilities 	Discrete probability distribution of demand. The 0-based 
	 * 							index of an entry represents the volume of demand, the 
	 * 							actual double value defines its probability.
	 */
	public Demand(Node node, double[] probabilities) {
		this.node = node;
		this.probabilities = probabilities;
}

}
