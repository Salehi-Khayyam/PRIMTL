dtmc

// number of candidates
const int c = 4;

// number of voters
const int N = 2;

// state and preference of each voter
global s1 : [0..1];
global secret vot1 : [1..c]; 
global s2 : [0..1];
global secret vot2 : [1..c]; 



// number of votes for each candidate
// public variables
global observable result1 : [0..N];
global observable result2 : [0..N];
global observable result3 : [0..N];
global observable result4 : [0..N];


// module for first voter1
module voter1	
    //voter1 voted to candidate 1
    [] s1=0 & vot1 = 1 & result1<N -> (result1'=result1+1) & (s1'=1);
    //voter1 voted to candidate 2
    [] s1=0 & vot1 = 2 & result2<N -> (result2'=result2+1) & (s1'=1);
    //voter1 voted to candidate 3
    [] s1=0 & vot1 = 3 & result3<N -> (result3'=result3+1) & (s1'=1);
    //voter1 voted to candidate 4
    [] s1=0 & vot1 = 4 & result4<N -> (result4'=result4+1) & (s1'=1);
endmodule

// construct further voters with renaming
module voter2 = voter1 [ vot1=vot2, s1=s2 ] endmodule


// set of initial states
// (voters in their initial state, "vot1, ..., votN" can be anything)
init  s1=0 & s2=0 & result1=0&result2=0&result3=0&result4=0 endinit

