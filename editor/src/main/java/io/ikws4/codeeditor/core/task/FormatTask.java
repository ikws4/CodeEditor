package io.ikws4.codeeditor.core.task;

import android.os.AsyncTask;
import android.text.Editable;

import java.lang.ref.WeakReference;

import io.ikws4.codeeditor.core.CodeEditor;

public class FormatTask extends AsyncTask<Void, Void, String> {
    private final WeakReference<CodeEditor> mEditor;
    private final OnTaskFinishedListener<String> mListener;

    public FormatTask(CodeEditor editor, OnTaskFinishedListener<String> listener) {
        super();
        mEditor = new WeakReference<>(editor);
        mListener = listener;
    }

    @Override
    protected String doInBackground(Void... voids) {
        CodeEditor editor = mEditor.get();
        Editable content = editor.getText();
        return editor.getLanguage().getStyler().format(content.toString());
    }

    @Override
    protected void onPostExecute(String s) {
        mListener.onFinished(s);
    }
}
