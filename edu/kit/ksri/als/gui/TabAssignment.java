package edu.kit.ksri.als.gui;

import edu.kit.ksri.als.ambulanceLocation.ProblemAssignment;
import edu.kit.ksri.als.ambulanceLocation.Solution;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

/**
 * GUI providing the means to operate the assignment process for the AAP model.
 * Enables user to enter the inputs required and define the data output of the process.
 * The GUI features batch executions of different parameter setups using dedicated separators in the text fields as well as specified check boxes.
 * For more information, please check the manual of the program.
 *
 */
@SuppressWarnings("serial")
public class TabAssignment extends JPanel{
	
	MainFrame menu;	
	HashMap<String,HashSet<String>> solutions = new HashMap<String,HashSet<String>>(); //container for all solutions
	JComboBox<String> graphComboBox, solutionComboBox;
	
	JCheckBox iterateAllGraphsCheckBox, iterateAllSolutionsOfAGraphCheckBox;
	JCheckBox exportSampleDemandsCheckBox, exportSampleSolutionsCheckBox,
		exportAssignmentCompleteCheckBox, exportAssignmentPerNodeCheckBox;
    
	
	JTextField mField, nField, alphaField, baseSeedField;
	
	TreeSet<String> graphSet = new TreeSet<String>();
	HashMap<String,HashSet<String>> solutionsSet = new HashMap<String,HashSet<String>>();
	TreeSet<Integer> mSet = new TreeSet<Integer>();
	TreeSet<Integer> nSet = new TreeSet<Integer>();
	TreeSet<Integer> baseSeedSet = new TreeSet<Integer>();
	TreeSet<Double> alphaSet = new TreeSet<Double>();
	
