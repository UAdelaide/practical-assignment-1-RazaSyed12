import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

/**
 * End-to-end tests for {@link RegexEngine}.
 *
 * <p>These drive {@code run} with a whole session's worth of input and check
 * everything it writes, which is the behaviour the assignment actually
 * specifies. The layers underneath are covered by their own tests.
 */
public class RegexEngine_Test {

  /** What one run of the engine produced. */
  private static final class Session {

    final int status;
    final List<String> output;
    final String errors;

    Session(int status, List<String> output, String errors) {
      this.status = status;
      this.output = output;
      this.errors = errors;
    }
  }

  /**
   * Runs the engine over the given standard input.
   *
   * @param stdin the complete input, expression line included
   */
  private static Session run(String stdin, String... args) throws IOException {
    ByteArrayInputStream input =
        new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
    ByteArrayOutputStream errorBytes = new ByteArrayOutputStream();

    int status;
    try (PrintStream output = newStream(outputBytes);
        PrintStream errors = newStream(errorBytes)) {
      status = RegexEngine.run(args, input, output, errors);
    }

    return new Session(
        status,
        linesOf(outputBytes.toString("UTF-8")),
        errorBytes.toString("UTF-8"));
  }

  private static PrintStream newStream(ByteArrayOutputStream sink)
      throws UnsupportedEncodingException {
    return new PrintStream(sink, true, "UTF-8");
  }

