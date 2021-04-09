package io.ikws4.codeeditor.task;

import android.os.AsyncTask;
import android.text.Editable;

import java.lang.ref.WeakReference;

import io.ikws4.codeeditor.api.editor.Editor;
import io.ikws4.codeeditor.component.TextArea;

public class FormatTask extends AsyncTask<Void, Void, String> {
    private final WeakReference<Editor> mEditor;
    private final OnTaskFinishedListener<String> mListener;

    public FormatTask(Editor editor, OnTaskFinishedListener<String> listener) {
        super();
        mEditor = new WeakReference<>(editor);
        mListener = listener;
    }

    @Override
    protected String doInBackground(Void... voids) {
        Editor editor = mEditor.get();
        CharSequence content = editor.getText();
        return editor.getLanguage().getStyler().format(content.toString());
    }

    @Override
    protected void onPostExecute(String s) {
        mListener.onFinished(s);
    }
}
