package edu.kit.ksri.als.gui;

import edu.kit.ksri.als.ambulanceLocation.Solution;
import edu.kit.ksri.als.dataExchange.ImportData;
import edu.kit.ksri.als.graph.AmbulanceGraph;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

/**
 * GUI providing the means to operate the import of solutions from the Excel file output of the location problem.
 * Also, solutions may be created manually. 
 * The Excel file must contain the information on the name of the graph and the number of ambulances at its bases.
 * When importing a solution, the corresponding graph is created from the current import file for graphs.
 * If the required graph is not in the graph import file, the solution will not be created.
 *
 */
@SuppressWarnings("serial")
public class TabImportSolution extends JPanel{
	
	MainFrame menu;

	ImportData currentSolutionImportData;
	JTextField filePathFieldGraph;
	
	public TabImportSolution(MainFrame menu) {
		this.menu = menu;	
		
		// create GUI elements
		// (1) elements for import files
		String graphFilePath = menu.tabImportGraph.currentImportDataGraph.file.getAbsolutePath();
		filePathFieldGraph = new JTextField(graphFilePath, 30);
		filePathFieldGraph.setEnabled(false);
		JTextField filePathFieldSolution = new JTextField("", 30);
		// (1b) buttons for import files
		JButton loadButton = new JButton("Load file");
		JButton browseButton = new JButton("Browse files");
		// (1c) combo box to select the import sheet
		JComboBox<String> sheetComboBox = new JComboBox<String>();

		loadButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				currentSolutionImportData = new ImportData(filePathFieldSolution.getText());
				sheetComboBox.removeAllItems();
				for (int i = 0; i < currentSolutionImportData.workbook.getNumberOfSheets(); i++) {
					sheetComboBox.addItem(currentSolutionImportData.workbook.getSheetName(i));
				}
				menu.log(filePathFieldSolution.getText()+" loaded for solution import.");
			}
		});	
		browseButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser();
				fileChooser.setFileFilter(new FileNameExtensionFilter("Excel files (*.xlsx)", "xlsx"));
				int returnVal = fileChooser.showOpenDialog(menu);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					filePathFieldSolution.setText(fileChooser.getSelectedFile().getAbsolutePath());
					loadButton.doClick();
				}
			}
		});
		
		JTextField columnGraphTextField = new JTextField("1", 2);
		JTextField columnSolutionTextField = new JTextField("8", 2);

		JButton importSolutionsButton = new JButton("Import solutions");
		importSolutionsButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				// import solutions...

				// read from Excel
				XSSFSheet sheet = currentSolutionImportData.workbook.getSheet((String) sheetComboBox.getSelectedItem()); //get selected sheet of the Excel file
				Iterator<Row> rowIterator = sheet.iterator(); //create sheet iterator
				rowIterator.next(); //skip first row (which is the title row)
				
				// parse column numbers: text field is 1-based, int values are 0-based
				int columnGraph = Integer.parseInt(columnGraphTextField.getText()) - 1;
				int columnSolution = Integer.parseInt(columnSolutionTextField.getText()) - 1;
				
				int successCounter = 0;
				int failCounterGraph = 0;
				int failCounterSolution = 0;
				
				while(rowIterator.hasNext()) { //iterate through all rows of the Excel file (one row contains information about one node)		

					Row row = rowIterator.next();			
					String graphName = row.getCell(columnGraph).getStringCellValue(); // read graph name
					System.out.println(graphName);
					AmbulanceGraph graph = menu.graphs.get(graphName);
					if (graph == null) { // if graph not yet created
						// create graph
						menu.tabImportGraph.importGraph(graphName);
						graph = menu.graphs.get(graphName);
						if (graph == null) { // if graph could not be created
							failCounterGraph++;
							continue; // read next line
						}						
					}
					
					String solutionStr = row.getCell(columnSolution).getStringCellValue();
					Solution solution = menu.tabAssignment.parseSolution(solutionStr);
					System.out.println(solutionStr);

					// check solution format (number of bases must be the same)
					if (solution.z.length == graph.bases.size()) {
						// save solution
						if (menu.tabAssignment.solutions.containsKey(graphName)) {
							menu.tabAssignment.solutions.get(graphName).add(solutionStr);
						}
						else {
							menu.tabAssignment.solutions.put(graphName, new HashSet<String>(Arrays.asList(solutionStr)));
						}
						successCounter++;
					}
					else { // if solution format wrong
						failCounterSolution++;
						continue;						
					}					
				}
				menu.tabAssignment.updateGraphComboBox();
				String message = successCounter+" solutions imported successfully.";
				message += "\n"+failCounterGraph+" solutions failed because of graph.";
				message += "\n"+failCounterSolution+" solutions failed because of solution format.";
				JOptionPane.showMessageDialog(menu, message, "Import Solutions", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		

		// create and arrange GUI elements
		JPanel pathPanel = new JPanel(new SpringLayout());		
		pathPanel.add(new JLabel("Import file path for graphs"));
		pathPanel.add(filePathFieldGraph);
		pathPanel.add(new JLabel("Import file path for solutions"));
		pathPanel.add(filePathFieldSolution);
		SpringUtilities.makeCompactGrid(pathPanel, 4, 1, 6, 6, 6, 6); // arrange elements in 4x1 grid
		
		JPanel fileButtonPanel = new JPanel(new SpringLayout());
		fileButtonPanel.add(loadButton);
		fileButtonPanel.add(browseButton);
	    SpringUtilities.makeCompactGrid(fileButtonPanel, 1, 2, 6, 6, 6, 6); // arrange elements in 1x2 grid
	    
	    JPanel columnPanel = new JPanel(new SpringLayout());
		columnPanel.add(new JLabel("Column with graph name"));
		columnPanel.add(columnGraphTextField);
		columnPanel.add(new JLabel("Column with solution"));
		columnPanel.add(columnSolutionTextField);
		SpringUtilities.makeCompactGrid(columnPanel, 1, 4, 6, 6, 6, 6); // arrange elements in 1x4 grid
	    
		JPanel sheetPanel = new JPanel(new SpringLayout());
	    sheetPanel.add(new JLabel("Select sheet to import solutions from"));
	    sheetPanel.add(sheetComboBox);
	    sheetPanel.add(columnPanel);
		SpringUtilities.makeCompactGrid(sheetPanel, 3, 1, 6, 6, 6, 6); // arrange elements in 3x1 grid
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(importSolutionsButton);
		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.add(pathPanel);
		mainPanel.add(fileButtonPanel);
		mainPanel.add(sheetPanel);
		mainPanel.add(buttonPanel);
		this.add(mainPanel);
			
	}
}
