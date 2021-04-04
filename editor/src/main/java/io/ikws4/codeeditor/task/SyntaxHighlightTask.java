package io.ikws4.codeeditor.task;

import android.os.AsyncTask;
import android.text.Editable;

import java.lang.ref.WeakReference;
import java.util.List;

import io.ikws4.codeeditor.CodeEditor;
import io.ikws4.codeeditor.api.configuration.SyntaxColorScheme;
import io.ikws4.codeeditor.api.language.LanguageStyler;
import io.ikws4.codeeditor.span.SyntaxHighlightSpan;

public class SyntaxHighlightTask extends AsyncTask<Void, Void, List<SyntaxHighlightSpan>> {
    private final WeakReference<CodeEditor> mEditor;
    private final OnTaskFinishedListener<List<SyntaxHighlightSpan>> mListener;

    public SyntaxHighlightTask(CodeEditor editor, OnTaskFinishedListener<List<SyntaxHighlightSpan>> listener) {
        super();
        mEditor = new WeakReference<>(editor);
        mListener = listener;
    }

    @Override
    protected List<SyntaxHighlightSpan> doInBackground(Void... voids) {
        CodeEditor editor = mEditor.get();
        Editable content = editor.getText();
        LanguageStyler highlighter = editor.getLanguage().getStyler();
        SyntaxColorScheme syntaxScheme = editor.getConfiguration().getColorScheme().getSyntaxColorScheme();
        return highlighter.highlight(content.toString(), syntaxScheme);
    }

    @Override
    protected void onPostExecute(List<SyntaxHighlightSpan> spans) {
        mListener.onFinished(spans);
    }
}