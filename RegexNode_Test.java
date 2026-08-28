import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import org.junit.Test;

/**
 * Tests the &epsilon;-NFA construction rules carried by {@link RegexNode}.
 *
 * <p>These tests assert on the <em>shape</em> of the machine -- which states
 * exist and which edges join them -- rather than on whether it matches
 * anything. Matching is the simulator's job and is tested separately. Checking
 * the shape here means that if acceptance later breaks, the failure points at
 * whichever layer actually broke.
 */
public class RegexNode_Test {

  /**
   * Minimal {@link NfaBuilder} that hands out numbered states and remembers
   * them, so a test can look a state up by the id it was given.
   */
  private static final class RecordingBuilder implements NfaBuilder {

    private final List<State> states = new ArrayList<>();

    @Override
    public State newState() {
      State created = new State(states.size());
      states.add(created);
      return created;
    }

    State get(int id) {
      return states.get(id);
    }

    int size() {
      return states.size();
    }
  }

  /** Renders a set of states as a comma-separated list of names. */
  private static String names(Set<State> states) {
    StringJoiner joiner = new StringJoiner(",");
    for (State state : states) {
      joiner.add(state.getName());
    }
    return joiner.toString();
  }

  private static String epsilonOf(State state) {
    return names(state.getEpsilonTransitions());
  }

  private static String moveOf(State state, char symbol) {
    return names(state.transitionsOn(symbol));
  }

  @Test
  public void literalCreatesOneEdgeAndOneState() {
    RecordingBuilder builder = new RecordingBuilder();
    State entry = builder.newState();

    State exit = new RegexNode.Literal('a').build(builder, entry);

    assertEquals("one state for the entry, one for the exit", 2, builder.size());
    assertEquals("q1", moveOf(entry, 'a'));
    assertEquals("q1", exit.getName());
    assertEquals("literal adds no epsilon edges", "", epsilonOf(entry));
  }

  @Test
  public void literalDoesNotRespondToOtherCharacters() {
    RecordingBuilder builder = new RecordingBuilder();
    State entry = builder.newState();

    new RegexNode.Literal('a').build(builder, entry);

    assertEquals("unknown characters are dead ends", "", moveOf(entry, 'z'));
  }

  @Test
  public void concatChainsStatesWithoutEpsilonEdges() {
    RecordingBuilder builder = new RecordingBuilder();
    State entry = builder.newState();

    State exit =
        new RegexNode.Concat(
                Arrays.asList(new RegexNode.Literal('a'), new RegexNode.Literal('b')))
            .build(builder, entry);

    assertEquals("three states, not four", 3, builder.size());
    assertEquals("q1", moveOf(builder.get(0), 'a'));
    assertEquals("q2", moveOf(builder.get(1), 'b'));
    assertEquals("q2", exit.getName());
    for (int id = 0; id < builder.size(); id++) {
      assertEquals(
          "concatenation should introduce no epsilon edges at all",
          "",
          epsilonOf(builder.get(id)));
    }
  }

  @Test
  public void starLetsControlReachTheExitWithoutEnteringTheBody() {
    RecordingBuilder builder = new RecordingBuilder();
    State entry = builder.newState();

    State exit = new RegexNode.Star(new RegexNode.Literal('a')).build(builder, entry);

    // q0 is the loop head: into the body on 'a', or straight out to q2.
    assertEquals("q1", moveOf(entry, 'a'));
    assertTrue(
        "zero iterations must be possible, so the head reaches the exit directly",
        entry.getEpsilonTransitions().contains(exit));
    assertEquals("body must loop back to the head", "q0", epsilonOf(builder.get(1)));
  }

