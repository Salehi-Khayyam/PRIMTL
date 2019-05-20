package leakage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jdd.JDDNode;
import jdd.JDDVars;
import odd.ODDNode;
import prism.PrismException;
import prism.PrismLog;
import prism.ProbModel;
import prism.StateListMTBDD;
import sparse.PrismSparse;

/**
*
* A class for applying back-bisimulation on ProbModel
*
* @author Khayyam Salehi & Ali A. Noroozi
*/
public class TopDownBackBisimulation {
	private ProbModel currentModel = null;
	private PrismLog mainLog = null;
	
	protected Map<Long, Map<Long, Double>> quotientTransitionForward; 	//quotient transitions after applying back-bisimulation
	protected List<QuotientState> quotientStates;	//quotient states after applying back-bisimulation, the index of each quotient state is the same as state_num in quotient state
	protected Set <Long> finalQuotientStates; //set of final quotient states
	protected List<State2> reachStates; // mapping from state_num to the state
	protected List<State2> startStates; // set of initial states
	protected Map<Integer, Set<Integer>> preMap;  //mapping from a state to it's predecessors, and null to initial states
	protected Map<Integer, Set<Integer>> postMap; //mapping from a state to it's successors (deadlock with self loops)
	protected List<Map<String, Set<State2>>> blocks_list;
	JDDNode matrix;
	String name;
	JDDVars rows;
	JDDVars cols;
	ODDNode odd;
	
	/**
	 * Constructor
	 * @param probModel
	 * @param mainLog 
	 * @throws PrismException 
	 */
	public TopDownBackBisimulation(ProbModel probModel, PrismLog mainLog) throws PrismException {
		this.currentModel = probModel;
		this.mainLog = mainLog;
		this.startStates = getInitialStates();
		this.reachStates = getStates();
		
		quotientTransitionForward = new HashMap<Long, Map<Long,Double>>();
		quotientStates = new ArrayList<QuotientState>();		
		finalQuotientStates = new HashSet<Long>();
		blocks_list = new ArrayList<Map<String,Set<State2>>>();
			
		matrix = this.currentModel.getTrans();
		name = this.currentModel.getTransSymbol();
		rows = this.currentModel.getAllDDRowVars();
		cols = this.currentModel.getAllDDColVars();
		odd = this.currentModel.getODD();
		
		double res = PrismSparse.PS_CreateSparseMatrix(matrix.ptr(), name, rows.array(), rows.n(), cols.array(), cols.n(), odd.ptr());
		if (res == -2) {
			throw new PrismException("Out of memory building transition matrix");
		}	
		
		// create the mapping for successor and predecessor of each state
		createPostAndPreMap();
	}
	
	/**
	  * 
	  * @return mapping from initial state number to the state in explicit manner
	  */
	private List<State2> getInitialStates() {
		StateListMTBDD start = (StateListMTBDD) currentModel.getStartStates();
		return start.getExplicitStates();
	}

