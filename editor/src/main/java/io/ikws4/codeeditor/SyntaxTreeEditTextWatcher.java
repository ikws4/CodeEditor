package io.ikws4.codeeditor;

import android.text.Editable;
import android.text.Layout;
import android.text.TextWatcher;

class SyntaxTreeEditTextWatcher implements TextWatcher {
    private int startByte;
    private int oldEndByte;
    private int newEndByte;
    private int startRow;
    private int startColumn;
    private int oldEndRow;
    private int oldEndColumn;
    private int newEndRow;
    private int newEndColumn;
    private final CodeEditor mCodeEditor;

    SyntaxTreeEditTextWatcher(CodeEditor codeEditor) {
        mCodeEditor = codeEditor;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        Layout layout = mCodeEditor.getLayout();
        if (layout != null) {
            startByte = start;
            oldEndByte = start + count;

            startRow = layout.getLineForOffset(startByte);
            startColumn = startByte - layout.getLineStart(startRow);

            oldEndRow = layout.getLineForOffset(oldEndByte);
            oldEndColumn = oldEndByte - layout.getLineStart(oldEndRow);
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        Layout layout = mCodeEditor.getLayout();
        if (layout != null) {
            newEndByte = start + count;

            newEndRow = layout.getLineForOffset(newEndByte);
            newEndColumn = newEndByte - layout.getLineStart(newEndRow);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        mCodeEditor.getLanguage().getStyler().editSyntaxTree(startByte, oldEndByte, newEndByte,
                startRow, startColumn,
                oldEndRow, oldEndColumn,
                newEndRow, newEndColumn);
    }
}
