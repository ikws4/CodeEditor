package io.ikws4.codeeditor;

import android.text.Spannable;
import android.text.method.ArrowKeyMovementMethod;
import android.text.method.Touch;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;

import io.ikws4.codeeditor.component.ClipboardPanel;

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