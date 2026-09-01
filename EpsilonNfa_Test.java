import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/** Tests for {@link EpsilonNfa}: state layout, closures, and table rendering. */
public class EpsilonNfa_Test {

  private static EpsilonNfa machineFor(String regex) throws RegexSyntaxException {
    return EpsilonNfa.build(RegexParser.parse(regex));
  }

  /** State ids in ascending order, so assertions do not depend on set order. */
  private static List<Integer> idsOf(Set<State> states) {
    List<Integer> ids = new ArrayList<>();
    for (State state : states) {
      ids.add(state.getId());
    }
    Collections.sort(ids);
    return ids;
  }

  private static List<Integer> ids(int... values) {
    List<Integer> ids = new ArrayList<>();
    for (int value : values) {
      ids.add(value);
    }
    return ids;
  }

  private static String lines(String... values) {
    return String.join(System.lineSeparator(), values);
  }

  @Test
  public void machineHasOneStartAndOneAcceptingState() throws Exception {
    EpsilonNfa machine = machineFor("(ab)*|c+");

    assertEquals("q0", machine.getStart().getName());
    assertEquals("q8", machine.getAccept().getName());
    assertEquals("nine states, as hand-traced", 9, machine.getStates().size());
  }

  @Test
  public void singleLiteralNeedsOnlyTwoStates() throws Exception {
    assertEquals(2, machineFor("a").getStates().size());
  }

  @Test
  public void alphabetIsSortedAndHoldsOnlyCharactersTheRegexUses() throws Exception {
    assertEquals("[a, b, c]", machineFor("(ab)*|c+").getAlphabet().toString());
    assertEquals("[ , 1, a, b]", machineFor("a b1").getAlphabet().toString());
  }

  @Test
  public void closureOfTheStartStateReachesTheAcceptingStateWhenEmptyMatches()
      throws Exception {
    EpsilonNfa machine = machineFor("(ab)*|c+");

    Set<State> closure = machine.epsilonClosure(Collections.singleton(machine.getStart()));

    assertEquals(ids(0, 1, 4, 5, 8), idsOf(closure));
    assertTrue(
        "(ab)* matches the empty string, so the accepting state must already be live",
        closure.contains(machine.getAccept()));
  }

  @Test
  public void closureOfTheStartStateSkipsTheAcceptingStateWhenEmptyDoesNotMatch()
      throws Exception {
    EpsilonNfa machine = machineFor("c+");

    Set<State> closure = machine.epsilonClosure(Collections.singleton(machine.getStart()));

    assertTrue("c+ needs at least one c", !closure.contains(machine.getAccept()));
  }

  @Test
  public void closureOfNothingIsNothing() throws Exception {
    EpsilonNfa machine = machineFor("abc");

    assertTrue(machine.epsilonClosure(Collections.<State>emptySet()).isEmpty());
  }

  @Test
  public void closureTerminatesOnAnExpressionWhoseEpsilonEdgesFormACycle()
      throws Exception {
    // a** builds nested loops, so the closure walk revisits states it has
    // already seen. Without the visited check this would not return.
    EpsilonNfa machine = machineFor("a**");

    Set<State> closure = machine.epsilonClosure(Collections.singleton(machine.getStart()));

    assertTrue(closure.contains(machine.getAccept()));
  }

  @Test
  public void tableForASingleLiteral() throws Exception {
    assertEquals(
        lines(
            "     epsilon  a   other",
            ">q0           q1",
            "*q1"),
        machineFor("a").transitionTable());
  }

  /** Locks the table against the hand-traced machine. */
  @Test
  public void tableForTheBriefsExample() throws Exception {
    assertEquals(
        lines(
            "     epsilon  a   b   c   other",
            ">q0  q1,q5",
            "q1   q4       q2",
            "q2                q3",
            "q3   q1",
            "q4   q8",
            "q5                    q6",
            "q6   q5,q7",
            "q7   q8",
            "*q8"),
        machineFor("(ab)*|c+").transitionTable());
  }

  @Test
  public void tableShowsASpaceLiteralAsAQuotedColumn() throws Exception {
    String table = machineFor("a b").transitionTable();

    assertTrue("a bare space column heading would be invisible", table.contains("' '"));
  }

  @Test
  public void tableMarksStartAndAcceptingStates() throws Exception {
    String table = machineFor("ab").transitionTable();

    assertTrue(table.contains(">q0"));
    assertTrue(table.contains("*q2"));
  }

  @Test
  public void tableAlwaysCarriesAnOtherColumn() throws Exception {
    assertTrue(machineFor("a").transitionTable().contains("other"));
  }
}
