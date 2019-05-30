package leakage;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import prism.PrismException;
import prism.PrismLog;


/**
*	A class for exploring traces of quotient states and quotient transitions obtained by back-bisimulation process
*
* @author Khayyam Salehi & Ali A. Noroozi
*/
public class ProbModelBackBisimExplorer {
	private TopDownBackBisimulation backBisimModel = null; 
	private PrismLog mainLog = null;
	/**
	 * contains probabilities of traces constructed by back bisimulation process,      
	 * final state number --> trace of public values --> probability to reach final
	 */
	protected Map<Long, Double> traceProbBackBisim; //probability of reaching to final quotient states 
	public static int UNIFORM_PRIOR_KNOWLEDGE = 0; // probability distribution of the secret variable not specified by the user -> uniform distribution assumed
	public static int INIT_DIST_FILE_PRIOR_KNOWLEDGE = 1; // probability distribution of the secret variable is imported from a file specified by the user
	private int priorKnowledgeType = UNIFORM_PRIOR_KNOWLEDGE; 
	Map<String, Double> priorKnowledge; // probability distribution of the secret variable
	
	/**
	 * Constructor
	 * @param initDistFileName
	 * @param backBisim 
	 * @param mainLog 
	 * @throws PrismException 
	 */
	public ProbModelBackBisimExplorer(String initDistFileName, TopDownBackBisimulation backBisim, PrismLog mainLog) throws PrismException {
		this.backBisimModel = backBisim;
		this.mainLog = mainLog;
		
		if (initDistFileName == null) { // probability distribution of the secret variable not specified by the user
			priorKnowledgeType = UNIFORM_PRIOR_KNOWLEDGE;
    		this.priorKnowledge = uniformPriorKnowledge();
		}
		else {
			priorKnowledgeType = INIT_DIST_FILE_PRIOR_KNOWLEDGE; // import probability distribution from initDistFileName
			this.priorKnowledge = readInitDistribution(initDistFileName);
		}
			
		traceProbBackBisim = new HashMap<>();
	}
	
