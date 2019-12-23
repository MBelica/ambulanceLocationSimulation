package edu.kit.ksri.als.ambulanceLocation;

import edu.kit.ksri.als.graph.AmbulanceGraph;
import edu.kit.ksri.als.graph.Base;
import edu.kit.ksri.als.graph.Demand;
//import edu.kit.ksri.als.model.ModelALWUD;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

/**
 * Data type that contains all information required to execute the {@link ModelALWUD} or the assignment models.
 * It comprises information on nodes, edges, costs, demands and their probabilities as well as the target service level. 
 * Also, every instance of this class saves the solution to its setup in itself.
 * 
 * The naming of the variables in this class concurs with the paper.
 */
public class Sample {
	public double[] f;				//costs per base
	public double[] g;				//costs per ambulance
	public ArrayList<HashSet<Integer>> demandsCoveredByBase = new ArrayList<HashSet<Integer>>();	//I_j
	public ArrayList<HashSet<Integer>> basesCoveringDemand  = new ArrayList<HashSet<Integer>>();	//J_i
	public int[][] d;				//demands per node and scenario
	public double[] pi; 			//probability per scenario
	Solution solution = null;
	public double[][] drivingTimes;
	
	//auxiliary variables for creating a complete, non-random sample
	int scenario;
	ArrayList<Demand> demands;	
	int numberOfScenarios;
	
	/**
	 * Creates a random sample of n demand scenarios.
	 * @param graph
	 * @param sampleSize n: Number of scenarios.
	 * @param seedValue Seed value for the random number generator. Same seed always gives the same sequence of random numbers.
	 * @param alpha Target service level.
	 */
	public Sample (AmbulanceGraph graph, int sampleSize, int seedValue) {
		initializeSample(graph); // initialize standard parameters of a sample
		
		d = new int[sampleSize][graph.demands.size()]; // demand scenarios
		pi = new double[sampleSize]; // demand scenario probabilities
		
		Random generator = new Random(seedValue); //random number stream for generating demands
		
		// generate demands for all scenarios of the sample
		for(int n=0; n<sampleSize; n++) { //for all scenarios of the sample
			
			for (int i=0; i<graph.demands.size(); i++) { // for all demand nodes
				double helpSum = 0.0; //cumulative probability of demands
				double random = generator.nextDouble(); //generate probability (random number between 0 and 1)
				
				for (int demand=0; demand<graph.demands.get(i).probabilities.length; demand++) { //iterate through all possible demand probabilities of the node
					helpSum += graph.demands.get(i).probabilities[demand]; // add probability of the current demand
					if (random<helpSum) { //find the demand that is associated to the cumulative probability
						d[n][i] = demand; // set demand for scenario n at node i
						break; //break loop since there is no need to iterate through higher demands
					}
				}				
			}
			pi[n] = 1.0/(double)sampleSize; // uniform distribution: every generated scenario has the same probability
		}
		
	}
	
	/**
	 * Creates a sample that includes all possible demand scenarios of a graph and their respective probabilities.
	 * The size of the sample grows exponentially for an increasing number of nodes and demand possibilities in a graph.
	 * Thus, lots of time and computational resources are required to generate the full sample.
	 * Even for moderate graph sizes, the program is not able to generate the sample in acceptable time, if at all.
	 * @param graph
	 * @param alpha Target service level.
	 */
	public Sample (AmbulanceGraph graph, double alpha) {
		initializeSample(graph); // initialize standard parameters of a sample	

		numberOfScenarios = 1;
		for (Demand demand : graph.demands) numberOfScenarios *= demand.probabilities.length; //calculate #scenarios
		d = new int[numberOfScenarios][graph.demands.size()]; // create array for all demand scenarios
		pi = new double[numberOfScenarios]; // create array for all demand scenario probabilities
		
		scenario = 0;
		demands = graph.demands;
		generatePermutations(0, 1, new int[graph.demands.size()]); // generate all demand permutations
	}
	
	
	/**
	 * Initialize parameters that are equal for both random and non-random sample.
	 * @param graph
	 */
	public void initializeSample(AmbulanceGraph graph) {
		f = new double[graph.bases.size()];
		g = new double[graph.bases.size()];
		this.drivingTimes = graph.drivingTimes;
		for (int j=0; j<graph.bases.size(); j++) {
			f[j] = graph.bases.get(j).costs;
			g[j] = graph.bases.get(j).costsPerAmbulance;
			
			HashSet<Integer> helpSet = new HashSet<Integer>(); //auxiliary variable to process demands covered by base j
			for (Demand demand : graph.bases.get(j).coveredDemands) {
				helpSet.add(graph.demands.indexOf(demand)); //add index of the demand (not index of the node)
			}
			demandsCoveredByBase.add(helpSet);
		}
		
		for (int i=0; i<graph.demands.size(); i++) { //auxiliary variable to process bases covering demand i
			HashSet<Integer> helpSet = new HashSet<Integer>();
			for (Base base : graph.demands.get(i).basesCovering) {
				helpSet.add(graph.bases.indexOf(base)); //add index of the base (not index of the node)
			}
			basesCoveringDemand.add(helpSet);
		}
	}
	

	/**
	 * Recursive method generating all demand permutations (scenarios) for a graph given its nodes and their demand probabilities.
	 * @param depth Current level of recursion, that is, the current node. The nodes are iterated from the first to the last.
	 * @param currentProb Probability of the scenario so far at the current depth.
	 * @param currentDemands Demands of the permutation (scenario) at hand up to the depth of the last iteration. The demands for later nodes are not yet specified.
	 */
	public void generatePermutations(int depth, double currentProb, int[] currentDemands) {
	    if(depth == demands.size()) { //if all demand nodes have been iterated
	    	d[scenario] = currentDemands; // save demand scenario
	    	pi[scenario++] = currentProb; // save probability of the demand scenario	
	    	
	    	// print status
	    	System.out.println((Math.round((10000000.0*(scenario/numberOfScenarios)))/100000.0) + "% of scenarios done.");
	    	
	    	return; // end recursion
	    }
	    
	    for(int i = 0; i < demands.get(depth).probabilities.length; i++) { // for all possible demands of the current node
	    	currentDemands[depth] = i; // save demand for current node
	    	generatePermutations(depth + 1, currentProb*demands.get(depth).probabilities[i], currentDemands.clone()); // generate permutations for next node
	    }	    
	}

}
