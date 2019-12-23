package edu.kit.ksri.als.graph;

import java.awt.geom.Point2D;

/**
 * Basic class describing a node in a graph.
 *
 */
public class Node {
	
	public int index; //1-based
	public String name;
	public Point2D.Double coordinates; //coordinates for the layout
	
	/**
	 * Standard constructor.
	 * @param index Index of the node, 1-based.
	 * @param name Name of the node.
	 * @param coordinates XY-coordinates of the node.
	 */
	public Node (int index, String name, Point2D.Double coordinates) {
		this.index = index;
		this.name = name;
		this.coordinates = coordinates;
	}
}
