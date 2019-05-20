dtmc

//value of the variable l
global observable l: [0..5];

//value of the variable h
global secret h: [0..1];

global counter : [0..14];

module thread
	
	[] h=1 & counter=0 -> (counter'=1) & (l'=1);
	[] counter=1 -> (counter'=2) & (l'=2);
    [] counter=2 -> (counter'=3) & (l'=3);
    [] counter=3 -> (counter'=4) & (l'=4);
    [] counter=4 -> (counter'=5) & (l'=5);
    
    [] h=0 & counter=0 -> 0.5 : (counter'=6) & (l'=1) + 0.5 : (counter'=8) & (l'=2);
    [] counter=6 -> (counter'=7) & (l'=2);
    [] counter=8 -> (counter'=9) & (l'=1);
    [] counter=7 | counter=9 -> (counter'=10) & (l'=3);
    [] counter=10 -> 0.5 : (counter'=11) & (l'=4) + 0.5 : (counter'=13) & (l'=5);
    [] counter=11 -> (counter'=12) & (l'=5);
    [] counter=13 -> (counter'=14) & (l'=4);

endmodule

init l=0 & counter=0 endinit
