package edu.kit.ksri.als.ambulanceLocation;

import edu.kit.ksri.als.graph.AmbulanceGraph;
import edu.kit.ksri.als.model.Model;
//import edu.kit.ksri.als.model.ModelAAP;
//import edu.kit.ksri.als.model.ModelAAPMaxSL;
//import edu.kit.ksri.als.model.ModelAAPWholeScenario;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;


/**
 * Defines an assignment problem. Based on a set of base locations and generated emergency demands, 
 * the ambulances are optimally assigned to the emergencies. 
 * For the assignment, there are three different methods available.
 * Also, the scope of the problem can either comprise the full set of possible demand scenarios of a graph
 * or a smaller set of randomly generated demand scenario samples. 
 * The full set guarantees a globally optimal solution, but might be too complex to compute.
 *
 */
public class ProblemAssignment {

	AmbulanceGraph graph;
	Solution solution; // container for the solution of the assignment problem
	int numberOfSamples = 1;
	int sampleSize = -1;
	double alpha; // target service level
	int baseSeed = -1;
	ArrayList<Sample> samples = new ArrayList<Sample>(); // set of samples
	int assignmentMethod = 2; // 0=whole scenarios, 1=part scenarios, 2=max service level
	String[] assignmentMethodText = new String[]{"whole scenarios","part scenarios","max service level"};
	
	/**
	 * Constructor for creating an assignment problem made up of random demand samples.
	 * This approach solves a randomly generated sub-problem for a given graph, service level and assignment method.
	 * It generates sample problems of a given size, that are solved individually.
	 * The randomly generated demands are determined by a base seed. The same base seed will always produce the same demands.
	 * @param graph
	 * @param solution Set of base locations.
	 * @param numberOfSamples m
	 * @param sampleSize n: Number of demand scenarios within one sample.
	 * @param baseSeed Seed value for the random number stream that generates the seeds for the random number streams of the individual scenarios.
	 * @param alpha Target service level.
	 * @param assignmentMethod
	 */
	public ProblemAssignment (AmbulanceGraph graph, Solution solution, int numberOfSamples, int sampleSize, int baseSeed, double alpha, int assignmentMethod) {
		this.graph = graph;
		this.solution = solution;
		this.numberOfSamples = numberOfSamples;
		this.sampleSize = sampleSize;
		this.alpha = alpha;
		this.baseSeed = baseSeed;
		Random generator = new Random(baseSeed); // random number stream for the generation of seed values for the single samples
		// create samples
		for (int m=0; m<numberOfSamples; m++) {
			samples.add(new Sample(graph, sampleSize, generator.nextInt()));
		}
		this.assignmentMethod = assignmentMethod;
	}
	
	/**
	 * Constructor for creating the complete assignment problem with one full, non-random sample that contains all possible demand scenarios and their probabilities.
	 * This approach solves the assignment problem optimally for a given graph, service level and assignment method.
	 * @param graph
	 * @param solution Set of base locations.
	 * @param alpha Target service level.
	 * @param assignmentMethod
	 */
	public ProblemAssignment(AmbulanceGraph graph, Solution solution, double alpha, int assignmentMethod) {
		this.graph = graph;
		this.solution = solution; // take solution
		//solution.y = null; //clear previous assignment (which may come from a different context)
		this.alpha = alpha;
		this.assignmentMethod = assignmentMethod;
	}
	
	/**
	 * Solves the assignment for all samples of the problem.
	 * Depending on the respective assignment method, a CPLEX model is created and solved.
	 * The solution is stored in the solution parameter of the {@link Sample}.
	 */
	public void solve() {
		for (Sample sample : samples) {
			Model model;
			switch (assignmentMethod) {
				//case  0: model = new ModelAAPWholeScenario(sample, solution, alpha); break;
				//case  1: model = new ModelAAP(sample, solution, alpha); break;
				//default: model = new ModelAAPMaxSL(sample, solution); //case 2
			}
			//model.generate();
			//sample.solution = model.solve();
			//model.end();
		}		
	}
	
	
	/**
	 * Processes the solution of the assignment problem into a database-friendly export format. 
	 * The solutions of the individual samples are averaged.
	 * @return Returns an ArrayList<String[]>. Each String[] represents a row in a database
	 * with each field depicting an entry per database column.
	 */
	public ArrayList<String[]> exportSolution() {
		ArrayList<String[]> results = new ArrayList<String[]>();
		calculateAverageSolution(); // get the averaged results for all samples
		
		// write results:
		// Database fields: graph	solution	type	m	n	alpha	base_seed	optimal_value	service_level	time		
		String[] result = new String[] {
			graph.name,""+Arrays.toString(solution.z),assignmentMethodText[assignmentMethod],""+numberOfSamples,""+sampleSize,
			""+alpha,""+baseSeed,""+solution.objectiveValue,""+solution.serviceLevel,""+solution.time
		};
		// adjust output for the target service level for the assignment method #2
		if (assignmentMethod == 2) { //type 2 is "max service level" and does not require a target service level
			result[5] = "-"; //change the entry for the service level to "-"
		}
		results.add(result);
		return results;
	}
	
