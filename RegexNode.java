import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A node in the parsed form of a regular expression, and the rule for building
 * that node into an &epsilon;-NFA.
 *
 * <p>Construction is <em>threaded</em>: {@link #build} is handed the state the
 * machine is in when this node starts matching, and returns the state it is in
 * once this node has matched. Concatenation is then just "feed the exit of one
 * node in as the entry of the next", which costs no &epsilon; transitions at
 * all -- the first node's exit state <em>is</em> the second node's entry state.
 *
 * <p>Putting the rule on the node rather than in a big {@code instanceof} chain
 * elsewhere keeps each construction next to the shape it builds, and means
 * adding an operator later touches one class instead of a switch statement.
 */
public abstract class RegexNode {

  /**
   * Builds this node into the machine under construction.
   *
   * @param builder source of fresh states
   * @param entry the state the machine is in before this node matches
   * @return the state the machine is in after this node has matched
   */
  public abstract State build(NfaBuilder builder, State entry);

  /** A single literal character. */
  public static final class Literal extends RegexNode {

    private final char symbol;

    public Literal(char symbol) {
      this.symbol = symbol;
    }

    public char getSymbol() {
      return symbol;
    }

    @Override
    public State build(NfaBuilder builder, State entry) {
      State exit = builder.newState();
      entry.addTransition(symbol, exit);
      return exit;
    }

    @Override
    public String toString() {
      return "Lit(" + symbol + ")";
    }
  }

  /** Two or more nodes that must match one after another. */
  public static final class Concat extends RegexNode {

    private final List<RegexNode> children;

    public Concat(List<RegexNode> children) {
      this.children = Collections.unmodifiableList(new ArrayList<>(children));
    }

    public List<RegexNode> getChildren() {
      return children;
    }

    @Override
    public State build(NfaBuilder builder, State entry) {
      // No new states and no epsilon edges: each child simply starts where the
      // previous one finished.
      State current = entry;
      for (RegexNode child : children) {
        current = child.build(builder, current);
      }
      return current;
    }

    @Override
    public String toString() {
      return join("Concat", children);
    }
  }

  /** Two or more alternatives, any one of which may match. */
  public static final class Alternation extends RegexNode {

    private final List<RegexNode> branches;

    public Alternation(List<RegexNode> branches) {
      this.branches = Collections.unmodifiableList(new ArrayList<>(branches));
    }

    public List<RegexNode> getBranches() {
      return branches;
    }

    @Override
    public State build(NfaBuilder builder, State entry) {
      // Each branch gets its own fresh entry state, so a branch that happens to
      // begin with a loop cannot accidentally use the shared entry as its loop
      // head and swallow the other branches.
      List<State> branchExits = new ArrayList<>(branches.size());
      for (RegexNode branch : branches) {
        State branchEntry = builder.newState();
        entry.addEpsilonTransition(branchEntry);
        branchExits.add(branch.build(builder, branchEntry));
      }
      State exit = builder.newState();
      for (State branchExit : branchExits) {
        branchExit.addEpsilonTransition(exit);
      }
      return exit;
    }

    @Override
    public String toString() {
      return join("Alt", branches);
    }
  }

  /** Zero or more repetitions of a node. */
  public static final class Star extends RegexNode {

    private final RegexNode body;

    public Star(RegexNode body) {
      this.body = body;
    }

    public RegexNode getBody() {
      return body;
    }

    @Override
    public State build(NfaBuilder builder, State entry) {
      // `entry` doubles as the loop head. The body loops back to it, and the
      // way out also leaves from it -- which is exactly what allows zero
      // iterations: you can reach the exit without ever entering the body.
      //
      // Reusing `entry` this way is safe because every exit state handed out by
      // this class is freshly created, so nothing built later can already be
      // pointing into the middle of this loop.
      State bodyExit = body.build(builder, entry);
      bodyExit.addEpsilonTransition(entry);
      State exit = builder.newState();
      entry.addEpsilonTransition(exit);
      return exit;
    }

    @Override
    public String toString() {
      return "Star(" + body + ")";
    }
  }

  /** One or more repetitions of a node. */
  public static final class Plus extends RegexNode {

    private final RegexNode body;

    public Plus(RegexNode body) {
      this.body = body;
    }

    public RegexNode getBody() {
      return body;
    }

    @Override
    public State build(NfaBuilder builder, State entry) {
      // The same loop as Star, with one edge moved: the way out leaves from the
      // body's exit instead of from the loop head. You therefore cannot reach
      // the exit without having matched the body at least once.
      State bodyExit = body.build(builder, entry);
      bodyExit.addEpsilonTransition(entry);
      State exit = builder.newState();
      bodyExit.addEpsilonTransition(exit);
      return exit;
    }

    @Override
    public String toString() {
      return "Plus(" + body + ")";
    }
  }

  /**
   * Renders a node with several children as {@code Name(a,b,c)}.
   *
   * <p>Used only by {@code toString}, which exists so that parser tests can
   * assert on the shape of the tree -- checking precedence by comparing one
   * string is far more readable than walking the tree with casts.
   *
   * @param name the node's label
   * @param children the child nodes
   * @return the rendered form
   */
  private static String join(String name, List<RegexNode> children) {
    StringBuilder out = new StringBuilder(name).append('(');
    for (int i = 0; i < children.size(); i++) {
      if (i > 0) {
        out.append(',');
      }
      out.append(children.get(i));
    }
    return out.append(')').toString();
  }
}
