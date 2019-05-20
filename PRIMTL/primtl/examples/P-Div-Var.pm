dtmc

//value of the variable l
global observable l: [0..1];

//value of the variable h
global secret h: [0..3];

global counter : [0..5];

module thread

    [] h=3 & counter=0 -> (counter'=1) & (l'=1);
    [] h!=3 & counter=0 -> 0.5 : (counter'=2) & (l'=floor(h/2)) + 0.5 : (counter'=3) & (l'=mod(h,2));
    [] counter=2 -> (counter'=4) & (l'=mod(h,2));
    [] counter=3 -> (counter'=4) & (l'=floor(h/2));

endmodule

init l=0 & counter=0 endinit
