package edu.kit.ksri.als.gui;

import edu.kit.ksri.als.*;
import edu.kit.ksri.als.graph.Node;
import edu.kit.ksri.als.graph.BasicGraph;
import math.geom2d.conic.Circle2D;
import math.geom2d.line.LineSegment2D;
import org.apache.batik.transcoder.*;
import org.graphstream.graph.EdgeRejectedException;
import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.io.*;

/**
 * Program for the visualization of graphs.
 * Gets a {@link BasicGraph} graph as input and turns it into a GraphStream graph.
 * The graph is visualized in the GraphStream viewer, can be modified 
 * within the viewer and may then be exported as an image in multiple formats.
 *
 */

@SuppressWarnings("serial")
public class GraphLayout extends JFrame implements ViewerListener, ActionListener, MouseListener, MouseWheelListener {
    
	int mouseX, mouseY; //mouse coordinates
	BasicGraph basicGraph; //the import graph
	
	//GraphStream variables
	View view;
    Viewer viewer;
    ViewerPipe fromViewer;
    Camera camera;
    Graph graph;
    //GraphStream viewer parameters
    double gridSize = 1.0; //distance from one grid line to the next
    double nodeSize = 0.6; 
    double textSize = 0.3;
    double nodeDistance = 2.0; //default distance between nodes for the automatic layout of GraphStream
    int numberOfNodes; //number of nodes in the BasicGraph graph    

