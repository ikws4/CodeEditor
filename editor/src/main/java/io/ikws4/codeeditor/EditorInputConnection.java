//package io.ikws4.codeeditor;
//
//import android.text.Editable;
//import android.view.inputmethod.BaseInputConnection;
//
//class EditorInputConnection extends BaseInputConnection {
//    private final TextEditor mTextEditor;
//
//    public EditorInputConnection(TextEditor editor) {
//        super(editor, true);
//        mTextEditor = editor;
//    }
//
//    @Override
//    public Editable getEditable() {
//        return mTextEditor.getText();
//    }
//
//}
