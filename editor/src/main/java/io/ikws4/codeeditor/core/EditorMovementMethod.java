package io.ikws4.codeeditor.core;

import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.KeyListener;
import android.text.method.QwertyKeyListener;
import android.text.method.ScrollingMovementMethod;
import android.text.method.Touch;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import org.checkerframework.checker.units.qual.A;

import io.ikws4.codeeditor.core.component.ClipboardPanel;
import io.ikws4.codeeditor.core.span.ReplacedSpan;

class EditorMovementMethod extends ArrowKeyMovementMethod {
    private final GestureDetector mGestureDetector;
    private final ClipboardPanel mClipboardPanel;
    private final CodeEditor mEditor;

    private EditorMovementMethod(CodeEditor editor) {
        mEditor = editor;
        mClipboardPanel = new ClipboardPanel(mEditor);
        mGestureDetector = new GestureDetector(mEditor.getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                showClipboardPanel();
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (mClipboardPanel.isShow()) {
                    mClipboardPanel.hide();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return Touch.onTouchEvent(widget, buffer, event);
    }

    @Override
    public boolean canSelectArbitrarily() {
        return true;
    }

    private void showClipboardPanel() {
        if (mClipboardPanel.isShow()) return;
        mEditor.startActionMode(mClipboardPanel);
    }

    public static EditorMovementMethod getInstance(CodeEditor editor) {
        if (sInstance == null) {
            sInstance = new EditorMovementMethod(editor);
        }
        return sInstance;
    }

    private static EditorMovementMethod sInstance;
}