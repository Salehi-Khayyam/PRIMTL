package leakage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import jdd.JDDNode;
import jdd.JDDVars;
import odd.ODDNode;
import prism.PrismException;
import prism.ProbModel;
import prism.StateListMTBDD;
import sparse.PrismSparse;

/**
*
*	A class for explicit representation and exploration of ProbModel
*
* @author Ali A. Noroozi
*/

public class ProbModelExplicitExplorer {
	
	private ProbModel currentModel = null;
	List<State2> reachStates; // set of reachable states
	List<State2> startStates; // set of initial states
	
	Map<List<String>, Double> traceProbs; // contains probabilities of traces
	Map<List<String>, Set<Long>> tracesFinalStates; // contains final states that result from each trace. A state is considered final, if it has no successor or the only successor is itself 
	
	public static int UNIFORM_PRIOR_KNOWLEDGE = 0; // probability distribution of the secret variable not specified by the user -> uniform distribution assumed
	public static int INIT_DIST_FILE_PRIOR_KNOWLEDGE = 1; // probability distribution of the secret variable is imported from a file specified by the user
	private int priorKnowledgeType = UNIFORM_PRIOR_KNOWLEDGE; 
	Map<String, Double> priorKnowledge; // probability distribution of the secret variable

	JDDNode matrix;
	String name;
	JDDVars rows;
	JDDVars cols;
	ODDNode odd;
	
	public ProbModelExplicitExplorer(ProbModel currentModel, String initDistFileName) throws PrismException {
		
		this.currentModel = currentModel;
		this.reachStates = getStates();
		this.startStates = getInitialStates();
		
		if (initDistFileName == null) { // probability distribution of the secret variable not specified by the user
			priorKnowledgeType = UNIFORM_PRIOR_KNOWLEDGE;
    		this.priorKnowledge = uniformPriorKnowledge();
		}
		else {
			priorKnowledgeType = INIT_DIST_FILE_PRIOR_KNOWLEDGE; // import probability distribution from initDistFileName
			this.priorKnowledge = readInitDistribution(initDistFileName);
		}
			
		traceProbs = new HashMap<>();
		tracesFinalStates = new HashMap<>();
		
		matrix = currentModel.getTrans();
		name = currentModel.getTransSymbol();
		rows = currentModel.getAllDDRowVars();
		cols = currentModel.getAllDDColVars();
		odd = currentModel.getODD();
	}
	
	/**
	 * DFS exploration of the model to determine traces, trace probabilities, and groups of trace final states
	 * 
	 */
	public void exploreModel(boolean bounded, int boundedStep) throws PrismException {
		
		double res = PrismSparse.PS_CreateSparseMatrix(matrix.ptr(), name, rows.array(), rows.n(), cols.array(), cols.n(), odd.ptr());
		if (res == -2) {
			throw new PrismException("Out of memory building transition matrix");
		}
		
         
    	if(bounded) { // bounded
    		
    		long path[] = new long[boundedStep+2];
    		for (State2 s : startStates)
    			explorePathsRecur(s.getStateNumber(), path, 0, bounded, boundedStep);
    	}
    	else // explore whole paths (till final states) 
    		for (State2 s : startStates)
    			explorePathsNonRecur(s.getStateNumber());
        
//        PrismSparse.PS_FreeSparseMatrix(matrix.ptr(), name, rows.array(), rows.n(), cols.array(), cols.n(), odd.ptr());
    	
    	return;
    }
	
	/**
     * Recursive helper function for explorePaths().
     * Given a state, and an array containing the path from the initial state 
     * down to but not including this state, explore all the paths.
     * 
     */
    private void explorePathsRecur(long s, long path[], int path_length, boolean bounded, int boundedStep) {
  
        /* append this state to the path array */
        path[path_length] = s;
        path_length++; // path_length=number of states of the path
  
        /* it's a final state, so add the path that led to here to path_list */
        if ((!bounded && isFinalState(s)) || 
        		(bounded && path_length == boundedStep + 1)){
        	
            List<Long> pa = new ArrayList<>();
            for(int i=0; i<path_length; i++)
                pa.add(path[i]);
            
            handlePath(pa);
        }
        else
            /* try subtrees */
            for(long ps: post(s)) 
            	explorePathsRecur(ps, path, path_length, bounded, boundedStep);
    }    
  
