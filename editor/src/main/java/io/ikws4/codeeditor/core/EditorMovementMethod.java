package io.ikws4.codeeditor.core;

import android.text.Selection;
import android.text.Spannable;
import android.text.method.ScrollingMovementMethod;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;

import org.checkerframework.checker.units.qual.A;

import io.ikws4.codeeditor.core.component.ClipboardPanel;

class EditorMovementMethod extends ScrollingMovementMethod {
    private final GestureDetector mGestureDetector;
    private final ClipboardPanel mClipboardPanel;
    private final CodeEditor mEditor;

    public EditorMovementMethod(CodeEditor editor) {
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
    public boolean canSelectArbitrarily() {
        return true;
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return super.onTouchEvent(widget, buffer, event);
    }

    private void showClipboardPanel() {
        if (mClipboardPanel.isShow()) return;
        mEditor.startActionMode(mClipboardPanel);
    }
}