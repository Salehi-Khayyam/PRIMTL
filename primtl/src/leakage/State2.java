package leakage;

/**
*
* @author Ali A. Noroozi
*/

public class State2 {
	
	protected long state_num; // state number
	protected String public_data; // values of observable variables in this state   
    private String secret_data; // values of secret variables in this state
   
    public State2(long state_num, String public_data, String secret_data) {
    	
    	this.state_num = state_num;
    	this.public_data = public_data;
    	this.secret_data = secret_data;
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
     * @return secret data of this state
     */
    public String getSecretData() {
        return secret_data;
    }

    public String toString() {
        return Long.toString(state_num);
    }
}
