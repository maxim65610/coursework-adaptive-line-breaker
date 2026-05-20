package hyphenation.model;

public class BreakCandidate {

    private final int utf16Offset;
    private final BreakType type;
    private final boolean appendHyphen;
    private final int penalty;

    public BreakCandidate(int utf16Offset, BreakType type, boolean appendHyphen, int penalty) {
        this.utf16Offset = utf16Offset;
        this.type = type;
        this.appendHyphen = appendHyphen;
        this.penalty = penalty;
    }

    public static BreakCandidate technicalStart(int utf16Offset) {
        return new BreakCandidate(utf16Offset, BreakType.EXPLICIT_BREAK, false, 0);
    }

    public static BreakCandidate space(int utf16Offset) {
        return new BreakCandidate(utf16Offset, BreakType.SPACE, false, 0);
    }

    public static BreakCandidate explicitBreak(int utf16Offset) {
        return new BreakCandidate(utf16Offset, BreakType.EXPLICIT_BREAK, false, Integer.MIN_VALUE);
    }

    public static BreakCandidate hyphenation(int utf16Offset, boolean appendHyphen, int penalty) {
        return new BreakCandidate(utf16Offset, BreakType.HYPHENATION, appendHyphen, penalty);
    }

    public static BreakCandidate punctuation(int utf16Offset) {
        return new BreakCandidate(utf16Offset, BreakType.PUNCT, false, 1);
    }

    public static BreakCandidate endOfText(int utf16Offset) {
        return new BreakCandidate(utf16Offset, BreakType.END_OF_TEXT, false, 0);
    }

    public int getUtf16Offset() {
        return utf16Offset;
    }

    public BreakType getType() {
        return type;
    }

    public boolean isAppendHyphen() {
        return appendHyphen;
    }

    public int getPenalty() {
        return penalty;
    }
}