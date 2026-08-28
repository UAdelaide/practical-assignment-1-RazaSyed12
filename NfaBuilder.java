/**
 * Supplies fresh states during &epsilon;-NFA construction.
 *
 * <p>{@link RegexNode#build} needs to create new states, but nothing else about
 * the machine being assembled. Narrowing that dependency to this one method
 * keeps the syntax tree from knowing anything about {@link EpsilonNfa}, which
 * in turn means the construction rules can be unit tested against a throwaway
 * builder rather than a whole engine.
 */
public interface NfaBuilder {

  /**
   * Creates a state that no edge yet points at.
   *
   * @return the new state, owned by the machine under construction
   */
  State newState();
}
