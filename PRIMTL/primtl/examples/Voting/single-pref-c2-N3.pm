dtmc

// number of candidates
const int c = 2;

// number of voters
const int N = 3;


// state and preference of each voter
global s1 : [0..1];
global secret vot1 : [1..c]; // secret variable
global s2 : [0..1];
global secret vot2 : [1..c]; // secret variable
global s3 : [0..1];
global secret vot3 : [1..c]; // secret variable

// number of votes for each candidate
global observable result1 : [0..N]; // observable (public) variable
global observable result2 : [0..N]; // observable (public) variable


// module for first voter1
module voter1	

    //count the votes
    //voter1 voted to candidate 1
    [] s1=0 & vot1 = 1 & result1<N -> (result1'=result1+1) & (s1'=1);

    //voter1 voted to candidate 2
    [] s1=0 & vot1 = 2 & result2<N -> (result2'=result2+1) & (s1'=1);
endmodule

// construct further voters with renaming
module voter2 = voter1 [ vot1=vot2, s1=s2 ] endmodule
module voter3 = voter1 [ vot1=vot3, s1=s3 ] endmodule

// set of initial states
// (voters in their initial state, "vot1, ..., votN" can be anything)
init  s1=0 & s2=0 & s3=0 & result1=0&result2=0 endinit

