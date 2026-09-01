import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/** Tests for {@link NfaSimulator}: acceptance, stepping, and resetting. */
public class NfaSimulator_Test {

  private static NfaSimulator simulatorFor(String regex) throws RegexSyntaxException {
    return new NfaSimulator(EpsilonNfa.build(RegexParser.parse(regex)));
  }

  private static void assertMatches(String regex, String input) throws Exception {
    assertTrue(
        "<" + regex + "> should match <" + input + ">", simulatorFor(regex).matches(input));
  }

  private static void assertRejects(String regex, String input) throws Exception {
    assertFalse(
        "<" + regex + "> should not match <" + input + ">",
        simulatorFor(regex).matches(input));
  }

  /**
   * The verdict before any input, then one after each character -- the sequence
   * verbose mode has to print.
   */
  private static List<Boolean> verdictsWhileReading(String regex, String input)
      throws RegexSyntaxException {
    NfaSimulator simulator = simulatorFor(regex);
    List<Boolean> verdicts = new ArrayList<>();
    verdicts.add(simulator.isAccepting());
    for (int index = 0; index < input.length(); index++) {
      simulator.consume(input.charAt(index));
      verdicts.add(simulator.isAccepting());
    }
    return verdicts;
  }

  @Test
  public void singleLiteral() throws Exception {
    assertMatches("a", "a");
    assertRejects("a", "");
    assertRejects("a", "b");
    assertRejects("a", "aa");
  }

  @Test
  public void concatenation() throws Exception {
    assertMatches("abc", "abc");
    assertRejects("abc", "ab");
    assertRejects("abc", "abcd");
  }

  @Test
  public void alternationTakesEitherBranch() throws Exception {
    assertMatches("cat|dog", "cat");
    assertMatches("cat|dog", "dog");
    assertRejects("cat|dog", "cog");
  }

  @Test
  public void alternationBranchesMayBeDifferentLengths() throws Exception {
    assertMatches("a|bcd", "a");
    assertMatches("a|bcd", "bcd");
    assertRejects("a|bcd", "ab");
  }

  @Test
  public void starAcceptsZeroOrMore() throws Exception {
    assertMatches("a*", "");
    assertMatches("a*", "a");
    assertMatches("a*", "aaaa");
    assertRejects("a*", "b");
  }

  @Test
  public void plusNeedsAtLeastOne() throws Exception {
    assertRejects("a+", "");
    assertMatches("a+", "a");
    assertMatches("a+", "aaaa");
  }

  @Test
  public void postfixAppliesToTheLastAtomOnly() throws Exception {
    assertMatches("ab*", "a");
    assertMatches("ab*", "abbb");
    assertRejects("ab*", "abab");
  }

  @Test
  public void bracketsMakeThePostfixApplyToTheWholeGroup() throws Exception {
    assertMatches("(ab)*", "");
    assertMatches("(ab)*", "abab");
    assertRejects("(ab)*", "aba");
  }

  @Test
  public void chainedOperators() throws Exception {
    assertMatches("a**", "");
    assertMatches("a**", "aaa");
    assertMatches("a*+", "");
    assertMatches("a*+", "aaa");
    assertRejects("a+*", "b");
    assertMatches("a+*", "");
  }

  @Test
  public void matchMustBeExactNotMerelyContained() throws Exception {
    assertRejects("ab", "xaby");
    assertRejects("ab", "abc");
    assertRejects("ab", "zab");
    assertMatches("ab", "ab");
  }

  @Test
  public void literalsMayBeDigitsUppercaseOrSpaces() throws Exception {
    assertMatches("A1 z", "A1 z");
    assertRejects("A1 z", "a1 z");
    assertMatches("a b*", "a ");
  }

  @Test
  public void inputCharactersOutsideTheAlphabetKillTheMatch() throws Exception {
    assertRejects("a*", "a?a");
    assertRejects("a*", "\t");
  }

  @Test
  public void theBriefsExample() throws Exception {
    assertMatches("(ab)*|c+", "");
    assertMatches("(ab)*|c+", "ab");
    assertMatches("(ab)*|c+", "abab");
    assertMatches("(ab)*|c+", "c");
    assertMatches("(ab)*|c+", "ccc");
    assertRejects("(ab)*|c+", "abc");
    assertRejects("(ab)*|c+", "a");
  }

  @Test
  public void onceDeadTheMachineStaysDead() throws Exception {
    NfaSimulator simulator = simulatorFor("a*");

    simulator.consume('z');
    assertTrue("no state survives an unknown character", simulator.isDead());

    simulator.consume('a');
    assertTrue("a later valid character must not revive it", simulator.isDead());
    assertFalse(simulator.isAccepting());
  }

  @Test
  public void resetClearsProgressFromThePreviousLine() throws Exception {
    NfaSimulator simulator = simulatorFor("ab");

    simulator.consume('a');
    simulator.reset();
    simulator.consume('a');
    simulator.consume('b');

    assertTrue("the leftover 'a' must not have counted", simulator.isAccepting());
  }

  @Test
  public void reusingOneSimulatorGivesTheSameAnswersEveryTime() throws Exception {
    NfaSimulator simulator = simulatorFor("(ab)*|c+");

    assertTrue(simulator.matches("abab"));
    assertFalse(simulator.matches("abc"));
    assertTrue(simulator.matches("ccc"));
    assertTrue(simulator.matches(""));
    assertTrue(simulator.matches("abab"));
  }

  @Test
  public void steppingReportsAVerdictBeforeInputAndAfterEachCharacter()
      throws Exception {
    // The sequence in the brief's verbose example: initial state, then one
    // verdict per character of "abc".
    assertEquals(
        java.util.Arrays.asList(true, false, true, false),
        verdictsWhileReading("(ab)*|c+", "abc"));
  }

  @Test
  public void steppingOnAnEmptyLineReportsOnlyTheInitialVerdict() throws Exception {
    assertEquals(java.util.Arrays.asList(true), verdictsWhileReading("(ab)*|c+", ""));
    assertEquals(java.util.Arrays.asList(false), verdictsWhileReading("c+", ""));
  }

  @Test
  public void lastVerdictWhileSteppingEqualsTheWholeStringVerdict() throws Exception {
    List<Boolean> verdicts = verdictsWhileReading("(ab)*|c+", "ccc");

    assertEquals(4, verdicts.size());
    assertEquals(simulatorFor("(ab)*|c+").matches("ccc"), verdicts.get(verdicts.size() - 1));
  }
}