    public GraphLayout(BasicGraph basicGraph) {
    	
    	// (1) create GraphStream graph from GraphBasic
		this.basicGraph = basicGraph; //get import graph
		graph = new SingleGraph("Graph"); //create undirected Graphstream graph

		// (1a) create nodes
		numberOfNodes = basicGraph.nodes.size();
		for (graph.Node node : basicGraph.nodes) { //for all nodes of the import graph
			String id = ""+node.index;
			graph.addNode(id); //add node
			graph.getNode(id).addAttribute("ui.label", node.name); //add label
			// Add default node coordinates. Must not be (0,0) in order for the automatic layout to work.
			graph.getNode(id).addAttribute("x", 1.0);
			graph.getNode(id).addAttribute("y", 0.0);
		}
		// (1b) create edges
		for (graph.Edge edge : basicGraph.edges) { // for all edges...
			int from = edge.fromNode.index; // from node
			int to = edge.toNode.index; // to node

			if (from != to) { // omit edges from a node to itself
				try {
					graph.addEdge(from+"-"+to, ""+from, ""+to, false); //add edge : false = undirected
					graph.getEdge(from+"-"+to).addAttribute("layout.weight", nodeDistance); //set default distance between nodes for automatic layout
				}
				catch (EdgeRejectedException e) {
					// catches undirected edges that are defined for the second time (from A to B and B to A) which causes an error
					e.printStackTrace(System.out);
				}
			}
		}

		// (1c) define GraphStream graph attributes
        graph.addAttribute("ui.antialias"); // activate anti-aliasing (better quality graphics)
        graph.addAttribute("ui.stylesheet", styleSheet); // load style sheet for nodes and edges

		// END (1)


		// (2) set up GraphStream (copy & pasted from tutorial)
		System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
		// We do as usual to display a graph. This connects the graph outputs to the viewer.
        // The viewer is a sink of the graph.
		viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
		view = viewer.addDefaultView(false);
		// We connect back the viewer to the graph, the graph becomes a sink for the viewer.
        // We also install us as a viewer listener to intercept the graphic events.
        fromViewer = viewer.newViewerPipe();
        fromViewer.addViewerListener(this);
        fromViewer.addSink(graph);
        fromViewer.removeElementSink(graph);

        viewer.enableAutoLayout(); // enable automatic layout
        camera = view.getCamera();
        camera.setViewPercent(2.0); // set zoom to 50%
    	camera.setAutoFitView(false);
    	// END (2)


        // (3) set up the GUI
		this.setTitle("Ambulance Location: Graph Layout Creator"); // window title
        this.setExtendedState(MAXIMIZED_BOTH); // full screen

        // (3a) create settings panel
		JPanel settings = new JPanel(new SpringLayout());
        // (3a-i) create text boxes
		gridSizeText = new JTextField(""+gridSize,2);
		nodeSizeText = new JTextField(""+nodeSize,2);
		textSizeText = new JTextField(""+textSize,2);
		nodeDistanceText = new JTextField(""+nodeDistance,2);
		// (3a-ii) add labels and text boxes
		settings.add(new JLabel("Grid Size:"));
		settings.add(gridSizeText);
		settings.add(new JLabel("Node Size:"));
		settings.add(nodeSizeText);
		settings.add(new JLabel("Text Size:"));
		settings.add(textSizeText);
		settings.add(new JLabel("Node Distance:"));
		settings.add(nodeDistanceText);
		SpringUtilities.makeCompactGrid(settings, 4, 2, 6, 6, 6, 6); // arrange elements in 4x2 grid
		settings.setMaximumSize(new Dimension(0,260)); //limit height to 260 pixels (otherwise textboxes are oversized)

		// (3b) create buttons and their respective action listeners
		autolayoutButton = new JToggleButton("Switch to Manual Layout");
		loadCoordinatesButton = new JButton("Load Coordinates from File");
		applySettingsButton = new JButton("Apply Settings");
		coordinateSystemButton = new JToggleButton("Show Coordinate System");
		autoFitButton = new JButton("Auto Fit to Grid");
		centerViewButton = new JButton("Center View");
		overlapButton = new JToggleButton("Show Overlap");
		coordinatesButton = new JButton("Copy Coordinates in Clipboard");
		tikzButton = new JButton("Copy Tikz Code in Clipboard");
		savePNGButton = new JButton("Save Screenshot as PNG");
		saveSVGButton = new JButton("Save Screenshot as SVG");
		autolayoutButton.addActionListener(this);
		loadCoordinatesButton.addActionListener(this);
		applySettingsButton.addActionListener(this);
		coordinateSystemButton.addActionListener(this);
		autoFitButton.addActionListener(this);
		centerViewButton.addActionListener(this);
		overlapButton.addActionListener(this);
		tikzButton.addActionListener(this);
		savePNGButton.addActionListener(this);
		saveSVGButton.addActionListener(this);
		autolayoutButton.setPreferredSize(new Dimension(0, 40)); // make layout button larger

		// (3c) create menu panel and add buttons and settings panel to it
        menuPanel = new JPanel(); //the menu panel
        menuPanel.setLayout(new SpringLayout());
		menuPanel.add(Box.createRigidArea(new Dimension(0,20))); // blank space of 20 pixels
        menuPanel.add(autolayoutButton);
        menuPanel.add(loadCoordinatesButton);
        menuPanel.add(Box.createRigidArea(new Dimension(0,40))); // blank space of 40 pixels
        menuPanel.add(centerViewButton);
        menuPanel.add(coordinateSystemButton);
        menuPanel.add(autoFitButton);
        menuPanel.add(overlapButton);
		menuPanel.add(Box.createRigidArea(new Dimension(0,40))); // blank space of 40 pixels
		menuPanel.add(new JLabel("Settings"));
		menuPanel.add(settings);
		menuPanel.add(applySettingsButton);
		menuPanel.add(Box.createRigidArea(new Dimension(0,40))); // blank space of 40 pixels
		menuPanel.add(coordinatesButton);
		menuPanel.add(tikzButton);
		menuPanel.add(savePNGButton);
		menuPanel.add(saveSVGButton);
		menuPanel.add(new JPanel()); // Empty panel. Necessary to stabilize to arrangement of elements.
		SpringUtilities.makeCompactGrid(menuPanel, 18, 1, 6, 6, 6, 6); // arrange elements in 17x1 grid


		// (4) add panels to main panel and finalize settings
        this.add((JPanel) view, BorderLayout.CENTER); //add graph viewer to main panel
        this.add(menuPanel, BorderLayout.EAST); //add button menu to main panel, right of the viewer
        this.setVisible(true); //set main panel visible
        view.addMouseListener(this); // add mouse listener for graph viewer
        ((JPanel)view).addMouseWheelListener(this); // add mouse wheel listener for graph viewer
        // define buttons which are (de-)activated depending on the layout mode
        deactivatableComponents.add(autoFitButton);
        deactivatableComponents.add(coordinateSystemButton);
        deactivatableComponents.add(overlapButton);
        deactivatableComponents.add(coordinatesButton);
        deactivatableComponents.add(tikzButton);
        deactivatableComponents.add(savePNGButton);
        deactivatableComponents.add(saveSVGButton);
        for (JComponent jc : deactivatableComponents) jc.setEnabled(false); // deactivate for default layout (automatic)

    }
    
    
    /**
     * Adjusts the graph within the coordinate system. Both the the minimum x-value and y-value
     * are set to 0. All nodes are moved accordingly. Thus, the graph itself is not changed, but only
     * moved within the coordinate system.
     * In other words, the graph is placed in the first quadrant of the coordinate system as far left and low as possible.
     */
    public void adjustCoordinates() {
    	double minX = getMinX(); // minimum x value of any node
    	double minY = getMinY(); // minimum y value of any node
    	
    	for (Node n : graph) { // for all nodes
    		if (n.getAttribute("ui.class") != "coordinateSystem") { // only move actual graph nodes (not those from the coordinate system)
    			double x = n.getAttribute("x"); // get x-value
    			double y = n.getAttribute("y"); // get y-value
    			n.setAttribute("x", x-minX); // adjust x-value
    			n.setAttribute("y", y-minY); // adjust y-value
    		}
    	}
    	// move view center of the camera accordingly
    	Point3 cam = camera.getViewCenter();
    	camera.setViewCenter(cam.x-minX, cam.y-minY, cam.z);
    }
    
    
    /**
     * Centers the view in the middle of the graph.
     */
    public void setViewCenter() {
    	double zoom = camera.getViewPercent(); //save old zoom
    	camera.resetView(); //center view in the middle of the graph (zoom set to 100%)
    	camera.setViewPercent(zoom); //restore old zoom
    }
    
    
    /**
     * Draws a coordinate system.
     * The coordinate system consists of a grid as well as a horizontal and a vertical scale.
     * The granularity of the grid depends on the parameter gridSize and 
     * determines the effect of the auto fit function of the Graph Layout Editor.
     * 
     * The coordinate system is made up of nodes and edges that are technically part of the graph.
     * However, these elements are explicitly marked and will be disregarded in the image output of the Editor.
     */
    public void addCoordinateSystem() {
    	adjustCoordinates(); // move graph to first quadrant of the coordinate system
    	double maxX = Math.round(Math.ceil(getMaxX()/gridSize)*gridSize*100.0)/100.0;
    	double maxY = Math.round(Math.ceil(getMaxY()/gridSize)*gridSize*100.0)/100.0;
    	
    	// iterate the coordinate system horizontally
    	// (number of steps depends on the parameter gridSize)
    	for (double x=0.0; x<=maxX; x=Math.round((x+gridSize)*100.0)/100.0) {
    		// (i) create node and define its settings 
    		graph.addNode("X_scale"+x); // create node for this step of the horizontal scale 
    		graph.getNode("X_scale"+x).addAttribute("ui.label", x); //add x-value as label
    		graph.getNode("X_scale"+x).addAttribute("ui.style", "text-alignment: under;");
    		graph.getNode("X_scale"+x).addAttribute("ui.style", "size: 0.01gu,0.1gu;"); // display node as vertical line with width 0.01 and length 0.1
    		graph.getNode("X_scale"+x).addAttribute("ui.class", "coordinateSystem");
    		graph.getNode("X_scale"+x).addAttribute("xy", x, -(nodeSize/2.0+0.1)); // place node with a distance of 0.1 of the lowest node (center of lowest node is at y=0)
    		
    		// (ii) draw vertical line of the grid at x
    		String name = "X";
    		for (double y : new double[]{0.0,maxY}) { //grid spans vertically from y=0 to y=maxY			
    			if (y==maxY) name = "X'";
    			graph.addNode(name+x); // create node for the grid
    			graph.getNode(name+x).addAttribute("ui.style", "size: 0gu;"); // size of node=0 => node is invisible
    			graph.getNode(name+x).addAttribute("ui.class", "coordinateSystem");
    			graph.getNode(name+x).addAttribute("xy", x,y); // place node
    		}
    		graph.addEdge("X"+x, "X"+x, "X'"+x).addAttribute("ui.class", "grid"); //create vertical grid line as edge between the two grid nodes
    	}
    	graph.addEdge("X_scale", "X_scale0.0", "X_scale"+maxX).addAttribute("ui.class", "scale"); //draw horizontal line for the x-scale
    	
    	
    	// iterate the coordinate system vertically
    	// (number of steps depends on the parameter gridSize)
    	for (double y=0.0; y<maxY+gridSize; y=Math.round((y+gridSize)*100.0)/100.0) { //because scale is from 0 to maxY
    		// (i) create node and define its settings 
    		graph.addNode("Y_scale"+y); // create node for this step of the vertical scale
    		graph.getNode("Y_scale"+y).addAttribute("ui.label", y); // add y-value as label
    		graph.getNode("Y_scale"+y).addAttribute("ui.style", "text-alignment: at-left;");
    		graph.getNode("Y_scale"+y).addAttribute("ui.style", "size: 0.1gu,0.01gu;"); // display node as horizontal line with width 0.01 and length 0.1
    		graph.getNode("Y_scale"+y).addAttribute("ui.class", "coordinateSystem");
    		graph.getNode("Y_scale"+y).addAttribute("xy", -(nodeSize/2.0+0.1), y); // place node with a distance of 0.1 of the most left node (center of most left node is at x=0)
    		
    		// (ii) draw vertical line of the grid at y
    		String name = "Y";
    		for (double x=0.0; x<=maxX; x=x+maxX) { //grid spans horizontally from x=0 to x=maxX  			
    			if (x==maxX) name = "Y'";
    			graph.addNode(name+y); // create node for the grid
    			graph.getNode(name+y).addAttribute("ui.style", "size: 0gu;"); // size of node=0 => node is invisible
    			graph.getNode(name+y).addAttribute("ui.class", "coordinateSystem");
    			graph.getNode(name+y).addAttribute("xy", x, y); // place node
    		}
    		graph.addEdge("Y"+y, "Y"+y, "Y'"+y).addAttribute("ui.class", "grid"); // create horizontal grid line as edge between the two grid nodes
    	}
    	graph.addEdge("Y_scale", "Y_scale0.0", "Y_scale"+maxY).addAttribute("ui.class", "scale"); // draw vertical line for the y-scale
    	
    	//update button accordingly
    	coordinateSystemButton.setSelected(true);
    }
    