  /**
   * Splits captured output into lines.
   *
   * <p>Done with a reader rather than {@code String.split}, which would pull in
   * {@code java.util.regex} -- the one library this assignment forbids.
   */
  private static List<String> linesOf(String text) throws IOException {
    List<String> lines = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
      String line = reader.readLine();
      while (line != null) {
        lines.add(line);
        line = reader.readLine();
      }
    }
    return lines;
  }

  /** Builds a stdin string: an expression line followed by input lines. */
  private static String session(String regex, String... inputs) {
    StringBuilder stdin = new StringBuilder(regex).append('\n');
    for (String input : inputs) {
      stdin.append(input).append('\n');
    }
    return stdin.toString();
  }

  @Test
  public void printsReadyBeforeAnyVerdict() throws Exception {
    Session result = run(session("abc"));

    assertEquals(Arrays.asList("ready"), result.output);
    assertEquals(0, result.status);
    assertEquals("", result.errors);
  }

  /** The worked example from the assignment brief. */
  @Test
  public void exampleFromTheBrief() throws Exception {
    Session result = run(session("(ab)*|c+", "abc", "ccc"));

    assertEquals(Arrays.asList("ready", "false", "true"), result.output);
    assertEquals(0, result.status);
  }

  @Test
  public void oneVerdictPerInputLine() throws Exception {
    Session result = run(session("a+", "a", "aaa", "", "b"));

    assertEquals(Arrays.asList("ready", "true", "true", "false", "false"), result.output);
  }

  @Test
  public void aBlankLineIsAnInputStringNotATerminator() throws Exception {
    Session result = run(session("a*", "", "a", ""));

    assertEquals(
        "the empty string matches a*, and input continues afterwards",
        Arrays.asList("ready", "true", "true", "true"),
        result.output);
  }

  @Test
  public void matchesMustBeExactNotPartial() throws Exception {
    Session result = run(session("ab", "ab", "abc", "xab", "xaby"));

    assertEquals(Arrays.asList("ready", "true", "false", "false", "false"), result.output);
  }

  @Test
  public void eachLineIsEvaluatedIndependently() throws Exception {
    // "ab" then "ab" must both pass; if state leaked between lines the second
    // would be read as a continuation and fail.
    Session result = run(session("ab", "ab", "ab", "ab"));

    assertEquals(Arrays.asList("ready", "true", "true", "true"), result.output);
  }

  @Test
  public void inputStringsMayContainSpacesAndTabs() throws Exception {
    Session result = run(session("a b", "a b", "a\tb"));

    assertEquals(
        "a tab is not in the expression's alphabet, so it cannot match",
        Arrays.asList("ready", "true", "false"),
        result.output);
  }

  @Test
  public void handlesTheLastLineWhenInputEndsWithoutANewline() throws Exception {
    Session result = run("a+\naaa");

    assertEquals(
        "the final line must still be evaluated",
        Arrays.asList("ready", "true"),
        result.output);
  }

  @Test
  public void handlesWindowsLineEndings() throws Exception {
    Session result = run("a+\r\na\r\naa\r\n");

    assertEquals(
        "a trailing carriage return must not be treated as part of the string",
        Arrays.asList("ready", "true", "true"),
        result.output);
  }

  @Test
  public void noInputStringsAtAllIsACleanExit() throws Exception {
    Session result = run("abc\n");

    assertEquals(Arrays.asList("ready"), result.output);
    assertEquals(0, result.status);
  }

  @Test
  public void invalidExpressionExitsWithStatusOne() throws Exception {
    Session result = run(session("a(b", "ab"));

    assertEquals(1, result.status);
    assertTrue("should say what was wrong", result.errors.contains("unclosed"));
    assertEquals("must not report itself ready", Arrays.asList(), result.output);
  }

  @Test
  public void endOfInputBeforeAnyExpressionExitsWithStatusOne() throws Exception {
    // Nothing at all on standard input: readLine gives null straight away.
    Session result = run("");

    assertEquals(1, result.status);
    assertTrue(
        "message was: " + result.errors,
        result.errors.contains("no regular expression"));
  }

  @Test
  public void blankExpressionLineExitsWithStatusOne() throws Exception {
    // A first line that exists but is empty is a different fault from no input
    // at all, and says so.
    Session result = run("\nabc\n");

    assertEquals(1, result.status);
    assertTrue("message was: " + result.errors, result.errors.contains("empty"));
  }

  @Test
  public void expressionsUsingEveryAllowedCharacterClass() throws Exception {
    Session result = run(session("(A1 z)+", "A1 z", "A1 zA1 z", "a1 z"));

    assertEquals(Arrays.asList("ready", "true", "true", "false"), result.output);
  }

  @Test
  public void normalModePrintsNoTable() throws Exception {
    Session result = run(session("(ab)*|c+", "ab"));

    assertEquals(Arrays.asList("ready", "true"), result.output);
  }

  @Test
  public void verboseModePrintsTheTableBeforeReady() throws Exception {
    Session result = run(session("a"), "-v");

    assertEquals(
        Arrays.asList(
            "     epsilon  a   other",
            ">q0           q1",
            "*q1",
            "",
            "ready"),
        result.output);
  }

  /** The verbose worked example from the assignment brief. */
  @Test
  public void verboseModeReportsAVerdictPerCharacter() throws Exception {
    Session result = run(session("(ab)*|c+", "abc", "ccc"), "-v");

    // Everything from "ready" onwards; the table itself is checked above and in
    // EpsilonNfa_Test.
    List<String> verdicts = result.output.subList(result.output.indexOf("ready") + 1,
        result.output.size());

    assertEquals(
        Arrays.asList(
            // "abc": start, then a, b, c
            "true", "false", "true", "false",
            // "ccc": start, then c, c, c
            "true", "true", "true", "true"),
        verdicts);
  }

  @Test
  public void verboseModeReportsOnlyTheStartVerdictForABlankLine() throws Exception {
    Session result = run(session("(ab)*|c+", ""), "-v");

    assertEquals(
        "an empty line consumes no characters, so there is one verdict",
        "true",
        result.output.get(result.output.size() - 1));
  }

  @Test
  public void verboseModesLastVerdictMatchesNormalMode() throws Exception {
    for (String input : new String[] {"", "ab", "abab", "abc", "ccc", "a"}) {
      Session plain = run(session("(ab)*|c+", input));
      Session verbose = run(session("(ab)*|c+", input), "-v");

      assertEquals(
          "the final verbose verdict for <" + input + "> must agree with normal mode",
          plain.output.get(plain.output.size() - 1),
          verbose.output.get(verbose.output.size() - 1));
    }
  }

  @Test
  public void verboseModeStillExitsWithStatusOneOnAnInvalidExpression()
      throws Exception {
    Session result = run(session("a(b", "ab"), "-v");

    assertEquals(1, result.status);
    assertEquals("no table for a machine that was never built", Arrays.asList(),
        result.output);
  }
}