	/**
	  * Read probability distribution of the secret variable (prior knowledge) from initDistFileName and return it
	  * @throws PrismException 
	  */
	private Map<String, Double> readInitDistribution(String initDistFileName) throws PrismException {
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

			int startSize = backBisimModel.startStates.size();
			if(initDist.size() != startSize)
				throw new PrismException("initDist file should contain " + startSize + " probabilities");
			 
			if(Math.abs(initDistSum - 1.0) > 0.001)
				throw new PrismException("Sume of probabilities in \"" + initDistFileName + "\" should be equal to 1.0");
			 
			int i=0;
			for(State2 s: backBisimModel.startStates) {
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
	private Map<String, Double> uniformPriorKnowledge() {
		
		Map<String, Double> uniformPriorKnowledge = new TreeMap<>();
		for(State2 s: backBisimModel.startStates) {
			uniformPriorKnowledge.put(s.getSecretData(), 1.0/backBisimModel.startStates.size());
		}
		return uniformPriorKnowledge;
	}
	
	/**
	  * 
	  * @return prior knowledge, which is the probability distribution of the secret values
	  */
	public Map<String, Double> getPriorKnowledge() {
		return priorKnowledge;
	}
	
	/**
	 * Exploration of traces containing quotient states and probability of initial to final quotient state  
	 *   
	 */
	public void exploreModel() {
		long initial = 0;
		long current;
		this.mainLog.println("\nStarting to expolre traces contining quotient states ...");

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
	        if (this.backBisimModel.finalQuotientStates.contains(current)) {
	        	addTopToBottomPath(current, parent);
	        }
	         // Push successors of the popped state to stack. Also set their parent state in the map
	         else
	             for(long ps: this.backBisimModel.quotientTransitionForward.get(current).keySet()) {
	            	 if(ps != current) {
	            		 parent.put(ps, current);
	            		 nodeStack.push(ps);
	            	 }
	             }
	     }
	}
	
	/**
	 * Function to add initial-to-final path for a final state using parent states stored in the parent Map
	 * @param curr
	 * @param parent
	 */
	private void addTopToBottomPath(long curr, Map<Long, Long> parent) {
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
	}

	/**
	 * Add probability path pa to traceProbbackBisim
	 * @param pa
	 */
	private void handlePath(List<Long> pa) {
		double prFinal = 0;
		double probPath = 1.0;
		long finalState = pa.get(pa.size()-1); 
		
		if(traceProbBackBisim.containsKey(finalState))
			prFinal = traceProbBackBisim.get(finalState);
		for(int i=0; i < pa.size()-1; i++)
			probPath *= this.backBisimModel.quotientTransitionForward.get(pa.get(i)).get(pa.get(i + 1));
		
		traceProbBackBisim.put(finalState, prFinal + probPath);
	}

	/**
	  * 
	  * @return type of the prior knowledge (uniform or distribution from file)
	  */
	public int getPriorKnowledgeType() {
		return priorKnowledgeType;
	}

	/**
	 * Create the distribution of secret values in final quotient states
	 * @param finalState number
	 * @return mapping from secret value to its probability in final quotient state 
	 */
	public Map<String, Double> finalStatesSecretDistribution(long finalState) {
		Map<String, Double> mp = new HashMap<String, Double>();
		List<String> sd = this.backBisimModel.quotientStates.get((int) finalState).getSecretData();
//		mainLog.println(sd);
		if (priorKnowledgeType == UNIFORM_PRIOR_KNOWLEDGE){
			for (String s: sd) {
				if (mp.containsKey(s)) {
					double d = mp.get(s);
					mp.put(s, d + 1.0/sd.size());
				}
				else
					mp.put(s, 1.0/sd.size());
			}
		}
		else 
		{
			Map<String, Integer> final_states_secret_frequencies = finalStatesSecretFrequencies(sd);
			double denom = 0.0;
			String s;
			int freq;
			double fssd_prob, fsp,d;
		    for(Map.Entry<String, Integer> entry_freq: final_states_secret_frequencies.entrySet()){
		        s = entry_freq.getKey();
		        freq = entry_freq.getValue();
		        fssd_prob = this.priorKnowledge.get(s);
		        fsp = fssd_prob * freq;
		        denom += fsp;
		    }
//		    mainLog.println(final_states_secret_frequencies);
//		    mainLog.println(denom);
		    
		    for(Map.Entry<String, Integer> entry_freq: final_states_secret_frequencies.entrySet()){
		    	s = entry_freq.getKey();
		        int final_state_frequency = entry_freq.getValue();
		        fssd_prob = this.priorKnowledge.get(s);
		        fsp = fssd_prob * final_state_frequency;
		        if (denom != 0)
		        	d = fsp / denom;
		        else 
		        	d = 0;
		        mp.put(s, d);
		    }
		}	
		return mp;
	}

	private Map<String, Integer> finalStatesSecretFrequencies(List<String> sd) {
		Map<String, Integer> finStateSecretFreq = new HashMap<>();
		int f;
	        
	    for(String s: sd) {
	    	f=0;
	    	if(finStateSecretFreq.containsKey(s)) 
	    		f = finStateSecretFreq.get(s);
	    	finStateSecretFreq.put(s, f+1);
	     }
	     
	    return finStateSecretFreq;
	}

	/**
	 * export back-bisimulation quotient to XDOT format
	 */
	public void exportXDOT() {
		try {
			FileWriter writer= new FileWriter("out.dot");
			writer.write("digraph P {\n");
			writer.write("size=\"8,5\"\n");
			writer.write("node [shape=box];\n");
			for (Map.Entry<Long, Map<Long, Double>> entry: this.backBisimModel.quotientTransitionForward.entrySet()) {
				long src = entry.getKey();
				Map<Long, Double> desMap = entry.getValue();
				for(Map.Entry<Long, Double> e: desMap.entrySet()) {
					long des = e.getKey();
					double prob = e.getValue();
					writer.write(src + " -> " + des + " [ label=\"" + prob + "\" ];\n");
				}
			}
			for (QuotientState q: this.backBisimModel.quotientStates) {
				writer.write(q.getStateNumber() + " [ label=\"" + q.getStateNumber() + "\\n" + q.getPrimitiveStates().toString() + "\" ];\n");
			}
			writer.write("}");
			writer.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}


}