  @Test
  public void plusForcesTheBodyToMatchAtLeastOnce() {
    RecordingBuilder builder = new RecordingBuilder();
    State entry = builder.newState();

    State exit = new RegexNode.Plus(new RegexNode.Literal('a')).build(builder, entry);

    assertEquals("q1", moveOf(entry, 'a'));
    assertFalse(
        "the head must NOT reach the exit directly, or the body could be skipped",
        entry.getEpsilonTransitions().contains(exit));
    assertEquals("body exit both loops back and leaves", "q0,q2", epsilonOf(builder.get(1)));
  }

  @Test
  public void starAndPlusDifferOnlyInWhereTheExitEdgeStarts() {
    RecordingBuilder starBuilder = new RecordingBuilder();
    State starEntry = starBuilder.newState();
    new RegexNode.Star(new RegexNode.Literal('a')).build(starBuilder, starEntry);

    RecordingBuilder plusBuilder = new RecordingBuilder();
    State plusEntry = plusBuilder.newState();
    new RegexNode.Plus(new RegexNode.Literal('a')).build(plusBuilder, plusEntry);

    assertEquals("same number of states", starBuilder.size(), plusBuilder.size());
    assertEquals("same body edge", moveOf(starEntry, 'a'), moveOf(plusEntry, 'a'));
    assertEquals("star exits from the head", "q2", epsilonOf(starEntry));
    assertEquals("plus does not", "", epsilonOf(plusEntry));
  }

  @Test
  public void alternationGivesEachBranchItsOwnEntryState() {
    RecordingBuilder builder = new RecordingBuilder();
    State entry = builder.newState();

    State exit =
        new RegexNode.Alternation(
                Arrays.asList(new RegexNode.Literal('a'), new RegexNode.Literal('b')))
            .build(builder, entry);

    assertEquals("q1,q3", epsilonOf(entry));
    assertEquals("q2", moveOf(builder.get(1), 'a'));
    assertEquals("q4", moveOf(builder.get(3), 'b'));
    assertEquals("both branches merge on one exit", "q5", epsilonOf(builder.get(2)));
    assertEquals("both branches merge on one exit", "q5", epsilonOf(builder.get(4)));
    assertEquals("q5", exit.getName());
  }

  /**
   * Checks the worked example against the hand-traced machine.
   */
  @Test
  public void bracketedExampleMatchesTheHandTracedMachine() {
    RegexNode ast =
        new RegexNode.Alternation(
            Arrays.asList(
                new RegexNode.Star(
                    new RegexNode.Concat(
                        Arrays.asList(new RegexNode.Literal('a'), new RegexNode.Literal('b')))),
                new RegexNode.Plus(new RegexNode.Literal('c'))));

    RecordingBuilder builder = new RecordingBuilder();
    State start = builder.newState();
    State accept = ast.build(builder, start);

    assertEquals("nine states, as hand-traced", 9, builder.size());
    assertEquals("q8", accept.getName());

    assertEquals("q1,q5", epsilonOf(builder.get(0)));
    assertEquals("q4", epsilonOf(builder.get(1)));
    assertEquals("q2", moveOf(builder.get(1), 'a'));
    assertEquals("q3", moveOf(builder.get(2), 'b'));
    assertEquals("q1", epsilonOf(builder.get(3)));
    assertEquals("q8", epsilonOf(builder.get(4)));
    assertEquals("q6", moveOf(builder.get(5), 'c'));
    assertEquals("q5,q7", epsilonOf(builder.get(6)));
    assertEquals("q8", epsilonOf(builder.get(7)));
    assertEquals("accepting state is a sink", "", epsilonOf(builder.get(8)));
  }

  @Test
  public void treeRendersItsStructureForParserTests() {
    RegexNode ast =
        new RegexNode.Alternation(
            Arrays.asList(
                new RegexNode.Concat(
                    Arrays.asList(
                        new RegexNode.Literal('a'),
                        new RegexNode.Star(new RegexNode.Literal('b')))),
                new RegexNode.Plus(new RegexNode.Literal('c'))));

    assertEquals("Alt(Concat(Lit(a),Star(Lit(b))),Plus(Lit(c)))", ast.toString());
  }
}
