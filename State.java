import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * One state of an &epsilon;-NFA, together with the edges leaving it.
 *
 * <p>States are compared by object identity rather than by {@code id}. The id
 * exists only so that a state can be given a stable, readable name
 * ({@code q0}, {@code q1}, ...) when the transition table is printed in verbose
 * mode.
 *
 * <p>Character edges and &epsilon; edges are stored in separate collections.
 * The alternative -- keeping &epsilon; in the same map under some sentinel
 * character -- does not work here, because in this assignment every printable
 * character is a legal literal, so there is no character left over to use as a
 * sentinel.
 *
 * <p>Both collections preserve insertion order, so the transition table renders
 * identically on every run. That matters for testing: an unordered set would
 * make the expected output depend on hash ordering.
 */
public class State {

  private final int id;

  /** Character-labelled edges, keyed by the symbol that must be consumed. */
  private final Map<Character, Set<State>> transitions = new LinkedHashMap<>();

  /** Edges that may be taken without consuming any input. */
  private final Set<State> epsilonTransitions = new LinkedHashSet<>();

  /**
   * Creates a state.
   *
   * @param id numeric identifier, unique within one machine
   */
  public State(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }

  /**
   * Returns the display name used in the transition table.
   *
   * @return this state's name, for example {@code q3}
   */
  public String getName() {
    return "q" + id;
  }

  /**
   * Adds an edge that consumes {@code symbol}.
   *
   * @param symbol the character that must be read to take this edge
   * @param target the state reached
   */
  public void addTransition(char symbol, State target) {
    transitions.computeIfAbsent(symbol, key -> new LinkedHashSet<>()).add(target);
  }

  /**
   * Adds an edge that consumes no input.
   *
   * @param target the state reached
   */
  public void addEpsilonTransition(State target) {
    epsilonTransitions.add(target);
  }

  /**
   * Returns the states reachable from here by consuming one character.
   *
   * @param symbol the character read
   * @return the reachable states, empty if this state has no edge for that
   *     symbol -- which is how "other" characters become dead ends
   */
  public Set<State> transitionsOn(char symbol) {
    return Collections.unmodifiableSet(
        transitions.getOrDefault(symbol, Collections.<State>emptySet()));
  }

  /**
   * @return the states reachable from here without consuming input
   */
  public Set<State> getEpsilonTransitions() {
    return Collections.unmodifiableSet(epsilonTransitions);
  }

  /**
   * @return every symbol this state has an outgoing edge for
   */
  public Set<Character> outgoingSymbols() {
    return Collections.unmodifiableSet(transitions.keySet());
  }

  @Override
  public String toString() {
    return getName();
  }
}
