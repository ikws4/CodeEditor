package io.ikws4.codeeditor.core;

import android.view.KeyEvent;

public interface EditorKeyListener {
    void onKeyDown(CodeEditor editor, int keycode, KeyEvent event);

    void onKeyUp(CodeEditor editor, int keycode, KeyEvent event);
}
