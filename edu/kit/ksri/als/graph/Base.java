package edu.kit.ksri.als.graph;

import java.util.HashSet;

/**
 * Describes a base for ambulances including its graph node, costs 
 * and relations to demand nodes in the graph.
 *
 */
public class Base {
	
	public HashSet<Demand> coveredDemands = new HashSet<Demand>(); // set of demands within timely reach
	public double costs; // costs of installing a base
	public double costsPerAmbulance; // costs per ambulance
	public Node node; // location in the graph
	
	/**
	 * Standard constructor.
	 * @param node Defines the location in the graph.
	 * @param costs Construction costs for this base.
	 * @param costsPerAmbulance Costs for operating one ambulance in this base.
	 */
	public Base(Node node, double costs, double costsPerAmbulance) {
		this.node = node;
		this.costs = costs;
		this.costsPerAmbulance = costsPerAmbulance;
	}
	
}
