### jPLSA: A Java package for the PLSA/PLSI model

This section describes the usage of jPLSA in command line or terminal, using a  pre-compiled file named `jPLSA.jar`. Here, it is supposed that Java is already set to run in command line or terminal (e.g. adding Java to the environment variable `path` in Windows OS).

Users can find the pre-compiled file `jPLSA.jar` and source codes in folders `jar` and `src`, respectively. The users can recompile the source codes by simply running `ant` (it is also expected that `ant` is already installed).

**File format of input corpus:**  Similar to file `corpus.txt`  in the `data` folder, jPLSA assumes that each line in the input corpus represents a document. Here, a document is a sequence of words/tokens separated by white space characters.

**Now, we can train PLSA/PLSI by executing:**

	$ java -jar jar/jPLSA.jar -corpus <PATH-TO-CORPUS> -ntopics <NUMBER-OF-TOPICS> -niters <NUMBER-OF-ITERATIONS> -name <EXPERIMENT-NAME> 

Example:	

	$ java -jar jar/jPLSA.jar -corpus data/corpus.txt -ntopics 20 -niters 1000 -name test 