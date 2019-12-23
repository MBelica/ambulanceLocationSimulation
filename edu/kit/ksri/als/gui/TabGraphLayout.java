package edu.kit.ksri.als.gui;

import edu.kit.ksri.als.graph.BasicGraph;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * GUI to start the Graph Layout program. 
 * Features a combo box to select the graph for the layout.
 *
 */
@SuppressWarnings("serial")
public class TabGraphLayout extends JPanel{
	
	JComboBox<String> graphComboBox = new JComboBox<String>();
	
	/**
	 * Creates GUI for starting up the Graph Layout.
	 * @param menu Link to the main window.
	 */
	public TabGraphLayout(MainFrame menu) {
		
		// create button and implement its functionality
		JButton createImageButton = new JButton("Create graph image");
	    createImageButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() { // start a new thread...
					@Override
					public void run() { // ... that starts the layout program
						BasicGraph graph = menu.graphs.get(graphComboBox.getSelectedItem()); // get graph defined in combo box
						new GraphLayout(graph); // start layout program
					}
				});
			}
		});
	    
	    // add GUI elements
	    this.add(graphComboBox);
	    this.add(createImageButton);
	}
}
