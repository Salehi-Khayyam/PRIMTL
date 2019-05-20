package leakage;

import java.util.Map;
import java.util.Map.Entry;

import prism.PrismException;
import prism.PrismLog;
import prism.ProbModel;

/**
 * A class for computing intermediate leakage using back bisimulation
 *
 * @author Khayyam Salehi & Ali A. Noroozi
 */

public class InterLeakComputerBackBisim {

	ProbModelBackBisimExplorer expModel;
	TopDownBackBisimulation backBisimModel;
	ProbModel probModel;
	
	// logs
	private PrismLog mainLog = null;
	
	public static boolean MIN_ENTROPY = false;
	public static boolean SHANNON_ENTROPY = true;
	private boolean entropyType = SHANNON_ENTROPY; 

	/**
	 * Constructor
	 * @param interleakbackbisimVerbose 
	 * @throws PrismException 
	 */
	public InterLeakComputerBackBisim(ProbModel probModel, boolean entropyType, String initDistFileName, PrismLog mainLog, boolean interleakbackbisimVerbose) throws PrismException {
		this.probModel = probModel;
		this.entropyType = entropyType;
		this.mainLog = mainLog;
		
    	this.mainLog.println("\nStarting to back bisimulatation process ...");

    	// do back bisimulation process
    	backBisimModel = new TopDownBackBisimulation(probModel, this.mainLog);
    	backBisimModel.doTopDownBackBisimulation(); 
    	
    	// explore traces obtained of back-bisimulation process and compute trace probabilities and final state secret distributions 
    	expModel = new ProbModelBackBisimExplorer(initDistFileName, backBisimModel, this.mainLog);
    	expModel.exploreModel();
    	
    	// Verbose the characteristics of the model and the quotients induced of Back-bisimulation, if arg -verbose is considered
    	if(interleakbackbisimVerbose)
    		verboseBackbisimulation();
    	// expModel.exportXDOT();   	
    }
	
	/**
	 * Verbose the characteristics of the model and the quotients induced of Back-bisimulation 
	 */
	private void verboseBackbisimulation() {
		mainLog.println();
        mainLog.println("#Quotient states:\t" + backBisimModel.quotientStates.size());
        int numTransition = 0;
        for (Map.Entry<Long, Map<Long, Double>> entry: backBisimModel.quotientTransitionForward.entrySet()) {
        	numTransition += entry.getValue().size();
        }
        mainLog.println("#Quotient transitions:\t" + numTransition);
        mainLog.println("#Level blocks:\t" + backBisimModel.blocks_list.size());
        mainLog.println("#Final quotient states:\t" + backBisimModel.finalQuotientStates.size());
	}

	/**
     * Compute initial uncertainty
     * 
     * @return 
     */
	public double initialUncertainty() {
		double initialUncertainty = 0;
	    initialUncertainty = entropy(expModel.getPriorKnowledge());
    	return initialUncertainty;
	}
	
	/**
     * 
     * @return Shannon or min-entropy of distribution
     */
	public double entropy(Map<String, Double> distribution) {
		if(entropyType == MIN_ENTROPY)
        	return minEntropy(distribution);
        else // SHANNON_ENTROPY
        	return shannonEntropy(distribution);
	}
	
	/**
     * 
     * @param distribution contains elements as String and their probabilities as Double
     * @return Shannon entropy of the distribution
     * 
     */
	public double shannonEntropy(Map<String, Double> distribution) {
        double shannon = 0;
        for(Map.Entry<String, Double> entry: distribution.entrySet()){
            double p = entry.getValue();
            if(p != 0) {
            	double log_p =  Logarithm.log2(p);
            	shannon += p * log_p;
            }
        }
        return -shannon;
	}

	/**
     * 
     * @param distribution contains elements as String and their probabilities as Double
     * @return min-entropy of the distribution
     * 
     */
	public double minEntropy(Map<String, Double> distribution) {
		double max_prob = -1.0;
        for(Map.Entry<String, Double> entry: distribution.entrySet()){
            double prob = entry.getValue();
            max_prob = Math.max(prob, max_prob);
        }
        double d =  - Logarithm.log2(max_prob);
        return d;
	}

	/**
     * Compute leakage of the dtmc model using back bisimulation. 
     * 
     * @return expected leakage
     */
	public double expectedLeakage() {
		double initial_uncertainty = initialUncertainty();
    	double remaining_uncertainty = remainingUncertainty();
        double leakage = initial_uncertainty - remaining_uncertainty;
        return leakage;
	}

	/**
     * Compute remaining uncertainty
     * @return remaining uncertainty 
     */
	private double remainingUncertainty() {
		long finalState; 
    	Map<String, Double> bbtrSecretDistribution;
    	double finalStateProb, bbtrFinalEntropy;
    	double remaining_uncertainty = 0;
        for(Entry<Long, Double> entry: expModel.traceProbBackBisim.entrySet()){
        	finalState = entry.getKey();
        	finalStateProb = entry.getValue();
            bbtrSecretDistribution = expModel.finalStatesSecretDistribution(finalState);
            bbtrFinalEntropy = entropy(bbtrSecretDistribution);
            remaining_uncertainty += bbtrFinalEntropy * finalStateProb;
        }
        return remaining_uncertainty;
	}
	
}
