/**
 * Thrown when the regular expression read from standard input cannot be parsed.
 *
 * <p>Checked rather than unchecked on purpose: a bad regex is an expected input,
 * not a programming fault, and the assignment requires it to be reported and
 * turned into exit status 1. Making it checked forces the caller to decide what
 * to do about it.
 */
public class RegexSyntaxException extends Exception {

  private static final long serialVersionUID = 1L;

  /** Index into the regex where the fault was found, or -1 if not positional. */
  private final int position;

  /**
   * Creates an exception for a fault that is not tied to one character.
   *
   * @param reason human-readable description of the problem
   */
  public RegexSyntaxException(String reason) {
    this(reason, -1);
  }

  /**
   * Creates an exception for a fault at a known offset.
   *
   * @param reason human-readable description of the problem
   * @param position zero-based index into the regex, or -1 if not positional
   */
  public RegexSyntaxException(String reason, int position) {
    super(position < 0 ? reason : reason + " (at position " + position + ")");
    this.position = position;
  }

  /**
   * Returns where in the expression the fault was found.
   *
   * @return the zero-based offset, or -1 if the fault has no single location
   */
  public int getPosition() {
    return position;
  }
}