	/**
	 * Creates GUI for operating the execution of the assignment process of the AAP model.
	 * @param menu Link to the main window.
	 */
	public TabAssignment(MainFrame menu) {
		this.menu = menu;	

		
		// create GUI elements: (1) solutions
		graphComboBox = new JComboBox<String>();
		solutionComboBox = new JComboBox<String>();
		JComboBox<String> assignmentTypeComboBox = new JComboBox<String>(new String[]{"whole scenarios","part scenarios","max service level"});
	    
		// procedure for displaying the corresponding solutions to a graph
		graphComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) { // when a different graph is selected
				
				String selectedGraph = (String) graphComboBox.getSelectedItem(); // read selected graph
				if (selectedGraph == null) return; // if no graph is selected, exit
				
				solutionComboBox.removeAllItems(); // clear solution combo box
				for (String solution : solutions.get(selectedGraph)) { // for all available solutions of the selected graph
					solutionComboBox.addItem(solution); // fill solution combo box
				};
			}
		});
		
		
		// create GUI elements: (2) input interfaces
	    mField = new JTextField("1000", 4);	    
	    nField = new JTextField("1", 4);	    
	    alphaField = new JTextField("0.95", 4);
	    baseSeedField = new JTextField("1", 4);	
		
		iterateAllGraphsCheckBox = new JCheckBox("", false);  // dis-/enables the batch execution of all imported graphs
		iterateAllSolutionsOfAGraphCheckBox = new JCheckBox("", false);  // dis-/enables the batch execution of all available solutions for a graph
	    // procedure to guarantee the correct selection of check boxes
		iterateAllGraphsCheckBox.addActionListener(new ActionListener() {	
			// if graph check box is selected, solutions check box must be selected too!
			@Override 
			public void actionPerformed(ActionEvent e) {
				if (iterateAllGraphsCheckBox.isSelected()) { // if graph check box is activated
					iterateAllSolutionsOfAGraphCheckBox.setSelected(true); // make sure solutions check box is activated too
					iterateAllSolutionsOfAGraphCheckBox.setEnabled(false); // deactivate solutions check box (forbid user to change this setting)
				}
				else { // if graph check box is deactivated
					iterateAllSolutionsOfAGraphCheckBox.setEnabled(true); // activate solutions check box
				}
			}
		});
		
		
		// create GUI elements: (3) export settings
		exportSampleDemandsCheckBox = new JCheckBox("Export Demand Scenarios", false);
		exportSampleSolutionsCheckBox = new JCheckBox("Export Solution of Samples", false);
		exportAssignmentPerNodeCheckBox = new JCheckBox("Export Assignment per Node", false);
		exportAssignmentCompleteCheckBox = new JCheckBox("Export Complete Assignment", false);
		
		
		// create GUI elements: (4) buttons
		JButton optimalButton = new JButton("Solve optimally");
	    // procedure for solving the problem batch optimally
		optimalButton.addActionListener(new ActionListener() {			
			@Override 
			public void actionPerformed(ActionEvent e) {
				
				prepareProblemBatch(); // read all inputs
				
				// run the batch in its own thread
				Thread queryThread = new Thread() {
				      public void run() {
				    	  
				    	  for (String graph : solutionsSet.keySet()) {
				    	  for (String solutionStr : solutionsSet.get(graph)) {
				    	  for (double alpha : alphaSet) {				    		  
				    	  
				    		  // create problem
				    		  Solution solution = parseSolution(solutionStr);
				    		  menu.log("Solving assignment for "+graph+" and solution "+solutionStr+" with alpha="+alpha+"...");
				    		  ProblemAssignment problem = new ProblemAssignment(menu.graphs.get(graph), solution, alpha, assignmentTypeComboBox.getSelectedIndex());
				    		  
				    		  // solve problem and write output
				    		  solveAndWriteOutput(problem);
				    	  }
				    	  }
				    	  }
				      }
			    };
			    queryThread.start();
			}
		});
		JButton samplingButton = new JButton("Solve with sampling");
	    // procedure for solving the problem batch using the sampling method
		samplingButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				prepareProblemBatch(); // read all inputs
				
				// run the batch in its own thread
				Thread queryThread = new Thread() {
					public void run() {				    	  
						for (String graph : solutionsSet.keySet()) {
						for (String solutionStr : solutionsSet.get(graph)) {
				    	for (double alpha : alphaSet) {	
				    	for (int m : mSet) {
				    	for (int n : nSet) {
				    	for (int baseSeed : baseSeedSet) {
				    		
			    		    // create problem
				    		Solution solution = parseSolution(solutionStr);
				    		menu.log("Solving Assignment "+assignmentTypeComboBox.getSelectedIndex()+" for "+graph+" (m="+m+",n="+n+",base seed="+baseSeed+",alpha="+alpha+") with sampling method...");
				    		ProblemAssignment problem = new ProblemAssignment(menu.graphs.get(graph),solution,m,n,baseSeed,alpha,assignmentTypeComboBox.getSelectedIndex());
				    		
				    		// solve problem and write output
				    		solveAndWriteOutput(problem);
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
	    
	    
	    // create and arrange GUI elements
		// (1) panel for selecting the solution
		JPanel solutionPanel = new JPanel(new SpringLayout());
	    solutionPanel.add(new JLabel("Select graph"));
	    solutionPanel.add(graphComboBox);		    
	    solutionPanel.add(new JLabel("Solution"));
	    solutionPanel.add(solutionComboBox);
	    solutionPanel.add(new JLabel("Assignment type"));
	    solutionPanel.add(assignmentTypeComboBox);
	    SpringUtilities.makeCompactGrid(solutionPanel, 3, 2, 6, 6, 6, 6); // arrange elements in 3x2 grid
	    
	    // (2) panel for defining input parameters and settings
	    JPanel inputFieldsPanel = new JPanel(new SpringLayout());
	    inputFieldsPanel.add(new JLabel("m"));
	    inputFieldsPanel.add(mField);
	    inputFieldsPanel.add(new JLabel("alpha"));
	    inputFieldsPanel.add(alphaField);
	    inputFieldsPanel.add(new JLabel("n"));
	    inputFieldsPanel.add(nField);	    
	    inputFieldsPanel.add(new JLabel("base seed"));
	    inputFieldsPanel.add(baseSeedField);
	    inputFieldsPanel.add(new JLabel("<html>iterate<br>all graphs</html>"));
	    inputFieldsPanel.add(iterateAllGraphsCheckBox);
	    inputFieldsPanel.add(new JLabel("<html>iterate all<br>solutions<br>of a graph</html>"));
	    inputFieldsPanel.add(iterateAllSolutionsOfAGraphCheckBox);
	    SpringUtilities.makeCompactGrid(inputFieldsPanel, 3, 4, 6, 6, 6, 6); // arrange elements in 3x4 grid
	    
	    // (3) panel for solve buttons and export settings
		JPanel solveButtonsPanel = new JPanel(new SpringLayout());
	    solveButtonsPanel.add(exportSampleSolutionsCheckBox);
	    solveButtonsPanel.add(exportAssignmentPerNodeCheckBox);	    
	    solveButtonsPanel.add(exportAssignmentCompleteCheckBox);
	    solveButtonsPanel.add(exportSampleDemandsCheckBox);
	    solveButtonsPanel.add(optimalButton);
	    solveButtonsPanel.add(samplingButton);
	    SpringUtilities.makeCompactGrid(solveButtonsPanel, 6, 1, 6, 6, 6, 6);  // arrange elements in 5x1 grid
	    
		// (4) main panel comprising the individual panels
	    JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
	    mainPanel.add(solutionPanel);
	    mainPanel.add(new JPanel());
	    mainPanel.add(inputFieldsPanel);
	    mainPanel.add(new JPanel());
	    mainPanel.add(solveButtonsPanel);
	    mainPanel.add(new JPanel());	    
	    this.add(mainPanel);
	}
	
	/**
	 * Reads and processes all input, thus creating a problem batch. 
	 * Different values may be assumed for the same input parameter.
	 * The batch is defined as all possible combinations of input parameters.
	 */
	@SuppressWarnings("unchecked")
	void prepareProblemBatch() {
		
		mSet = menu.parseTextFieldsInteger(mField); // get numbers of samples for the batch
		nSet = menu.parseTextFieldsInteger(nField); // get sample sizes for the batch
		baseSeedSet = menu.parseTextFieldsInteger(baseSeedField); // get base seeds for the batch
		alphaSet = menu.parseTextFieldsDouble(alphaField); // get alphas for the batch
		solutionsSet.clear(); // clear old solution batch set

		if (!iterateAllGraphsCheckBox.isSelected()) { // if only the selected graph shall be part of the batch			
			String graph = (String) graphComboBox.getSelectedItem(); // get name of the selected graph
			
			// get solutions for this graph
			HashSet<String> singleGraphSolutionSet = new HashSet<String>();
			if (!iterateAllGraphsCheckBox.isSelected()) { // if only the selected solution shall be part of the batch
				if (solutionComboBox.getSelectedItem() == null) return; // abort if there is no solution selected
				singleGraphSolutionSet.add((String) solutionComboBox.getSelectedItem()); // get only the selected solution
			}
			else {
				singleGraphSolutionSet.addAll((HashSet<String>) solutions.get(graph).clone()); // get all solutions to the selected graph
			}
			solutionsSet.put(graph, singleGraphSolutionSet); // create solution set for the problem batch
		}
		else { // if all available graphs shall be part of the batch
			for (String graph : solutions.keySet()) { // for all graphs of available solutions
				solutionsSet.put(graph, (HashSet<String>) solutions.get(graph).clone()); // add graph and all its solutions to the batch
			}
		}
	}
	
	/**
	 * Solves a given assignment problem and controls the output of relevant data.
	 * @param problem The unsolved assignment problem.
	 */
	void solveAndWriteOutput(ProblemAssignment problem){
		// solve problem
		  problem.solve();
		  menu.log("Assignment problem solved.");
		  
		  // write output
		  menu.currentExportData.write(problem.exportSolution(), menu.currentExportPrefix+"_assignment"); // write assignment results (standard data output)  
		  menu.log("Solution written in "+menu.currentExportData.fileName+"\\"+menu.currentExportPrefix+"_assignment"+".");	// write log						
		  // write additionally desired output
		  if (exportSampleSolutionsCheckBox.isSelected()) { // if dedicated check box activated...
			  menu.currentExportData.write(problem.exportSolutionsOfSamples(), menu.currentExportPrefix+"_assignment_samples"); // write this one-sample-solution in the sheet for individual samples
		  }
		  if (exportAssignmentPerNodeCheckBox.isSelected()) { // if dedicated check box activated...
			  menu.currentExportData.write(problem.exportAssignmentSumPerNode(), menu.currentExportPrefix+"_assignment_per_node");
		  }	
		  if (exportAssignmentCompleteCheckBox.isSelected()) { // if dedicated check box activated...
			  menu.currentExportData.write(problem.exportAssignmentOfSamples(), menu.currentExportPrefix+"_assignment_detailed");
		  }	
		  if (exportSampleDemandsCheckBox.isSelected()) {  // if dedicated check box activated...
			  menu.currentExportData.write(problem.exportScenariosOfSamples(), menu.currentExportPrefix+"_scenarios"); // write all demand scenarios of the problem
		  }
		  
	}
	
	/**
	 * Parses String representation of the number of ambulances and their locations into the dedicated {@link Solution} format. 
	 * @param solutionStr String representation of an Integer array, "," as separator of values.
	 * @return 
	 */
	Solution parseSolution(String solutionStr) {
		solutionStr = solutionStr.replaceAll("\\[", ""); // remove brackets
		solutionStr = solutionStr.replaceAll("\\]", ""); // remove brackets
		solutionStr = solutionStr.replaceAll(" ", ""); // remove spaces
		String[] zStr = solutionStr.split(",");
		int[] z = new int[zStr.length];
		for (int i=0; i<z.length; i++) {
			z[i] = Integer.parseInt(zStr[i]);
		}
		return new Solution(new int[0], new int[0][0][0], z, 0, 0, 0); //only z relevant
	}
	
	/**
	 * Updates the contents of the combo box for the selection of graphs.
	 */
	void updateGraphComboBox () {
		graphComboBox.removeAllItems(); // clear contents
		for (String graph : solutions.keySet()) { // for all graphs of imported solutions
			graphComboBox.addItem(graph); // add graph to combo box
		}
	}
}
