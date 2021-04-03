package io.ikws4.codeeditor.core;

import android.text.Editable;
import android.view.KeyEvent;

import io.ikws4.codeeditor.core.span.ReplacedSpan;

/**
 * Handle base keyevent for indent.
 */
public class EditorBaseKeyListener implements EditorKeyListener {

    @Override
    public void onKeyDown(CodeEditor editor, int keycode, KeyEvent event) {
        handleReplacedSpan(editor, keycode, event);
    }

    @Override
    public void onKeyUp(CodeEditor editor, int keycode, KeyEvent event) {
        handleIndent(editor,keycode, event);
    }

    private void handleIndent(CodeEditor editor, int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER && (event.hasNoModifiers())) {
            editor.indent(editor.getCurrentLine());
        }
    }

    private void handleReplacedSpan(CodeEditor editor, int keyCode, KeyEvent event) {
        Editable content = editor.getText();
        int start = editor.getSelectionStart();
        if (keyCode == KeyEvent.KEYCODE_DEL && (event.hasNoModifiers() || event.hasModifiers(KeyEvent.META_ALT_ON))) {
            ReplacedSpan[] repl = content.getSpans(start - 1, start, ReplacedSpan.class);
            if (repl.length > 0) {
                int st = content.getSpanStart(repl[0]);
                int en = content.getSpanEnd(repl[0]);
                String old = new String(repl[0].getText());
                content.removeSpan(repl[0]);
                if (start >= en) {
                    content.replace(st, en, old);
                }
            }
        }
    }
}
