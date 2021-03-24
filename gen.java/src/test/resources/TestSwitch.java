public class TestSwitch {
  public List<Diff> parse(InputStream in) {
    ResizingParseWindow window = new ResizingParseWindow(in);
    ParserState state = ParserState.INITIAL;
    List<Diff> parsedDiffs = new ArrayList<>();
    Diff currentDiff = new Diff();
    String currentLine;
    while ((currentLine = window.slideForward()) != null) {
      ParserState lastState = state;
      state = state.nextState(window);
      switch (state) {
        case INITIAL:
          // nothing to do
          break;
        case HEADER:
          if ((lastState != ParserState.INITIAL)
              && (lastState != ParserState.HEADER)
              && (lastState != ParserState.END)) {
            parsedDiffs.add(currentDiff);
            currentDiff = new Diff();
          }
          parseHeader(currentDiff, currentLine);
          break;
        case FROM_FILE:
          parseFromFile(currentDiff, currentLine);
          break;
        case TO_FILE:
          parseToFile(currentDiff, currentLine);
          break;
        case HUNK_START:
          parseHunkStart(currentDiff, currentLine);
          break;
        case FROM_LINE:
          parseFromLine(currentDiff, currentLine);
          break;
        case TO_LINE:
          parseToLine(currentDiff, currentLine);
          break;
        case NEUTRAL_LINE:
          parseNeutralLine(currentDiff, currentLine);
          break;
        case END:
          parsedDiffs.add(currentDiff);
          currentDiff = new Diff();
          break;
        default:
          throw new IllegalStateException(String.format("Illegal parser state '%s", state));
      }
    }

    // Something like that may be needed to make sure no diffs are lost.
    if (currentDiff.getHunks().size() > 0) {
      parsedDiffs.add(currentDiff);
      currentDiff = new Diff();
    }

    return parsedDiffs;
  }
}