package edu.kit.ksri.als.gui;


import edu.kit.ksri.als.ambulanceLocation.ProblemLocation;
import edu.kit.ksri.als.ambulanceLocation.Solution;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.TreeSet;

/**
 * GUI providing the means to operate the location process for the ALWUD model.
 * Enables user to enter the inputs required and define the data output of the process.
 * The GUI features batch executions of different parameter setups using dedicated separators in the text fields as well as specified check boxes.
 * For more information, please check the manual of the program.
 *
 */
@SuppressWarnings("serial")
public class TabLocation extends JPanel{
	
	MainFrame menu;	
	
	// parameter input elements
	JComboBox<String> graphComboBox;
	JTextField betaField, mField, nField, alphaField, baseSeedField;
	JCheckBox iterateCheckBox;
	
	// parameter input sets (the combination of these sets makes up the batch)
	TreeSet<String> graphSet = new TreeSet<String>();
	TreeSet<Double> betaSet = new TreeSet<Double>();
	TreeSet<Integer> mSet = new TreeSet<Integer>();
	TreeSet<Integer> nSet = new TreeSet<Integer>();
	TreeSet<Integer> baseSeedSet = new TreeSet<Integer>();
	TreeSet<Double> alphaSet = new TreeSet<Double>();
	
	// output settings elements
	JCheckBox exportSampleSolutionsCheckBox, exportSampleDemandsCheckBox;
	
