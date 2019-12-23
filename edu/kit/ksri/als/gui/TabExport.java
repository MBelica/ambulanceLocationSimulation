package edu.kit.ksri.als.gui;

import edu.kit.ksri.als.dataExchange.ExportData;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;

/**
 * GUI providing the means to operate export files. 
 * More precisely, the user can create Excel files and corresponding sheet prefixes.
 * The resulting data of the program is written in Excel sheets whose names are made up of two parts respectively.
 * The first part is the user-defined prefix, the second part describes the type of information being stored.
 *
 */
@SuppressWarnings("serial")
public class TabExport extends JPanel{
	
	
	/**
	 * Creates GUI for operating the file export.
	 * @param menu Link to the main window.
	 */
	public TabExport(MainFrame menu) {  
		
		// labelPanel: panel which displays information
		JPanel labelPanel = new JPanel(new SpringLayout());
		JLabel exportFileLabel = new JLabel(menu.defaultExportFileName);
	    JLabel sheetPrefixLabel = new JLabel(menu.currentExportPrefix);
	    labelPanel.add(new JLabel("Export file: "));
	    labelPanel.add(exportFileLabel);
	    labelPanel.add(new JLabel("Sheet prefix: "));
	    labelPanel.add(sheetPrefixLabel);
	    SpringUtilities.makeCompactGrid(labelPanel, 2, 2, 6, 6, 6, 6); // arrange elements of the panel in a 2x2 grid
		
	    
	    // selectPanel: panel to select export file and sheet prefix
		JPanel selectPanel = new JPanel(new SpringLayout());
		JComboBox<String> fileComboBox = new JComboBox<String>(); // combo box for selecting the export file
		JComboBox<String> prefixComboBox = new JComboBox<String>(); // combo box for selecting the sheet prefix
	    // initialize combo boxes
		for (String ed : menu.exportDatas.keySet()) fileComboBox.addItem(ed); //add all export files (usually it's just the default export file)
		prefixComboBox.addItem(menu.currentExportPrefix); // add default prefix
		// procedure for updating sheets when selecting a file in the combo box
		fileComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// (1) get the file
				String selectedFile = (String) fileComboBox.getSelectedItem();
				// (2) update its prefixes
				prefixComboBox.removeAllItems(); // clear combo box
				// (2a) read existing prefixes
				HashSet<String> prefixes = new HashSet<String>(); 
				for (String sheet : menu.exportDatas.get(selectedFile).rowNums.keySet()) { // for all sheets of the file
					prefixes.add(sheet.split("_")[0]); // add sheet prefix (Everything before the first "_". HashSet avoids duplicates.)
				}
				// (2b) add all prefixes to the combo box
				for (String prefix : prefixes) prefixComboBox.addItem(prefix);
			}
		});
		
		
		// buttons of the selectPanel		
		// create buttons
	    JButton createFileButton = new JButton("Create new");
	    JButton createPrefixButton = new JButton("Create new");
	    JButton confirmButton = new JButton("Change output settings");
	    // implement buttons' functionalities
	    createFileButton.addActionListener(new ActionListener() {			
			@Override
			// create a new file
			public void actionPerformed(ActionEvent e) {
				
				// create message dialog to enter the file name in
				String fileName = (String)JOptionPane.showInputDialog(menu, "Enter file name", "Export",
                        JOptionPane.PLAIN_MESSAGE, null, null, "Results\\Results.xlsx");
    			
				// check file name
				if (menu.exportDatas.containsKey(fileName)) { // if file name already exists
					// show error message
    				JOptionPane.showMessageDialog(menu, "File name already exists.", "Export Error", JOptionPane.ERROR_MESSAGE);
    				return; // exit without adding new file
    			}
    			else if (!fileName.endsWith(".xlsx")) { // if file name has the wrong format
					// show error message
    				JOptionPane.showMessageDialog(menu, "File name needs to end in \".xlsx\".", "Export Error", JOptionPane.ERROR_MESSAGE);
    				return; // exit without adding new file
    			}
				// ... if file name passed the checks
				menu.exportDatas.put(fileName, new ExportData(fileName)); // create new file and add it to list of all export files
				fileComboBox.addItem(fileName);	// add reference to new file to the combo box
				fileComboBox.setSelectedItem(fileName); // select new file in the combo box
			}
		});
	    
	    createPrefixButton.addActionListener(new ActionListener() {			
	    	@Override
	    	// create a new prefix
			public void actionPerformed(ActionEvent e) {
				
				// create message dialog to enter the prefix name in
				String prefix = (String)JOptionPane.showInputDialog(menu, "Enter prefix name", "Export",
                        JOptionPane.PLAIN_MESSAGE, null, null, "results");
				
				// check prefix name
				if(!(((DefaultComboBoxModel<String>)prefixComboBox.getModel()).getIndexOf(prefix) == -1)) { //if prefix already exists
					// show error message
					JOptionPane.showMessageDialog(menu, "Prefix already exists.", "Export Error", JOptionPane.ERROR_MESSAGE);
    				return; // exit without adding new prefix
    			}
				else if (prefix.length() == 0) { // if prefix name is empty
    				JOptionPane.showMessageDialog(menu, "Illegal prefix name.", "Export Error", JOptionPane.ERROR_MESSAGE);
    				return; // exit without adding new prefix
    			}
				else if (prefix.contains("_")) { // if name contains an "_"
    				JOptionPane.showMessageDialog(menu, "Illegal prefix name. Must not contain '_'", "Export Error", JOptionPane.ERROR_MESSAGE);
    				return; // exit without adding new prefix
    			}
				// ... if prefix name passed the checks
				prefixComboBox.addItem(prefix); // add reference to new file to the combo box
				prefixComboBox.setSelectedItem(prefix); // select new prefix
			}
		});
	    
	    confirmButton.addActionListener(new ActionListener() {			
			@Override
			// confirm current selection of file and prefix 
			public void actionPerformed(ActionEvent e) {
				if (prefixComboBox.getSelectedItem() == null) { //check if prefix name has been entered
    				JOptionPane.showMessageDialog(menu, "Enter prefix name.", "Export Error", JOptionPane.ERROR_MESSAGE);
    				return; // exit without confirming
    			}
				// update file, prefix and labels
				menu.currentExportData = menu.exportDatas.get(fileComboBox.getSelectedItem()); // set export data
				menu.currentExportPrefix = (String) prefixComboBox.getSelectedItem(); // set file
				exportFileLabel.setText(menu.currentExportData.fileName);
				sheetPrefixLabel.setText(menu.currentExportPrefix);
			}
		});
	    
	    // create and arrange GUI elements of the selection panel    
	    selectPanel.add(new JLabel("Select file"));
	    selectPanel.add(fileComboBox);
	    selectPanel.add(createFileButton);
	    selectPanel.add(new JLabel("Select sheet prefix"));
	    selectPanel.add(prefixComboBox);
	    selectPanel.add(createPrefixButton);
	    selectPanel.add(new JPanel());
	    selectPanel.add(confirmButton);
	    selectPanel.add(new JPanel());
	    SpringUtilities.makeCompactGrid(selectPanel, 3, 3, 6, 6, 6, 6); // arrange elements of the panel in a 3x3 grid
		
	    // create and arrange GUI elements
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
	    mainPanel.add(selectPanel);
	    mainPanel.add(new JPanel()); //empty space
	    mainPanel.add(labelPanel);
	    this.add(mainPanel);	
	}
	
}
