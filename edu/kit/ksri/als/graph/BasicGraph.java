package edu.kit.ksri.als.graph;

import edu.kit.ksri.als.dataExchange.*;
import edu.kit.ksri.als.gui.GraphLayout;

import java.awt.geom.Point2D;
import java.util.ArrayList;

/**
 * A regular directed graph specified by a name and sets of nodes and edges.
 * Contains only the information that is absolutely necessary to create a graph for the {@link GraphLayout}.
 */
public class BasicGraph{
	
	public String name; // name of the graph
	public ArrayList<Node> nodes = new ArrayList<Node>(); // list of all nodes of the graph
	public ArrayList<Edge> edges = new ArrayList<Edge>(); // list of all edges of the graph	
	
	// column numbers in the import Excel file where dedicated information is found (0-based)
	int graphNameColumn		= 0; // column A in Excel
	int nodeNameColumn 		= 1; // column B in Excel
	int edgeWeightsColumn	= 7; // column H in Excel
	int nodeXColumn 		= 8; // column I in Excel
	int nodeYColumn 		= 9; // column J in Excel
		
	/**
	 * Creates a basic graph using the information of a dedicated Excel sheet.
	 * @param graphName
	 */
	public BasicGraph(String graphName, ImportData file) {
		this.name = graphName;		
		double[][] edgeWeights = null; // auxiliary field to create the edges
		int nodeID = 0;

		// read from Excel
		while(file.rowIterator.hasNext()) { //iterate through all rows of the Excel file (one row contains information about one node)

			file.getNextRow();

			if (!name.equals(file.getCellInCurrentRowAsString(graphNameColumn))) continue; // only read information if it is the correct graph, otherwise skip this row
			
			// read and create node
            String nodeName = file.getCellInCurrentRowAsString(nodeNameColumn);
		    //read and save node layout coordinates
		    Point2D.Double coordinates = new Point2D.Double(0.0,0.0); // default coordinates (0,0)
			if ( file.isCellInCurrentRowNotNull(nodeXColumn) && file.isCellInCurrentRowNotNull(nodeYColumn) ){ // if information is available
				coordinates = new Point2D.Double( file.getCellInCurrentRowAsDouble(nodeXColumn), file.getCellInCurrentRowAsDouble(nodeYColumn) ); // read coordinates
			}
			nodes.add(new Node(++nodeID, nodeName, coordinates)); //create node and add it to list of nodes

		    //read edge weights (preliminary step to creating the edges)		    
		    String edgeWeightsStr = file.getCellInCurrentRowAsString(edgeWeightsColumn);
		    edgeWeightsStr = edgeWeightsStr.replaceAll(",","."); // replace "," (German number notation) with "." (Java number notation)
		    String[] edgeWeightsArrayStr = edgeWeightsStr.split(";"); // create edge weight array from String (";" as separator)
	    	if (edgeWeights == null) edgeWeights = new double[edgeWeightsArrayStr.length][edgeWeightsArrayStr.length]; // create array for edge weights
	    	for (int i=0; i<edgeWeightsArrayStr.length; i++) {
	    		if (edgeWeightsArrayStr[i].equals("-")) edgeWeights[nodeID-1][i] = Double.MAX_VALUE; // if there is no edge, assign Double.MAX_VALUE as edge weight
	    		else edgeWeights[nodeID-1][i] = new Double(edgeWeightsArrayStr[i]); // assign edge weight
	    	}
	    	
		} //end reading Excel file		
		
		//create edges
		for (Node node : nodes) { // for all nodes
			for (int i=0; i<edgeWeights[node.index-1].length; i++) { // for all edge weights of a node that have been read (Remark: node.id is 1-based)
				double edgeWeight = edgeWeights[node.index-1][i];
				if (edgeWeight<Double.MAX_VALUE) { // if edge weight exists (Double.MAX_VALUE marks the lack of an edge)
					edges.add(new Edge(node, nodes.get(i), edgeWeight)); // create edge
				}
			}
		}
	}
}
