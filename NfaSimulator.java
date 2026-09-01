import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Runs an input string through an {@link EpsilonNfa}, one character at a time.
 *
 * <p>This is the event-driven half of the engine: each character read from the
 * input is an event, and handling it moves the machine from one set of active
 * states to the next. The machine itself never changes -- all of the state that
 * varies during a run lives here, which is what makes resetting between input
 * lines a single assignment rather than a rebuild.
 *
 * <p>No subset construction is performed. Tracking the set of active states
 * directly gives the same answer as converting to a DFA first, without paying
 * for states the input never visits.
 */
public final class NfaSimulator {

  private final EpsilonNfa machine;

  /** The states the machine could be in right now. */
  private Set<State> current;

  /**
   * Creates a simulator positioned at the start of the machine.
   *
   * @param machine the machine to run
   */
  public NfaSimulator(EpsilonNfa machine) {
    this.machine = machine;
    reset();
  }

  /**
   * Returns the machine to its starting position, discarding any progress.
   *
   * <p>Called between input lines. Each line is matched independently, so state
   * left over from the previous line would corrupt the next one.
   */
  public void reset() {
    current = machine.epsilonClosure(Collections.singleton(machine.getStart()));
  }

  /**
   * Advances the machine by one character.
   *
   * @param symbol the character read
   */
  public void consume(char symbol) {
    Set<State> moved = new LinkedHashSet<>();
    for (State state : current) {
      moved.addAll(state.transitionsOn(symbol));
    }
    // An empty result stays empty for every later character, which is exactly
    // what makes a string that merely contains a match still come out false.
    current = machine.epsilonClosure(moved);
  }

  /**
   * @return whether the input read so far is an exact match
   */
  public boolean isAccepting() {
    return current.contains(machine.getAccept());
  }

  /**
   * @return whether the machine has run out of live states and can no longer
   *     match whatever follows
   */
  public boolean isDead() {
    return current.isEmpty();
  }

  /**
   * @return the states the machine could currently be in
   */
  public Set<State> getCurrentStates() {
    return Collections.unmodifiableSet(current);
  }

  /**
   * Matches a whole string from a clean start.
   *
   * @param input the string to test
   * @return whether the input matches the expression exactly
   */
  public boolean matches(String input) {
    reset();
    for (int index = 0; index < input.length(); index++) {
      consume(input.charAt(index));
    }
    return isAccepting();
  }
}
