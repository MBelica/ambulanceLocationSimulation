package edu.kit.ksri.als.ambulanceLocation;

import edu.kit.ksri.als.graph.AmbulanceGraph;
//import edu.kit.ksri.als.model.ModelALWUD;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

/**
 * Defines a location problem. Based on a graph with base nodes and demand nodes, 
 * bases are placed and sized optimally. 
 * The scope of the problem can either comprise the full set of possible demand scenarios of a graph
 * or a smaller set of randomly generated demand scenario samples. 
 * The full set guarantees a globally optimal solution, but might be too complex to compute.
 *
 */
public class ProblemLocation {

	AmbulanceGraph graph;
	double beta = 0.0;
	int numberOfSamples = 1;
	int sampleSize = -1;
	double alpha;
	int baseSeed = -1;
	ArrayList<Sample> samples = new ArrayList<Sample>();
	
	public Solution solution = null; //constructed solution based on the solutions of the individual samples
	
	// comparator for sorting Double[] by the value of their 2nd field (non-increasingly)
	Comparator <Double[]> comparatorDim2 = new Comparator<Double[]>() {
	    public int compare(Double[] array1, Double[] array2) {
	        if (array1[1] > array2[1]) return -1;    	// tells Arrays.sort() that array1 comes before array2
	        else if (array1[1] < array2[1]) return 1;	// tells Arrays.sort() that array1 comes after array2
	        else {return 0;}
	    }
	};
	
	/**
	 * Constructor for creating a location problem made up of random demand samples.
	 * This approach solves a randomly generated sub-problem for a given graph, service level (alpha) and beta.
	 * It generates sample problems of a given size, that are solved individually.
	 * Based on these individual solutions, a general solution is constructed in a later step.
	 * The randomly generated demands are determined by a base seed. The same base seed will always produce the same demands.
	 * 
	 * @param graph
	 * @param beta Factor balancing costs for driving with costs for construction. For details, read paper!
	 * @param numberOfSamples m
	 * @param sampleSize n : Number of demand scenarios within one sample.
	 * @param baseSeed Seed value for the random number generator. Same seed always gives the same sequence of random numbers.
	 * @param alpha Target service level.
	 */
	public ProblemLocation (AmbulanceGraph graph, double beta, int numberOfSamples, int sampleSize, int baseSeed, double alpha) {
		this.graph = graph;
		this.beta = beta;
		this.numberOfSamples = numberOfSamples;
		this.sampleSize = sampleSize;
		this.alpha = alpha;
		this.baseSeed = baseSeed;
		Random generator = new Random(baseSeed); //generates random seed values for the single samples
		// create samples
		for (int m=0; m<numberOfSamples; m++) {
			samples.add(new Sample(graph, sampleSize, generator.nextInt()));
		}
	}
	
	
	/**
	 * Constructor for creating the complete location problem with one full, non-random sample that contains all possible demand scenarios and their probabilities.
	 * This approach solves the assignment problem optimally for a given graph, service level and assignment method.
	 * Only applicable in simple graphs, otherwise computing times become too long.
	 * @param graph
	 */
	public ProblemLocation(AmbulanceGraph graph, double beta, double alpha) {
		this.graph = graph;
		this.beta = beta;
		this.alpha = alpha;
		samples.add(new Sample(graph, alpha));
		sampleSize = samples.get(0).scenario;
	}	
	
	/**
	 * Solves all samples of the problem.
	 * Depending on the alpha, beta and the demand scenarios of the sample, a CPLEX model is created and solved.
	 * The solution is stored in the solution parameter of the {@link Sample}.
	 */
	public void solve() {
		for (Sample sample : samples) {
			//ModelALWUD model = new ModelALWUD(sample, alpha, beta);
			//model.generate();
			//sample.solution = model.solve();
			//model.end();
		}
	}
	
	/**
	 * Processes the solution to the location problem into a database-friendly export format. 
	 * For this, the solution is constructed from the solutions of all indivual samples.
	 * @return Returns an ArrayList<Sring[]>. Each String[] represents a row in a database
	 * with each field depicting an entry per database column.
	 */
	public ArrayList<String[]> exportSolution() {
		constructSolution(); // construct a solution from the solutions of the individual samples
		
		ArrayList<String[]> results = new ArrayList<String[]>();
		//Database fields: graph	m	n	alpha	beta	base seed	x	z	optimal value	sample average	computation time
		results.add(new String[] {
				graph.name,""+numberOfSamples,""+sampleSize,""+alpha,""+beta,""+baseSeed, 
				Arrays.toString(solution.x),Arrays.toString(solution.z),""+getNumberOfBases(),
				""+getNumberOfAmbulances(), ""+solution.objectiveValue,""+calculateAverageCosts(),
				""+solution.time
				});
		return results;
	}
	
