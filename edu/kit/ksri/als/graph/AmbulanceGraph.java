package edu.kit.ksri.als.graph;

import edu.kit.ksri.als.dataExchange.ImportData;
//import model.ModelALWUDBound;

import java.util.*;

/**
 * Graph with specific properties inherent to the Ambulance Location problem.
 * Emergency demands are covered by ambulances which are stationed at bases.
 * Each ambulance is clearly assigned to a specific base. 
 * The number of its ambulances constitutes the capacity of a base.
 * 
 * The ambulance graph is based on a regular graph defined in {@link BasicGraph}. 
 * Each node of the basic graph can serve as demand, base, both demand 
 * and base or be a basic node carrying none of the characteristics above.
 * The edge weights represent driving times.
 */
public class AmbulanceGraph extends BasicGraph {
	
	public ArrayList<Demand> demands = new ArrayList<Demand>(); // list of all demands
	public ArrayList<Base> bases = new ArrayList<Base>(); // list of all bases
	public double[][] drivingTimes; // driving time matrix for nodes
	double maxTime = 13.0; // Time limit for reaching an emergency. Determines which bases may serve which demands.
	
	// column numbers in the import Excel file where dedicated information is found (0-based)
	int isBaseColumn 				= 2; // column C in Excel
	int isDemandColumn				= 3; // column D in Excel	
	int baseCostsColumn 			= 4; // column E in Excel
	int ambulanceCostsColumn		= 5; // column F in Excel
	int demandProbabilitiesColumn 	= 6; // column G in Excel
	

	// bounds for the ambulance graph
	public int numberOfBasesLB = -1; //lower bound for the number of bases
	public HashMap<Double,Integer> numberOfAmbulancesUB = new HashMap<Double,Integer>(); //upper bound for number of ambulances (depending on alpha)	
	
