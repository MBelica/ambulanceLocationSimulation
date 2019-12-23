package edu.kit.ksri.als.gui;

import edu.kit.ksri.als.dataExchange.ImportData;
import edu.kit.ksri.als.graph.AmbulanceGraph;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * GUI providing the means to operate the import of graphs from dedicated Excel files. 
 * The Excel file must contain the information on graphs in its first (and usually only) sheet.
 *
 */
@SuppressWarnings("serial")
public class TabImportGraph extends JPanel{
	
	MainFrame menu;
	
	ImportData currentImportDataGraph; // currently opened file
	
	public JButton loadButton; // loads the file whose path is specified in the dedicated text field
	DefaultListModel<String> importedGraphsListModel; // list of all imported graphs
	
	/**
	 * Creates GUI for operating the graph import.
	 * @param menu Link to the main window.
	 */
	public TabImportGraph(MainFrame menu) {
		this.menu = menu;
		
		currentImportDataGraph = new ImportData(menu.defaultImportFilePathGraph); // set initial import file


		DefaultListModel<String> graphsInFileListModel = new DefaultListModel<String>();
		importedGraphsListModel = new DefaultListModel<String>();
		JList<String> graphsInFile = new JList<String>(graphsInFileListModel);
		JList<String> importedGraphs = new JList<String>(importedGraphsListModel);
		
		// implement import procedure: double click on a graph triggers its import
		graphsInFile.addMouseListener(new MouseAdapter() { // add mouse listener
			@SuppressWarnings("unchecked")
			public void mouseClicked(MouseEvent evt) { // observe mouse clicks
		        JList<String> list = (JList<String>)evt.getSource(); // get the graph list (source of clicks)
		        if (evt.getClickCount() == 2) { // if double-click
		        	int index = list.locationToIndex(evt.getPoint()); // get index of the item clicked on
		        	String graphName = graphsInFileListModel.elementAt(index); // get graph name to of this item
		        	if (!menu.graphs.containsKey(graphName)) { //if graph not yet imported
		        		importGraph(graphName);
		        	}
		        	else { // if graph name already exists
		        		// let user decide what to do...
		        		String[] options = {"Overwrite", "Import with new name", "Cancel"}; // define options for the user
		        		int answer = JOptionPane.showOptionDialog(menu, "Graph already exists. Would you like to...", "Import Warning",
		        			    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[2]); // create user dialog
		        		
		        		if (answer == 0) { // if user chose to overwrite the existing graph
		        			menu.graphs.put(graphName, new AmbulanceGraph(graphName, currentImportDataGraph)); // replace graph
		        			menu.log(graphName+" replaced with "+graphName+" from "+currentImportDataGraph.file.getName()+"."); // write log
		        		}
		        		else if (answer == 1) { // if user chose to rename the graph
		        			// get new name from user dialog
		        			String newGraphName = (String)JOptionPane.showInputDialog(menu, "Enter different graph name", "Customized Dialog",
		                            JOptionPane.PLAIN_MESSAGE, null, null, graphName+" (2)");
		        			
		        			if (importedGraphsListModel.contains(newGraphName)) { // make sure new name does not exist yet
		        				JOptionPane.showMessageDialog(menu, "Graph name already exists.", "Import Error", JOptionPane.ERROR_MESSAGE);
		        			}
		        			else if (newGraphName != null) { // if new name is not empty
		        				AmbulanceGraph graph = new AmbulanceGraph(graphName, currentImportDataGraph); // create graph from the information in the import file
		        				menu.graphs.put(newGraphName, graph); //add graph to central graph list
		        				importedGraphsListModel.addElement(newGraphName); // add graph to the display list of all imported graphs
				        		menu.log(graphName+" from "+currentImportDataGraph.file.getName()+" imported as "+newGraphName+"."); // write log
		        			}
		        		}
		        		// if graph is neither replaced nor renamed, do nothing
		        	}
		        }
			}
		});
		
		// GUI elements to operate the import
		// (1) load the file which is defined by the path in the text field
		JTextField filePathField = new JTextField(currentImportDataGraph.file.getAbsolutePath(), 40); // text field with file path
		loadButton = new JButton("Load file"); 
		loadButton.addActionListener(new ActionListener() {	// create button action listener	
			// import procedure when clicking the button
			@Override
			public void actionPerformed(ActionEvent e) {
				
				currentImportDataGraph = new ImportData(filePathField.getText()); // create new import file
				menu.tabImportSolution.filePathFieldGraph.setText(filePathField.getText()); // update graph file in tab ImportSolution
				graphsInFileListModel.clear(); // clear list of graphs

				// display all graphs defined in the import file
				TreeSet<String> graphs = new TreeSet<String>(); // ordered, non-duplicate set for the graph names
				Iterator<Row> rowIterator = currentImportDataGraph.workbook.getSheetAt(0).iterator(); //create row iterator for the first sheet of the import file
				rowIterator.next(); //skip first row (which is the title row)
				while(rowIterator.hasNext()) { // iterate through all rows (one row contains information about one node)
					Row row = rowIterator.next(); // get row
					//row.getCell(0).setCellType(Cell.CELL_TYPE_STRING); // convert first field of the row (which contains the graph name) to String
					graphs.add(row.getCell(0).getStringCellValue()); // add graph name of this row to set of graph names
				}
				
				for (String graph : graphs)	graphsInFileListModel.addElement(graph); // display names of all graphs in the file
				menu.log(filePathField.getText()+" loaded for graph import."); // write log
			}
		});
		// (2) select the file using a file chooser and load it
		JButton browseButton = new JButton("Browse files");
		browseButton.addActionListener(new ActionListener() { // create button action listener
			// import procedure when clicking the button
			@Override
			public void actionPerformed(ActionEvent e) {

				JFileChooser fileChooser = new JFileChooser(); // create file chooser...
				fileChooser.setFileFilter(new FileNameExtensionFilter("Excel files (*.xlsx)", "xlsx")); // that selects only .xlsx files
				int returnVal = fileChooser.showOpenDialog(menu); // open file chooser
				if (returnVal == JFileChooser.APPROVE_OPTION) { // if file chooser confirms a file...
					filePathField.setText(fileChooser.getSelectedFile().getAbsolutePath()); // ...write its file path in the file path text field
					loadButton.doClick(); // load file from file path
				}
			}
		});
		
		
		// create and arrange GUI elements
		// (1) panel displaying the file path
		JPanel pathPanel = new JPanel(new SpringLayout());
		pathPanel.add(new JLabel("Import file path"));
		pathPanel.add(filePathField);
		SpringUtilities.makeCompactGrid(pathPanel, 2, 1, 6, 6, 6, 6); // arrange elements of the panel in a 2x1 grid	
		
		// (2) panel for the buttons and the labels for the import table
		JPanel fileButtonPanel = new JPanel(new SpringLayout());
		fileButtonPanel.add(loadButton);
		fileButtonPanel.add(browseButton);
		fileButtonPanel.add(new JPanel()); // empty space
		fileButtonPanel.add(new JPanel()); // empty space
		fileButtonPanel.add(new JLabel("Graphs in loaded file"));
		fileButtonPanel.add(new JLabel("Already imported graphs"));
	    SpringUtilities.makeCompactGrid(fileButtonPanel, 3, 2, 6, 6, 6, 6);  // arrange elements of the panel in a 3x2 grid
	    
	    // (3a) panel for the table with importable and imported graphs
		JPanel listPanel = new JPanel(new GridLayout(1,2));
		listPanel.add(graphsInFile);
		listPanel.add(importedGraphs);
		// (3b) scroll pane embedding the graph table
		JScrollPane scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED); // add scroll bar that is displayed only when required
		scrollPane.setViewportView(listPanel);
		
