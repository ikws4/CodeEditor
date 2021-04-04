package io.ikws4.codeeditor.span;

import android.text.NoCopySpan;
import android.view.KeyEvent;

import io.ikws4.codeeditor.CodeEditor;
import io.ikws4.codeeditor.EditorBaseKeyListener;

/**
 * It will be handle in {@link EditorBaseKeyListener#handleReplacedSpan(CodeEditor, int, KeyEvent)}
 */
public class ReplacedSpan implements NoCopySpan {
    private final char[] mText;

    public ReplacedSpan(char[] text) {
        mText = text;
    }

    public char[] getText() {
        return mText;
    }
}
