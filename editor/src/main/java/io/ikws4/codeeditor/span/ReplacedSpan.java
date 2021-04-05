package io.ikws4.codeeditor.span;

import android.text.NoCopySpan;
import android.view.KeyEvent;

import io.ikws4.codeeditor.CodeEditor;
import io.ikws4.codeeditor.EditorBaseKeyListener;

/**
 * It will be handle in {@link EditorBaseKeyListener#handleReplacedSpan(CodeEditor, int, KeyEvent)}
 */
public class ReplacedSpan implements NoCopySpan, ExtendedSpan {
    private final char[] mText;
    private int mStart;
    private int mEnd;

    public ReplacedSpan(char[] text, int start, int end) {
        mText = text;
        mStart = start;
        mEnd = end;
    }

    public char[] getText() {
        return mText;
    }

    @Override
    public int getStart() {
        return mStart;
    }

    @Override
    public int getEnd() {
        return mEnd;
    }

    @Override
    public void shift(int offset) {
        mStart += offset;
        mEnd += offset;
    }
}
