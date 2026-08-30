import java.util.ArrayList;
import java.util.List;

/**
 * Turns the text of a regular expression into a {@link RegexNode} tree.
 *
 * <p>Recursive descent, one method per precedence level. The order in which the
 * methods call each other <em>is</em> the precedence table: {@code alternation}
 * calls {@code concat} calls {@code repeat} calls {@code atom}, so the
 * loosest-binding operator sits at the top and the tightest at the bottom.
 * There is no precedence table anywhere in this class because none is needed.
 *
 * <p>The language is small enough that there is no separate tokeniser. Every
 * token is exactly one character, so a tokeniser would only copy the string
 * into an array of one-character tokens and add a layer to step through.
 */
public final class RegexParser {

  /** The regex being read. */
  private final String source;

  /** Index of the next character to look at. */
  private int position;

  private RegexParser(String source) {
    this.source = source;
    this.position = 0;
  }

  /**
   * Parses a regular expression.
   *
   * @param regex the expression text
   * @return the root of the parsed tree
   * @throws RegexSyntaxException if the expression is not valid
   */
  public static RegexNode parse(String regex) throws RegexSyntaxException {
    if (regex == null) {
      throw new RegexSyntaxException("no regular expression was supplied");
    }
    RegexParser parser = new RegexParser(regex);
    RegexNode tree = parser.alternation(false);
    if (!parser.atEnd()) {
      // alternation() only stops early on a ')', and at the top level there is
      // no '(' for it to close.
      throw new RegexSyntaxException("unmatched ')'", parser.position);
    }
    return tree;
  }

  /**
   * One or more concatenations separated by {@code |}.
   *
   * @param inBrackets whether we are already inside a bracketed group
   */
  private RegexNode alternation(boolean inBrackets) throws RegexSyntaxException {
    List<RegexNode> branches = new ArrayList<>();
    branches.add(concat(inBrackets));
    while (peekIs('|')) {
      position++;
      branches.add(concat(inBrackets));
    }
    return branches.size() == 1 ? branches.get(0) : new RegexNode.Alternation(branches);
  }

  /**
   * A run of repeat-expressions, ended by {@code |}, {@code )} or end of input.
   *
   * <p>The empty check here is doing three jobs at once: it is what rejects
   * {@code a|}, {@code |a} and {@code ()}.
   */
  private RegexNode concat(boolean inBrackets) throws RegexSyntaxException {
    List<RegexNode> items = new ArrayList<>();
    while (!atEnd() && !peekIs('|') && !peekIs(')')) {
      items.add(repeat(inBrackets));
    }
    if (items.isEmpty()) {
      throw new RegexSyntaxException(describeEmptySequence(), position);
    }
    return items.size() == 1 ? items.get(0) : new RegexNode.Concat(items);
  }

  /**
   * An atom followed by any number of postfix operators.
   *
   * <p>The loop is what makes chained operators work: {@code a*+} wraps once
   * for {@code *} and again for {@code +}, giving {@code Plus(Star(Lit(a)))}.
   */
  private RegexNode repeat(boolean inBrackets) throws RegexSyntaxException {
    RegexNode node = atom(inBrackets);
    while (peekIs('*') || peekIs('+')) {
      char operator = source.charAt(position);
      position++;
      node = operator == '*' ? new RegexNode.Star(node) : new RegexNode.Plus(node);
    }
    return node;
  }

  /**
   * A single literal, or a bracketed group.
   *
   * <p>Only called when at least one character remains, which {@code concat}
   * guarantees.
   */
  private RegexNode atom(boolean inBrackets) throws RegexSyntaxException {
    char next = source.charAt(position);

    if (next == '(') {
      if (inBrackets) {
        throw new RegexSyntaxException("nested brackets are not supported", position);
      }
      position++;
      RegexNode inner = alternation(true);
      if (!peekIs(')')) {
        throw new RegexSyntaxException("unclosed '('", position);
      }
      position++;
      return inner;
    }
    if (next == '*' || next == '+') {
      throw new RegexSyntaxException("'" + next + "' has nothing to repeat", position);
    }
    if (!isLiteral(next)) {
      throw new RegexSyntaxException(
          "'" + next + "' is not allowed in a regular expression", position);
    }
    position++;
    return new RegexNode.Literal(next);
  }

  /**
   * Works out which of the empty-sequence faults we have hit, so the message
   * names the actual problem instead of saying "empty expression" three
   * different times.
   */
  private String describeEmptySequence() {
    if (source.isEmpty()) {
      return "the regular expression is empty";
    }
    if (peekIs('|') || (position > 0 && source.charAt(position - 1) == '|')) {
      return "'|' must have an expression on both sides";
    }
    return "empty group '()'";
  }

  /**
   * Whether a character may appear as a literal.
   *
   * <p>Spelled out as ranges rather than using {@code Character.isLetterOrDigit},
   * which would also accept letters and digits from other scripts. The
   * assignment allows exactly these.
   */
  private static boolean isLiteral(char candidate) {
    return (candidate >= 'a' && candidate <= 'z')
        || (candidate >= 'A' && candidate <= 'Z')
        || (candidate >= '0' && candidate <= '9')
        || candidate == ' ';
  }

  private boolean atEnd() {
    return position >= source.length();
  }

  private boolean peekIs(char expected) {
    return !atEnd() && source.charAt(position) == expected;
  }
}
