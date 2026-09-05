import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Command line entry point for the regular expression engine.
 *
 * <p>Reads a regular expression from the first line of standard input, then
 * reads input strings one line at a time and reports whether each one matches
 * the expression exactly. Runs until end of input.
 *
 * <pre>
 *   javac RegexEngine.java
 *   java RegexEngine
 *   java RegexEngine -v
 * </pre>
 *
 * <p>In normal mode each input line produces one verdict. In verbose mode the
 * transition table is printed first, and each input line produces a verdict for
 * the machine's starting position followed by one after every character read --
 * so a line of {@code n} characters produces {@code n + 1} lines of output, the
 * last of which is what normal mode would have printed on its own.
 *
 * <p>No part of this engine uses {@code java.util.regex}; matching is done with
 * the &epsilon;-NFA built by {@link EpsilonNfa}.
 */
public final class RegexEngine {

  /** Printed once the expression has been accepted and the machine is built. */
  private static final String READY = "ready";

  /** Switches on the transition table and per-character reporting. */
  private static final String VERBOSE_FLAG = "-v";

  private RegexEngine() {
    // Entry point only.
  }

  /**
   * Runs the engine against the real standard input and output.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    System.exit(run(args, System.in, System.out, System.err));
  }

  /**
   * Runs the engine against the given streams.
   *
   * <p>Split out from {@link #main} so that tests can drive it with strings and
   * inspect what it wrote, instead of having to start a process and capture the
   * console. It returns the exit status rather than calling
   * {@code System.exit}, for the same reason.
   *
   * @param args command line arguments
   * @param input stream to read the expression and the input strings from
   * @param output stream for the table, {@code ready} and the match verdicts
   * @param errors stream for error messages
   * @return the process exit status: 0 normally, 1 if the expression is invalid
   */
  static int run(String[] args, InputStream input, PrintStream output, PrintStream errors) {
    boolean verbose = isVerbose(args);

    BufferedReader reader =
        new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

    EpsilonNfa machine;
    try {
      // readLine returns null at end of input, which parse() reports as a
      // missing expression rather than throwing NullPointerException.
      machine = EpsilonNfa.build(RegexParser.parse(reader.readLine()));
    } catch (RegexSyntaxException invalid) {
      errors.println("error: " + invalid.getMessage());
      errors.flush();
      return 1;
    } catch (IOException unreadable) {
      errors.println("error: could not read the regular expression");
      errors.flush();
      return 1;
    }

    if (verbose) {
      output.println(machine.transitionTable());
      output.println();
    }
    output.println(READY);
    output.flush();

    NfaSimulator simulator = new NfaSimulator(machine);
    try {
      String line = reader.readLine();
      while (line != null) {
        // Each line is matched from a clean start, so nothing carries over from
        // the line before it.
        if (verbose) {
          reportWhileReading(simulator, line, output);
        } else {
          output.println(simulator.matches(line));
        }
        // Flushed per line because the user is typing these interactively and
        // expects the verdict before entering the next string.
        output.flush();
        line = reader.readLine();
      }
    } catch (IOException unreadable) {
      errors.println("error: could not read input");
      errors.flush();
      return 1;
    }

    return 0;
  }

  /**
   * Reports the machine's verdict as each character of a line is consumed.
   *
   * <p>The first verdict is printed before any character is read, which is what
   * reports an expression that matches the empty string as {@code true} on a
   * blank line.
   *
   * @param simulator the machine to step, reset before use
   * @param line the input string being read
   * @param output where the verdicts are written
   */
  private static void reportWhileReading(
      NfaSimulator simulator, String line, PrintStream output) {
    simulator.reset();
    output.println(simulator.isAccepting());
    for (int index = 0; index < line.length(); index++) {
      simulator.consume(line.charAt(index));
      output.println(simulator.isAccepting());
      // Flushed per character, since verbose mode exists to be watched as the
      // input is typed.
      output.flush();
    }
  }

  /**
   * @param args command line arguments
   * @return whether verbose output was requested
   */
  private static boolean isVerbose(String[] args) {
    for (String argument : args) {
      if (VERBOSE_FLAG.equals(argument)) {
        return true;
      }
    }
    return false;
  }
}
