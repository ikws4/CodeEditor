package io.ikws4.codeeditor.api.editor;

import android.view.KeyEvent;

import io.ikws4.codeeditor.CodeEditor;

public interface EditorKeyListener {
    void onKeyDown(CodeEditor editor, int keycode, KeyEvent event);

    void onKeyUp(CodeEditor editor, int keycode, KeyEvent event);
}