	/**
	 * Creates GUI for operating the execution of the location process of the ALWUD model.
	 * @param menu Link to the main window.
	 */
	public TabLocation(MainFrame menu) {
		this.menu = menu;	
		
		// create GUI elements
		// (1) input interfaces
		graphComboBox = new JComboBox<String>();
		betaField = new JTextField("0.0", 4);
	    mField = new JTextField("10", 4);	    
	    nField = new JTextField("25", 4);	    
	    alphaField = new JTextField("0.99", 4);
	    baseSeedField = new JTextField("1", 4);	
		iterateCheckBox = new JCheckBox("", false); // dis-/enables the batch execution of all imported graphs

		// (2) output interfaces
		JCheckBox makeAvailableForAssignmentCheckBox = new JCheckBox("Make Solution Available for Assignment", true);
		exportSampleSolutionsCheckBox = new JCheckBox("Export Solution of Samples", false); // dis-/enables the export of the solutions of the individual samples (additionally to the general constructed solution)
		exportSampleDemandsCheckBox = new JCheckBox("Export Demand Scenarios", false); // dis-/enables the export of all demand scenarios    
		// (2b) buttons
	    JButton optimalButton = new JButton("Solve optimally");
	    // procedure for solving the problem batch optimally
	    optimalButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {

				prepareProblemBatch(); // read and process input

				// run the batch in its own thread
				Thread queryThread = new Thread() {
					public void run() {
						for (String graph : graphSet) {	// run all graphs of the batch	
						for (double alpha : alphaSet) {	// run all alphas of the batch	
						for (double beta : betaSet) { // run all betas of the batch	
							
							// create problem
							menu.log("Solving "+graph+" (beta="+beta+",alpha="+alpha+") optimally..."); // write log						
							ProblemLocation problem = new ProblemLocation(menu.graphs.get(graph), beta, alpha); //create problem for the given input
							
							// solve problem and write output
							solveAndWriteOutput(problem);
							
							// make solution available for assignment process
							if (makeAvailableForAssignmentCheckBox.isSelected()) {
					    		saveSolutionForAssignment(graph, problem.solution);
				    		}
						}	
						}
						}
					}
				};
				queryThread.start();
				}
		});
	    JButton samplingButton = new JButton("Solve with sampling");
	    // procedure for solving the problem batch with the sampling method
	    samplingButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {

				prepareProblemBatch(); // read and process input

				// run the batch in its own thread
				Thread queryThread = new Thread() {
					public void run() {				    	  
						for (String graph : graphSet) {	// for all graphs		    		  
				    	for (double alpha : alphaSet) {	// for all alphas
				    	for (double beta : betaSet) { // for all betas
				    	for (int m : mSet) { // for all numbers of samples
				    	for (int n : nSet) { // for all sample sizes
				    	for (int baseSeed : baseSeedSet) { // for all base seeds
				    		
							// create problem
				    		menu.log("Solving "+graph+" (beta="+beta+",m="+m+",n="+n+",base seed="+baseSeed+",alpha="+alpha+") with sampling method...");
				    		ProblemLocation problem = new ProblemLocation(menu.graphs.get(graph),beta,m,n,baseSeed,alpha);

				    		// solve problem and write output
				    		solveAndWriteOutput(problem);
				    		
				    		// make solution available for assignment process
				    		if (makeAvailableForAssignmentCheckBox.isSelected()) {
					    		saveSolutionForAssignment(graph, problem.solution);
				    		}
				    	}
				    	}
				    	}
				    	}
				    	}
				    	}
				      }
			    };
			    queryThread.start();
			}
		});
	    
	    JButton boundsButton = new JButton("Export graph bounds");
	    // procedure for calculating the bounds of the graph
	    boundsButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				alphaSet = menu.parseTextFieldsDouble(alphaField); // read alphas
				for (double alpha : alphaSet) {	// for all alphas
					menu.currentExportData.write(menu.graphs.get(graphComboBox.getSelectedItem()).exportBounds(alpha), menu.currentExportPrefix+"_graph_bounds"); // write graph bound
					menu.log("Bounds of "+graphComboBox.getSelectedItem()+" (alpha="+alpha+") written in "+menu.currentExportData.fileName+"\\"+menu.currentExportPrefix+"_graph_bounds"+"."); // write log
				}
			}
		});
	    
		
	    
	    // create and arrange GUI elements
	    // (1) panel for selecting the graph
		JPanel graphPanel = new JPanel(new SpringLayout());
	    graphPanel.add(new JLabel("Select graph"));
	    graphPanel.add(graphComboBox);	    
	    SpringUtilities.makeCompactGrid(graphPanel, 1, 2, 6, 6, 6, 6);
	    
	    // (2) panel for entering the input
	    JPanel inputFieldsPanel = new JPanel(new SpringLayout());  
	    inputFieldsPanel.add(new JLabel("m"));
	    inputFieldsPanel.add(mField);
	    inputFieldsPanel.add(new JLabel("alpha"));
	    inputFieldsPanel.add(alphaField);
	    inputFieldsPanel.add(new JLabel("n"));
	    inputFieldsPanel.add(nField);	    
	    inputFieldsPanel.add(new JLabel("base seed"));
	    inputFieldsPanel.add(baseSeedField);
	    inputFieldsPanel.add(new JLabel("beta"));
	    inputFieldsPanel.add(betaField);
	    inputFieldsPanel.add(new JLabel("iterate all graphs"));
	    inputFieldsPanel.add(iterateCheckBox);
	    SpringUtilities.makeCompactGrid(inputFieldsPanel, 3, 4, 6, 6, 6, 6); // arrange elements in 3x4 grid

	    // (3) panel for entering output settings and starting the solving process
	    JPanel solveButtonsPanel = new JPanel(new SpringLayout());
	    solveButtonsPanel.add(makeAvailableForAssignmentCheckBox);
	    solveButtonsPanel.add(exportSampleSolutionsCheckBox);
	    solveButtonsPanel.add(exportSampleDemandsCheckBox);
	    solveButtonsPanel.add(optimalButton);
	    solveButtonsPanel.add(samplingButton);
	    solveButtonsPanel.add(boundsButton);	    
	    SpringUtilities.makeCompactGrid(solveButtonsPanel, 6, 1, 6, 6, 6, 6); // arrange elements in 6x1 grid

		// (4) main panel comprising the individual panels
	    JPanel mainPanel = new JPanel(new SpringLayout());
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
	    mainPanel.add(graphPanel);
	    mainPanel.add(new JPanel()); // empty space
	    mainPanel.add(inputFieldsPanel);
	    mainPanel.add(new JPanel()); // empty space
	    mainPanel.add(solveButtonsPanel);
	    mainPanel.add(new JPanel()); // empty space
	    this.add(mainPanel);
	}
	
	/**
	 * Reads and processes all input, thus creating a problem batch. 
	 * Different values may be assumed for the same input parameter.
	 * The batch is defined as all possible combinations of input parameters.
	 */
	void prepareProblemBatch() {
		// read relevant input that forms the problem batch
		mSet = menu.parseTextFieldsInteger(mField);
		nSet = menu.parseTextFieldsInteger(nField);
		baseSeedSet = menu.parseTextFieldsInteger(baseSeedField);		
		betaSet = menu.parseTextFieldsDouble(betaField); // read betas
		alphaSet = menu.parseTextFieldsDouble(alphaField); // read alphas
		// read graphs
		graphSet.clear();
		if (iterateCheckBox.isSelected()) { // if dedicated box checked
			// select all graphs in the combo box
			for (int i=0; i<graphComboBox.getItemCount(); i++) {
				graphSet.add(graphComboBox.getItemAt(i));						
			}
		}
		else {
			// select only the currently selected graph 
			graphSet.add((String) graphComboBox.getSelectedItem());
		}
	}
	
	/**
	 * Solves a given location problem and writes the output in the export file.
	 * @param problem Unsolved location problem.
	 */
	void solveAndWriteOutput(ProblemLocation problem){
		// solve problem
		problem.solve(); // solve the problem
		menu.log("Location problem solved."); // write log
		
		// write output
		menu.currentExportData.write(problem.exportSolution(), menu.currentExportPrefix+"_location"); // write the location solutions (standard data output)
		menu.log("Solution written in "+menu.currentExportData.fileName+"\\"+menu.currentExportPrefix+"_location.");

		if (exportSampleSolutionsCheckBox.isSelected()) {  // if dedicated check box activated...
			menu.currentExportData.write(problem.exportSolutionsOfSamples(), menu.currentExportPrefix+"_location_samples"); // write this one-sample-solution in the sheet for individual samples
			menu.log("Solutions for individual samples written in "+menu.currentExportData.fileName+"\\"+menu.currentExportPrefix+"_location_samples.");
		}
		if (exportSampleDemandsCheckBox.isSelected()) { // if dedicated check box activated...
			menu.currentExportData.write(problem.exportScenariosOfSamples(), menu.currentExportPrefix+"_scenarios"); // write all demand scenarios of the problem
			menu.log("Demand scenarios written in "+menu.currentExportData.fileName+"\\"+menu.currentExportPrefix+"_scenarios.");
		}
	}
	
	/**
	 * Makes solutions available for the assignment process.
	 * @param graph Name of the graph.
	 * @param solution Number of ambulances at the bases.
	 */
	void saveSolutionForAssignment(String graph, Solution solution) {
		if (menu.tabAssignment.solutions.get(graph) == null) { //if no entry for this graph yet
			menu.tabAssignment.solutions.put(graph, new HashSet<String>()); //create entry
		}
		menu.tabAssignment.solutions.get(graph).add(Arrays.toString(solution.z)); //add solution
		menu.tabAssignment.updateGraphComboBox();
	}
	
}