	 /**
	  * An iterative function to do pre-order traversal of the machine  and add 
	  * initial-to-final path to path_list without using recursion
	  * 
	  */
	 void explorePathsNonRecur(long initial)
	 {
		 long current;
		 
	     // Create an empty stack and push initial to it
	     Stack<Long> nodeStack = new Stack<>();
	     nodeStack.push(initial);
	  
	     // Create a map to store parent states of each state
	     Map<Long, Long> parent = new HashMap<>();
	  
	     // parent of initial is NULL
	     parent.put(initial, (long) -1);
	  
	     // Pop all items one by one and push their successors
	     while (!nodeStack.empty())
	     {
	         // Pop the top item from stack
	         current = nodeStack.pop();
	  
	         // If final state encountered, add Top-To-Bottom path to path_list
	         if (isFinalState(current)) {
	             addTopToBottomPath(current, parent);
	         }
	  
	         // Push successors of the popped state to stack. Also set their parent state in the map
	         else
	             for(long ps: post(current)) {
	            	 if(ps != current) {
	            		 parent.put(ps, current);
	            		 nodeStack.push(ps);
	            	 }
	             }
	     }
	 }
    
	 /**
	  * Function to add initial-to-final path for a final state using parent states stored in the parent Map
	  *
	  */
	 void addTopToBottomPath(long curr, Map<Long, Long> parent)
	 {
		 Stack<Long> stk = new Stack<>();
	  
	     // start from final state and keep on pushing nodes into stack till initial state is reached
	     while (curr != -1)
	     {
	         stk.push(curr);
	         curr = parent.get(curr);
	     }
	     
	     List<Long> pa = new ArrayList<>();
	     // Start popping sates from stack and build a path in order to add it to path_list
	     while (!stk.empty())
	     {
	         curr = stk.pop();
	         pa.add(curr);
	     }
	     
	     handlePath(pa);
	     
	     return;
    	 	     
	 }
	 
	 /**
	  * Add probability path pa to traceProbs and final state of path pa to tracesFinalStates
	  * 
	  */
	 public void handlePath(List<Long> pa) {
		 
		 List<String> tr = trace(pa);
	     double pr = 0;
	     	     
	     if(traceProbs.containsKey(tr))
	    	 pr = traceProbs.get(tr);
	    	     
	     
	     double muInit;
	     if (priorKnowledgeType == UNIFORM_PRIOR_KNOWLEDGE) // uniform prior knowledge
	    	 muInit = 1.0 / startStates.size();
	     else { // prior knowledge determined by the user (It may be uniform or not)
	    	 long startSt = pa.get(0);
	    	 muInit = priorKnowledge.get(reachStates.get((int)startSt).getSecretData());
	     }
	     traceProbs.put(tr, pr + muInit*prob(pa));
	     
	     long finState = pa.get(pa.size()-1);
	     Set<Long> finGroup;
	     
	     if (!tracesFinalStates.containsKey(tr)) {
	    	 finGroup = new HashSet<>();
	     }
	     else {
	    	 finGroup = tracesFinalStates.get(tr);
	     }
	     finGroup.add(finState);
    	 tracesFinalStates.put(tr, finGroup);
    	 
    	 return;
	 }
	 
	 /**
	  * 
	  * @return the explicit set of reachable states 
	  */
	 public List<State2> getStates() {
		 
		 StateListMTBDD states = (StateListMTBDD) currentModel.getReachableStates();
		 return states.getExplicitStates();
	 }

	 /**
	  * 
	  * @return the explicit set of initial states 
	  */
	 public List<State2> getInitialStates() {
		 
		 StateListMTBDD start = (StateListMTBDD) currentModel.getStartStates();
		 return start.getExplicitStates();
	 }
	 
	 /**
	  * 
	  * @return successor states of s. If s has a self-loop, it is included in post(s)
	  */
	 public int[] post(long s) {
		 
		 return PrismSparse.PS_SuccessorStates((int) s, matrix.ptr(), name, rows.array(), rows.n(), cols.array(), cols.n(), odd.ptr());
	 }
	 
	 /**
	  * 
	  * @return true if s has no successor or the only successor is itself
	  */
	 public boolean isFinalState(long s) {
		 
		 return PrismSparse.isFinalState((int) s, matrix, name, rows, cols, odd);
	 }
	 
	 /**
	  * 
	  * @return transition probability between states i and j
	  */
	 public double getTransitionProb(long i, long j) {
		 
		 return PrismSparse.PS_GetTransitionProb((int) i, (int) j, matrix.ptr(), name, rows.array(), rows.n(), cols.array(), cols.n(), odd.ptr());
	 }
	 
	 /**
	  * 
	  * @return probability of path. Probability of the initial state is not included.
	  */
	 public double prob(List<Long> path) {
		 
        double prob = 1.0;
        for(int i=0; i < path.size()-1; i++)
            prob = prob *  getTransitionProb(path.get(i), path.get(i+1));
        
        return prob;
	 }
	 
	 /**
	  * 
	  * @return trace of path
	  */
	 public List<String> trace(List<Long> path){
		 
        List<String> trace = new ArrayList<>();
        for(long s: path)
            trace.add(reachStates.get((int) s).getPublicData());
        
        return trace;
	 }
	 
