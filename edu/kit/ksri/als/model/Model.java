package edu.kit.ksri.als.model;

//import ambulanceLocation.Solution;

/**
 * Describes the inherent procedure for any mathematical model used in this program.
 *
 */
public interface Model {

	/**
	 * Generates the model in CPLEX.
	 */
	public void generate();
	
	/**
	 * Solves the previously generated model.
	 * @return Returns the solution to the model.
	 */
	//public Solution solve();
	
	/**
	 * Closes the model.
	 */
	public void end();
}
