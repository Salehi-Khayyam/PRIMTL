dtmc

// for internal attacker, make coin1 and coin2 observable

// number of cryptographers
const int N = 4;

// coin probability
const double pr = 0.5;

// global variable which decides who pays
// (0 - master pays, i=1..N - cryptographer i pays)
global secret pay : [0..N];

global coin1 : [0..2]; // coin value of crypt1
global coin2 : [0..2]; // coin value of crypt2
global coin3 : [0..2]; // coin value of crypt3
global coin4 : [0..2]; // coin value of crypt4

// declaration of each cryptographer (0 = disagree, 1 = agree)
global observable agree1 : [0..1]; // declaration of crypt1 
global observable agree2 : [0..1]; // declaration of crypt2 
global observable agree3 : [0..1]; // declaration of crypt3 
global observable agree4 : [0..1]; // declaration of crypt4 

// constants used in renaming (identities of cryptographers)
const int p1 = 1;
const int p2 = 2;
const int p3 = 3;
const int p4 = 4;


// module for first cryptographer
module crypt
	s1 : [-4..N]; // its status 
	
	// flip coins
	[] s1=-4 & coin1=0 -> pr : (coin1'=1) & (s1'=-3) + 1.0-pr : (coin1'=2) & (s1'=-3);
    [] s1=-3 & coin2=0 -> pr : (coin2'=1) & (s1'=-2) + 1.0-pr : (coin2'=2) & (s1'=-2);
    [] s1=-2 & coin3=0 -> pr : (coin3'=1) & (s1'=-1) +  1.0-pr : (coin3'=2) & (s1'=-1);
    [] s1=-1 & coin4=0 -> pr : (coin4'=1) & (s1'=0) +  1.0-pr : (coin4'=2) & (s1'=0);
	
	// make statement (once relevant coins have been flipped)
	// agree (coins the same and does not pay)
	[] s1=0 & coin1>0 & coin2>0 & coin1=coin2    & (pay!=p1) -> (s1'=1) & (agree1'=1);
	// disagree (coins different and does not pay)
	[] s1=0 & coin1>0 & coin2>0 & !(coin1=coin2) & (pay!=p1) -> (s1'=1);
	// disagree (coins the same and pays)
	[] s1=0 & coin1>0 & coin2>0 & coin1=coin2    & (pay=p1)  -> (s1'=1);
	// agree (coins different and pays)
	[] s1=0 & coin1>0 & coin2>0 & !(coin1=coin2) & (pay=p1)  -> (s1'=1) & (agree1'=1);
	
	[] s1=1 & coin2>0 & coin3>0 & coin2=coin3    & (pay!=p2) -> (s1'=2) & (agree2'=1);
	[] s1=1 & coin2>0 & coin3>0 & !(coin2=coin3) & (pay!=p2) -> (s1'=2);
	[] s1=1 & coin2>0 & coin3>0 & coin2=coin3    & (pay=p2)  -> (s1'=2);
	[] s1=1 & coin2>0 & coin3>0 & !(coin2=coin3) & (pay=p2)  -> (s1'=2) & (agree2'=1);

    [] s1=2 & coin3>0 & coin4>0 & coin3=coin4    & (pay!=p3) -> (s1'=3) & (agree3'=1);
	[] s1=2 & coin3>0 & coin4>0 & !(coin3=coin4) & (pay!=p3) -> (s1'=3);
	[] s1=2 & coin3>0 & coin4>0 & coin3=coin4    & (pay=p3)  -> (s1'=3);
	[] s1=2 & coin3>0 & coin4>0 & !(coin3=coin4) & (pay=p3)  -> (s1'=3) & (agree3'=1);

	[] s1=3 & coin4>0 & coin1>0 & coin4=coin1    & (pay!=p4) -> (s1'=4) & (agree4'=1);
	[] s1=3 & coin4>0 & coin1>0 & !(coin4=coin1) & (pay!=p4) -> (s1'=4);
	[] s1=3 & coin4>0 & coin1>0 & coin4=coin1    & (pay=p4)  -> (s1'=4);
	[] s1=3 & coin4>0 & coin1>0 & !(coin4=coin1) & (pay=p4)  -> (s1'=4) & (agree4'=1);
	
	[] s1=4 -> true;
endmodule

// set of initial states
// (cryptographers in their initial state, "pay" can be anything)
init  s1=-4 & coin1=0&agree1=0 & coin2=0&agree2=0 & coin3=0&agree3=0 & coin4=0&agree4=0  endinit

