package hyphenation.model;

/**
 * Один допустимый кандидат разрыва строки.
 */
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