package leakage;

import java.util.List;
import java.util.Set;

/**
*
* @author Khayyam Salehi & Ali A. Noroozi
*/

public class QuotientState {
	private long state_num; // state number
	private String public_data; // values of observable variables in this state   
    private List<String> secret_data_set; // values of the set of secret variables in this state
    private Set<Integer> primitiveStates; 
      
    public QuotientState(long state_num, String public_data, List<String> secret_data, Set<Integer> primitiveStates) {
		this.state_num = state_num;
		this.public_data = public_data;
		this.secret_data_set = secret_data;
		this.primitiveStates = primitiveStates;
	}

    /**
     * 
     * @return state number
     */
   public long getStateNumber() {
        return state_num;
    }
    
    /**
     * 
     * @return public data of this state
     */
    public String getPublicData() {
        return public_data;
    }    
    
    /**
     * 
     * @return the set of secret data of this state
     */
    public List<String> getSecretData() {
        return secret_data_set;
    }
    
    /**
     * 
     * @return the number of states with different secret in this quotient state
     */
    public int getNumSecretStates() {
        return secret_data_set.size();
    }    
    
    /**
     * 
     * @return the number of states in this quotient state
     */
    public int getNumStates() {
        return primitiveStates.size();
    }    
    
    /**
     * 
     * @return the primitive states creating the quotient state 
     */
    public Set<Integer> getPrimitiveStates() {
		return primitiveStates;
	}

	public String toString() {
        return Long.toString(state_num);
    }	
}
