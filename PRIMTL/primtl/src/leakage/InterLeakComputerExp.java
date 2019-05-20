package leakage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import prism.PrismException;
import prism.PrismLog;
import prism.ProbModel;

/**
 * A class for computing intermediate leakage
 *
 * @author Ali A. Noroozi
 */

public class InterLeakComputerExp {
	
	ProbModelExplicitExplorer expModel;
	ProbModel probModel;
	
	// logs
	private PrismLog mainLog = null;
	
	public static boolean MIN_ENTROPY = false;
	public static boolean SHANNON_ENTROPY = true;
	private boolean entropyType = SHANNON_ENTROPY; 
		
	Map<List<String>, Double> traceProbs; // contains probabilities of traces
	
    public InterLeakComputerExp(ProbModel probModel, boolean bounded, int boundedStep, 
    		boolean entropyType, String initDistFileName, PrismLog mainLog) throws PrismException {
    	
    	this.entropyType = entropyType;
    	this.mainLog = mainLog;
    	
    	if(!bounded)
    		mainLog.println("\nStarting to explore traces ...\n");
    	// explore traces and compute trace probabilities and final state secret distributions
    	expModel = new ProbModelExplicitExplorer(probModel, initDistFileName);
    	expModel.exploreModel(bounded, boundedStep);
    	
    	traceProbs = expModel.getTraceProbs();
    	if(!bounded)
    		mainLog.println(traceProbs.size() + " traces found");
    }
    
    /**
     * Compute leakage of the dtmc model using trace-exploration-based method. 
     * 
     * @return expected leakage
     */
    public double expectedLeakage() throws PrismException {

    	double initial_uncertainty = initialUncertainty();
    	double remaining_uncertainty = remainingUncertainty();
        double leakage = initial_uncertainty - remaining_uncertainty;
        return leakage;
    }
    
    /**
     * Compute remaining uncertainty
     * 
     * @return remaining uncertainty 
     */
    public double remainingUncertainty() throws PrismException {
    	
    	List<String> tr;
    	Map<String, Double> trSecretDistribution;
    	double trProb, trFinalEntropy;
    	
        double remaining_uncertainty = 0;
        for(Map.Entry<List<String>, Double> entry: traceProbs.entrySet()){
        	
            tr = entry.getKey();
            trProb = entry.getValue();
            trSecretDistribution = expModel.finalStatesSecretDistribution(tr);
            
            trFinalEntropy = 0;
            trFinalEntropy = entropy(trSecretDistribution);
            
            remaining_uncertainty += trFinalEntropy * trProb;
        }
        
        return remaining_uncertainty;
    }
    
    /**
     * Compute initial uncertainty
     * 
     * @return 
     */
    public double initialUncertainty(){
    	
    	double initialUncertainty = 0;
	    initialUncertainty = entropy(expModel.getPriorKnowledge());
    	return initialUncertainty;
    }
    
    /**
     * Compute maximum leakage using trace-exploration-based method. 
     * 
     * @return maximum leakage
     */
    public double maxLeakage(){
        
        // compute minimum entropy of groups of final states of traces
        double minimumEntropy = minimumEntropy();
        double initialUncertainty = initialUncertainty();
        double maxLeakage = initialUncertainty - minimumEntropy;
        return maxLeakage;
    }
    
    /**
     * Compute minimum leakage. 
     * 
     * @return minimum leakage
     */
    public double minLeakage(){
        
        // compute maximum entropy of groups of final states of all traces
        double maximumEntropy = maximumEntropy();
        double initialUncertainty = initialUncertainty();
        double leakage = initialUncertainty - maximumEntropy;
        return leakage;
    }
    
    /**
     * Compute probability of maximum leakage of state_machine using trace-exploration-based method. 
     * 
     * @return probability of maximum leakage
     */
    public double probMaxLeakage(){
        
        // compute minimum entropy of groups of final states of traces
        double minimumEntropy = minimumEntropy();
        
        // compute probability of maximum leakage
        List<String> tr;
    	Map<String, Double> trSecretDistribution;
    	double trFinalEntropy;
    	
    	double probMaxLeakage = 0;
        for(Map.Entry<List<String>, Double> entry: traceProbs.entrySet()){
        	
            tr = entry.getKey();
            trSecretDistribution = expModel.finalStatesSecretDistribution(tr);
            
        	trFinalEntropy = entropy(trSecretDistribution);
        	if(minimumEntropy == trFinalEntropy)
        		probMaxLeakage += traceProbs.get(tr);
        }

        return probMaxLeakage;
    }
    
