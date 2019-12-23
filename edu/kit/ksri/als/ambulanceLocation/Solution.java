package edu.kit.ksri.als.ambulanceLocation;


/**
 * Data type that can store all relevant information of a solved {@link Sample}
 * for both location and assignment problem.
 * 
 * The naming of the variables in this class concurs with the paper.
 *
 */
public class Solution {

	public int[] x; //bases
	public int[][][] y; //allocation of ambulances to emergencies
	public int[] z; //ambulances
	public double serviceLevel;
	public double objectiveValue;
	public double time; //CPLEX computation time
	
	/**
	 * Creates a solution.
	 * @param x Bases: 1 if base is constructed at node j, 0 otherwise.
	 * @param y Assignment of demand i to base j in scenario w. In scenario w, y ambulances of base j cover y emergencies at demand i.
	 * @param z Number of ambulances stationed at base j.
	 * @param objectiveValue Objective value depending on the model applied to find the solution. Usually the costs of the solution.
	 * @param time Time required for CPLEX to compute the solution.
	 */
	public Solution(int[] x, int[][][] y, int[] z, double objectiveValue, double serviceLevel, double time) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.serviceLevel = serviceLevel;
		this.objectiveValue = objectiveValue;
		this.time = time;
	}
	
	
	/**
	 * Returns a String representation of the object.
	 */
	@Override  
	public String toString() {
		String result = "";
		for (int j=0; j<x.length; j++) {
			if (x[j] == 0) result += "Base " + j + " not installed.";
			else result += "Base "+j+" installed with "+z[j]+" ambulances.";
			if (j<x.length-1) result += "\n"; //add line break
		}
		return result;
	}
	
}
