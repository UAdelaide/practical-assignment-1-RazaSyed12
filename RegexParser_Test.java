import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Tests for {@link RegexParser}.
 *
 * <p>Shape is checked by comparing {@code toString} of the parsed tree against
 * an expected string. Precedence bugs show up immediately as a differently
 * shaped tree, and one string comparison reads far better than walking the tree
 * with casts.
 */
public class RegexParser_Test {

  /** Parses and renders, so each test is one readable line. */
  private static String shapeOf(String regex) throws RegexSyntaxException {
    return RegexParser.parse(regex).toString();
  }

  /** Asserts the regex is rejected, and hands back the failure to inspect. */
  private static RegexSyntaxException rejectionOf(String regex) {
    try {
      RegexParser.parse(regex);
    } catch (RegexSyntaxException expected) {
      return expected;
    }
    fail("expected <" + regex + "> to be rejected");
    return null;
  }

  private static void assertRejected(String regex, String expectedFragment) {
    RegexSyntaxException failure = rejectionOf(regex);
    assertTrue(
        "message was: " + failure.getMessage(),
        failure.getMessage().contains(expectedFragment));
  }

  @Test
  public void singleLiteral() throws Exception {
    assertEquals("Lit(a)", shapeOf("a"));
  }

  @Test
  public void concatenationOfLiterals() throws Exception {
    assertEquals("Concat(Lit(a),Lit(b),Lit(c))", shapeOf("abc"));
  }

  @Test
  public void postfixBindsTighterThanConcatenation() throws Exception {
    assertEquals("Concat(Lit(a),Star(Lit(b)))", shapeOf("ab*"));
    assertEquals("Concat(Lit(a),Plus(Lit(b)))", shapeOf("ab+"));
  }

  @Test
  public void alternationBindsLoosestOfAll() throws Exception {
    assertEquals("Alt(Concat(Lit(a),Lit(b)),Concat(Lit(c),Lit(d)))", shapeOf("ab|cd"));
  }

  @Test
  public void alternationAndPostfixTogether() throws Exception {
    assertEquals("Alt(Concat(Lit(a),Star(Lit(b))),Lit(c))", shapeOf("ab*|c"));
  }

  @Test
  public void severalBranchesCollapseIntoOneAlternation() throws Exception {
    assertEquals("Alt(Lit(a),Lit(b),Lit(c))", shapeOf("a|b|c"));
  }

  @Test
  public void bracketsRegroupWhatThePostfixAppliesTo() throws Exception {
    assertEquals("Star(Concat(Lit(a),Lit(b)))", shapeOf("(ab)*"));
    assertEquals("Concat(Lit(a),Star(Lit(b)))", shapeOf("ab*"));
  }

  @Test
  public void bracketsContainTheirOwnAlternation() throws Exception {
    assertEquals("Concat(Alt(Lit(a),Lit(b)),Lit(c))", shapeOf("(a|b)c"));
  }

  @Test
  public void redundantBracketsAreHarmless() throws Exception {
    assertEquals("Lit(a)", shapeOf("(a)"));
  }

  @Test
  public void chainedPostfixOperatorsStack() throws Exception {
    assertEquals("Star(Star(Lit(a)))", shapeOf("a**"));
    assertEquals("Plus(Star(Lit(a)))", shapeOf("a*+"));
    assertEquals("Star(Plus(Lit(a)))", shapeOf("a+*"));
    assertEquals("Plus(Star(Concat(Lit(a),Lit(b))))", shapeOf("(ab)*+"));
  }

  @Test
  public void lettersDigitsAndSpacesAreAllLiterals() throws Exception {
    assertEquals("Concat(Lit(A),Lit(1),Lit( ),Lit(z))", shapeOf("A1 z"));
  }

  /** The worked example from the assignment brief. */
  @Test
  public void bracketedExampleFromTheBrief() throws Exception {
    assertEquals(
        "Alt(Star(Concat(Lit(a),Lit(b))),Plus(Lit(c)))", shapeOf("(ab)*|c+"));
  }

  @Test
  public void emptyRegexIsRejected() {
    assertRejected("", "empty");
  }

  @Test
  public void nullRegexIsRejected() {
    RegexSyntaxException failure = rejectionOf(null);
    assertTrue(failure.getMessage().contains("no regular expression"));
  }

  @Test
  public void unclosedBracketIsRejected() {
    assertRejected("a(b", "unclosed");
  }

  @Test
  public void unmatchedClosingBracketIsRejected() {
    assertRejected("a)b", "unmatched");
  }

  @Test
  public void nestedBracketsAreRejected() {
    assertRejected("((a))", "nested");
    assertRejected("(a(b)c)", "nested");
  }

  @Test
  public void emptyGroupIsRejected() {
    assertRejected("()", "empty group");
  }

  @Test
  public void postfixWithNothingToRepeatIsRejected() {
    assertRejected("*a", "nothing to repeat");
    assertRejected("+a", "nothing to repeat");
    assertRejected("a|*b", "nothing to repeat");
    assertRejected("(*a)", "nothing to repeat");
  }

  @Test
  public void emptyAlternationBranchIsRejected() {
    assertRejected("a|", "both sides");
    assertRejected("|a", "both sides");
    assertRejected("a||b", "both sides");
    assertRejected("(a|)", "both sides");
  }

  @Test
  public void charactersOutsideTheAlphabetAreRejected() {
    assertRejected("a?b", "not allowed");
    assertRejected("a.b", "not allowed");
    assertRejected("a[b", "not allowed");
  }

  @Test
  public void failuresCarryThePositionOfTheOffendingCharacter() {
    assertEquals(1, rejectionOf("a?b").getPosition());
    assertEquals(0, rejectionOf("*a").getPosition());
    assertEquals(2, rejectionOf("(a(b)c)").getPosition());
  }
}