	/**
	 * Processes the results of every individual sample into a database-friendly export format. 
	 * @return Returns an ArrayList<Sring[]>. Each String[] represents a row in a database
	 * with each field depicting an entry per database column.
	 */
	public ArrayList<String[]> exportSolutionsOfSamples() {
		ArrayList<String[]> results = new ArrayList<String[]>();
		int sampleNo = 1;
		//Database fields: graph	solution	alpha	base_seed	sample_id	optimal_value	service_level	time	
		for (Sample sample : samples) { // for all samples
			// determine optimal value of the sample (if any)
			String optimalValueStr = "infeasible"; // by default infeasible
			double optimalValue = sample.solution.objectiveValue;
			// if an optimal value exists, replace the default value (Negative Infinity means there is no optimal value)
			if (optimalValue > Double.NEGATIVE_INFINITY) optimalValueStr = ""+Math.round(1000*optimalValue)/1000.0;			
			
			// write sample result
			String[] result = new String[] {
				graph.name,Arrays.toString(solution.z),assignmentMethodText[assignmentMethod],
				""+numberOfSamples,""+sampleSize,
				""+alpha,
				""+baseSeed,""+sampleNo++,
				""+optimalValueStr,
				""+Math.round(1000*sample.solution.serviceLevel)/1000.0,
				""+sample.solution.time
			};
			// adjust output for the target service level for the assignment method #2
			if (assignmentMethod == 2) { //type 2 is "max service level" and does not require a target service level
				result[5] = "-"; //change the entry for the service level to "-"
			}
			results.add(result);			
		}
		return results;
	}
	
	/**
	 * Averages the results for all samples.
	 * @return Returns number of feasible samples.
	 */
	public int calculateAverageSolution() {
		double optimalValue = 0.0;
		double serviceLevel = 0.0;
		double time = 0.0;
		int numberOfFeasibleSamples = samples.size();
		// sum up all results
		for (Sample sample : samples) {
			time += sample.solution.time;
			if (sample.solution.objectiveValue == Double.NEGATIVE_INFINITY) {//if infeasible
				numberOfFeasibleSamples--;
				continue;
			}
			optimalValue += sample.solution.objectiveValue;
			serviceLevel += sample.solution.serviceLevel;			
		}
		
		// calculate the averages
		solution.objectiveValue = optimalValue/(double)(numberOfFeasibleSamples); // divide by feasible samples only
		//solution.serviceLevel = serviceLevel/(double)(numberOfFeasibleSamples);
		solution.serviceLevel = serviceLevel/(double)(samples.size()); // divide by all samples
		solution.time = time; // keep time as the total sum, not the average
		
		// compensate Java rounding errors for the averaged solution
		solution.objectiveValue = Math.round(1000*solution.objectiveValue)/1000.0;
		solution.serviceLevel = Math.round(1000*solution.serviceLevel)/1000.0;
		solution.time = Math.round(1000*solution.time)/1000.0;
		
		return numberOfFeasibleSamples;
	}
	
	
	/**
	 * Processes the assignment of every individual sample into a database-friendly export format. 
	 * This method was mainly implemented for bug fixing and produces large amounts of data.
	 * @return Returns an ArrayList<Sring[]>. Each String[] represents a row in a database
	 * with each field depicting an entry per database column.
	 */	
	public ArrayList<String[]> exportAssignmentOfSamples() {
		ArrayList<String[]> results = new ArrayList<String[]>();
		int counter = 0;
		for (Sample sample : samples) { // for all samples
			counter++;
			if (sample.solution.y == null) continue; //skip infeasible samples
			
			for (int i=0; i<sample.solution.y.length; i++) { // for all demands i (0-based)
				for (int j=0; j<sample.solution.y[i].length; j++) { // for all bases j (0-based)
					for (int w=0; w<sample.solution.y[i][j].length; w++) { // for all scenarios w (0-based)
						if (sample.solution.y[i][j][w]>0) { // only write assignments > 0
							results.add(new String[] {
								graph.name,Arrays.toString(solution.z),assignmentMethodText[assignmentMethod],
								""+numberOfSamples,""+sampleSize,
								""+alpha,""+baseSeed,""+counter++,
								""+(i+1),""+(j+1),""+(w+1), // write assignment of demand i to base j in scenario w (write them 1-based)
								""+sample.solution.y[i][j][w]
							}); // skip assignments of y=0 to reduce the amount of data
						}
					}
				}
			}
		}
		return results;
	}

	
	/**
	 * Processes the assignment of every individual sample into a database-friendly export format. 
	 * @return Returns an ArrayList<Sring[]>. Each String[] represents a row in a database
	 * with each field depicting an entry per database column.
	 */	
	public ArrayList<String[]> exportAssignmentSumPerNode() {
		ArrayList<String[]> results = new ArrayList<String[]>();
		for (int j=0; j<graph.bases.size(); j++) { // for all demands i (0-based)
			int sumOfAssignment = 0; // total number of ambulances assigned to demand i

			// calculate sum of assignment for node i
			for (Sample sample : samples) { // for all samples
				if (sample.solution.y == null) continue; //skip infeasible samples			
				
				for (int i=0; i<graph.demands.size(); i++) { // for all demands i (0-based)
					for (int w=0; w<sample.solution.y[i][j].length; w++) { // for all scenarios w (0-based)
						sumOfAssignment += sample.solution.y[i][j][w]; // add assignment from this sample from base j in scenario w to the total sum
					}
				}
			}
			
			results.add(new String[] {
					graph.name,Arrays.toString(solution.z),assignmentMethodText[assignmentMethod],
					""+numberOfSamples,""+sampleSize,
					""+alpha,""+baseSeed,
					""+graph.nodes.get(j).name, // write name of i-th node
					""+sumOfAssignment // write the total number of ambulances
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
	public ArrayList<String[]> exportScenariosOfSamples() {
		ArrayList<String[]> results = new ArrayList<String[]>();
		int sampleCounter = 0;
		for (Sample sample : samples) { // for all samples
			sampleCounter++;
			int scenarioCounter = 1;
			for (int[] demand : sample.d) { // for all scenarios of the sample
				
				 // calculate sum of demand for the scenario
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
}
