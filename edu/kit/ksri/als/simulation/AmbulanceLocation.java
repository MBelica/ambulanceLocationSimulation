package edu.kit.ksri.als.simulation;

import edu.kit.ksri.als.gui.MainFrame;


/**
 * The program AmbulanceLocation is an implementation of the paper "Ambulance Location under 
 * Stochastic Demand: A Sampling Approach" by Nickel, Reuter, Saldanha-da-Gama (2015).
 * It provides a multitude of features that can be operated using a graphical user interface.
 * 
 * - import graphs from dedicated Excel files. 
 * Those graphs may then be solved either optimally or heuristically using the sampling approach 
 * described in the paper using CPLEX.
 * Results and computational times are saved in Excel files.
 *
 */
public class AmbulanceLocation {
	
	/**
	 * Main method. 
	 * Starts the whole program or executes parts specific program parts such as the Graph Layout Editor.
	 * @param args
	 */
	public static void main(String[] args) {
		new MainFrame(); // full program
	}
}
