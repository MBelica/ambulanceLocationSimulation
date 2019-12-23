package edu.kit.ksri.als.gui;

import  edu.kit.ksri.als.dataExchange.ExportData;
import  edu.kit.ksri.als.dataExchange.ImportData;
import  edu.kit.ksri.als.graph.AmbulanceGraph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Central class for the Graphical User Interface (GUI) of the Ambulance Location program. Provides the main window that features
 * different tabs with dedicated functionalities.
 * Contains multiple variables used to centrally operate the individual tabs.
 *
 */

public class MainFrame extends JFrame {
	
	
	TreeMap<String, AmbulanceGraph> graphs = new TreeMap<String,AmbulanceGraph>(); // set that stores all imported graphs
	
	// export file variables
	TreeMap<String,ExportData> exportDatas = new TreeMap<String,ExportData>(); // set that stores all export files	
	ExportData currentExportData; // export file in which the results are written in
	String currentExportPrefix; // prefix for the Excel sheet name when exporting data (the sheet suffix describes the type of data)
	String defaultExportFileName = "Results\\Results.xlsx"; // default export file
	String defaultExportPrefix = "results"; // default export sheet prefix

	// import file variables
	String defaultImportFilePathGraph = "Source\\Graphs.xlsx"; // default import file path for graphs
	//String defaultImportFilePathSolution = "Source\\Graphs_db.xlsx"; // default import file path for solutions

	// tabs 
	TabLocation tabLocation;
	TabAssignment tabAssignment;
	TabImportGraph tabImportGraph;
	TabImportSolution tabImportSolution;
	TabGraphLayout tabGraphLayout;
	
	// log
	//Log log;
	
	/**
	 * Creates all elements of the GUI and loads the dedicated tabs.
	 */
	public MainFrame() {
		
		// set up export file
		ExportData defaultExportFile = new ExportData(defaultExportFileName); // create a new export file with default name at default path
		exportDatas.put(defaultExportFile.fileName, defaultExportFile); // set default 
		
		currentExportData = defaultExportFile;
		currentExportPrefix = defaultExportPrefix;
		
		// set up tabs
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP,JTabbedPane.SCROLL_TAB_LAYOUT); // create tab pane
		// create tabs
		//log = new Log();
		tabLocation = new TabLocation(this);
		tabAssignment = new TabAssignment(this);
		tabImportGraph = new TabImportGraph(this);
		tabImportSolution = new TabImportSolution(this);
		tabGraphLayout = new TabGraphLayout(this);
		// add tabs to pane
		tabbedPane.addTab("Location", tabLocation);
		tabbedPane.addTab("Assignment", tabAssignment);
		tabbedPane.addTab("Import Graph", tabImportGraph);
		tabbedPane.addTab("Import Solution", tabImportSolution);
		tabbedPane.addTab("Export", new TabExport(this));
		//tabbedPane.addTab("Log", tabLog);
		tabbedPane.addTab("Graph Layout", tabGraphLayout);
		
		tabbedPane.setSelectedIndex(2); // select Import Graph tab as default tab at start
		tabImportGraph.loadButton.doClick(); // open default import file in Import Graph tab
		
		// create and arrange GUI elements
		JPanel mainPanel = new JPanel(new SpringLayout()); // create main panel
		mainPanel.add(tabbedPane); // add tabs bar to main panel
		//mainPanel.add(log); // add log to main panel
		//log.setMinimumSize(new Dimension(0, 50)); // logging panel with a minimum height of 50px
		SpringUtilities.makeCompactGrid(mainPanel, 2, 1, 6, 6, 6, 6); // arrange elements in panel
		this.add(mainPanel); // add main panel to the main frame	    
	    this.setTitle("Ambulance Location & Assignment");
		//this.setSize(610, 500); // manually set size of the window
		this.pack(); // automatically adjust window size to show all elements of the JFrame

