Student ID: a1992110

Programming Assignment 1 - Regular Expression Engine
Event Driven Computing

A regular expression engine that parses an expression into a syntax tree,
constructs an epsilon-NFA from it, and evaluates input strings by stepping
through that machine one character at a time. No java.util.regex is used
anywhere in the implementation.


BUILDING AND RUNNING

    javac RegexEngine.java
    java RegexEngine
    java RegexEngine -v

The first line of standard input is the regular expression. Every line after
it is an input string to test. An invalid expression is reported on standard
error and exits with status 1.


RUNNING THE TESTS

JUnit 4 and Hamcrest are in lib/.

    javac -cp "lib/junit-4.13.2.jar:lib/hamcrest-core-1.3.jar" *.java
    java -cp ".:lib/junit-4.13.2.jar:lib/hamcrest-core-1.3.jar" \
        org.junit.runner.JUnitCore RegexEngine_Test

On Windows, use ; instead of : as the classpath separator.

Test classes:

    RegexNode_Test        epsilon-NFA construction rules
    RegexParser_Test      parsing, precedence, and every error case
    EpsilonNfa_Test       state layout, epsilon closure, transition table
    NfaSimulator_Test     acceptance, stepping, resetting between lines
    RegexEngine_Test      end to end, both output modes, exit status


SOURCE FILES

    RegexEngine.java            command line entry point, both output modes
    RegexParser.java            recursive descent parser
    RegexNode.java              syntax tree, and each node's construction rule
    RegexSyntaxException.java   invalid expression reporting, with position
    EpsilonNfa.java             the machine, epsilon closure, transition table
    NfaSimulator.java           active state set, stepping, acceptance
    State.java                  one state and its outgoing edges
    NfaBuilder.java             supplies fresh states during construction