	/**
	 * Processes the solutions of every individual sample into a database-friendly export format. 
	 * This method was mainly implemented for bug fixing and produces large amounts of data.
	 * @return Returns an ArrayList<Sring[]>. Each String[] represents a row in a database
	 * with each field depicting an entry per database column.
	 */	
	public ArrayList<String[]> exportSolutionsOfSamples() {
		ArrayList<String[]> results = new ArrayList<String[]>();
		int sampleCounter = 1;
		for (Sample sample : samples) {
			results.add(new String[] {
				//Database fields: graph	m	n	alpha	beta	base seed	sample id	x	z	optimal value	time
				graph.name,""+numberOfSamples,""+sampleSize,""+alpha,""+beta,""+baseSeed,
				""+sampleCounter++,Arrays.toString(sample.solution.x),
				Arrays.toString(sample.solution.z),""+sample.solution.objectiveValue,
				""+sample.solution.time
				});
		}
		return results;
	}
	
	/**
	 * Processes the demand scenarios of every individual sample into a database-friendly export format. 
	 * This method was mainly implemented for bug fixing and produces large amounts of data.
	 * @return Returns an ArrayList<Sring[]>. Each String[] represents a row in a database
	 * with each field depicting an entry per database column.
	 */	
	//export demand information of scenarios of all samples
	public ArrayList<String[]> exportScenariosOfSamples() {
		ArrayList<String[]> results = new ArrayList<String[]>();
		int sampleCounter = 0;
		for (Sample sample : samples) {
			sampleCounter++;
			int scenarioCounter = 1;
			for (int[] demand : sample.d) { // for all scenarios of the sample
				
				 // calculate total sum of demand for this scenario
				int demandSum = 0;
				for (int singleDemand : demand) demandSum += singleDemand;
				
				// write results
				// Database fields: graph	m	n	base_seed	sample_id	scenario_id	demand	probability
				results.add(new String[] {
					graph.name,""+numberOfSamples,""+sampleSize,""+baseSeed,
					""+sampleCounter,""+scenarioCounter++,
					Arrays.toString(demand),""+demandSum,
					""+sample.pi[scenarioCounter-2]
				});
			}
		}
		return results;
	}
	
	/**
	 * Sums up number of bases for the current (constructed) solution.
	 * @return Number of bases.
	 */
	public int getNumberOfBases() {
		if (solution == null) return -1; //if no solution available, return -1
		int helpSum = 0;
		for (int j=0; j<solution.x.length; j++) {
			helpSum += solution.x[j]; // add base at node j (1 if there is one, 0 otherwise)
		}
		return helpSum;
	}
	
	/**
	 * Sums up number of ambulances for the current (constructed) solution.
	 * @return Number of ambulances.
	 */
	public int getNumberOfAmbulances() {
		if (solution == null) return -1; //if no solution available, return -1
		int helpSum = 0;
		for (int j=0; j<solution.z.length; j++) {
			helpSum += solution.z[j]; // add ambulances stationed at node j
		}
		return helpSum;
	}
	
	/**
	 * Calculate costs of the (constructed) solution of the location problem.
	 * @return Costs of the solution.
	 */
	public double calculateCosts() {
		if (solution == null) return -1.0; //if no solution available, return -1
		double helpSum = 0.0;
		for (int j=0; j<solution.x.length; j++) {
			helpSum += solution.x[j] * graph.bases.get(j).costs;			//add cost of base j
			helpSum += solution.z[j] * graph.bases.get(j).costsPerAmbulance;//add costs of ambulance in base j
		}
		return helpSum;
	}
	
	/**
	 * Calculate the average costs of an individual sample solution.
	 * @return Average costs of a sample solution.
	 */
	public double calculateAverageCosts() {
		if (samples.get(0).solution == null) return -1.0; //if no solution available, return -1
		double helpSum = 0.0;
		for (Sample scenario : samples) helpSum += scenario.solution.objectiveValue; // sum up the costs of all samples
		double result = Math.round((helpSum / samples.size())*1000.0)/1000.0; // divide by the number of samples (and round by 3 digits)
		
		return result;
	}
	