	/**
	  * 
	  * @return mapping from state number to the state in explicit manner
	  */
	private List<State2> getStates() {
		StateListMTBDD states = (StateListMTBDD) currentModel.getReachableStates();
		return states.getExplicitStates();
		
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
	  * @return successor states of the set of states, considering self-loop
	  */
	public Set<State2> post(Set<State2> set_states) {
		Set<Integer> post_states_set_integer = new HashSet<Integer>();
		Set<State2> post_state2_set = new HashSet<State2>();
		
		for(State2 s: set_states) {
			post_states_set_integer = SetUtils.union(post_states_set_integer, postMap.get((int)s.getStateNumber()));
		}
		for (Integer i: post_states_set_integer) {
			Set<State2> tmp = new HashSet<State2>();
			tmp.add(this.reachStates.get(i));
			post_state2_set = SetUtils.union(post_state2_set, tmp);			
		}
		return post_state2_set;		
	}
			
	/**
	  * 
	  * @return predecessor states of the a state, considering self-loop
	  */
	private Set<State2> pre(int stateNumber) {
		Set<State2> pre_state2_set = new HashSet<State2>();
		for (Integer i: preMap.get(stateNumber)) {
			pre_state2_set.add(this.reachStates.get(i));
		}
		return pre_state2_set;	
	}
	 
	/**
	  * Create a mapping from each state to it's successors and predecessors
	  */
	private void createPostAndPreMap() {
		
		this.mainLog.println("\nStarting to create Post and Pre Map ...");
		this.preMap = new HashMap<Integer, Set<Integer>>();
		this.postMap = new HashMap<Integer, Set<Integer>>();
		int[] post_state_tmp = null;
		Set<Integer> post_state_tmp_set = null;
		Set<Integer> oldPreMap;

		for (int i = 0; i < this.reachStates.size(); i++) {
			post_state_tmp = post(i);
			post_state_tmp_set = Arrays.stream(post_state_tmp).boxed().collect(Collectors.toSet());
			postMap.put(i, post_state_tmp_set);

			for (Integer j: post_state_tmp_set) {
				if (!preMap.containsKey(j)) {
					Set<Integer> tmp = new HashSet<Integer>();
					tmp.add(i);
					preMap.put(j, tmp);
				}
				else {
					oldPreMap = preMap.get(j);
					oldPreMap.add(i);
					preMap.put(j, oldPreMap);
				}
			}			
		}
		for (State2 s: this.startStates) {
			preMap.put((int) s.getStateNumber(), null);
		}
	}
	
	 /**
	  * 
	  * @return true if s has no successor or the only successor is itself
	  */
	public boolean isFinalState(long s) {
		 
		 return PrismSparse.isFinalState((int) s, matrix, name, rows, cols, odd);
	 }
	
	/**
	 * execute top down back-bisimulation on the model and generate quotient states and transitions
	 * initial quotient state is at index 0 of this.quotientStates list
	 */
	public void doTopDownBackBisimulation() {
		this.mainLog.println("\nStarting to compute level blocks ...");
        blocks_list = computeLevelBlocks();
        
		this.mainLog.println("\nStarting to construct quotient states and transitions ...");
        doConstructQuotientStatesAndTransitions(blocks_list);
	}
	
	/**
	 * Construct quotient states and transitions between them including probability
	 * @param blocks_list
	 */
	private void doConstructQuotientStatesAndTransitions(List<Map<String, Set<State2>>> blocks_list) {
		int state_num = 0;
		int startIndexPreviousLevel = -1;
		int endIndexPreviousLevel = 0;
		
        for(Map<String, Set<State2>> bl: blocks_list){ //bl contains blocks of each level
        	for(Map.Entry<String, Set<State2>> entry: bl.entrySet()){
                Set<State2> state_set = (Set<State2>) entry.getValue();
                String public_data = ((State2)state_set.toArray()[0]).getPublicData();
                Set<Integer> primitiveStates = new HashSet<Integer>();
				List<String> secret_data = new ArrayList<String>();
				for(State2 s: state_set) {
					primitiveStates.add((int) s.getStateNumber());
					secret_data.add(s.getSecretData());
				}
				QuotientState b_state = new QuotientState(state_num, public_data, secret_data, primitiveStates);
				this.quotientStates.add(b_state);
				
				if (this.quotientStates.size() != 1) { // there is no transition to the initial quotient state (state_num = 0)
					setTransition(b_state, startIndexPreviousLevel, endIndexPreviousLevel);				
				}
				if (entry.getKey().startsWith("f")) {
					this.finalQuotientStates.add((long) state_num);					
				}
				state_num ++;
            }
        	startIndexPreviousLevel = endIndexPreviousLevel;
        	endIndexPreviousLevel += bl.size();
        }
	}
	
	/**
	 * Set transition from previous level to this state
	 * @param b_state
	 * @param startIndexPreviousLevel
	 * @param endIndexPreviousLevel
	 */
	 private void setTransition(QuotientState b_state, int startIndexPreviousLevel, int endIndexPreviousLevel) {
		 for (int i = startIndexPreviousLevel; i < endIndexPreviousLevel;i++) {
			 QuotientState q = this.quotientStates.get(i);
			 double prob = 0.0;
			 for (Integer s: q.getPrimitiveStates()) {
				 for (Integer t: b_state.getPrimitiveStates()) {
					 double tp = getTransitionProb(s, t);
					 if(tp > 0)
						 prob += tp;
				 }
			 }
			 prob /= q.getNumStates();
			 if(prob > 0 ) 
				 if(!this.quotientTransitionForward.containsKey(q.getStateNumber())) {
					 Map<Long, Double> m2 = new HashMap<Long, Double>();
					 m2.put(b_state.getStateNumber(), prob);
					 this.quotientTransitionForward.put(q.getStateNumber(), m2);
				 }
				 else {
					 Map<Long, Double> m2 = this.quotientTransitionForward.get(q.getStateNumber());
					 m2.put(b_state.getStateNumber(), prob);
				 }
		 }
	}

	/**
	  * 
	  * @return transition probability between states src and des
	  */
	private double getTransitionProb(Integer src, Integer des) {
		return PrismSparse.PS_GetTransitionProb((int) src, (int) des, matrix.ptr(), name, rows.array(), rows.n(), cols.array(), cols.n(), odd.ptr());
	}

	/**
     * compute back-bisimulation blocks for each level of the currentModel (probModel)
	 *
     * @return back-bisimulation blocks for each level of the currentModel (probModel)
     */
	private List<Map<String, Set<State2>>> computeLevelBlocks() {
		List<Map<String, Set<State2>>> level_block_list = new ArrayList<>();
        Map<String, Set<State2>> blocks_map = new HashMap<>(); //blocks in each level
        Set<State2> level_states; //all states in each level of the state machine
        Set<State2> level_states_post; //successor states of all states in each level of the state machine
        Set<State2> block;
        
        int level = 0;
        level_states = new HashSet<State2>(startStates);
        blocks_map.put("0", level_states);
        level_block_list.add(blocks_map);
        
        while(true) {
        	level++;
        	
        	level_states_post = SetUtils.difference(post(level_states), level_states); //difference is for eliminating "absorbing states" from states_post
        	if(level_states_post.isEmpty())
        		break; //last level reached, hop out of the while loop!
        	level_states = level_states_post;
        	
        	//compute blocks of level i
        	blocks_map = new HashMap<>();
        	for(State2 s: level_states){ //refine states of depth level to back-bisimilar states, based on backSignature of each state
        		String sig = backBisimSignature(s, level_block_list.get(level-1));
        		if(!blocks_map.containsKey(sig)){
        			block = new HashSet<>();
        			block.add(s);
        			blocks_map.put(sig, block);
        		}
        		else{
        			block = blocks_map.get(sig);
        			block.add(s);
        			//TODO چرا به بلاکز مپ پوت نشده؟
        		}
        	}
        	level_block_list.add(blocks_map);
        }
        return level_block_list;
	}
 
	/**
     * back-bisimulation signature of state s: public data + name (key) of blocks 
     * in block_list that have a transition to state s + "f" if s is a final state.
     * 
     * @param s
     * @param block_list is the set of back-bisim blocks of previous level of states
     * @return back bisimulation signature of state s
     */
	private String backBisimSignature(State2 s, Map<String, Set<State2>> block_list) {
		String str = s.getPublicData();
		for(Map.Entry<String, Set<State2>> entry_block: block_list.entrySet()) {
			Set<State2> block = (Set<State2>) entry_block.getValue();
            String block_label = (String) entry_block.getKey();
            if(!SetUtils.intersection(pre((int)s.getStateNumber()), block).isEmpty())
                str += ", " + block_label;
		}
		if(isFinalState(s.getStateNumber()))
			return "f" + str.hashCode();
		return Integer.toString(str.hashCode());
	}
}