		// closing procedure for the closing button of the main frame
		this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // do not close program immediately but execute window listener first
		MainFrame menu = this; // auxiliary variable for window closing event
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {	        	
				ImportData.closeAll();	// close all opened import data files
				ExportData.exportAll();	// export all data into Excel files (and then close files)
				System.out.println("Program finished.");
				menu.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // exit program
			}
		});
		
		// workaround: wait 50ms before showing program window
		// gives elements time to load and prevents error
		Thread queryThread = new Thread() {
			public void run() {				    	  
				try {
					Thread.sleep(50); // wait 50ms
					menu.setVisible(true); // show program window
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		      }
	    };
	    queryThread.start();

	}
	
	/**
	 * Writes text in the log.
	 * @param str Text to write in log.
	 */
	void log(String str) {

		//log.log(str);
	}
	
	/**
	 * Reads input String and parses it into a set of Doubles.
	 * The input String may contain multiple segments (individual values or sets of values) separated by the separator ";".
	 * Sets of numbers with steady intervals between the values can be created using "-". 
	 * Such a value set is defined by its minimum and maximum value and the interval size.
	 * It is used in the format: "minimum value-maximum value-interval".
	 * Example: "1-5-1" results in the set {1,2,3,4,5}. 
	 * 
	 * If segments of the input String do not conform to the format, they are ignored.
	 * @param textField	Text field which contains the input String.
	 * @return Set of Double values in increasing order without duplicates.
	 */
	public TreeSet<Double> parseTextFieldsDouble(JTextField textField) {
		TreeSet<Double> result = new TreeSet<Double>(); // result set (inherently stores values in increasing order)
		
		// divide input string into segments
		String[] segmentsStr = textField.getText().split(";"); // read input String and split into segments using the first separator ";"
		
		for (String segment : segmentsStr) { // for all segments
			
			// look for a steady interval set in the segment
			String[] setDefinitionStr = segment.split("-"); //first value is min, second is max, third is interval size
			double[] setDefinition = new double[setDefinitionStr.length];
			for (int i=0; i<setDefinition.length; i++) setDefinition[i] = Double.parseDouble(setDefinitionStr[i]); // parse set parameters
			
			if (setDefinition.length==3) { // if segment is an interval set
				// iterate the interval from min to max
				for (double value=setDefinition[0]; value<=setDefinition[1]; value=Math.round(100000.0*(value+setDefinition[2]))/100000.0) {
					result.add(value); // add value
				}
			}
			else if (setDefinition.length==1) { // if segment is an individual value
				result.add(setDefinition[0]); // add value
			}
			// no else clause: if segment does not conform to the format, it is ignored
		}
		return result; 
	}
	
	/**
	 * Reads input String and parses it into a set of Integers.
	 * The input String may contain multiple segments (individual values or sets of values) separated by the separator ";".
	 * Sets of numbers with steady intervals between the values can be created using "-". 
	 * Such a value set is defined by its minimum and maximum value and the interval size.
	 * It is used in the format: "minimum value-maximum value-interval".
	 * Example: "1-5-1" results in the set {1,2,3,4,5}. 
	 * 
	 * If segments of the input String do not conform to the format, they are ignored.
	 * @param textField	Text field which contains the input String.
	 * @return Set of Integer values in increasing order without duplicates.
	 */
	public TreeSet<Integer> parseTextFieldsInteger(JTextField textField) {
		TreeSet<Integer> result = new TreeSet<Integer>();
		// divide input string into segments
				String[] segmentsStr = textField.getText().split(";"); // read input String and split into segments using the first separator ";"
				
				for (String segment : segmentsStr) { // for all segments
					
					// look for a steady interval set in the segment
					String[] setDefinitionStr = segment.split("-"); //first value is min, second is max, third is interval size
					int[] setDefinition = new int[setDefinitionStr.length];
					for (int i=0; i<setDefinition.length; i++) setDefinition[i] = Integer.parseInt(setDefinitionStr[i]); // parse set parameters
					
					if (setDefinition.length==3) { // if segment is an interval set
						// iterate the interval from min to max
						for (int value=setDefinition[0]; value<=setDefinition[1]; value=value+setDefinition[2]) {
							result.add(value); // add value
						}
					}
					else if (setDefinition.length==1) { // if segment is an individual value
						result.add(setDefinition[0]); // add value
					}
					// no else clause: if segment does not conform to the format, it is ignored
				}
				return result; 
			}
}