	/**
	 * Construct a solution from the solutions of the individual samples according to the method introduced in the paper.
	 * @return The constructed solution.
	 */
	public Solution constructSolution() {
		int[] bases = constructBases();
		int[] ambulances = constructAmbulances(bases);
		
		// sum up time for all samples
		double timeSum = 0.0;
		for (Sample sample : samples) timeSum +=sample.solution.time;
		timeSum = Math.round(timeSum*10000)/10000.0;		
		
		// save constructed solution for this set of samples. The assignment y is not considered.
		solution = new Solution(bases, null, ambulances, -1.0, -1.0, timeSum);
		solution.objectiveValue = calculateCosts();
		return solution;
	}
		
	/**
	 * Constructs the bases from the solutions of the individual samples.
	 * Approach and notation concur with the paper.
	 * @return The constructed solution for bases.
	 */
	public int[] constructBases() {
		int[] bases = new int[graph.bases.size()];
		
		if (graph.numberOfBasesLB == -1) { //check if lower bound has been calculated
			graph.calculateNumberOfBasesLB(); // calculate lower bound
		}
		
		// determine estimated number of bases
		double estNumberOfBases = 0.0; //^B
		Double[][] helpBases = new Double[graph.bases.size()][2]; //[][0] index, [][1] average x
		for (int j=0; j<graph.bases.size(); j++) { // for all bases
			helpBases[j][0] = new Double(j); // write base index
			helpBases[j][1] = new Double(0);
			
			for (Sample sample : samples) { //for all samples
				helpBases[j][1] += (double) sample.solution.x[j] / samples.size(); //calculate average x_j
			}
			estNumberOfBases += helpBases[j][1]; //^B
		}
		estNumberOfBases = Math.round(10000*estNumberOfBases)/10000.0;//round for the 5th decimal to avoid Java rounding error
		estNumberOfBases = Math.max(Math.ceil(estNumberOfBases), graph.numberOfBasesLB);
		
		// construct bases
		Arrays.sort(helpBases, comparatorDim2); //sort bases by average x_j (non-increasing)
		for (int j=0; j<graph.bases.size(); j++) {			
			if (j<estNumberOfBases || helpBases[j][1] == 1) {
				bases[helpBases[j][0].intValue()] = 1; // assign base
			}
			else {
				bases[helpBases[j][0].intValue()] = 0; // do not assign base
			}
		}
		
		return bases;
	}
	
	/**
	 * Constructs the number of ambulances per base based on the constructed bases and
	 * the number of ambulances of the individual samples.
	 * Approach and notation concur with the paper.
	 * @return The constructed ambulances per base.
	 */
	public int[] constructAmbulances(int[] bases) {
		int[] ambulances = new int[graph.bases.size()];
		
		if (!graph.numberOfAmbulancesUB.containsKey(alpha)) { //check if bounds have been calculated
			graph.calculateNumberOfAmbulancesUB(alpha); // calculate upper bound for the number of ambulances
		}
		
		// determine estimated number of ambulances
		double estTotalNumberOfAmbulances = 0.0; //
		double[] helpAmbulances = new double[graph.bases.size()];
		for (int j=0; j<graph.bases.size(); j++) { // for all bases
			for (Sample sample : samples) { // for all samples
				helpAmbulances[j] += (double) sample.solution.z[j] / samples.size();
			}
			estTotalNumberOfAmbulances += helpAmbulances[j];  //
		}
		
		// estimated number of ambulances must not be greater than the upper bound
		estTotalNumberOfAmbulances = Math.min(Math.ceil(estTotalNumberOfAmbulances), graph.numberOfAmbulancesUB.get(alpha));
		
		// construct ambulances according to paper
		double ambulancesDenominator = 0.0;
		for (int j=0; j<graph.bases.size(); j++) { // for all bases
			if (bases[j] == 1) ambulancesDenominator += helpAmbulances[j];
		}		
		for (int j=0; j<graph.bases.size(); j++) { // for all bases
			if (bases[j] == 1) ambulances[j] = (int) Math.round((helpAmbulances[j] / ambulancesDenominator)*estTotalNumberOfAmbulances);
		}
		
		return ambulances;
	}
	
	
}