    /**
     * Compute probability of minimum leakage of state_machine using trace-exploration-based method. 
     * 
     * @return probability of minimum leakage
     */
    public double probMinLeakage(){
        
        // compute maximum entropy of groups of final states of traces
        double maximumEntropy = maximumEntropy();
        
        // compute probability of minimum leakage
        List<String> tr;
    	Map<String, Double> trSecretDistribution;
    	double trFinalEntropy;
    	
    	double probMinLeakage = 0;
        for(Map.Entry<List<String>, Double> entry: traceProbs.entrySet()){
        	
            tr = entry.getKey();
            trSecretDistribution = expModel.finalStatesSecretDistribution(tr);
            
        	trFinalEntropy = entropy(trSecretDistribution);
        	if(maximumEntropy == trFinalEntropy)
        		probMinLeakage += traceProbs.get(tr);
        }

        return probMinLeakage;
    }
    
    /**
     * 
     * @return maximum entropy of groups of final states of all traces 
     */
    public double maximumEntropy() {
    	
        List<String> tr;
    	Map<String, Double> trSecretDistribution;
    	double trFinalEntropy;
    	
    	double maximumEntropy = -1;
        for(Map.Entry<List<String>, Double> entry: traceProbs.entrySet()){
        	
            tr = entry.getKey();
            trSecretDistribution = expModel.finalStatesSecretDistribution(tr);
        
        	trFinalEntropy = entropy(trSecretDistribution);
        	if(maximumEntropy < trFinalEntropy)
        		maximumEntropy = trFinalEntropy;
        }
        
        return maximumEntropy;
    }
    
    /**
     * 
     * @return minimum entropy (Shannon or min-entropy) of groups of final states of all traces 
     */
    public double minimumEntropy() {
    	
    	// compute minimum entropy of groups of final states of traces
        List<String> tr;
    	Map<String, Double> trSecretDistribution;
    	double trFinalEntropy;
    	
    	double minimumEntropy = Double.MAX_VALUE;
        for(Map.Entry<List<String>, Double> entry: traceProbs.entrySet()){
        	
            tr = entry.getKey();
            trSecretDistribution = expModel.finalStatesSecretDistribution(tr);
        
            trFinalEntropy = entropy(trSecretDistribution);
        	if(minimumEntropy > trFinalEntropy)
        		minimumEntropy = trFinalEntropy;
        }
        return minimumEntropy;
    }
    
    
    /**
     * 
     * @return list of traces with maximum probability
     */
    public List<List<String>> tracesMaxProbability(){
    	
    	// compute max probability of all trace probabilities
    	List<String> tr;
    	double trProb;
    	
    	double maxTraceProb = -1;
    	for(Map.Entry<List<String>, Double> entry: traceProbs.entrySet()){
        	
            tr = entry.getKey();
    		trProb = entry.getValue();
    		
    		if(trProb > maxTraceProb)
    			maxTraceProb = trProb;
    	}
    	// find traces with maximum probability
    	List<List<String>> tracesMaxProb = new ArrayList<>();
    	
    	for(Map.Entry<List<String>, Double> entry: traceProbs.entrySet()){
        	
            tr = entry.getKey();
    		trProb = entry.getValue();
    		
    		if(trProb == maxTraceProb)
    			tracesMaxProb.add(tr);
    	}
    	return tracesMaxProb;
    }
    
    
    
    /**
     * Complete leakage occurs in traces that result in min-entropy of 0.
     * 
     * @return probability of complete leakage
     */
    public double probCompleteLeakage() {
    	
    	List<String> tr;
    	Map<String, Double> trSecretDistribution;
    	double trFinalEntropy, trProb;
    	
    	double probCompleteLeakage = 0;
        for(Map.Entry<List<String>, Double> entry: traceProbs.entrySet()){
        	
            tr = entry.getKey();
            trSecretDistribution = expModel.finalStatesSecretDistribution(tr);
    	
            trFinalEntropy = entropy(trSecretDistribution);
	        if(trFinalEntropy == 0.0) { // complete leakage
	        	trProb = traceProbs.get(tr);
	        	probCompleteLeakage += trProb;
	        }
		}
    	return probCompleteLeakage;
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
     * @return min-entropy of the distribution
     * 
     */
    public double minEntropy(Map<String, Double> distribution){
        double max_prob = -1.0;
        for(Map.Entry<String, Double> entry: distribution.entrySet()){
            double prob = entry.getValue();
            max_prob = Math.max(prob, max_prob);
        }
        double d =  - Logarithm.log2(max_prob);
        return d;
    }
    
    /**
     * 
     * @param distribution contains elements as String and their probabilities as Double
     * @return Shannon entropy of the distribution
     * 
     */
    public double shannonEntropy(Map<String, Double> distribution){
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
}