	 /**
	  * Computes freq^h_{s_f(T)}
	  * @return frequencies of secret values in the group of final states of trace
	  */
	 public Map<String, Integer> finalStatesSecretFrequencies(List<String> trace){
	        
		 Map<String, Integer> finStateSecretFreq = new HashMap<>();
		 String finStateSecret;
		 int f;
	        
	     for(long finState: tracesFinalStates.get(trace)) {
	            
	    	 finStateSecret = reachStates.get((int) finState).getSecretData();
        
	    	 f=0;
	    	 if(finStateSecretFreq.containsKey(finStateSecret)) 
	    		 f = finStateSecretFreq.get(finStateSecret);
	    	 finStateSecretFreq.put(finStateSecret, f+1);
	     }
	     
	     return finStateSecretFreq;
	 }
	 
	 /**
	  * Computes mu^h_{s_f(T)}
	  * @return probability distribution of secret values in the group of final states of trace
	  */
	 public Map<String, Double> finalStatesSecretDistribution(List<String> trace){
     
     
		 int freq;
		 double fssd_prob, fsp, dist;
		 String secret_data;
		 
	     Map<String, Double> final_states_secret_distribution = new HashMap<>();    
	     
	     Map<String, Integer> final_states_secret_frequencies = finalStatesSecretFrequencies(trace);
	     
	     double denom = 0.0;
	     for(Map.Entry<String, Integer> entry_freq: final_states_secret_frequencies.entrySet()){
	         secret_data = entry_freq.getKey();
	         freq = entry_freq.getValue();
	         fssd_prob = this.priorKnowledge.get(secret_data);
	         fsp = fssd_prob * freq;
	         denom += fsp;
	     }
	     
	     for(Map.Entry<String, Integer> entry_freq: final_states_secret_frequencies.entrySet()){
	    	 secret_data = entry_freq.getKey();
	         int final_state_frequency = entry_freq.getValue();
	         fssd_prob = this.priorKnowledge.get(secret_data);
	         fsp = fssd_prob * final_state_frequency;
	         dist = fsp / denom;
	         final_states_secret_distribution.put(secret_data, dist);
	     }
         
	     return final_states_secret_distribution;
	 }
	 
	 /**
	  * Read probability distribution of the secret variable (prior knowledge) from initDistFileName and return it
	  */
	 public Map<String, Double> readInitDistribution(String initDistFileName) throws PrismException{
		 
		 Map<String, Double> priorKnowledge = new HashMap<>();
		 List<Double> initDist = new ArrayList<>();
		 double initDistSum = 0.0; 
					 
		 BufferedReader in;
		 String l;
		 int lineNum = 0;
		 double d;
		
		 try {
			// open file for reading
			 in = new BufferedReader(new FileReader(initDistFileName));
			 // read remaining lines
			 l = in.readLine(); lineNum++;
			 while (l != null) {
				 l = l.trim();
				 if (!("".equals(l))) {
					 d = Double.parseDouble(l);
					 initDist.add(d);
					 initDistSum += d;
				 }
				 l = in.readLine(); lineNum++;
			 }
			 // Close file
			 in.close();

			 int startSize = startStates.size();
			 if(initDist.size() != startSize)
				 throw new PrismException("initDist file should contain " + startSize + " probabilities");
			 
			 if(Math.abs(initDistSum - 1.0) > 0.001)
				 throw new PrismException("Sume of probabilities in \"" + initDistFileName + "\" should be equal to 1.0");
			 
			 int i=0;
			 for(State2 s: startStates) {
				 priorKnowledge.put(s.getSecretData(), initDist.get(i));
				 i++;
			 }
			 
		 } catch (IOException e) {
			 throw new PrismException("File I/O error reading from \"" + initDistFileName + "\"");
		 }
		 catch (NumberFormatException e) {
			 throw new PrismException("Error detected at line " + lineNum + " of file \"" + initDistFileName + "\"");
		 }
		
		 return priorKnowledge;
	 }
	 
	 /**
	  * 
	  * @return uniform probability distribution for the secret values of initial states
	  */
	 public Map<String, Double> uniformPriorKnowledge() {
	    
		 Map<String, Double> uniformPriorKnowledge = new TreeMap<>();
		 
		 for(State2 s: startStates) {
			 uniformPriorKnowledge.put(s.getSecretData(), 1.0/startStates.size());
		 }
		
		return uniformPriorKnowledge;
	 }
	 
	 /**
	  * 
	  * @return probabilities of traces
	  */
	 public Map<List<String>, Double> getTraceProbs() {
		 
		 return traceProbs;
	 }
	 
	 /**
	  * 
	  * @return prior knowledge, which is the probability distribution of the secret values
	  */
	 public Map<String, Double> getPriorKnowledge() {
		 
		 return priorKnowledge;
	 }
	
}