    /**
     * Remove the coordinate system.
     */
    public void removeCoordinateSystem() {
    	for (Node n: graph) { //iterate all nodes of the GraphStream graph
    		//remove all nodes of the coordinate system
    		if (n.getAttribute("ui.class") == "coordinateSystem") {
    			graph.removeNode(n);
    		}
    	}
    	// check if all nodes really have been removed (GraphStream doesn't always catch all at first try)
    	// (graph.getNodeCount() relates to GraphStream graph, numberOfNodes relates to BasicGraph)
    	if (graph.getNodeCount()>numberOfNodes) removeCoordinateSystem(); //if not, try again
    	else coordinateSystemButton.setSelected(false); //if yes, update button accordingly
    }
    
    
    /**
     * Implements ALL button handlers centrally.
     */
    @Override
	public void actionPerformed(ActionEvent actionEvent) {
    	fromViewer.pump(); // refresh GraphStream viewer (!)
    	
    	
    	/*
    	 * (1) Auto layout button
    	 * Switches between the graph layout modes AUTO and MANUAL.
    	 */
		if (actionEvent.getSource() == autolayoutButton) {
			if (!autolayoutButton.isSelected()) { // if manual layout is activated, switch to auto layout
				autolayoutButton.setText(("Switch to Manual Layout"));
				if (coordinateSystemButton.isSelected()) coordinateSystemButton.doClick(); //remove grid if activated
				for (JComponent jc : deactivatableComponents) jc.setEnabled(false); // deactivate non-dedicated buttons
				viewer.enableAutoLayout();
				setViewCenter();
			}
			else { // if auto layout is activated, switch to manual layout
				for (JComponent jc : deactivatableComponents) jc.setEnabled(true); //activate dedicated buttons
				autolayoutButton.setText(("Switch to Automatic Layout"));
				viewer.disableAutoLayout();
				for (Node n : graph) { // for all nodes
					// transfer auto layout coordinates to manual coordinates
		    		n.addAttribute("x", (double) Math.round(GraphPosLengthUtils.nodePosition(n)[0]*1000)/1000);
		    		n.addAttribute("y", (double) Math.round(GraphPosLengthUtils.nodePosition(n)[1]*1000)/1000);
				}				
				adjustCoordinates();			
			}
		}
		
		/*
		 * (2) Load coordinates button
		 * Restores coordinates from the graph import file (if available).
		 */
		if (actionEvent.getSource() == loadCoordinatesButton) {
			for (graph.Node node : basicGraph.nodes) { // for all nodes of the BasicGraph
				String id = ""+node.index;	
				//save BasicGraph coordinates to GraphStream graph
				graph.getNode(id).addAttribute("x", node.coordinates.x);
				graph.getNode(id).addAttribute("y", node.coordinates.y);
			}
		}
		
		/*
		 *  (3) Center view button
		 *  Centers the view in the middle of the graph.
		 */
		if (actionEvent.getSource() == centerViewButton) setViewCenter();
		
		/*
		 *  (4) Overlap button
		 *  Displays nodes and edges that overlap in the current layout.
		 *  Overlapping nodes and edges are marked and thus fall under a different section in the style sheet.
		 */
		if (actionEvent.getSource() == overlapButton) {			
			if (overlapButton.isSelected()) { // if overlap mode is disabled, enable it
				if (coordinateSystemButton.isSelected()) coordinateSystemButton.doClick(); // hide coordinate system if it is activated
				overlapButton.setText(("Hide Overlap")); // set caption of the button
				
				// disable all buttons except the overlap button itself
				for (Component c : menuPanel.getComponents()) c.setEnabled(false);
				overlapButton.setEnabled(true);
				
				// (i) retrieve nodes overlapping with nodes
				for (int i=1; i<=numberOfNodes-1; i++) { // for all nodes i
					Node node1 = graph.getNode(""+i);
					for (int j=i+1; j<=numberOfNodes; j++) { // for all nodes j	with higher id than i		
						Node node2 = graph.getNode(""+j);
						// check if node i and j are overlapping
						if (Math.abs((double)node1.getAttribute("x") - (double)node2.getAttribute("x")) <= nodeSize &&
							Math.abs((double)node1.getAttribute("y") - (double)node2.getAttribute("y")) <= nodeSize ) {
							
							// mark both nodes as overlapping
							node1.setAttribute("ui.class", "overlap");
							node2.setAttribute("ui.class", "overlap");
						}
					}
				}		
				
				// (ii) retrieve edges overlapping with nodes
				for (Edge e : graph.getEachEdge()) { // for all edges of the graph
					String[] nodes = e.getId().split("-"); // extract the corresponding nodes
					// create points that resemble the nodes
					Point2D p1 = new Point2D.Double(graph.getNode(nodes[0]).getAttribute("x"), graph.getNode(nodes[0]).getAttribute("y"));
					Point2D p2 = new Point2D.Double(graph.getNode(nodes[1]).getAttribute("x"), graph.getNode(nodes[1]).getAttribute("y"));
					// create a line that resembles the edge (using the nodes coordinates)
					LineSegment2D edge = new LineSegment2D(p1.getX(), p1.getY(), p2.getX(), p2.getY());
					
					// check if edge intersects with a node
					for (Node n : graph) { // for all nodes
						//skip node if it is one of the edge nodes
						if (n.getId().equals(nodes[0]) || n.getId().equals(nodes[1])) continue;
						
						// create a circle that resembles the node
						Circle2D circle = new Circle2D((double)n.getAttribute("x"), (double)n.getAttribute("y"), nodeSize/2.0);
						
						// check if the line (edge) intersects with the circle
						if (circle.intersections(edge).size()>0) { //if there are intersections
							e.setAttribute("ui.class", "overlap"); // mark edge as overlapping
							break; // break loop, investigate next edge						
						}						
					}
				}	
			}
			else {  // if overlap mode is enabled, disable it
				for (Component c : menuPanel.getComponents()) c.setEnabled(true); // activate all buttons
				overlapButton.setText(("Show Overlap")); // set button caption
				
				// unmark all marked nodes and edges
				for (int i=1; i<= numberOfNodes; i++) {
					Node node = graph.getNode(""+i);
					if (node.getAttribute("ui.class") == "overlap") node.removeAttribute("ui.class");
				}
				for (Edge e : graph.getEachEdge()) {
					if (e.getAttribute("ui.class") == "overlap") e.removeAttribute("ui.class");
				}
			}
		}
		
		/*
		 * (5) Apply settings button
		 * Reads values in the settings text boxes and applies them.
		 */
		if (actionEvent.getSource() == applySettingsButton) {
			
			// (i) read grid size
			double gridSizeOld = gridSize;
			gridSize = Math.round(Double.parseDouble(gridSizeText.getText())*100.0)/100.0; // read value from text box
			// re-draw coordinate system if grid size has changed
			if (gridSizeOld != gridSize && coordinateSystemButton.isSelected()) {
				removeCoordinateSystem();
				addCoordinateSystem();
			}
			
			// (ii) read node size and text size
			nodeSize = Math.round(Double.parseDouble(nodeSizeText.getText())*100.0)/100.0; // read value from text box		
			// apply node size to style sheet
			styleSheet = styleSheet.replaceFirst("size: \\d+.\\d+gu;", "size: "+nodeSize+"gu;");
			
			// (iii) read text size
			textSize = Math.round(Double.parseDouble(textSizeText.getText())*100.0)/100.0; // read value from text box

			// (iv) read node distance
			nodeDistance = Math.round(Double.parseDouble(nodeDistanceText.getText())*100.0)/100.0;  // read value from text box
			// apply node distance to edges
			for (Edge e : graph.getEachEdge()) e.setAttribute("layout.weight", nodeDistance);
		}
		
		/*
		 * (6) Auto fit button
		 * Adjust node positions to the nearest grid node.
		 */
		if (actionEvent.getSource() == autoFitButton) {
			for (Node n : graph) { // for all nodes
				if (n.getAttribute("ui.class") != "coordinateSystem") { // only nodes of the graph, not the coordinate system
					//round node positions according to grid size
					n.setAttribute("x", Math.round((double)n.getAttribute("x")/gridSize)*gridSize);
					n.setAttribute("y", Math.round((double)n.getAttribute("y")/gridSize)*gridSize);
				}
			}		
		}
		
		/*
		 * (7) Coordinate system button
		 * Adjust node positions to the nearest grid node.
		 */
		if (actionEvent.getSource() == coordinateSystemButton) {
			if (!coordinateSystemButton.isSelected()) { // if coordinate system is drawn, remove it
				removeCoordinateSystem();
				coordinateSystemButton.setText("Show Coordinate System");
			}
			else { // if coordinate system is deactivated, draw it
				addCoordinateSystem();
				coordinateSystemButton.setText("Hide Coordinate System");
			}
		}
		
		/*
		 * (8) Copy coordinates button
		 * Copies the coordinates of all nodes in the clipboard in a way
		 * that they can be pasted into an Excel spreadsheet.
		 * Each node's coordinates are written in a new row with 
		 * respective columns for x-values and y-values.
		 */
		if (actionEvent.getSource() == coordinatesButton) {			
			String coordinates = "";
			for (Node n : graph) { // for all nodes
				coordinates += n.getAttribute("x") + "\t" + n.getAttribute("y") + "\n"; // save coordinates
				coordinates = coordinates.replace(".", ","); // change number format to German ","
			}
			
			// copy text in clipboard
			StringSelection selection = new StringSelection(coordinates);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(selection, selection);
		}
		
		/*
		 * (9) tikz button button
		 * Copies the tikz code that describes the graph in the clipboard.
		 */
		if (actionEvent.getSource() == tikzButton) {
			// (i) write tikz initialization code
			String tikzCode =	"\\begin{figure}[ht!]\n" +
								"\\begin{center}\n" +
								"\\begin{tikzpicture}\n";
			
			// (ii) write code for nodes
			for (Node n : graph) { // for all nodes
				if (n.hasAttribute("ui.class")) break; //break loop when all graph nodes are iterated (only coordinate system nodes have the "class" attribute)
				tikzCode += "\\node[circle, draw] (n"+n.getId() +") at ("+n.getAttribute("x")+","+((double)n.getAttribute("y"))+") {"+n.getAttribute("ui.label")+"};\n";
			}
			
			// (iii) write code for edges
			tikzCode += "\\path\n";
			for (Edge e:graph.getEachEdge()) {
				if (e.hasAttribute("ui.class")) break; //break loop when all graph edges are iterated (only coordinate system edges have the "class" attribute)
				tikzCode += "\t(n"+e.getId().split("-")[0]+") edge (n"+e.getId().split("-")[1]+")\n";
			}
			tikzCode = tikzCode.substring(0, tikzCode.length()-1); // remove last character
			
			// (iv) write tikz end code
			tikzCode += ";\n" +
						"\\end{tikzpicture}\n" +
						"\\caption{PLACEHOLDER}\n" +
						"\\label{PLACEHOLDER}\n" +
						"\\end{center}\n" +
						"\\end{figure}";
			
			// (v) copy text in clipboard
			StringSelection selection = new StringSelection(tikzCode);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(selection, selection);
		}
		
		/*
		 * (10) Save PNG button
		 * Creates a PNG image based on an SVG image.
		 * The user specifies the width of the image, the height is derived automatically.
		 */
		if (actionEvent.getSource() == savePNGButton) {
			
			// (i) create an SVG file first
			saveSVGButton.doClick();
			
			// (ii) ask user for image width in dialog box
			String width = (String)JOptionPane.showInputDialog(this, "Enter image width", "Graph Layout",
		        	JOptionPane.PLAIN_MESSAGE, null, null, 2000);
			
			// (iii) create the PNG image (copy & pasted from tutorial)
			PNGTranscoder t = new PNGTranscoder();
			// Set the transcoding hints.
			// image height is calculated from image width
			t.addTranscodingHint(JPEGTranscoder.KEY_WIDTH, new Float(width)); //set image width
			t.addTranscodingHint(JPEGTranscoder.KEY_HEIGHT, new Float((new Double(width)*((getMaxY()+1)/(getMaxX()+1))))); //set image height
			
			try {
				// Create the transcoder input (from SVG).
				@SuppressWarnings("deprecation")
				String svgURI = new File("graph.svg").toURL().toString(); //load SVG
				TranscoderInput input = new TranscoderInput(svgURI);			              
				// Create the transcoder output.
				OutputStream ostream = new FileOutputStream("graph.png");
				TranscoderOutput output = new TranscoderOutput(ostream);
				// Save the image.
				t.transcode(input, output);
				// Flush and close the stream.
				ostream.flush();
				ostream.close();
				//confirmation dialog box
	    		JOptionPane.showMessageDialog(this, "\"graph.png\" saved.", "Graph Layout", JOptionPane.DEFAULT_OPTION);
			} catch (Exception e) {
				e.printStackTrace();
			}      
		}
		
		/*
		 * (11) Save SVG button
		 * Creates an vector-graphic SVG image.
		 */
		if (actionEvent.getSource() == saveSVGButton) {
			if (coordinateSystemButton.isSelected()) coordinateSystemButton.doClick(); //deactivate coordinate system if activated
			adjustCoordinates();
			adjustTextSize();
			double border = 0.5*nodeSize+0.1; //white border of the graphic of 0.1
			
			// element styles		
			String nodeStyle = " fill=\"white\" stroke=\"black\" stroke-width=\"0.005\" ";
			String edgeStyle = " style=\"stroke:black;stroke-width:0.005\" ";
			String textStyle = "text-anchor=\"middle\" dy=\".3em\" font-size=\""+ textSize +"\" font-family=\"Times New Roman\" ";
			
			// (i) write SVG initialization code
			String svg = "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>\r\n" +
				"<svg xmlns=\"http://www.w3.org/2000/svg\"\r\n" +
				"viewBox=\""+(-border) +" "+(-border)+" "+(getMaxX()+(2.0*border)) +" "+ (getMaxY()+(2.0*border)) +"\">\r\n" +
				"<g>\r\n";	
			
			// (ii) write code for edges
			for(Edge e:graph.getEachEdge()) {
				double x1,x2,y1,y2; // edge coordinates
				x1 = e.getNode0().getAttribute("x");
				y1 = e.getNode0().getAttribute("y");
				x2 = e.getNode1().getAttribute("x");
				y2 = e.getNode1().getAttribute("y");
				svg += "<line x1=\""+x1+"\" y1=\""+(getMaxY()-y1)+"\" x2=\""+x2+"\" y2=\""+(getMaxY()-y2)+"\""+edgeStyle+" />\r\n";
			}
			// (iii) write code for nodes
			for (Node n : graph) {
				double x,y;
				x = (double)n.getAttribute("x");
				y = getMaxY()-(double)n.getAttribute("y");
				svg += "<g><circle cx=\""+x+"\" cy=\""+y+"\" r=\""+nodeSize/2.0+"\""+nodeStyle+"/> " +
						"\r\n<text "+textStyle+"x=\""+x+"\" y=\""+y+"\" >"+n.getAttribute("ui.label")+"</text></g>\r\n";
			}
			
			svg += "</g></svg>"; // finish SVG code
		
			// (iv) write SVG file
			try {
				Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("graph.svg"), "UTF-8"));
				out.write(svg);
				out.close();
			} catch (IOException e1) {}			
		}
					
	}
    
    
    /**
     * Returns the highest value for the y-coordinate of any node.
     * @return Maximum y-coordinate.
     */
    public double getMaxY() {
    	double maxY = Double.MIN_VALUE, y;
    	for (Node n : graph) {
    		if (n.getAttribute("ui.class") != "coordinateSystem") { // check only nodes of the graph
    			y = n.getAttribute("y");
    			if (y > maxY) maxY = y;
    		}
    	}
    	return maxY;
    }

    /**
     * Returns the highest value for the x-coordinate of any node.
     * @return Maximum x-coordinate.
     */
	public double getMaxX() {
		double maxX = Double.MIN_VALUE, x;
    	for (Node n : graph) {
    		if (n.getAttribute("ui.class") != "coordinateSystem") {  // check only nodes of the graph
    			x = n.getAttribute("x");
    			if (x > maxX) maxX = x;
    		}
    	}
    	return maxX;
    }
	
	/**
     * Returns the lowest value for the x-coordinate of any node.
     * @return Minimum x-coordinate.
     */
    public double getMinX() {
    	double minX = Double.MAX_VALUE, x;
    	for (Node n : graph) {
    		if (n.getAttribute("ui.class") != "coordinateSystem") {
    			x = n.getAttribute("x");
    			if (x < minX) minX = x;
    		}
    	}
    	return minX;
    }
    
    /**
     * Returns the lowest value for the y-coordinate of any node.
     * @return Minimum y-coordinate.
     */
    public double getMinY() {
    	double minY = Double.MAX_VALUE, y;
    	for (Node n : graph) {
    		if (n.getAttribute("ui.class") != "coordinateSystem") {
    			y = n.getAttribute("y");
    			if (y < minY) minY = y;
    		}
    	}
    	return minY;
    }
    

    /**
     * Necessary GraphStream method.
     */
    public void viewClosed(String id) {}
 
    /**
     * Necessary GraphStream method.
     */
    public void buttonPushed(String id) {
        //System.out.println("Button pushed on node "+id);
    	//System.out.println(graph.getNode(id).getAttribute("x")+","+graph.getNode(id).getAttribute("y"));
    }
 
    /**
     * Updates the coordinates of a node when a it is moved and released.
     */
    public void buttonReleased(String id) {
        Node n = graph.getNode(id);
        n.setAttribute("x", GraphPosLengthUtils.nodePosition(n)[0]);
    	n.setAttribute("y", GraphPosLengthUtils.nodePosition(n)[1]);        
    }
    

    /**
     * Mouse listener that controls the camera movement (PART I).
     * Saves the current mouse position when right-clicking.
     */    
    @Override
    public void mousePressed(MouseEvent e) {
    	if(e.getButton() == MouseEvent.BUTTON3) { //BUTTON3 = right mouse click
    		mouseX = e.getX();
    		mouseY = e.getY();
    	}
     }

     /**
      * Mouse listener that controls the camera movement (PART II).
      * Updates the camera position after right mouse has been released.
      */
     @Override
     public void mouseReleased(MouseEvent e) {
    	 if(e.getButton() == MouseEvent.BUTTON3) { //BUTTON3 = right mouse click
    		//calculate the pixels the mouse has moved from click to release
     		int moveX = e.getX() - mouseX;
     		int moveY = e.getY() - mouseY;
     		//transform camera position into pixels
     		Point3 centerGu = camera.transformGuToPx(camera.getViewCenter().x,camera.getViewCenter().y,camera.getViewCenter().z);
     		//move camera in pixels and re-transform to graph units
     		Point3 centerPx = camera.transformPxToGu(centerGu.x-moveX, centerGu.y-moveY);
     		camera.setViewCenter(centerPx.x,centerPx.y,centerPx.z);
     	}
     }


     /**
      * Necessary MouseListener method.
      */
	@Override
	public void mouseClicked(MouseEvent e) {}
	 /**
     * Necessary MouseListener method.
     */
	@Override
	public void mouseEntered(MouseEvent e) {}
	 /**
     * Necessary MouseListener method.
     */
	@Override
	public void mouseExited(MouseEvent e) {}


	/**
     * Mouse listener that controls the camera zoom.
     * Scrolling up zooms in, scrolling down zooms out.
     * The zoom is bound to values between 30% and 200% zoom
    */
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		double zoom = Math.min(3,Math.max(0.5, camera.getViewPercent()+0.1*e.getWheelRotation()));
		camera.setViewPercent(zoom);
		adjustTextSize();
	}
	
	/**
	 * Adjust the GraphStream text size.
	 * The text size in GraphStream can only be specified in pixels.
	 * In contrast, all other values are based on GraphUnits (gu).
	 * Hence, text size needs to be adjusted when the zoom is changed.
	 */
	public void adjustTextSize() {
		// creates two points in a distance of the textSize parameter and transform the GraphUnit coordinates to pixels
		Point3 point1 = camera.transformGuToPx(camera.getViewCenter().x, camera.getViewCenter().y, camera.getViewCenter().z);
		Point3 point2 = camera.transformGuToPx(camera.getViewCenter().x, camera.getViewCenter().y+textSize, camera.getViewCenter().z);
		// update the style sheet String
		styleSheet = styleSheet.replaceFirst("text-size: \\d+px;", "text-size: "+Math.round(point1.y-point2.y)+"px;");
		// update the style sheet in GraphStream
		graph.setAttribute("ui.stylesheet", styleSheet);
	}
}
