# About
**PRIMTL** is a tool to compute **information leakage** of concurrent probabilistic programs. It takes as input a program written in the [PRISM language](http://www.prismmodelchecker.org/manual/ThePRISMLanguage/Introduction) and analyzes the information leakage of the program. 

The tool is built upon the [PRISM model checker](http://www.prismmodelchecker.org/). PRISM compiles a program written in the PRISM language, builds a discrete-time Markov chain model of the program and stores it using BDDs (Binary Decision Diagrams) and MTBDDs (Multi-Terminal Binary Decision Diagrams). PRIMTL uses these data structures to extract the set of reachable states and also create a sparse matrix containing the transitions. It then employs a depth-first path exploration algorithm (trace-based method) to find all paths and traces of the program. For each trace, it computes the probability of the trace and the posterior entropy of the secret induced by the trace. Furthermore, PRIMTL exploits a back-bisimulation-based method to highly minimize the state space to quotient states. It computes the leakage of the program from the quotient model. Using these values, PRIMTL computes the *exact* values of the information leakage. 

A main difference of PRIMTL and other related leakage quantification tools is that PRISM-InterLeak takes into account *intermediate leakages*. This is suitable for *concurrent* programs, in which the attacker is able to observe intermediate values of publicly observable variables. 

# Installation
Compiling:
```
cd primtl
make
```


# Usage
Change directory to `bin` and run `prism` with `-trace` or `-backbisim` switches to exploit trace-based or back-bisimulation-based methods respectively:
```
cd bin
prism  {-trace|-backbisim} [options] <model-file> [more-options]
```
Options:

`-verbose` Verbose the features of quotient model induced by back-bisimulation-based method (only works with the `-backbisim` switch)

`-min`  Compute the expected leakage using min-entropy 

`-shannon`  Compute the expected leakage using Shannon entropy. The default is Shannon entropy

`-leakbounds`  Compute maximum and minimum leakages, which are upper and lower leakage bounds for an attacker with a given prior knowledge about the secret input

`-bounded <n>`  Compute bounded time leakage, which is the amount of expected leakage at a given time (step)

`-initdist <file>`  Specify the initial probability distribution of the secret input. If not specified, the uniform distribution is assumed

`-help | -h | -?`  Display this help message

`-prismhelp`  Display PRISM help message

`-version`  Display PRISM-InterLeak and PRISM version info


# People
The people currently working on the tool are:

* Khayyam Salehi, currently a Ph.D. student at University of Tabriz and developer of the project,

* Ali A. Noroozi, currently a Ph.D. student at [University of Tabriz](http://tabrizu.ac.ir/en) and developer of the project,

* [Jaber Karimpour](http://simap.tabrizu.ac.ir/cv/karimpour/?lang=en-gb), an associate professor at University of Tabriz and supervisor of the project,

* [Ayaz Isazadeh](http://isazadeh.net/ayaz), a professor at University of Tabriz and supervisor of the project.