	// auxiliary variables for calculating the upper bound of the number of ambulances
	int scenarioCounter = 0;
	double numberOfScenarios = 1.0;
	TreeMap<Integer,Double> numberOfAmbulancesWithProbabilities; // different numbers of ambulances and their associated probabilities
	
	
	/**
	 * Creates an ambulance graph using the information of a dedicated Excel sheet.
	 * @param graphName
	 */
	public AmbulanceGraph(String graphName, ImportData file) {
		super(graphName, file); // create a basic graph as basis

		for (Node node : nodes) {

			// iterate through rows of the Excel file until the right graph and node are found
			// (one row contains information about one node)
			while (file.rowIterator.hasNext()) {
				file.getNextRow();
				graphName = file.getCellInCurrentRowAsString(graphNameColumn); // read graph name
				String nodeName = file.getCellInCurrentRowAsString(nodeNameColumn); // read node name
				if (graphName.equals(name) && nodeName.equals(node.name)) break; // if right graph and node are found, stop iteration
			}

			// read node settings
		    boolean isBase   = (file.getCellInCurrentRowAsDouble(isBaseColumn)==1); // read if node is base
		    boolean isDemand = (file.getCellInCurrentRowAsDouble(isDemandColumn)==1); // read if node is demand
			
		    // if a node is a base, read and store base information 
		    if (isBase) {
		    	double costsPerBase = file.getCellInCurrentRowAsDouble(baseCostsColumn); // read costs of a base
		    	double costsPerAmbulance = file.getCellInCurrentRowAsDouble(ambulanceCostsColumn); // read costs per ambulance
			    bases.add(new Base(node,costsPerBase,costsPerAmbulance)); // create base and add to list of bases
		    }
		    
		    // if a node is a demand, read and store demand information
		    if (isDemand) {
		    	String demandProbabilitiesStr = file.getCellInCurrentRowAsString(demandProbabilitiesColumn); // read demand information String
		    	demandProbabilitiesStr = demandProbabilitiesStr.replaceAll(",","."); // replace "," (German number notation) with "." (English number notation)
			    String[] demandProbabilitiesStrArr = demandProbabilitiesStr.split(";"); // read probability distribution into String array
		    	//transform demand distribution array from String to double
		    	double[] demandProbabilities = new double[demandProbabilitiesStrArr.length];
		    	for (int i=0; i<demandProbabilities.length; i++) demandProbabilities[i] = new Double(demandProbabilitiesStrArr[i]);
		    	demands.add(new Demand(node,demandProbabilities)); //create demand and add to list of demands
		    }		    
		}		
		
		// initialize driving times
		drivingTimes = new double[nodes.size()][nodes.size()];
		for (int i=0; i<nodes.size(); i++) {
			for (int j=0; j<nodes.size(); j++) {
				drivingTimes[i][j] = 99999.9;
			}
		}
		// set driving time between adjacent nodes
		for (Edge edge : edges) {
			int fromIndex = edge.fromNode.index - 1;
			int toIndex = edge.toNode.index - 1;
			drivingTimes[fromIndex][toIndex] = edge.weight;
		}
		//calculate driving times between all nodes using the Tripel algorithm
		for (int j=0; j<nodes.size(); j++){
			for (int i=0; i<nodes.size(); i++){
				for (int k=0; k<nodes.size(); k++){
					if (drivingTimes[i][j] + drivingTimes[j][k] < drivingTimes[i][k]) {
						if (drivingTimes[i][k]>maxTime && drivingTimes[i][j] + drivingTimes[j][k] <= maxTime) System.out.println("New edge added between "+(i+1)+" and "+(k+1)+".");
						drivingTimes[i][k] = drivingTimes[i][j] + drivingTimes[j][k];
					}
				}
			}
		}
		
		// retrieve the demands that are covered by the bases
		for (Base base : bases) {
			for (int i=0; i<drivingTimes[base.node.index-1].length; i++) { //node id is 1-based
				if (drivingTimes[base.node.index-1][i]<=maxTime) { //if node i can be reached from the base in time...
					for (Demand demand : demands) {
						if (nodes.get(i) == demand.node) { // if node i is a demand node
							base.coveredDemands.add(demand);
						}
					}
				}
			}
		}
		
		// retrieve the bases that cover the demands
		for (Demand demand : demands) {
			for (int i=0; i<drivingTimes.length; i++) {
				if (drivingTimes[i][demand.node.index-1]<=maxTime ){ //if node i can reach the demand in time...
					for (Base base : bases) { 
						if (nodes.get(i) == base.node) { // if node i is a base
							demand.basesCovering.add(base);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Processes the bounds of the graph into a database-friendly export format. 
	 * The maximum number of ambulances depends on the service level required.
	 * @param alpha Target service level.
	 * @return Returns an ArrayList<Sring[]>. Each String[] represents a row in a database
	 * with each field depicting an entry per database column.
	 */
	public ArrayList<String[]> exportBounds(double alpha) {
		// if not yet calculated, calculate lower bound for number of bases
		if (numberOfBasesLB == -1) calculateNumberOfBasesLB();
		// if not yet calculated, calculate upper bound for number of ambulances for a given service level
		if (!numberOfAmbulancesUB.containsKey(alpha)) calculateNumberOfAmbulancesUB(alpha);
		
		// write data
		// Database fields: graph	alpha	min_bases	max_ambulances
		ArrayList<String[]> results = new ArrayList<String[]>();
		results.add(new String[] {name,""+alpha,""+numberOfBasesLB,""+numberOfAmbulancesUB.get(alpha)});
		
		return results;
	}
	
	
	/**
	 * Determines the lower bound for the number of bases according to the method described in the paper.
	 */
	public void calculateNumberOfBasesLB() {
		// transform the information of bases covering demands into indices to be able to solve them in a mathematical model
		ArrayList<HashSet<Integer>> basesCoveringDemandsIndices = new ArrayList<HashSet<Integer>>(); // J_i
		for (Demand demand : demands) { // for all demands
			HashSet<Integer> basesCoveringDemandIndices = new HashSet<Integer>();
			for (Base base : demand.basesCovering) { // for all bases that cover a demand
				basesCoveringDemandIndices.add(bases.indexOf(base)); //add ID of the base (not ID of the node)
			}
			basesCoveringDemandsIndices.add(basesCoveringDemandIndices);
		}
		
		// create and solve the model that determines the lower bound for the number of bases
		// todo
		//ModelALWUDBound model = new ModelALWUDBound(bases.size(), demands.size(), basesCoveringDemandsIndices);
		//model.generate();
		//numberOfBasesLB = model.solve();
		//model.end();
	}
	
	/**
	 * Determines the upper bound for the number of ambulances.
	 * There are to ways to determine the upper bound. 
	 * The first calculates it according to the method described in the paper. 
	 * This method requires lots of time and computational resources and is 
	 * therefore not able to finish in acceptable time for graphs of moderate sizes.
	 * The second version quickly calculates a very weak upper bound.
	 */
	public void calculateNumberOfAmbulancesUB(double alpha) {
		boolean strongBound = false; // decision variable for which way the bound is computed
		
		if (strongBound) {
			// Stronger upper bound. Takes lots of time and resources to compute.
			for (Demand d : demands) numberOfScenarios *= d.probabilities.length;			
			System.out.println(numberOfScenarios+" demand scenarios have to be iterated to determine the upper bound for the number of ambulances.");
			numberOfAmbulancesWithProbabilities = new TreeMap<Integer,Double>();
			generatePermutations(0, 1, 0); // generate all demand scenarios with probabilities
			
			double cumulatedProbability = 0.0;
			for (Integer numberOfAmbulances : numberOfAmbulancesWithProbabilities.keySet()) {
				cumulatedProbability += numberOfAmbulancesWithProbabilities.get(numberOfAmbulances);
				if (cumulatedProbability >= alpha) {
					numberOfAmbulancesUB.put(alpha, numberOfAmbulances); //set upper bound
					break;
				}
			}
		}
		else { 	
			// weak upper bound, independent of alpha, easy to compute
			int maxAmbulances = (demands.get(0).probabilities.length-1)*demands.size();
			numberOfAmbulancesUB.put(alpha, maxAmbulances); //set upper bound
		}
			
	}
	
	/**
	 * Recursive method generating all demand permutations in order to determine 
	 * the probability of having a specific number of ambulances.
	 * @param depth Current demand index. (depth-1) demands have already been iterated.
	 * @param currentProb Probability of the demand scenario so far.
	 * @param currentNOA  Number Of Ambulances of the scenario so far.
	 */
	public void generatePermutations(int depth, double currentProb, int currentNOA) {
		
	    if(depth == demands.size()) { // if all demand nodes have been iterated
	    	if (numberOfAmbulancesWithProbabilities.containsKey(currentNOA)) { // if there already is a probability for the current number of ambulances
	    		numberOfAmbulancesWithProbabilities.put(currentNOA, numberOfAmbulancesWithProbabilities.get(currentNOA)+currentProb); // sum up probabilities
	    	}
	    	else { // otherwise create new entry
	    		numberOfAmbulancesWithProbabilities.put(currentNOA, currentProb);
	    	}
	    	
	    	// print status
	    	System.out.println((Math.round((10000000.0*(++scenarioCounter/numberOfScenarios)))/100000.0) + "% of scenarios done.");
	    	
	    	return; // end recursion
	    }
	    for(int i = 0; i < demands.get(depth).probabilities.length; i++) { // iterate all possible demands of this demand node
	    	generatePermutations(depth + 1, currentProb*demands.get(depth).probabilities[i], currentNOA + i);
	    }
	}
}
