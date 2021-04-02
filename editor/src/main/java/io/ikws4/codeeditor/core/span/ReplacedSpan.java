package io.ikws4.codeeditor.core.span;

import android.text.NoCopySpan;

public class ReplacedSpan implements NoCopySpan {
    private char[] mText;

    public ReplacedSpan(char[] text) {
        mText = text;
    }

    public char[] getText() {
        return mText;
    }
}
