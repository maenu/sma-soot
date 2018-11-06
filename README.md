# Example repository for using Soot for static analysis

## Installation

Clone this repo and import it as a Maven project into your IDE.
Run `ReachingDefinitionsAnalysis` to run a reaching definition analysis on the provided `TestClass`.
`ReachingDefinitionsAnalysis` shows an example of a dataflow analysis.
Its output is stored as Jimple files and Dot files, as well as the Dot files rendered as PDF, if you have the `dot` program installed.
If the execution fails with an error, you are probably on Windows and do not have `dot` installed.
In that case you can copy the content of a `*.dot` file into an [online renderer](http://www.webgraphviz.com/).

## Exercise

Your task is to assess how Guava v18.0, a utility library from Google, makes use of its own APIs.
You do this by statically analyzing the Guava code and count how often each internal method is invoked.

### Task 1: Finish `InternalInvocationAnalysis`

`InternalInvocationAnalysis` counts how often a method of an application is invoked internally, i.e., how many calls it makes to its own methods.
Your task is to implement `InternalInvocationAnalysis#process()` that is applied on all methods defined within the analyzed application.
Look at the class and its comments to get an idea on what is left to do.
If you run `InternalInvocationAnalysis` with an implemented `InternalInvocationAnalysis#process`, you will be shown the 10 most used methods.
You might want to look into the [Soot wiki](https://github.com/Sable/soot/wiki/Fundamental-Soot-objects) to better understand Soot.

Submit the code of the `InternalInvocationAnalysis#process` method.
Submit the output of the analysis.

### Task 2: Inspect the results

Analyze the output of `InternalInvocationAnalysis`.
Inspect the documentation and code of the 10 most used methods (you may want to look into the [Guava v18.0 repo](https://github.com/google/guava/tree/v18.0/guava)).
How would you summarize how Guava "eats its own dogfood", i.e., what are the features that Guava uses from itself?

Submit a short summary of your inspection results.