		// (4) main panel comprising the individual panels
		JPanel mainPanel = new JPanel();		
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
	    mainPanel.add(pathPanel);
	    mainPanel.add(fileButtonPanel);
		mainPanel.add(new JPanel()); // empty space
		mainPanel.add(scrollPane);		
		this.add(mainPanel);
	}
	
	/**
	 * Creates a graph from the current import data file and updates the combo boxes for graph selection in all tabs.
	 * @param graphName Name of the graph to import.
	 */
	void importGraph(String graphName) {
		AmbulanceGraph graph = new AmbulanceGraph(graphName, currentImportDataGraph); // create graph from the information in the import file
		menu.graphs.put(graphName, graph); //add graph to central graph list
		importedGraphsListModel.addElement(graphName); // add graph to the display list of all imported graphs
		menu.log(graphName+" from "+currentImportDataGraph.file.getName()+" imported."); // write log
	
		// update combo boxes for graphs in other tabs
		menu.tabLocation.graphComboBox.removeAllItems(); // clear combo box in location tab
		menu.tabGraphLayout.graphComboBox.removeAllItems(); // clear combo box in graph layout tab
    	
		for (int i=0; i<importedGraphsListModel.size(); i++) { // for all imported graphs
			//fill combo boxes with all imported graphs
			menu.tabLocation.graphComboBox.addItem(importedGraphsListModel.elementAt(i)); // add to combo box in location tab
			menu.tabGraphLayout.graphComboBox.addItem(importedGraphsListModel.elementAt(i)); // add to combo box in graph layout tab	
    	}	
	}
}
