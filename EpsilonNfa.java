import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeSet;

/**
 * An &epsilon;-NFA built from a parsed regular expression.
 *
 * <p>The machine owns its states and hands out new ones during construction,
 * which is why it is its own {@link NfaBuilder}. Once
 * {@link #build(RegexNode)} returns, nothing about the machine changes; all the
 * state that varies while matching lives in {@link NfaSimulator} instead.
 *
 * <p>There is exactly one start state and exactly one accepting state, which is
 * what the threaded construction in {@link RegexNode} guarantees.
 */
public final class EpsilonNfa implements NfaBuilder {

  /** Every state, indexed by id. */
  private final List<State> states = new ArrayList<>();

  private final State start;

  private final State accept;

  private EpsilonNfa(RegexNode tree) {
    this.start = newState();
    this.accept = tree.build(this, start);
  }

  /**
   * Builds the machine for a parsed expression.
   *
   * @param tree the root of the parsed expression
   * @return a machine that accepts exactly the strings the expression describes
   */
  public static EpsilonNfa build(RegexNode tree) {
    return new EpsilonNfa(tree);
  }

  @Override
  public State newState() {
    State created = new State(states.size());
    states.add(created);
    return created;
  }

  public State getStart() {
    return start;
  }

  public State getAccept() {
    return accept;
  }

  /**
   * Returns every state in the machine.
   *
   * @return the states, in the order they were created
   */
  public List<State> getStates() {
    return Collections.unmodifiableList(states);
  }

  /**
   * Every character the machine has an edge for, in sorted order.
   *
   * <p>Sorted rather than insertion-ordered so the transition table columns come
   * out the same way whichever order the expression happened to mention them.
   *
   * @return the machine's alphabet
   */
  public SortedSet<Character> getAlphabet() {
    SortedSet<Character> alphabet = new TreeSet<>();
    for (State state : states) {
      alphabet.addAll(state.outgoingSymbols());
    }
    return alphabet;
  }

  /**
   * Expands a set of states to include everything reachable without consuming
   * input.
   *
   * <p>Worklist rather than recursion. The {@code add} return value is what
   * stops the loop on a cyclic machine -- an expression like {@code a**} has
   * &epsilon; edges that form a cycle, and revisiting a state already in the
   * closure would spin forever.
   *
   * @param from the states to expand
   * @return {@code from} plus everything &epsilon;-reachable from it
   */
  public Set<State> epsilonClosure(Collection<State> from) {
    Set<State> closure = new LinkedHashSet<>(from);
    Deque<State> pending = new ArrayDeque<>(from);
    while (!pending.isEmpty()) {
      State current = pending.removeFirst();
      for (State target : current.getEpsilonTransitions()) {
        if (closure.add(target)) {
          pending.addLast(target);
        }
      }
    }
    return closure;
  }

  /**
   * Renders the transition table shown in verbose mode.
   *
   * <p>Columns are the &epsilon; edges, then one per character in the alphabet,
   * then {@code other} -- which is always blank, and is there to make explicit
   * that a character the expression never mentions leads nowhere. Rows are
   * marked {@code >} for the start state and {@code *} for the accepting one.
   *
   * @return the table, one line per state, no trailing whitespace
   */
  public String transitionTable() {
    List<Character> alphabet = new ArrayList<>(getAlphabet());

    List<String> headers = new ArrayList<>();
    headers.add("");
    headers.add("epsilon");
    for (char symbol : alphabet) {
      headers.add(columnLabel(symbol));
    }
    headers.add("other");

    List<List<String>> rows = new ArrayList<>();
    for (State state : states) {
      List<String> row = new ArrayList<>();
      row.add(rowLabel(state));
      row.add(namesOf(state.getEpsilonTransitions()));
      for (char symbol : alphabet) {
        row.add(namesOf(state.transitionsOn(symbol)));
      }
      row.add("");
      rows.add(row);
    }

    int[] widths = new int[headers.size()];
    for (int column = 0; column < headers.size(); column++) {
      widths[column] = headers.get(column).length();
      for (List<String> row : rows) {
        widths[column] = Math.max(widths[column], row.get(column).length());
      }
    }

    StringBuilder table = new StringBuilder();
    table.append(renderRow(headers, widths));
    for (List<String> row : rows) {
      table.append(System.lineSeparator()).append(renderRow(row, widths));
    }
    return table.toString();
  }

  /** Pads each cell to its column width, then trims the line end. */
  private static String renderRow(List<String> cells, int[] widths) {
    StringBuilder line = new StringBuilder();
    for (int column = 0; column < cells.size(); column++) {
      if (column > 0) {
        line.append("  ");
      }
      String cell = cells.get(column);
      line.append(cell);
      for (int pad = cell.length(); pad < widths[column]; pad++) {
        line.append(' ');
      }
    }
    // Trailing padding carries no information and makes the output awkward to
    // compare in tests.
    int end = line.length();
    while (end > 0 && line.charAt(end - 1) == ' ') {
      end--;
    }
    return line.substring(0, end);
  }

  private String rowLabel(State state) {
    StringBuilder label = new StringBuilder();
    if (state == start) {
      label.append('>');
    }
    if (state == accept) {
      label.append('*');
    }
    return label.append(state.getName()).toString();
  }

  /** A space would be invisible as a column heading, so show it quoted. */
  private static String columnLabel(char symbol) {
    return symbol == ' ' ? "' '" : String.valueOf(symbol);
  }

  private static String namesOf(Collection<State> states) {
    StringJoiner joiner = new StringJoiner(",");
    for (State state : states) {
      joiner.add(state.getName());
    }
    return joiner.toString();
  }
}
