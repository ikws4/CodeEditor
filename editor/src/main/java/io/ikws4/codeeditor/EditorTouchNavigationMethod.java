package io.ikws4.codeeditor;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;

//class EditorTouchNavigationMethod extends GestureDetector.SimpleOnGestureListener {
//    private final TextEditor mTextEditor;
//    private final GestureDetector mGestureDetector;
//
//    public EditorTouchNavigationMethod(TextEditor editor) {
//        mTextEditor = editor;
//        mGestureDetector = new GestureDetector(editor.getContext(), this);
//    }
//
//    protected boolean onTouchEvent(MotionEvent event) {
//        return mGestureDetector.onTouchEvent(event);
//    }
//
//    @Override
//    public boolean onSingleTapUp(MotionEvent e) {
//        InputMethodManager imm = (InputMethodManager) mTextEditor.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//        imm.showSoftInput(mTextEditor, 0);
//        return true;
//    }
//}
